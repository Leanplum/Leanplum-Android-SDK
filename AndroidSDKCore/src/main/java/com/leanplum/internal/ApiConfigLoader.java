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
import android.text.TextUtils;

/**
 * Loads appId, accessKey and type of environment from the Android string resources.
 */
public class ApiConfigLoader {
  private static final String STRING_APPID = "leanplum_app_id";
  private static final String STRING_PROD_KEY = "leanplum_prod_key";
  private static final String STRING_DEV_KEY = "leanplum_dev_key";
  private static final String STRING_ENV = "leanplum_environment";

  private static final String ENV_DEV = "development";
  private static final String ENV_PROD = "production";

  private Context context;

  @FunctionalInterface
  public interface KeyListener {
    void onKeysLoaded(String appId, String accessKey);
  }

  public ApiConfigLoader(Context context) {
    this.context = context;
  }

  private String getStringResource(String key) {
    try {
      String packageName = context.getPackageName();
      int id = context.getResources().getIdentifier(key, "string", packageName);
      return context.getString(id);
    } catch (Throwable t) { // Resources.NotFoundException
      Log.e("Cannot load string for key = %s. Message = %s", key, t.getMessage());
      return null;
    }
  }

  public void loadFromResources(KeyListener prodKeyListener, KeyListener devKeyListener) {
    String appId = getStringResource(STRING_APPID);
    String prodKey = getStringResource(STRING_PROD_KEY);
    String devKey = getStringResource(STRING_DEV_KEY);
    String environment = getStringResource(STRING_ENV);

    if (TextUtils.isEmpty(appId))
      return;

    boolean devEnv = ENV_DEV.equals(environment);
    boolean hasProdKey = !TextUtils.isEmpty(prodKey);
    boolean hasDevKey = !TextUtils.isEmpty(devKey);

    if (devEnv && hasDevKey) {
      devKeyListener.onKeysLoaded(appId, devKey);
      Log.i("Using appId and accessKey from Android resources for development environment.");
    }
    else if (!devEnv && hasProdKey) {
      prodKeyListener.onKeysLoaded(appId, prodKey);
      Log.i("Using appId and accessKey from Android resources for production environment.");
    }
  }
}
