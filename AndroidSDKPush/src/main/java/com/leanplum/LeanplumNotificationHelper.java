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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.RemoteViews;

import androidx.core.app.NotificationManagerCompat;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Constants.Keys;
import com.leanplum.internal.JsonConverter;
import com.leanplum.internal.Log;
import com.leanplum.utils.BitmapUtil;
import com.leanplum.utils.BuildUtil;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

/**
 * LeanplumNotificationHelper helper class for push notifications.
 *
 * @author Anna Orlova
 */
class LeanplumNotificationHelper {

  private static final String LEANPLUM_DEFAULT_PUSH_ICON = "leanplum_default_push_icon";

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
      Log.e("Failed to post notification, there are no notification channels configured.");
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
  @TargetApi(26)
  private static Notification.Builder getDefaultNotificationBuilder(Context context,
      boolean isNotificationChannelSupported) {
    if (!isNotificationChannelSupported) {
      return new Notification.Builder(context);
    }
    String channelId = LeanplumNotificationChannel.getDefaultNotificationChannelId(context);
    if (!TextUtils.isEmpty(channelId)) {
      return new Notification.Builder(context, channelId);
    } else {
      Log.e("Failed to post notification, there are no notification channels configured.");
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
        String channel = message.getString(Keys.PUSH_NOTIFICATION_CHANNEL);
        if (!TextUtils.isEmpty(channel)) {
          // Create channel if it doesn't exist and post notification to that channel.
          Map<String, Object> channelDetails = JsonConverter.fromJson(channel);
          String channelId = LeanplumNotificationChannel.createNotificationChannel(context,
              channelDetails);
          if (!TextUtils.isEmpty(channelId)) {
            builder = new NotificationCompat.Builder(context, channelId);
          } else {
            Log.e("Failed to post notification to specified channel.");
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
  private static Notification.Builder getNotificationBuilder(Context context, Bundle message) {
    Notification.Builder builder = null;
    // If we are targeting API 26, try to find supplied channel to post notification.
    if (BuildUtil.isNotificationChannelSupported(context)) {
      try {
        String channel = message.getString(Keys.PUSH_NOTIFICATION_CHANNEL);
        if (!TextUtils.isEmpty(channel)) {
          // Create channel if it doesn't exist and post notification to that channel.
          Map<String, Object> channelDetails = JsonConverter.fromJson(channel);
          String channelId = LeanplumNotificationChannel.createNotificationChannel(context,
              channelDetails);
          if (!TextUtils.isEmpty(channelId)) {
            builder = new Notification.Builder(context, channelId);
          } else {
            Log.e("Failed to post notification to specified channel.");
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

  public static boolean areNotificationsEnabled(Context context, Bundle message) {
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
      // all notifications are turned off
      return false;
    }

    if (BuildUtil.isNotificationChannelSupported(context)) {
      String channelId = resolveChannelId(context, message);
      return !TextUtils.isEmpty(channelId) && isChannelEnabled(context, channelId);
    } else {
      return true;
    }
  }

  private static String resolveChannelId(Context context, Bundle message) {
    String channelJson = message.getString(Keys.PUSH_NOTIFICATION_CHANNEL);
    if (!TextUtils.isEmpty(channelJson)) {
      Map<String, Object> channelDetails = JsonConverter.fromJson(channelJson);
      String channelId = (String) channelDetails.get("id");
      if (!TextUtils.isEmpty(channelId)) {
        return channelId;
      }
    }

    // try default channel if other is not provided
    String defaultChannelId = LeanplumNotificationChannel.getDefaultNotificationChannelId(context);
    if (!TextUtils.isEmpty(defaultChannelId)) {
      return defaultChannelId;
    }

    return null;
  }

  @RequiresApi(api = VERSION_CODES.O)
  private static boolean isChannelEnabled(Context context, String channelId) {
      NotificationChannel channel =
          NotificationManagerCompat.from(context).getNotificationChannel(channelId);

      return channel != null &&
          channel.getImportance() > NotificationManager.IMPORTANCE_NONE;
  }

  /**
   * Gets NotificationCompat.Builder for provided parameters.
   *
   * @param context The application context.
   * @param message Push notification Bundle.
   * @param contentIntent PendingIntent.
   * @param title String with title for push notification.
   * @param messageText String with text for push notification.
   * @param bigPicture Bitmap for BigPictureStyle notification.
   * @param defaultNotificationIconResourceId int Resource id for default push notification icon.
   * @return NotificationCompat.Builder or null.
   */
  static NotificationCompat.Builder getNotificationCompatBuilder(Context context, Bundle message,
      PendingIntent contentIntent, String title, final String messageText, Bitmap bigPicture,
      int defaultNotificationIconResourceId) {
    if (message == null) {
      return null;
    }

    NotificationCompat.Builder notificationCompatBuilder =
        getNotificationCompatBuilder(context, message);

    if (notificationCompatBuilder == null) {
      return null;
    }

    if (defaultNotificationIconResourceId == 0) {
      notificationCompatBuilder.setSmallIcon(context.getApplicationInfo().icon);
    } else {
      notificationCompatBuilder.setSmallIcon(defaultNotificationIconResourceId);
    }

    notificationCompatBuilder.setContentTitle(title)
        .setStyle(new NotificationCompat.BigTextStyle()
            .bigText(messageText))
        .setContentText(messageText);

    if (bigPicture != null) {
      notificationCompatBuilder.setStyle(new NotificationCompat.BigPictureStyle()
          .bigPicture(bigPicture)
          .setBigContentTitle(title)
          .setSummaryText(messageText));
    }

    // Try to put a notification on top of the notification area. This method was deprecated in API
    // level 26. For API level 26 and above we must use setImportance(int) for each notification
    // channel, not for each notification message.
    if (Build.VERSION.SDK_INT >= 16 && !BuildUtil.isNotificationChannelSupported(context)) {
      //noinspection deprecation
      notificationCompatBuilder.setPriority(Notification.PRIORITY_MAX);
    }
    notificationCompatBuilder.setAutoCancel(true);
    notificationCompatBuilder.setContentIntent(contentIntent);

    return notificationCompatBuilder;
  }

  /**
   * Calls setStyle for notificationBuilder.
   *
   * @param notificationBuilder current Notification.Builder.
   * @param bigPictureStyle current Notification.BigPictureStyle.
   */
  static void setModifiedBigPictureStyle(Notification.Builder notificationBuilder,
      Notification.Style bigPictureStyle) {
    if (Build.VERSION.SDK_INT < 16 || notificationBuilder == null || bigPictureStyle == null) {
      return;
    }

    notificationBuilder.setStyle(bigPictureStyle);
  }

  /**
   * Gets Notification.BigPictureStyle with 1 line title and 1 line summary.
   *
   * @param message Push notification Bundle.
   * @param bigPicture Bitmap for BigPictureStyle notification.
   * @param title String with title for push notification.
   * @param messageText String with text for push notification.
   * @return Notification.BigPictureStyle or null.
   */
  @TargetApi(16)
  static Notification.BigPictureStyle getBigPictureStyle(Bundle message, Bitmap bigPicture,
      String title, final String messageText) {
    if (Build.VERSION.SDK_INT < 16 || message == null || bigPicture == null) {
      return null;
    }

    return new Notification.BigPictureStyle()
        .bigPicture(bigPicture)
        .setBigContentTitle(title)
        .setSummaryText(messageText);
  }

  /**
   * Gets Notification.Builder for provided parameters.
   *
   * @param context The application context.
   * @param message Push notification Bundle.
   * @param contentIntent PendingIntent.
   * @param title String with title for push notification.
   * @param messageText String with text for push notification.
   * @param defaultNotificationIconResourceId int Resource id for default push notification icon.
   * @return Notification.Builder or null.
   */
  static Notification.Builder getNotificationBuilder(Context context, Bundle message,
      PendingIntent contentIntent, String title, final String messageText,
      int defaultNotificationIconResourceId) {
    Notification.Builder notificationBuilder = getNotificationBuilder(context, message);
    if (notificationBuilder == null) {
      return null;
    }

    if (defaultNotificationIconResourceId == 0) {
      notificationBuilder.setSmallIcon(context.getApplicationInfo().icon);
    } else {
      notificationBuilder.setSmallIcon(defaultNotificationIconResourceId);
    }

    notificationBuilder.setContentTitle(title)
        .setContentText(messageText);

    if (Build.VERSION.SDK_INT > 16) {
      notificationBuilder.setStyle(new Notification.BigTextStyle()
          .bigText(messageText));
      if (!BuildUtil.isNotificationChannelSupported(context)) {
        //noinspection deprecation
        notificationBuilder.setPriority(Notification.PRIORITY_MAX);
      }
    }
    notificationBuilder.setAutoCancel(true);
    notificationBuilder.setContentIntent(contentIntent);
    return notificationBuilder;
  }

  /**
   * Checks a possibility to create icon drawable from current app icon.
   *
   * @param context Current application context.
   * @return boolean True if it is possible to create a drawable from current app icon.
   */
  @TargetApi(16)
  private static boolean canCreateIconDrawable(Context context) {
    try {
      // Try to create icon drawable.
      Drawable drawable = AdaptiveIconDrawable.createFromStream(
          context.getResources().openRawResource(context.getApplicationInfo().icon),
          "applicationInfo.icon");
      // If there was no crash, we still need to check for null.
      if (drawable != null) {
        return true;
      }
    } catch (Throwable ignored) {
    }
    return false;
  }

  /**
   * Validation of Application icon for small icon on push notification.
   *
   * @param context Current application context.
   * @return boolean True if application icon can be used for small icon on push notification.
   */
  static boolean isApplicationIconValid(Context context) {
    if (context == null) {
      return false;
    }

    // TODO: Potentially there should be checked for Build.VERSION.SDK_INT != 26, but we need to
    // TODO: confirm that adaptive icon works well on 27, before to change it.
    if (Build.VERSION.SDK_INT < 26) {
      return true;
    }

    return canCreateIconDrawable(context);
  }

  /**
   * Gets default push notification resource id for LEANPLUM_DEFAULT_PUSH_ICON in drawable.
   *
   * @param context Current application context.
   * @return int Resource id.
   */
  static int getDefaultPushNotificationIconResourceId(Context context) {
    try {
      Resources resources = context.getResources();
      return resources.getIdentifier(LEANPLUM_DEFAULT_PUSH_ICON, "drawable",
          context.getPackageName());
    } catch (Throwable ignored) {
      return 0;
    }
  }

  /**
   * Schedule JobService to JobScheduler.
   *
   * @param context Current application context.
   * @param clazz JobService class.
   * @param jobId JobService id.
   */
  @TargetApi(21)
  static void scheduleJobService(Context context, Class clazz, int jobId) {
    if (context == null) {
      return;
    }
    ComponentName serviceName = new ComponentName(context, clazz);
    JobScheduler jobScheduler =
        (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    if (jobScheduler != null) {
      jobId = verifyJobId(jobScheduler.getAllPendingJobs(), jobId);
      JobInfo startMyServiceJobInfo = new JobInfo.Builder(jobId, serviceName)
          .setMinimumLatency(10).build();
      jobScheduler.schedule(startMyServiceJobInfo);
    }
  }

  /**
   * Verifies that jobId don't present on JobScheduler pending jobs. If jobId present on
   * JobScheduler pending jobs generates a new one.
   *
   * @param allPendingJobs List of current pending jobs.
   * @param jobId JobService id.
   * @return jobId if jobId don't present on JobScheduler pending jobs
   */
  @TargetApi(21)
  private static int verifyJobId(List<JobInfo> allPendingJobs, int jobId) {
    if (allPendingJobs != null && !allPendingJobs.isEmpty()) {
      TreeSet<Integer> idsSet = new TreeSet<>();
      for (JobInfo jobInfo : allPendingJobs) {
        idsSet.add(jobInfo.getId());
      }
      if (idsSet.contains(jobId)) {
        if (idsSet.first() > Integer.MIN_VALUE) {
          jobId = idsSet.first() - 1;
        } else if (idsSet.last() < Integer.MIN_VALUE) {
          jobId = idsSet.last() + 1;
        } else {
          while (idsSet.contains(jobId)) {
            jobId = new Random().nextInt();
          }
        }
      }
    }
    return jobId;
  }

  /**
   * Gets bitmap for BigPicture style push notification.
   *
   * @param context Current application context.
   * @param imageUrl String with url to image.
   * @return Scaled bitmap for push notification with big image or null.
   */
  @Nullable
  static Bitmap getBigPictureBitmap(Context context, String imageUrl) {
    Bitmap bigPicture = null;
    // BigPictureStyle support requires API 16 and higher.
    if (!TextUtils.isEmpty(imageUrl) && Build.VERSION.SDK_INT >= 16) {
      bigPicture = BitmapUtil.getScaledBitmap(context, imageUrl);
      if (bigPicture == null) {
        Log.d("Failed to download image for push notification: %s", imageUrl);
      }
    }
    return bigPicture;
  }
}
