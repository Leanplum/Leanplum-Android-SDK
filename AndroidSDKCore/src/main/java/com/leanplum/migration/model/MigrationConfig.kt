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

package com.leanplum.migration.model

import com.leanplum.internal.CollectionUtil
import com.leanplum.internal.JsonConverter
import com.leanplum.utils.StringPreference
import com.leanplum.utils.StringPreferenceNullable

object MigrationConfig {

  var state: String by StringPreference(key = "migration_state", defaultValue = "undefined")
    private set

  var hash: String by StringPreference(key = "ct_config_hash", defaultValue = "defaultHash")
    private set

  var accountId: String? by StringPreferenceNullable(key = "ct_account_id")
    private set

  var accountToken: String? by StringPreferenceNullable(key = "ct_account_token")
    private set

  var accountRegion: String? by StringPreferenceNullable(key = "ct_region_code")
    private set

  private var attributeMappings: String? by StringPreferenceNullable(key = "ct_attribute_mappings")
  var attributeMap: Map<String, String>? = null
    private set
    get() {
      if (field == null) {
        field = CollectionUtil.uncheckedCast(JsonConverter.fromJson(attributeMappings)) ?: mapOf()
      }
      return field
    }

  var trackGooglePlayPurchases = true
    private set

  fun update(data: ResponseData) {
    state = data.state
    hash = data.hash
    accountId = data.accountId
    accountToken = data.token
    accountRegion = data.regionCode
    attributeMappings = data.attributeMappings.also {
      attributeMap = null
    }
  }

}
