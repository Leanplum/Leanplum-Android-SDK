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

package com.leanplum.migration

import com.clevertap.android.sdk.CleverTapAPI
import com.leanplum.internal.Log

object MigrationConstants {
  const val IDENTITY = "Identity"
  const val STATE_PREFIX = "state_"

  const val CHARGED_EVENT_PARAM = "event"
  const val VALUE_PARAM = "value"
  const val CURRENCY_CODE_PARAM = "currencyCode"
  const val INFO_PARAM = "info"
  const val GP_PURCHASE_DATA_PARAM = "googlePlayPurchaseData"
  const val GP_PURCHASE_DATA_SIGNATURE_PARAM = "googlePlayPurchaseDataSignature"
  const val IAP_ITEM_PARAM = "item"

  fun mapLogLevel(lpLevel: Int): CleverTapAPI.LogLevel = when (lpLevel) {
    Log.Level.OFF -> CleverTapAPI.LogLevel.OFF
    Log.Level.ERROR -> CleverTapAPI.LogLevel.INFO
    Log.Level.INFO -> CleverTapAPI.LogLevel.INFO
    Log.Level.DEBUG -> CleverTapAPI.LogLevel.VERBOSE
    else -> CleverTapAPI.LogLevel.INFO
  }
}
