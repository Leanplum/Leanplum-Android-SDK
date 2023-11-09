/*
 * Copyright 2022, Leanplum, Inc. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.leanplum.migration

import com.leanplum.Leanplum
import com.leanplum.internal.Constants.Params
import com.leanplum.internal.Log
import com.leanplum.migration.model.MigrationState
import com.leanplum.migration.model.ResponseData
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ResponseHandler {

  /**
   * Example JSON:
   *
   * {
   *   "ct": {
   *     "accountId": "...",
   *     "regionCode": "...",
   *     "attributeMappings": {
   *       "x": "y",
   *       "a": "b"
   *     },
   *     "token": "...",
   *     "identityKeys": [
   *       "Identity",
   *       "Phone"
   *     ]
   *   },
   *   "sha256": "...",
   *   "loggedInUserId": "...",
   *   "success": true,
   *   "profileUploadStartedTs": "...",
   *   "eventsUploadStartedTs": "...",
   *   "state": "EVENTS_UPLOAD_STARTED",
   *   "sdk": "lp+ct",
   *   "api": {
   *     "profile": "lp+ct",
   *     "events": "lp+ct"
   *   },
   *   "reqId": "..."
   * }
   *
   * @return Data parsed from json.
   */
  fun handleMigrateStateContent(json: JSONObject): ResponseData? {
    try {
      if (!json.isNull(Params.SDK)) {
        val state = json.getString(Params.SDK)
        val hash = json.getString(Params.MIGRATE_STATE_HASH)
        val loggedInUserId = if (json.has(Params.LOGGED_IN_USER_ID)) {
          json.getString(Params.LOGGED_IN_USER_ID)
        } else {
          null
        }
        return if (MigrationState.from(state).useCleverTap()) {
          var accountId: String? = null
          var token: String? = null
          var regionCode: String? = null
          var attributeMappings: String? = null
          var identityKeysCsv: String? = null
          json.optJSONObject(Params.CLEVERTAP)?.apply {
            accountId = optString(Params.CT_ACCOUNT_ID)
            token = optString(Params.CT_TOKEN)
            regionCode = optString(Params.CT_REGION_CODE)
            optJSONObject(Params.CT_ATTRIBUTE_MAPPINGS)?.let {
              attributeMappings = it.toString()
            }
            optJSONArray(Params.CT_IDENTITY_KEYS)?.let {
              val keys = mutableListOf<String>()
              for (i in 0 until it.length()) {
                keys += it.optString(i)
              }
              if (keys.isNotEmpty()) {
                identityKeysCsv = keys.joinToString(separator = ",")
              }
            }
          }
          ResponseData(state, hash, loggedInUserId, accountId, token, regionCode, attributeMappings, identityKeysCsv)
        } else {
          ResponseData(state, hash, loggedInUserId)
        }
      }
    } catch (e: JSONException) {
      Log.e("Error parsing response for CT config.", e)
    }
    return null
  }

  /**
   * Example JSON:
   *
   * "migrateState": {
   *   "sha256": "5f3e3640f0dc5a8e147294d2b06f63dc63f7c75dd39d050b2f105b5586620b1a"
   * }
   *
   * @return Returns the sha256 hash from the JSON.
   *
   */
  fun handleMigrateState(json: JSONObject): String? {
    try {
      if (!json.isNull(Params.MIGRATE_STATE)) {
        val content = json.getJSONObject(Params.MIGRATE_STATE)
        return content.getString(Params.MIGRATE_STATE_HASH)
      }
    } catch (e: JSONException) {
      Log.e("Error parsing response for CT config.", e)
    }
    return null
  }

}
