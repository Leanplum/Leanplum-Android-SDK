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
import android.content.Context;
import androidx.annotation.NonNull;
import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.internal.Log;
import com.leanplum.messagetemplates.MessageTemplate;
import com.leanplum.messagetemplates.controllers.RichHtmlController;
import com.leanplum.messagetemplates.options.RichHtmlOptions;

public class RichHtmlMessage implements MessageTemplate {
  private static final String HTML = "HTML";

  @NonNull
  @Override
  public String getName() {
    return HTML;
  }

  @NonNull
  @Override
  public ActionArgs createActionArgs(Context context) {
    return RichHtmlOptions.toArgs();
  }

  @Override
  public void handleAction(ActionContext context) {
    Activity activity = LeanplumActivityHelper.getCurrentActivity();
    if (activity == null || activity.isFinishing())
      return;

    try {
      RichHtmlOptions richOptions = new RichHtmlOptions(context);
      if (richOptions.getHtmlTemplate() == null) {
        return;
      }

      // Message is shown after html is rendered. Check handleOpenEvent(url) method.
      new RichHtmlController(activity, richOptions);

    } catch (Throwable t) {
      Log.e("Fail on show HTML In-App message: %s", t.getMessage());
    }
  }

  @Override
  public boolean waitFilesAndVariables() {
    return true;
  }
}
