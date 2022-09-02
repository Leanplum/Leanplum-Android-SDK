package com.leanplum.migration.model

enum class MigrationState {
  Undefined,
  LeanplumOnly,
  CleverTapOnly,
  Duplicate;

  fun useLeanplum() = when (this) {
    Undefined, LeanplumOnly, Duplicate -> true
    else -> false
  }

  fun useCleverTap() = when (this) {
    CleverTapOnly, Duplicate -> true
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
