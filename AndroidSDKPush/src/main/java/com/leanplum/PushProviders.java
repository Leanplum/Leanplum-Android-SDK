/*
 * Copyright 2020, Leanplum, Inc. All rights reserved.
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

package com.leanplum;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.VisibleForTesting;
import com.leanplum.internal.APIConfig;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Log;
import com.leanplum.internal.OperationQueue;
import com.leanplum.utils.SharedPreferencesUtil;
import java.util.HashMap;
import java.util.Map;

public class PushProviders {
  private static String FCM_PROVIDER_CLASS = "com.leanplum.LeanplumFcmProvider";

  private Map<PushProviderType, IPushProvider> providers = new HashMap<>();

  public PushProviders() {
    IPushProvider fcm = createFcm();
    if (fcm != null) {
      providers.put(PushProviderType.FCM, fcm);
    }
  }

  public void updateRegistrationIdsAndBackend() {
    boolean hasAppIDChanged = hasAppIDChanged(APIConfig.getInstance().appId());

    for (IPushProvider provider: providers.values()) {
      if (hasAppIDChanged) {
        provider.unregister();
      }
      OperationQueue.sharedInstance().addParallelOperation(new Runnable() {
        @Override
        public void run() {
          provider.updateRegistrationId();
        }
      });
    }
  }

  private static IPushProvider createFcm() {
    try {
      IPushProvider fcmProvider =
          (IPushProvider) Class.forName(FCM_PROVIDER_CLASS).getConstructor().newInstance();
      return fcmProvider;
    } catch (Throwable t) {
      Log.e("FCM not found. Did you forget to include FCM module dependency "
          + "\"com.leanplum:leanplum-fcm\"?", t);
      return null;
    }
  }

  /**
   * Checks if current application id is different from stored one.
   *
   * @param currentAppId Current application id.
   * @return True if application id was stored before and doesn't equal to current.
   */
  @VisibleForTesting
  static boolean hasAppIDChanged(String currentAppId) {
    if (TextUtils.isEmpty(currentAppId)) {
      return false;
    }

    Context context = Leanplum.getContext();
    if (context == null) {
      return false;
    }

    String storedAppId = SharedPreferencesUtil.getString(
        context,
        Constants.Defaults.LEANPLUM_PUSH,
        Constants.Defaults.APP_ID);

    if (!currentAppId.equals(storedAppId)) {
      Log.d("Saving the application id in the shared preferences.");
      SharedPreferencesUtil.setString(
          context,
          Constants.Defaults.LEANPLUM_PUSH,
          Constants.Defaults.APP_ID,
          currentAppId);

      // Check application id was stored before.
      if (!SharedPreferencesUtil.DEFAULT_STRING_VALUE.equals(storedAppId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Update provider's registration ID.
   */
  public void setRegistrationId(PushProviderType type, String registrationId) {
    IPushProvider provider = providers.get(type);
    if (provider != null)
      provider.setRegistrationId(registrationId);
  }

}
