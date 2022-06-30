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
import com.leanplum.internal.ActionManager

data class Action(
  val actionType: ActionType = ActionType.SINGLE,
  val context: ActionContext) {

  fun isNotification(): Boolean {
    return context.parentContext?.actionName() == ActionManager.PUSH_NOTIFICATION_ACTION_NAME
  }

  companion object {
    @JvmStatic
    fun create(context: ActionContext): Action {
      if (context.parentContext != null && !context.isChainedMessage) {
        return Action(actionType = ActionType.EMBEDDED, context = context)
      }

      if (context.isChainedMessage) {
        return Action(actionType = ActionType.CHAINED, context = context)
      }

      return Action(context = context)
    }
  }

  enum class ActionType {
    SINGLE, // Default action
    CHAINED, // Chained to existing action
    EMBEDDED // Embedded inside existing action
  }
}

enum class Priority {
  HIGH,
  DEFAULT
}

data class ActionsTrigger(
  val eventName: String?,
  val condition: List<String>?,
  val contextualValues: ActionContext.ContextualValues?
)

/**
 * Called from ActionContext.runActionNamed
 * Implemented in ActionManagerExecution
 */
fun interface ActionDidDismiss {
  fun onDismiss()
}

/**
 * Called from ActionContext.runActionNamed
 * Implemented in ActionManagerExecution
 */
fun interface ActionDidExecute {
  fun onActionExecuted(context: ActionContext)
}
