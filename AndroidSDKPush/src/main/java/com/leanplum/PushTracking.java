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
import androidx.annotation.NonNull;
import com.leanplum.internal.Constants.Keys;
import java.util.HashMap;
import java.util.Map;

class PushTracking {

  enum DeliveryChannel {
    FCM,
    FCM_SILENT_TRACK,
    MIPUSH
  }

  static void appendDeliveryChannel(
      @NonNull Bundle message,
      @NonNull DeliveryChannel channel) {
    message.putSerializable(Keys.CHANNEL_INTERNAL_KEY, channel);
  }

  static DeliveryChannel getDeliveryChannel(@NonNull Bundle message) {
    return (DeliveryChannel)message.getSerializable(Keys.CHANNEL_INTERNAL_KEY);
  }

  static boolean isFcmSilentPush(@NonNull Bundle message) {
    DeliveryChannel channel = getDeliveryChannel(message);
    return DeliveryChannel.FCM_SILENT_TRACK.equals(channel);
  }

  static void trackDelivery(@NonNull Bundle message) {
    Map<String,String> properties = new HashMap<>();
    properties.put("messageID", LeanplumPushService.getMessageId(message));

    DeliveryChannel channel = getDeliveryChannel(message);
    if (channel != null) {
      properties.put(Keys.PUSH_MESSAGE_DELIVERY_CHANNEL, channel.name());
    }
    Leanplum.track("Push Delivered", properties);
  }

  static void trackOpen(@NonNull Bundle message) {
    Map<String,String> properties = new HashMap<>();
    properties.put("messageID", LeanplumPushService.getMessageId(message));

    DeliveryChannel channel = getDeliveryChannel(message);
    if (channel != null) {
      properties.put(Keys.PUSH_MESSAGE_DELIVERY_CHANNEL, channel.name());
    }
    Leanplum.track("Push Opened", properties);
  }

}
