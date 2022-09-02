package com.leanplum.migration.wrapper

/**
 * Purpose of this singleton is to be used instead of null value to avoid checking for null in Java.
 */
internal object NullWrapper : IWrapper
