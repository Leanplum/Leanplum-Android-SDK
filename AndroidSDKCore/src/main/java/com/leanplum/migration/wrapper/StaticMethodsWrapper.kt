package com.leanplum.migration.wrapper

import android.app.Application
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.clevertap.android.sdk.CleverTapAPI
import com.leanplum.internal.Log

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

  override fun setLogLevel(level: Int) {
    when(level) {
      Log.Level.OFF -> CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.OFF)
      Log.Level.ERROR -> CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.INFO)
      Log.Level.INFO -> CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.INFO)
      Log.Level.DEBUG -> CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.VERBOSE)
      else -> Unit
    }
  }

}
