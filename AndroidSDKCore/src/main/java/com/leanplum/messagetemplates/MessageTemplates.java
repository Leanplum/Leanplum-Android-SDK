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
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.callbacks.ActionCallback;
import com.leanplum.callbacks.PostponableAction;
import com.leanplum.callbacks.VariablesChangedCallback;
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

  private static ActionCallback createCallback(@NonNull MessageTemplate template) {
    return new ActionCallback() {
      @Override
      public boolean onResponse(ActionContext actionContext) {
        LeanplumActivityHelper.queueActionUponActive(
            new PostponableAction() {
              @Override
              public void run() {
                template.handleAction(actionContext);
              }
            });
        return true;
      }
    };
  }

  private static ActionCallback createCallbackWaitVarsAndFiles(@NonNull MessageTemplate template) {
    return new ActionCallback() {
      @Override
      public boolean onResponse(ActionContext actionContext) {
        Leanplum.addOnceVariablesChangedAndNoDownloadsPendingHandler(
            new VariablesChangedCallback() {
              @Override
              public void variablesChanged() {
                LeanplumActivityHelper.queueActionUponActive(
                    new PostponableAction() {
                      @Override
                      public void run() {
                        template.handleAction(actionContext);
                      }
                    });
              }
            });
        return true;
      }
    };
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

    ActionCallback callback;
    if (template.waitFilesAndVariables() || args.containsFile()) { // checks args just in case
      callback = createCallbackWaitVarsAndFiles(template);
    } else {
      callback = createCallback(template);
    }

    Leanplum.defineAction(name, kind, args, callback);
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
