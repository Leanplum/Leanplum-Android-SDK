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
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.leanplum.internal.Constants;
import com.leanplum.internal.JsonConverter;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

import java.util.Date;
import java.util.HashMap;
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

    //send the new token to backend
    LeanplumPushService.getCloudMessagingProvider().onRegistrationIdReceived(this.getApplicationContext(), token);
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
      if (Leanplum.getContext() == null) {
        Leanplum.setApplicationContext(this);
      }
      Map<String, String> messageMap = remoteMessage.getData();
      if (isDuplicateNotification(messageMap)) {
        return;
      }
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

  private Boolean isDuplicateNotification(Map<String, String> messageMap) {
    Boolean isDuplicate = false;
    String messageId = "";
    if (messageMap.containsKey("_lpn")) {
      messageId = messageMap.get("_lpn");
    }
    if (messageMap.containsKey("_lpm")) {
      messageId = messageMap.get("_lpm");
    }

    Map<String, Object> handledNotifications = retrieveHandledNotifications();
    handledNotifications = purgeExpiredHandledNotifications(handledNotifications);
    if (handledNotifications.containsKey(messageId)) {
      Long timeNotificationShown = (Long) handledNotifications.get(messageId);
      if (new Date().getTime() - timeNotificationShown < 3600000) { //milliseconds
        isDuplicate = true;
      }
    } else {
      handledNotifications.put(messageId, new Date().getTime());
    }
    persistHandledNotifications(handledNotifications);
    return isDuplicate;
  }

  private Map<String, Object> retrieveHandledNotifications() {
    SharedPreferences preferences = this.getSharedPreferences(Constants.Defaults.LEANPLUM,
            Context.MODE_PRIVATE);
    String jsonString = preferences.getString(handledNotificationsKey(), null);
    Map<String, Object> handledNotifications;
    if (jsonString != null) {
      handledNotifications = JsonConverter.fromJson(jsonString);
    } else {
      handledNotifications = new HashMap<>();
    }
    return handledNotifications;
  }

  private void persistHandledNotifications(Map<String, Object> handledNotifications) {
    SharedPreferences.Editor editor = this.getSharedPreferences(Constants.Defaults.LEANPLUM,
            Context.MODE_PRIVATE).edit();
    String jsonString = JsonConverter.toJson(handledNotifications);
    editor.putString(handledNotificationsKey(), jsonString).apply();
  }

  private Map<String, Object> purgeExpiredHandledNotifications(Map<String, Object> handledNotifications) {
    Long now = new Date().getTime();
    for (String messageId : handledNotifications.keySet()) {
      Long timeNotificationShown = (Long) handledNotifications.get(messageId);
      if (now - timeNotificationShown > 3600000) { //milliseconds
        handledNotifications.remove(messageId);
      }
    }
    return handledNotifications;
  }

  private String handledNotificationsKey() {
    return "LeanplumPushFirebaseMessagingService_handledNotifications";
  }
}
