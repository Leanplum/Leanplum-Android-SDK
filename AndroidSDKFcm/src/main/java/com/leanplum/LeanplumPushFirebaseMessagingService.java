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

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

import java.util.Map;

/**
 * FCM listener service, which enables handling messages on the app's behalf.
 *
 * @author Anna Orlova
 */
@SuppressLint("Registered")
public class LeanplumPushFirebaseMessagingService extends FirebaseMessagingService {

  @Override
  public void onNewToken(String token) {
    super.onNewToken(token);

    LeanplumPushService.setCloudMessagingProvider(new LeanplumFcmProvider());
    LeanplumPushService.getCloudMessagingProvider().storePreferences(this.getApplicationContext(), token);
    try {
      if (Build.VERSION.SDK_INT < 26) {
        LeanplumNotificationHelper.startPushRegistrationService(this, "FCM");
      } else {
        LeanplumNotificationHelper.scheduleJobService(this,
            LeanplumFcmRegistrationJobService.class, LeanplumFcmRegistrationJobService.JOB_ID);
      }
    } catch (Throwable t) {
      Log.e("Failed to update FCM token.", t);
    }
  }

  /**
   * Called when a message is received. This is also called when a notification message is received
   * while the app is in the foreground.
   *
   * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
   */
  @Override
  public void onMessageReceived(RemoteMessage remoteMessage) {
    try {
      Map<String, String> messageMap = remoteMessage.getData();
      if (messageMap.containsKey(Constants.Keys.PUSH_MESSAGE_TEXT)) {
        LeanplumPushService.handleNotification(this, getBundle(messageMap));
      }
      Log.i("Received: " + messageMap.toString());
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * @param messageMap {@link RemoteMessage}'s data map.
   */
  private Bundle getBundle(Map<String, String> messageMap) {
    Bundle bundle = new Bundle();
    if (messageMap != null) {
      for (Map.Entry<String, String> entry : messageMap.entrySet()) {
        bundle.putString(entry.getKey(), entry.getValue());
      }
    }
    return bundle;
  }
}
