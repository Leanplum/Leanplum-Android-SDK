/*
 * Copyright 2020, Leanplum, Inc. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.leanplum.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.VisibleForTesting;
import com.leanplum.Leanplum;
import com.leanplum.utils.SharedPreferencesUtil;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

public class RequestSender {

  private static RequestSender INSTANCE = new RequestSender();

  private static final long DEVELOPMENT_MIN_DELAY_MS = 100;
  private static final long DEVELOPMENT_MAX_DELAY_MS = 5000;
  private static final long PRODUCTION_DELAY = 60000;
  static final int MAX_EVENTS_PER_API_CALL;

  static {
    if (Build.VERSION.SDK_INT <= 17) {
      MAX_EVENTS_PER_API_CALL = 5000;
    } else {
      MAX_EVENTS_PER_API_CALL = 10000;
    }
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

  private final LeanplumEventCallbackManager eventCallbackManager =
      new LeanplumEventCallbackManager();

  private List<Map<String, Object>> localErrors = new ArrayList<>();
  private long lastSendTimeMs;

  @VisibleForTesting
  public RequestSender() {
  }

  public static RequestSender getInstance() {
    return INSTANCE;
  }

  @VisibleForTesting
  public static void setInstance(RequestSender instance) {
    INSTANCE = instance;
  }

  /**
   * Saves requests into database.
   * Saving will be executed on background thread serially.
   * @param args json to save.
   */
  private void saveRequestForLater(final Request request, final Map<String, Object> args) {
    OperationQueue.sharedInstance().addOperation(new Runnable() {
      @Override
      public void run() {
        try {
          Context context = Leanplum.getContext();
          if (context == null) {
            return;
          }

          SharedPreferences preferences = context.getSharedPreferences(
              Constants.Defaults.LEANPLUM, Context.MODE_PRIVATE);
          SharedPreferences.Editor editor = preferences.edit();
          long count = LeanplumEventDataManager.sharedInstance().getEventsCount();
          String uuid = preferences.getString(Constants.Defaults.UUID_KEY, null);
          if (uuid == null || count % MAX_EVENTS_PER_API_CALL == 0) {
            uuid = UUID.randomUUID().toString();
            editor.putString(Constants.Defaults.UUID_KEY, uuid);
            SharedPreferencesUtil.commitChanges(editor);
          }
          args.put(Constants.Params.UUID, uuid);
          LeanplumEventDataManager.sharedInstance().insertEvent(JsonConverter.toJson(args));

          // Checks if here response and/or error callback for this request. We need to add callbacks to
          // eventCallbackManager only if here was internet connection, otherwise triggerErrorCallback
          // will handle error callback for this event.
          if (request.response != null || request.error != null && !Util.isConnected()) {
            eventCallbackManager.addCallbacks(request, request.response, request.error);
          }

        } catch (Throwable t) {
          Log.exception(t);
        }
      }
    });
  }

  public void send(final Request request) {
    sendEventually(request);

    if (Constants.isDevelopmentModeEnabled) {
      long currentTimeMs = System.currentTimeMillis();
      long delayMs;
      if (lastSendTimeMs == 0 || currentTimeMs - lastSendTimeMs > DEVELOPMENT_MAX_DELAY_MS) {
        delayMs = DEVELOPMENT_MIN_DELAY_MS;
      } else {
        delayMs = (lastSendTimeMs + DEVELOPMENT_MAX_DELAY_MS) - currentTimeMs;
      }
      OperationQueue.sharedInstance().addOperationAfterDelay(new Runnable() {
        @Override
        public void run() {
          try {
            sendIfConnected(request);
          } catch (Throwable t) {
            Log.exception(t);
          }
        }
      }, delayMs);
    }

    Leanplum.countAggregator().incrementCount("send_request");
  }

  public void sendEventually(Request request) {
    if (Constants.isTestMode) {
      return;
    }

    if (LeanplumEventDataManager.sharedInstance().willSendErrorLogs()) {
      return;
    }

    if (!request.isSent()) {
      request.setSent(true);

      // Check if it's error and there was SQLite exception.
      if (RequestBuilder.ACTION_LOG.equals(request.getApiAction())
          && LeanplumEventDataManager.sharedInstance().willSendErrorLogs()) {
        addLocalError(request);
      }

      Map<String, Object> args = createArgsDictionary(request);
      saveRequestForLater(request, args);
    }
    Leanplum.countAggregator().incrementCount("send_eventually");
  }

  public void sendIfConnected(Request request) {
    if (Util.isConnected()) {
      sendNow(request);
    } else {
      sendEventually(request);
      Log.i("Device is offline, will send later");
    }
    Leanplum.countAggregator().incrementCount("send_if_connected");
  }

  private void sendNow(final Request request) {
    if (Constants.isTestMode) {
      return;
    }

    // always save request first
    sendEventually(request);

    // in case appId and accessKey are set later, request is already saved and will be
    // sent when variables are set.
    if (APIConfig.getInstance().appId() == null) {
      Log.e("Cannot send request. appId is not set.");
      return;
    }
    if (APIConfig.getInstance().accessKey() == null) {
      Log.e("Cannot send request. accessKey is not set.");
      return;
    }

    Leanplum.countAggregator().incrementCount("send_now");

    // Try to send all saved requests.
    OperationQueue.sharedInstance().addOperation(new Runnable() {
      @Override
      public void run() {
        try {
          sendRequests();
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
    });
  }

  protected static String jsonEncodeRequests(List<Map<String, Object>> requestData) {
    Map<String, Object> data = new HashMap<>();
    data.put(Constants.Params.DATA, requestData);
    return JsonConverter.toJson(data);
  }

  private RequestsWithEncoding getRequestsWithEncodedStringForErrors() {
    List<Map<String, Object>> unsentRequests = new ArrayList<>();
    List<Map<String, Object>> requestsToSend;
    String jsonEncodedRequestsToSend;

    String uuid = UUID.randomUUID().toString();
    for (Map<String, Object> error : localErrors) {
      error.put(Constants.Params.UUID, uuid);
      unsentRequests.add(error);
    }
    requestsToSend = unsentRequests;
    jsonEncodedRequestsToSend = jsonEncodeRequests(unsentRequests);

    RequestsWithEncoding requestsWithEncoding = new RequestsWithEncoding();
    // for errors, we send all unsent requests so they are identical
    requestsWithEncoding.unsentRequests = unsentRequests;
    requestsWithEncoding.requestsToSend = requestsToSend;
    requestsWithEncoding.jsonEncodedString = jsonEncodedRequestsToSend;

    return requestsWithEncoding;
  }

  protected RequestsWithEncoding getRequestsWithEncodedStringStoredRequests(double fraction) {
    try {
      List<Map<String, Object>> unsentRequests;
      List<Map<String, Object>> requestsToSend;
      String jsonEncodedRequestsToSend;
      RequestsWithEncoding requestsWithEncoding = new RequestsWithEncoding();

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

  private RequestsWithEncoding getRequestsWithEncodedString() {
    RequestsWithEncoding requestsWithEncoding;
    // Check if we have localErrors, if yes then we will send only errors to the server.
    if (localErrors.size() != 0) {
      requestsWithEncoding = getRequestsWithEncodedStringForErrors();
    } else {
      requestsWithEncoding = getRequestsWithEncodedStringStoredRequests(1.0);
    }

    return requestsWithEncoding;
  }

  public void sendRequests() {
    Leanplum.countAggregator().sendAllCounts();

    RequestsWithEncoding requestsWithEncoding = getRequestsWithEncodedString();

    List<Map<String, Object>> unsentRequests = requestsWithEncoding.unsentRequests;
    List<Map<String, Object>> requestsToSend = requestsWithEncoding.requestsToSend;
    String jsonEncodedString = requestsWithEncoding.jsonEncodedString;

    if (requestsToSend.isEmpty()) {
      return;
    }

    final Map<String, Object> multiRequestArgs = new HashMap<>();
    if (!APIConfig.getInstance().attachApiKeys(multiRequestArgs)) {
      return;
    }

    multiRequestArgs.put(Constants.Params.DATA, jsonEncodedString);
    multiRequestArgs.put(Constants.Params.SDK_VERSION, Constants.LEANPLUM_VERSION);
    multiRequestArgs.put(Constants.Params.ACTION, RequestBuilder.ACTION_MULTI);
    multiRequestArgs.put(Constants.Params.TIME, Double.toString(new Date().getTime() / 1000.0));

    HttpURLConnection op = null;
    try {
      try {
        op = Util.operation(
            Constants.API_HOST_NAME,
            Constants.API_SERVLET,
            multiRequestArgs,
            RequestBuilder.POST,
            Constants.API_SSL,
            Constants.NETWORK_TIMEOUT_SECONDS);

        JSONObject responseBody = Util.getJsonResponse(op);
        int statusCode = op.getResponseCode();

        if (statusCode >= 200 && statusCode <= 299) {
          // Parse response body and trigger callbacks
          triggerCallbackManager(responseBody, null);

          // Clear localErrors list.
          localErrors.clear();
          deleteSentRequests(unsentRequests.size());

          // Send another request if the last request had maximum events per api call.
          if (unsentRequests.size() == MAX_EVENTS_PER_API_CALL) {
            sendRequests();
          }
        } else {
          Exception errorException = new Exception("HTTP error " + statusCode);
          if (statusCode != -1 && statusCode != 408 && !(statusCode >= 500 && statusCode <= 599)) {
            deleteSentRequests(unsentRequests.size());
          }
          triggerCallbackManager(responseBody, errorException);
        }
      } catch (JSONException e) {
        Log.e("Error parsing JSON response: " + e.toString() + "\n" + Log.getStackTraceString(e));
        deleteSentRequests(unsentRequests.size());
        triggerCallbackManager(null, e);
      } catch (Exception e) {
        Log.e("Unable to send request: " + e.toString() + "\n" + Log.getStackTraceString(e));
        triggerCallbackManager(null, e);
      } finally {
        if (op != null) {
          op.disconnect();
        }
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  public List<Map<String, Object>> getUnsentRequests(double fraction) {
    List<Map<String, Object>> requestData;

    synchronized (Request.class) {
      lastSendTimeMs = System.currentTimeMillis();
      Context context = Leanplum.getContext();
      SharedPreferences preferences = context.getSharedPreferences(
          Constants.Defaults.LEANPLUM, Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = preferences.edit();
      int count = (int) (fraction * MAX_EVENTS_PER_API_CALL);
      requestData = LeanplumEventDataManager.sharedInstance().getEvents(count);
      editor.remove(Constants.Defaults.UUID_KEY);
      SharedPreferencesUtil.commitChanges(editor);
      // if we send less than 100% of requests, we need to reset the batch
      // UUID for the next batch
      if (fraction < 1) {
        RequestUtil.setNewBatchUUID(requestData);
      }
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
            && RequestBuilder.ACTION_START.equals(requestData.get(i + 1).get(Constants.Params.ACTION))
            && RequestBuilder.ACTION_START.equals(currentRequest.get(Constants.Params.ACTION))
            && Boolean.TRUE.toString().equals(currentRequest.get(Constants.Params.BACKGROUND))) {
          continue;
        }
        relevantRequests.add(currentRequest);
      }
    }

    return relevantRequests;
  }

  /**
   * Parse response body from server.  Invoke potential error or response callbacks for all events
   * of this request.
   *
   * @param responseBody JSONObject with response body from server.
   * @param error Exception.
   */
  protected void triggerCallbackManager(JSONObject responseBody, Exception error) {
    synchronized (Request.class) {
      if (responseBody == null && error != null) {
        // Invoke potential error callbacks for all events of this request.
        eventCallbackManager.invokeAllCallbacksWithError(error);
        return;
      } else if (responseBody == null) {
        return;
      }
      eventCallbackManager.invokeCallbacks(responseBody);
    }
  }

  void deleteSentRequests(int requestsCount) {
    if (requestsCount == 0) {
      return;
    }
    synchronized (Request.class) {
      LeanplumEventDataManager.sharedInstance().deleteEvents(requestsCount);
    }
  }

  /**
   * Wait 1 second for potential other API calls, and then sends the call synchronously if no other
   * call has been sent within 1 minute.
   */
  public void sendIfDelayed(final Request request) {
    sendEventually(request);
    OperationQueue.sharedInstance().addOperationAfterDelay(new Runnable() {
      @Override
      public void run() {
        try {
          sendIfDelayedHelper(request);
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
    }, 1000);
    Leanplum.countAggregator().incrementCount("send_if_delayed");
  }

  /**
   * Sends the call synchronously if no other call has been sent within 1 minute.
   */
  private void sendIfDelayedHelper(Request request) {
    if (Constants.isDevelopmentModeEnabled) {
      send(request);
    } else {
      long currentTimeMs = System.currentTimeMillis();
      if (lastSendTimeMs == 0 || currentTimeMs - lastSendTimeMs > PRODUCTION_DELAY) {
        sendIfConnected(request);
      }
    }
  }

  private void addLocalError(Request request) {
    Map<String, Object> dict = createArgsDictionary(request);
    localErrors.add(dict);
  }

  static Map<String, Object> createArgsDictionary(Request request) {
    Map<String, Object> args = new HashMap<>();
    args.put(Constants.Params.DEVICE_ID, APIConfig.getInstance().deviceId());
    args.put(Constants.Params.USER_ID, APIConfig.getInstance().userId());
    args.put(Constants.Params.ACTION, request.getApiAction());
    args.put(Constants.Params.SDK_VERSION, Constants.LEANPLUM_VERSION);
    args.put(Constants.Params.DEV_MODE, Boolean.toString(Constants.isDevelopmentModeEnabled));
    args.put(Constants.Params.TIME, Double.toString(new Date().getTime() / 1000.0));
    args.put(Constants.Params.REQUEST_ID, request.getRequestId());
    String token = APIConfig.getInstance().token();
    if (token != null) {
      args.put(Constants.Params.TOKEN, token);
    }
    args.putAll(request.getParams());
    return args;
  }
}
