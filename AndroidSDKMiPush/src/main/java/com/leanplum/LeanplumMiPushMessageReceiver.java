/*
 * Copyright 2021, Leanplum, Inc. All rights reserved.
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

import android.content.Context;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;

/**
 * Implementation of MiPush listener for messages.
 */
public class LeanplumMiPushMessageReceiver extends PushMessageReceiver {

  private LeanplumMiPushHandler handler = new LeanplumMiPushHandler();

  @Override
  public void onReceivePassThroughMessage(Context context, MiPushMessage message) {
    handler.onReceivePassThroughMessage(context, message);
  }

  @Override
  public void onNotificationMessageClicked(Context context, MiPushMessage message) {
    handler.onNotificationMessageClicked(context, message);
  }

  @Override
  public void onNotificationMessageArrived(Context context, MiPushMessage message) {
    handler.onNotificationMessageArrived(context, message);
  }

  @Override
  public void onCommandResult(Context context, MiPushCommandMessage message) {
    handler.onCommandResult(context, message);
  }

  @Override
  public void onReceiveRegisterResult(Context context, MiPushCommandMessage message) {
    handler.onReceiveRegisterResult(context, message);
  }
}
