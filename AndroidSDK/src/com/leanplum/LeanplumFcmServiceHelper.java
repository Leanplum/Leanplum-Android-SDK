// Copyright 2017, Leanplum, Inc.
package com.leanplum;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * Helper class for enabling the FCM service declared in manifest without app restart.
 *
 * @author Anna Orlova
 */
public class LeanplumFcmServiceHelper extends IntentService {
  public LeanplumFcmServiceHelper() {
    super("LeanplumFcmServiceHelper");
  }

  @Override
  protected void onHandleIntent(@Nullable Intent intent) {
  }
}
