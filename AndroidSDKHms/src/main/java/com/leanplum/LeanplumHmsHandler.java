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
import android.widget.Toast;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Constants.Keys;
import com.leanplum.internal.Log;
import java.util.Map;

/**
 * This class encapsulates functionality for handling notification messages and registration ID from
 * Huawei Push Kit. Needs to be called from your instance of {@link HmsMessageService}.
 */
public class LeanplumHmsHandler {

  public void onCreate(Context context) {
    Leanplum.setApplicationContext(context);
  }

  public void onNewToken(String token, Context context) {
    Toast.makeText(context, "Push token = " + token, Toast.LENGTH_SHORT).show();
    Log.e("HMS Token = " + token);

    // TODO call provider
  }

  public void onMessageReceived(RemoteMessage remoteMessage, Context context) {
    try {
      Map<String, String> messageMap = remoteMessage.getDataOfMap();

      String channel = PushTracking.CHANNEL_HMS;

      if (messageMap.containsKey(Constants.Keys.PUSH_MESSAGE_TEXT)) {
        Bundle notification = getBundle(messageMap);
        notification.putString(Keys.CHANNEL_INTERNAL_KEY, channel); // TODO is it necessary
        LeanplumPushService.handleNotification(context, notification); // TODO uncomment
      }
      Log.d("Received HMS notification message: %s", messageMap.toString());
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

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
