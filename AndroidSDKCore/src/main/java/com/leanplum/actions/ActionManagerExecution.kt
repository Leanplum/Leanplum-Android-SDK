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

import androidx.annotation.UiThread
import com.leanplum.Leanplum
import com.leanplum.callbacks.VariablesChangedCallback
import com.leanplum.internal.*

fun ActionManager.appendAction(action: Action) {
  if (isEnabled) {
    queue.pushBack(action)
    performActions()
  }
}

fun ActionManager.appendActions(actions: List<Action>) {
  if (isEnabled) {
    queue.pushBack(actions)
    performActions()
  }
}

fun ActionManager.insertAction(action: Action) {
  if (isEnabled) {
    queue.pushFront(action)
    performActions()
  }
}

fun ActionManager.insertActions(actions: List<Action>) {
  if (isEnabled) {
    queue.pushFront(actions)
    performActions()
  }
}

/**
 * Uses UI thread for synchronized access.
 * Uses pending downloads handler to assure no files are missing for actions.
 */
fun ActionManager.performActions() {
  Log.d("[ActionManager]: performing all available actions.")
  Leanplum.addOnceVariablesChangedAndNoDownloadsPendingHandler(object: VariablesChangedCallback() {
    override fun variablesChanged() {
      if (Util.isMainThread()) {
        performActionsImpl()
      } else {
        OperationQueue.sharedInstance().addUiOperation { performActionsImpl() }
      }
    }
  })
}

@UiThread // TODO make other synchronisation other than UI thread?
private fun ActionManager.performActionsImpl() {
  if (isPaused) return

  // TODO check if queue.top() is the open of a push and dismiss currentAction (check flag too)
  // do not run if we have current action running
  if (currentAction != null) return

  // gets the next action from the queue
  currentAction = queue.pop() ?: return

  with (currentAction.context) {
    Log.d("[ActionManager]: running action with name: ${this}.")

    // decide if we are going to display the message
    // by calling delegate and let it decide what we are supposed to do
    val displayDecision = messageDisplayController?.shouldDisplayMessage(this)
    when (displayDecision?.type) {
      // if message is discarded, early exit
      MessageDisplayChoice.Type.DISCARD -> {
        currentAction = null
        return@with
      }

      // if message is delayed, add it to the scheduler to be delayed
      // by the amount of seconds, and exit
      MessageDisplayChoice.Type.DELAY -> {
        Log.d("[ActionManager]: delaying action: ${this} for ${displayDecision.delaySeconds}s.")
        if (displayDecision.delaySeconds > 0) {
          // Schedule for delayed time
          scheduler.schedule(currentAction, displayDecision.delaySeconds)
        } else {
          // Insert in delayed queue
          delayedQueue.pushBack(currentAction)
        }
        currentAction = null
        return@with
      }

      else -> Unit
    }

    // logic:
    // 1) ask client to show view controller
    // 2) ask and wait for client to execute action
    // 3) ask and wait for client to dismiss view controller

    // get the action definition
    val definition = definitions.actionDefinitions.firstOrNull {
      it.name == this.actionName()
    }

    // 3) set dismiss block
    this.setActionDidDismiss {
      Log.d("[ActionManager]: actionDidDismiss: ${this}.")
      messageDisplayListener?.onMessageDismissed(this)
      currentAction = null // stop executing current action
      performActions()
    }

    // 2) set the action block
    this.setActionDidExecute { actionName ->
      Log.d("[ActionManager]: actionDidExecute: ${this}.")
      messageDisplayListener?.onActionExecuted(actionName, this)
    }

    // 1) ask to present, return if it's not
    val presented: Boolean = definition?.presentHandler?.onResponse(this) ?: false
    if (!presented) {
      Log.d("[ActionManager]: action NOT presented: ${this}.")
      currentAction = null
      return@with
    }

    // invoke registered onAction handlers
    definitions.handlers[this.actionName()]?.forEach {
      OperationQueue.sharedInstance().addUiOperation {
        it.onResponse(this)
      }
    }

    if (currentAction != null) {
      recordImpression(currentAction)
    }

    Log.i("[ActionManager]: action presented: ${this}.")
    // propagate event that message is displayed
    messageDisplayListener?.onMessageDisplayed(this)
  }

  performActions()
}

private fun ActionManager.recordImpression(action: Action) {
  try {
    val ctx = action.context
    if (action.actionType == Action.ActionType.CHAINED) {
      // We do not want to count occurrences for action kind, because in multi message
      // campaigns the Open URL action is not a message. Also if the user has defined
      // actions of type Action we do not want to count them.
      val actionKind = definitions.findDefinition(ctx.actionName())?.kind
      if (actionKind == Leanplum.ACTION_KIND_ACTION) {
        recordChainedActionImpression(ctx.messageId)
      } else {
        recordMessageImpression(ctx.messageId)
      }
      Leanplum.triggerMessageDisplayed(ctx) // TODO remove triggerMessageDisplayed handlers?

    } else {
      recordMessageImpression(action.context.messageId)
      Leanplum.triggerMessageDisplayed(ctx)
    }
  } catch (t: Throwable) {
    Log.exception(t)
  }
}
