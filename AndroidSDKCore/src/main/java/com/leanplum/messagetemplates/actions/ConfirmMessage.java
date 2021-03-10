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

package com.leanplum.messagetemplates.actions;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;
import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.internal.Util;
import com.leanplum.messagetemplates.MessageTemplate;
import com.leanplum.messagetemplates.MessageTemplateConstants.Args;
import com.leanplum.messagetemplates.MessageTemplateConstants.Values;

/**
 * Registers a Leanplum action that displays a system confirm dialog.
 *
 * @author Andrew First
 */
public class ConfirmMessage implements MessageTemplate {
  private static final String CONFIRM = "Confirm";

  @NonNull
  @Override
  public String getName() {
    return CONFIRM;
  }

  @NonNull
  @Override
  public ActionArgs createActionArgs(Context context) {
    return new ActionArgs()
        .with(Args.TITLE, Util.getApplicationName(context))
        .with(Args.MESSAGE, Values.CONFIRM_MESSAGE)
        .with(Args.ACCEPT_TEXT, Values.YES_TEXT)
        .with(Args.CANCEL_TEXT, Values.NO_TEXT)
        .withAction(Args.ACCEPT_ACTION, null)
        .withAction(Args.CANCEL_ACTION, null);
  }

  @Override
  public void handleAction(ActionContext context) {
    Activity activity = LeanplumActivityHelper.getCurrentActivity();
    if (activity == null || activity.isFinishing())
      return;

    new AlertDialog.Builder(activity)
        .setTitle(context.stringNamed(Args.TITLE))
        .setMessage(context.stringNamed(Args.MESSAGE))
        .setCancelable(false)
        .setPositiveButton(context.stringNamed(Args.ACCEPT_TEXT),
            (dialog, id) -> context.runTrackedActionNamed(Args.ACCEPT_ACTION))
        .setNegativeButton(context.stringNamed(Args.CANCEL_TEXT),
            (dialog, id) -> context.runActionNamed(Args.CANCEL_ACTION))
        .create()
        .show();
  }
}
