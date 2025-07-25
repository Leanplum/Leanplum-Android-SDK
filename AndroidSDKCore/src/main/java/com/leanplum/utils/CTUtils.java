package com.leanplum.utils;

import android.annotation.SuppressLint;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.LocalDataStore;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.CTExecutors;
import java.util.HashMap;
import java.util.Map;

public class CTUtils {

  @SuppressLint("RestrictedApi")
  public static void ensureLocalDataStoreValue(String key, CleverTapAPI cleverTapApi) {
    LocalDataStore localDataStore = cleverTapApi.getCoreState().getLocalDataStore();
    Object value = localDataStore.getProfileProperty(key);
    if (value == null) {
      Map<String, Object> map = new HashMap<>();
      map.put(key, "");
      localDataStore.updateProfileFields(map);
    }
  }

  @SuppressLint("RestrictedApi")
  public static void addMultiValueForKey(String key, String value, CleverTapAPI cleverTapApi) {
    CleverTapInstanceConfig config = cleverTapApi.getCoreState().getConfig();

    CTExecutors executors = CTExecutorFactory.executors(config);
    executors
            .postAsyncSafelyTask()
            .execute("CTUtils", () -> {
              ensureLocalDataStoreValue(key, cleverTapApi);
              cleverTapApi.addMultiValueForKey(key, value);
              return null;
            });
  }
}
