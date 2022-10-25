package com.leanplum.migration.wrapper

import com.clevertap.android.sdk.Utils
import com.leanplum.utils.HashUtil

private const val DEVICE_ID_MAX_LENGTH = 50

class LPIdentity(
  private val deviceId: String,
  private var userId: String
) {

  private var deviceIdHash: String? = null
  private var userIdHash: String? = null

  init {
    if (deviceId.length > DEVICE_ID_MAX_LENGTH || !Utils.validateCTID(deviceId)) {
      deviceIdHash = HashUtil.sha256_200(deviceId)
    }
    userIdHash = HashUtil.sha256_40(userId)
  }

  fun deviceId() = deviceIdHash ?: deviceId
  fun originalDeviceId() = deviceId
  fun userId() = userIdHash
  fun originalUserId() = userId
  fun isAnonymous() = userId == deviceId

  fun setUserId(userId: String): Boolean {
    if (this.userId == userId) {
      return false
    }
    this.userId = userId
    this.userIdHash = HashUtil.sha256_40(userId)
    return true
  }
}
