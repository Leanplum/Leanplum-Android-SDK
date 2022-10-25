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
import android.text.TextUtils
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CTUtils
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.pushnotification.PushConstants
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler
import com.leanplum.callbacks.CleverTapInstanceCallback
import com.leanplum.internal.Constants
import com.leanplum.internal.Log
import com.leanplum.migration.MigrationConstants
import com.leanplum.migration.MigrationManager
import com.leanplum.migration.push.FcmMigrationHandler
import com.leanplum.migration.push.HmsMigrationHandler
import com.leanplum.migration.push.MiPushMigrationHandler
import com.leanplum.utils.SharedPreferencesUtil

internal class CTWrapper(
  private val accountId: String,
  private val accountToken: String,
  private val accountRegion: String,
  deviceId: String,
  userId: String?
) : IWrapper by StaticMethodsWrapper {

  override val fcmHandler: FcmMigrationHandler = FcmMigrationHandler()
  override val hmsHandler: HmsMigrationHandler = HmsMigrationHandler()
  override val miPushHandler: MiPushMigrationHandler = MiPushMigrationHandler()

  private var cleverTapInstance: CleverTapAPI? = null
  private var instanceCallback: CleverTapInstanceCallback? = null

  private var identityManager = IdentityManager(deviceId, userId ?: deviceId)
  private var firstTimeStart = identityManager.isFirstTimeStart()

  override fun launch(context: Context, callback: CleverTapInstanceCallback?) {
    instanceCallback = callback

    val lpLevel = Log.getLogLevel()
    val ctLevel = MigrationConstants.mapLogLevel(lpLevel).intValue()

    val config = CleverTapInstanceConfig.createInstance(
      context,
      accountId,
      accountToken,
      accountRegion).apply {
      enableCustomCleverTapId = true
      debugLevel = ctLevel // set instance log level
      setLogLevel(lpLevel) // set static log level, arg needs to be Leanplum's level
    }

    val cleverTapId = identityManager.cleverTapId()
    val profile = identityManager.profile()
    Log.d("Wrapper: using CleverTapID=__h$cleverTapId")

    cleverTapInstance = CleverTapAPI.instanceWithConfig(context, config, cleverTapId)?.apply {
      setLibrary("Leanplum")
      if (!ActivityLifecycleCallback.registered) {
        ActivityLifecycleCallback.register(context.applicationContext as? Application)
      }
      if (identityManager.isAnonymous()) {
        Log.d("Wrapper: identity not set for anonymous user")
      } else {
        Log.d("Wrapper: will call onUserLogin with $profile and __h$cleverTapId")
        onUserLogin(profile, cleverTapId)
        setDevicesProperty()
      }
      Log.d("Wrapper: CleverTap instance created by Leanplum")
    }
    if (firstTimeStart) {
      // Send tokens in same session, because often a restart is needed for CT SDK to get them
      sendPushTokens(context)
    }
    triggerInstanceCallback()
  }

  private fun triggerInstanceCallback() {
    cleverTapInstance?.also { instance ->
      instanceCallback?.apply {
        Log.d("Wrapper: instance callback will be called")
        onInstance(instance)
      }
    }
  }

  override fun setInstanceCallback(callback: CleverTapInstanceCallback?) {
    instanceCallback = callback
    triggerInstanceCallback()
  }

  private fun sendPushTokens(context: Context) {
    // FCM
    val fcmToken = SharedPreferencesUtil.getString(context,
      Constants.Defaults.LEANPLUM_PUSH, Constants.Defaults.PROPERTY_FCM_TOKEN_ID)
    if (!TextUtils.isEmpty(fcmToken)) {
      val type = PushConstants.PushType.FCM.type
      PushNotificationHandler.getPushNotificationHandler().onNewToken(context, fcmToken, type)
      Log.d("Wrapper: fcm token sent")
    }

    // XPS
    val miPushToken = SharedPreferencesUtil.getString(context,
      Constants.Defaults.LEANPLUM_PUSH, Constants.Defaults.PROPERTY_MIPUSH_TOKEN_ID)
    if (!TextUtils.isEmpty(miPushToken)) {
      val type = PushConstants.PushType.XPS.type
      PushNotificationHandler.getPushNotificationHandler().onNewToken(context, miPushToken, type)
      Log.d("Wrapper: xps token sent")
    }

    // HMS
    val hmsToken = SharedPreferencesUtil.getString(context,
      Constants.Defaults.LEANPLUM_PUSH, Constants.Defaults.PROPERTY_HMS_TOKEN_ID)
    if (!TextUtils.isEmpty(hmsToken)) {
      val type = PushConstants.PushType.HPS.type
      PushNotificationHandler.getPushNotificationHandler().onNewToken(context, hmsToken, type)
      Log.d("Wrapper: hms token sent")
    }
  }

  override fun setUserId(userId: String?) {
    if (userId == null || userId.isEmpty()) return

    if (!identityManager.setUserId(userId)) {
      // trying to set same userId
      return
    }

    val cleverTapId = identityManager.cleverTapId()
    val profile = identityManager.profile()

    Log.d("Wrapper: Leanplum.setUserId will call onUserLogin with $profile and __h$cleverTapId")
    cleverTapInstance?.onUserLogin(profile, cleverTapId)
    cleverTapInstance?.setDevicesProperty()
  }

  private fun CleverTapAPI.setDevicesProperty() {
    if (identityManager.isDeviceIdHashed()) {
      val deviceId = identityManager.getOriginalDeviceId()
      CTUtils.addMultiValueForKey(MigrationConstants.DEVICES_USER_PROPERTY, deviceId, this)
    }
  }

  /**
   * LP doesn't allow iterables in params.
   */
  override fun track(
    event: String?,
    value: Double,
    info: String?,
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

    if (info != null) {
      properties[MigrationConstants.INFO_PARAM] = info
    }

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
  override fun advanceTo(state: String?, info: String?, params: Map<String, Any?>?) {
    if (state == null) return;

    val event = MigrationConstants.STATE_PREFIX + state
    val properties =
      params?.mapValues(::mapNotSupportedValues)?.toMutableMap()
        ?: mutableMapOf()

    if (info != null) {
      properties[MigrationConstants.INFO_PARAM] = info
    }

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
