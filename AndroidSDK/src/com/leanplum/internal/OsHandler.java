// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal;

import android.os.Handler;
import android.os.Looper;

/**
 * Wraps Handler while allowing overriding of methods that are needed for unit testing
 *
 * @author kkafadarov
 */
public class OsHandler {
  // Posts to UI thread. Visible for testing.
  public static OsHandler instance;

  final Handler handler = new Handler(Looper.getMainLooper());

  public Boolean post(Runnable runnable) {
    return handler.post(runnable);
  }

  public Boolean postDelayed(Runnable runnable, long lng) {
    return handler.postDelayed(runnable, lng);
  }

  public static OsHandler getInstance() {
    if (instance == null) {
      instance = new OsHandler();
    }
    return instance;
  }
}
