// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.annotation.SuppressLint;
import android.content.Intent;

import com.google.firebase.iid.FirebaseInstanceIdService;
import com.leanplum.internal.Log;

/**
 * Firebase Cloud Messaging InstanceID listener service to handle creation, rotation, and updating
 * of registration tokens.
 *
 * @author Anna Orlova
 */
@SuppressLint("Registered")
public class LeanplumPushFcmListenerService extends FirebaseInstanceIdService {
  /**
   * Called if InstanceID token is updated. This may occur if the security of the previous token had
   * been compromised. This call is initiated by the InstanceID provider.
   */
  @Override
  public void onTokenRefresh() {
    Log.i("FCM InstanceID token needs an update");
    // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
    Intent intent = new Intent(this, LeanplumPushRegistrationService.class);
    startService(intent);
  }
}
