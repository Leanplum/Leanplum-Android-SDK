package com.leanplum.migration.wrapper

import android.app.Application
import android.content.Context
import com.leanplum.migration.push.FcmMigrationHandler
import com.leanplum.migration.push.HmsMigrationHandler
import com.leanplum.migration.push.MiPushMigrationHandler

interface IWrapper {

  val fcmHandler: FcmMigrationHandler? get() = null

  val hmsHandler: HmsMigrationHandler? get() = null

  val miPushHandler: MiPushMigrationHandler? get() = null

  fun launch(context: Context, userId: String?, deviceId: String?) = Unit

  fun setUserId(userId: String?) = Unit

  fun setDeviceId(deviceId: String?) = Unit

  fun track(
    eventName: String?,
    value: Double?,
    info: String?,
    params: Map<String, Any>?,
    args: Map<String, Any>?
  ) = Unit

  fun advance(stateName: String?, info: String?, params: Map<String, Any>?) = Unit

  fun setUserAttributes(attributes: Map<String, Any>?) = Unit

  fun registerLifecycleCallback(app: Application) = Unit

  fun setLogLevel(level: Int) = Unit

}
