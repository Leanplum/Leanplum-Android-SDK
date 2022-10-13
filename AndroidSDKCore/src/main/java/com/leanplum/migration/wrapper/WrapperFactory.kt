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

import com.leanplum.Leanplum
import com.leanplum.callbacks.CleverTapInstanceCallback
import com.leanplum.internal.Constants
import com.leanplum.internal.LeanplumInternal
import com.leanplum.migration.model.MigrationConfig

internal object WrapperFactory {

  fun createWrapper(callback: CleverTapInstanceCallback?): IWrapper {
    val account = MigrationConfig.accountId
    val token = MigrationConfig.accountToken
    val region = MigrationConfig.accountRegion
    if (account == null || token == null || region == null) {
      return NullWrapper
    }

    if (!LeanplumInternal.hasCalledStart()) {
      // giving access only to CT static methods, because Leanplum state is not fully initialised
      return StaticMethodsWrapper
    }

    val context = Leanplum.getContext()
    val deviceId = Leanplum.getDeviceId()
    val userId = Leanplum.getUserId()
    if (context == null || deviceId == null) {
      // giving access only to CT static methods, because Leanplum state is not fully initialised
      return StaticMethodsWrapper
    }

    return CTWrapper(account, token, region, deviceId, userId).apply {
      launch(context, callback)
    }
  }

}
