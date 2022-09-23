package com.leanplum.migration.wrapper

import android.app.Application
import android.content.Context
import android.text.TextUtils
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.leanplum.internal.Constants
import com.leanplum.internal.LeanplumInternal
import com.leanplum.internal.Log
import com.leanplum.migration.MigrationConstants
import com.leanplum.migration.MigrationManager
import com.leanplum.migration.push.FcmMigrationHandler
import com.leanplum.migration.push.HmsMigrationHandler
import com.leanplum.migration.push.MiPushMigrationHandler
import com.leanplum.utils.StringPreferenceNullable

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
   * Anonymous data will be merged to first user that logs-in, but CT ID will remain the same as
   * the anonymous' deviceId to allow the merge.
   */
  var firstLoginUserId: String? by StringPreferenceNullable("ct_first_login_userid")
  var firstLoginDeviceId: String? by StringPreferenceNullable("ct_first_login_deviceid")

  /**
   * Needs to be calculated each time.
   */
  private fun cleverTapId(): String {
    return when (userId) {
      null -> deviceId
      deviceId -> deviceId
      firstLoginUserId -> firstLoginDeviceId ?: deviceId
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

  private fun isAnonymous() = userId == deviceId

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
    Log.d("Wrapper: using CleverTapID=__h$cleverTapId")

    cleverTapInstance = CleverTapAPI.instanceWithConfig(context, config, cleverTapId).apply {
      setLibrary("Leanplum")
      if (!ActivityLifecycleCallback.registered) {
        ActivityLifecycleCallback.register(context.applicationContext as? Application)
      }
      if (isAnonymous()) {
        Log.d("Wrapper: identity not set for anonymous user")
      } else {
        val profile: Map<String, Any> = mutableMapOf(MigrationConstants.IDENTITY to identity)
        Log.d("Wrapper: will call onUserLogin with $profile and __h$cleverTapId")
        onUserLogin(profile, cleverTapId)
      }
      Log.d("Wrapper: CleverTap instance created by Leanplum")
    }
  }

  override fun setUserId(userId: String?) {
    if (TextUtils.isEmpty(userId)) return
    if (this.userId == userId) return

    val wasAnonymous = isAnonymous()
    this.userId = userId

    val cleverTapId = cleverTapId()
    val identity = identity()
    val profile: Map<String, Any> = mutableMapOf(MigrationConstants.IDENTITY to identity)

    if (wasAnonymous) {
      firstLoginUserId = userId
      firstLoginDeviceId = deviceId
      Log.d("Wrapper: anonymous data will be merged to $firstLoginUserId")
      Log.d("Wrapper: Leanplum.setUserId will call onUserLogin with $profile")
      cleverTapInstance?.onUserLogin(profile)
    } else {
      Log.d("Wrapper: Leanplum.setUserId will call onUserLogin with $profile and __h$cleverTapId")
      cleverTapInstance?.onUserLogin(profile, cleverTapId)
    }
  }

  override fun setDeviceId(deviceId: String?) {
    if (deviceId == null) return
    this.deviceId = deviceId

    val cleverTapId = cleverTapId()
    val identity = identity()
    val profile: Map<String, Any> = mutableMapOf(MigrationConstants.IDENTITY to identity)

    Log.d("Wrapper: Leanplum.setDeviceId will call onUserLogin with $profile and __h$cleverTapId")
    cleverTapInstance?.onUserLogin(profile, cleverTapId)
  }

  override fun track(
    event: String?,
    value: Double,
    info: String?,
    params: Map<String, Any?>? // TODO validate params against CT or test not valid parameter data?
  ) {
    if (Constants.isNoop()) return
    if (event == null) return

    val properties = params?.toMutableMap() ?: mutableMapOf()

    if (value != 0.0) {
      properties[MigrationConstants.VALUE_PARAM] = value
    }
    if (info != null) {
      properties[MigrationConstants.INFO_PARAM] = info
    }

    LeanplumInternal.addStartIssuedHandler {
      Log.d("Wrapper: Leanplum.track will call pushEvent with $event and $properties")
      cleverTapInstance?.pushEvent(event, properties)
    }
  }

  override fun trackPurchase(
    event: String,
    value: Double,
    currencyCode: String?,
    params: Map<String, Any?>?
  ) {
    if (Constants.isNoop()) return

    val details = HashMap<String, Any?>().apply {
      this[MigrationConstants.CHARGED_EVENT_PARAM] = event
      this[MigrationConstants.VALUE_PARAM] = value
      if (currencyCode != null) {
        this[MigrationConstants.CURRENCY_CODE_PARAM] = currencyCode
      }
    }

    val items = arrayListOf<HashMap<String, Any?>>().apply {
      if (params != null) {
        add(HashMap(params))
      }
    }

    LeanplumInternal.addStartIssuedHandler {
      Log.d("Wrapper: Leanplum.trackPurchase will call pushChargedEvent with $details and $items")
      cleverTapInstance?.pushChargedEvent(details, items)
    }
  }

  override fun trackGooglePlayPurchase(
    event: String,
    item: String?,
    value: Double,
    currencyCode: String?,
    purchaseData: String?,
    dataSignature: String?,
    params: Map<String, Any?>?
  ) {
    if (Constants.isNoop()) return

    val details = HashMap<String, Any?>().apply {
      this[MigrationConstants.CHARGED_EVENT_PARAM] = event
      this[MigrationConstants.VALUE_PARAM] = value
      this[MigrationConstants.CURRENCY_CODE_PARAM] = currencyCode
      this[MigrationConstants.GP_PURCHASE_DATA_PARAM] = purchaseData
      this[MigrationConstants.GP_PURCHASE_DATA_SIGNATURE_PARAM] = dataSignature
    }

    val items = arrayListOf<HashMap<String, Any?>>().apply {
      if (params != null) {
        add(HashMap(params).apply {
          this[MigrationConstants.IAP_ITEM_PARAM] = item
        })
      } else {
        add(hashMapOf(MigrationConstants.IAP_ITEM_PARAM to item))
      }
    }

    LeanplumInternal.addStartIssuedHandler {
      Log.d("Wrapper: Leanplum.trackGooglePlayPurchase will call pushChargedEvent with $details and $items")
      cleverTapInstance?.pushChargedEvent(details, items)
    }
  }

  override fun advanceTo(state: String?, info: String?, params: Map<String, Any?>?) {
    if (state == null) return;

    val event = MigrationConstants.STATE_PREFIX + state
    val properties = params?.toMutableMap() ?: mutableMapOf()

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
      .mapKeys { MigrationManager.mapAttributeName(it) }

    Log.d("Wrapper: Leanplum.setUserAttributes will call pushProfile with $profile")
    cleverTapInstance?.pushProfile(profile)

    attributes
      .filterValues { value -> value == null}
      .mapKeys { MigrationManager.mapAttributeName(it) }
      .forEach {
        Log.d("Wrapper: Leanplum.setUserAttributes will call removeValueForKey with ${it.key}")
        cleverTapInstance?.removeValueForKey(it.key)
      }
  }

}
