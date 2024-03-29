/*
 * Copyright 2022, Leanplum, Inc. All rights reserved.
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

package com.leanplum.internal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Build;
import androidx.annotation.NonNull;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumLocalPushListenerService;
import com.leanplum.internal.Constants.Defaults;
import com.leanplum.internal.Constants.Keys;
import com.leanplum.utils.BuildUtil;
import com.leanplum.utils.SharedPreferencesUtil;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * Leanplum local push notification helper class.
 *
 * @author Anna Orlova
 */
class LeanplumLocalPushHelper {

  /**
   * Schedule local push notification. This method will call by reflection from AndroidSDKCore.
   *
   * @param actionContext Action Context.
   * @return True if notification was scheduled.
   */
  static boolean scheduleLocalPush(@NonNull ActionContext actionContext) {
    try {
      String messageId = actionContext.getMessageId();

      // Get eta.
      Object countdownObj;
      if (((BaseActionContext) actionContext).isPreview()) {
        countdownObj = 5.0;
      } else {
        Map<String, Object> messageConfig = CollectionUtil.uncheckedCast(
            VarCache.getMessageDiffs().get(messageId));
        if (messageConfig == null) {
          Log.e("Could not find message options for ID " + messageId);
          return false;
        }
        countdownObj = messageConfig.get("countdown");
      }
      if (!(countdownObj instanceof Number)) {
        Log.e("Invalid notification countdown: " + countdownObj);
        return false;
      }
      long eta = Clock.getInstance().currentTimeMillis() + ((Number) countdownObj).longValue() * 1000L;

      // Schedule notification.
      return scheduleAlarm(actionContext, messageId, eta);

    } catch (Throwable t) {
      Log.exception(t);
      return false;
    }
  }

  /**
   * Schedule local push notification.
   *
   * @param actionContext Action Context.
   * @param messageId String message id for local push notification.
   * @param eta Eta for local push notification.
   * @return True if notification was scheduled.
   */
  private static boolean scheduleAlarm(ActionContext actionContext, String messageId, long eta) {
    try {
      Context context = Leanplum.getContext();
      Intent intentAlarm = LeanplumLocalPushListenerService.getIntent(context);
      AlarmManager alarmManager = (AlarmManager) context.getSystemService(
          Context.ALARM_SERVICE);

      // If there's already one scheduled before the eta, discard this.
      // Otherwise, discard the scheduled one.
      SharedPreferences preferences = context.getSharedPreferences(
          Constants.Defaults.MESSAGING_PREF_NAME, Context.MODE_PRIVATE);
      long existingEta = preferences.getLong(String.format(
          Constants.Defaults.LOCAL_NOTIFICATION_KEY, messageId), 0L);
      if (existingEta > 0L && existingEta > System.currentTimeMillis()) {
        if (existingEta < eta) {
          return false;
        } else if (existingEta >= eta) {
          PendingIntent existingIntent = PendingIntent.getBroadcast(
              context, messageId.hashCode(), intentAlarm,
              BuildUtil.createIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT));
          alarmManager.cancel(existingIntent);
        }
      }

      // Specify custom data for the notification
      Map<String, Serializable> data = actionContext.objectNamed("Advanced options.Data");
      if (data != null) {
        for (String key : data.keySet()) {
          intentAlarm.putExtra(key, data.get(key));
        }
      }

      // Set unique occurrence id
      intentAlarm.putExtra(Keys.PUSH_OCCURRENCE_ID, UUID.randomUUID().toString());

      // Specify open action
      String openAction = actionContext.stringNamed(Constants.Values.DEFAULT_PUSH_ACTION);
      boolean muteInsideApp = Boolean.TRUE.equals(actionContext.objectNamed(
          "Advanced options.Mute inside app"));
      if (openAction != null) {
        if (muteInsideApp) {
          intentAlarm.putExtra(Constants.Keys.PUSH_MESSAGE_ID_MUTE_WITH_ACTION, messageId);
        } else {
          intentAlarm.putExtra(Constants.Keys.PUSH_MESSAGE_ID_NO_MUTE_WITH_ACTION, messageId);
        }
      } else {
        if (muteInsideApp) {
          intentAlarm.putExtra(Constants.Keys.PUSH_MESSAGE_ID_MUTE, messageId);
        } else {
          intentAlarm.putExtra(Constants.Keys.PUSH_MESSAGE_ID_NO_MUTE, messageId);
        }
      }

      // Message.
      String message = actionContext.stringNamed("Message");
      intentAlarm.putExtra(Constants.Keys.PUSH_MESSAGE_TEXT,
          message != null ? message : Constants.Values.DEFAULT_PUSH_MESSAGE);

      // Collapse key.
      String collapseKey = actionContext.stringNamed("Android options.Collapse key");
      if (collapseKey != null) {
        intentAlarm.putExtra("collapseKey", collapseKey);
      }

      // Delay while idle.
      boolean delayWhileIdle = Boolean.TRUE.equals(actionContext.objectNamed(
          "Android options.Delay while idle"));
      if (delayWhileIdle) {
        intentAlarm.putExtra("delayWhileIdle", true);
      }

      // Schedule notification.
      PendingIntent operation = PendingIntent.getBroadcast(
          context, messageId.hashCode(), intentAlarm,
          BuildUtil.createIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT));

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Util.isXiaomiDevice()) {
        // Improve delivery of local pushes, particularly for offline and locked devices.
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, eta, operation);
      } else {
        alarmManager.set(AlarmManager.RTC_WAKEUP, eta, operation);
      }

      // Save notification so we can cancel it later.
      SharedPreferences.Editor editor = preferences.edit();
      editor.putLong(String.format(Constants.Defaults.LOCAL_NOTIFICATION_KEY, messageId), eta);
      SharedPreferencesUtil.commitChanges(editor);

      Log.i("Scheduling local notification.");
      return true;
    } catch (Throwable t) {
      Log.exception(t);
      return false;
    }
  }

  /**
   * Cancel local push notification. This method will call by reflection from AndroidSDKCore.
   *
   * @param messageId Message id of notification that should be canceled.
   */
  static boolean cancelLocalPush(@NonNull String messageId) {
    try {
      // Get existing eta and clear notification from preferences.
      Context context = Leanplum.getContext();
      SharedPreferences preferences = context.getSharedPreferences(
          Defaults.MESSAGING_PREF_NAME, Context.MODE_PRIVATE);
      String preferencesKey = String.format(Constants.Defaults.LOCAL_NOTIFICATION_KEY,
          messageId);
      long existingEta = preferences.getLong(preferencesKey, 0L);
      SharedPreferences.Editor editor = preferences.edit();
      editor.remove(preferencesKey);
      SharedPreferencesUtil.commitChanges(editor);

      // Cancel notification.
      cancelAlarm(context, messageId);
      boolean didCancel = existingEta > Clock.getInstance().currentTimeMillis();
      if (didCancel) {
        Log.i("Cancelled notification");
      }
      return didCancel;
    } catch (Throwable t) {
      Log.exception(t);
      return false;
    }
  }

  /**
   * Cancel local push notification.
   *
   * @param context The application context.
   * @param messageId Message id of notification that should be canceled.
   */
  private static void cancelAlarm(Context context, String messageId) {
    try {
      Intent intentAlarm = LeanplumLocalPushListenerService.getIntent(context);
      AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      PendingIntent existingIntent = PendingIntent.getBroadcast(
          context, messageId.hashCode(), intentAlarm,
          BuildUtil.createIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT));
      if (alarmManager != null && existingIntent != null) {
        alarmManager.cancel(existingIntent);
      }
    } catch (Throwable ignored) {
    }
  }
}
