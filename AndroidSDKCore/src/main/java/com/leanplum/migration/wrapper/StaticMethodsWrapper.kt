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
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.leanplum.internal.Log
import com.leanplum.migration.MigrationConstants

/**
 * Singleton wrapping the static utility methods of CT SDK, used as a delegate from [CTWrapper].
 *
 * It is initialised at the first possible moment to give access for some utility methods. When
 * Leanplum SDK is initialised a [CTWrapper] instance will be used.
 */
object StaticMethodsWrapper : IWrapper {

  override fun registerLifecycleCallback(app: Application) {
    ActivityLifecycleCallback.register(app)
  }

  override fun setLogLevel(lpLevel: Int) {
    CleverTapAPI.setDebugLevel(MigrationConstants.mapLogLevel(lpLevel))
  }

}
