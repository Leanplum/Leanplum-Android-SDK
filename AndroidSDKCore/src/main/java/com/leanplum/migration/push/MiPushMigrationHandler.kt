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

package com.leanplum.migration.push

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler

class MiPushMigrationHandler internal constructor() {

  fun createNotification(context: Context?, messageContent: String?): Boolean {
    val messageBundle: Bundle = Utils.stringToBundle(messageContent)
    val isSuccess = try {
      PushNotificationHandler.getPushNotificationHandler().onMessageReceived(
        context,
        messageBundle,
        PushType.XPS.toString())
    } catch (t: Throwable) {
      t.printStackTrace()
      false
    }
    return isSuccess
  }

  fun onNewToken(context: Context?, token: String?): Boolean {
    var isSuccess = false
    try {
      PushNotificationHandler.getPushNotificationHandler().onNewToken(
        context,
        token,
        PushType.XPS.type
      )
      isSuccess = true
    } catch (t: Throwable) {
      t.printStackTrace()
    }
    return isSuccess
  }
}
