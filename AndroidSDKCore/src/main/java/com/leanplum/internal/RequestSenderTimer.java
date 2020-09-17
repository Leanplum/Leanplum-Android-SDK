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

public class RequestSenderTimer {
  private static final RequestSenderTimer INSTANCE = new RequestSenderTimer();
  private static final long TIMER_MILLIS = 15 * 60 * 1000; // 15min

  private Runnable timerOperation;

  public static RequestSenderTimer get() {
    return INSTANCE;
  }

  private void sendAllRequestsWithHeartbeat() {
    Request request = RequestBuilder.withHeartbeatAction().create();
    RequestSender.getInstance().sendNow(request);
  }

  private Runnable createTimerOperation() {
    return new Runnable() {
      @Override
      public void run() {
        long eventsCount = LeanplumEventDataManager.sharedInstance().getEventsCount();
        if (eventsCount > 0) { // TODO send heartbeat no matter eventsCount?
          sendAllRequestsWithHeartbeat();
        }
        OperationQueue.sharedInstance().addOperationAfterDelay(timerOperation, TIMER_MILLIS);
      }
    };
  }

  public synchronized void start() {
    if (timerOperation != null)
      return;

    timerOperation = createTimerOperation();
    OperationQueue.sharedInstance().addOperationAfterDelay(timerOperation, TIMER_MILLIS);
  }

  public synchronized void stop() {
    if (timerOperation == null)
      return;

     OperationQueue.sharedInstance().removeOperation(timerOperation);
     timerOperation = null;
  }
}
