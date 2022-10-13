/*
 * Copyright 2022, Leanplum, Inc. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.leanplum.utils

import com.leanplum.Leanplum
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class StringPreferenceNullable(
  private val key: String,
  private val defaultValue: String? = null
) : ReadWriteProperty<Any, String?> {

  override fun getValue(thisRef: Any, property: KProperty<*>): String? {
    val prefs = Leanplum.getContext()?.getLeanplumPrefs() ?: return defaultValue
    return prefs.getString(key, defaultValue)
  }

  override fun setValue(thisRef: Any, property: KProperty<*>, value: String?) {
    val prefs = Leanplum.getContext()?.getLeanplumPrefs() ?: return
    prefs.edit().putString(key, value).apply()
  }
}
