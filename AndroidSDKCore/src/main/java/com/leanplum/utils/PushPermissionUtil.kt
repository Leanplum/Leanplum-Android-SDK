/*
 * Copyright 2023, Leanplum, Inc. All rights reserved.
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

package com.leanplum.utils

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.leanplum.internal.Log

/**
 * Could be changed by client if code is already in use. Request code can be used in activity's
 * onRequestPermissionsResult method to receive feedback whether the permission was granted or not.
 */
var pushPermissionRequestCode = 1233321

@TargetApi(33)
private fun isNotificationPermissionGranted(activity: Activity): Boolean {
  val res = ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
  return res == PackageManager.PERMISSION_GRANTED
}

@TargetApi(33)
fun shouldShowRegisterForPush(activity: Activity): Boolean {
  return BuildUtil.isPushPermissionSupported(activity)
      && !isNotificationPermissionGranted(activity)
      && !NotificationManagerCompat.from(activity.applicationContext).areNotificationsEnabled()
}

@TargetApi(33)
fun shouldShowPrePermission(activity: Activity): Boolean {
  return shouldShowRegisterForPush(activity)
      && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
}

@TargetApi(33)
fun requestNativePermission(activity: Activity) {
  activity.requestPermissions(
    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
    pushPermissionRequestCode)
}

fun printDebugLog(activity: Activity) {
  if (BuildUtil.isPushPermissionSupported(activity)) {
    val permissionGranted = isNotificationPermissionGranted(activity)
    val notificationsEnabled = NotificationManagerCompat
      .from(activity.applicationContext)
      .areNotificationsEnabled()
    val shouldShowRequestPermissionRationale = ActivityCompat
      .shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)

    Log.d("Notification permission: granted=$permissionGranted " +
        "notificationsEnabled=$notificationsEnabled " +
        "shouldShowRequestPermissionRationale=$shouldShowRequestPermissionRationale")
  } else {
    Log.d("Notification permission: not supported by target or device version")
  }
}