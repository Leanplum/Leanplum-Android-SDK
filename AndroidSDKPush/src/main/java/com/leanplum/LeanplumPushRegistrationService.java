/*
 * Copyright 2016, Leanplum, Inc. All rights reserved.
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

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

import com.leanplum.internal.Log;

/**
 * Registration service that handles registration with the GCM and FCM, using
 * InstanceID.
 *
 * @author Aleksandar Gyorev
 */
public class LeanplumPushRegistrationService extends IntentService {

  public LeanplumPushRegistrationService() {
    super("LeanplumPushRegistrationService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    try {
      LeanplumCloudMessagingProvider provider = LeanplumPushService.getCloudMessagingProvider();
      if (provider == null) {
        Log.e("Failed to complete registration token refresh.");
        return;
      }
      //Here for FCM, its no longer a syncronous call.
      String registrationId = provider.getRegistrationId();
      if (!TextUtils.isEmpty(registrationId)) {
        provider.onRegistrationIdReceived(getApplicationContext(), registrationId);
      }
    } catch (Throwable t) {
      Log.e("Failed to complete registration token refresh.", t);
    }
  }
}
