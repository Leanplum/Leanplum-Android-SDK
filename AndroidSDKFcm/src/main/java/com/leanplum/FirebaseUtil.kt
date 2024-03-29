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

package com.leanplum

import android.text.TextUtils
import com.google.firebase.messaging.FirebaseMessaging
import com.leanplum.internal.Log
import com.leanplum.migration.MigrationManager

internal fun updateRegistrationId(provider: LeanplumCloudMessagingProvider) {
  try {
    Present.updateRegistrationId(provider)
  } catch (e: NoSuchMethodError) {
    Log.e("Minimum supported version of Firebase is 20.3.0", e)
  }
}

internal fun unregister() {
  try {
    Present.unregister()
  } catch (e: NoSuchMethodError) {
    Log.e("Minimum supported version of Firebase is 20.3.0", e)
  }
}

/**
 * Present Firebase interface was added in version 20.3.0.
 * Legacy methods are removed since 22.0.0.
 */
private object Present {
  fun updateRegistrationId(provider: LeanplumCloudMessagingProvider) {
    FirebaseMessaging.getInstance().token.addOnCompleteListener {
      if (it.isSuccessful) {
        val token = it.result.toString()
        if (!TextUtils.isEmpty(token)) {
          provider.registrationId = token

          MigrationManager.wrapper.fcmHandler?.onNewToken(Leanplum.getContext(), token)
        }
      } else {
        Log.e("getToken failed:\n" + Log.getStackTraceString(it.exception))
      }
    }
  }

  fun unregister() {
    try {
      FirebaseMessaging.getInstance().deleteToken()
      Log.i("Application was unregistered from FirebaseMessaging.")
    } catch (e: Exception) {
      Log.e("Failed to unregister from FirebaseMessaging.")
    }
  }
}
