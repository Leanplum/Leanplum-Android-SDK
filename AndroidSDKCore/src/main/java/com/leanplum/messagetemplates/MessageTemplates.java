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
import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.callbacks.ActionCallback;
import com.leanplum.callbacks.PostponableAction;
import com.leanplum.callbacks.VariablesChangedCallback;
import com.leanplum.messagetemplates.actions.AlertMessage;
import com.leanplum.messagetemplates.actions.AppRatingAction;
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
  private static final String ALERT = "Alert";
  private static final String CENTER_POPUP = "Center Popup";
  private static final String CONFIRM = "Confirm";
  private static final String HTML = "HTML";
  private static final String INTERSTITIAL = "Interstitial";
  private static final String OPEN_URL = "Open URL";
  private static final String WEB_INTERSTITIAL = "Web Interstitial";
  private static final String APP_RATING = "Request App Rating";

  private static boolean registered = false;

  @FunctionalInterface
  private interface MessageFactory {
    void showMessage(ActionContext context);
  }

  @FunctionalInterface
  private interface ActionArgsFactory {
    ActionArgs createActionArgs(Context context);
  }

  private static ActionCallback createCallback(MessageFactory messageFactory) {
    return new ActionCallback() {
      @Override
      public boolean onResponse(ActionContext actionContext) {
        LeanplumActivityHelper.queueActionUponActive(
            new PostponableAction() {
              @Override
              public void run() {
                messageFactory.showMessage(actionContext);
              }
            });
        return true;
      }
    };
  }

  private static ActionCallback createCallbackWaitVarsAndFiles(MessageFactory messageFactory) {
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
                        messageFactory.showMessage(actionContext);
                      }
                    });
              }
            });
        return true;
      }
    };
  }

  private static void defineAction(
      String name,
      ActionArgsFactory argsFactory,
      MessageFactory messageFactory,
      Context context) {

    int kind = Leanplum.ACTION_KIND_MESSAGE | Leanplum.ACTION_KIND_ACTION;

    ActionArgs args = argsFactory.createActionArgs(context);

    ActionCallback callback = createCallback(messageFactory);

    Leanplum.defineAction(name, kind, args, callback);
  }

  private static void defineActionWaitVarsAndFiles(
      String name,
      ActionArgsFactory argsFactory,
      MessageFactory messageFactory,
      Context context) {

    int kind = Leanplum.ACTION_KIND_MESSAGE | Leanplum.ACTION_KIND_ACTION;

    ActionArgs args = argsFactory.createActionArgs(context);

    ActionCallback callback = createCallbackWaitVarsAndFiles(messageFactory);

    Leanplum.defineAction(name, kind, args, callback);
  }

  public synchronized static void register(Context context) {
    if (registered) {
      return;
    }
    registered = true;

    defineAction(
        OPEN_URL,
        OpenUrlAction::createActionArgs,
        OpenUrlAction::onActionTriggered,
        context);

    defineAction(
        ALERT,
        AlertMessage::createActionArgs,
        AlertMessage::showMessage,
        context);

    defineAction(
        CONFIRM,
        ConfirmMessage::createActionArgs,
        ConfirmMessage::showMessage,
        context);

    defineActionWaitVarsAndFiles(
        CENTER_POPUP,
        CenterPopupMessage::createActionArgs,
        CenterPopupMessage::showMessage,
        context);

    defineActionWaitVarsAndFiles(
        INTERSTITIAL,
        InterstitialMessage::createActionArgs,
        InterstitialMessage::showMessage,
        context);

    defineAction(
        WEB_INTERSTITIAL,
        WebInterstitialMessage::createActionArgs,
        WebInterstitialMessage::showMessage,
        context);

    defineActionWaitVarsAndFiles(
        HTML,
        RichHtmlMessage::createActionArgs,
        RichHtmlMessage::showMessage,
        context);

    defineAction(
        APP_RATING,
        AppRatingAction::createActionArgs,
        AppRatingAction::onActionTriggered,
        context);
  }
}
