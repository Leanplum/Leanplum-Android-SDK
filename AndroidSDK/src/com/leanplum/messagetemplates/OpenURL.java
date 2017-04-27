// Copyright 2014, Leanplum, Inc.

package com.leanplum.messagetemplates;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.callbacks.ActionCallback;
import com.leanplum.callbacks.PostponableAction;
import com.leanplum.messagetemplates.MessageTemplates.Args;
import com.leanplum.messagetemplates.MessageTemplates.Values;

import java.util.List;

/**
 * Registers a Leanplum action that opens a particular URL. If the URL cannot be handled by the
 * system URL handler, you can add your own action responder using {@link Leanplum#onAction} that
 * handles the URL how you want.
 *
 * @author Andrew First
 */
class OpenURL {
  private static final String NAME = "Open URL";

  public static void register() {
    Leanplum.defineAction(NAME, Leanplum.ACTION_KIND_ACTION,
        new ActionArgs().with(Args.URL, Values.DEFAULT_URL), new ActionCallback() {
          @Override
          public boolean onResponse(ActionContext context) {
            String url = context.stringNamed(Args.URL);
            final Intent uriIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            // Calling startActivity() from outside of an Activity context requires the
            // FLAG_ACTIVITY_NEW_TASK flag.
            if (!(Leanplum.getContext() instanceof Activity)) {
              uriIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            try {
              if (Leanplum.getContext() != null) {
                LeanplumActivityHelper.queueActionUponActive(
                    new PostponableAction() {
                      @Override
                      public void run() {
                        Context context = Leanplum.getContext();
                        if (context == null) {
                          return;
                        }
                        List<ResolveInfo> resolveInfoList = context.getPackageManager().
                            queryIntentActivities(uriIntent, 0);
                        // If url can be handled by current app - set package name to intent, so url
                        // will be open by current app. Skip chooser dialog.
                        if (resolveInfoList != null && resolveInfoList.size() != 0) {
                          for (ResolveInfo resolveInfo : resolveInfoList) {
                            if (resolveInfo != null && resolveInfo.activityInfo != null &&
                                resolveInfo.activityInfo.name != null) {
                              if (resolveInfo.activityInfo.name.contains(
                                  context.getPackageName())) {
                                uriIntent.setPackage(resolveInfo.activityInfo.packageName);
                              }
                            }
                          }
                          try {
                            // Even if we have valid destination, startActivity can crash if
                            // activity we are trying to open is not exported in manifest.
                            context.startActivity(uriIntent);
                          } catch (ActivityNotFoundException e) {
                            Log.e("Leanplum", "Activity you are trying to start doesn't exist or " +
                                "isn't exported in manifest: " + e);
                          }
                        }
                      }
                    });
                return true;
              } else {
                return false;
              }
            } catch (ActivityNotFoundException e) {
              Log.e("Leanplum", "Unable to handle URL " + url);
              return false;
            }
          }
        });
  }
}
