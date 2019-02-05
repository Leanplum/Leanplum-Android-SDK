package com.leanplum.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import com.leanplum.Leanplum;
import com.leanplum.utils.SharedPreferencesUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

public class RequestSender {
    private static RequestSender defaultSender;

    private CountAggregator countAggregator;

    public synchronized static RequestSender getInstance() {
        if (defaultSender == null) {
            defaultSender = new RequestSender();
            defaultSender.countAggregator = Leanplum.countAggregator();
        }
        return defaultSender;
    }

    private final long DEVELOPMENT_MIN_DELAY_MS = 100;
    private final long DEVELOPMENT_MAX_DELAY_MS = 5000;
    private final long PRODUCTION_DELAY = 60000;
    private RequestSequenceRecorder requestSequenceRecorder;
    static final int MAX_EVENTS_PER_API_CALL;
    final String LEANPLUM = "__leanplum__";
    final String UUID_KEY = "uuid";

    private String appId;
    private String accessKey;
    private String deviceId;
    private String userId;
    private static long lastSendTimeMs;

    private final LeanplumEventCallbackManager
            eventCallbackManager = new LeanplumEventCallbackManager();
    // The token is saved primarily for legacy SharedPreferences decryption. This could
    // likely be removed in the future.
    private String token = null;

    private RequestOld.ApiResponseCallback apiResponse;

    private List<Map<String, Object>> localErrors = new ArrayList<>();

    static {
        if (Build.VERSION.SDK_INT <= 17) {
            MAX_EVENTS_PER_API_CALL = 5000;
        } else {
            MAX_EVENTS_PER_API_CALL = 10000;
        }
    }

    public void setAppId(String appId, String accessKey) {
        if (!TextUtils.isEmpty(appId)) {
            this.appId = appId.trim();
        }
        if (!TextUtils.isEmpty(accessKey)) {
            this.accessKey = accessKey.trim();
        }
        Leanplum.countAggregator().incrementCount("set_app_id");
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setToken(String token) {
        this.token = token;
        Leanplum.countAggregator().incrementCount("set_token");
    }

    public String token() {
        return token;
    }

    public void loadToken() {
        Context context = Leanplum.getContext();
        SharedPreferences defaults = context.getSharedPreferences(
                LEANPLUM, Context.MODE_PRIVATE);
        String token = defaults.getString(Constants.Defaults.TOKEN_KEY, null);
        if (token == null) {
            return;
        }
        setToken(token);
        Leanplum.countAggregator().incrementCount("load_token");
    }

    public void saveToken() {
        Context context = Leanplum.getContext();
        SharedPreferences defaults = context.getSharedPreferences(
                LEANPLUM, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = defaults.edit();
        editor.putString(Constants.Defaults.TOKEN_KEY, RequestOld.token());
        SharedPreferencesUtil.commitChanges(editor);
    }

    public String appId() {
        return appId;
    }

    public String deviceId() {
        return deviceId;
    }

    public String userId() {
        return userId;
    }

    @VisibleForTesting
    public Map<String, Object> createArgsDictionary(Requesting request) {
        Map<String, Object> args = new HashMap<>();
        args.put(Constants.Params.DEVICE_ID, deviceId);
        args.put(Constants.Params.USER_ID, userId);
        args.put(Constants.Params.ACTION, request.apiMethod);
        args.put(Constants.Params.SDK_VERSION, Constants.LEANPLUM_VERSION);
        args.put(Constants.Params.DEV_MODE, Boolean.toString(Constants.isDevelopmentModeEnabled));
        args.put(Constants.Params.TIME, Double.toString(new Date().getTime() / 1000.0));
        args.put(RequestOld.REQUEST_ID_KEY, request.requestId);
        if (token != null) {
            args.put(Constants.Params.TOKEN, token);
        }
        args.putAll(request.params);
        return args;
    }

    private void saveRequestForLater(Requesting request, Map<String, Object> args) {
        try {
            requestSequenceRecorder.beforeWrite();

            synchronized (RequestOld.class) {
                Context context = Leanplum.getContext();
                SharedPreferences preferences = context.getSharedPreferences(
                        LEANPLUM, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                long count = LeanplumEventDataManager.getEventsCount();
                String uuid = preferences.getString(Constants.Defaults.UUID_KEY, null);
                if (uuid == null || count % MAX_EVENTS_PER_API_CALL == 0) {
                    uuid = UUID.randomUUID().toString();
                    editor.putString(Constants.Defaults.UUID_KEY, uuid);
                    SharedPreferencesUtil.commitChanges(editor);
                }
                args.put(UUID_KEY, uuid);
                LeanplumEventDataManager.insertEvent(JsonConverter.toJson(args));

                request.setDataBaseIndex(count);
                // Checks if here response and/or error callback for this request. We need to add callbacks to
                // eventCallbackManager only if here was internet connection, otherwise triggerErrorCallback
                // will handle error callback for this event.
                if (request.response != null || request.error != null && !Util.isConnected()) {
                    eventCallbackManager.addCallbacks(request, request.response, request.error);
                }
            }

            requestSequenceRecorder.afterWrite();
        } catch (Throwable t) {
            Util.handleException(t);
        }
    }

    public void send(final Requesting request) {
        this.sendEventually(request);
        if (Constants.isDevelopmentModeEnabled) {
            long currentTimeMs = System.currentTimeMillis();
            long delayMs;
            if (lastSendTimeMs == 0 || currentTimeMs - lastSendTimeMs > DEVELOPMENT_MAX_DELAY_MS) {
                delayMs = DEVELOPMENT_MIN_DELAY_MS;
            } else {
                delayMs = (lastSendTimeMs + DEVELOPMENT_MAX_DELAY_MS) - currentTimeMs;
            }
            OsHandler.getInstance().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        sendIfConnected(request);
                    } catch (Throwable t) {
                        Util.handleException(t);
                    }
                }
            }, delayMs);
        }
        Leanplum.countAggregator().incrementCount("send_request");
    }

    /**
     * Wait 1 second for potential other API calls, and then sends the call synchronously if no other
     * call has been sent within 1 minute.
     */
    public void sendIfDelayed(final Requesting request) {
        sendEventually(request);
        OsHandler.getInstance().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sendIfDelayedHelper(request);
                } catch (Throwable t) {
                    Util.handleException(t);
                }
            }
        }, 1000);
        Leanplum.countAggregator().incrementCount("send_if_delayed");
    }

    /**
     * Sends the call synchronously if no other call has been sent within 1 minute.
     */
    private void sendIfDelayedHelper(Requesting request) {
        if (Constants.isDevelopmentModeEnabled) {
            send(request);
        } else {
            long currentTimeMs = System.currentTimeMillis();
            if (lastSendTimeMs == 0 || currentTimeMs - lastSendTimeMs > PRODUCTION_DELAY) {
                sendIfConnected(request);
            }
        }
    }

    public void sendIfConnected(Requesting request) {
        if (Util.isConnected()) {
            this.sendNow(request);
        } else {
            this.sendEventually(request);
            Log.i("Device is offline, will send later");
            triggerErrorCallback(request, new Exception("Not connected to the Internet"));
        }
        Leanplum.countAggregator().incrementCount("send_if_connected");
    }

    private void triggerErrorCallback(Requesting request, Exception e) {
        if (request.error != null) {
            request.error.error(e);
        }
        if (apiResponse != null) {
            List<Map<String, Object>> requests = getUnsentRequests(1.0);
            List<Map<String, Object>> requestsToSend = removeIrrelevantBackgroundStartRequests(requests);
            apiResponse.response(requestsToSend, null, requests.size());
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean attachApiKeys(Map<String, Object> dict) {
        if (appId == null || accessKey == null) {
            Log.e("API keys are not set. Please use Leanplum.setAppIdForDevelopmentMode or "
                    + "Leanplum.setAppIdForProductionMode.");
            return false;
        }
        dict.put(Constants.Params.APP_ID, appId);
        dict.put(Constants.Params.CLIENT_KEY, accessKey);
        dict.put(Constants.Params.CLIENT, Constants.CLIENT);
        return true;
    }

//    public interface ResponseCallback {
//        void response(JSONObject response);
//    }
//
//    public interface ApiResponseCallback {
//        void response(List<Map<String, Object>> requests, JSONObject response, int countOfEvents);
//    }
//
//    public interface ErrorCallback {
//        void error(Exception e);
//    }
//
//    public interface NoPendingDownloadsCallback {
//        void noPendingDownloads();
//    }

    /**
     * Parse response body from server.  Invoke potential error or response callbacks for all events
     * of this request.
     *
     * @param responseBody JSONObject with response body from server.
     * @param requestsToSend List of requests that were sent to the server/
     * @param error Exception.
     * @param unsentRequestsSize Size of unsent request, that we will delete.
     */
    private void parseResponseBody(JSONObject responseBody, List<Map<String, Object>>
            requestsToSend, Exception error, int unsentRequestsSize) {
        synchronized (RequestOld.class) {
            if (responseBody == null && error != null) {
                // Invoke potential error callbacks for all events of this request.
                eventCallbackManager.invokeAllCallbacksWithError(error, unsentRequestsSize);
                return;
            } else if (responseBody == null) {
                return;
            }

            // Response for last start call.
            if (apiResponse != null) {
                apiResponse.response(requestsToSend, responseBody, unsentRequestsSize);
            }

            // We will replace it with error from response body, if we found it.
            Exception lastResponseError = error;
            // Valid response, parse and handle response body.
            int numResponses = RequestOld.numResponses(responseBody);
            for (int i = 0; i < numResponses; i++) {
                JSONObject response = RequestOld.getResponseAt(responseBody, i);
                if (RequestOld.isResponseSuccess(response)) {
                    continue; // If event response is successful, proceed with next one.
                }

                // If event response was not successful, handle error.
                String errorMessage = getReadableErrorMessage(RequestOld.getResponseError(response));
                Log.e(errorMessage);
                // Throw an exception if last event response is negative.
                if (i == numResponses - 1) {
                    lastResponseError = new Exception(errorMessage);
                }
            }

            if (lastResponseError != null) {
                // Invoke potential error callbacks for all events of this request.
                eventCallbackManager.invokeAllCallbacksWithError(lastResponseError, unsentRequestsSize);
            } else {
                // Invoke potential response callbacks for all events of this request.
                eventCallbackManager.invokeAllCallbacksForResponse(responseBody, unsentRequestsSize);
            }
        }
    }

    /**
     * Parse error message from server response and return readable error message.
     *
     * @param errorMessage String of error from server response.
     * @return String of readable error message.
     */
    @NonNull
    private String getReadableErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.length() == 0) {
            errorMessage = "API error";
        } else if (errorMessage.startsWith("App not found")) {
            errorMessage = "No app matching the provided app ID was found.";
            Constants.isInPermanentFailureState = true;
        } else if (errorMessage.startsWith("Invalid access key")) {
            errorMessage = "The access key you provided is not valid for this app.";
            Constants.isInPermanentFailureState = true;
        } else if (errorMessage.startsWith("Development mode requested but not permitted")) {
            errorMessage = "A call to Leanplum.setAppIdForDevelopmentMode "
                    + "with your production key was made, which is not permitted.";
            Constants.isInPermanentFailureState = true;
        } else {
            errorMessage = "API error: " + errorMessage;
        }
        return errorMessage;
    }

    private void sendNow(Requesting request) {
        if (Constants.isTestMode) {
            return;
        }
        if (appId == null) {
            Log.e("Cannot send request. appId is not set.");
            return;
        }
        if (accessKey == null) {
            Log.e("Cannot send request. accessKey is not set.");
            return;
        }

        this.sendEventually(request);

        Leanplum.countAggregator().incrementCount("send_now");

        Util.executeAsyncTask(true, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    sendRequests();
                } catch (Throwable t) {
                    Util.handleException(t);
                }
                return null;
            }
        });
    }


    /**
     * This class wraps the unsent requests, requests that we need to send
     * and the JSON encoded string. Wrapping it in the class allows us to
     * retain consistency in the requests we are sending and the actual
     * JSON string.
     */
    static class RequestsWithEncoding {
        List<Map<String, Object>> unsentRequests;
        List<Map<String, Object>> requestsToSend;
        String jsonEncodedString;
    }

    private RequestOld.RequestsWithEncoding getRequestsWithEncodedStringForErrors() {
        List<Map<String, Object>> unsentRequests = new ArrayList<>();
        List<Map<String, Object>> requestsToSend;
        String jsonEncodedRequestsToSend;

        String uuid = UUID.randomUUID().toString();
        for (Map<String, Object> error : localErrors) {
            error.put(UUID_KEY, uuid);
            unsentRequests.add(error);
        }
        requestsToSend = unsentRequests;
        jsonEncodedRequestsToSend = jsonEncodeRequests(unsentRequests);

        RequestOld.RequestsWithEncoding requestsWithEncoding = new RequestOld.RequestsWithEncoding();
        // for errors, we send all unsent requests so they are identical
        requestsWithEncoding.unsentRequests = unsentRequests;
        requestsWithEncoding.requestsToSend = requestsToSend;
        requestsWithEncoding.jsonEncodedString = jsonEncodedRequestsToSend;

        return requestsWithEncoding;
    }


    protected RequestOld.RequestsWithEncoding getRequestsWithEncodedStringStoredRequests(double fraction) {
        try {
            List<Map<String, Object>> unsentRequests;
            List<Map<String, Object>> requestsToSend;
            String jsonEncodedRequestsToSend;
            RequestOld.RequestsWithEncoding requestsWithEncoding = new RequestOld.RequestsWithEncoding();

            if (fraction < 0.01) { //base case
                unsentRequests = new ArrayList<>(0);
                requestsToSend = new ArrayList<>(0);
            } else {
                unsentRequests = getUnsentRequests(fraction);
                requestsToSend = removeIrrelevantBackgroundStartRequests(unsentRequests);
            }

            jsonEncodedRequestsToSend = jsonEncodeRequests(requestsToSend);
            requestsWithEncoding.unsentRequests = unsentRequests;
            requestsWithEncoding.requestsToSend = requestsToSend;
            requestsWithEncoding.jsonEncodedString = jsonEncodedRequestsToSend;

            return requestsWithEncoding;
        } catch (OutOfMemoryError E) {
            // half the requests will need less memory, recursively
            return getRequestsWithEncodedStringStoredRequests(0.5 * fraction);
        }
    }

    private RequestOld.RequestsWithEncoding getRequestsWithEncodedString() {
        RequestOld.RequestsWithEncoding requestsWithEncoding;
        // Check if we have localErrors, if yes then we will send only errors to the server.
        if (localErrors.size() != 0) {
            requestsWithEncoding = getRequestsWithEncodedStringForErrors();
        } else {
            requestsWithEncoding = getRequestsWithEncodedStringStoredRequests(1.0);
        }

        return requestsWithEncoding;
    }

    private void sendRequests() {
        Leanplum.countAggregator().sendAllCounts();
        requestSequenceRecorder.beforeRead();

        RequestOld.RequestsWithEncoding requestsWithEncoding = getRequestsWithEncodedString();

        requestSequenceRecorder.afterRead();

        List<Map<String, Object>> unsentRequests = requestsWithEncoding.unsentRequests;
        List<Map<String, Object>> requestsToSend = requestsWithEncoding.requestsToSend;
        String jsonEncodedString = requestsWithEncoding.jsonEncodedString;

        if (requestsToSend.isEmpty()) {
            return;
        }

        final Map<String, Object> multiRequestArgs = new HashMap<>();
        if (attachApiKeys(multiRequestArgs)) {
            return;
        }
        multiRequestArgs.put(Constants.Params.DATA, jsonEncodedString);
        multiRequestArgs.put(Constants.Params.SDK_VERSION, Constants.LEANPLUM_VERSION);
        multiRequestArgs.put(Constants.Params.ACTION, Constants.Methods.MULTI);
        multiRequestArgs.put(Constants.Params.TIME, Double.toString(new Date().getTime() / 1000.0));

        JSONObject responseBody;
        HttpURLConnection op = null;
        try {
            try {
                op = Util.operation(
                        Constants.API_HOST_NAME,
                        Constants.API_SERVLET,
                        multiRequestArgs,
                        "POST",
                        Constants.API_SSL,
                        Constants.NETWORK_TIMEOUT_SECONDS);

                responseBody = Util.getJsonResponse(op);
                int statusCode = op.getResponseCode();

                Exception errorException;
                if (statusCode >= 200 && statusCode <= 299) {
                    if (responseBody == null) {
                        errorException = new Exception("Response JSON is null.");
                        deleteSentRequests(unsentRequests.size());
                        parseResponseBody(null, requestsToSend, errorException, unsentRequests.size());
                        return;
                    }

                    Exception exception = null;
                    // Checks if we received the same number of responses as a number of sent request.
                    int numResponses = RequestOld.numResponses(responseBody);
                    if (numResponses != requestsToSend.size()) {
                        Log.w("Sent " + requestsToSend.size() + " requests but only" +
                                " received " + numResponses);
                    }
                    parseResponseBody(responseBody, requestsToSend, null, unsentRequests.size());
                    // Clear localErrors list.
                    localErrors.clear();
                    deleteSentRequests(unsentRequests.size());

                    // Send another request if the last request had maximum events per api call.
                    if (unsentRequests.size() == MAX_EVENTS_PER_API_CALL) {
                        sendRequests();
                    }
                } else {
                    errorException = new Exception("HTTP error " + statusCode);
                    if (statusCode != -1 && statusCode != 408 && !(statusCode >= 500 && statusCode <= 599)) {
                        deleteSentRequests(unsentRequests.size());
                        parseResponseBody(responseBody, requestsToSend, errorException, unsentRequests.size());
                    }
                }
            } catch (JSONException e) {
                Log.e("Error parsing JSON response: " + e.toString() + "\n" + Log.getStackTraceString(e));
                deleteSentRequests(unsentRequests.size());
                parseResponseBody(null, requestsToSend, e, unsentRequests.size());
            } catch (Exception e) {
                Log.e("Unable to send request: " + e.toString() + "\n" + Log.getStackTraceString(e));
            } finally {
                if (op != null) {
                    op.disconnect();
                }
            }
        } catch (Throwable t) {
            Util.handleException(t);
        }
    }

    public void sendEventually(final Requesting request) {
        if (Constants.isTestMode) {
            return;
        }

        if (LeanplumEventDataManager.willSendErrorLog) {
            return;
        }

        if (!request.sent) {
            request.setSent(true);
            Util.executeAsyncTask(true, new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Map<String, Object> args = createArgsDictionary(request);
                    saveRequestForLater(request, args);
                    return null;
                }
            });
        }
        Leanplum.countAggregator().incrementCount("send_eventually");
    }

    static void deleteSentRequests(int requestsCount) {
        if (requestsCount == 0) {
            return;
        }
        synchronized (RequestOld.class) {
            LeanplumEventDataManager.deleteEvents(requestsCount);
        }
    }

    public List<Map<String, Object>> getUnsentRequests(double fraction) {
        List<Map<String, Object>> requestData;

        synchronized (RequestOld.class) {
            lastSendTimeMs = System.currentTimeMillis();
            Context context = Leanplum.getContext();
            SharedPreferences preferences = context.getSharedPreferences(
                    LEANPLUM, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            int count = (int) (fraction * MAX_EVENTS_PER_API_CALL);
            requestData = LeanplumEventDataManager.getEvents(count);
            editor.remove(Constants.Defaults.UUID_KEY);
            SharedPreferencesUtil.commitChanges(editor);
        }

        return requestData;
    }

    /**
     * In various scenarios we can end up batching a big number of requests (e.g. device is offline,
     * background sessions), which could make the stored API calls batch look something like:
     * <p>
     * <code>start(B), start(B), start(F), track, start(B), track, start(F), resumeSession</code>
     * <p>
     * where <code>start(B)</code> indicates a start in the background, and <code>start(F)</code>
     * one in the foreground.
     * <p>
     * In this case the first two <code>start(B)</code> can be dropped because they don't contribute
     * any relevant information for the batch call.
     * <p>
     * Essentially we drop every <code>start(B)</code> call, that is directly followed by any kind of
     * a <code>start</code> call.
     *
     * @param requestData A list of the requests, stored on the device.
     * @return A list of only these requests, which contain relevant information for the API call.
     */
    private static List<Map<String, Object>> removeIrrelevantBackgroundStartRequests(
            List<Map<String, Object>> requestData) {
        List<Map<String, Object>> relevantRequests = new ArrayList<>();

        int requestCount = requestData.size();
        if (requestCount > 0) {
            for (int i = 0; i < requestCount; i++) {
                Map<String, Object> currentRequest = requestData.get(i);
                if (i < requestCount - 1
                        && Constants.Methods.START.equals(requestData.get(i + 1).get(Constants.Params.ACTION))
                        && Constants.Methods.START.equals(currentRequest.get(Constants.Params.ACTION))
                        && Boolean.TRUE.toString().equals(currentRequest.get(Constants.Params.BACKGROUND))) {
                    continue;
                }
                relevantRequests.add(currentRequest);
            }
        }

        return relevantRequests;
    }

    protected static String jsonEncodeRequests(List<Map<String, Object>> requestData) {
        Map<String, Object> data = new HashMap<>();
        data.put(Constants.Params.DATA, requestData);
        return JsonConverter.toJson(data);
    }


    private static String getSizeAsString(int bytes) {
        if (bytes < (1 << 10)) {
            return bytes + " B";
        } else if (bytes < (1 << 20)) {
            return (bytes >> 10) + " KB";
        } else {
            return (bytes >> 20) + " MB";
        }
    }

    public int numResponses(JSONObject response) {
        if (response == null) {
            return 0;
        }
        try {
            return response.getJSONArray("response").length();
        } catch (JSONException e) {
            Log.e("Could not parse JSON response.", e);
            return 0;
        }
    }

    public JSONObject getResponseAt(JSONObject response, int index) {
        Leanplum.countAggregator().incrementCount("get_response_at");
        try {
            return response.getJSONArray("response").getJSONObject(index);
        } catch (JSONException e) {
            Log.e("Could not parse JSON response.", e);
            return null;
        }
    }

    public JSONObject getLastResponse(JSONObject response) {
        int numResponses = numResponses(response);
        Leanplum.countAggregator().incrementCount("get_last_response");
        if (numResponses > 0) {
            return getResponseAt(response, numResponses - 1);
        } else {
            return null;
        }
    }

    public boolean isResponseSuccess(JSONObject response) {
        Leanplum.countAggregator().incrementCount("is_response_success");
        if (response == null) {
            return false;
        }
        try {
            return response.getBoolean("success");
        } catch (JSONException e) {
            Log.e("Could not parse JSON response.", e);
            return false;
        }
    }

    public String getResponseError(JSONObject response) {
        Leanplum.countAggregator().incrementCount("get_response_error");
        if (response == null) {
            return null;
        }
        try {
            JSONObject error = response.optJSONObject("error");
            if (error == null) {
                return null;
            }
            return error.getString("message");
        } catch (JSONException e) {
            Log.e("Could not parse JSON response.", e);
            return null;
        }
    }
}

