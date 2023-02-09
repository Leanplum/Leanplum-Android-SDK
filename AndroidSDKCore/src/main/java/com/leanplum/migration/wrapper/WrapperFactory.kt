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

package com.leanplum.migration.wrapper

import android.content.Context
import com.leanplum.Leanplum
import com.leanplum.callbacks.CleverTapInstanceCallback
import com.leanplum.internal.AESCrypt
import com.leanplum.internal.Constants
import com.leanplum.internal.LeanplumInternal
import com.leanplum.internal.Log
import com.leanplum.migration.model.MigrationConfig
import com.leanplum.utils.getLeanplumPrefs
import kotlin.system.measureTimeMillis

internal object WrapperFactory {

  fun createWrapper(callbacks: List<CleverTapInstanceCallback>): IWrapper {
    val account = MigrationConfig.accountId
    val token = MigrationConfig.accountToken
    val region = MigrationConfig.accountRegion
    if (account == null || token == null || region == null) {
      return NullWrapper
    }

    val context = Leanplum.getContext() ?: return StaticMethodsWrapper
    val deviceId: String?
    val userId: String?

    if (LeanplumInternal.hasCalledStart()) {
      deviceId = Leanplum.getDeviceId()
      userId = Leanplum.getUserId()
    } else {
      val profile = getDeviceAndUserFromPrefs(context)
      deviceId = profile.first
      userId = profile.second
    }

    if (deviceId == null) {
      return StaticMethodsWrapper
    }

    val identityList = MigrationConfig.identityList

    return CTWrapper(account, token, region, identityList, deviceId, userId).apply {
      val timeToLaunch = measureTimeMillis {
        launch(context, callbacks)
      }
      Log.d("Wrapper: launch took $timeToLaunch millis")
    }
  }

  private fun getDeviceAndUserFromPrefs(context: Context): Pair<String?, String?> {
    val appId = MigrationConfig.appId ?: return Pair(null, null)

    val sharedPrefs = context.getLeanplumPrefs()
    val aesCrypt = AESCrypt(appId, null)

    val deviceId = aesCrypt.decodePreference(sharedPrefs, Constants.Params.DEVICE_ID, null)
    val userId = aesCrypt.decodePreference(sharedPrefs, Constants.Params.USER_ID, null)

    return Pair(deviceId, userId)
  }

}
