package com.leanplum.monitoring;

import android.content.Context;

import com.leanplum.internal.Log;

import java.lang.reflect.Constructor;

public class LeanplumCrashHandler {
  private static final String RAYGUN_CRASH_REPORTER_CLASS =
      "com.leanplum.monitoring.internal.RaygunCrashReporter";
  private static final LeanplumCrashHandler ourInstance = new LeanplumCrashHandler();

  public static LeanplumCrashHandler getInstance() {
    return ourInstance;
  }

  private LeanplumCrashReporting crashReporter;

  private LeanplumCrashHandler() {}

  public void setContext(Context context) {
    try {
      Class<?> clazz = Class.forName(RAYGUN_CRASH_REPORTER_CLASS);
      Constructor<?> constructor = clazz.getConstructor(Context.class);
      crashReporter = (LeanplumCrashReporting) constructor.newInstance(context);
    } catch (Throwable t) {
      Log.e("LeanplumCrashHandler", t);
    }
  }

  public void reportException(Throwable exception) {
    if (crashReporter != null) {
      try {
        crashReporter.reportException(exception);
      } catch (Throwable t) {
        Log.e("LeanplumCrashHandler", t);
      }
    }
  }
}
