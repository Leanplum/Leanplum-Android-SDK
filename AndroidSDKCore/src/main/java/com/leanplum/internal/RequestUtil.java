package com.leanplum.internal;

import android.text.TextUtils;
import com.leanplum.Leanplum;
import com.leanplum.internal.Constants.Params;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RequestUtil {

    /**
     * Get response json object for request Id
     *
     * @param response response body
     * @param reqId request id
     * @return JSONObject for specified request id.
     */
    public static JSONObject getResponseForId(JSONObject response, String reqId) {
        try {
            JSONArray jsonArray = response.getJSONArray(Constants.Params.RESPONSE);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (jsonObject != null) {
                        String requestId = jsonObject.getString(Constants.Params.REQUEST_ID);
                        if (reqId.equalsIgnoreCase(requestId)) {
                            return jsonObject;
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("Could not get response for id: " + reqId, e);
            return null;
        }
        return null;
    }

    /**
     * Checks whether particular response is successful or not
     *
     * @param response JSONObject to check
     * @return true if successful, false otherwise
     */
    public static boolean isResponseSuccess(JSONObject response) {
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

    /**
     * Get response error from JSONObject
     *
     * @param response JSONObject to get error from
     * @return request error
     */
    public static String getResponseError(JSONObject response) {
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

    /**
     * Parse error message from server response and return readable error message.
     *
     * @param errorMessage String of error from server response.
     * @return String of readable error message.
     */
    public static String getReadableErrorMessage(String errorMessage) {
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

    /**
     * JSON with API config looks like:
     * {
     * 	"response": [{
     * 		"success": false,
     * 		"devServerHost": "dev.leanplum.com",
     * 		"apiHost": "api.leanplum.com",
     * 		"apiPath": "api",
     * 		"error": { "message": "App endpoint configuration mismatch." }
     *  }]
     * }
     *
     * @param responseBody The JSON response.
     * @return True if API was updated, false otherwise.
     */
    public static boolean updateApiConfig(JSONObject responseBody) {
        try {
            if (!responseBody.isNull(Constants.Params.RESPONSE)) {
                JSONArray responseArray = responseBody.getJSONArray(Constants.Params.RESPONSE);
                for (int i = 0; i < responseArray.length(); i++) {
                    JSONObject item = responseArray.getJSONObject(i);
                    if (isResponseSuccess(item)) {
                        continue;
                    }
                    if (item != null) {
                        String apiHost = item.optString(Params.API_HOST);
                        String apiPath = item.optString(Params.API_PATH);
                        String devServerHost = item.optString(Params.DEV_SERVER_HOST);
                        // Prevent setting the same API config and request retry loop
                        boolean configUpdated = false;

                        boolean hasNewApiHost = !TextUtils.isEmpty(apiHost)
                            && !apiHost.equals(APIConfig.getInstance().getApiHost());
                        boolean hasNewApiPath = !TextUtils.isEmpty(apiPath)
                            && !apiPath.equals(APIConfig.getInstance().getApiPath());
                        boolean hasNewSocketHost = !TextUtils.isEmpty(devServerHost)
                            && !devServerHost.equals(APIConfig.getInstance().getSocketHost());

                        // API config
                        if (hasNewApiHost || hasNewApiPath) {
                            configUpdated = true;
                            if (TextUtils.isEmpty(apiHost)) {
                                apiHost = APIConfig.getInstance().getApiHost();
                            }
                            if (TextUtils.isEmpty(apiPath)) {
                                apiPath = APIConfig.getInstance().getApiPath();
                            }
                            boolean ssl = APIConfig.getInstance().getApiSSL();
                            Log.d("Changing API endpoint to " + apiHost + "/" + apiPath);
                            Leanplum.setApiConnectionSettings(apiHost, apiPath, ssl);
                        }

                        // Socket config
                        if (hasNewSocketHost) {
                            configUpdated = true;
                            int socketPort = APIConfig.getInstance().getSocketPort();
                            Log.d("Changing socket to " + devServerHost + ":" + socketPort);
                            Leanplum.setSocketConnectionSettings(devServerHost, socketPort);
                        }

                        return configUpdated;
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("Error parsing response for API config", e);
        }
        return false;
    }
}
