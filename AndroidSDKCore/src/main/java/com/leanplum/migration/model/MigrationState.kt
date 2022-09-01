package com.leanplum.migration.model

enum class MigrationState {
  Undefined,
  LeanplumOnly,
  CleverTapOnly,
  Duplicate;

  fun useCleverTap() = when(this) {
    CleverTapOnly -> true
    Duplicate -> true
    else -> false
  }

  companion object {
    @JvmStatic
    fun from(state: String) = when (state) {
      "lp" -> LeanplumOnly
      "ct" -> CleverTapOnly
      "lp+ct" -> Duplicate
      else -> Undefined
    }
  }
}
