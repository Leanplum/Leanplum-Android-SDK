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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
    application = LeanplumTestApp.class
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "org.json.*", "org.powermock.*"})
public class RequestOldTest extends TestCase {
  public final String POST = "POST";
  /**
   * Runs before every test case.
   */
  @Before
  public void setUp() {
    Application context = RuntimeEnvironment.application;
    assertNotNull(context);
    Leanplum.setApplicationContext(context);
  }

  /** Test that request include a generated request id **/
  @Test
  public void testCreateArgsDictionaryShouldIncludeRequestId() {
      RequestOld request = new RequestOld(POST, Constants.Methods.START, null);
      Map<String, Object> args = request.createArgsDictionary();
      assertTrue(args.containsKey(RequestOld.REQUEST_ID_KEY));
  }

  /** Test that read writes happened sequentially when calling sendNow(). */
  @Test
  public void shouldWriteRequestAndSendInSequence() throws InterruptedException {
    // Given a request.
    Map<String, Object> params = new HashMap<>();
    params.put("data1", "value1");
    params.put("data2", "value2");
    final ThreadRequestSequenceRecorder threadRequestSequenceRecorder =
        new ThreadRequestSequenceRecorder();
    RequestOld request =
        new RequestOld(POST, Constants.Methods.START, params, threadRequestSequenceRecorder);
    request.setAppId("fskadfshdbfa", "wee5w4waer422323");

    new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  Thread.sleep(100);
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
                threadRequestSequenceRecorder.writeSemaphore.release(1);
              }
            })
        .start();

    // When the request is sent.
    request.sendIfConnected();

    threadRequestSequenceRecorder.testThreadSemaphore.tryAcquire(5000, TimeUnit.MILLISECONDS);

    // Then the request is written to the local db first, and then read and sent.
    threadRequestSequenceRecorder.assertCallSequence();
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
  public void testRemoveIrrelevantBackgroundStartRequests() throws NoSuchMethodException,
      InvocationTargetException, IllegalAccessException {
    LeanplumEventDataManager.init(Leanplum.getContext());
    // Prepare testable objects and method.
    RequestOld request = new RequestOld("POST", Constants.Methods.START, null);
    Method removeIrrelevantBackgroundStartRequests =
        RequestOld.class.getDeclaredMethod("removeIrrelevantBackgroundStartRequests", List.class);
    removeIrrelevantBackgroundStartRequests.setAccessible(true);

    // Invoke method with specific test data.
    // Expectation: No request returned.
    List unsentRequests = request.getUnsentRequests(1.0);
    assertNotNull(unsentRequests);
    assertEquals(0, unsentRequests.size());

    // Regular start request.
    // Expectation: One request returned.
    request.sendEventually();
    unsentRequests = request.getUnsentRequests(1.0);
    assertNotNull(unsentRequests);
    assertEquals(1, unsentRequests.size());
    RequestOld.deleteSentRequests(unsentRequests.size());

    // Two foreground start requests.
    // Expectation: Both foreground start request returned.
    new RequestOld("POST", Constants.Methods.START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(false));
      put("fg", "1");
    }}).sendEventually();
    new RequestOld("POST", Constants.Methods.START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(false));
      put("fg", "2");
    }}).sendEventually();
    unsentRequests = request.getUnsentRequests(1.0);
    assertNotNull(unsentRequests);
    assertEquals(2, unsentRequests.size());
    RequestOld.deleteSentRequests(unsentRequests.size());

    // One background start request followed by a foreground start request.
    // Expectation: Only one foreground start request returned.
    new RequestOld("POST", Constants.Methods.START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(true));
      put("bg", "1");
    }}).sendEventually();
    new RequestOld("POST", Constants.Methods.START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(false));
      put("fg", "1");
    }}).sendEventually();
    unsentRequests = request.getUnsentRequests(1.0);
    List unsentRequestsData =
        (List) removeIrrelevantBackgroundStartRequests.invoke(RequestOld.class, unsentRequests);
    assertNotNull(unsentRequestsData);
    assertEquals(1, unsentRequestsData.size());
    assertEquals("1", ((Map) unsentRequestsData.get(0)).get("fg"));
    RequestOld.deleteSentRequests(unsentRequests.size());

    // Two background start request followed by a foreground start requests.
    // Expectation: Only one foreground start request returned.
    new RequestOld("POST", Constants.Methods.START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(true));
      put("bg", "1");
    }}).sendEventually();
    new RequestOld("POST", Constants.Methods.START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(true));
      put("bg", "2");
    }}).sendEventually();
    new RequestOld("POST", Constants.Methods.START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(false));
      put("fg", "1");
    }}).sendEventually();
    unsentRequests = request.getUnsentRequests(1.0);

    assertNotNull(unsentRequests);
    unsentRequestsData =
        (List) removeIrrelevantBackgroundStartRequests.invoke(RequestOld.class, unsentRequests);
    assertEquals(1, unsentRequestsData.size());
    assertEquals("1", ((Map) unsentRequestsData.get(0)).get("fg"));
    RequestOld.deleteSentRequests(unsentRequests.size());

    // A foreground start request followed by two background start requests.
    // Expectation: Should keep the foreground and the last background start request returned.
    new RequestOld("POST", Constants.Methods.START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(false));
      put("fg", "1");
    }}).sendEventually();
    new RequestOld("POST", Constants.Methods.START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(true));
      put("bg", "1");
    }}).sendEventually();
    new RequestOld("POST", Constants.Methods.START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(true));
      put("bg", "2");
    }}).sendEventually();
    unsentRequests = request.getUnsentRequests(1.0);
    assertNotNull(unsentRequests);
    unsentRequestsData =
        (List) removeIrrelevantBackgroundStartRequests.invoke(RequestOld.class, unsentRequests);
    assertEquals(2, unsentRequestsData.size());
    assertEquals("2", ((Map) unsentRequestsData.get(1)).get("bg"));
    RequestOld.deleteSentRequests(unsentRequests.size());

    // A foreground start request followed by two background start requests.
    // Expectation: Should keep the foreground and the last background start request returned.
    new RequestOld("POST", Constants.Methods.START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(false));
      put("fg", "1");
    }}).sendEventually();
    new RequestOld("POST", Constants.Methods.START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(false));
      put("bg", "1");
    }}).sendEventually();
    new RequestOld("POST", Constants.Methods.START, new HashMap<String, Object>() {{
      put(Constants.Params.BACKGROUND, Boolean.toString(true));
      put("bg", "2");
    }}).sendEventually();
    unsentRequests = request.getUnsentRequests(1.0);
    unsentRequestsData =
        (List) removeIrrelevantBackgroundStartRequests.invoke(RequestOld.class, unsentRequests);
    assertNotNull(unsentRequestsData);
    assertEquals(3, unsentRequestsData.size());
    RequestOld.deleteSentRequests(unsentRequests.size());
    LeanplumEventDataManagerTest.setDatabaseToNull();
  }

  // Given a list of 5000 that generate OOM exceptions
  // we want to generate the requests to send
  // The list should try and get a smaller fraction of the available requests
  @Test
  public void testJsonEncodeUnsentRequestsWithExceptionLargeNumbers() throws NoSuchMethodException,
          InvocationTargetException, IllegalAccessException {
    LeanplumEventDataManager.init(Leanplum.getContext());
    RequestOld.RequestsWithEncoding requestsWithEncoding;
    // Prepare testable objects and method.
    RequestOld request = spy(new RequestOld("POST", Constants.Methods.START, null));
    request.sendEventually(); // first request added
    for (int i = 0;i < 4999; i++) { // remaining requests to make up 5000
      new RequestOld("POST", Constants.Methods.START, null).sendEventually();
    }
    // Expectation: 5000 requests returned.
    requestsWithEncoding = request.getRequestsWithEncodedStringStoredRequests(1.0);

    assertNotNull(requestsWithEncoding.unsentRequests);
    assertNotNull(requestsWithEncoding.requestsToSend);
    assertNotNull(requestsWithEncoding.jsonEncodedString);
    assertEquals(5000, requestsWithEncoding.unsentRequests.size());

    // Throw OOM on 5000 requests
    // Expectation: 2500 requests returned.
    when(request.getUnsentRequests(1.0)).thenThrow(OutOfMemoryError.class);
    requestsWithEncoding = request.getRequestsWithEncodedStringStoredRequests(1.0);

    assertNotNull(requestsWithEncoding.unsentRequests);
    assertNotNull(requestsWithEncoding.requestsToSend);
    assertNotNull(requestsWithEncoding.jsonEncodedString);
    assertEquals(2500, requestsWithEncoding.unsentRequests.size());

    // Throw OOM on 2500, 5000 requests
    // Expectation: 1250 requests returned.
    when(request.getUnsentRequests(0.5)).thenThrow(OutOfMemoryError.class);
    requestsWithEncoding = request.getRequestsWithEncodedStringStoredRequests(1.0);
    assertEquals(1250, requestsWithEncoding.unsentRequests.size());

    // Throw OOM on serializing any finite number of requests (extreme condition)
    // Expectation: Determine only 0 requests to be sent
    when(request.getUnsentRequests(not(eq(0)))).thenThrow(OutOfMemoryError.class);
    requestsWithEncoding = request.getRequestsWithEncodedStringStoredRequests(1.0);

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

    RequestOld realRequest = new RequestOld("POST", Constants.Methods.START, null);
    RequestOld request = spy(realRequest);
    when(request.getUnsentRequests(1.0)).thenThrow(OutOfMemoryError.class);
    when(request.getUnsentRequests(0.5)).thenThrow(OutOfMemoryError.class);
    when(request.getUnsentRequests(0.25)).thenReturn(requests);

    RequestOld.RequestsWithEncoding requestsWithEncoding = request.getRequestsWithEncodedStringStoredRequests(1.0);

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

    RequestOld realRequest = new RequestOld("POST", Constants.Methods.START, null);
    RequestOld request = spy(realRequest);
    when(request.getUnsentRequests(1.0)).thenReturn(requests);

    RequestOld.RequestsWithEncoding requestsWithEncoding = request.getRequestsWithEncodedStringStoredRequests(1.0);

    assertEquals(4, requestsWithEncoding .unsentRequests.size());
    assertEquals(4, requestsWithEncoding .requestsToSend.size());
    final String expectedJson =  "{\"data\":[{\"0\":\"testData\"},{\"1\":\"testData\"},{\"2\":\"testData\"},{\"3\":\"testData\"}]}";
    assertEquals(expectedJson, requestsWithEncoding.jsonEncodedString);
  }

  // Given a list of requests
  // we want to encode to a JSON String
  // The String should have the expected format
  @Test
  public void testGetRequestsWithEncodedStringStoredRequests() {
    List<Map<String, Object>> requests = mockRequests(4);
    String json = RequestOld.jsonEncodeRequests(requests);

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

  private static class ThreadRequestSequenceRecorder implements RequestSequenceRecorder {
    Instant beforeReadTime, afterReadTime, beforeWriteTime, afterWriteTime;
    final Semaphore writeSemaphore = new Semaphore(0);
    final Semaphore readSemaphore = new Semaphore(1);
    final Semaphore testThreadSemaphore = new Semaphore(0);

    @Override
    public void beforeRead() {
      try {
        readSemaphore.tryAcquire(10, TimeUnit.SECONDS);
        beforeReadTime = Instant.now();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } finally {
        readSemaphore.release();
      }
    }

    @Override
    public void afterRead() {
      afterReadTime = Instant.now();
      testThreadSemaphore.release(1);
    }

    @Override
    public void beforeWrite() {
      // since we are blocking on main thread
      try {
        writeSemaphore.tryAcquire(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } finally {
        writeSemaphore.release();
      }
      beforeWriteTime = Instant.now();
    }

    @Override
    public void afterWrite() {
      afterWriteTime = Instant.now();
    }

    void assertCallSequence() {
      assertTrue(
          beforeWriteTime.isBefore(afterWriteTime)
              && beforeReadTime.isBefore(afterReadTime)
              && beforeReadTime.isAfter(afterWriteTime));
    }
  }
}
