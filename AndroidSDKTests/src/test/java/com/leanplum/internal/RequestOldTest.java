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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
public class RequestOldTest extends TestCase {
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
      RequestOld request = new RequestOld(POST, RequestBuilder.ACTION_START, null);
      Map<String, Object> args = request.createArgsDictionary();
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
        RequestOld request = new RequestOld(POST, RequestBuilder.ACTION_START, params);
        RequestSender.getInstance().sendIfConnected(request);

        latch.countDown();
      }
    });

    operationQueue.addOperation(new Runnable() {
      @Override
      public void run() {
        RequestOld request = new RequestOld(POST, RequestBuilder.ACTION_START, params);
        RequestSender.getInstance().sendIfConnected(request);

        latch.countDown();
      }
    });

    latch.await();
  }

  /**
   * Tests the testRemoveIrrelevantBackgroundStartRequests method.
   * <p>
   * <p>
   * <code>start(B), start(B), start(F), track, start(B), track, start(F), resumeSession</code>
   * <p>
   * where <code>start(B)</code> indicates a start in the background, and <code>start(F)</code> one
   * in the foreground.
   * <p>
   * In this case the first two <code>start(B)</code> can be dropped because they don't contribute
   * any relevant information for the batch call.
   * <p>
   * Essentially we drop every <code>start(B)</code> call, that is directly followed by any kind of
   * a <code>start</code> call.
   */
  @Test
  public void testRemoveIrrelevantBackgroundStartRequests() throws Exception {
    // Prepare testable objects and method.
    RequestOld request = new RequestOld("POST", RequestBuilder.ACTION_START, null);
    Method removeIrrelevantBackgroundStartRequests =
        RequestSender.class.getDeclaredMethod("removeIrrelevantBackgroundStartRequests", List.class);
    removeIrrelevantBackgroundStartRequests.setAccessible(true);

    // Invoke method with specific test data.
    // Expectation: No request returned.
    List unsentRequests = RequestSender.getInstance().getUnsentRequests(1.0);
    assertNotNull(unsentRequests);
    assertEquals(0, unsentRequests.size());

    // Regular start request.
    // Expectation: One request returned.
    RequestSender.getInstance().sendEventually(request);

    // loop to complete all tasks
    ShadowLooper.idleMainLooperConstantly(true);

    unsentRequests = RequestSender.getInstance().getUnsentRequests(1.0);
    assertNotNull(unsentRequests);
    assertEquals(1, unsentRequests.size());
    RequestSender.getInstance().deleteSentRequests(unsentRequests.size());

    // Two foreground start requests.
    // Expectation: Both foreground start request returned.
    RequestOld req = new RequestOld("POST", RequestBuilder.ACTION_START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(false));
      put("fg", "1");
    }});
    RequestSender.getInstance().sendEventually(req);

    req = new RequestOld("POST", RequestBuilder.ACTION_START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(false));
      put("fg", "2");
    }});
    RequestSender.getInstance().sendEventually(req);

    unsentRequests = RequestSender.getInstance().getUnsentRequests(1.0);
    assertNotNull(unsentRequests);
    assertEquals(2, unsentRequests.size());
    RequestSender.getInstance().deleteSentRequests(unsentRequests.size());

    // One background start request followed by a foreground start request.
    // Expectation: Only one foreground start request returned.
    req = new RequestOld("POST", RequestBuilder.ACTION_START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(true));
      put("bg", "1");
    }});
    RequestSender.getInstance().sendEventually(req);

    req = new RequestOld("POST", RequestBuilder.ACTION_START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(false));
      put("fg", "1");
    }});
    RequestSender.getInstance().sendEventually(req);

    unsentRequests = RequestSender.getInstance().getUnsentRequests(1.0);
    List unsentRequestsData =
        (List) removeIrrelevantBackgroundStartRequests.invoke(RequestOld.class, unsentRequests);
    assertNotNull(unsentRequestsData);
    assertEquals(1, unsentRequestsData.size());
    assertEquals("1", ((Map) unsentRequestsData.get(0)).get("fg"));
    RequestSender.getInstance().deleteSentRequests(unsentRequests.size());

    // Two background start request followed by a foreground start requests.
    // Expectation: Only one foreground start request returned.
    req = new RequestOld("POST", RequestBuilder.ACTION_START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(true));
      put("bg", "1");
    }});
    RequestSender.getInstance().sendEventually(req);

    req = new RequestOld("POST", RequestBuilder.ACTION_START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(true));
      put("bg", "2");
    }});
    RequestSender.getInstance().sendEventually(req);

    req = new RequestOld("POST", RequestBuilder.ACTION_START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(false));
      put("fg", "1");
    }});
    RequestSender.getInstance().sendEventually(req);

    unsentRequests = RequestSender.getInstance().getUnsentRequests(1.0);

    assertNotNull(unsentRequests);
    unsentRequestsData =
        (List) removeIrrelevantBackgroundStartRequests.invoke(RequestOld.class, unsentRequests);
    assertEquals(1, unsentRequestsData.size());
    assertEquals("1", ((Map) unsentRequestsData.get(0)).get("fg"));
    RequestSender.getInstance().deleteSentRequests(unsentRequests.size());

    // A foreground start request followed by two background start requests.
    // Expectation: Should keep the foreground and the last background start request returned.
    req = new RequestOld("POST", RequestBuilder.ACTION_START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(false));
      put("fg", "1");
    }});
    RequestSender.getInstance().sendEventually(req);

    req = new RequestOld("POST", RequestBuilder.ACTION_START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(true));
      put("bg", "1");
    }});
    RequestSender.getInstance().sendEventually(req);

    req = new RequestOld("POST", RequestBuilder.ACTION_START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(true));
      put("bg", "2");
    }});
    RequestSender.getInstance().sendEventually(req);

    unsentRequests = RequestSender.getInstance().getUnsentRequests(1.0);
    assertNotNull(unsentRequests);
    unsentRequestsData =
        (List) removeIrrelevantBackgroundStartRequests.invoke(RequestOld.class, unsentRequests);
    assertEquals(2, unsentRequestsData.size());
    assertEquals("2", ((Map) unsentRequestsData.get(1)).get("bg"));
    RequestSender.getInstance().deleteSentRequests(unsentRequests.size());

    // A foreground start request followed by two background start requests.
    // Expectation: Should keep the foreground and the last background start request returned.
    req = new RequestOld("POST", RequestBuilder.ACTION_START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(false));
      put("fg", "1");
    }});
    RequestSender.getInstance().sendEventually(req);

    req = new RequestOld("POST", RequestBuilder.ACTION_START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(false));
      put("bg", "1");
    }});
    RequestSender.getInstance().sendEventually(req);

    req = new RequestOld("POST", RequestBuilder.ACTION_START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(true));
      put("bg", "2");
    }});
    RequestSender.getInstance().sendEventually(req);

    unsentRequests = RequestSender.getInstance().getUnsentRequests(1.0);
    unsentRequestsData =
        (List) removeIrrelevantBackgroundStartRequests.invoke(RequestOld.class, unsentRequests);
    assertNotNull(unsentRequestsData);
    assertEquals(3, unsentRequestsData.size());
    RequestSender.getInstance().deleteSentRequests(unsentRequests.size());
    LeanplumEventDataManagerTest.setDatabaseToNull();
  }

  // Given a list of 5000 that generate OOM exceptions
  // we want to generate the requests to send
  // The list should try and get a smaller fraction of the available requests
  @Test
  public void testJsonEncodeUnsentRequestsWithExceptionLargeNumbers() throws Exception {
    RequestSender.RequestsWithEncoding requestsWithEncoding;
    // Prepare testable objects and method.
    RequestSender requestSender = spy(new RequestSender());
    RequestOld request = new RequestOld("POST", RequestBuilder.ACTION_START, null);
    requestSender.sendEventually(request); // first request added

    for (int i = 0; i < 5000; i++) { // remaining requests to make up 5000
      RequestOld startRequest = new RequestOld("POST", RequestBuilder.ACTION_START, null);
      requestSender.sendEventually(startRequest);
    }

    // Expectation: 5000 requests returned.
    requestsWithEncoding = requestSender.getRequestsWithEncodedStringStoredRequests(1.0);

    assertNotNull(requestsWithEncoding.unsentRequests);
    assertNotNull(requestsWithEncoding.requestsToSend);
    assertNotNull(requestsWithEncoding.jsonEncodedString);
    assertEquals(5000, requestsWithEncoding.unsentRequests.size());

    // Throw OOM on 5000 requests
    // Expectation: 2500 requests returned.
    when(requestSender.getUnsentRequests(1.0)).thenThrow(OutOfMemoryError.class);
    requestsWithEncoding = requestSender.getRequestsWithEncodedStringStoredRequests(1.0);

    assertNotNull(requestsWithEncoding.unsentRequests);
    assertNotNull(requestsWithEncoding.requestsToSend);
    assertNotNull(requestsWithEncoding.jsonEncodedString);
    assertEquals(2500, requestsWithEncoding.unsentRequests.size());

    // Throw OOM on 2500, 5000 requests
    // Expectation: 1250 requests returned.
    when(requestSender.getUnsentRequests(0.5)).thenThrow(OutOfMemoryError.class);
    requestsWithEncoding = requestSender.getRequestsWithEncodedStringStoredRequests(1.0);
    assertEquals(1250, requestsWithEncoding.unsentRequests.size());

    // Throw OOM on serializing any finite number of requests (extreme condition)
    // Expectation: Determine only 0 requests to be sent
    when(requestSender.getUnsentRequests(not(eq(0)))).thenThrow(OutOfMemoryError.class);
    requestsWithEncoding = requestSender.getRequestsWithEncodedStringStoredRequests(1.0);

    assertNotNull(requestsWithEncoding.unsentRequests);
    assertNotNull(requestsWithEncoding.requestsToSend);
    assertNotNull(requestsWithEncoding.jsonEncodedString);
    assertEquals(0, requestsWithEncoding.unsentRequests.size());
    assertEquals(0, requestsWithEncoding.requestsToSend.size());
    assertEquals("{\"data\":[]}", requestsWithEncoding.jsonEncodedString);

  }

  // Given a list of unsent requests that generate an OOM exception
  // we want to generate the requests to send
  // The list should try and get a smaller fraction of the available requests
  @Test
  public void testJsonEncodeUnsentRequestsWithException() {
    List<Map<String, Object>> requests = mockRequests(4);

    RequestSender realRequestSender = new RequestSender();
    RequestSender requestSender = spy(realRequestSender);
    when(requestSender.getUnsentRequests(1.0)).thenThrow(OutOfMemoryError.class);
    when(requestSender.getUnsentRequests(0.5)).thenThrow(OutOfMemoryError.class);
    when(requestSender.getUnsentRequests(0.25)).thenReturn(requests);

    RequestSender.RequestsWithEncoding requestsWithEncoding = requestSender.getRequestsWithEncodedStringStoredRequests(1.0);

    assertEquals(4, requestsWithEncoding .unsentRequests.size());
    assertEquals(4, requestsWithEncoding .requestsToSend.size());
    final String expectedJson =  "{\"data\":[{\"0\":\"testData\"},{\"1\":\"testData\"},{\"2\":\"testData\"},{\"3\":\"testData\"}]}";
    assertEquals(expectedJson, requestsWithEncoding.jsonEncodedString);
  }

  // Given a list of unsent requests
  // we want to generate the requests to send
  // The String should have the expected format, and the request count should be equal to the
  // number of unsent requests
  @Test
  public void testJsonEncodeUnsentRequests() {
    List<Map<String, Object>> requests = mockRequests(4);

    RequestSender requestSender = spy(new RequestSender());
    when(requestSender.getUnsentRequests(1.0)).thenReturn(requests);

    RequestSender.RequestsWithEncoding requestsWithEncoding = requestSender.getRequestsWithEncodedStringStoredRequests(1.0);

    assertEquals(4, requestsWithEncoding.unsentRequests.size());
    assertEquals(4, requestsWithEncoding.requestsToSend.size());
    final String expectedJson =  "{\"data\":[{\"0\":\"testData\"},{\"1\":\"testData\"},{\"2\":\"testData\"},{\"3\":\"testData\"}]}";
    assertEquals(expectedJson, requestsWithEncoding.jsonEncodedString);
  }

  // Given a list of requests
  // we want to encode to a JSON String
  // The String should have the expected format
  @Test
  public void testGetRequestsWithEncodedStringStoredRequests() {
    List<Map<String, Object>> requests = mockRequests(4);
    String json = RequestSender.getInstance().jsonEncodeRequests(requests);

    final String expectedJson =  "{\"data\":[{\"0\":\"testData\"},{\"1\":\"testData\"},{\"2\":\"testData\"},{\"3\":\"testData\"}]}";
    assertEquals(json, expectedJson);
  }

  private List<Map<String, Object>> mockRequests(int requestSize) {
    List<Map<String, Object>> requests = new ArrayList<>();

    for (int i=0; i < requestSize; i++) {
      Map<String, Object> request = new HashMap<String, Object>();
      request.put(Integer.toString(i), "testData");
      requests.add(request);
    }
    return requests;
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
    RequestOld request = new RequestOld(POST, RequestBuilder.ACTION_START, params);
    request.onError(new RequestOld.ErrorCallback() {
      @Override
      public void error(Exception e) {
        assertNotNull(e);
        semaphore.release();
      }
    });
    APIConfig.getInstance().setAppId("fskadfshdbfa", "wee5w4waer422323");

    // When the request is sent.
    RequestSender.getInstance().sendIfConnected(request);

    Leanplum.setApplicationContext(context);
  }
}
