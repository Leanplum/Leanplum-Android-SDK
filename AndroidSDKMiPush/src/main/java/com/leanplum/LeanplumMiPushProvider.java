/*
 * Copyright 2021, Leanplum, Inc. All rights reserved.
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

package com.leanplum;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.leanplum.internal.Constants.Defaults;
import com.leanplum.internal.Log;
import com.xiaomi.mipush.sdk.MiPushClient;

/**
 * Leanplum provider for work with Xiaomi MiPush.
 * Class is instantiated by reflection using default constructor.
 */
class LeanplumMiPushProvider extends LeanplumCloudMessagingProvider {

  /**
   * Constructor called by reflection.
   */
  public LeanplumMiPushProvider() {
    registerApp(Leanplum.getContext());
  }

  private void registerApp(@NonNull Context context) {
    String miAppId = LeanplumMiPushHandler.MI_APP_ID;
    String miAppKey = LeanplumMiPushHandler.MI_APP_KEY;

    if (TextUtils.isEmpty(miAppId) || TextUtils.isEmpty(miAppKey)) {
      Log.e("You need to provide appId and appKey for MiPush to work."
          + "Call LeanplumMiPushHandler.setApplication(miAppId, miAppKey) before Leanplum.start()");
      return;
    }

    Log.d("Calling MiPushClient.registerPush");
    MiPushClient.registerPush(context, miAppId, miAppKey);
  }

  @Override
  protected String getSharedPrefsPropertyName() {
    return Defaults.PROPERTY_MIPUSH_TOKEN_ID;
  }

  @Override
  public PushProviderType getType() {
    return PushProviderType.MIPUSH;
  }

  @Override
  public void unregister() {
    if (Leanplum.getContext() == null)
      return;

    MiPushClient.unregisterPush(Leanplum.getContext());
  }

  @Override
  public void updateRegistrationId() {
    if (Leanplum.getContext() == null)
      return;

    String regId = MiPushClient.getRegId(Leanplum.getContext());
    if (!TextUtils.isEmpty(regId)) {
      setRegistrationId(regId);
    }
  }
}
