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

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.leanplum.internal.Constants.Keys;
import java.util.HashMap;
import java.util.Map;

class PushTracking {

  static final String CHANNEL_FCM = "FCM";
  static final String CHANNEL_FCM_SILENT_TRACK = "FCM_SILENT_TRACK";
  static final String CHANNEL_MIPUSH = "MIPUSH";

  static boolean isFcmSilentPush(@NonNull Bundle message) {
    String channel = message.getString(Keys.CHANNEL_INTERNAL_KEY);
    return CHANNEL_FCM_SILENT_TRACK.equals(channel);
  }

  static void trackDelivery(@NonNull Bundle message) {
    Map<String,String> properties = new HashMap<>();
    properties.put(Keys.PUSH_METRIC_MESSAGE_ID, LeanplumPushService.getMessageId(message));

    String sentTime = message.getString(Keys.PUSH_SENT_TIME);
    if (!TextUtils.isEmpty(sentTime)) {
      properties.put(Keys.PUSH_METRIC_SENT_TIME, sentTime);
    }

    String channel = message.getString(Keys.CHANNEL_INTERNAL_KEY);
    if (!TextUtils.isEmpty(channel)) {
      properties.put(Keys.PUSH_METRIC_CHANNEL, channel);
    }

    Leanplum.track("Push Delivered", properties);
  }

  static void trackOpen(@NonNull Bundle message) {
    Map<String,String> properties = new HashMap<>();
    properties.put(Keys.PUSH_METRIC_MESSAGE_ID, LeanplumPushService.getMessageId(message));

    String sentTime = message.getString(Keys.PUSH_SENT_TIME);
    if (!TextUtils.isEmpty(sentTime)) {
      properties.put(Keys.PUSH_METRIC_SENT_TIME, sentTime);
    }

    String channel = message.getString(Keys.CHANNEL_INTERNAL_KEY);
    if (!TextUtils.isEmpty(channel)) {
      properties.put(Keys.PUSH_METRIC_CHANNEL, channel);
    }

    Leanplum.track("Push Opened", properties);
  }

}
