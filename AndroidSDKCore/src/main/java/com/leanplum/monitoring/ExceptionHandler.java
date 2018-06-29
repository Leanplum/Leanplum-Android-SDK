package com.leanplum.monitoring;

import android.content.Context;

import com.leanplum.internal.Log;

import java.lang.reflect.Constructor;

public class ExceptionHandler {
  private static final String RAYGUN_CRASH_REPORTER_CLASS =
          "com.leanplum.monitoring.internal.RaygunCrashReporter";
  private static final ExceptionHandler instance = new ExceptionHandler();

  private ExceptionReporting exceptionReporting;

  private CrashHandler() {}

  public static ExceptionHandler getInstance() {
    return instance;
  }

  public void setContext(Context context) {
    try {
      Class<?> clazz = Class.forName(RAYGUN_CRASH_REPORTER_CLASS);
      Constructor<?> constructor = clazz.getConstructor(Context.class);
      exceptionReporting = (ExceptionReporting) constructor.newInstance(context);
    } catch (Throwable t) {
      Log.e("LeanplumCrashHandler", t);
    }
  }

  public void reportException(Throwable exception) {
    if (exceptionReporting != null) {
      try {
        exceptionReporting.reportException(exception);
      } catch (Throwable t) {
        Log.e("LeanplumCrashHandler", t);
      }
    }
  }
}
