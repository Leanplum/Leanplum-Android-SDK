/*
 * Copyright 2013, Leanplum, Inc. All rights reserved.
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

package com.leanplum.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.TypedValue;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumDeviceIdMode;
import com.leanplum.internal.Constants.Params;
import com.leanplum.monitoring.ExceptionHandler;
import com.leanplum.utils.SharedPreferencesUtil;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.annotation.RequiresPermission;

/**
 * Leanplum utilities.
 *
 * @author Andrew First
 */
public class Util {

  private static final String ACCESS_WIFI_STATE_PERMISSION = "android.permission.ACCESS_WIFI_STATE";

  private static String appName = null;
  private static String versionName = null;

  private static boolean hasPlayServicesCalled = false;
  private static boolean hasPlayServices = false;

  public static class DeviceIdInfo {
    public final String id;
    public boolean limitAdTracking;

    public DeviceIdInfo(String id) {
      this.id = id;
    }

    public DeviceIdInfo(String id, boolean limitAdTracking) {
      this.id = id;
      this.limitAdTracking = limitAdTracking;
    }
  }

  /**
   * Gets MD5 hash of given string.
   *
   * @param string String for which want to have MD5 hash.
   * @return String with MD5 hash of given string.
   */
  private static String md5(String string) throws Exception {
    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
    messageDigest.update(string.getBytes(Charset.forName("UTF-8")));
    byte digest[] = messageDigest.digest();

    StringBuilder result = new StringBuilder();
    for (byte dig : digest) {
      result.append(String.format("%02x", dig));
    }
    return result.toString();
  }

  /**
   * Gets SHA-256 hash of given string.
   */
  public static String sha256(String string) throws NoSuchAlgorithmException {
    MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
    messageDigest.update(string.getBytes(Charset.forName("UTF-8")));
    byte digest[] = messageDigest.digest();

    StringBuilder result = new StringBuilder();
    for (byte dig : digest) {
      result.append(String.format("%02x", dig));
    }
    return result.toString();
  }

  private static String checkDeviceId(String deviceIdMethod, String deviceId) {
    if (deviceId != null) {
      if (!isValidDeviceId(deviceId)) {
        Log.e("Invalid device id generated (" + deviceIdMethod + "): " + deviceId);
        return null;
      }
    }
    return deviceId;
  }

  @RequiresPermission(ACCESS_WIFI_STATE_PERMISSION)
  private static String getWifiMacAddressHash(Context context) {
    String logPrefix = "Skipping wifi device id; ";
    if (context.checkCallingOrSelfPermission(ACCESS_WIFI_STATE_PERMISSION) !=
        PackageManager.PERMISSION_GRANTED) {
      Log.d(logPrefix + "no wifi state permissions.");
      return null;
    }
    try {
      WifiManager manager = (WifiManager) context.getApplicationContext()
          .getSystemService(Context.WIFI_SERVICE);
      WifiInfo wifiInfo = manager.getConnectionInfo();
      if (wifiInfo == null) {
        Log.d(logPrefix + "null WifiInfo.");
        return null;
      }
      @SuppressLint("HardwareIds")
      String macAddress = wifiInfo.getMacAddress();
      if (macAddress == null || macAddress.isEmpty()) {
        Log.d(logPrefix + "no mac address returned.");
        return null;
      }
      if (Constants.INVALID_MAC_ADDRESS.equals(macAddress)) {
        // Note(ed): this is the expected case for Marshmallow and later, as they return
        // INVALID_MAC_ADDRESS; we intend to fall back to the Android id for Marshmallow devices.
        Log.d(logPrefix + "Marshmallow and later returns a fake MAC address.");
        return null;
      }
      @SuppressLint("HardwareIds")
      String deviceId = md5(wifiInfo.getMacAddress());
      Log.d("Using wifi device id: " + deviceId);
      return checkDeviceId("mac address", deviceId);
    } catch (Exception e) {
      Log.d("Error getting wifi MAC address.");
    }
    return null;
  }

  /**
   * Retrieves the advertising ID. Requires Google Play Services or androidX. Note: This method must
   * not run on the main thread.
   */
  private static DeviceIdInfo getAdvertisingId(Context caller) {
    try {
      final String[] classNames = {
          "androidx.ads.identifier.AdvertisingIdClient",
          "com.google.android.gms.ads.identifier.AdvertisingIdClient"
      };

      for (String name : classNames) {
        try {
          Object adInfo = Class.forName(name)
              .getMethod("getAdvertisingIdInfo", Context.class)
              .invoke(null, caller);

          if (name.equals(classNames[0])) {
            Method get = adInfo.getClass().getMethod("get", long.class, TimeUnit.class);
            adInfo = get.invoke(adInfo, 5, TimeUnit.SECONDS);
          }

          String id = checkDeviceId("advertising id", (String) adInfo.getClass().getMethod("getId")
              .invoke(adInfo));

          if (id != null) {
            boolean limitTracking = (Boolean) adInfo.getClass()
                .getMethod("isLimitAdTrackingEnabled")
                .invoke(adInfo);
            Log.d("Using advertising device id: " + id);
            return new DeviceIdInfo(id, limitTracking);
          }
        } catch (Throwable t) {
          Log.i("Couldn't get AdvertisingID using class: " + name);
        }
      }
    } catch (Throwable t) {
      Log.e("Error getting advertising ID. Google Play Services are not available: ", t);
    }
    return null;
  }

  private static String getAndroidId(Context context) {
    @SuppressLint("HardwareIds")
    String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
    if (androidId == null || androidId.isEmpty()) {
      Log.d("Skipping Android device id; no id returned.");
      return null;
    }
    if (Constants.INVALID_ANDROID_ID.equals(androidId)) {
      Log.d("Skipping Android device id; got invalid " + "device id: " + androidId);
      return null;
    }
    Log.d("Using Android device id: " + androidId);
    return checkDeviceId("android id", androidId);
  }

  /**
   * Final fallback device id -- generate a random device id.
   */
  private static String generateRandomDeviceId() {
    // Mark random IDs to be able to identify them.
    String randomId = UUID.randomUUID().toString() + "-LP";
    Log.d("Using generated device id: " + randomId);
    return randomId;
  }

  private static boolean isValidForCharset(String id, String charsetName) {
    CharsetEncoder encoder = null;
    try {
      Charset charset = Charset.forName(charsetName);
      encoder = charset.newEncoder();
    } catch (UnsupportedCharsetException e) {
      Log.d("Unsupported charset: " + charsetName);
    }
    if (encoder != null && !encoder.canEncode(id)) {
      Log.d("Invalid id (contains invalid characters): " + id);
      return false;
    }
    return true;
  }

  public static boolean isValidUserId(String userId) {
    String logPrefix = "Invalid user id ";
    if (userId == null || userId.isEmpty()) {
      Log.d(logPrefix + "(sentinel): " + userId);
      return false;
    }
    if (userId.length() > Constants.MAX_USER_ID_LENGTH) {
      Log.d(logPrefix + "(too long): " + userId);
      return false;
    }
    if (userId.contains("\n")) {
      Log.d(logPrefix + "(contains newline): " + userId);
      return false;
    }
    if (userId.contains("\"") || userId.contains("\'")) {
      Log.d(logPrefix + "(contains quotes): " + userId);
      return false;
    }
    return isValidForCharset(userId, "UTF-8");
  }

  public static boolean isValidDeviceId(String deviceId) {
    String logPrefix = "Invalid device id ";
    if (deviceId == null || deviceId.isEmpty() ||
        Constants.INVALID_ANDROID_ID.equals(deviceId) ||
        Constants.INVALID_MAC_ADDRESS_HASH.equals(deviceId) ||
        Constants.OLD_INVALID_MAC_ADDRESS_HASH.equals(deviceId)) {
      Log.d(logPrefix + "(sentinel): " + deviceId);
      return false;
    }
    if (deviceId.length() > Constants.MAX_DEVICE_ID_LENGTH) {
      Log.d(logPrefix + "(too long): " + deviceId);
      return false;
    }
    if (deviceId.contains("[")) {
      Log.d(logPrefix + "(contains brackets): " + deviceId);
      return false;
    }
    if (deviceId.contains("\n")) {
      Log.d(logPrefix + "(contains newline): " + deviceId);
      return false;
    }
    if (deviceId.contains(",")) {
      Log.d(logPrefix + "(contains comma): " + deviceId);
      return false;
    }
    if (deviceId.contains("\"") || deviceId.contains("\'")) {
      Log.d(logPrefix + "(contains quotes): " + deviceId);
      return false;
    }
    return isValidForCharset(deviceId, "US-ASCII");
  }

  @RequiresPermission(ACCESS_WIFI_STATE_PERMISSION)
  public static DeviceIdInfo getDeviceId(LeanplumDeviceIdMode mode) {
    Context context = Leanplum.getContext();

    if (mode.equals(LeanplumDeviceIdMode.ADVERTISING_ID)) {
      try {
        DeviceIdInfo info = getAdvertisingId(context);
        if (info != null) {
          return info;
        }
      } catch (Exception e) {
        Log.e("Error getting advertising ID: %s", e);
      }
    }

    if (isSimulator() || mode.equals(LeanplumDeviceIdMode.ANDROID_ID)) {
      String androidId = getAndroidId(context);
      if (androidId != null) {
        return new DeviceIdInfo(getAndroidId(context));
      }
    }

    String macAddressHash = getWifiMacAddressHash(context);
    if (macAddressHash != null) {
      return new DeviceIdInfo(macAddressHash);
    }

    String androidId = getAndroidId(context);
    if (androidId != null) {
      return new DeviceIdInfo(androidId);
    }

    return new DeviceIdInfo(generateRandomDeviceId());
  }

  public static String getVersionName() {
    if (versionName != null) {
      return versionName;
    }
    Context context = Leanplum.getContext();
    try {
      if (TextUtils.isEmpty(versionName)) {
        PackageInfo pInfo = context.getPackageManager().getPackageInfo(
            context.getPackageName(), 0);
        versionName = pInfo.versionName;
      }
    } catch (Exception e) {
      Log.d("Could not extract versionName from Manifest or PackageInfo.");
    }
    return versionName;
  }

  public static String getDeviceModel() {
    if (isSimulator()) {
      return "Android Emulator";
    }
    String manufacturer = Build.MANUFACTURER;
    String model = Build.MODEL;
    if (model.startsWith(manufacturer)) {
      return capitalize(model);
    } else {
      return capitalize(manufacturer) + " " + model;
    }
  }

  public static String getApplicationName(Context context) {
    if (appName != null) {
      return appName;
    }
    int stringId = context.getApplicationInfo().labelRes;
    if (stringId == 0) {
      appName = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
    } else {
      appName = context.getString(stringId);
    }
    return appName;
  }

  private static String capitalize(String s) {
    if (s == null || s.length() == 0) {
      return "";
    }
    char first = s.charAt(0);
    if (Character.isUpperCase(first)) {
      return s;
    } else {
      return Character.toUpperCase(first) + s.substring(1);
    }
  }

  @SuppressWarnings("SameReturnValue")
  public static String getSystemName() {
    return "Android OS";
  }

  @SuppressWarnings("SameReturnValue")
  public static String getSystemVersion() {
    return Build.VERSION.RELEASE;
  }

  public static boolean isSimulator() {
    String model = android.os.Build.MODEL.toLowerCase(Locale.getDefault());
    return model.contains("google_sdk")
        || model.contains("emulator")
        || model.contains("sdk");
  }

  public static String getDeviceName() {
    if (isSimulator()) {
      return "Android Emulator";
    }
    return getDeviceModel();
  }

  public static String getLocale() {
    String language = Locale.getDefault().getLanguage();
    if ("".equals(language)) {
      language = "xx";
    }
    String country = Locale.getDefault().getCountry();
    if ("".equals(country)) {
      country = "XX";
    }
    return language + "_" + country;
  }

  /**
   * Check whether the device has a network connection. WARNING: Does not check for available
   * internet connection! use isOnline()
   *
   * @return Whether a network connection is available or not.
   */
  public static boolean isConnected() {
    try {
      Context context = Leanplum.getContext();
      ConnectivityManager manager = (ConnectivityManager) context.getSystemService(
          Context.CONNECTIVITY_SERVICE);
      if (manager == null) {
        return false;
      }
      NetworkInfo netInfo = manager.getActiveNetworkInfo();
      return !(netInfo == null || !netInfo.isConnectedOrConnecting());
    } catch (Exception e) {
      Log.d("Error getting connectivity info", e);
      return false;
    }
  }

  public static <T> T multiIndex(Map<?, ?> map, Object... indices) {
    if (map == null) {
      return null;
    }
    Object current = map;
    for (Object index : indices) {
      if (!((Map<?, ?>) current).containsKey(index)) {
        return null;
      }
      current = ((Map<?, ?>) current).get(index);
    }
    return CollectionUtil.uncheckedCast(current);
  }

  /**
   * Check the device to make sure it has the Google Play Services APK. If it doesn't, display a
   * dialog that allows users to download the APK from the Google Play Store or enable it in the
   * device's system settings.
   */
  public static boolean hasPlayServices() {
    if (hasPlayServicesCalled) {
      return hasPlayServices;
    }
    Context context = Leanplum.getContext();
    PackageManager packageManager = context.getPackageManager();
    PackageInfo packageInfo;
    try {
      packageInfo = packageManager.getPackageInfo("com.google.android.gms",
          PackageManager.GET_SIGNATURES);
    } catch (PackageManager.NameNotFoundException e) {
      hasPlayServicesCalled = true;
      hasPlayServices = false;
      return false;
    }
    if (packageInfo.versionCode < 4242000) {
      Log.i("Google Play services version is too old: " + packageInfo.versionCode);
      hasPlayServicesCalled = true;
      hasPlayServices = false;
      return false;
    }
    ApplicationInfo info;
    try {
      info = packageManager.getApplicationInfo("com.google.android.gms", 0);
    } catch (PackageManager.NameNotFoundException e) {
      hasPlayServicesCalled = true;
      hasPlayServices = false;
      return false;
    }
    hasPlayServicesCalled = true;
    hasPlayServices = info.enabled;
    return info.enabled;
  }

  public static boolean isInBackground() {
    return (LeanplumActivityHelper.getCurrentActivity() == null ||
        LeanplumActivityHelper.isActivityPaused());
  }

  /**
   * Include install time and last app update time in start API params the first time that the app
   * runs with Leanplum.
   */
  public static void initializePreLeanplumInstall(Map<String, Object> params) {
    Context context = Leanplum.getContext();
    SharedPreferences preferences = context.getSharedPreferences("__leanplum__",
        Context.MODE_PRIVATE);
    if (preferences.getBoolean(Constants.Keys.INSTALL_TIME_INITIALIZED, false)) {
      return;
    }

    PackageManager packageManager = context.getPackageManager();
    String packageName = context.getPackageName();
    setInstallTime(params, packageManager, packageName);
    setUpdateTime(params, packageManager, packageName);

    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean(Constants.Keys.INSTALL_TIME_INITIALIZED, true);
    SharedPreferencesUtil.commitChanges(editor);
  }

  /**
   * Set install time from package manager and update time from apk file modification time.
   */
  private static void setInstallTime(Map<String, Object> params, PackageManager packageManager,
      String packageName) {
    try {
      PackageInfo info = packageManager.getPackageInfo(packageName, 0);
      params.put(Params.INSTALL_DATE, "" + (info.firstInstallTime / 1000.0));
    } catch (NameNotFoundException e) {
      Log.d("Failed to find package info: " + e);
    }
  }

  /**
   * Set update time from apk file modification time.
   */
  private static void setUpdateTime(Map<String, Object> params, PackageManager packageManager,
      String packageName) {
    try {
      ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
      File apkFile = new File(info.sourceDir);
      if (apkFile.exists()) {
        params.put(Constants.Params.UPDATE_DATE, "" + (apkFile.lastModified() / 1000.0));
      }
    } catch (Throwable t) {
      Log.d("Failed to find package info: " + t);
    }
  }

  /**
   * Initialize exception handling in the SDK.
   */
  public static void initExceptionHandling(Context context) {
    ExceptionHandler.getInstance().setContext(context);
  }

  /**
   * Constructs a {@link HashMap} with the given keys and values.
   */
  public static <K, V> Map<K, V> newMap(K firstKey, V firstValue, Object... otherValues) {
    if (otherValues.length % 2 == 1) {
      throw new IllegalArgumentException("Must supply an even number of values.");
    }

    Map<K, V> map = new HashMap<>();
    map.put(firstKey, firstValue);
    for (int i = 0; i < otherValues.length; i += 2) {
      K otherKey = CollectionUtil.uncheckedCast(otherValues[i]);
      V otherValue = CollectionUtil.uncheckedCast(otherValues[i + 1]);
      map.put(otherKey, otherValue);
    }
    return map;
  }

  /**
   * Generates a Resource name from resourceId located in res/ folder.
   *
   * @param resourceId id of the resource, must be greater then 0.
   * @return resourceName in format folder/file.extension.
   */
  public static String generateResourceNameFromId(int resourceId) {
    try {
      if (resourceId <= 0) {
        Log.d("Provided resource id is invalid.");
        return null;
      }
      Resources resources = Leanplum.getContext().getResources();
      // Get entryName from resourceId, which represents a file name in res/ directory.
      String entryName = resources.getResourceEntryName(resourceId);
      // Get typeName from resourceId, which represents a folder where file is located in
      // res/ directory.
      String typeName = resources.getResourceTypeName(resourceId);

      // By using TypedValue we can get full path of a file with extension.
      TypedValue value = new TypedValue();
      resources.getValue(resourceId, value, true);

      // Regex matching to find real file extension, "image.img.png" will produce "png".
      String[] fullFileName = value.string.toString().split("\\.(?=[^\\.]+$)");
      String extension = "";
      // If extension is found, we will append dot before it.
      if (fullFileName.length == 2) {
        extension = "." + fullFileName[1];
      }

      // Return full resource name in format: drawable/image.png
      return typeName + "/" + entryName + extension;
    } catch (Exception e) {
      Log.e("Failed to generate resource name from provided resource id: %s", e.getMessage());
      Log.exception(e);
    }
    return null;
  }

  /**
   * Generates resource Id based on Resource name.
   *
   * @param resourceName name of the resource including folder and file extension.
   * @return id of the resource if found, 0 otherwise.
   */
  public static int generateIdFromResourceName(String resourceName) {
    // Split resource name to extract folder and file name.
    String[] parts = resourceName.split("/");
    if (parts.length == 2) {
      Resources resources = Leanplum.getContext().getResources();
      // Type name represents folder where file is contained.
      String typeName = parts[0];
      String fileName = parts[1];
      String entryName = fileName;
      // Since fileName contains extension we have to remove it,
      // to be able to get resource id.
      String[] fileParts = fileName.split("\\.(?=[^\\.]+$)");
      if (fileParts.length == 2) {
        entryName = fileParts[0];
      }
      // Get identifier for a file in specified directory
      if (!TextUtils.isEmpty(typeName) && !TextUtils.isEmpty(entryName)) {
        return resources.getIdentifier(entryName, typeName, Leanplum.getContext().getPackageName());
      }
    }
    Log.d("Could not extract resource id from provided resource name: ", resourceName);
    return 0;
  }
}
