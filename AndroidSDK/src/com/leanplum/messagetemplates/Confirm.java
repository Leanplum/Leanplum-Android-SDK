// Copyright 2014, Leanplum, Inc.

package com.leanplum.messagetemplates;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.callbacks.ActionCallback;
import com.leanplum.callbacks.PostponableAction;
import com.leanplum.messagetemplates.MessageTemplates.Args;
import com.leanplum.messagetemplates.MessageTemplates.Values;

import static com.leanplum.messagetemplates.MessageTemplates.getApplicationName;

/**
 * Registers a Leanplum action that displays a system confirm dialog.
 *
 * @author Andrew First
 */
class Confirm {
  private static final String NAME = "Confirm";

  public static void register(Context currentContext) {
    Leanplum.defineAction(
        NAME,
        Leanplum.ACTION_KIND_MESSAGE | Leanplum.ACTION_KIND_ACTION,
        new ActionArgs().with(Args.TITLE, getApplicationName(currentContext))
            .with(Args.MESSAGE, Values.CONFIRM_MESSAGE)
            .with(Args.ACCEPT_TEXT, Values.YES_TEXT)
            .with(Args.CANCEL_TEXT, Values.NO_TEXT)
            .withAction(Args.ACCEPT_ACTION, null)
            .withAction(Args.CANCEL_ACTION, null), new ActionCallback() {

          @Override
          public boolean onResponse(final ActionContext context) {
            LeanplumActivityHelper.queueActionUponActive(new PostponableAction() {
              @Override
              public void run() {
                Activity activity = LeanplumActivityHelper.getCurrentActivity();
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    activity);
                alertDialogBuilder
                    .setTitle(context.stringNamed(Args.TITLE))
                    .setMessage(context.stringNamed(Args.MESSAGE))
                    .setCancelable(false)
                    .setPositiveButton(context.stringNamed(Args.ACCEPT_TEXT),
                        new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog, int id) {
                            context.runTrackedActionNamed(Args.ACCEPT_ACTION);
                          }
                        })
                    .setNegativeButton(context.stringNamed(Args.CANCEL_TEXT),
                        new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog, int id) {
                            context.runActionNamed(Args.CANCEL_ACTION);
                          }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                if (activity != null && !activity.isFinishing()) {
                  alertDialog.show();
                }
              }
            });
            return true;
          }
        });
  }
}
