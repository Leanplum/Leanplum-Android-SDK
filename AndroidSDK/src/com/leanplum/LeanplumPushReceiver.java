// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

/**
 * Handles push notification intents, for example, by tracking opens and performing the open
 * action.
 *
 * @author Aleksandar Gyorev
 */
public class LeanplumPushReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    try {
      if (intent == null) {
        Log.e("Received a null intent.");
        return;
      }
      LeanplumPushService.openNotification(context, intent.getExtras());
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }
}
