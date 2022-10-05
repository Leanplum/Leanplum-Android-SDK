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

  // TODO read from getMigrateState and set false as default
  var trackGooglePlayPurchases = true//false
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
