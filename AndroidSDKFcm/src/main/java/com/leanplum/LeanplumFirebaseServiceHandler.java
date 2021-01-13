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

package com.leanplum;

import android.content.Context;
import android.os.Bundle;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Log;
import java.util.Map;

/**
 * This class encapsulates functionality for handling data messages from FCM. Needs to be called
 * from your instance of {@link FirebaseMessagingService}.
 */
public final class LeanplumFirebaseServiceHandler {

  /**
   * Call from your implementation of {@link FirebaseMessagingService#onCreate()}
   */
  public void onCreate(Context context) {
    Leanplum.setApplicationContext(context);
  }

  /**
   * Call from your implementation of {@link FirebaseMessagingService#onNewToken(String)}
   */
  public void onNewToken(String token, Context context) {
    //send the new token to backend
    LeanplumPushService.getPushProviders().setRegistrationId(PushProviderType.FCM, token);
  }

  /**
   * Call from your implementation of
   * {@link FirebaseMessagingService#onMessageReceived(RemoteMessage)}
   */
  public void onMessageReceived(RemoteMessage remoteMessage, Context context) {
    try {
      Map<String, String> messageMap = remoteMessage.getData();
      if (messageMap.containsKey(Constants.Keys.PUSH_MESSAGE_TEXT)) {
        LeanplumPushService.handleNotification(context, getBundle(messageMap));
      }
      Log.d("Received push notification message: %s", messageMap.toString());
    } catch (Throwable t) {
      Log.exception(t);
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
