// Copyright 2013, Leanplum, Inc.

package com.leanplum;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import com.leanplum.annotations.Parser;
import com.leanplum.internal.Constants;

/**
 * Base class for your Application that handles lifecycle events.
 *
 * @author Andrew First
 */
@SuppressLint("Registered")
public class LeanplumApplication extends Application {
  private static LeanplumApplication instance;

  public static LeanplumApplication getInstance() {
    return instance;
  }

  public static Context getContext() {
    return instance;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    instance = this;
    LeanplumActivityHelper.enableLifecycleCallbacks(this);
    Parser.parseVariables(this);
  }

  @Override
  public Resources getResources() {
    if (Constants.isNoop() || !Leanplum.isResourceSyncingEnabled()) {
      return super.getResources();
    }
    return new LeanplumResources(super.getResources());
  }
}
