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
public class RequestOld {

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
    if (RequestBuilder.ACTION_LOG.equals(apiMethod) && LeanplumEventDataManager.sharedInstance().willSendErrorLogs()) {
      RequestSender.getInstance().addLocalError(this);
    }
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
