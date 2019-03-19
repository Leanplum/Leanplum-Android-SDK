/*
 * Copyright 2016, Leanplum, Inc. All rights reserved.
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
import android.nfc.Tag;
import android.text.TextUtils;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.leanplum.internal.LeanplumManifestHelper;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

import java.util.Collections;

import androidx.annotation.NonNull;

/**
 * Leanplum provider for work with Firebase.
 *
 * @author Anna Orlova
 */
class LeanplumFcmProvider extends LeanplumCloudMessagingProvider {

  @Override
  public String getRegistrationId() {
    return this.getStoredRegistrationPreferences(Leanplum.getContext());
  }

  @Override
  public void getCurrentRegistrationIdAndUpdateBackend() {
    FirebaseInstanceId.getInstance().getInstanceId()
        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
          @Override
          public void onComplete(@NonNull Task<InstanceIdResult> task) {
            if (!task.isSuccessful()) {
              Log.e("getInstanceId failed");
              return;
            }
            // Get new Instance ID token
            String tokenId = task.getResult().getToken();
            if (!TextUtils.isEmpty(tokenId)) {
                onRegistrationIdReceived(Leanplum.getContext(), tokenId);
              }
            }
        });
  }

  @Override
  public boolean isInitialized() {
    return true;
  }

  @Override
  public boolean isManifestSetup() {
    Context context = Leanplum.getContext();
    if (context == null) {
      return false;
    }

    try {
      boolean hasPushReceiver = LeanplumManifestHelper.checkComponent(LeanplumManifestHelper.ApplicationComponent.RECEIVER,
          LeanplumManifestHelper.LP_PUSH_RECEIVER, false, null,
          Collections.singletonList(LeanplumManifestHelper.LP_PUSH_FCM_MESSAGING_SERVICE), context.getPackageName());

      boolean hasPushFirebaseMessagingService = LeanplumManifestHelper.checkComponent(
          LeanplumManifestHelper.ApplicationComponent.SERVICE,
          LeanplumManifestHelper.LP_PUSH_FCM_MESSAGING_SERVICE, false, null,
          Collections.singletonList(LeanplumManifestHelper.FCM_MESSAGING_EVENT), context.getPackageName());

      boolean hasRegistrationService = LeanplumManifestHelper.checkComponent(
          LeanplumManifestHelper.ApplicationComponent.SERVICE,
          LeanplumPushRegistrationService.class.getName(), false, null, null, context.getPackageName());

      boolean hasServices = hasPushFirebaseMessagingService &&
          hasRegistrationService;

      if (hasPushReceiver && hasServices) {
        Log.i("Firebase Messaging is setup correctly.");
        return true;
      }
    } catch (Throwable t) {
      Util.handleException(t);
    }
    Log.e("Failed to setup Firebase Messaging, check your manifest configuration.");
    return false;
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
