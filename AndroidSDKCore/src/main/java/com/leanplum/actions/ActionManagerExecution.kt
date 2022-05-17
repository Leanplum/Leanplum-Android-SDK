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

/**
 * Checks if next action comes from notification click and dismisses current one.
 */
private fun ActionManager.prioritizePushNotificationActions() {
  if (!dismissOnPushOpened) return

  val nextAction = queue.first()
  if (nextAction != null && nextAction.isNotification()) { // TODO test isNotification for campaign payload (messageId)
    val currentContext = currentAction.context
    val definition = definitions.findDefinition(currentContext.actionName())

    definition?.dismissHandler?.also {
      Log.d("[ActionManager]: dismiss requested for: ${currentContext}.")
      it.onResponse(currentContext)
    }
  }
}

@UiThread // TODO make other synchronisation other than UI thread?
private fun ActionManager.performActionsImpl() {
  if (isPaused) return

  // do not continue if we have action running
  if (currentAction != null) {
    prioritizePushNotificationActions()
    return
  }

  // gets the next action from the queue
  currentAction = queue.pop() ?: return

  val currentContext = currentAction.context
  Log.d("[ActionManager]: action popped from queue: ${currentContext}.")

  // decide if we are going to display the message
  // by calling delegate and let it decide what we are supposed to do
  val displayDecision = messageDisplayController?.shouldDisplayMessage(currentContext)
  when (displayDecision?.type) {
    // if message is discarded, early exit
    MessageDisplayChoice.Type.DISCARD -> {
      currentAction = null
      performActions()
      return
    }

    // if message is delayed, add it to the scheduler to be delayed
    // by the amount of seconds, and exit
    MessageDisplayChoice.Type.DELAY -> {
      Log.d("[ActionManager]: delaying action: ${currentContext} for ${displayDecision.delaySeconds}s.")
      if (displayDecision.delaySeconds > 0) {
        // Schedule for delayed time
        scheduler.schedule(currentAction, displayDecision.delaySeconds)
      } else {
        // Insert in delayed queue
        delayedQueue.pushBack(currentAction)
      }
      currentAction = null
      performActions()
      return
    }

    else -> Unit
  }

  // logic:
  // 1) ask client to show view controller
  // 2) ask and wait for client to execute action
  // 3) ask and wait for client to dismiss view controller

  // get the action definition
  val definition = definitions.findDefinition(currentContext.actionName())

  // 3) set dismiss block
  currentContext.setActionDidDismiss {
    Log.d("[ActionManager]: actionDidDismiss: ${currentContext}.")
    messageDisplayListener?.onMessageDismissed(currentContext)
    currentAction = null // stop executing current action
    performActions()
  }

  // 2) set the action block
  currentContext.setActionDidExecute { actionName ->
    Log.d("[ActionManager]: actionDidExecute: ${currentContext}.")
    messageDisplayListener?.onActionExecuted(actionName, currentContext)
  }

  // 1) ask to present, return if it's not
  val presented: Boolean = definition?.presentHandler?.onResponse(currentContext) ?: false
  if (!presented) {
    Log.d("[ActionManager]: action NOT presented: ${currentContext}.")
    currentAction = null
    performActions()
    return
  }

  if (currentAction != null) {
    recordImpression(currentAction)
  }

  Log.i("[ActionManager]: action presented: ${currentContext}.")
  // propagate event that message is displayed
  messageDisplayListener?.onMessageDisplayed(currentContext)

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

    } else {
      recordMessageImpression(action.context.messageId)
    }
  } catch (t: Throwable) {
    Log.exception(t)
  }
}
