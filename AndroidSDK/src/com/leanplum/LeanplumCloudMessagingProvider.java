// Copyright 2016, Leanplum, Inc.

package com.leanplum;

import android.content.Context;

import com.leanplum.internal.Constants;
import com.leanplum.internal.Log;
import com.leanplum.utils.SharedPreferencesUtil;

/**
 * Leanplum Cloud Messaging provider.
 *
 * @author Anna Orlova
 */
abstract class LeanplumCloudMessagingProvider {
  static final String PUSH_REGISTRATION_SERVICE = "com.leanplum.LeanplumPushRegistrationService";
  static final String PUSH_RECEIVER = "com.leanplum.LeanplumPushReceiver";

  private static String registrationId;

  /**
   * Registration app for Cloud Messaging.
   *
   * @return String - registration id for app.
   */
  public abstract String getRegistrationId();

  /**
   * Verifies that Android Manifest is set up correctly.
   *
   * @return true If Android Manifest is set up correctly.
   */
  public abstract boolean isManifestSetUp();

  public abstract boolean isInitialized();

  /**
   * Unregister from cloud messaging.
   */
  public abstract void unregister();

  static String getCurrentRegistrationId() {
    return registrationId;
  }

  void onRegistrationIdReceived(Context context, String registrationId) {
    if (registrationId == null) {
      Log.w("Registration ID is undefined.");
      return;
    }
    LeanplumCloudMessagingProvider.registrationId = registrationId;
    // Check if received push notification token is different from stored one and send new one to
    // server.
    if (!LeanplumCloudMessagingProvider.registrationId.equals(SharedPreferencesUtil.getString(
        context, Constants.Defaults.LEANPLUM_PUSH, Constants.Defaults.PROPERTY_REGISTRATION_ID))) {
      Log.i("Device registered for push notifications with registration token", registrationId);
      storePreferences(context.getApplicationContext());
    }
    // Send push token on every launch for not missed token when user force quit the app.
    sendRegistrationIdToBackend(LeanplumCloudMessagingProvider.registrationId);
  }

  /**
   * Sends the registration ID to the server over HTTP.
   */
  private static void sendRegistrationIdToBackend(String registrationId) {
    Leanplum.setRegistrationId(registrationId);
  }

  /**
   * Stores the registration ID in the application's {@code SharedPreferences}.
   *
   * @param context application's context.
   */
  public void storePreferences(Context context) {
    Log.v("Saving the registration ID in the shared preferences.");
    SharedPreferencesUtil.setString(context, Constants.Defaults.LEANPLUM_PUSH,
        Constants.Defaults.PROPERTY_REGISTRATION_ID, registrationId);
  }
}
