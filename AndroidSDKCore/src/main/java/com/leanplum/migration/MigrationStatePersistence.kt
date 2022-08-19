package com.leanplum.migration

import com.leanplum.Leanplum
import com.leanplum.utils.getLeanplumPrefs
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class MigrationStatePersistence(
  private val key: String
) : ReadWriteProperty<Any, MigrationState> {

  var state: MigrationState? = null

  @Synchronized
  override fun getValue(thisRef: Any, property: KProperty<*>): MigrationState {
    return state ?: run {
      val prefs = Leanplum.getContext()?.getLeanplumPrefs() ?: return MigrationState.Undefined
      val persistedValue = prefs.getString(key, null) ?: MigrationState.Undefined.toString()
      val parsedValue = try {
        MigrationState.valueOf(persistedValue)
      } catch (t: Throwable) {
        MigrationState.Undefined
      }
      state = parsedValue
      parsedValue
    }
  }

  @Synchronized
  override fun setValue(thisRef: Any, property: KProperty<*>, value: MigrationState) {
    if (state != value) {
      state = value
      val prefs = Leanplum.getContext()?.getLeanplumPrefs() ?: return
      prefs.edit().putString(key, value.toString()).apply()
    }
  }
}
