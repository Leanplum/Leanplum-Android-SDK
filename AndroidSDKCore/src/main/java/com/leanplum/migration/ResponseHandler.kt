package com.leanplum.migration

import com.leanplum.internal.Constants.Params
import com.leanplum.internal.Log
import com.leanplum.migration.model.MigrationState
import com.leanplum.migration.model.ResponseData
import org.json.JSONException
import org.json.JSONObject

class ResponseHandler {

  /**
   * TODO provide json example
   */
  fun handleMigrateStateContent(json: JSONObject): ResponseData? {
    try {
      if (!json.isNull(Params.SDK)) {
        val state = json.getString(Params.SDK)
        val hash = json.getString(Params.MIGRATE_STATE_HASH)
        return if (MigrationState.from(state).useCleverTap()) {
          var accountId: String? = null
          var token: String? = null
          var regionCode: String? = null
          json.optJSONObject(Params.CLEVERTAP)?.apply {
            accountId = optString(Params.CT_ACCOUNT_ID)
            token = optString(Params.CT_TOKEN)
            regionCode = optString(Params.CT_REGION_CODE)
          }
          ResponseData(state, hash, accountId, token, regionCode)
        } else {
          ResponseData(state, hash)
        }
      }
    } catch (e: JSONException) {
      Log.e("Error parsing response for CT config.", e)
    }
    return null
  }

  /**
   * TODO provide json example
   */
  fun handleMigrateState(json: JSONObject): ResponseData? {
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
