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
import android.widget.Toast;
import com.leanplum.PushTracking.DeliveryChannel;
import com.leanplum.internal.Log;
import com.leanplum.internal.OperationQueue;
import com.xiaomi.mipush.sdk.ErrorCode;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

// TODO javadoc as in fcm
public class LeanplumMiPushHandler {

  static String APP_ID;
  static String APP_KEY;

  public static void setApplication(String miAppId, String miAppKey) {
    APP_ID = miAppId;
    APP_KEY = miAppKey;
  }

  public void onReceivePassThroughMessage(Context context, MiPushMessage message) {
    // Handle data message
    toastAndLog(context, "onReceivePassThroughMessage: id=" + message.getMessageId() + " content=" + message.getContent() + " notifyId="+message.getNotifyId());
  }

  public void onNotificationMessageClicked(Context context, MiPushMessage message) {
    // Handle notification message
    if (context == null || message == null) {
      return;
    }
    toastAndLog(context, "onNotificationMessageClicked = " + message.getMessageId() + " content="+message.getContent() + " notifyId="+message.getNotifyId());

    Map<String, String> messageMap = parsePayload(message.getContent());
    Bundle notification = createBundle(messageMap);
    PushTracking.appendDeliveryChannel(notification, DeliveryChannel.MIPUSH);
    LeanplumPushService.openNotification(context, notification);
  }

  public void onNotificationMessageArrived(Context context, MiPushMessage message) {
    //MiPushClient.clearNotification(context, message.getNotifyId()); TODO clean notification if app is in foreground and "mute inside app" is set
    toastAndLog(context, "onNotificationMessageArrived: id=" + message.getMessageId() + " content=" + message.getContent() + " notifyId="+message.getNotifyId());
  }

  public void onCommandResult(Context context, MiPushCommandMessage message) {
    toastAndLog(context, "onCommandResult = " + message.getCommand());
  }

  public void onReceiveRegisterResult(Context context, MiPushCommandMessage message) {
    if (message != null && MiPushClient.COMMAND_REGISTER.equals(message.getCommand())) {
      if (message.getResultCode() == ErrorCode.SUCCESS) {
        List<String> args = message.getCommandArguments();
        if (args != null && args.size() > 0) {
          String registrationId = args.get(0);

          LeanplumPushService.getPushProviders()
              .setRegistrationId(PushProviderType.MIPUSH, registrationId);
          toastAndLog(context, "regid = " + registrationId);
        }
      }
    }

    toastAndLog(context, "onReceiveRegisterResult = " + message.getCommand());
  }

  private void toastAndLog(Context context, String message) { // TODO remove logging
    OperationQueue.sharedInstance().addUiOperation(
        () -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    android.util.Log.e("Xiaomi", message);
  }


  private Map<String, String> parsePayload(String payload) {
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
      Log.e("Error parsing MiPush payload=" + payload, t);
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
}
