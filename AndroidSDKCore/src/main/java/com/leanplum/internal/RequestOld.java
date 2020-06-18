/*
 * Copyright 2013, Leanplum, Inc. All rights reserved.
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
import android.text.TextUtils;

import com.leanplum.Leanplum;
import com.leanplum.utils.SharedPreferencesUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Leanplum request class.
 *
 * @author Andrew First
 */
public class RequestOld implements Requesting {

  static final String LEANPLUM = "__leanplum__";
  static final String UUID_KEY = "uuid";

  private static String appId;
  private static String accessKey;
  private static String deviceId;
  private static String userId;
  private String requestId = UUID.randomUUID().toString();

  // The token is saved primarily for legacy SharedPreferences decryption. This could
  // likely be removed in the future.
  private static String token = null;

  private final String httpMethod;
  private final String apiMethod;
  private final Map<String, Object> params;
  ResponseCallback response;
  ErrorCallback error;
  private boolean saved;

  public static void setAppId(String appId, String accessKey) {
    if (!TextUtils.isEmpty(appId)) {
      RequestOld.appId = appId.trim();
    }
    if (!TextUtils.isEmpty(accessKey)) {
      RequestOld.accessKey = accessKey.trim();
    }
    Leanplum.countAggregator().incrementCount("set_app_id");
  }

  public static void setDeviceId(String deviceId) {
    RequestOld.deviceId = deviceId;
  }

  public static void setUserId(String userId) {
    RequestOld.userId = userId;
  }

  public static void setToken(String token) {
    RequestOld.token = token;
    Leanplum.countAggregator().incrementCount("set_token");
  }

  public static String token() {
    return token;
  }

  public static void loadToken() {
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

  public static void saveToken() {
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
        LEANPLUM, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = defaults.edit();
    editor.putString(Constants.Defaults.TOKEN_KEY, RequestOld.token());
    SharedPreferencesUtil.commitChanges(editor);
  }

  public String requestId() {
    return requestId;
  }

  public static String appId() {
    return appId;
  }

  public static String accessKey() {
    return accessKey;
  }

  public static String deviceId() {
    return deviceId;
  }

  public static String userId() {
    return RequestOld.userId;
  }

  public RequestOld(String httpMethod, String apiMethod, Map<String, Object> params) {
    this.httpMethod = httpMethod;
    this.apiMethod = apiMethod;
    this.params = params != null ? params : new HashMap<String, Object>();
    // Check if it is error and here was SQLite exception.
    if (Constants.Methods.LOG.equals(apiMethod) && LeanplumEventDataManager.sharedInstance().willSendErrorLogs()) {
      RequestSender.getInstance().addLocalError(this);
    }
  }

  public static RequestOld get(String apiMethod, Map<String, Object> params) {
    Log.LeanplumLogType level = Constants.Methods.LOG.equals(apiMethod) ?
        Log.LeanplumLogType.DEBUG : Log.LeanplumLogType.VERBOSE;
    Log.log(level, "Will call API method " + apiMethod + " with arguments " + params);
    Leanplum.countAggregator().incrementCount("get_request");
    return RequestFactory.getInstance().createRequest("GET", apiMethod, params);
  }

  public static RequestOld post(String apiMethod, Map<String, Object> params) {
    Log.LeanplumLogType level = Constants.Methods.LOG.equals(apiMethod) ?
        Log.LeanplumLogType.DEBUG : Log.LeanplumLogType.VERBOSE;
    Log.log(level, "Will call API method " + apiMethod + " with arguments " + params);
    Leanplum.countAggregator().incrementCount("post_request");
    return RequestFactory.getInstance().createRequest("POST", apiMethod, params);
  }

  public void onResponse(ResponseCallback response) {
    this.response = response;
    Leanplum.countAggregator().incrementCount("on_response");
  }

  public void onError(ErrorCallback error) {
    this.error = error;
    Leanplum.countAggregator().incrementCount("on_error");
  }

  public Map<String, Object> createArgsDictionary() {
    Map<String, Object> args = new HashMap<>();
    args.put(Constants.Params.DEVICE_ID, deviceId);
    args.put(Constants.Params.USER_ID, userId);
    args.put(Constants.Params.ACTION, apiMethod);
    args.put(Constants.Params.SDK_VERSION, Constants.LEANPLUM_VERSION);
    args.put(Constants.Params.DEV_MODE, Boolean.toString(Constants.isDevelopmentModeEnabled));
    args.put(Constants.Params.TIME, Double.toString(new Date().getTime() / 1000.0));
    args.put(Constants.Params.REQUEST_ID, requestId);
    if (token != null) {
      args.put(Constants.Params.TOKEN, token);
    }
    args.putAll(params);
    return args;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  static boolean attachApiKeys(Map<String, Object> dict) {
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

  public interface ResponseCallback {
    void response(JSONObject response);
  }

  public interface ErrorCallback {
    void error(Exception e);
  }

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
      Log.e("Could not get response for id: ", reqId, e);
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

  /**
   * Get response error from JSONObject
   *
   * @param response JSONObject to get error from
   * @return request error
   */
  public static String getResponseError(JSONObject response) {
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

  public void setSaved(boolean saved) {
    this.saved = saved;
  }

  public boolean isSaved() {
    return saved;
  }

  public String getHttpMethod() {
    return httpMethod;
  }
}
