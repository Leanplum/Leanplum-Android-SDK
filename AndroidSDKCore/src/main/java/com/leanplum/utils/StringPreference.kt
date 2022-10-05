package com.leanplum.utils

import com.leanplum.Leanplum
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class StringPreference(
  private val key: String,
  private val defaultValue: String
) : ReadWriteProperty<Any, String> {

  override fun getValue(thisRef: Any, property: KProperty<*>): String {
    val prefs = Leanplum.getContext()?.getLeanplumPrefs() ?: return defaultValue
    return prefs.getString(key, defaultValue) ?: defaultValue
  }

  override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
    val prefs = Leanplum.getContext()?.getLeanplumPrefs() ?: return
    prefs.edit().putString(key, value).apply()
  }
}
