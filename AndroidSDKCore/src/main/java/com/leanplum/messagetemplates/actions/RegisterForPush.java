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
import com.leanplum.utils.PushPermissionUtilKt;

public class RegisterForPush implements MessageTemplate {
  private static final String REGISTER_FOR_PUSH = "Register For Push";

  @NonNull
  @Override
  public String getName() {
    return REGISTER_FOR_PUSH;
  }

  @NonNull
  @Override
  public ActionArgs createActionArgs(@NonNull Context context) {
    return new ActionArgs();
  }

  @Override
  public boolean present(@NonNull ActionContext context) {
    Activity activity = LeanplumActivityHelper.getCurrentActivity();
    if (activity == null || activity.isFinishing()) {
      return false;
    }

    if (PushPermissionUtilKt.shouldShowRegisterForPush(activity)) {
      showRegisterForPush(context, activity);
      return true;
    } else {
      Log.d("Will not show Register For Push dialog because:");
      PushPermissionUtilKt.printDebugLog(activity);
      return false;
    }
  }

  private void showRegisterForPush(ActionContext context, Activity activity) {
    // Run after the other ActionManagerExecution code
    OperationQueue.sharedInstance().addUiOperation(() -> {
      context.actionDismissed();
      // Flow of permission request will be shown on top of any other in-app
      PushPermissionUtilKt.requestNativePermission(activity);
    });
  }

  @Override
  public boolean dismiss(@NonNull ActionContext context) {
    // nothing to do
    return true;
  }
}
