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

package com.leanplum.actions.internal

/**
 * Implementation of action queue. All methods are synchronized using the instance monitor.
 */
class ActionQueue {

  @Volatile
  var queue: MutableList<Action> = mutableListOf()

  @Synchronized
  fun pushFront(action: Action) = queue.add(0, action)

  @Synchronized
  fun pushFront(actions: List<Action>) = actions.reversed().forEach { pushFront(it) }

  @Synchronized
  fun pushBack(action: Action) = queue.add(action)

  @Synchronized
  fun pushBack(actions: List<Action>) = actions.forEach { pushBack(it) }

  @Synchronized
  fun pop() = if (queue.isNotEmpty()) queue.removeAt(0) else null

  @Synchronized
  fun popAll(): List<Action> {
    val all = mutableListOf<Action>()
    while (queue.isNotEmpty()) {
      pop()?.also { all.add(it) }
    }
    return all
  }

  @Synchronized
  fun remove(item: Action) = queue.remove(item)

  @Synchronized
  fun first() = if (queue.isNotEmpty()) queue.first() else null

  @Synchronized
  fun last() = if (queue.isNotEmpty()) queue.last() else null

  @Synchronized
  fun empty() = queue.isEmpty()

  @Synchronized
  override fun toString(): String {
    return queue.toString()
  }
}
