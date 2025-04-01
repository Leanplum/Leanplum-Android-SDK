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

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.text.TextUtils
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.CoreMetaData
import com.leanplum.LeanplumActivityHelper
import com.leanplum.callbacks.CleverTapInstanceCallback
import com.leanplum.internal.Constants
import com.leanplum.internal.Log
import com.leanplum.migration.MigrationConstants
import com.leanplum.migration.MigrationManager
import com.leanplum.migration.push.FcmMigrationHandler
import com.leanplum.migration.push.HmsMigrationHandler
import com.leanplum.utils.CTUtils
import com.leanplum.utils.SharedPreferencesUtil

internal class CTWrapper(
  private val accountId: String,
  private val accountToken: String,
  private val accountRegion: String,
  private val identityList: List<String>?,
  private val useCustomCleverTapId: Boolean,
  deviceId: String,
  userId: String?,
  loggedInUserId: String?,
) : IWrapper by StaticMethodsWrapper {

  override val fcmHandler: FcmMigrationHandler = FcmMigrationHandler()
  override val hmsHandler: HmsMigrationHandler = HmsMigrationHandler()

  private var cleverTapInstance: CleverTapAPI? = null
  private var instanceCallbackList: MutableList<CleverTapInstanceCallback> = mutableListOf()

  private var identityManager = IdentityManager(deviceId, userId ?: deviceId, loggedInUserId)
  private var firstTimeStart = identityManager.isFirstTimeStart()

  @SuppressLint("WrongConstant", "RestrictedApi")
  override fun launch(context: Context, callbacks: List<CleverTapInstanceCallback>) {
    instanceCallbackList.addAll(callbacks)

    val lpLevel = Log.getLogLevel()
    val ctLevel = MigrationConstants.mapLogLevel(lpLevel).intValue()

    val config = CleverTapInstanceConfig.createInstance(
      context,
      accountId,
      accountToken,
      accountRegion).apply {
      enableCustomCleverTapId = useCustomCleverTapId
      debugLevel = ctLevel // set instance log level
      setLogLevel(lpLevel) // set static log level, arg needs to be Leanplum's level
      identityList?.also { // setting IdentityKeys
        setIdentityKeys(*it.toTypedArray())
      }
    }

    cleverTapInstance = if (useCustomCleverTapId) {
      val cleverTapId = identityManager.cleverTapId()
      Log.d("Wrapper: using CleverTapID=__h$cleverTapId")
      CleverTapAPI.instanceWithConfig(context, config, cleverTapId)
    } else {
      Log.d("Wrapper: without CleverTapID")
      CleverTapAPI.instanceWithConfig(context, config)
    }
    cleverTapInstance?.apply {
      setLibrary("Leanplum")
      if (LeanplumActivityHelper.getCurrentActivity() != null) {
        if (!ActivityLifecycleCallback.registered) {
          ActivityLifecycleCallback.register(context.applicationContext as? Application)
          if (!LeanplumActivityHelper.isActivityPaused() && !CleverTapAPI.isAppForeground()) {
            // Trigger onActivityResumed because onResume of ActivityLifecycle has already been executed
            // in this case. This could happen on first start with ct migration. This method will also
            // trigger App Launched if it was not send already.
            CleverTapAPI.onActivityResumed(LeanplumActivityHelper.getCurrentActivity(), cleverTapID)
          }
        } else if (CoreMetaData.getCurrentActivity() == null && !LeanplumActivityHelper.isActivityPaused()) {
          // If CT ActivityLifecycleCallback was registered before LP had created the CT instance
          // CleverTapAPI.onActivityResumed would have not executed its initialization logic
          // (CleverTapAPI.instances would still have been null). This is checked here by
          // CoreMetaData.getCurrentActivity() == null.
          // In this case call onActivityResumed explicitly.
          CleverTapAPI.onActivityResumed(LeanplumActivityHelper.getCurrentActivity(), cleverTapID)
        }
      }
      if (identityManager.isAnonymous()) {
        Log.d("Wrapper: identity not set for anonymous user")
        setAnonymousDeviceProperty()
      } else {
        onUserLogin()
        setDevicesProperty()
      }
      Log.d("Wrapper: CleverTap instance created by Leanplum")
      if (firstTimeStart) {
        // Send tokens in the first session, because a restart is needed for CT SDK to get them
        sendPushTokens(context, this)
      }
    }
    triggerInstanceCallbacks()
  }

  private fun triggerInstanceCallbacks() {
    cleverTapInstance?.also { instance ->
      Log.d("Wrapper: notifying ${instanceCallbackList.size} instance callbacks")
      instanceCallbackList.forEach {
        it.onInstance(instance)
      }
    }
  }

  override fun addInstanceCallback(callback: CleverTapInstanceCallback) {
    instanceCallbackList.add(callback)
    cleverTapInstance?.also { instance ->
      Log.d("Wrapper: notifying new instance callback")
      callback.onInstance(instance)
    }
  }

  override fun removeInstanceCallback(callback: CleverTapInstanceCallback) {
    instanceCallbackList.remove(callback)
  }

  private fun sendPushTokens(context: Context, cleverTap: CleverTapAPI) {
    // FCM
    val fcmToken = SharedPreferencesUtil.getString(context,
      Constants.Defaults.LEANPLUM_PUSH, Constants.Defaults.PROPERTY_FCM_TOKEN_ID)
    if (!TextUtils.isEmpty(fcmToken)) {
      cleverTap.pushFcmRegistrationId(fcmToken, true)
      Log.d("Wrapper: fcm token sent")
    }

    // HMS
    val hmsToken = SharedPreferencesUtil.getString(context,
      Constants.Defaults.LEANPLUM_PUSH, Constants.Defaults.PROPERTY_HMS_TOKEN_ID)
    if (!TextUtils.isEmpty(hmsToken)) {
      cleverTap.pushRegistrationToken(hmsToken, HmsMigrationHandler.HPS_PUSH_TYPE, true)
      Log.d("Wrapper: hms token sent")
    }
  }

  override fun setUserId(userId: String?) {
    if (userId == null || userId.isEmpty()) return

    if (!identityManager.setUserId(userId)) {
      // trying to set same userId
      return
    }
    onUserLogin()
    cleverTapInstance?.setDevicesProperty()
  }

  private fun onUserLogin() {
    val cleverTapId = identityManager.cleverTapId()
    val profile = identityManager.profile()

    if (useCustomCleverTapId) {
      Log.d("Wrapper: Leanplum.setUserId will call onUserLogin with $profile and __h$cleverTapId")
      cleverTapInstance?.onUserLogin(profile, cleverTapId)
    } else {
      Log.d("Wrapper: Leanplum.setUserId will call onUserLogin with $profile")
      cleverTapInstance?.onUserLogin(profile)
    }
  }

  private fun CleverTapAPI.setAnonymousDeviceProperty() {
    if (identityManager.isDeviceIdHashed()) {
      val deviceId = identityManager.getOriginalDeviceId()
      Log.d("Wrapper: property ${MigrationConstants.ANONYMOUS_DEVICE_PROPERTY} set $deviceId")
      pushProfile(mapOf(MigrationConstants.ANONYMOUS_DEVICE_PROPERTY to deviceId))
    }
  }

  private fun CleverTapAPI.setDevicesProperty() {
    if (identityManager.isDeviceIdHashed()) {
      val deviceId = identityManager.getOriginalDeviceId()
      Log.d("Wrapper: property ${MigrationConstants.DEVICES_USER_PROPERTY} add $deviceId")
      CTUtils.addMultiValueForKey(MigrationConstants.DEVICES_USER_PROPERTY, deviceId, this)
    }
  }

  /**
   * LP doesn't allow iterables in params.
   */
  override fun track(
    event: String?,
    value: Double,
    params: Map<String, Any?>?
  ) {
    if (event == null) return
    if (event == Constants.PUSH_DELIVERED_EVENT_NAME || event == Constants.PUSH_OPENED_EVENT_NAME) {
      return
    }

    val properties =
      params?.mapValues(::mapNotSupportedValues)?.toMutableMap()
        ?: mutableMapOf()

    properties[MigrationConstants.VALUE_PARAM] = value

    Log.d("Wrapper: Leanplum.track will call pushEvent with $event and $properties")
    cleverTapInstance?.pushEvent(event, properties)
  }

  /**
   * LP doesn't allow iterables in params.
   */
  override fun trackPurchase(
    event: String,
    value: Double,
    currencyCode: String?,
    params: Map<String, Any?>?
  ) {
    val filteredParams =
      params?.mapValues(::mapNotSupportedValues)?.toMutableMap()
        ?: mutableMapOf()

    val details = HashMap<String, Any?>(filteredParams).apply {
      this[MigrationConstants.CHARGED_EVENT_PARAM] = event
      this[MigrationConstants.VALUE_PARAM] = value
      if (currencyCode != null) {
        this[MigrationConstants.CURRENCY_CODE_PARAM] = currencyCode
      }
    }

    val items = arrayListOf<HashMap<String, Any?>>()

    Log.d("Wrapper: Leanplum.trackPurchase will call pushChargedEvent with $details and $items")
    cleverTapInstance?.pushChargedEvent(details, items)
  }

  /**
   * LP doesn't allow iterables in params.
   */
  override fun trackGooglePlayPurchase(
    event: String,
    item: String?,
    value: Double,
    currencyCode: String?,
    purchaseData: String?,
    dataSignature: String?,
    params: Map<String, Any?>?
  ) {
    val filteredParams =
      params?.mapValues(::mapNotSupportedValues)?.toMutableMap()
        ?: mutableMapOf()

    val details = HashMap<String, Any?>(filteredParams).apply {
      this[MigrationConstants.CHARGED_EVENT_PARAM] = event
      this[MigrationConstants.VALUE_PARAM] = value
      this[MigrationConstants.CURRENCY_CODE_PARAM] = currencyCode
      this[MigrationConstants.GP_PURCHASE_DATA_PARAM] = purchaseData
      this[MigrationConstants.GP_PURCHASE_DATA_SIGNATURE_PARAM] = dataSignature
      this[MigrationConstants.IAP_ITEM_PARAM] = item
    }

    val items = arrayListOf<HashMap<String, Any?>>()

    Log.d("Wrapper: Leanplum.trackGooglePlayPurchase will call pushChargedEvent with $details and $items")
    cleverTapInstance?.pushChargedEvent(details, items)
  }

  /**
   * LP doesn't allow iterables in params.
   */
  override fun advanceTo(state: String?, params: Map<String, Any?>?) {
    if (state == null) return;

    val event = MigrationConstants.STATE_PREFIX + state
    val properties =
      params?.mapValues(::mapNotSupportedValues)?.toMutableMap()
        ?: mutableMapOf()

    Log.d("Wrapper: Leanplum.advance will call pushEvent with $event and $properties")
    cleverTapInstance?.pushEvent(event, properties)
  }

  /**
   * To remove an attribute CT.removeValueForKey is used.
   */
  override fun setUserAttributes(attributes: Map<String, Any?>?) {
    if (attributes == null || attributes.isEmpty()) {
      return
    }

    val profile = attributes
      .filterValues { value -> value != null }
      .mapValues(::mapNotSupportedValues)
      .mapKeys { MigrationManager.mapAttributeName(it) }

    Log.d("Wrapper: Leanplum.setUserAttributes will call pushProfile with $profile")
    cleverTapInstance?.pushProfile(profile)

    attributes
      .filterValues { value -> value == null}
      .mapKeys(MigrationManager::mapAttributeName)
      .forEach {
        Log.d("Wrapper: Leanplum.setUserAttributes will call removeValueForKey with ${it.key}")
        cleverTapInstance?.removeValueForKey(it.key)
      }
  }

  private fun mapNotSupportedValues(entry: Map.Entry<String, Any?>): Any? {
    return when (val value = entry.value) {
      is Iterable<*> ->
        value
        .filterNotNull()
        .joinToString(separator = ",", prefix = "[", postfix = "]")
      is Byte -> value.toInt()
      is Short -> value.toInt()
      else -> value
    }
  }

  override fun setTrafficSourceInfo(info: Map<String, String>) {
    val source = info["publisherName"]
    val medium = info["publisherSubPublisher"]
    val campaign = info["publisherSubCampaign"]
    Log.d("Wrapper: Leanplum.setTrafficSourceInfo will call pushInstallReferrer with " +
        "$source, $medium, and $campaign")
    cleverTapInstance?.pushInstallReferrer(source, medium, campaign)
  }

}
