package com.leanplum.migration.wrapper

import com.leanplum.Leanplum
import com.leanplum.migration.MigrationManager
import com.leanplum.migration.model.MigrationConfig

internal object WrapperFactory {

  fun createWrapper(): IWrapper {
    val account = MigrationConfig.accountId
    val token = MigrationConfig.accountToken
    val region = MigrationConfig.accountRegion

    return if (MigrationManager.getState().useCleverTap()
      && account != null
      && token != null
      && region != null
    ) {
      CTWrapper(account, token, region).apply {
        val context = Leanplum.getContext()
        if (context != null) {
          launch(context, Leanplum.getUserId(), Leanplum.getDeviceId())
        }
      }
    } else {
      NullWrapper
    }
  }

  fun createNullWrapper(): IWrapper {
    return NullWrapper
  }
}
