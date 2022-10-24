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

import com.leanplum.internal.Log
import com.leanplum.migration.MigrationConstants
import com.leanplum.utils.StringPreference
import com.leanplum.utils.StringPreferenceNullable
import kotlin.properties.ReadWriteProperty

private const val UNDEFINED = "undefined"
private const val ANONYMOUS = "anonymous"
private const val IDENTIFIED = "identified"

/**
 * Scheme for migrating user profile is as follows:
 *   - anonymous is translated to <CTID=deviceId, Identity=null>
 *   - non-anonymous to <CTID=deviceId_hash(userId), Identity=userId>
 * Where deviceId is also hashed if it is longer than 50 characters or contains invalid symbols.
 *
 * When you login, but previous profile is anonymous, a merge should happen. CT SDK allows merges
 * only when the CTID remains the same, meaning that the merged profile would get the anonymous
 * profile's CTID. This is the reason to save anonymousMergeUserId into SharedPreferences and to
 * allow it to restore when user is back.
 *
 * When you call Leanplum.start and pass userId, that is not currently logged in, there are several
 * cases that could happen:
 *
 * 1. "undefined" state
 *
 * Wrapper hasn't been started even once, meaning that anonymous profile doesn't exist, so use the
 * "deviceId_hash(userId)" scheme.
 *
 * 2. "anonymous" state
 *
 * Wrapper has been started and previous user is anonymous - use deviceId as CTID to allow merge of
 * anonymous data.
 *
 * 3. "identified" state
 *
 * Wrapper has been started and previous user is not anonymous - use the "deviceId_hash(userId)"
 * scheme.
 */
class IdentityManager(
  deviceId: String,
  userId: String,
  stateDelegate: ReadWriteProperty<Any, String> = StringPreference("ct_login_state", UNDEFINED),
  mergeUserDelegate: ReadWriteProperty<Any, String?> = StringPreferenceNullable("ct_anon_merge_userid"),
) {

  private val identity: LPIdentity = LPIdentity(deviceId = deviceId, userId = userId)
  private var state: String by stateDelegate
  private var anonymousMergeUserId: String? by mergeUserDelegate

  init {
    if (isAnonymous()) {
      loginAnonymously()
    } else {
      loginIdentified()
    }
  }

  fun isAnonymous() = identity.isAnonymous()

  fun isStateUndefined() = state == UNDEFINED

  private fun loginAnonymously() {
    state = ANONYMOUS
  }

  private fun loginIdentified() {
    if (state == UNDEFINED) {
      state = IDENTIFIED
    }
    else if (state == ANONYMOUS) {
      anonymousMergeUserId = identity.userId()
      Log.d("Wrapper: anonymous data will be merged to $anonymousMergeUserId")
      state = IDENTIFIED
    }
  }

  fun cleverTapId(): String {
    if (identity.isAnonymous()) {
      return identity.deviceId()
    } else if (identity.userId() == anonymousMergeUserId) {
      return identity.deviceId()
    } else {
      return "${identity.deviceId()}_${identity.userId()}"
    }
  }

  fun profile() = mapOf(MigrationConstants.IDENTITY to identity.originalUserId())

  fun setUserId(userId: String): Boolean {
    if (!identity.setUserId(userId)) {
      // trying to set same userId
      return false
    }

    if (state == ANONYMOUS) {
      anonymousMergeUserId = identity.userId()
      Log.d("Wrapper: anonymous data will be merged to $anonymousMergeUserId")
      state = IDENTIFIED
    }
    return true;
  }

  fun getOriginalDeviceId() = identity.originalDeviceId()

}
