/*
 * Copyright 2018, Leanplum, Inc. All rights reserved.
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

import android.app.Notification;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

/**
 * Implement LeanplumNotificationBuilderCustomizer to customize the appearance of notifications if
 * you want to support 2 lines of text in BigPicture style push notification.
 *
 * @author Anna Orlova
 */

public interface LeanplumNotificationBuilderCustomizer extends LeanplumPushNotificationCustomizer{
  /**
   * Implement this method if you want to support 2 lines of text in BigPicture style push
   * notification and you already have a customizer. Note, that you still need to implement
   * {@link LeanplumPushNotificationCustomizer#customize(NotificationCompat.Builder, Bundle)}
   * to support BigText style on devices with API less than 16. Please call
   * {@link LeanplumPushService#setCustomizer(LeanplumNotificationBuilderCustomizer)}
   *
   * @param builder Notification.Builder for push notification.
   * @param notificationPayload Bundle notification payload.
   */
  void customize(Notification.Builder builder, Bundle notificationPayload);
}
