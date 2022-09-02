package com.leanplum.migration.wrapper

import com.leanplum.Leanplum
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
    val userId = Leanplum.getUserId() // userId is null for anonymous users
    val deviceId = Leanplum.getDeviceId()
    if (context == null || deviceId == null) {
      // Leanplum state is not fully initialised, thus giving access only to CT static methods.
      return StaticMethodsWrapper
    }

    return if (MigrationManager.getState().useCleverTap()) {
      CTWrapper(account, token, region).apply {
        launch(context, userId, deviceId)
      }
    } else {
      NullWrapper
    }
  }

  fun createNullWrapper(): IWrapper {
    return NullWrapper
  }
}
