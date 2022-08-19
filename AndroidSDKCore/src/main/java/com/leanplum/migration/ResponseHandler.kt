package com.leanplum.migration

import com.leanplum.internal.Constants.Params
import com.leanplum.internal.Log
import org.json.JSONException
import org.json.JSONObject

class ResponseHandler {

  data class Result(
    val state: MigrationState,
    val accountId: String? = null,
    val accountToken: String? = null
  )

  /**
   * TODO provide json example
   */
  fun handleMigrateStateContent(json: JSONObject): Result? {
    try {
      if (!json.isNull(Params.SDK)) {
        val state = parseMigrationState(json.getString(Params.SDK))
        return if (state.useCleverTap()) {
          var accountId: String? = null
          var accountToken: String? = null
          json.optJSONObject(Params.CLEVERTAP)?.apply {
            accountId = optString(Params.CT_ACCOUNT)
            accountToken = optString(Params.CT_TOKEN)
          }
          Result(state, accountId, accountToken)
        } else {
          Result(state)
        }
      }
    } catch (e: JSONException) {
      Log.e("Error parsing response for CT config.", e)
    }
    return null
  }

  private fun parseMigrationState(sdk: String): MigrationState = when(sdk) {
    "lp" -> MigrationState.LeanplumOnly
    "ct" -> MigrationState.CleverTapOnly
    "lp+ct" -> MigrationState.Duplicate
    else -> MigrationState.Undefined
  }

  /**
   * TODO provide json example
   */
  fun handleMigrateState(json: JSONObject): Result? {
    try {
      if (!json.isNull(Params.MIGRATE_STATE)) {
        val contentJson = json.getJSONObject(Params.MIGRATE_STATE)
        return handleMigrateStateContent(contentJson)
      }
    } catch (e: JSONException) {
      Log.e("Error parsing response for CT config.", e)
    }
    return null
  }

}
