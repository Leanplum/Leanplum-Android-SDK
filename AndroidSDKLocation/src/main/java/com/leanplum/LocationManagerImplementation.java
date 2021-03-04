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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.Geofence.Builder;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.leanplum.internal.Constants;
import com.leanplum.internal.JsonConverter;
import com.leanplum.internal.LeanplumInternal;
import com.leanplum.internal.LeanplumMessageMatchFilter;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;
import com.leanplum.models.GeofenceEventType;
import com.leanplum.utils.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class GeofenceStatus {
  static final int UNKNOWN = 1;
  static final int INSIDE = 2;
  static final int OUTSIDE = 4;

  static boolean shouldTriggerEnteredGeofence(Number currentStatus, Number newStatus) {
    return ((currentStatus.intValue() == OUTSIDE || currentStatus.intValue() == UNKNOWN) &&
        newStatus.intValue() == INSIDE);
  }

  static boolean shouldTriggerExitedGeofence(Number currentStatus, Number newStatus) {
    return (currentStatus.intValue() == INSIDE &&
        newStatus.intValue() == OUTSIDE);
  }
}

/**
 * Handles geo-fencing and sending user location.
 *
 * @author Atanas Dobrev
 */
class LocationManagerImplementation implements
    GoogleApiClient.ConnectionCallbacks, OnConnectionFailedListener, LocationManager,
    LocationListener {
  private static final long LOCATION_UPDATE_INTERVAL = 7200000; // 2 hours in milliseconds.
  private static final long LOCATION_REQUEST_INTERVAL = 60000; // a minute in milliseconds.
  private static final double ACCURACY_THRESHOLD_GPS = 100; // 100m

  private static final String PERMISSION = "android.permission.ACCESS_FINE_LOCATION";
  private static final String METADATA = "com.google.android.gms.version";

  private Map<String, Object> lastKnownState;
  private Map<String, Object> stateBeforeBackground;
  private List<Geofence> allGeofences;
  private List<Geofence> backgroundGeofences;
  private List<String> trackedGeofenceIds;
  private boolean isInBackground;
  private boolean isSendingLocation;
  private Date lastLocationSentDate;
  private LeanplumLocationAccuracyType lastLocationSentAccuracyType;

  private GoogleApiClient googleApiClient;

  private static LocationManagerImplementation instance;

  public static synchronized LocationManager instance() {
    try {
      if (LocationServices.API != null) {
        if (instance == null) {
          instance = new LocationManagerImplementation();
        }
        return instance;
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  private LocationManagerImplementation() {
    trackedGeofenceIds = new ArrayList<>();
    loadLastKnownRegionState();
    isInBackground = Util.isInBackground();
    isSendingLocation = false;
    lastLocationSentAccuracyType = LeanplumLocationAccuracyType.IP;
    // When an app starts LocationManager will be initialized. Update user location.
    updateUserLocation();
  }

  @SuppressWarnings("unchecked")
  public void setRegionsData(Map<String, Object> regionData,
      Set<String> foregroundRegionNames, Set<String> backgroundRegionNames) {
    if (!Util.hasPlayServices()) {
      return;
    }

    allGeofences = new ArrayList<>();
    backgroundGeofences = new ArrayList<>();
    for (Map.Entry<String, Object> entry : regionData.entrySet()) {
      String regionName = entry.getKey();
      boolean isForeground = foregroundRegionNames.contains(regionName);
      boolean isBackground = backgroundRegionNames.contains(regionName);
      if (isForeground || isBackground) {
        Geofence geofence = geofenceFromMap((Map<String, Object>) entry.getValue(),
            regionName);
        if (geofence != null) {
          if (isBackground) {
            backgroundGeofences.add(geofence);
          }
          allGeofences.add(geofence);
          if (lastKnownState != null && geofence.getRequestId() != null
              && lastKnownState.get(geofence.getRequestId()) == null) {
            lastKnownState.put(geofence.getRequestId(), GeofenceStatus.UNKNOWN);
          }
        }
      }
    }

    updateGeofencing();
  }

  /**
   * Starts location client if it has not been started, and calls requestLocation().
   */
  public void updateUserLocation() {
    startLocationClient();
    if (googleApiClient != null && googleApiClient.isConnected()) {
      requestLocation();
    }
  }

  /**
   * Starts location client if it has not been started, and calls updateTrackedGeofences()
   */
  public void updateGeofencing() {
    if (allGeofences != null && backgroundGeofences != null) {
      startLocationClient();
      if (googleApiClient != null && googleApiClient.isConnected()) {
        updateTrackedGeofences();
      }
    }
  }

  private void loadLastKnownRegionState() {
    if (lastKnownState != null) {
      return;
    }
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
        "__leanplum__location", Context.MODE_PRIVATE);
    String regionsStateJson = defaults.getString(Constants.Keys.REGION_STATE, null);
    if (regionsStateJson == null) {
      lastKnownState = new HashMap<>();
    } else {
      lastKnownState = JsonConverter.fromJson(regionsStateJson);
    }
  }

  private void saveLastKnownRegionState() {
    if (lastKnownState == null) {
      return;
    }
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
        "__leanplum__location", Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = defaults.edit();
    editor.putString(Constants.Keys.REGION_STATE, JsonConverter.toJson(lastKnownState));
    SharedPreferencesUtil.commitChanges(editor);
  }

  private Geofence geofenceFromMap(Map<String, Object> regionData, String regionName) {
    Number latitude = (Number) regionData.get("lat");
    Number longitude = (Number) regionData.get("lon");
    Number radius = (Number) regionData.get("radius");
    Number version = (Number) regionData.get("version");
    if (latitude == null) {
      return null;
    }
    Builder geofenceBuilder = new Builder();
    geofenceBuilder.setCircularRegion(latitude.floatValue(),
        longitude.floatValue(), radius.floatValue());
    geofenceBuilder.setRequestId(geofenceID(regionName, version.intValue()));
    geofenceBuilder.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
        Geofence.GEOFENCE_TRANSITION_EXIT);
    geofenceBuilder.setExpirationDuration(Geofence.NEVER_EXPIRE);
    return geofenceBuilder.build();
  }

  private String geofenceID(String regionName, Integer version) {
    return "__leanplum" + regionName + "_" + version.toString();
  }

  private void startLocationClient() {
    if (!isPermissionGranted() || !isMetaDataSet()) {
      Log.i("You have to set the application meta data and location "
          + "permission to use location services.");
      return;
    }
    if (googleApiClient == null) {
      googleApiClient = new GoogleApiClient.Builder(Leanplum.getContext())
          .addApi(LocationServices.API)
          .addConnectionCallbacks(this)
          .addOnConnectionFailedListener(this)
          .build();
    }
    if (!googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
      googleApiClient.connect();
    }
  }

  private boolean isPermissionGranted() {
    Context context = Leanplum.getContext();
    try {
      return context.checkCallingOrSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED;
    } catch (RuntimeException ignored) {
      return false;
    }
  }

  private boolean isMetaDataSet() {
    Context context = Leanplum.getContext();
    try {
      ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
          context.getPackageName(), PackageManager.GET_META_DATA);
      if (appInfo != null) {
        if (appInfo.metaData != null) {
          Object value = appInfo.metaData.get(METADATA);
          if (value != null) {
            return true;
          }
        }
      }
      return false;
    } catch (NameNotFoundException e) {
      return false;
    }
  }

  // Suppressing missing permission warning which since it is up to client to add location
  // permission to their manifest.
  @SuppressWarnings("MissingPermission")
  private void updateTrackedGeofences() {
    if (allGeofences == null || googleApiClient == null || !googleApiClient.isConnected()) {
      return;
    }
    if (!isInBackground && Util.isInBackground()) {
      stateBeforeBackground = new HashMap<>();
      if (lastKnownState != null && lastKnownState.size() > 0) {
        for (Map.Entry<String, Object> entry : lastKnownState.entrySet()) {
          stateBeforeBackground.put(entry.getKey(), entry.getValue());
        }
      }
    }
    List<Geofence> toBeTrackedGeofences = allGeofences;
    if (trackedGeofenceIds.size() > 0) {
      LocationServices.GeofencingApi.removeGeofences(googleApiClient, trackedGeofenceIds);
    }
    trackedGeofenceIds = new ArrayList<>();
    if (toBeTrackedGeofences != null && toBeTrackedGeofences.size() > 0) {
      LocationServices.GeofencingApi.addGeofences(googleApiClient,
          toBeTrackedGeofences, getTransitionPendingIntent());
      for (Geofence geofence : toBeTrackedGeofences) {
        if (geofence != null && geofence.getRequestId() != null) {
          String geofenceId = geofence.getRequestId();
          trackedGeofenceIds.add(geofenceId);
          //TODO: stateBeforeBackground doesn't get persisted.
          // If the app goes to the background and terminates, stateBeforeBackground will be reset.
          if (isInBackground && !Util.isInBackground() && stateBeforeBackground != null) {
            Number lastStatus = (Number) stateBeforeBackground.get(geofenceId);
            Number currentStatus = (Number) lastKnownState.get(geofenceId);
            if (currentStatus != null && lastStatus != null) {
              // Only foreground geofences should be triggered here
              if (GeofenceStatus.shouldTriggerEnteredGeofence(lastStatus, currentStatus)) {
                maybePerformActions(geofence, "enterRegion", true);
                Leanplum.trackGeofence(GeofenceEventType.ENTER_REGION, geofenceId);
              }
              if (GeofenceStatus.shouldTriggerExitedGeofence(lastStatus, currentStatus)) {
                maybePerformActions(geofence, "exitRegion", true);
                Leanplum.trackGeofence(GeofenceEventType.EXIT_REGION, geofenceId);
              }
            }
          }
        }
      }
    }
    if (isInBackground && !Util.isInBackground()) {
      stateBeforeBackground = null;
    }
    isInBackground = Util.isInBackground();
  }

  private List<Geofence> getToBeTrackedGeofences() {
    if (Util.isInBackground()) {
      return backgroundGeofences;
    } else {
      return allGeofences;
    }
  }

  void updateStatusForGeofences(List<Geofence> geofences, int transitionType) {
    for (Geofence geofence : geofences) {
      String geofenceId = geofence.getRequestId();
      if (!trackedGeofenceIds.contains(geofenceId) &&
          geofenceId.startsWith("__leanplum")) {
        ArrayList<String> geofencesToRemove = new ArrayList<>();
        geofencesToRemove.add(geofenceId);
        if (googleApiClient != null && googleApiClient.isConnected()) {
          LocationServices.GeofencingApi.removeGeofences(googleApiClient, geofencesToRemove);
        }
        continue;
      }
      Number currentStatus = (Number) lastKnownState.get(geofenceId);
      if (currentStatus != null) {
        if (GeofenceStatus.shouldTriggerEnteredGeofence(currentStatus,
            getStatusForTransitionType(transitionType))) {
          maybePerformActions(geofence, "enterRegion", false);
          Leanplum.trackGeofence(GeofenceEventType.ENTER_REGION, geofenceId);
        }
        if (GeofenceStatus.shouldTriggerExitedGeofence(currentStatus,
            getStatusForTransitionType(transitionType))) {
          maybePerformActions(geofence, "exitRegion", false);
          Leanplum.trackGeofence(GeofenceEventType.EXIT_REGION, geofenceId);
        }
      }
      lastKnownState.put(geofenceId,
          getStatusForTransitionType(transitionType));
    }
    saveLastKnownRegionState();
  }

  private void maybePerformActions(Geofence geofence, String action, boolean foregroundActionsOnly) {
    String regionName = getRegionName(geofence.getRequestId());
    if (regionName != null) {
      int filter = Util.isInBackground() ?
          LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_BACKGROUND :
          (foregroundActionsOnly ?
              LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_FOREGROUND :
              LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL);
      LeanplumInternal.maybePerformActions(action, regionName, filter, null, null);
    }
  }

  private int getStatusForTransitionType(int transitionType) {
    if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER ||
        transitionType == Geofence.GEOFENCE_TRANSITION_DWELL) {
      return GeofenceStatus.INSIDE;
    } else {
      return GeofenceStatus.OUTSIDE;
    }
  }

  private String getRegionName(String geofenceRequestId) {
    if (geofenceRequestId.startsWith("__leanplum")) {
      return geofenceRequestId.substring(10, geofenceRequestId.lastIndexOf("_"));
    }
    return null;
  }

  private PendingIntent getTransitionPendingIntent() {
    Context context = Leanplum.getContext();
    Intent intent = new Intent(context, ReceiveTransitionsIntentService.class);
    return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  @Override
  public void onConnected(Bundle arg0) {
    try {
      updateTrackedGeofences();
      requestLocation();
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  @Override
  public void onConnectionSuspended(int i) {
    // According to the Android documentation at
    // https://developers.google.com/android/reference/com/google/android/gms/common/api/GoogleApiClient.ConnectionCallbacks?hl=en
    // GoogleApiClient will automatically attempt to restore the connection.
  }

  @Override
  public void onConnectionFailed(
      @SuppressWarnings("NullableProblems") ConnectionResult connectionResult) {
  }

  @Override
  public void onLocationChanged(Location location) {
    try {
      if (!location.hasAccuracy()) {
        Log.e("Received a location with no accuracy.");
        return;
      }

      // Currently, location segment treats GPS and CELL the same. In the future, we might want more
      // distinction in the accuracy types. For example, a customer might want to send messages only
      // to the ones with very accurate location information. We are assuming that it is from GPS if
      // the location accuracy is less than or equal to |ACCURACY_THRESHOLD_GPS|.
      LeanplumLocationAccuracyType locationAccuracyType =
          location.getAccuracy() >= ACCURACY_THRESHOLD_GPS ?
              LeanplumLocationAccuracyType.CELL : LeanplumLocationAccuracyType.GPS;

      if (!isSendingLocation && needToSendLocation(locationAccuracyType)) {
        try {
          setUserAttributesForLocationUpdate(location, locationAccuracyType);
        } catch (Throwable t) {
          Log.exception(t);
        }
      }

      LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    } catch (Throwable t) {
      Log.e("Cannot change location", t);
    }
  }

  /**
   * Request location for user location update if googleApiClient is connected.
   */
  // Suppressing missing permission warning which since it is up to client to add location
  // permission to their manifest.
  @SuppressWarnings("MissingPermission")
  private void requestLocation() {
    try {
      if (!Leanplum.isLocationCollectionEnabled() || googleApiClient == null
          || !googleApiClient.isConnected()) {
        return;
      }
      LocationRequest request = new LocationRequest();
      // Although we set the interval as |LOCATION_REQUEST_INTERVAL|, we stop listening
      // |onLocationChanged|. So we are essentially requesting location only once.
      request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
          .setInterval(LOCATION_REQUEST_INTERVAL)
          .setFastestInterval(LOCATION_REQUEST_INTERVAL);
      LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, request, this);
    } catch (Throwable throwable) {
      Log.e("Cannot request location updates.", throwable);
    }
  }

  /**
   * Checks whether it is needed to call setUserAttributes API for location update.
   *
   * @param newLocationAccuracyType LeanplumLocationAccuracyType of the new location.
   * @return boolean Whether it is needed to call setUserAttributes API for location update.
   */
  private boolean needToSendLocation(LeanplumLocationAccuracyType newLocationAccuracyType) {
    return (lastLocationSentDate == null
        || (new Date().getTime() - lastLocationSentDate.getTime()) > LOCATION_UPDATE_INTERVAL
        || lastLocationSentAccuracyType.value() < newLocationAccuracyType.value());
  }

  /**
   * Call setUserAttributes API method for location update.
   *
   * @param location location collected from LocationServices
   * @param locationAccuracyType LeanplumLocationAccuracyType of the location IP, CELL, or GPS
   */
  private void setUserAttributesForLocationUpdate(Location location,
      final LeanplumLocationAccuracyType locationAccuracyType) {
    isSendingLocation = true;
    LeanplumInternal.setUserLocationAttribute(location, locationAccuracyType,
        new LeanplumInternal.locationAttributeRequestsCallback() {
          @Override
          public void response(boolean success) {
            isSendingLocation = false;
            if (success) {
              lastLocationSentAccuracyType = locationAccuracyType;
              lastLocationSentDate = new Date();
              Log.d("setUserAttributes with location is successfully called");
            }
          }
        });
  }
}
