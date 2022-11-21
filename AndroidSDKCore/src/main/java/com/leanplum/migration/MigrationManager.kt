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

import com.leanplum.callbacks.CleverTapInstanceCallback
import com.leanplum.core.BuildConfig
import com.leanplum.internal.*
import com.leanplum.migration.model.MigrationConfig
import com.leanplum.migration.model.MigrationState
import com.leanplum.migration.wrapper.IWrapper
import com.leanplum.migration.wrapper.NullWrapper
import com.leanplum.migration.wrapper.StaticMethodsWrapper
import com.leanplum.migration.wrapper.WrapperFactory
import org.json.JSONObject

object MigrationManager {

  @JvmStatic
  fun getState(): MigrationState = MigrationState.from(MigrationConfig.state)

  @JvmStatic
  var wrapper: IWrapper = NullWrapper
    @Synchronized get
    private set

  /**
   * List is kept the same as the one in IWrapper instance, because if wrapper state is changed it
   * would still continue to work.
   */
  private val instanceCallbackList: MutableList<CleverTapInstanceCallback> = mutableListOf()

  @JvmStatic
  @Synchronized
  fun addCleverTapInstanceCallback(callback: CleverTapInstanceCallback) {
    instanceCallbackList.add(callback)
    wrapper.addInstanceCallback(callback)
  }

  @JvmStatic
  @Synchronized
  fun removeCleverTapInstanceCallback(callback: CleverTapInstanceCallback) {
    instanceCallbackList.remove(callback)
    wrapper.removeInstanceCallback(callback)
  }

  @JvmStatic
  @Synchronized
  fun updateWrapper() {
    if (Constants.isNoop()) {
      wrapper = NullWrapper
      return
    }

    if (getState().useCleverTap()
      && (wrapper == NullWrapper || wrapper == StaticMethodsWrapper)
    ) {
      wrapper = WrapperFactory.createWrapper(instanceCallbackList)
    } else if (wrapper != NullWrapper && !getState().useCleverTap()) {
      wrapper = NullWrapper
    }
  }

  @JvmStatic
  fun fetchState(callback: (MigrationState) -> Unit) {
    if (getState() != MigrationState.Undefined) {
      updateWrapper() // replaces StaticMethodsWrapper with CTWrapper
      callback.invoke(getState())
    } else {
      fetchStateAsync {
        callback.invoke(getState())
      }
    }
  }

  private fun fetchStateAsync(callback: (Boolean) -> Unit) {
    val request = RequestBuilder
      .withGetMigrateState()
      .andType(Request.RequestType.IMMEDIATE)
      .create()

    request.onError {
      Log.d("Error getting migration state", it)
      callback.invoke(false)
    }

    request.onResponse {
      Log.d("Migration state response: $it")
      val responseData = ResponseHandler().handleMigrateStateContent(it)
      if (responseData != null) {
        val oldState = getState()
        MigrationConfig.update(responseData)
        val newState = getState()
        handleStateTransition(oldState, newState)
      }
      callback.invoke(true)
    }

    RequestSender.getInstance().send(request)
  }

  @JvmStatic
  fun refreshStateMidSession(responseBody: JSONObject): Boolean {
    val newHash = ResponseHandler().handleMigrateState(responseBody) ?: return false
    if (newHash != MigrationConfig.hash) {
      fetchStateAsync { success ->
        if (success) {
          // transition side effects are handled in fetchStateAsync
        }
      }
      return true
    }
    return false
  }

  private fun handleStateTransition(oldState: MigrationState, newState: MigrationState) {
    if (oldState.useLeanplum() && !newState.useLeanplum()) {
      OperationQueue.sharedInstance().addOperation {
        // flush all saved data to LP
        RequestSender.getInstance().sendRequests()
        // delete LP data
        VarCache.clearUserContent()
        VarCache.saveDiffs()
      }
    }

    if (!oldState.useCleverTap() && newState.useCleverTap()) {
      OperationQueue.sharedInstance().addOperation {
        // flush all saved data to LP, new data will come with the flag ct=true
        RequestSender.getInstance().sendRequests()
        // create wrapper
        updateWrapper()
      }
    }

    if (oldState.useCleverTap() && !newState.useCleverTap()) {
      // remove wrapper
      updateWrapper()
    }
  }

  fun mapAttributeName(attributeName: String): String {
    val newName = MigrationConfig.attributeMap?.get(attributeName)
    return newName ?: attributeName
  }

  fun mapAttributeName(attribute: Map.Entry<String, Any?>): String {
    val newName = MigrationConfig.attributeMap?.get(attribute.key)
    return newName ?: attribute.key
  }

  @JvmStatic
  fun trackGooglePlayPurchases() = MigrationConfig.trackGooglePlayPurchases

  private fun getCleverTapVersion(): String? {
    return try {
      val clazz = Class.forName("com.clevertap.android.sdk.BuildConfig")
      val field = clazz.getField("VERSION_NAME")
      field.get(null) as String
    } catch (ignored: Throwable) {
      null
    }
  }

  @JvmStatic
  fun verifyCleverTapVersion() {
    val lpVersion = BuildConfig.CT_SDK_VERSION
    val clientVersion = getCleverTapVersion()
    if (lpVersion != clientVersion) {
      Log.e("Your CleverTap SDK dependency version is:\n" +
          "com.clevertap.android:clevertap-android-sdk:${clientVersion}\n" +
          "but you must use the supported by Leanplum SDK:\n" +
          "com.clevertap.android:clevertap-android-sdk:${lpVersion}")
    }
  }

}
