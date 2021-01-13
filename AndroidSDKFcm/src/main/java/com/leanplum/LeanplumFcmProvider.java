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

import android.text.TextUtils;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Log;

import androidx.annotation.NonNull;

/**
 * Leanplum provider for work with Firebase.
 * Class is instantiated by reflection using default constructor.
 *
 * @author Anna Orlova
 */
class LeanplumFcmProvider extends LeanplumCloudMessagingProvider {

  /**
   * Constructor called by reflection.
   */
  public LeanplumFcmProvider() {
  }

  @Override
  protected String getSharedPrefsPropertyName() {
    return Constants.Defaults.PROPERTY_FCM_TOKEN_ID;
  }

  @Override
  public PushProviderType getType() {
    return PushProviderType.FCM;
  }

  @Override
  public void updateRegistrationId() {
    FirebaseInstanceId.getInstance().getInstanceId()
        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
          @Override
          public void onComplete(@NonNull Task<InstanceIdResult> task) {
            if (!task.isSuccessful()) {
              Exception exc = task.getException();
              Log.e("getInstanceId failed:\n" + Log.getStackTraceString(exc));
              return;
            }
            // Get new Instance ID token
            String tokenId = task.getResult().getToken();
            if (!TextUtils.isEmpty(tokenId)) {
                setRegistrationId(tokenId);
              }
            }
        });
  }

  @Override
  public void unregister() {
    try {
      FirebaseInstanceId.getInstance().deleteInstanceId();
      Log.i("Application was unregistered from FCM.");
    } catch (Exception e) {
      Log.e("Failed to unregister from FCM.");
    }
  }
}
