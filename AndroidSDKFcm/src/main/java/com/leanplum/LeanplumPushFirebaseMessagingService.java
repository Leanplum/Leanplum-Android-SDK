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

import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * FCM listener service, which enables handling messages on the app's behalf.
 *
 * @author Anna Orlova
 */
public class LeanplumPushFirebaseMessagingService extends FirebaseMessagingService {

  private final LeanplumFirebaseServiceHandler handler = new LeanplumFirebaseServiceHandler();

  @Override
  public void onCreate() {
    super.onCreate();
    handler.onCreate(getApplicationContext());
  }

  @Override
  public void onNewToken(@NonNull String token) {
    super.onNewToken(token);
    handler.onNewToken(token, getApplicationContext());
  }

  /**
   * Called when a message is received. This is also called when a notification message is received
   * while the app is in the foreground.
   *
   * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
   */
  @Override
  public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
    handler.onMessageReceived(remoteMessage, getApplicationContext());
  }
}
