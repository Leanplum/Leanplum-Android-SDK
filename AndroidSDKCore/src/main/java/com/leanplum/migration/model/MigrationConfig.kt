package com.leanplum.migration.model

import com.leanplum.utils.StringPreference
import com.leanplum.utils.StringPreferenceNullable

object MigrationConfig {

  var state: String by StringPreference(key = "migration_state", defaultValue = "undefined")

  var hash: String by StringPreference(key = "ct_config_hash", defaultValue = "defaultHash")

  var accountId: String? by StringPreferenceNullable(key = "ct_account_id")

  var accountToken: String? by StringPreferenceNullable(key = "ct_account_token")

  var accountRegion: String? by StringPreferenceNullable(key = "ct_region_code")

  fun update(data: ResponseData) {
    state = data.state
    hash = data.hash
    accountId = data.accountId
    accountToken = data.token
    accountRegion = data.regionCode
  }

}
