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
import android.os.HandlerThread;
import android.os.Process;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class OperationQueue {

    private static OperationQueue instance;

    private static final String OPERATION_QUEUE_NAME = "com.leanplum.operation_queue";
    private static final int OPERATION_QUEUE_PRIORITY = Process.THREAD_PRIORITY_DEFAULT;

    private HandlerThread handlerThread;
    private Handler handler;

    private Executor executor = Executors.newCachedThreadPool();

    public static OperationQueue sharedInstance() {
        if (instance == null) {
            instance = new OperationQueue();
        }
        return instance;
    }

    OperationQueue() {
        start();
    }

    /**
     * Start the underlying thread, call to this method is optional.
     */
    private void start() {
        if (handlerThread == null) {
            handlerThread = new HandlerThread(OPERATION_QUEUE_NAME, OPERATION_QUEUE_PRIORITY);
            handlerThread.start();
        }

        handler = new Handler(handlerThread.getLooper());
    }

    /**
     * Stop OperationQueue and remove all operations
     */
    private void stop() {
        removeAllOperations();

        handlerThread.quit();
        handlerThread = null;
    }

    /**
     * Add operation to Executor to be run in parallel
     * @param operation The operation that will be executed.
     */
    public void addParallelOperation(Runnable operation) {
        if (operation != null && executor != null) {
            executor.execute(operation);
        }
    }

    /**
     * Add operation to OperationQueue at the end
     * @param operation The operation that will be executed.
     * @return return true if the operation was successfully placed in to the operation queue. Returns false on failure.
     */
    public boolean addOperation(Runnable operation) {
        if (operation != null && handler != null) {
            return handler.post(new Operation(operation));
        }
        return false;
    }

    /**
     * Add operation to OperationQueue at the front
     * @param operation The operation that will be executed.
     * @return return true if the operation was successfully placed in to the operation queue. Returns false on failure.
     */
    public boolean addOperationAtFront(Runnable operation) {
        if (operation != null && handler != null) {
            return handler.postAtFrontOfQueue(new Operation(operation));
        }
        return false;
    }

    /**
     * Add operation to OperationQueue, to be run at a specific time given by millis.
     * @param operation operation The operation that will be executed.
     * @return return true if the operation was successfully placed in to the operation queue. Returns false on failure.
     */
    public boolean addOperationAtTime(Runnable operation, long millis) {
        if (operation != null && handler != null) {
            return handler.postAtTime(new Operation(operation), millis);
        }
        return false;
    }

    /**
     * Add operation to OperationQueue, to be run after the specific time given by delayMillis.
     * @param operation operation operation The operation that will be executed.
     * @param delayMillis
     * @return return true if the operation was successfully placed in to the operation queue. Returns false on failure.
     */
    public boolean addOperationAfterDelay(Runnable operation, long delayMillis) {
        if (operation != null && handler != null) {
            return handler.postDelayed(new Operation(operation), delayMillis);
        }
        return false;
    }

    /**
     * Remove all pending Operations that are in OperationQueue
     */
    public void removeAllOperations() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}
