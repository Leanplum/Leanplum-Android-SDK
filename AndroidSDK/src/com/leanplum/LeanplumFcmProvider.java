/*
 * Copyright 2017, Leanplum. All rights reserved.
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

import com.google.firebase.iid.FirebaseInstanceId;
import com.leanplum.internal.LeanplumManifestHelper;
import com.leanplum.internal.Log;

import java.util.Collections;

/**
 * Leanplum provider for work with Firebase.
 *
 * @author Anna Orlova
 */
class LeanplumFcmProvider extends LeanplumCloudMessagingProvider {
  private static final String INSTANCE_ID_EVENT = "com.google.firebase.INSTANCE_ID_EVENT";
  private static final String MESSAGING_EVENT = "com.google.firebase.MESSAGING_EVENT";
  private static final String PUSH_FCM_LISTENER_SERVICE =
      "com.leanplum.LeanplumPushFcmListenerService";
  private static final String PUSH_FIREBASE_MESSAGING_SERVICE =
      "com.leanplum.LeanplumPushFirebaseMessagingService";

  public String getRegistrationId() {
    return FirebaseInstanceId.getInstance().getToken();
  }

  public boolean isInitialized() {
    return true;
  }

  public boolean isManifestSetUp() {
    boolean hasReceivers =
        LeanplumManifestHelper.checkComponent(LeanplumManifestHelper.getReceivers(),
            PUSH_RECEIVER, false, null,
            Collections.singletonList(PUSH_FIREBASE_MESSAGING_SERVICE), null);

    boolean hasPushFirebaseMessagingService = LeanplumManifestHelper.checkComponent(
        LeanplumManifestHelper.getServices(), PUSH_FIREBASE_MESSAGING_SERVICE, false, null,
        Collections.singletonList(MESSAGING_EVENT), null);
    boolean hasPushFcmListenerService = LeanplumManifestHelper.checkComponent(
        LeanplumManifestHelper.getServices(), PUSH_FCM_LISTENER_SERVICE, false, null,
        Collections.singletonList(INSTANCE_ID_EVENT), null);
    boolean hasPushRegistrationService = LeanplumManifestHelper.checkComponent(
        LeanplumManifestHelper.getServices(), PUSH_REGISTRATION_SERVICE, false, null, null, null);

    boolean hasServices = hasPushFirebaseMessagingService && hasPushFcmListenerService
        && hasPushRegistrationService;

    return hasReceivers && hasServices;
  }

  /**
   * Unregister from FCM.
   */
  public void unregister() {
    try {
      FirebaseInstanceId.getInstance().deleteInstanceId();
      Log.i("Application was unregistred from FCM.");
    } catch (Exception e) {
      Log.e("Failed to unregister from FCM.");
    }
  }
}
