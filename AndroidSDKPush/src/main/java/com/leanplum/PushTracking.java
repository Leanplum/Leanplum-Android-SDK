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
import com.leanplum.internal.Constants;
import com.leanplum.internal.Constants.Keys;
import com.leanplum.internal.Log;
import java.util.HashMap;
import java.util.Map;

public class PushTracking {

  static final String CHANNEL_FCM = "FCM";
  static final String CHANNEL_FCM_SILENT_TRACK = "FCM_SILENT_TRACK";
  static final String CHANNEL_HMS = "HMS";

  static boolean isFcmSilentPush(@NonNull Bundle message) {
    String channel = message.getString(Keys.CHANNEL_INTERNAL_KEY);
    return CHANNEL_FCM_SILENT_TRACK.equals(channel);
  }

  public static void trackDelivery(@NonNull Context context, @NonNull Bundle message) {
    if (!Leanplum.isPushDeliveryTrackingEnabled()) {
      Log.d("Push delivery tracking is disabled for " + LeanplumPushService.getMessageId(message));
      return;
    }

    Map<String,String> properties = new HashMap<>();
    properties.put(Keys.PUSH_METRIC_MESSAGE_ID, LeanplumPushService.getMessageId(message));

    String occurrenceId = message.getString(Keys.PUSH_OCCURRENCE_ID);
    if (!TextUtils.isEmpty(occurrenceId)) {
      properties.put(Keys.PUSH_METRIC_OCCURRENCE_ID, occurrenceId);
    }

    String sentTime = message.getString(Keys.PUSH_SENT_TIME);
    if (!TextUtils.isEmpty(sentTime)) {
      properties.put(Keys.PUSH_METRIC_SENT_TIME, sentTime);
    }

    String channel = message.getString(Keys.CHANNEL_INTERNAL_KEY);
    if (!TextUtils.isEmpty(channel)) {
      properties.put(Keys.PUSH_METRIC_CHANNEL, channel);
    }

    boolean notificationsEnabled =
        LeanplumNotificationHelper.areNotificationsEnabled(context, message);
    properties.put(Keys.PUSH_METRIC_NOTIFICATIONS_ENABLED, Boolean.toString(notificationsEnabled));

    Leanplum.track(Constants.PUSH_DELIVERED_EVENT_NAME, properties);
  }

  public static void trackOpen(@NonNull Bundle message) {
    Map<String,String> properties = new HashMap<>();
    properties.put(Keys.PUSH_METRIC_MESSAGE_ID, LeanplumPushService.getMessageId(message));

    String occurrenceId = message.getString(Keys.PUSH_OCCURRENCE_ID);
    if (!TextUtils.isEmpty(occurrenceId)) {
      properties.put(Keys.PUSH_METRIC_OCCURRENCE_ID, occurrenceId);
    }

    String sentTime = message.getString(Keys.PUSH_SENT_TIME);
    if (!TextUtils.isEmpty(sentTime)) {
      properties.put(Keys.PUSH_METRIC_SENT_TIME, sentTime);
    }

    String channel = message.getString(Keys.CHANNEL_INTERNAL_KEY);
    if (!TextUtils.isEmpty(channel)) {
      properties.put(Keys.PUSH_METRIC_CHANNEL, channel);
    }

    Leanplum.track(Constants.PUSH_OPENED_EVENT_NAME, properties);
  }

}
