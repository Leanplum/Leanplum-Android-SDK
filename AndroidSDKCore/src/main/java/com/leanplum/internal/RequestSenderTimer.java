/*
 * Copyright 2020, Leanplum, Inc. All rights reserved.
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

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.leanplum.EventsUploadInterval;
import com.leanplum.internal.Request.RequestType;

public class RequestSenderTimer {
  private static final RequestSenderTimer INSTANCE = new RequestSenderTimer();
  private EventsUploadInterval timerInterval = EventsUploadInterval.AT_MOST_15_MINUTES;

  private Runnable timerOperation;

  public static RequestSenderTimer get() {
    return INSTANCE;
  }

  @VisibleForTesting
  protected long getIntervalMillis() {
    return timerInterval.getMinutes() * 60 * 1000;
  }

  private void sendAllRequestsWithHeartbeat() {
    Request request = RequestBuilder
        .withHeartbeatAction()
        .andType(RequestType.IMMEDIATE)
        .create();
    RequestSender.getInstance().send(request);
  }

  @VisibleForTesting
  protected Runnable createTimerOperation() {
    return new Runnable() {
      @Override
      public void run() {
        sendAllRequestsWithHeartbeat();
        OperationQueue.sharedInstance().addOperationAfterDelay(timerOperation, getIntervalMillis());
      }
    };
  }

  public synchronized void start() {
    if (timerOperation != null)
      return;

    timerOperation = createTimerOperation();
    OperationQueue.sharedInstance().addOperationAfterDelay(timerOperation, getIntervalMillis());
  }

  public synchronized void stop() {
    if (timerOperation == null)
      return;

     OperationQueue.sharedInstance().removeOperation(timerOperation);
     timerOperation = null;
  }

  public void setTimerInterval(@NonNull EventsUploadInterval timerInterval) {
    this.timerInterval = timerInterval;
  }
}
