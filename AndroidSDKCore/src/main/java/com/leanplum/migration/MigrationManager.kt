package com.leanplum.migration

import com.leanplum.internal.Log
import com.leanplum.internal.Request
import com.leanplum.internal.RequestBuilder
import com.leanplum.internal.RequestSender
import com.leanplum.migration.wrapper.*
import com.leanplum.utils.StringPreference
import org.json.JSONObject

// TODO mark all classes as internal ?
// TODO synchronize access ?
object MigrationManager {

  @JvmStatic
  var state: MigrationState by MigrationStatePersistence(key = "migration_state")
    private set

  private var accountId: String? by StringPreference(key = "ct_account_id")
  private var accountToken: String? by StringPreference(key = "ct_account_token")

  @JvmStatic
  var wrapper: IWrapper = NullWrapper
    private set

  private fun createWrapper(): IWrapper {
    val acc = accountId
    val token = accountToken
    return if (state.useCleverTap() && acc != null && token != null) {
      CTWrapper(acc, token)
    } else {
      NullWrapper
    }
  }

  @JvmStatic
  fun updateWrapper() {
    if (wrapper == NullWrapper && state.useCleverTap()) {
      wrapper = createWrapper()
    }
  }

  private fun setState(state: MigrationState, accountId: String?, accountToken: String?) {
    MigrationManager.state = state
    MigrationManager.accountId = accountId
    MigrationManager.accountToken = accountToken
  }

  @JvmStatic
  fun fetchState(callback: (MigrationState) -> Unit) {
    if (state != MigrationState.Undefined) {
      callback.invoke(state)
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
      Log.e("Migration state onError")
      callback.invoke(state)
    }
    request.onResponse {
      Log.e("Migration state onResponse: $it")
      ResponseHandler().handleMigrateStateContent(it)?.let { result ->
        setState(result.state, result.accountId, result.accountToken)
        updateWrapper()
      }
      callback.invoke(state)
    }
    RequestSender.getInstance().send(request)
  }

  @JvmStatic
  fun handleResponseBody(responseBody: JSONObject): Boolean {
    ResponseHandler().handleMigrateState(responseBody)?.let {
      setState(it.state, it.accountId, it.accountToken)
      // wrapper will be launched with new parameters on next SDK start
      return true
    } ?: return false
  }
}
