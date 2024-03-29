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

import androidx.annotation.UiThread
import com.leanplum.ActionContext
import com.leanplum.Leanplum
import com.leanplum.actions.LeanplumActions
import com.leanplum.actions.MessageDisplayChoice
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
  Log.d("[ActionManager]: performing all available actions: $queue")
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
  if (nextAction != null && nextAction.isNotification()) {
    dismissCurrentAction()
  }
}

fun ActionManager.dismissCurrentAction() {
  val currentContext = currentAction?.context ?: return
  val definition = definitions.findDefinition(currentContext.actionName())

  definition?.dismissHandler?.also {
    Log.d("[ActionManager][${Util.getThread()}]: dismiss requested for: ${currentContext}.")
    try {
      it.onResponse(currentContext)
    } catch (t: Throwable) {
      Log.e("Cannot dismiss in-app", t)
    }
  }
}

@UiThread
private fun ActionManager.performActionsImpl() {
  if (isPaused || !isEnabled) return

  // do not continue if we have action running
  if (currentAction != null) {
    Log.d("[ActionManager][${Util.getThread()}]: will not pop queue, because an action is already presenting")
    if (!LeanplumActions.useWorkerThreadForDecisionHandlers) { // disable prioritization with worker thread
      prioritizePushNotificationActions()
    }
    return
  }

  // gets the next action from the queue
  currentAction = queue.pop() ?: return

  val currentContext = currentAction.context
  Log.d("[ActionManager][${Util.getThread()}]: action popped from queue: ${currentContext}.")

  if (currentAction.actionType == Action.ActionType.SINGLE
    && LeanplumInternal.shouldSuppressMessage(currentContext)) {
    Log.i("[ActionManager][${Util.getThread()}]: Local IAM caps reached, suppressing $currentContext")
    currentAction = null
    performActions()
    return
  }

  if (LeanplumActions.useWorkerThreadForDecisionHandlers) {
    OperationQueue.sharedInstance().addActionOperation { askUserAndPresentAction(currentContext) }
  } else {
    askUserAndPresentAction(currentContext)
  }
}

private fun ActionManager.askUserAndPresentAction(currentContext: ActionContext) {

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
      val action = currentAction ?: return
      Log.d("[ActionManager][${Util.getThread()}]: delaying action: ${currentContext} for ${displayDecision.delaySeconds}s.")
      if (displayDecision.delaySeconds > 0) {
        // Schedule for delayed time
        scheduler.schedule(action, displayDecision.delaySeconds)
      } else {
        // Insert in delayed queue
        delayedQueue.pushBack(action)
      }
      currentAction = null
      performActions()
      return
    }

    else -> Unit
  }

  if (LeanplumActions.useWorkerThreadForDecisionHandlers) {
    OperationQueue.sharedInstance().addUiOperation { presentAction(currentContext) }
  } else {
    presentAction(currentContext)
  }
}

private fun ActionManager.presentAction(currentContext: ActionContext) {
  // logic:
  // 1) ask client to show view controller
  // 2) ask and wait for client to execute action
  // 3) ask and wait for client to dismiss view controller

  // get the action definition
  val definition = definitions.findDefinition(currentContext.actionName())

  // 3) set dismiss block
  currentContext.setActionDidDismiss {
    val dismissOperation = {
      Log.d("[ActionManager][${Util.getThread()}]: actionDidDismiss: ${currentContext}.")
      messageDisplayListener?.onMessageDismissed(currentContext)
      currentAction = null // stop executing current action
      performActions()
    }

    if (LeanplumActions.useWorkerThreadForDecisionHandlers) {
      OperationQueue.sharedInstance().addActionOperation(dismissOperation)
    } else {
      dismissOperation.invoke()
    }
  }

  // 2) set the action block
  currentContext.setActionDidExecute { actionNamedContext ->
    val actionExecutedOperation = {
      Log.d("[ActionManager][${Util.getThread()}]: actionDidExecute: ${actionNamedContext}.")
      messageDisplayListener?.onActionExecuted(actionNamedContext.actionName(), actionNamedContext)
      Unit
    }

    if (LeanplumActions.useWorkerThreadForDecisionHandlers) {
      OperationQueue.sharedInstance().addActionOperation(actionExecutedOperation)
    } else {
      actionExecutedOperation.invoke()
    }
  }

  // 1) ask to present, return if it's not
  val presented: Boolean = definition?.presentHandler?.onResponse(currentContext) ?: false
  if (!presented) {
    Log.d("[ActionManager][${Util.getThread()}]: action NOT presented: ${currentContext}.")
    currentAction = null
    performActions()
    return
  }

  if (currentAction != null) {
    recordImpression(currentAction)
  }

  Log.i("[ActionManager][${Util.getThread()}]: action presented: ${currentContext}.")
  // propagate event that message is displayed
  if (LeanplumActions.useWorkerThreadForDecisionHandlers) {
    OperationQueue.sharedInstance().addActionOperation {
      messageDisplayListener?.onMessageDisplayed(currentContext)
    }
  } else {
    messageDisplayListener?.onMessageDisplayed(currentContext)
  }

  performActions()
}

private fun ActionManager.recordImpression(action: Action) {
  try {
    val ctx = action.context
    when (action.actionType) {

      Action.ActionType.CHAINED -> {
        // We do not want to count occurrences for action kind, because in multi message
        // campaigns the Open URL action is not a message. Also if the user has defined
        // actions of type Action we do not want to count them.

        val actionKind = definitions.findDefinition(ctx.actionName())?.kind
        if (actionKind == Leanplum.ACTION_KIND_ACTION) {
          recordChainedActionImpression(ctx.messageId)
        } else {
          recordMessageImpression(ctx.messageId)
        }
      }

      Action.ActionType.SINGLE -> {
        recordMessageImpression(ctx.messageId)
      }

      Action.ActionType.EMBEDDED -> {
        // do nothing
      }
    }
  } catch (t: Throwable) {
    Log.exception(t)
  }
}
