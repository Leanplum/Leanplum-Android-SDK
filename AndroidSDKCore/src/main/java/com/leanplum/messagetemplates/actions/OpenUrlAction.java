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

package com.leanplum.messagetemplates.actions;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.annotation.NonNull;
import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.internal.Log;
import com.leanplum.internal.OperationQueue;
import com.leanplum.messagetemplates.MessageTemplate;
import com.leanplum.messagetemplates.MessageTemplateConstants.Args;
import com.leanplum.messagetemplates.MessageTemplateConstants.Values;

import java.util.List;

/**
 * Registers a Leanplum action that opens a particular URL. If the URL cannot be handled by the
 * system URL handler, you can add your own action responder using {@link Leanplum#onAction} that
 * handles the URL how you want.
 *
 * @author Andrew First
 */
public class OpenUrlAction implements MessageTemplate {
  private static final String OPEN_URL = "Open URL";

  @NonNull
  @Override
  public String getName() {
    return OPEN_URL;
  }

  private static boolean openUriIntent(Context context, Intent uriIntent) {
    List<ResolveInfo> resolveInfoList =
        context.getPackageManager().queryIntentActivities(uriIntent, 0);

    // If url can be handled by current app - set package name to intent, so url
    // will be open by current app. Skip chooser dialog.
    if (!resolveInfoList.isEmpty()) {
      for (ResolveInfo resolveInfo : resolveInfoList) {
        if (resolveInfo != null && resolveInfo.activityInfo != null &&
            resolveInfo.activityInfo.name != null) {
          if (resolveInfo.activityInfo.name.contains(
              context.getPackageName())) {
            uriIntent.setPackage(resolveInfo.activityInfo.packageName);
          }
        }
      }
    }
    try {
      // Even if we have valid destination, startActivity can crash if
      // activity we are trying to open is not exported in manifest.
      context.startActivity(uriIntent);
      return true;
    } catch (ActivityNotFoundException e) {
      Log.e("Activity you are trying to start doesn't exist or " +
          "isn't exported in manifest: " + e);
      return false;
    }
  }

  @NonNull
  @Override
  public ActionArgs createActionArgs(@NonNull Context context) {
    return new ActionArgs().with(Args.URL, Values.DEFAULT_URL);
  }

  @Override
  public boolean present(@NonNull ActionContext actionContext) {
    Context context = Leanplum.getContext();
    if (context == null) {
      return false;
    }
    String url = actionContext.stringNamed(Args.URL);
    Intent uriIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

    // Calling startActivity() from outside of an Activity context requires the
    // FLAG_ACTIVITY_NEW_TASK flag.
    if (!(context instanceof Activity)) {
      uriIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    boolean opened = openUriIntent(context, uriIntent);

    // Run after the other executor code
    OperationQueue.sharedInstance().addUiOperation(() -> actionContext.runActionNamed(Args.DISMISS_ACTION));

    // TODO Trqbva li da se dobavi kym Lifecycle callback-a i dostyp do executor-a, za da moje da se izchistva ako se smeni aktivity?
    // za momenta uspqva da si pokaje novoto activity i chak togava pokazva sledvashtoto syobshtenie ot opashkata, zaradi
    // upotrebata na UI queue-to

    return opened;
  }

  @Override
  public boolean dismiss(@NonNull ActionContext context) {
    // nothing to do
    return true;
  }
}
