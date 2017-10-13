/*
 * Copyright 2017, Leanplum, Inc. All rights reserved.
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
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.leanplum.internal.Constants;
import com.leanplum.internal.JsonConverter;
import com.leanplum.internal.Log;
import com.leanplum.utils.BuildUtil;

import java.util.Map;

/**
 * LeanplumNotificationHelper helper class for push notifications.
 *
 * @author Anna Orlova
 */
class LeanplumNotificationHelper {

  private static final int BIGPICTURE_TEXT_TOP_PEDDING = -14;
  private static final int BIGPICTURE_TEXT_SIZE = 14;

  /**
   * If notification channels are supported this method will try to create
   * NotificationCompat.Builder with default notification channel if default channel id is provided.
   * If notification channels not supported this method will return NotificationCompat.Builder for
   * context.
   *
   * @param context The application context.
   * @param isNotificationChannelSupported True if notification channels are supported.
   * @return NotificationCompat.Builder for provided context or null.
   */
  // NotificationCompat.Builder(Context context) constructor was deprecated in API level 26.
  @SuppressWarnings("deprecation")
  static NotificationCompat.Builder getDefaultCompatNotificationBuilder(Context context,
      boolean isNotificationChannelSupported) {
    if (!isNotificationChannelSupported) {
      return new NotificationCompat.Builder(context);
    }
    String channelId = LeanplumNotificationChannel.getDefaultNotificationChannelId(context);
    if (!TextUtils.isEmpty(channelId)) {
      return new NotificationCompat.Builder(context, channelId);
    } else {
      Log.w("Failed to post notification, there are no notification channels configured.");
      return null;
    }
  }

  /**
   * If notification channels are supported this method will try to create
   * Notification.Builder with default notification channel if default channel id is provided.
   * If notification channels not supported this method will return Notification.Builder for
   * context.
   *
   * @param context The application context.
   * @param isNotificationChannelSupported True if notification channels are supported.
   * @return Notification.Builder for provided context or null.
   */
  // Notification.Builder(Context context) constructor was deprecated in API level 26.
  @SuppressWarnings("deprecation")
  private static Notification.Builder getDefaultNotificationBuilder(Context context,
      boolean isNotificationChannelSupported) {
    if (!isNotificationChannelSupported) {
      return new Notification.Builder(context);
    }
    String channelId = LeanplumNotificationChannel.getDefaultNotificationChannelId(context);
    if (!TextUtils.isEmpty(channelId)) {
      return new Notification.Builder(context, channelId);
    } else {
      Log.w("Failed to post notification, there are no notification channels configured.");
      return null;
    }
  }

  /**
   * If notification channels are supported this method will try to create a channel with
   * information from the message if it doesn't exist and return NotificationCompat.Builder for this
   * channel. In the case where no channel information inside the message, we will try to get a
   * channel with default channel id. If notification channels not supported this method will return
   * NotificationCompat.Builder for context.
   *
   * @param context The application context.
   * @param message Push notification Bundle.
   * @return NotificationCompat.Builder or null.
   */
  // NotificationCompat.Builder(Context context) constructor was deprecated in API level 26.
  @SuppressWarnings("deprecation")
  static NotificationCompat.Builder getNotificationCompatBuilder(Context context, Bundle message) {
    NotificationCompat.Builder builder = null;
    // If we are targeting API 26, try to find supplied channel to post notification.
    if (BuildUtil.isNotificationChannelSupported(context)) {
      try {
        String channel = message.getString("lp_channel");
        if (!TextUtils.isEmpty(channel)) {
          // Create channel if it doesn't exist and post notification to that channel.
          Map<String, Object> channelDetails = JsonConverter.fromJson(channel);
          String channelId = LeanplumNotificationChannel.createNotificationChannel(context,
              channelDetails);
          if (!TextUtils.isEmpty(channelId)) {
            builder = new NotificationCompat.Builder(context, channelId);
          } else {
            Log.w("Failed to post notification to specified channel.");
          }
        } else {
          // If channel isn't supplied, try to look up for default channel.
          builder = LeanplumNotificationHelper.getDefaultCompatNotificationBuilder(context, true);
        }
      } catch (Exception e) {
        Log.e("Failed to post notification to specified channel.");
      }
    } else {
      builder = new NotificationCompat.Builder(context);
    }
    return builder;
  }

  /**
   * If notification channels are supported this method will try to create a channel with
   * information from the message if it doesn't exist and return Notification.Builder for this
   * channel. In the case where no channel information inside the message, we will try to get a
   * channel with default channel id. If notification channels not supported this method will return
   * Notification.Builder for context.
   *
   * @param context The application context.
   * @param message Push notification Bundle.
   * @return Notification.Builder or null.
   */
  static Notification.Builder getNotificationBuilder(Context context, Bundle message) {
    Notification.Builder builder = null;
    // If we are targeting API 26, try to find supplied channel to post notification.
    if (BuildUtil.isNotificationChannelSupported(context)) {
      try {
        String channel = message.getString("lp_channel");
        if (!TextUtils.isEmpty(channel)) {
          // Create channel if it doesn't exist and post notification to that channel.
          Map<String, Object> channelDetails = JsonConverter.fromJson(channel);
          String channelId = LeanplumNotificationChannel.createNotificationChannel(context,
              channelDetails);
          if (!TextUtils.isEmpty(channelId)) {
            builder = new Notification.Builder(context, channelId);
          } else {
            Log.w("Failed to post notification to specified channel.");
          }
        } else {
          // If channel isn't supplied, try to look up for default channel.
          builder = LeanplumNotificationHelper.getDefaultNotificationBuilder(context, true);
        }
      } catch (Exception e) {
        Log.e("Failed to post notification to specified channel.");
      }
    } else {
      builder = new Notification.Builder(context);
    }
    return builder;
  }

  /**
   * Gets Notification.Builder with 2 lines at BigPictureStyle notification text.
   *
   * @param context The application context.
   * @param message Push notification Bundle.
   * @param contentIntent PendingIntent.
   * @param title String with title for push notification.
   * @param messageText String with text for push notification.
   * @param bigPicture Bitmap for BigPictureStyle notification.
   * @return Notification.Builder or null.
   */
  static Notification.Builder getNotificationBuilder(Context context, Bundle message,
      PendingIntent contentIntent, String title, final String messageText, Bitmap bigPicture) {
    if (Build.VERSION.SDK_INT < 16) {
      return null;
    }
    Notification.Builder notificationBuilder =
        getNotificationBuilder(context, message);
    notificationBuilder.setSmallIcon(context.getApplicationInfo().icon)
        .setContentTitle(title)
        .setContentText(messageText);
    Notification.BigPictureStyle bigPictureStyle = new Notification.BigPictureStyle() {
      @Override
      protected RemoteViews getStandardView(int layoutId) {
        RemoteViews remoteViews = super.getStandardView(layoutId);
        // Modifications of stanxdard push RemoteView.
        try {
          int id = Resources.getSystem().getIdentifier("text", "id", "android");
          remoteViews.setBoolean(id, "setSingleLine", false);
          remoteViews.setInt(id, "setLines", 2);
          if (Build.VERSION.SDK_INT < 23) {
            // Make text smaller.
            remoteViews.setViewPadding(id, 0, BIGPICTURE_TEXT_TOP_PEDDING, 0, 0);
            remoteViews.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, BIGPICTURE_TEXT_SIZE);
          }
        } catch (Throwable throwable) {
          Log.e("Cannot modify push notification layout.");
        }
        return remoteViews;
      }
    };

    bigPictureStyle.bigPicture(bigPicture)
        .setBigContentTitle(title)
        .setSummaryText(message.getString(Constants.Keys.PUSH_MESSAGE_TEXT));
    notificationBuilder.setStyle(bigPictureStyle);

    if (Build.VERSION.SDK_INT >= 24) {
      // By default we cannot reach getStandardView method on API>=24. I we call
      // createBigContentView, Android will call getStandardView method and we can get
      // modified RemoteView.
      try {
        RemoteViews remoteView = notificationBuilder.createBigContentView();
        if (remoteView != null) {
          // We need to set received RemoteView as a custom big content view.
          notificationBuilder.setCustomBigContentView(remoteView);
        }
      } catch (Throwable t) {
        Log.e("Cannot modify push notification layout.", t);
      }
    }

    notificationBuilder.setAutoCancel(true);
    notificationBuilder.setContentIntent(contentIntent);
    return notificationBuilder;
  }
}
