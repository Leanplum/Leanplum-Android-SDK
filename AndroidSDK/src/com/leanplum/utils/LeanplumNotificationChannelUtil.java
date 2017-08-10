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

package com.leanplum.utils;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.leanplum.Leanplum;
import com.leanplum.internal.Log;

import java.util.List;

/**
 * Push notification channels manipulation utilities. Please use this class for Android O and upper
 * with targetSdkVersion 26 and above.
 *
 * @author Anna Orlova
 */
public class LeanplumNotificationChannelUtil {
  private static int targetSdk = -1;

  static {
    getTargetSdkVersion(Leanplum.getContext());
  }

  /**
   * Create push notification channel with provided id, name and importance of the channel.
   * You can call this method also when you need to update the name of a channel, at this case you
   * should use original channel id.
   *
   * @param context The application context.
   * @param channelId The id of the channel.
   * @param channelName The user-visible name of the channel.
   * @param channelImportance The importance of the channel. Use value from 0 to 5. 3 is default.
   * Read more https://developer.android.com/reference/android/app/NotificationManager.html#IMPORTANCE_DEFAULT
   * Once you create a notification channel, only the system can modify its importance.
   */
  public static void createNotificationChannel(Context context, String channelId, String
      channelName, int channelImportance) {
    createNotificationChannel(context, channelId, channelName, channelImportance, null, null, false,
        0, false, null);
  }

  /**
   * Create push notification channel with provided id, name, description and default importance(3)
   * of the channel. You can call this method also when you need to update the name or description
   * of a channel, at this case you should use original channel id.
   *
   * @param context The application context.
   * @param channelId The id of the channel.
   * @param channelName The user-visible name of the channel.
   * @param channelDescription The user-visible description of the channel.
   */
  public static void createNotificationChannel(Context context, String channelId, String
      channelName, String channelDescription) {
    createNotificationChannel(context, channelId, channelName,
        NotificationManager.IMPORTANCE_DEFAULT, channelDescription, null, false, 0, false, null);
  }

  /**
   * Create push notification channel with provided id, name and importance of the channel.
   * You can call this method also when you need to update the name or description of a channel, at
   * this case you should use original channel id.
   *
   * @param context The application context.
   * @param channelId The id of the channel.
   * @param channelName The user-visible name of the channel.
   * @param channelImportance The importance of the channel. Use value from 0 to 5. 3 is default.
   * Read more https://developer.android.com/reference/android/app/NotificationManager.html#IMPORTANCE_DEFAULT
   * Once you create a notification channel, only the system can modify its importance.
   * @param channelDescription The user-visible description of the channel.
   */
  public static void createNotificationChannel(Context context, String channelId, String
      channelName, int channelImportance, String channelDescription) {
    createNotificationChannel(context, channelId, channelName, channelImportance,
        channelDescription, null, false, 0, false, null);
  }

  /**
   * Create push notification channel with provided id, name and importance of the channel.
   * You can call this method also when you need to update the name or description of a channel, at
   * this case you should use original channel id.
   *
   * @param context The application context.
   * @param channelId The id of the channel.
   * @param channelName The user-visible name of the channel.
   * @param channelImportance The importance of the channel. Use value from 0 to 5. 3 is default.
   * Read more https://developer.android.com/reference/android/app/NotificationManager.html#IMPORTANCE_DEFAULT
   * Once you create a notification channel, only the system can modify its importance.
   * @param channelDescription The user-visible description of the channel.
   * @param groupId The id of push notification channel group.
   */
  public static void createNotificationChannel(Context context, String channelId, String
      channelName, int channelImportance, String channelDescription, String groupId) {
    createNotificationChannel(context, channelId, channelName, channelImportance,
        channelDescription, groupId, false, 0, false, null);
  }

  /**
   * Create push notification channel with provided id, name and importance of the channel.
   * You can call this method also when you need to update the name or description of a channel, at
   * this case you should use original channel id.
   *
   * @param context The application context.
   * @param channelId The id of the channel.
   * @param channelName The user-visible name of the channel.
   * @param channelImportance The importance of the channel. Use value from 0 to 5. 3 is default.
   * Read more https://developer.android.com/reference/android/app/NotificationManager.html#IMPORTANCE_DEFAULT
   * Once you create a notification channel, only the system can modify its importance.
   * @param channelDescription The user-visible description of the channel.
   * @param groupId The id of push notification channel group.
   * @param enableLights True if lights enable for this channel.
   * @param lightColor Light color for notifications posted to this channel, if the device supports
   * this feature.
   * @param enableVibration True if vibration enable for this channel.
   * @param vibrationPattern Vibration pattern for notifications posted to this channel.
   */
  public static void createNotificationChannel(Context context, String channelId, String
      channelName, int channelImportance, String channelDescription, String groupId, boolean
      enableLights, int lightColor, boolean enableVibration, long[] vibrationPattern) {
    if (context == null || TextUtils.isEmpty(channelId)) {
      return;
    }

    if (Build.VERSION.SDK_INT >= 26 && currentTargetSdk(context) >= 26) {
      try {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
          Log.e("Notification manager is null");
          return;
        }

        NotificationChannel notificationChannel = new NotificationChannel(channelId,
            channelName, channelImportance);
        if (!TextUtils.isEmpty(channelDescription)) {
          notificationChannel.setDescription(channelDescription);
        }
        if (enableLights) {
          notificationChannel.enableLights(true);
          notificationChannel.setLightColor(lightColor);
        }
        if (enableVibration) {
          notificationChannel.enableVibration(true);
          notificationChannel.setVibrationPattern(vibrationPattern);
        }
        if (!TextUtils.isEmpty(groupId)) {
          notificationChannel.setGroup(groupId);
        }
        notificationManager.createNotificationChannel(notificationChannel);
      } catch (Throwable t) {
        Log.e("Cannot create notification channel.", t);
      }
    }
  }

  /**
   * Delete push notification channel.
   *
   * @param context The application context.
   * @param channelId The id of the channel.
   */
  public static void deleteNotificationChannel(Context context, String channelId) {
    if (context == null) {
      return;
    }
    if (Build.VERSION.SDK_INT >= 26 && currentTargetSdk(context) >= 26) {
      try {
        NotificationManager notificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
          Log.e("Notification manager is null");
          return;
        }

        notificationManager.deleteNotificationChannel(channelId);
      } catch (Throwable t) {
        Log.e("Cannot delete notification channel.", t);
      }
    }
  }

  /**
   * Create push notification channel group.
   *
   * @param context The application context.
   * @param groupId The id of the group.
   * @param groupName The user-visible name of the group.
   */
  public static void createNotificationGroup(Context context, String groupId, String groupName) {
    if (context == null || TextUtils.isEmpty(groupId)) {
      return;
    }
    if (Build.VERSION.SDK_INT >= 26 && currentTargetSdk(context) >= 26) {
      try {
        NotificationManager notificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
          Log.e("Notification manager is null");
          return;
        }

        notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(groupId,
            groupName));
      } catch (Throwable t) {
        Log.e("Cannot create notification channel group.", t);
      }
    }
  }

  /**
   * Delete push notification channel group.
   *
   * @param context The application context.
   * @param groupId The id of the channel.
   */
  public static void deleteNotificationChannelGroup(Context context, String groupId) {
    if (context == null || TextUtils.isEmpty(groupId)) {
      return;
    }
    if (Build.VERSION.SDK_INT >= 26 && currentTargetSdk(context) >= 26) {
      try {
        NotificationManager notificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
          Log.e("Notification manager is null");
          return;
        }

        notificationManager.deleteNotificationChannelGroup(groupId);
      } catch (Throwable t) {
        Log.e("Cannot delete notification channel.", t);
      }
    }
  }

  /**
   * Check if channel with channelId exists.
   *
   * @param context The application context.
   * @param channelId The id of the channel.
   * @return True if channel with channelId exists.
   */
  public static boolean isNotificationChannelExists(Context context, String channelId) {
    if (Build.VERSION.SDK_INT >= 26 && currentTargetSdk(context) >= 26) {
      NotificationManager notificationManager = (NotificationManager)
          context.getSystemService(Context.NOTIFICATION_SERVICE);
      if (notificationManager != null) {
        return notificationManager.getNotificationChannel(channelId) != null;
      }
    }
    return false;
  }

  /**
   * Get list of NotificationChannel.
   *
   * @param context The application context.
   * @return Returns all notification channels belonging to the calling package.
   */
  public static List<NotificationChannel> getNotificationChannels(Context context) {
    if (Build.VERSION.SDK_INT >= 26 && currentTargetSdk(context) >= 26) {
      NotificationManager notificationManager =
          (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      if (notificationManager == null) {
        Log.e("Notification manager is null");
        return null;
      }
      return notificationManager.getNotificationChannels();
    }
    return null;
  }

  /**
   * Get targetSdkVersion from application info.
   *
   * @param context The application context.
   */
  private static void getTargetSdkVersion(Context context) {
    if (context != null) {
      targetSdk = context.getApplicationInfo().targetSdkVersion;
    }
  }

  // Return current target SDK version.
  public static int currentTargetSdk(Context context) {
    if (targetSdk == -1) {
      getTargetSdkVersion(context);
    }
    return targetSdk;
  }
}
