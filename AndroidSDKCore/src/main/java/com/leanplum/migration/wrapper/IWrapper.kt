package com.leanplum.migration.wrapper

import android.app.Application
import android.content.Context
import com.leanplum.callbacks.CleverTapInstanceCallback
import com.leanplum.migration.push.FcmMigrationHandler
import com.leanplum.migration.push.HmsMigrationHandler
import com.leanplum.migration.push.MiPushMigrationHandler

interface IWrapper {

  val fcmHandler: FcmMigrationHandler? get() = null

  val hmsHandler: HmsMigrationHandler? get() = null

  val miPushHandler: MiPushMigrationHandler? get() = null

  fun launch(context: Context, callback: CleverTapInstanceCallback?) = Unit

  fun setInstanceCallback(callback: CleverTapInstanceCallback?) = Unit

  fun setUserId(userId: String?) = Unit

  fun setDeviceId(deviceId: String?) = Unit

  fun track(
    event: String?,
    value: Double,
    info: String?,
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

  fun advanceTo(state: String?, info: String?, params: Map<String, Any?>?) = Unit

  fun setUserAttributes(attributes: Map<String, Any?>?) = Unit

  fun setTrafficSourceInfo(info: Map<String, String>) = Unit

  fun registerLifecycleCallback(app: Application) = Unit

  fun setLogLevel(level: Int) = Unit

}
