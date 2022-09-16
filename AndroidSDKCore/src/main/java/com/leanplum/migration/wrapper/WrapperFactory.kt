package com.leanplum.migration.wrapper

import com.leanplum.Leanplum
import com.leanplum.internal.Log
import com.leanplum.migration.MigrationManager
import com.leanplum.migration.model.MigrationConfig

internal object WrapperFactory {

  fun createWrapper(): IWrapper {
    val account = MigrationConfig.accountId
    val token = MigrationConfig.accountToken
    val region = MigrationConfig.accountRegion
    if (account == null || token == null || region == null) {
      return NullWrapper
    }

    val context = Leanplum.getContext()
    val deviceId = Leanplum.getDeviceId()
    val userId = Leanplum.getUserId()
    //Log.e("deviceId=$deviceId and userId=$userId")
    //Log.d(Exception().stackTraceToString())
    if (context == null || deviceId == null) {
      // Leanplum state is not fully initialised, thus giving access only to CT static methods.
      return StaticMethodsWrapper
    }

    return if (MigrationManager.getState().useCleverTap()) {
      CTWrapper(account, token, region, deviceId, userId).apply {
        launch(context)
      }
    } else {
      NullWrapper
    }
  }

  fun createNullWrapper(): IWrapper {
    return NullWrapper
  }
}
