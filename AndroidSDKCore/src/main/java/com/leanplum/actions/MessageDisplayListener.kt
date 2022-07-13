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

interface MessageDisplayListener {
  /**
   * Called when the message is displayed.
   */
  fun onMessageDisplayed(action: ActionContext)

  /**
   * Called when the message is dismissed.
   */
  fun onMessageDismissed(action: ActionContext)

  /**
   * Called when the message is clicked
   */
  fun onActionExecuted(name: String, action: ActionContext)
}
