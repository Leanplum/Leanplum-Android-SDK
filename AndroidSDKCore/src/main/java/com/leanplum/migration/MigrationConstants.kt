package com.leanplum.migration

import com.clevertap.android.sdk.CleverTapAPI
import com.leanplum.internal.Log

object MigrationConstants {
  const val IDENTITY = "Identity"
  const val STATE_PREFIX = "state_"

  const val CHARGED_EVENT_PARAM = "event"
  const val VALUE_PARAM = "value"
  const val CURRENCY_CODE_PARAM = "currencyCode"
  const val INFO_PARAM = "info"
  const val GP_PURCHASE_DATA_PARAM = "googlePlayPurchaseData"
  const val GP_PURCHASE_DATA_SIGNATURE_PARAM = "googlePlayPurchaseDataSignature"
  const val IAP_ITEM_PARAM = "item"

  fun mapLogLevel(lpLevel: Int): CleverTapAPI.LogLevel = when (lpLevel) {
    Log.Level.OFF -> CleverTapAPI.LogLevel.OFF
    Log.Level.ERROR -> CleverTapAPI.LogLevel.INFO
    Log.Level.INFO -> CleverTapAPI.LogLevel.INFO
    Log.Level.DEBUG -> CleverTapAPI.LogLevel.VERBOSE
    else -> CleverTapAPI.LogLevel.INFO
  }
}
