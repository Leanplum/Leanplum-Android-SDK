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
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Log;
import com.leanplum.utils.SharedPreferencesUtil;

/**
 * Leanplum Cloud Messaging provider.
 *
 * @author Anna Orlova
 */
abstract class LeanplumCloudMessagingProvider implements IPushProvider {

  /**
   * Returns the name of the property in shared preferences where this provider's ID is saved.
   */
  protected abstract String getSharedPrefsPropertyName();

  @Override
  public void setRegistrationId(String registrationId) {
    if (TextUtils.isEmpty(registrationId)) {
      Log.d("Registration ID for %s is undefined.", getType());
      return;
    }

    Log.d("Registering for %s push notifications with ID %s", getType(), registrationId);

    if (!registrationId.equals(getRegistrationId())) {
      storeRegistrationId(registrationId);
      Log.d("Sending registration ID to backend.");
      Leanplum.setRegistrationId(getType(), registrationId);
    }
  }

  @VisibleForTesting
  void storeRegistrationId(@NonNull String registrationId) {
    Context context = Leanplum.getContext();
    if (context == null) {
      return;
    }

    Log.d("Saving the registration ID %s in the shared preferences.", registrationId);
    SharedPreferencesUtil.setString(
        context,
        Constants.Defaults.LEANPLUM_PUSH,
        getSharedPrefsPropertyName(),
        registrationId);
  }

  @Override
  public String getRegistrationId() {
    Context context = Leanplum.getContext();
    if (context == null)
      return null;

    return SharedPreferencesUtil.getString(
        context,
        Constants.Defaults.LEANPLUM_PUSH,
        getSharedPrefsPropertyName());
  }
}
