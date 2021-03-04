/*
 * Copyright 2020, Leanplum, Inc. All rights reserved.
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

package com.leanplum.messagetemplates;

import android.content.Context;
import androidx.annotation.NonNull;
import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;

/**
 * Wraps action name, arguments and handler.
 */
public interface MessageTemplate {

  /**
   * Returns the name of the action to register.
   */
  @NonNull
  String getName();

  /**
   * Creates the user-customizable options for the action.
   *
   * @param context Android context
   */
  @NonNull
  ActionArgs createActionArgs(Context context);

  /**
   * Called in response to the registered action.
   * Use it to show your message to the user.
   *
   * @param context The context in which an action or message is executed.
   */
  void handleAction(ActionContext context);

  /**
   * If your custom template depends on files or variables, that will be downloaded, override and
   * return true.
   *
   * @return true to wait for files and variables to finish downloading before calling
   * {@link #handleAction(ActionContext)}.
   */
  default boolean waitFilesAndVariables() {
    return false;
  }
}
