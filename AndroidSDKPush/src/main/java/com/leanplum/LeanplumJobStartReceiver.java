/*
 * Copyright 2019, Leanplum, Inc. All rights reserved.
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.JobIntentService;

import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

/**
 * Broadcast receiver used for starting any JobIntentService.
 * Received intent needs to have {@link LeanplumJobStartReceiver#LP_EXTRA_SERVICE_CLASS} and
 * {@link LeanplumJobStartReceiver#LP_EXTRA_JOB_ID} populated to successfully enqueue work.
 */
public class LeanplumJobStartReceiver extends BroadcastReceiver {

    public static final String LP_EXTRA_SERVICE_CLASS = "com.leanplum.service_class";
    public static final String LP_EXTRA_JOB_ID = "com.leanplum.service_job_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (intent.getExtras() == null) {
                Log.d("Cannot enqueue work on JobIntentService, no extras in intent.");
                return;
            }

            String serviceName = intent.getStringExtra(LP_EXTRA_SERVICE_CLASS);
            int jobId = intent.getIntExtra(LP_EXTRA_JOB_ID, 0);

            Class service = Class.forName(serviceName);
            if (!JobIntentService.class.isAssignableFrom(service)) {
                Log.d("The service provided is not a type of JobIntentService.");
                return;
            }

            intent.setClass(context, service);

            JobIntentService.enqueueWork(context, service, jobId, intent);
        } catch (Exception e) {
            Log.exception(e);
        }
    }
}
