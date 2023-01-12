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

package com.leanplum.messagetemplates.options;

import android.app.Activity;
import android.content.Context;
import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.messagetemplates.MessageTemplateConstants;
import com.leanplum.messagetemplates.MessageTemplateConstants.Args;
import com.leanplum.utils.PushPermissionUtilKt;

public class PushPrePermissionOptions extends CenterPopupOptions {

  public PushPrePermissionOptions(ActionContext context) {
    super(context);
  }

  /**
   * Overrides the default accept action, because for PushPrePermission we want behavior similar to
   * iOS, where the accept action is not added in arguments and it is set as a callback.
   */
  @Override
  public void accept() {
    Activity activity = LeanplumActivityHelper.getCurrentActivity();
    if (activity == null || activity.isFinishing()) {
      return;
    }
    PushPermissionUtilKt.requestNativePermission(activity);
    super.accept(); // tracks accept event
  }

  @Override
  public boolean hasDismissButton() {
    return false;
  }

  public static ActionArgs toArgs(Context context) {
    return BaseMessageOptions.createPushPrePermissionArgs(context)
        .with(Args.LAYOUT_WIDTH, MessageTemplateConstants.Values.CENTER_POPUP_WIDTH)
        .with(Args.LAYOUT_HEIGHT, MessageTemplateConstants.Values.CENTER_POPUP_HEIGHT);
  }
}
