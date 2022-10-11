package com.leanplum.migration.wrapper

import com.leanplum.Leanplum
import com.leanplum.callbacks.CleverTapInstanceCallback
import com.leanplum.internal.Constants
import com.leanplum.internal.LeanplumInternal
import com.leanplum.migration.model.MigrationConfig

internal object WrapperFactory {

  fun createWrapper(callback: CleverTapInstanceCallback?): IWrapper {
    val account = MigrationConfig.accountId
    val token = MigrationConfig.accountToken
    val region = MigrationConfig.accountRegion
    if (account == null || token == null || region == null) {
      return NullWrapper
    }

    if (!LeanplumInternal.hasCalledStart()) {
      // giving access only to CT static methods, because Leanplum state is not fully initialised
      return StaticMethodsWrapper
    }

    val context = Leanplum.getContext()
    val deviceId = Leanplum.getDeviceId()
    val userId = Leanplum.getUserId()
    if (context == null || deviceId == null) {
      // giving access only to CT static methods, because Leanplum state is not fully initialised
      return StaticMethodsWrapper
    }

    return CTWrapper(account, token, region, deviceId, userId).apply {
      launch(context, callback)
    }
  }

}
