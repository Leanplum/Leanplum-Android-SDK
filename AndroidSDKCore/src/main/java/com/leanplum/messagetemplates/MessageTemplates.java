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

package com.leanplum.messagetemplates;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.actions.internal.ActionDefinition;
import com.leanplum.actions.internal.ActionManagerDefinitionKt;
import com.leanplum.callbacks.ActionCallback;
import com.leanplum.internal.ActionManager;
import com.leanplum.messagetemplates.actions.AlertMessage;
import com.leanplum.messagetemplates.actions.CenterPopupMessage;
import com.leanplum.messagetemplates.actions.InterstitialMessage;
import com.leanplum.messagetemplates.actions.RichHtmlMessage;
import com.leanplum.messagetemplates.actions.WebInterstitialMessage;
import com.leanplum.messagetemplates.actions.ConfirmMessage;
import com.leanplum.messagetemplates.actions.OpenUrlAction;

/**
 * Registers all of the built-in message templates.
 *
 * @author Andrew First
 */
public class MessageTemplates {

  private static boolean registered = false;
  private static DialogCustomizer customizer;

  /**
   * Registers customizer to use when creating the message templates.
   * Call this method before Leanplum.start.
   *
   * @param customizer Dialog customizer
   */
  public static void setCustomizer(@Nullable DialogCustomizer customizer) {
    MessageTemplates.customizer = customizer;
  }

  /**
   * Returns the registered customizer.
   *
   * @return Dialog customizer
   */
  @Nullable
  public static DialogCustomizer getCustomizer() {
    return customizer;
  }

  /**
   * Registers a message template to respond to a given action.
   * Will be shown in the templates group in Dashboard.
   *
   * @param template Wrapper for action name, action arguments and handler.
   * @param context Android context
   */
  public static void registerTemplate(
      @NonNull MessageTemplate template,
      @NonNull Context context) {
    register(template, context, Leanplum.ACTION_KIND_MESSAGE | Leanplum.ACTION_KIND_ACTION);
  }

  /**
   * Registers an action template to respond to a given action.
   * Will be shown in the actions group in Dashboard.
   *
   * @param template Wrapper for action name, action arguments and handler.
   * @param context Android context
   */
  public static void registerAction(
      @NonNull MessageTemplate template,
      @NonNull Context context) {
    register(template, context, Leanplum.ACTION_KIND_ACTION);
  }

  private static void register(
      @NonNull MessageTemplate template,
      @NonNull Context context,
      int kind) {

    String name = template.getName();
    ActionArgs args = template.createActionArgs(context);

    ActionDefinition definition = new ActionDefinition(
        name,
        kind,
        args,
        null, // options
        new ActionCallback() {
          @Override
          public boolean onResponse(ActionContext context) {
            return template.present(context);
          }
        },
        new ActionCallback() {
          @Override
          public boolean onResponse(ActionContext context) {
            return template.dismiss(context);
          }
        });
    ActionManagerDefinitionKt.defineAction(ActionManager.getInstance(), definition);
  }

  public synchronized static void register(Context context) {
    if (registered) {
      return;
    }
    registered = true;

    registerAction(new OpenUrlAction(), context);
    registerTemplate(new AlertMessage(), context);
    registerTemplate(new ConfirmMessage(), context);
    registerTemplate(new CenterPopupMessage(), context);
    registerTemplate(new InterstitialMessage(), context);
    registerTemplate(new WebInterstitialMessage(), context);
    registerTemplate(new RichHtmlMessage(), context);
  }
}
