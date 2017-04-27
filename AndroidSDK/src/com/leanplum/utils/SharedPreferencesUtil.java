// Copyright 2017, Leanplum, Inc.

package com.leanplum.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Shared preferences manipulation utilities.
 *
 * @author Anna Orlova
 */
public class SharedPreferencesUtil {
  public static final String DEFAULT_STRING_VALUE = "";

  /**
   * Gets string value for key from shared preferences.
   *
   * @param context Application context.
   * @param sharedPreferenceName Shared preference name.
   * @param key Key of preference.
   * @return String Value for key, if here no value - return DEFAULT_STRING_VALUE.
   */
  public static String getString(Context context, String sharedPreferenceName, String key) {
    final SharedPreferences sharedPreferences = getPreferences(context, sharedPreferenceName);
    return sharedPreferences.getString(key, DEFAULT_STRING_VALUE);
  }

  /**
   * Get application shared preferences with sharedPreferenceName name.
   *
   * @param context Application context.
   * @param sharedPreferenceName Shared preference name.
   * @return Application's {@code SharedPreferences}.
   */
  private static SharedPreferences getPreferences(Context context, String sharedPreferenceName) {
    return context.getSharedPreferences(sharedPreferenceName, Context.MODE_PRIVATE);
  }

  /**
   * Sets string value for provided key to shared preference with sharedPreferenceName name.
   *
   * @param context application context.
   * @param sharedPreferenceName shared preference name.
   * @param key key of preference.
   * @param value value of preference.
   */
  public static void setString(Context context, String sharedPreferenceName, String key,
      String value) {
    final SharedPreferences sharedPreferences = getPreferences(context, sharedPreferenceName);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(key, value);
    try {
      editor.apply();
    } catch (NoSuchMethodError e) {
      editor.commit();
    }
  }
}
