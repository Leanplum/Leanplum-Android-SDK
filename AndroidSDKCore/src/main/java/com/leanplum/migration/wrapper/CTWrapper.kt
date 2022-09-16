package com.leanplum.migration.wrapper

import android.app.Application
import android.content.Context
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.leanplum.internal.Log
import com.leanplum.migration.Constants
import com.leanplum.migration.push.FcmMigrationHandler
import com.leanplum.migration.push.HmsMigrationHandler
import com.leanplum.migration.push.MiPushMigrationHandler

internal class CTWrapper(
  private val accountId: String,
  private val accountToken: String,
  private val accountRegion: String,
  private var deviceId: String,
  private var userId: String?
) : IWrapper by StaticMethodsWrapper {

  override val fcmHandler: FcmMigrationHandler = FcmMigrationHandler()
  override val hmsHandler: HmsMigrationHandler = HmsMigrationHandler()
  override val miPushHandler: MiPushMigrationHandler = MiPushMigrationHandler()

  var cleverTapInstance: CleverTapAPI? = null

  /**
   * Needs to be calculated each time.
   */
  private fun cleverTapId(): String {
    return when (userId) {
      null -> deviceId
      deviceId -> deviceId
      else -> "${deviceId}_${userId}"
    }
  }

  /**
   * Needs to be calculated each time.
   */
  private fun identity(): String {
    return when (val userId = userId) {
      null -> deviceId
      deviceId -> deviceId
      else -> userId
    }
  }

  override fun launch(context: Context) {
    val config = CleverTapInstanceConfig.createInstance(
      context,
      accountId,
      accountToken,
      accountRegion).apply {
      // staging = 18 // staging 0 for prod
      enableCustomCleverTapId = true
    }

    val cleverTapId = cleverTapId()
    val identity = identity()
    Log.d("Wrapper: using CleverTapID=__h$cleverTapId and Identity=$identity")

    cleverTapInstance = CleverTapAPI.instanceWithConfig(context, config, cleverTapId).apply {
      setLibrary("Leanplum")
      if (!ActivityLifecycleCallback.registered) {
        ActivityLifecycleCallback.register(context.applicationContext as? Application)
      }
      val profile: Map<String, Any> = mutableMapOf(Constants.IDENTITY to identity)
      onUserLogin(profile, cleverTapId)
      Log.d("Wrapper: CleverTap instance created by Leanplum")
    }
  }

  override fun setUserId(userId: String?) {
    if (userId == null) return
    this.userId = userId

    val cleverTapId = cleverTapId()
    val identity = identity()
    val profile: Map<String, Any> = mutableMapOf(Constants.IDENTITY to identity)

    Log.d("Wrapper: Leanplum.setUserId will call onUserLogin with $profile and __h$cleverTapId")
    cleverTapInstance?.onUserLogin(profile, cleverTapId)
  }

  override fun setDeviceId(deviceId: String?) {
    if (deviceId == null) return
    this.deviceId = deviceId

    val cleverTapId = cleverTapId()
    val identity = identity()
    val profile: Map<String, Any> = mutableMapOf(Constants.IDENTITY to identity)

    Log.d("Wrapper: Leanplum.setDeviceId will call onUserLogin with $profile and __h$cleverTapId")
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

    Log.d("Wrapper: Leanplum.track will call pushEvent with $eventName and $eventProperties")
    cleverTapInstance?.pushEvent(eventName, eventProperties)
  }

  override fun advance(stateName: String?, info: String?, params: Map<String, Any>?) {
    if (stateName == null) return;

    val eventName = Constants.STATE_PREFIX + stateName
    val eventProperties = params?.toMutableMap()

    // TODO handle info ?

    Log.d("Wrapper: Leanplum.advance will call pushEvent with $eventName and $eventProperties")
    cleverTapInstance?.pushEvent(eventName, eventProperties)
  }

  override fun setUserAttributes(attributes: Map<String, Any>?) {
    val profile = attributes?.toMutableMap() ?: return

    Log.d("Wrapper: Leanplum.setUserAttributes will call pushProfile with $profile")
    cleverTapInstance?.pushProfile(profile)
  }

}
