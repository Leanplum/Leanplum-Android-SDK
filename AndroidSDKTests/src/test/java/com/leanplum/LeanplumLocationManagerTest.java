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

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.leanplum.__setup.AbstractTest;
import com.leanplum._whitebox.utilities.RequestHelper;
import com.leanplum.internal.LeanplumInternal;
import com.leanplum.internal.RequestBuilder;
import com.leanplum.internal.Request;
import com.leanplum.internal.Util;
import com.leanplum.internal.VarCache;

import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Unit Tests for LocationManager.
 *
 * @author Kyu Hyun Chang
 */
@PrepareForTest(value = {
    Leanplum.class,
    LeanplumInternal.class,
    Util.class,
    LeanplumActivityHelper.class,
    URL.class,
    LocationManagerImplementation.class,
    Request.class,
    LocationServices.class,
    FusedLocationProviderApi.class,
    VarCache.class,
}, fullyQualifiedNames = {"com.leanplum.internal.*"})
public class LeanplumLocationManagerTest extends AbstractTest {
  private static final long MILLIS_IN_A_DAY = 1000 * 60 * 60 * 24;
  private static LocationManager mLocationManager;

  @Override
  public void before() throws Exception {
    super.before();
    mLocationManager = Whitebox.invokeMethod(LocationManagerImplementation.class, "instance");
  }


  @Override
  public void after() {
    // Do nothing.
  }

  /**
   * A helper method for testing needToSendLocation.
   *
   * @param lastLocationSentDate Date of the last location sent.
   * @param lastLocationSentAccuracyType Accuracy type of the last location sent.
   * @param currentLocationAccuracyType Accuracy type of the current location.
   * @param expected Expected return value of needToSendLocation given the parameters.
   * @throws Exception
   */
  private void verifyNeedToSendLocation(Date lastLocationSentDate,
      LeanplumLocationAccuracyType lastLocationSentAccuracyType,
      LeanplumLocationAccuracyType currentLocationAccuracyType, boolean expected)
      throws Exception {
    Whitebox.setInternalState(mLocationManager, "lastLocationSentDate", lastLocationSentDate);
    Whitebox.setInternalState(mLocationManager, "lastLocationSentAccuracyType",
        lastLocationSentAccuracyType);

    Boolean result = Whitebox.invokeMethod(mLocationManager, "needToSendLocation",
        currentLocationAccuracyType);

    assertEquals(expected, result);
  }

  /**
   * A helper method for testing setUserAttributesForLocationUpdate.
   *
   * @param location User location.
   * @param locationAccuracyType Accuracy of the user location.
   * @param latLonExpected Expected string of latitude of longitude to be sent.
   * @throws Exception
   */
  private void verifySetUserAttributesForLocationUpdate(Location location,
      final LeanplumLocationAccuracyType locationAccuracyType, final String latLonExpected) throws
      Exception {
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_SET_USER_ATTRIBUTES, apiMethod);
        assertEquals(latLonExpected, params.get("location"));
        assertEquals(locationAccuracyType, params.get("locationAccuracyType"));
      }
    });
    Leanplum.setDeviceLocation(location);
  }

  /**
   * Tests for needToSendLocation.
   *
   * @throws Exception
   */
  @Test
  public void needToSendLocation() throws Exception {
    Date lastLocationSentDate;

    // Last location sent to the server unknown (nil).
    lastLocationSentDate = Whitebox.getInternalState(mLocationManager, "lastLocationSentDate");
    assertNull(lastLocationSentDate);
    Boolean needToSendLocation = Whitebox.invokeMethod(mLocationManager, "needToSendLocation",
        LeanplumLocationAccuracyType.CELL);
    assertTrue(needToSendLocation);

    // Last location sent to the server is less accurate.
    lastLocationSentDate = new Date();
    verifyNeedToSendLocation(lastLocationSentDate, LeanplumLocationAccuracyType.CELL,
        LeanplumLocationAccuracyType.GPS, true);

    // Last location sent to the server is equally accurate, but it has not been outdated.
    lastLocationSentDate = new Date();
    verifyNeedToSendLocation(lastLocationSentDate, LeanplumLocationAccuracyType.GPS,
        LeanplumLocationAccuracyType.GPS, false);

    // Last location sent to the server is more accurate, and it has not been outdated.
    lastLocationSentDate = new Date();
    verifyNeedToSendLocation(lastLocationSentDate, LeanplumLocationAccuracyType.GPS,
        LeanplumLocationAccuracyType.CELL, false);

    // Last location sent to the server is more accurate, but it has been outdated.
    lastLocationSentDate = new Date(new Date().getTime() - MILLIS_IN_A_DAY);
    verifyNeedToSendLocation(lastLocationSentDate, LeanplumLocationAccuracyType.GPS,
        LeanplumLocationAccuracyType.CELL, true);
  }

  /**
   * Tests for setUserAttributesForLocationUpdate.
   *
   * @throws Exception
   */
  @Test
  public void setUserAttributesForLocationUpdate() throws Exception {
    Location location;
    LeanplumLocationAccuracyType locationAccuracyType;
    String expectedLatLon;
    location = new Location("dummyServiceProvider");

    // Mock reverse geocoding.
    Address address = new Address(Locale.US);
    address.setCountryCode("US");
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);
    Geocoder geocoder = Mockito.mock(Geocoder.class);
    whenNew(Geocoder.class).withAnyArguments().thenReturn(geocoder);
    Mockito.when(geocoder.getFromLocation( anyDouble(), anyDouble(), anyInt()))
            .thenReturn(addresses);

    // Location: San Francisco.
    location.setLatitude(37.774900);
    location.setLongitude(-122.41940);
    locationAccuracyType = LeanplumLocationAccuracyType.GPS;
    expectedLatLon = "37.774900,-122.419400";
    verifySetUserAttributesForLocationUpdate(location, locationAccuracyType, expectedLatLon);

    // Location: New York City.
    location.setLatitude(40.712800);
    location.setLongitude(-74.005900);
    locationAccuracyType = LeanplumLocationAccuracyType.CELL;
    expectedLatLon = "40.712800,-74.005900";
    verifySetUserAttributesForLocationUpdate(location, locationAccuracyType, expectedLatLon);

    // Location: Berlin.
    location.setLatitude(52.520000);
    location.setLongitude(13.405000);
    locationAccuracyType = LeanplumLocationAccuracyType.GPS;
    expectedLatLon = "52.520000,13.405000";
    verifySetUserAttributesForLocationUpdate(location, locationAccuracyType, expectedLatLon);

    // Location: Sofia.
    location.setLatitude(42.697700);
    location.setLongitude(23.321900);
    locationAccuracyType = LeanplumLocationAccuracyType.GPS;
    expectedLatLon = "42.697700,23.321900";
    verifySetUserAttributesForLocationUpdate(location, locationAccuracyType, expectedLatLon);

    // Location: Seoul.
    location.setLatitude(37.566500);
    location.setLongitude(126.978000);
    locationAccuracyType = LeanplumLocationAccuracyType.GPS;
    expectedLatLon = "37.566500,126.978000";
    verifySetUserAttributesForLocationUpdate(location, locationAccuracyType, expectedLatLon);

    // Location: Tokyo.
    location.setLatitude(35.689500);
    location.setLongitude(139.691700);
    locationAccuracyType = LeanplumLocationAccuracyType.GPS;
    expectedLatLon = "35.689500,139.691700";
    verifySetUserAttributesForLocationUpdate(location, locationAccuracyType, expectedLatLon);
  }

  /**
   * Tests for requestLocation.
   *
   * @throws Exception
   */
  @Test
  public void requestLocation() throws Exception {
    GoogleApiClient mockGoogleApiClient = mock(GoogleApiClient.class);
    doReturn(true).when(mockGoogleApiClient).isConnected();

    LocationManager mockLocationManager = spy(mLocationManager);
    Whitebox.setInternalState(mockLocationManager, "googleApiClient", mockGoogleApiClient);

    FusedLocationProviderApi mockLocationProviderApi = mock(FusedLocationProviderApi.class);
    Whitebox.setInternalState(LocationServices.class, "FusedLocationApi", mockLocationProviderApi);

    // Testing when a customer did not disableLocationCollection.
    Whitebox.invokeMethod(mockLocationManager, "requestLocation");
    verify(mockLocationProviderApi).requestLocationUpdates(any(GoogleApiClient.class),
        any(LocationRequest.class), any(LocationListener.class));

    // Testing when a customer disableLocationCollection.
    Leanplum.disableLocationCollection();
    Whitebox.invokeMethod(mockLocationManager, "requestLocation");
    verifyNoMoreInteractions(mockLocationProviderApi);
  }
}
