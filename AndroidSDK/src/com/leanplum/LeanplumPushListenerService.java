// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.os.Bundle;

import com.google.android.gms.gcm.GcmListenerService;
import com.leanplum.internal.Constants.Keys;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

/**
 * GCM listener service, which enables handling messages on the app's behalf.
 *
 * @author Aleksandar Gyorev
 */
public class LeanplumPushListenerService extends GcmListenerService {
  /**
   * Called when a message is received.
   *
   * @param senderId Sender ID of the sender.
   * @param data Data bundle containing the message data as key-value pairs.
   */
  @Override
  public void onMessageReceived(String senderId, Bundle data) {
    try {
      if (data.containsKey(Keys.PUSH_MESSAGE_TEXT)) {
        LeanplumPushService.handleNotification(this, data);
      }
      Log.i("Received: " + data.toString());
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }
}
