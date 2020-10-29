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

import android.app.Application;
import com.leanplum.Leanplum;
import com.leanplum.__setup.LeanplumTestApp;
import com.leanplum.internal.Request.RequestType;
import java.lang.reflect.Field;
import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = 16,
    application = LeanplumTestApp.class,
    shadows = {
        ShadowLooper.class,
    }
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "org.json.*", "org.powermock.*"})
public class RequestBatchFactoryTest {

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
  public void testRemoveIrrelevantBackgroundStartRequests() {
    // Prepare testable objects and method.
    Request request = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT, null);
    RequestBatchFactory batchFactory = new RequestBatchFactory();

    // Invoke method with specific test data.
    // Expectation: No request returned.
    List unsentRequests = batchFactory.getUnsentRequests(1.0);
    assertNotNull(unsentRequests);
    assertEquals(0, unsentRequests.size());

    // Regular start request.
    // Expectation: One request returned.
    RequestSender.getInstance().send(request);

    // loop to complete all tasks
    ShadowLooper.idleMainLooperConstantly(true);

    unsentRequests = batchFactory.getUnsentRequests(1.0);
    assertNotNull(unsentRequests);
    assertEquals(1, unsentRequests.size());
    RequestBatch batch = new RequestBatch(unsentRequests, null, null);
    batchFactory.deleteFinishedBatch(batch);

    // Two foreground start requests.
    // Expectation: Both foreground start request returned.
    Request req = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT,
        new HashMap<String, Object>() {{
          put(Constants.Params.BACKGROUND, Boolean.toString(false));
          put("fg", "1");
        }});
    RequestSender.getInstance().send(req);

    req = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT,
        new HashMap<String, Object>() {{
          put(Constants.Params.BACKGROUND, Boolean.toString(false));
          put("fg", "2");
        }});
    RequestSender.getInstance().send(req);

    unsentRequests = batchFactory.getUnsentRequests(1.0);
    assertNotNull(unsentRequests);
    assertEquals(2, unsentRequests.size());
    batch = new RequestBatch(unsentRequests, null, null);
    batchFactory.deleteFinishedBatch(batch);

    // One background start request followed by a foreground start request.
    // Expectation: Only one foreground start request returned.
    req = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT,
        new HashMap<String, Object>() {{
          put(Constants.Params.BACKGROUND, Boolean.toString(true));
          put("bg", "1");
        }});
    RequestSender.getInstance().send(req);

    req = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT,
        new HashMap<String, Object>() {{
          put(Constants.Params.BACKGROUND, Boolean.toString(false));
          put("fg", "1");
        }});
    RequestSender.getInstance().send(req);

    unsentRequests = batchFactory.getUnsentRequests(1.0);
    List unsentRequestsData =
        batchFactory.removeIrrelevantBackgroundStartRequests(unsentRequests);
    assertNotNull(unsentRequestsData);
    assertEquals(1, unsentRequestsData.size());
    assertEquals("1", ((Map) unsentRequestsData.get(0)).get("fg"));
    batch = new RequestBatch(unsentRequests, null, null);
    batchFactory.deleteFinishedBatch(batch);

    // Two background start request followed by a foreground start requests.
    // Expectation: Only one foreground start request returned.
    req = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT,
        new HashMap<String, Object>() {{
          put(Constants.Params.BACKGROUND, Boolean.toString(true));
          put("bg", "1");
        }});
    RequestSender.getInstance().send(req);

    req = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT,
        new HashMap<String, Object>() {{
          put(Constants.Params.BACKGROUND, Boolean.toString(true));
          put("bg", "2");
        }});
    RequestSender.getInstance().send(req);

    req = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT,
        new HashMap<String, Object>() {{
          put(Constants.Params.BACKGROUND, Boolean.toString(false));
          put("fg", "1");
        }});
    RequestSender.getInstance().send(req);

    unsentRequests = batchFactory.getUnsentRequests(1.0);

    assertNotNull(unsentRequests);
    unsentRequestsData = batchFactory.removeIrrelevantBackgroundStartRequests(unsentRequests);
    assertEquals(1, unsentRequestsData.size());
    assertEquals("1", ((Map) unsentRequestsData.get(0)).get("fg"));
    batch = new RequestBatch(unsentRequests, null, null);
    batchFactory.deleteFinishedBatch(batch);

    // A foreground start request followed by two background start requests.
    // Expectation: Should keep the foreground and the last background start request returned.
    req = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT,
        new HashMap<String, Object>() {{
          put(Constants.Params.BACKGROUND, Boolean.toString(false));
          put("fg", "1");
        }});
    RequestSender.getInstance().send(req);

    req = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT,
        new HashMap<String, Object>() {{
          put(Constants.Params.BACKGROUND, Boolean.toString(true));
          put("bg", "1");
        }});
    RequestSender.getInstance().send(req);

    req = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT,
        new HashMap<String, Object>() {{
          put(Constants.Params.BACKGROUND, Boolean.toString(true));
          put("bg", "2");
        }});
    RequestSender.getInstance().send(req);

    unsentRequests = batchFactory.getUnsentRequests(1.0);
    assertNotNull(unsentRequests);
    unsentRequestsData = batchFactory.removeIrrelevantBackgroundStartRequests(unsentRequests);
    assertEquals(2, unsentRequestsData.size());
    assertEquals("2", ((Map) unsentRequestsData.get(1)).get("bg"));
    batch = new RequestBatch(unsentRequests, null, null);
    batchFactory.deleteFinishedBatch(batch);

    // A foreground start request followed by two background start requests.
    // Expectation: Should keep the foreground and the last background start request returned.
    req = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT,
        new HashMap<String, Object>() {{
          put(Constants.Params.BACKGROUND, Boolean.toString(false));
          put("fg", "1");
        }});
    RequestSender.getInstance().send(req);

    req = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT,
        new HashMap<String, Object>() {{
          put(Constants.Params.BACKGROUND, Boolean.toString(false));
          put("bg", "1");
        }});
    RequestSender.getInstance().send(req);

    req = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT,
        new HashMap<String, Object>() {{
          put(Constants.Params.BACKGROUND, Boolean.toString(true));
          put("bg", "2");
        }});
    RequestSender.getInstance().send(req);

    unsentRequests = batchFactory.getUnsentRequests(1.0);
    unsentRequestsData = batchFactory.removeIrrelevantBackgroundStartRequests(unsentRequests);
    assertNotNull(unsentRequestsData);
    assertEquals(3, unsentRequestsData.size());
    batch = new RequestBatch(unsentRequests, null, null);
    batchFactory.deleteFinishedBatch(batch);
    LeanplumEventDataManagerTest.setDatabaseToNull();
  }

  // Given a list of 5000 that generate OOM exceptions
  // we want to generate the requests to send
  // The list should try and get a smaller fraction of the available requests
  @Test
  public void testJsonEncodeUnsentRequestsWithExceptionLargeNumbers() throws Exception {
    RequestBatch requestBatch;
    // Prepare testable objects and method.
    RequestSender requestSender = new RequestSender();
    Request request = new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT, null);
    requestSender.send(request); // first request added

    RequestBatchFactory batchFactory = spy(new RequestBatchFactory());

    for (int i = 0; i < 5000; i++) { // remaining requests to make up 5000
      Request startRequest =
          new Request("POST", RequestBuilder.ACTION_START, RequestType.DEFAULT, null);
      requestSender.send(startRequest);
    }

    // Expectation: 5000 requests returned.
    requestBatch = batchFactory.createNextBatch(1.0);

    assertNotNull(requestBatch.requests);
    assertNotNull(requestBatch.requestsToSend);
    assertNotNull(requestBatch.jsonEncoded);
    assertEquals(5000, requestBatch.requests.size());

    // Throw OOM on 5000 requests
    // Expectation: 2500 requests returned.
    when(batchFactory.getUnsentRequests(1.0)).thenThrow(OutOfMemoryError.class);
    requestBatch = batchFactory.createNextBatch(1.0);

    assertNotNull(requestBatch.requests);
    assertNotNull(requestBatch.requestsToSend);
    assertNotNull(requestBatch.jsonEncoded);
    assertEquals(2500, requestBatch.requests.size());

    // Throw OOM on 2500, 5000 requests
    // Expectation: 1250 requests returned.
    when(batchFactory.getUnsentRequests(0.5)).thenThrow(OutOfMemoryError.class);
    requestBatch = batchFactory.createNextBatch(1.0);
    assertEquals(1250, requestBatch.requests.size());

    // Throw OOM on serializing any finite number of requests (extreme condition)
    // Expectation: Determine only 0 requests to be sent
    when(batchFactory.getUnsentRequests(not(eq(0)))).thenThrow(OutOfMemoryError.class);
    requestBatch = batchFactory.createNextBatch(1.0);

    assertNotNull(requestBatch.requests);
    assertNotNull(requestBatch.requestsToSend);
    assertNotNull(requestBatch.jsonEncoded);
    assertEquals(0, requestBatch.requests.size());
    assertEquals(0, requestBatch.requestsToSend.size());
    assertEquals("{\"data\":[]}", requestBatch.jsonEncoded);

  }

  // Given a list of unsent requests that generate an OOM exception
  // we want to generate the requests to send
  // The list should try and get a smaller fraction of the available requests
  @Test
  public void testJsonEncodeUnsentRequestsWithException() {
    List<Map<String, Object>> requests = mockRequests(4);

    RequestBatchFactory batchFactory = spy(new RequestBatchFactory());
    when(batchFactory.getUnsentRequests(1.0)).thenThrow(OutOfMemoryError.class);
    when(batchFactory.getUnsentRequests(0.5)).thenThrow(OutOfMemoryError.class);
    when(batchFactory.getUnsentRequests(0.25)).thenReturn(requests);

    RequestBatch requestBatch = batchFactory.createNextBatch(1.0);

    assertEquals(4, requestBatch.requests.size());
    assertEquals(4, requestBatch.requestsToSend.size());
    final String expectedJson =  "{\"data\":[{\"0\":\"testData\"},{\"1\":\"testData\"},{\"2\":\"testData\"},{\"3\":\"testData\"}]}";
    assertEquals(expectedJson, requestBatch.jsonEncoded);
  }

  // Given a list of unsent requests
  // we want to generate the requests to send
  // The String should have the expected format, and the request count should be equal to the
  // number of unsent requests
  @Test
  public void testJsonEncodeUnsentRequests() {
    List<Map<String, Object>> requests = mockRequests(4);

    RequestBatchFactory batchFactory = spy(new RequestBatchFactory());
    when(batchFactory.getUnsentRequests(1.0)).thenReturn(requests);

    RequestBatch requestBatch = batchFactory.createNextBatch(1.0);

    assertEquals(4, requestBatch.requests.size());
    assertEquals(4, requestBatch.requestsToSend.size());
    final String expectedJson =  "{\"data\":[{\"0\":\"testData\"},{\"1\":\"testData\"},{\"2\":\"testData\"},{\"3\":\"testData\"}]}";
    assertEquals(expectedJson, requestBatch.jsonEncoded);
  }

  // Given a list of requests
  // we want to encode to a JSON String
  // The String should have the expected format
  @Test
  public void testJsonEncodeRequests() {
    RequestBatchFactory batchFactory = new RequestBatchFactory();
    List<Map<String, Object>> requests = mockRequests(4);
    String json = batchFactory.jsonEncodeRequests(requests);

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

}
