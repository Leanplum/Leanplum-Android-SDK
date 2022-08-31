package com.leanplum.migration.wrapper

import android.app.Application
import android.content.Context
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.leanplum.Leanplum
import com.leanplum.internal.Log
import com.leanplum.migration.Constants
import com.leanplum.migration.push.FcmMigrationHandler
import com.leanplum.migration.push.HmsMigrationHandler
import com.leanplum.migration.push.MiPushMigrationHandler

// TODO: Think about removing the LP dependencies from here
internal class CTWrapper(
  private val accountId: String,
  private val accountToken: String
  ) : IWrapper {

  override val fcmHandler: FcmMigrationHandler = FcmMigrationHandler()
  override val hmsHandler: HmsMigrationHandler = HmsMigrationHandler()
  override val miPushHandler: MiPushMigrationHandler = MiPushMigrationHandler()

  var cleverTapInstance: CleverTapAPI? = null
  var cleverTapId: String? = null

  private fun createCleverTapId(userId: String?, deviceId: String?): String? {
    deviceId ?: return null
    userId ?: return deviceId
    return "${deviceId}_${userId}"
  }

  override fun launch(context: Context, userId: String?, deviceId: String?) {
    val config = CleverTapInstanceConfig.createInstance(context, accountId, accountToken)

    cleverTapId = createCleverTapId(userId, deviceId)
    if (cleverTapId != null) {
      config.enableCustomCleverTapId = true
      cleverTapInstance = CleverTapAPI.instanceWithConfig(context, config, cleverTapId)
    } else {
      cleverTapInstance = CleverTapAPI.instanceWithConfig(context, config)
    }

    cleverTapInstance?.let {
      it.setLibrary("Leanplum")
      // add other configuration here
    }
  }

  override fun setUserId(userId: String?) {
    if (userId == null) return
    if (cleverTapId == null) return

    val profile: Map<String, Any> = mutableMapOf(Constants.IDENTITY to userId)
    cleverTapInstance?.onUserLogin(profile, cleverTapId)
  }

  override fun setDeviceId(deviceId: String?) {
    if (deviceId == null) return
    if (cleverTapId == null) return

    val identity = Leanplum.getUserId() ?: deviceId
    val profile: Map<String, Any> = mutableMapOf(Constants.IDENTITY to identity)
    cleverTapInstance?.onUserLogin(profile, cleverTapId)
  }

  override fun track(
    eventName: String?,
    value: Double?,
    info: String?,
    params: Map<String, Any>?,
    args: Map<String, Any>?
  ) {
    if (eventName == null) return

    // TODO check for duplications
    val eventProperties = params?.toMutableMap()?.apply {
      if (value != null && value != 0.0) {
        this[Constants.PARAM_VALUE] = value
      }

      if (info != null) {
        this[Constants.PARAM_INFO] = info
      }
    }

    // TODO handle purchase event ?

    cleverTapInstance?.pushEvent(eventName, eventProperties)
  }

  override fun advance(stateName: String?, info: String?, params: Map<String, Any>?) {
    if (stateName == null) return;

    val eventName = Constants.STATE_PREFIX + stateName
    val eventProperties = params?.toMutableMap()

    // TODO handle info ?

    cleverTapInstance?.pushEvent(eventName, eventProperties)
  }

  override fun setUserAttributes(attributes: Map<String, Any>?) {
    val profile = attributes?.toMutableMap() ?: return
    cleverTapInstance?.pushProfile(profile)
  }

  override fun registerLifecycleCallback(app: Application) {
    ActivityLifecycleCallback.register(app)
  }

  override fun setLogLevel(level: Int) {
    when(level) {
      Log.Level.OFF -> CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.OFF)
      Log.Level.ERROR -> CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.INFO)
      Log.Level.INFO -> CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.INFO)
      Log.Level.DEBUG -> CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.DEBUG)
      else -> Unit
    }
  }
}
