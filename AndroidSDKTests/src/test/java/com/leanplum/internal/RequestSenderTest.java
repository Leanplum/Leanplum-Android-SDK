/*
 * Copyright 2016, Leanplum, Inc. All rights reserved.
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

import android.app.Application;
import android.content.Context;

import com.leanplum.Leanplum;
import com.leanplum.__setup.LeanplumTestApp;

import com.leanplum.internal.Request.RequestType;
import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * @author Ben Marten
 */
@RunWith(RobolectricTestRunner.class)
@Config(
        sdk = 16,
        application = LeanplumTestApp.class,
        shadows = {
                ShadowLooper.class,
        }
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "org.json.*", "org.powermock.*"})
public class RequestSenderTest extends TestCase {
  public final String POST = "POST";
  /**
   * Runs before every test case.
   */
  @Before
  public void setUp() throws Exception {
    Application context = RuntimeEnvironment.application;
    assertNotNull(context);

    Leanplum.setApplicationContext(context);

    ReflectionHelpers.setStaticField(LeanplumEventDataManager.class, "instance", null);
    LeanplumEventDataManager.sharedInstance();

    ShadowOperationQueue shadowOperationQueue = new ShadowOperationQueue();

    Field instance = OperationQueue.class.getDeclaredField("instance");
    instance.setAccessible(true);
    instance.set(instance, shadowOperationQueue);

    ShadowLooper.idleMainLooperConstantly(true);
  }

  /** Test that request include a generated request id **/
  @Test
  public void testCreateArgsDictionaryShouldIncludeRequestId() {
      Request request = new Request(POST, RequestBuilder.ACTION_START, RequestType.DEFAULT, null);
      Map<String, Object> args = RequestSender.createArgsDictionary(request);
      assertTrue(args.containsKey(Constants.Params.REQUEST_ID));
  }

  /** Test that read writes happened sequentially when calling sendNow(). */
  @Test
  public void shouldWriteRequestAndSendInSequence() throws InterruptedException {
    final Map<String, Object> params = new HashMap<>();
    params.put("data1", "value1");
    params.put("data2", "value2");

    APIConfig.getInstance().setAppId("appId", "accessKey");

    final CountDownLatch latch = new CountDownLatch(2);

    ShadowOperationQueue operationQueue = new ShadowOperationQueue();
    operationQueue.addOperation(new Runnable() {
      @Override
      public void run() {
        Request request = new Request(
            POST,
            RequestBuilder.ACTION_START,
            RequestType.IMMEDIATE,
            params);
        RequestSender.getInstance().send(request);

        latch.countDown();
      }
    });

    operationQueue.addOperation(new Runnable() {
      @Override
      public void run() {
        Request request = new Request(
            POST,
            RequestBuilder.ACTION_START,
            RequestType.IMMEDIATE,
            params);
        RequestSender.getInstance().send(request);

        latch.countDown();
      }
    });

    latch.await();
  }

  @Test
  public void testNotSendingIfContextIsNull() {
    Context context = Leanplum.getContext();
    Leanplum.setApplicationContext(null);

    final Semaphore semaphore = new Semaphore(1);
    semaphore.tryAcquire();

    // Given a request.
    Map<String, Object> params = new HashMap<>();
    params.put("data1", "value1");
    params.put("data2", "value2");
    Request request = new Request(POST, RequestBuilder.ACTION_START, RequestType.IMMEDIATE, params);
    request.onError(new Request.ErrorCallback() {
      @Override
      public void error(Exception e) {
        assertNotNull(e);
        semaphore.release();
      }
    });
    APIConfig.getInstance().setAppId("fskadfshdbfa", "wee5w4waer422323");

    // When the request is sent.
    RequestSender.getInstance().send(request);

    Leanplum.setApplicationContext(context);
  }
}
