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

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.leanplum.Leanplum;
import com.leanplum.internal.Request.RequestType;
import com.leanplum.internal.http.NetworkOperation;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class RequestSender {

  private static RequestSender INSTANCE = new RequestSender();

  private final LeanplumEventCallbackManager eventCallbackManager =
      new LeanplumEventCallbackManager();

  private final RequestBatchFactory batchFactory = new RequestBatchFactory();
  private final RequestUuidHelper uuidHelper = new RequestUuidHelper();

  private final List<Map<String, Object>> localErrors = new ArrayList<>();

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

  private boolean handleDatabaseError(Request request) {
    if (LeanplumEventDataManager.sharedInstance().hasDatabaseError()) {
      if (RequestBuilder.ACTION_LOG.equals(request.getApiAction())) {
        // intercepting 'action=log' requests created from Log.exception
        addLocalError(request);
      }
      return true;
    }
    return false;
  }

  /**
   * Saves requests into database synchronously.
   */
  private void saveRequest(Request request) {
    if (handleDatabaseError(request)) {
      // do not save request on database error
      return;
    }

    Map<String, Object> args = createArgsDictionary(request);

    try {
      if (!uuidHelper.attachUuid(args)) {
        return;
      }
      LeanplumEventDataManager.sharedInstance().insertEvent(JsonConverter.toJson(args));

      // Checks if here response and/or error callback for this request. We need to add callbacks to
      // eventCallbackManager only if here was internet connection, otherwise triggerErrorCallback
      // will handle error callback for this event.
      if (request.response != null || (request.error != null && !Util.isConnected())) {
        eventCallbackManager.addCallbacks(request, request.response, request.error);
      }

    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  private RequestBatch createNextBatch() {
    // Check if we have localErrors, if yes then we will send only errors to the server.
    if (localErrors.size() > 0)
      return batchFactory.createErrorBatch(localErrors);
    else
      return batchFactory.createNextBatch();
  }

  @VisibleForTesting
  public void sendRequests() {
    Leanplum.countAggregator().sendAllCounts();

    RequestBatch batch = createNextBatch();

    if (batch.isEmpty()) {
      return;
    }

    final Map<String, Object> multiRequestArgs = new HashMap<>();
    if (!APIConfig.getInstance().attachApiKeys(multiRequestArgs)) {
      return;
    }
    multiRequestArgs.put(Constants.Params.DATA, batch.getJson());
    multiRequestArgs.put(Constants.Params.SDK_VERSION, Constants.LEANPLUM_VERSION);
    multiRequestArgs.put(Constants.Params.ACTION, RequestBuilder.ACTION_MULTI);
    multiRequestArgs.put(Constants.Params.TIME, Double.toString(new Date().getTime() / 1000.0));

    NetworkOperation op = null;
    try {
      try {
        op = new NetworkOperation(
            Constants.API_HOST_NAME,
            Constants.API_SERVLET,
            multiRequestArgs,
            RequestBuilder.POST,
            Constants.API_SSL,
            Constants.NETWORK_TIMEOUT_SECONDS);

        JSONObject responseBody = op.getJsonResponse();
        int statusCode = op.getResponseCode();

        if (statusCode >= 200 && statusCode <= 299) {
          // Parse response body and trigger callbacks
          invokeCallbacks(responseBody);

          // Clear localErrors list.
          localErrors.clear();
          batchFactory.deleteFinishedBatch(batch);

          // Send another batch if the last batch had maximum events per api call.
          if (batch.isFull()) {
            sendRequests();
          }
        } else {
          Exception errorException = new Exception("HTTP error " + statusCode);
          if (statusCode != -1 && statusCode != 408 && !(statusCode >= 500 && statusCode <= 599)) {
            batchFactory.deleteFinishedBatch(batch);
          }
          invokeCallbacksWithError(errorException);
        }
      } catch (JSONException e) {
        Log.e("Error parsing JSON response: " + e.toString() + "\n" + Log.getStackTraceString(e));
        batchFactory.deleteFinishedBatch(batch);
        invokeCallbacksWithError(e);
      } catch (Exception e) {
        Log.e("Unable to send request: " + e.toString() + "\n" + Log.getStackTraceString(e));
        invokeCallbacksWithError(e);
      } finally {
        if (op != null) {
          op.disconnect();
        }
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  @VisibleForTesting
  protected void invokeCallbacks(@NonNull JSONObject responseBody) {
    eventCallbackManager.invokeCallbacks(responseBody);
  }

  private void invokeCallbacksWithError(@NonNull Exception exception) {
    eventCallbackManager.invokeAllCallbacksWithError(exception);
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

  public void send(@NonNull final Request request) {
    OperationQueue.sharedInstance().addOperation(new Runnable() {
      @Override
      public void run() {
        sendSync(request);
      }
    });
  }

  /**
   * Saves the request and sends all saved requests synchronously.
   */
  private void sendSync(@NonNull Request request) {
    if (Constants.isTestMode) {
      return;
    }

    saveRequest(request);

    if (Constants.isDevelopmentModeEnabled || RequestType.IMMEDIATE.equals(request.getType())) {
      try {
        if (validateConfig(request)) {
          sendRequests();
        }
      } catch (Throwable t) {
        Log.exception(t);
      }
    }
  }

  private boolean validateConfig(@NonNull Request request) {
    if (APIConfig.getInstance().appId() == null) {
      Log.e("Cannot send request. appId is not set.");
      return false;
    }

    if (APIConfig.getInstance().accessKey() == null) {
      Log.e("Cannot send request. accessKey is not set.");
      return false;
    }

    if (!Util.isConnected()) {
      Log.d("Device is offline, will try sending requests again later.");
      if (request.error != null) {
        request.error.error(new Exception("Leanplum: device is offline"));
      }
      return false;
    }

    return true;
  }

}
