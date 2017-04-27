// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

import java.util.Map;

/**
 * FCM listener service, which enables handling messages on the app's behalf.
 *
 * @author Anna Orlova
 */
@SuppressLint("Registered")
public class LeanplumPushFirebaseMessagingService extends FirebaseMessagingService {
  /**
   * Called when a message is received. This is also called when a notification message is received
   * while the app is in the foreground.
   *
   * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
   */
  @Override
  public void onMessageReceived(RemoteMessage remoteMessage) {
    try {
      Map<String, String> messageMap = remoteMessage.getData();
      if (messageMap.containsKey(Constants.Keys.PUSH_MESSAGE_TEXT)) {
        LeanplumPushService.handleNotification(this, getBundle(messageMap));
      }
      Log.i("Received: " + messageMap.toString());
    } catch (Throwable t) {
      Util.handleException(t);
    }
  }

  /**
   * @param messageMap {@link RemoteMessage}'s data map.
   */
  private Bundle getBundle(Map<String, String> messageMap) {
    Bundle bundle = new Bundle();
    if (messageMap != null) {
      for (Map.Entry<String, String> entry : messageMap.entrySet()) {
        bundle.putString(entry.getKey(), entry.getValue());
      }
    }
    return bundle;
  }
}
