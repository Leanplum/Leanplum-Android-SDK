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

import com.leanplum.actions.internal.triggerDelayedMessages
import com.leanplum.internal.ActionManager

/**
 * Contains configuration methods for the action queue.
 */
object LeanplumActions {
  /**
   * Keep or dismiss in-app message when push notification is opened. If kept, the action from the
   * push notification will go into the queue and will present after in-app dismissal, otherwise
   * the in-app is dismissed and the push notification's action is presented.
   *
   * Default value is true.
   *
   * @param flag If true in-app is dismissed, otherwise push action goes into the queue.
   */
  @JvmStatic
  fun setDismissOnPushOpened(flag: Boolean) {
    ActionManager.getInstance().dismissOnPushOpened = flag
  }

  /**
   * Message queue is paused when app is backgrounded and resumed when app is foregrounded. You
   * can change that behaviour if you pass false.
   *
   * Default value is true.
   */
  @JvmStatic
  fun setContinueOnActivityResumed(flag: Boolean) {
    ActionManager.getInstance().continueOnActivityResumed = flag
  }

  /**
   * Sets controller instance that will decide the order and priority of messages.
   *
   * If controller is not set and multiple messages are triggered at once it will execute only the
   * first one. This ensures backwards compatibility.
   *
   * @param controller Instance of the controller class. Pass null if you want to remove your
   * instance.
   */
  @JvmStatic
  fun setMessageDisplayController(controller: MessageDisplayController?) {
    ActionManager.getInstance().messageDisplayController = controller
  }

  /**
   * Sets a listener instance to be invoked when a message is displayed, dismissed, or clicked.
   *
   * @param listener Instance of listener class. Pass null if you want remove your instance.
   */
  @JvmStatic
  fun setMessageDisplayListener(listener: MessageDisplayListener?) {
    ActionManager.getInstance().messageDisplayListener = listener
  }

  /**
   * Method will trigger postponed messages when indefinite time was used with
   * [MessageDisplayController.shouldDisplayMessage]
   */
  @JvmStatic
  fun triggerDelayedMessages() {
    ActionManager.getInstance().triggerDelayedMessages()
  }

  /**
   * When queue is paused it will stop executing actions but new actions will continue to be
   * added.
   */
  @JvmStatic
  fun setQueuePaused(paused: Boolean) {
    setContinueOnActivityResumed(!paused)
    ActionManager.getInstance().isPaused = paused
  }

  /**
   * Returns the paused state of the queue.
   * Check [setQueuePaused].
   */
  @JvmStatic
  fun isQueuePaused() = ActionManager.getInstance().isPaused

  /**
   * When queue is disabled it will stop executing actions and new actions won't be added.
   */
  @JvmStatic
  fun setQueueEnabled(enabled: Boolean) {
    ActionManager.getInstance().isEnabled = enabled
  }

  /**
   * Returns the enabled state of the queue.
   * Check [setQueueEnabled]
   */
  @JvmStatic
  fun isQueueEnabled() = ActionManager.getInstance().isEnabled

  @JvmStatic
  var useWorkerThreadForDecisionHandlers = false
}
