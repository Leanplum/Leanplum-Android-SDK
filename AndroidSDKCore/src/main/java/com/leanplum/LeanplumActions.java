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

package com.leanplum;

import androidx.annotation.NonNull;
import com.leanplum.actions.ActionManagerTriggeringKt;
import com.leanplum.actions.MessageDisplayController;
import com.leanplum.actions.MessageDisplayListener;
import com.leanplum.internal.ActionManager;

public class LeanplumActions {

   /**
    * Keep or dismiss in-app message when push notification is opened. If
    * kept, the action from the push notification will go into the queue and will present after
    * in-app dismissal, otherwise the in-app is dismissed and the push notification's action is
    * presented.
    *
    * Default value is true.
    *
    * @param flag If true in-app is dismissed, otherwise push action goes into the queue.
    */
   public static void setDismissOnPushOpened(boolean flag) {
      ActionManager.getInstance().setDismissOnPushOpened(flag);
   }

   /**
    * Message queue is paused when app is backgrounded and resumed when app is foregrounded.
    * You can change that behaviour if you pass false.
    *
    * Default value is true.
    */
   public static void setContinueOnActivityResumed(boolean flag) {
      ActionManager.getInstance().setContinueOnActivityResumed(flag);
   }

   /**
    * Sets controller instance that will decide the order and priority of messages.
    *
    * If controller is not set then all messages are added into the queue in the order they are
    * triggered.
    *
    * @param controller Instance of the controller class. Pass null if you want to remove your
    *                   instance.
    */
   public static void setMessageDisplayController(@NonNull MessageDisplayController controller) {
      ActionManager.getInstance().setMessageDisplayController(controller);
   }

   /**
    * Sets a listener instance to be invoked when a message is displayed, dismissed, or clicked.
    *
    * @param listener Instance of listener class. Pass null if you want remove your instance.
    */
   public static void setMessageDisplayListener(@NonNull MessageDisplayListener listener) {
      ActionManager.getInstance().setMessageDisplayListener(listener);
   }

   /**
    * Triggers postponed messages when indefinite time was used with
    * {@link MessageDisplayController#shouldDisplayMessage(ActionContext)}
    */
   public static void triggerDelayedMessages() {
      ActionManagerTriggeringKt.triggerDelayedMessages(ActionManager.getInstance());
   }
}
