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

package com.leanplum.internal;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumPushService;

/**
 * LeanplumManifestHelper class to work with AndroidManifest components.
 *
 * @author Anna Orlova
 */
public class LeanplumManifestHelper {

  /**
   * Gets Class for name.
   *
   * @param className - class name.
   * @return Class for provided class name.
   */
  public static Class getClassForName(String className) {
    try {
      return Class.forName(className);
    } catch (Throwable t) {
      return null;
    }
  }

  /**
   * Enables and starts service for provided class name.
   *
   * @param context Current Context.
   * @param packageManager Current PackageManager.
   * @param className Name of Class that needs to be enabled and started.
   * @return True if service was enabled and started.
   */
  public static boolean enableServiceAndStart(Context context, PackageManager packageManager,
      String className) {
    Class clazz;
    try {
      clazz = Class.forName(className);
    } catch (Throwable t) {
      return false;
    }
    return enableServiceAndStart(context, packageManager, clazz);
  }

  /**
   * Enables and starts service for provided class name.
   *
   * @param context Current Context.
   * @param packageManager Current PackageManager.
   * @param clazz Class of service that needs to be enabled and started.
   * @return True if service was enabled and started.
   */
  public static boolean enableServiceAndStart(Context context, PackageManager packageManager,
      Class clazz) {
    if (!enableComponent(context, packageManager, clazz)) {
      return false;
    }
    try {
      context.startService(new Intent(context, clazz));
    } catch (Throwable t) {
      Log.w("Could not start service " + clazz.getName());
      return false;
    }
    return true;
  }

  /**
   * Enables component for provided class name.
   *
   * @param context Current Context.
   * @param packageManager Current PackageManager.
   * @param className Name of Class for enable.
   * @return True if component was enabled.
   */
  public static boolean enableComponent(Context context, PackageManager packageManager,
      String className) {
    try {
      Class clazz = Class.forName(className);
      return enableComponent(context, packageManager, clazz);
    } catch (Throwable t) {
      return false;
    }
  }

  /**
   * Enables component for provided class.
   *
   * @param context Current Context.
   * @param packageManager Current PackageManager.
   * @param clazz Class for enable.
   * @return True if component was enabled.
   */
  public static boolean enableComponent(Context context, PackageManager packageManager,
      Class clazz) {
    if (clazz == null || context == null || packageManager == null) {
      return false;
    }

    try {
      packageManager.setComponentEnabledSetting(new ComponentName(context, clazz),
          PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    } catch (Throwable t) {
      Log.w("Could not enable component " + clazz.getName());
      return false;
    }
    return true;
  }

  /**
   * Checks if component for provided class enabled before.
   *
   * @param context Current Context.
   * @param packageManager Current PackageManager.
   * @param clazz Class for check.
   * @return True if component was enabled before.
   */
  public static boolean wasComponentEnabled(Context context, PackageManager packageManager,
      Class clazz) {
    if (clazz == null || context == null || packageManager == null) {
      return false;
    }
    int componentStatus = packageManager.getComponentEnabledSetting(new ComponentName(context,
        clazz));
    if (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT == componentStatus ||
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED == componentStatus) {
      return false;
    }
    return true;
  }

  /**
   * Parses and returns client broadcast receiver class name.
   *
   * @return Client broadcast receiver class name.
   */
  public static String parseNotificationMetadata() {
    try {
      Context context = Leanplum.getContext();
      ApplicationInfo app = context.getPackageManager().getApplicationInfo(context.getPackageName(),
          PackageManager.GET_META_DATA);
      Bundle bundle = app.metaData;
      return bundle.getString(LeanplumPushService.LEANPLUM_NOTIFICATION);
    } catch (Throwable ignored) {
    }
    return null;
  }
}
