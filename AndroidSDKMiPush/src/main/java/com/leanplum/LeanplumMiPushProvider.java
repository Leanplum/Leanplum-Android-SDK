package com.leanplum;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.leanplum.internal.Constants.Defaults;
import com.leanplum.internal.Log;
import com.xiaomi.mipush.sdk.MiPushClient;

/**
 * Leanplum provider for work with Xiaomi MiPush.
 * Class is instantiated by reflection using default constructor.
 */
class LeanplumMiPushProvider extends LeanplumCloudMessagingProvider {

  private boolean appRegistered = false;

  /**
   * Constructor called by reflection.
   */
  public LeanplumMiPushProvider() {
    if (Leanplum.getContext() != null) {
      // there is theoretical possibility for context to be null
      registerApp(Leanplum.getContext());
    } else {
      Log.e("MiPush app not registered because context is null");
    }
  }

  private void registerApp(@NonNull Context context) {
    if (TextUtils.isEmpty(LeanplumMiPushHandler.APP_ID)
        || TextUtils.isEmpty(LeanplumMiPushHandler.APP_KEY)) {
      Log.e("You need to provide appId and appKey for MiPush to work.");
      return;
    }
    MiPushClient.registerPush(context, LeanplumMiPushHandler.APP_ID, LeanplumMiPushHandler.APP_KEY);
    appRegistered = true;
  }

  @Override
  protected String getSharedPrefsPropertyName() {
    return Defaults.PROPERTY_MIPUSH_TOKEN_ID;
  }

  @Override
  public PushProviderType getType() {
    return PushProviderType.MIPUSH;
  }

  @Override
  public void unregister() {
    if (Leanplum.getContext() == null)
      return;

    MiPushClient.unregisterPush(Leanplum.getContext());
  }

  @Override
  public void updateRegistrationId() {
    if (Leanplum.getContext() == null)
      return;

    if (!appRegistered) {
      registerApp(Leanplum.getContext());
    }

    String regId = MiPushClient.getRegId(Leanplum.getContext());
    if (!TextUtils.isEmpty(regId)) {
      setRegistrationId(regId);
    }
  }
}
