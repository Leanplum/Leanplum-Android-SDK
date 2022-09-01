package com.leanplum.migration.model

data class ResponseData(
  val state: String,
  val hash: String,
  val accountId: String? = null,
  val token: String? = null,
  val regionCode: String? = null
)
