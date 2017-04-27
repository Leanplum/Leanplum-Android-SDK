// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.content.Context;

/**
 * Leanplum provider for manually registering for Cloud Messaging services.
 *
 * @author Anna Orlova
 */
public class LeanplumManualProvider extends LeanplumCloudMessagingProvider {
  LeanplumManualProvider(Context context, String registrationId) {
    onRegistrationIdReceived(context, registrationId);
  }

  public String getRegistrationId() {
    return getCurrentRegistrationId();
  }

  public boolean isInitialized() {
    return true;
  }

  public boolean isManifestSetUp() {
    return true;
  }

  public void unregister() {

  }
}
