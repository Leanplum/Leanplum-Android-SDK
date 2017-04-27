// Copyright 2014, Leanplum, Inc.

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
      Util.handleException(t);
    }
  }
}
