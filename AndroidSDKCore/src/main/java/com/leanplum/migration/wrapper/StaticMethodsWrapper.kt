package com.leanplum.migration.wrapper

import android.app.Application
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.leanplum.internal.Log
import com.leanplum.migration.MigrationConstants

/**
 * Singleton wrapping the static utility methods of CT SDK, used as a delegate from [CTWrapper].
 *
 * It is initialised at the first possible moment to give access for some utility methods. When
 * Leanplum SDK is initialised a [CTWrapper] instance will be used.
 */
object StaticMethodsWrapper : IWrapper {

  override fun registerLifecycleCallback(app: Application) {
    ActivityLifecycleCallback.register(app)
  }

  override fun setLogLevel(lpLevel: Int) {
    CleverTapAPI.setDebugLevel(MigrationConstants.mapLogLevel(lpLevel))
  }

}
