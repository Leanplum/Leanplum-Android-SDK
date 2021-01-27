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
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.leanplum.PushTracking.DeliveryChannel;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Log;
import com.xiaomi.mipush.sdk.ErrorCode;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/**
 * This class encapsulates functionality for handling notification messages and registration ID from
 * MiPush. Needs to be called from your instance of {@link PushMessageReceiver}.
 */
public class LeanplumMiPushHandler {

  static String APP_ID;
  static String APP_KEY;

  public static void setApplication(String miAppId, String miAppKey) {
    APP_ID = miAppId;
    APP_KEY = miAppKey;
  }

  /**
   * Receives data message (pass-through message).
   */
  public void onReceivePassThroughMessage(Context context, MiPushMessage message) {
    if (context == null || message == null)
      return;

    Log.d("Received MiPush data message %s: %s", message.getMessageId(), getContentLog(message));
  }

  /**
   * Handle opening of notification message.
   */
  public void onNotificationMessageClicked(Context context, MiPushMessage message) {
    if (context == null || message == null) {
      return;
    }

    Log.d("MiPush notification clicked %s: %s", message.getMessageId(), getContentLog(message));

    Map<String, String> messageMap = parsePayload(message.getContent());
    if (messageMap.containsKey(Constants.Keys.PUSH_MESSAGE_TEXT)) {
      Bundle notification = createBundle(messageMap);
      PushTracking.appendDeliveryChannel(notification, DeliveryChannel.MIPUSH);
      LeanplumPushService.openNotification(context, notification);
    }
  }

  /**
   * Call when notification message is received.
   */
  public void onNotificationMessageArrived(Context context, MiPushMessage message) {
    if (message == null)
      return;

    Log.d("Received MiPush notification message %s: %s",
        message.getMessageId(), getContentLog(message));

    Map<String, String> messageMap = parsePayload(message.getContent());
    if (messageMap.containsKey(Constants.Keys.PUSH_MESSAGE_TEXT)) {
      Bundle notification = createBundle(messageMap);
      if (LeanplumPushService.shouldMuteNotification(notification)) {
        // note that tracking of "Push Delivered" metric happens on server side
        MiPushClient.clearNotification(context, message.getNotifyId());
      }
    }
  }

  /**
   * Handles result of command.
   */
  public void onCommandResult(Context context, MiPushCommandMessage message) {
    // no implementation
  }

  /**
   * Receives registration ID.
   */
  public void onReceiveRegisterResult(Context context, MiPushCommandMessage message) {
    if (message != null && MiPushClient.COMMAND_REGISTER.equals(message.getCommand())) {
      if (message.getResultCode() == ErrorCode.SUCCESS) {
        List<String> args = message.getCommandArguments();
        if (args != null && args.size() > 0) {
          String registrationId = args.get(0);

          LeanplumPushService.getPushProviders().setRegistrationId(
              PushProviderType.MIPUSH, registrationId);
        }
      }
    }
  }

  @VisibleForTesting
  Map<String, String> parsePayload(String payload) {
    Map<String, String> message = new HashMap<>();
    if (TextUtils.isEmpty(payload)) {
      return message;
    }

    try {
      JSONObject json = new JSONObject(payload);
      Iterator<String> it = json.keys();
      while (it.hasNext()) {
        String key = it.next();
        Object val = json.get(key);
        message.put(key, val.toString());
      }
    } catch (Throwable t) {
      Log.e("Error parsing MiPush payload: " + payload, t);
    }
    return message;
  }

  private Bundle createBundle(Map<String, String> messageMap) {
    Bundle bundle = new Bundle();
    if (messageMap != null) {
      for (Map.Entry<String, String> entry : messageMap.entrySet()) {
        bundle.putString(entry.getKey(), entry.getValue());
      }
    }
    return bundle;
  }

  private String getContentLog(@NonNull MiPushMessage message) {
    String content = message.getContent();
    if (!TextUtils.isEmpty(content)) {
      return content;
    }
    return "(empty content)";
  }
}
