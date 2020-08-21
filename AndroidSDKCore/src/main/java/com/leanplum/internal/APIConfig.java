/*
 * Copyright 2018, Leanplum, Inc. All rights reserved.
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
import androidx.annotation.VisibleForTesting;
import com.leanplum.Leanplum;
import com.leanplum.utils.SharedPreferencesUtil;
import java.util.Map;

public class APIConfig {
  private static final APIConfig INSTANCE = new APIConfig();

  private String appId;
  private String accessKey;
  private String deviceId;
  private String userId;
  // The token is saved primarily for legacy SharedPreferences decryption. This could
  // likely be removed in the future.
  private String token;

  @VisibleForTesting
  APIConfig() {
  }

  public static APIConfig getInstance() {
    return INSTANCE;
  }

  public void setAppId(String appId, String accessKey) {
    if (!TextUtils.isEmpty(appId)) {
      this.appId = appId.trim();
    }
    if (!TextUtils.isEmpty(accessKey)) {
      this.accessKey = accessKey.trim();
    }
  }

  public void loadToken(String token) {
    this.token = token;
  }

  public String appId() {
    return appId;
  }

  public String accessKey() {
    return accessKey;
  }

  public String deviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public String userId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String token() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public void loadToken() {
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
        Constants.Defaults.LEANPLUM, Context.MODE_PRIVATE);
    String token = defaults.getString(Constants.Defaults.TOKEN_KEY, null);
    if (token == null) {
      return;
    }
    setToken(token);
  }

  public void saveToken() {
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
        Constants.Defaults.LEANPLUM, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = defaults.edit();
    editor.putString(Constants.Defaults.TOKEN_KEY, APIConfig.getInstance().token());
    SharedPreferencesUtil.commitChanges(editor);
  }

  public boolean attachApiKeys(Map<String, Object> dict) {
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
}
