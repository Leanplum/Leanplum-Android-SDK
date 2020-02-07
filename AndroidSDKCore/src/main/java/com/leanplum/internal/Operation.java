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

package com.leanplum.internal;

import android.os.Handler;
import android.os.Looper;


public abstract class Operation implements Runnable {

    private static final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Helper methods to execute runnable on main thread
     *
     * @param runnable to executes
     */
    public static void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    /**
     * Helper methods to execute runnable on main thread after delay
     *
     * @param runnable to executes
     */
    public static void runOnUiThreadAfterDelay(Runnable runnable, long delayTimeMillis) {
        handler.postDelayed(runnable, delayTimeMillis);
    }

    /**
     * Helper methods to remove runnable from main thread
     *
     * @param runnable to remove
     */
    public static void removeOperationOnUiThread(Runnable runnable) {
        handler.removeCallbacks(runnable);
    }
}
