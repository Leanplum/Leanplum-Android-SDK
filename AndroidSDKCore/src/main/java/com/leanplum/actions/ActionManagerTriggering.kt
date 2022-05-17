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

package com.leanplum.actions

import com.leanplum.ActionContext
import com.leanplum.internal.ActionManager
import com.leanplum.internal.Log

fun ActionManager.trigger(
  context: ActionContext,
  priority: Priority = Priority.DEFAULT) {

  trigger(listOf(context), priority)
}

fun ActionManager.trigger(
  contexts: List<ActionContext>,
  priority: Priority = Priority.DEFAULT,
  trigger: ActionsTrigger? = null) {

  val orderedContexts = messageDisplayController?.orderMessages(contexts, trigger) ?: contexts
  val actions: List<Action> = orderedContexts.map { context -> Action.create(context) }

  Log.d("[ActionManager]: triggering with priority: ${priority.name} and actions: $orderedContexts")

  when (priority) {
    Priority.HIGH -> insertActions(actions)
    Priority.DEFAULT -> appendActions(actions)
  }
}

fun ActionManager.triggerDelayedMessages() {
  appendActions(delayedQueue.popAll())
}
