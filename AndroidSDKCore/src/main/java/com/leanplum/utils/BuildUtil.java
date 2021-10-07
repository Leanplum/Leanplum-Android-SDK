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
package com.leanplum.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

/**
 * Utilities related to Build Version and target SDK.
 *
 * @author Anna Orlova
 */
public class BuildUtil {
  private static int targetSdk = -1;

  /**
   * Whether notification channels are supported.
   *
   * @param context The application context.
   * @return True if notification channels are supported, false otherwise.
   */
  public static boolean isNotificationChannelSupported(Context context) {
    return Build.VERSION.SDK_INT >= 26 && getTargetSdkVersion(context) >= 26;
  }

  /**
   * Checks whether notification trampolines are not supported.
   * Targeting Android 12 means you cannot use a service or broadcast receiver as a trampoline to
   * start an activity. The activity must be started immediately when notification is clicked.
   *
   * @param context The application context.
   * @return True if notification trampolines are not supported.
   */
  public static boolean shouldDisableTrampolines(Context context) {
    return Build.VERSION.SDK_INT >= 31 && getTargetSdkVersion(context) >= 31;
  }

  /**
   * Returns target SDK version parsed from manifest.
   *
   * @param context The application context.
   * @return Target SDK version.
   */
  private static int getTargetSdkVersion(Context context) {
    if (targetSdk == -1 && context != null) {
      targetSdk = context.getApplicationInfo().targetSdkVersion;
    }
    return targetSdk;
  }

  /**
   * Adds immutable property to the intent flags. Mandatory when targeting API 31.
   *
   * @param flags The default flags.
   * @return Flags with additional immutable property set.
   */
  public static int createIntentFlags(int flags) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return flags | PendingIntent.FLAG_IMMUTABLE;
    }
    return flags;
  }
}
