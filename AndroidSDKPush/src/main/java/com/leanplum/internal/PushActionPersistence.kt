/*
 * Copyright 2021, Leanplum, Inc. All rights reserved.
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

package com.leanplum.internal

import android.content.Context
import com.leanplum.Leanplum
import com.leanplum.internal.Constants.Defaults

private const val PREF_KEY = "__leanplum_push_open_actions"

private val records: MutableMap<String, Long> by lazy {
    val saved = load()
    val youngerThanMonth = saved.filterValues { Clock.getInstance().lessThanMonthAgo(it) }

    if (youngerThanMonth.size < saved.size) {
        save(youngerThanMonth)
    }
    youngerThanMonth as MutableMap<String, Long>
}

private fun load(): Map<String, Long> {
    val context = Leanplum.getContext() ?: return mutableMapOf()
    val prefs = context.getSharedPreferences(Defaults.MESSAGING_PREF_NAME, Context.MODE_PRIVATE)
    val savedValue = prefs.getString(PREF_KEY, "{}")
    return CollectionUtil.uncheckedCast(JsonConverter.fromJson(savedValue))
}

private fun save(records: Map<String, Long>) {
    val context = Leanplum.getContext() ?: return
    val prefs = context.getSharedPreferences(Defaults.MESSAGING_PREF_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(PREF_KEY, JsonConverter.toJson(records)).apply()
}

fun recordOpenAction(occurrenceId: String) {
    synchronized(records) {
        records[occurrenceId] = Clock.getInstance().currentTimeMillis()
        save(records)
    }
}

fun isOpened(occurrenceId: String): Boolean {
    synchronized(records) {
        return records.contains(occurrenceId)
    }
}
