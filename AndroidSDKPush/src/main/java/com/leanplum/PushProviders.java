/*
 * Copyright 2021, Leanplum, Inc. All rights reserved.
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
import com.leanplum.internal.Util;
import com.leanplum.utils.SharedPreferencesUtil;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class PushProviders {
  private static String FCM_PROVIDER_CLASS = "com.leanplum.LeanplumFcmProvider";
  private static String MIPUSH_PROVIDER_CLASS = "com.leanplum.LeanplumMiPushProvider";
  private static String HMS_PROVIDER_CLASS = "com.leanplum.LeanplumHmsProvider";

  private final Map<PushProviderType, IPushProvider> providers = new ConcurrentHashMap<>();
  private boolean initialized = false;

  public PushProviders() {
    init();
  }

  public synchronized void init() { // synchronize access to 'initialized' variable
    if (initialized) {
      return;
    }

    if (Leanplum.getContext() == null) {
      // init() method will be called for second time from LeanplumPushService.onStart
      return;
    }

    IPushProvider fcm = createFcm();
    if (fcm != null) {
      providers.put(PushProviderType.FCM, fcm);
    }

    IPushProvider miPush = createMiPush();
    if (miPush != null) {
      providers.put(PushProviderType.MIPUSH, miPush);
    }

    IPushProvider hms = createHms();
    if (hms != null) {
      providers.put(PushProviderType.HMS, hms);
    }

    initialized = true;
  }

  public void updateRegistrationIdsAndBackend() {
    boolean hasAppIDChanged = hasAppIDChanged(APIConfig.getInstance().appId());

    synchronized (providers) {
      for (IPushProvider provider : providers.values()) {
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
  }

  private static IPushProvider createFcm() {
    if (!Util.hasPlayServices()) {
      Log.i("No valid Google Play Services APK found. FCM will not initialize.");
      return null;
    }

    try {
      IPushProvider fcmProvider =
          (IPushProvider) Class.forName(FCM_PROVIDER_CLASS).getConstructor().newInstance();
      return fcmProvider;
    } catch (Throwable t) {
      Log.i("FCM module not found. "
          + "For Firebase messaging include dependency \"com.leanplum:leanplum-fcm\".");
      return null;
    }
  }

  private static IPushProvider createMiPush() {
    try {
      Class<?> clazz = Class.forName(MIPUSH_PROVIDER_CLASS);

      if (!Util.isXiaomiDevice()) {
        Log.d("Will not initialize MiPush provider for non-Xiaomi device.");
        return null;
      }

      return (IPushProvider) clazz.getConstructor().newInstance();
    } catch (Throwable t) {
      Log.i("MiPush module not found. "
          + "For Mi Push messaging include dependency \"com.leanplum:leanplum-mipush\".");
      return null;
    }
  }

  private static IPushProvider createHms() {
    // TODO check for EMUI device or HMS
    try {
      Class<?> clazz = Class.forName(HMS_PROVIDER_CLASS);

      if (!Util.isHuaweiDevice()) {
        Log.d("Will not initialize HMS provider for non-Huawei device.");
        return null;
      }

      return (IPushProvider) clazz.getConstructor().newInstance();
    } catch (Throwable t) {
      Log.i("HMS module not found. "
          + "For Huawei Push Kit messaging include dependency \"com.leanplum:leanplum-hms\".");
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
    synchronized (providers) {
      IPushProvider provider = providers.get(type);
      if (provider != null) {
        provider.setRegistrationId(registrationId);
      }
    }
  }

}
