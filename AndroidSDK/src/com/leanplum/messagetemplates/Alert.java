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
 * Registers a Leanplum action that displays a system alert dialog.
 *
 * @author Andrew First
 */
public class Alert {
  private static final String NAME = "Alert";

  public static void register(Context currentContext) {
    Leanplum.defineAction(
        NAME,
        Leanplum.ACTION_KIND_MESSAGE | Leanplum.ACTION_KIND_ACTION,
        new ActionArgs().with(Args.TITLE, getApplicationName(currentContext))
            .with(Args.MESSAGE, Values.ALERT_MESSAGE)
            .with(Args.DISMISS_TEXT, Values.OK_TEXT)
            .withAction(Args.DISMISS_ACTION, null), new ActionCallback() {

          @Override
          public boolean onResponse(final ActionContext context) {
            LeanplumActivityHelper.queueActionUponActive(new PostponableAction() {
              @Override
              public void run() {
                Activity activity = LeanplumActivityHelper.getCurrentActivity();
                if (activity == null) {
                  return;
                }
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    activity);
                alertDialogBuilder
                    .setTitle(context.stringNamed(Args.TITLE))
                    .setMessage(context.stringNamed(Args.MESSAGE))
                    .setCancelable(false)
                    .setPositiveButton(context.stringNamed(Args.DISMISS_TEXT),
                        new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog, int id) {
                            context.runActionNamed(Args.DISMISS_ACTION);
                          }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                if (!activity.isFinishing()) {
                  alertDialog.show();
                }
              }
            });
            return true;
          }
        });
  }
}
