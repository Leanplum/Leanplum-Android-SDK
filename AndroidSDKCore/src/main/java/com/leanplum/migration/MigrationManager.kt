package com.leanplum.migration

import com.leanplum.internal.Log
import com.leanplum.internal.Request
import com.leanplum.internal.RequestBuilder
import com.leanplum.internal.RequestSender
import com.leanplum.migration.model.MigrationConfig
import com.leanplum.migration.model.MigrationState
import com.leanplum.migration.wrapper.*
import org.json.JSONObject

// TODO mark all classes as internal ?
// TODO synchronize access ?
object MigrationManager {

  @JvmStatic
  fun getState(): MigrationState = MigrationState.from(MigrationConfig.state)

  @JvmStatic
  var wrapper: IWrapper = NullWrapper
    private set

  private fun createWrapper(): IWrapper {
    val acc = MigrationConfig.accountId
    val token = MigrationConfig.accountToken
    val region = MigrationConfig.accountRegion
    return if (getState().useCleverTap() && acc != null && token != null && region != null) {
      CTWrapper(acc, token, region)
    } else {
      NullWrapper
    }
  }

  @JvmStatic
  fun updateWrapper() {
    if (wrapper == NullWrapper && getState().useCleverTap()) {
      wrapper = createWrapper()
    }
  }

  @JvmStatic
  fun fetchState(callback: (MigrationState) -> Unit) {
    if (getState() != MigrationState.Undefined) {
      callback.invoke(getState())
    } else {
      fetchStateAsync(callback)
    }
  }

  private fun fetchStateAsync(callback: (MigrationState) -> Unit) {
    val request = RequestBuilder
      .withGetMigrateState()
      .andType(Request.RequestType.IMMEDIATE)
      .create()

    request.onError {
      Log.d("Error getting migration state:", it)
      callback.invoke(getState())
    }
    request.onResponse {
      Log.d("Migration state response: $it")
      ResponseHandler().handleMigrateStateContent(it)?.let { responseData ->
        MigrationConfig.update(responseData)
        updateWrapper()
      }
      callback.invoke(getState())
    }
    RequestSender.getInstance().send(request)
  }

  @JvmStatic
  fun handleResponseBody(responseBody: JSONObject): Boolean {
    ResponseHandler().handleMigrateState(responseBody)?.let { responseData ->
      MigrationConfig.update(responseData)
      // TODO handle case to duplicate or CT only, do not handle change of account params
      // wrapper will be launched with new parameters on next SDK start
      return true
    } ?: return false
  }
}
