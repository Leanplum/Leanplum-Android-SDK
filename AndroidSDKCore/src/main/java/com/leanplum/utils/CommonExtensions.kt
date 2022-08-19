package com.leanplum.utils

import android.content.Context
import android.content.SharedPreferences
import com.leanplum.internal.Constants

internal fun Context.getLeanplumPrefs(): SharedPreferences? =
  getSharedPreferences(Constants.Defaults.LEANPLUM, Context.MODE_PRIVATE)
