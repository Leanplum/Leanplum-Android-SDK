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

public class LeanplumActions { // TODO discuss whether to move code to Leanplum.java

   /**
    * TODO
    * Default value is true.
    *
    * @param flag
    */
   public static void setDismissOnPushOpened(boolean flag) {
      ActionManager.getInstance().setDismissOnPushOpened(flag);
   }

   /**
    * TODO
    * Default value is true.
    *
    * @param flag
    */
   public static void setContinueOnActivityResumed(boolean flag) {
      // TODO
   }

   /**
    * TODO
    * @param controller
    */
   public static void setMessageDisplayController(@NonNull MessageDisplayController controller) {
      ActionManager.getInstance().setMessageDisplayController(controller);
   }

   /**
    * TODO
    * @param listener
    */
   public static void setMessageDisplayListener(@NonNull MessageDisplayListener listener) {
      ActionManager.getInstance().setMessageDisplayListener(listener);
   }

   /**
    * Triggers postponed messages when indefinite time was used with
    * MessageDisplayController.shouldDisplayMessage
    */
   public static void triggerDelayedMessages() {
      ActionManagerTriggeringKt.triggerDelayedMessages(ActionManager.getInstance());
   }
}
