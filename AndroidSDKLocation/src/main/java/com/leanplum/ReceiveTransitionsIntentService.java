/*
 * Copyright 2014, Leanplum, Inc. All rights reserved.
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

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.leanplum.internal.ActionManager;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

import java.util.List;

/**
 * Receives the geo-fence state changes.
 *
 * @author Atanas Dobrev
 */
public class ReceiveTransitionsIntentService extends IntentService {
  public ReceiveTransitionsIntentService() {
    super("ReceiveTransitionsIntentService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    try {
      GeofencingEvent event = GeofencingEvent.fromIntent(intent);
      if (event.hasError()) {
        int errorCode = event.getErrorCode();
        Log.d("Location Client Error with code: " + errorCode);
      } else {
        int transitionType = event.getGeofenceTransition();
        List<Geofence> triggeredGeofences = event.getTriggeringGeofences();
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER ||
            transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
          LocationManagerImplementation locationManager = (LocationManagerImplementation)
              ActionManager.getLocationManager();
          if (locationManager != null) {
            locationManager.updateStatusForGeofences(triggeredGeofences, transitionType);
          }
        }
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }
}
