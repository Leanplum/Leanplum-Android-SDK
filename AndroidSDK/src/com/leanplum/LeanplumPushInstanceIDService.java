// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;
import com.leanplum.internal.Log;

/**
 * GCM InstanceID listener service to handle creation, rotation, and updating of registration
 * tokens.
 *
 * @author Aleksandar Gyorev
 */
public class LeanplumPushInstanceIDService extends InstanceIDListenerService {
  /**
   * Called if InstanceID token is updated. This may occur if the security of the previous token had
   * been compromised. This call is initiated by the InstanceID provider.
   */
  @Override
  public void onTokenRefresh() {
    Log.i("GCM InstanceID token needs an update");
    // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
    Intent intent = new Intent(this, LeanplumPushRegistrationService.class);
    startService(intent);
  }
}
