package com.leanplum.utils

import android.annotation.SuppressLint
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.Task

object CTUtils {

  @SuppressLint("RestrictedApi")
  fun ensureLocalDataStoreValue(key: String, cleverTapApi: CleverTapAPI) {
    val value = cleverTapApi.coreState.localDataStore.getProfileProperty(key)
    if (value == null) {
      cleverTapApi.coreState.localDataStore.updateProfileFields(mapOf(key to ""))
    }
  }

  @SuppressLint("RestrictedApi")
  fun addMultiValueForKey(key: String, value: String, cleverTapApi: CleverTapAPI) {
    CTExecutorFactory
      .executors(cleverTapApi.coreState.config)
      .postAsyncSafelyTask<Task<Void>>()
      .execute("CTUtils") {
        ensureLocalDataStoreValue(key, cleverTapApi)
        cleverTapApi.addMultiValueForKey(key, value)
        null
      }
  }
}
