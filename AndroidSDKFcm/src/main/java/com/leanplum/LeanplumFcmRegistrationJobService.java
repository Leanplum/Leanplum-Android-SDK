/*
 * Copyright 2018, Leanplum, Inc. All rights reserved.
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

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;

import com.leanplum.internal.Log;

/**
 * Leanplum FCM registration Job Service for start registration service.
 *
 * @author Anna Orlova
 */
@TargetApi(21)
public class LeanplumFcmRegistrationJobService extends JobService {
  public static final int JOB_ID = -32373478;

  @Override
  public boolean onStartJob(JobParameters jobParameters) {
    try {
      Log.i("FCM InstanceID token needs an update");
      // Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
      Intent intent = new Intent(this, LeanplumPushRegistrationService.class);
      startService(intent);
    } catch (Throwable t) {
      Log.e("Couldn't start GCM registration service.", t);
    }
    return false;
  }

  @Override
  public boolean onStopJob(JobParameters jobParameters) {
    return false;
  }
}
