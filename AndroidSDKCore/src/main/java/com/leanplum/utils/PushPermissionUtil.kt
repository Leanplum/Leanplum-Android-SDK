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
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.leanplum.Leanplum
import com.leanplum.internal.Log

private const val DECLINE_LIMIT = 2

/**
 * Could be changed by client if code is already in use. Request code can be used in activity's
 * [onRequestPermissionResult] method to receive feedback whether the permission was granted or not.
 */
var pushPermissionRequestCode = 1233321

/**
 * Decline count is tracked only when [onRequestPermissionResult] is invoked by client.
 *
 * Setting value of 2 would disable asking for push permission permanently.
 * Setting value of 0 would ask for push permission again.
 */
var declineCount: Int by IntPreference(key = "push_permission_decline_count", defaultValue = 0)

/**
 * Invoke method from your activity's onRequestPermissionResult to allow Leanplum SDK to track the
 * number of consecutive declines of the POST_NOTIFICATIONS permission. When two consecutive
 * declines happen none of the permission dialogs will be shown again and user would have to
 * manually allow notifications. Note that dismissing the native dialog without clicking on Allow or
 * Don't Allow does count as a decline from OS API.
 */
fun onRequestPermissionResult(requestCode: Int,
                              permissions: Array<String>,
                              grantResults: IntArray
) {
  val context: Context? = Leanplum.getContext()
  if (context == null || !BuildUtil.isPushPermissionSupported(context)) {
    return
  }

  if (requestCode != pushPermissionRequestCode || permissions.size != grantResults.size) {
    return
  }

  for (i in permissions.indices) {
    if (permissions[i] == Manifest.permission.POST_NOTIFICATIONS) {
      if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
        declineCount = 0
      } else {
        declineCount++
      }
      break
    }
  }
}

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
      && declineCount < DECLINE_LIMIT
}

@TargetApi(33)
fun requestNativePermission(activity: Activity) {
  if (declineCount < DECLINE_LIMIT) {
    activity.requestPermissions(
      arrayOf(Manifest.permission.POST_NOTIFICATIONS),
      pushPermissionRequestCode
    )
  }
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
        "shouldShowRequestPermissionRationale=$shouldShowRequestPermissionRationale " +
        "declineCount=$declineCount")
  } else {
    Log.d("Notification permission: not supported by target or device version")
  }
}