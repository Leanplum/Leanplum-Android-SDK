// Copyright 2014, Leanplum, Inc.

package com.leanplum;

import java.util.Map;
import java.util.Set;

/**
 * Public interface to LocationManager. This is abstracted away so that the Google Play Services
 * dependencies are constrained to {@link LocationManagerImplementation}.
 *
 * @author Andrew First
 */
public interface LocationManager {
  void updateGeofencing();

  void updateUserLocation();

  void setRegionsData(Map<String, Object> regionData,
      Set<String> foregroundRegionNames, Set<String> backgroundRegionNames);
}
