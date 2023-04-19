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

import com.leanplum.ActionContext
import com.leanplum.actions.LeanplumActions
import com.leanplum.internal.ActionManager
import com.leanplum.internal.Log
import com.leanplum.internal.OperationQueue
import com.leanplum.internal.Util

fun ActionManager.trigger(
  context: ActionContext,
  priority: Priority = Priority.DEFAULT) {

  if (LeanplumActions.useWorkerThreadForDecisionHandlers) {
    OperationQueue.sharedInstance().addActionOperation {
      triggerImpl(listOf(context), priority)
    }
  } else {
    triggerImpl(listOf(context), priority)
  }
}

fun ActionManager.trigger(
  contexts: List<ActionContext>,
  priority: Priority = Priority.DEFAULT,
  trigger: ActionsTrigger? = null) {

  if (LeanplumActions.useWorkerThreadForDecisionHandlers) {
    OperationQueue.sharedInstance().addActionOperation {
      triggerImpl(contexts, priority, trigger)
    }
  } else {
    triggerImpl(contexts, priority, trigger)
  }
}

private fun ActionManager.triggerImpl(
  contexts: List<ActionContext>,
  priority: Priority = Priority.DEFAULT,
  trigger: ActionsTrigger? = null) {

  if (contexts.isEmpty()) return

  // By default, add only one message to queue if `prioritizeMessages` is not implemented
  // This ensures backwards compatibility
  val orderedContexts =
    messageDisplayController?.prioritizeMessages(contexts, trigger) ?: listOf(contexts.first())

  val actions: List<Action> = orderedContexts.map { context -> Action.create(context) }

  Log.d("[ActionManager][${Util.getThread()}]: triggering with priority: ${priority.name} and actions: $orderedContexts")

  when (priority) {
    Priority.HIGH -> insertActions(actions)
    Priority.DEFAULT -> appendActions(actions)
  }
}

fun ActionManager.triggerDelayedMessages() {
  appendActions(delayedQueue.popAll())
}
