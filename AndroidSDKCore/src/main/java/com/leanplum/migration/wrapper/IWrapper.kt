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

import android.app.Application
import android.content.Context
import com.leanplum.callbacks.CleverTapInstanceCallback
import com.leanplum.migration.push.FcmMigrationHandler
import com.leanplum.migration.push.HmsMigrationHandler

interface IWrapper {

  val fcmHandler: FcmMigrationHandler? get() = null

  val hmsHandler: HmsMigrationHandler? get() = null

  fun launch(context: Context, callbacks: List<CleverTapInstanceCallback>) = Unit

  fun addInstanceCallback(callback: CleverTapInstanceCallback) = Unit

  fun removeInstanceCallback(callback: CleverTapInstanceCallback) = Unit

  fun setUserId(userId: String?) = Unit

  fun track(
    event: String?,
    value: Double,
    params: Map<String, Any?>?,
  ) = Unit

  fun trackPurchase(
    event: String,
    value: Double,
    currencyCode: String?,
    params: Map<String, Any?>?
  ) = Unit

  fun trackGooglePlayPurchase(
    event: String,
    item: String?,
    value: Double,
    currencyCode: String?,
    purchaseData: String?,
    dataSignature: String?,
    params: Map<String, Any?>?,
  ) = Unit

  fun advanceTo(state: String?, params: Map<String, Any?>?) = Unit

  fun setUserAttributes(attributes: Map<String, Any?>?) = Unit

  fun setTrafficSourceInfo(info: Map<String, String>) = Unit

  fun registerLifecycleCallback(app: Application) = Unit

  fun setLogLevel(lpLevel: Int) = Unit

}
