package com.leanplum.migration

enum class MigrationState {
  Undefined,
  LeanplumOnly,
  CleverTapOnly,
  Duplicate
}

fun MigrationState.useCleverTap() = when(this) {
  MigrationState.CleverTapOnly -> true
  MigrationState.Duplicate -> true
  else -> false
}
