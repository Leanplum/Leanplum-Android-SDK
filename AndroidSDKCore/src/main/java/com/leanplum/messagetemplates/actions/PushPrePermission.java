/*
 * Copyright 2023, Leanplum, Inc. All rights reserved.
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
import com.leanplum.internal.OperationQueue;
import com.leanplum.messagetemplates.MessageTemplate;
import com.leanplum.messagetemplates.controllers.CenterPopupController;
import com.leanplum.messagetemplates.options.PushPrePermissionOptions;
import com.leanplum.utils.PushPermissionUtilKt;

public class PushPrePermission implements MessageTemplate {
  private static final String PUSH_ASK_TO_ASK = "Push Ask to Ask";

  private CenterPopupController popup;

  @NonNull
  @Override
  public String getName() {
    return PUSH_ASK_TO_ASK;
  }

  @NonNull
  @Override
  public ActionArgs createActionArgs(@NonNull Context context) {
    return PushPrePermissionOptions.toArgs(context);
  }

  @Override
  public boolean present(@NonNull ActionContext context) {
    Activity activity = LeanplumActivityHelper.getCurrentActivity();
    if (activity == null || activity.isFinishing()) {
      return false;
    }

    if (PushPermissionUtilKt.shouldShowPrePermission(activity)) {
      showPrePermission(context, activity);
      return true;
    } else if (PushPermissionUtilKt.shouldShowRegisterForPush(activity)) {
      showRegisterForPush(context, activity);
      return true;
    } else {
      // nothing to do
      Log.d("Will not show Push Pre-Permission dialog because:");
      PushPermissionUtilKt.printDebugLog(activity);
      return false;
    }
  }

  private void showPrePermission(ActionContext context, Activity activity) {
    PushPrePermissionOptions options = new PushPrePermissionOptions(context);
    popup = new CenterPopupController(activity, options);
    popup.setOnDismissListener(listener -> {
      popup = null;
      context.actionDismissed();
    });
    popup.show();
  }

  private void showRegisterForPush(ActionContext context, Activity activity) {
    // Run after ActionManagerExecution code
    OperationQueue.sharedInstance().addUiOperation(() -> {
      context.actionDismissed();
      // Flow of permission request will be shown on top of any other in-app
      PushPermissionUtilKt.requestNativePermission(activity);
    });
  }

  @Override
  public boolean dismiss(@NonNull ActionContext context) {
    if (popup != null) {
      popup.dismiss();
    }
    return true;
  }
}
