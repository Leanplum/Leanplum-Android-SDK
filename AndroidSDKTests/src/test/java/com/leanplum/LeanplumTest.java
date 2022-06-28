/*
 * Copyright 2021, Leanplum, Inc. All rights reserved.
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

import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import com.leanplum.__setup.AbstractTest;
import com.leanplum.__setup.TestClassUtil;
import com.leanplum._whitebox.utilities.RequestHelper;
import com.leanplum._whitebox.utilities.RequestHelper.RequestHandler;
import com.leanplum._whitebox.utilities.ResponseHelper;
import com.leanplum._whitebox.utilities.VariablesTestClass;
import com.leanplum.annotations.Parser;
import com.leanplum.callbacks.MessageDisplayedCallback;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.callbacks.VariablesChangedCallback;
import com.leanplum.internal.APIConfig;
import com.leanplum.internal.ApiConfigLoader;
import com.leanplum.internal.CollectionUtil;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Constants.Params;
import com.leanplum.internal.FeatureFlagManager;
import com.leanplum.internal.FileManager;
import com.leanplum.internal.JsonConverter;
import com.leanplum.internal.LeanplumEventDataManager;
import com.leanplum.internal.LeanplumEventDataManagerTest;
import com.leanplum.internal.Log;
import com.leanplum.internal.Request.RequestType;
import com.leanplum.internal.RequestBatchFactory;
import com.leanplum.internal.RequestBuilder;
import com.leanplum.internal.Request;
import com.leanplum.internal.RequestSender;
import com.leanplum.internal.Util;
import com.leanplum.internal.VarCache;
import com.leanplum.internal.http.LeanplumHttpConnection;
import com.leanplum.internal.http.NetworkOperation;
import com.leanplum.models.GeofenceEventType;
import com.leanplum.models.MessageArchiveData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Method;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import java.util.Set;

/**
 * Tests Leanplum SDK calls and general functionality.
 *
 * @author Kiril Kafadarov, Aleksandar Gyorev, Ben Marten
 */
public class LeanplumTest extends AbstractTest {
  @Test
  public void testStart() {
    ResponseHelper.seedResponse("/responses/simple_start_response.json");

    // Expected request params.
    final HashMap<String, Object> expectedRequestParams = CollectionUtil.newHashMap(
            "city", "(detect)",
            "country", "(detect)",
            "location", "(detect)",
            "region", "(detect)",
            "locale", "en_US"
    );

    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_START, apiMethod);

        // Validate request.
        assertTrue(params.keySet().containsAll(expectedRequestParams.keySet()));
        assertTrue(params.values().containsAll(expectedRequestParams.values()));
      }
    });
    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());
  }

  @Test
  public void testStartWithCallback() throws Exception {
    final Semaphore semaphore = new Semaphore(1);
    semaphore.acquire();

    // Seed response from the file.
    ResponseHelper.seedResponse("/responses/simple_start_response.json");

    // Expected request params.
    final HashMap<String, Object> expectedRequestParams = CollectionUtil.newHashMap(
            "city", "(detect)",
            "country", "(detect)",
            "location", "(detect)",
            "region", "(detect)",
            "locale", "en_US"
    );

    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_START, apiMethod);
        assertTrue(params.keySet().containsAll(expectedRequestParams.keySet()));
        assertTrue(params.values().containsAll(expectedRequestParams.values()));
      }
    });

    Leanplum.start(mContext, new StartCallback() {
      @Override
      public void onResponse(boolean success) {
        assertTrue(success);
        semaphore.release();
      }
    });
    assertTrue(Leanplum.hasStarted());
  }

  /**
   * Test to verify that callbacks are invoked when Exception is thrown after https request.
   */
  @Test
  public void testStartFailWithException() throws Exception {
    // Seed response to throw exception.
    ResponseHelper.seedJsonResponseException();

    // Expected request params.
    final HashMap<String, Object> expectedRequestParams = CollectionUtil.newHashMap(
        "city", "(detect)",
        "country", "(detect)",
        "location", "(detect)",
        "region", "(detect)",
        "locale", "en_US"
    );

    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_START, apiMethod);
        assertTrue(params.keySet().containsAll(expectedRequestParams.keySet()));
        assertTrue(params.values().containsAll(expectedRequestParams.values()));
      }
    });

    class TestStartCallback extends StartCallback {
      private boolean onResponseCalled;
      private boolean startSuccessfull;
      @Override
      public void onResponse(boolean success) {
        onResponseCalled = true;
        startSuccessfull = success;
      }
    }

    TestStartCallback callback = new TestStartCallback();
    Leanplum.start(mContext, callback);

    assertTrue(Leanplum.hasStarted());
    assertTrue(callback.onResponseCalled);
    assertFalse(callback.startSuccessfull);
  }

  /**
   * Test to verify that callbacks are invoked when responseCode is 500 after https request.
   */
  @Test
  public void testStartFailWithInternalServerError() throws Exception {
    prepareHttpsURLConnection(500);

    ResponseHelper.seedJsonResponseNull();

    // Expected request params.
    final HashMap<String, Object> expectedRequestParams = CollectionUtil.newHashMap(
        "city", "(detect)",
        "country", "(detect)",
        "location", "(detect)",
        "region", "(detect)",
        "locale", "en_US"
    );

    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_START, apiMethod);
        assertTrue(params.keySet().containsAll(expectedRequestParams.keySet()));
        assertTrue(params.values().containsAll(expectedRequestParams.values()));
      }
    });

    class TestStartCallback extends StartCallback {
      private boolean onResponseCalled;
      private boolean startSuccessfull;
      @Override
      public void onResponse(boolean success) {
        onResponseCalled = true;
        startSuccessfull = success;
      }
    }

    TestStartCallback callback = new TestStartCallback();
    Leanplum.start(mContext, callback);

    assertTrue(Leanplum.hasStarted());
    assertTrue(callback.onResponseCalled);
    assertFalse(callback.startSuccessfull);
  }

  @Test
  public void testStartWithUserAttributes() {
    ResponseHelper.seedResponse("/responses/simple_start_response.json");

    // Expected request params.
    final HashMap<String, Object> expectedRequestParams = CollectionUtil.newHashMap(
            "city", "(detect)",
            "country", "(detect)",
            "location", "(detect)",
            "region", "(detect)",
            "locale", "en_US"
    );

    // Setup user attributes.
    final HashMap<String, Object> userAttributes = CollectionUtil.newHashMap(
            "name", "John Smith",
            "age", 42,
            "address", "New York"
    );

    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_START, apiMethod);

        assertTrue(params.keySet().containsAll(expectedRequestParams.keySet()));
        assertTrue(params.values().containsAll(expectedRequestParams.values()));

        String requestUserAttributes = (String) params.get("userAttributes");

        assertTrue(userAttributes.keySet()
                .containsAll(JsonConverter.fromJson(requestUserAttributes).keySet()));
        assertTrue(userAttributes.values()
                .containsAll(JsonConverter.fromJson(requestUserAttributes).values()));
      }
    });

    Leanplum.start(mContext, userAttributes);
    assertTrue(Leanplum.hasStarted());
  }

  @Test
  public void testStartWithUserId() {
    ResponseHelper.seedResponse("/responses/simple_start_response.json");

    // Expected request params.
    final HashMap<String, Object> expectedRequestParams = CollectionUtil.newHashMap(
            "city", "(detect)",
            "country", "(detect)",
            "location", "(detect)",
            "region", "(detect)",
            "locale", "en_US"
    );

    String userId = "user_id";

    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_START, apiMethod);

        assertTrue(params.keySet().containsAll(expectedRequestParams.keySet()));
        assertTrue(params.values().containsAll(expectedRequestParams.values()));
      }
    });

    Leanplum.start(mContext, userId);
    assertTrue(Leanplum.hasStarted());
    assertTrue(APIConfig.getInstance().userId().equals(userId));
  }

  @Test
  public void testStartWithUserIdAndCallback() {
    // Seed response from the file.
    ResponseHelper.seedResponse("/responses/simple_start_response.json");

    // Expected request params.
    final HashMap<String, Object> expectedRequestParams = CollectionUtil.newHashMap(
            "city", "(detect)",
            "country", "(detect)",
            "location", "(detect)",
            "region", "(detect)",
            "locale", "en_US"
    );

    String userId = "user_id";

    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_START, apiMethod);

        assertTrue(params.keySet().containsAll(expectedRequestParams.keySet()));
        assertTrue(params.values().containsAll(expectedRequestParams.values()));
      }
    });

    Leanplum.start(mContext, userId, new StartCallback() {
      @Override
      public void onResponse(boolean success) {
        assertTrue(success);
      }
    });
    assertTrue(Leanplum.hasStarted());
    assertTrue(APIConfig.getInstance().userId().equals(userId));
  }

  @Test
  public void testStartWithUserIdAndAttributes() {
    ResponseHelper.seedResponse("/responses/simple_start_response.json");

    // Expected request params.
    final HashMap<String, Object> expectedRequestParams = CollectionUtil.newHashMap(
            "city", "(detect)",
            "country", "(detect)",
            "location", "(detect)",
            "region", "(detect)",
            "locale", "en_US"
    );

    // Setup user attributes.
    final HashMap<String, Object> userAttributes = CollectionUtil.newHashMap(
            "name", "John Smith",
            "age", 42,
            "address", "New York"
    );

    String userId = "user_id";

    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_START, apiMethod);

        assertTrue(params.keySet().containsAll(expectedRequestParams.keySet()));
        assertTrue(params.values().containsAll(expectedRequestParams.values()));

        String requestUserAttributes = (String) params.get("userAttributes");

        assertTrue(userAttributes.keySet()
                .containsAll(JsonConverter.fromJson(requestUserAttributes).keySet()));
        assertTrue(userAttributes.values()
                .containsAll(JsonConverter.fromJson(requestUserAttributes).values()));
      }
    });

    Leanplum.start(mContext, userId, userAttributes);
    assertTrue(Leanplum.hasStarted());
    assertTrue(APIConfig.getInstance().userId().equals(userId));
  }

  @Test
  public void testStartWithUserIdAttributesAndCallback() throws Exception {
    ResponseHelper.seedResponse("/responses/simple_start_response.json");

    // Expected request params.
    final HashMap<String, Object> expectedRequestParams = CollectionUtil.newHashMap(
            "city", "(detect)",
            "country", "(detect)",
            "location", "(detect)",
            "region", "(detect)",
            "locale", "en_US"
    );

    // Setup user attributes.
    final HashMap<String, Object> userAttributes = CollectionUtil.newHashMap(
            "name", "John Smith",
            "age", 42,
            "address", "New York"
    );

    String userId = "user_id";

    final CountDownLatch countDownLatch = new CountDownLatch(1);

    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_START, apiMethod);

        assertTrue(params.keySet().containsAll(expectedRequestParams.keySet()));
        assertTrue(params.values().containsAll(expectedRequestParams.values()));

        String requestUserAttributes = (String) params.get("userAttributes");

        assertTrue(userAttributes.keySet()
                .containsAll(JsonConverter.fromJson(requestUserAttributes).keySet()));
        assertTrue(userAttributes.values()
                .containsAll(JsonConverter.fromJson(requestUserAttributes).values()));
      }
    });

    Leanplum.start(RuntimeEnvironment.application, userId, userAttributes, new StartCallback() {
      @Override
      public void onResponse(boolean success) {
        assertTrue(success);
        countDownLatch.countDown();
      }
    });
    countDownLatch.await(10, TimeUnit.SECONDS);
    assertTrue(Leanplum.hasStarted());
    assertTrue(APIConfig.getInstance().userId().equals(userId));
  }

  @Test
  public void shouldStartWithParamToIncludeVariantDebugInfo() {
    ResponseHelper.seedResponse("/responses/simple_start_response.json");

    Leanplum.setVariantDebugInfoEnabled(true);
    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        // Expected request params.
        final HashMap<String, Object> expectedRequestParams = CollectionUtil.newHashMap(
                "city", "(detect)",
                "country", "(detect)",
                "location", "(detect)",
                "region", "(detect)",
                "locale", "en_US",
                "includeVariantDebugInfo", true
        );

        assertEquals(RequestBuilder.ACTION_START, apiMethod);

        // Validate request.
        assertTrue(params.keySet().containsAll(expectedRequestParams.keySet()));
        assertTrue(params.values().containsAll(expectedRequestParams.values()));
      }
    });
    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());
  }


  /**
   * Tests Metadata.
   */
  @Test
  public void testMetadata() {
    Leanplum.setApplicationContext(mContext);
    List<Map<String, Object>> variants = new ArrayList<Map<String, Object>>() {{
      add(new HashMap<String, Object>() {{
        put("id", "1");
      }});
      add(new HashMap<String, Object>() {{
        put("id", "2");
      }});
    }};

    Map<String, Object> messages = new HashMap<String, Object>() {{
      put("123", new HashMap<String, Object>() {{
        put("action", new HashMap<>());
        put("vars", new HashMap<>());
        put("whenLimits", new HashMap<>());
        put("whenTriggers", new HashMap<>());
      }});
    }};

    VarCache.applyVariableDiffs(null, messages, null, variants, null, null, null, null);
    assertEquals(variants, Leanplum.variants());
    assertEquals(messages, Leanplum.messageMetadata());
  }

  /**
   * Note that test fails on debug flavor and runs ok on release flavor.
   */
  @Test
  public void testCrashes() throws Exception {
    // Setup sdk first.
    setupSDK(mContext, "/responses/start_variables_response.json");

    RequestSender.setInstance(new RequestSender()); // override the immediate sender from @before method

    Context currentCotext = Leanplum.getContext();
    assertNotNull(currentCotext);
    LeanplumEventDataManager.sharedInstance();

    // Add two events to database.
    Request request1 =
        new Request("POST", RequestBuilder.ACTION_GET_INBOX_MESSAGES, RequestType.DEFAULT, null);
    Request request2 = new Request("POST", RequestBuilder.ACTION_LOG, RequestType.DEFAULT, null);

    RequestSender.getInstance().send(request1);
    RequestSender.getInstance().send(request2);

    final double fraction = 1.0;
    // Get a number of events in the database.
    // Expectation: 2 events.
    List unsentRequests = new RequestBatchFactory().getUnsentRequests(fraction);
    assertNotNull(unsentRequests);
    assertEquals(2, unsentRequests.size());

    // Verify Log.exception method is never called.
    verifyStatic(never());
    Log.exception(any(Throwable.class));

    doNothing().when(Log.class, "exception", any(Throwable.class));

    // Crash SDK.
    ActionContext actionContext = new ActionContext("name", new HashMap<String, Object>() {{
      put("1", "aaa");
    }}, "messageId");
    actionContext.numberNamed("1");

    // Verify Log.exception method is called 1 time.
    verifyStatic(times(1));
    Log.exception(any(Throwable.class));

    // Get a number of events in the database. Checks if ours two events still here.
    // Expectation: 2 events.
    unsentRequests = new RequestBatchFactory().getUnsentRequests(fraction);
    assertNotNull(unsentRequests);
    assertEquals(2, unsentRequests.size());

    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_PAUSE_STATE, apiMethod);
      }
    });

    // Call pause method.
    Leanplum.pauseState();
    RequestSender.getInstance().sendRequests();

    // Get a number of events in the database. Make sure we sent all events.
    // Expectation: 0 events.
    unsentRequests = new RequestBatchFactory().getUnsentRequests(fraction);
    assertNotNull(unsentRequests);
    assertEquals(0, unsentRequests.size());

    LeanplumEventDataManagerTest.setDatabaseToNull();
  }

  @Test
  public void testTrack() throws Exception {
    // Setup sdk first.
    setupSDK(mContext, "/responses/simple_start_response.json");

    // Setup event values.
    final String eventName = "test_event_name";
    final double eventValue = 10.0;
    final String eventInfo = "test_event_info";
    final HashMap<String, Object> eventParams = CollectionUtil.newHashMap(
            "test_param_string", "string",
            "test_param_int", 42
    );

    // Validate request for track with event name.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);

        String requestEventName = (String) params.get("event");

        assertEquals(eventName, requestEventName);
      }
    });
    Leanplum.track(eventName);

    // Validate request for track with event name and value.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);

        String requestEventName = (String) params.get("event");
        String requestEventValue = (String) params.get("value");

        assertEquals(eventName, requestEventName);
        assertEquals(String.valueOf(eventValue), requestEventValue);
      }
    });
    Leanplum.track(eventName, eventValue);

    // Validate request for track with event name, value and info.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);

        String requestEventName = (String) params.get("event");
        String requestEventInfo = (String) params.get("info");
        String requestEventValue = (String) params.get("value");

        assertEquals(eventName, requestEventName);
        assertEquals(eventInfo, requestEventInfo);
        assertEquals(String.valueOf(eventValue), requestEventValue);
      }
    });
    Leanplum.track(eventName, eventValue, eventInfo);

    // Validate request for track with event name, value and params.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);

        String requestEventName = (String) params.get("event");
        String requestEventValue = (String) params.get("value");
        String requestEventParams = (String) params.get("params");

        assertEquals(eventName, requestEventName);
        assertEquals(String.valueOf(eventValue), requestEventValue);
        assertTrue(eventParams.keySet()
                .containsAll(JsonConverter.fromJson(requestEventParams).keySet()));
        assertTrue(eventParams.values()
                .containsAll(JsonConverter.fromJson(requestEventParams).values()));
      }
    });
    Leanplum.track(eventName, eventValue, eventParams);

    // Validate request for track with event name and params.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);

        String requestEventName = (String) params.get("event");
        String requestEventParams = (String) params.get("params");

        assertEquals(eventName, requestEventName);
        assertTrue(eventParams.keySet()
                .containsAll(JsonConverter.fromJson(requestEventParams).keySet()));
        assertTrue(eventParams.values()
                .containsAll(JsonConverter.fromJson(requestEventParams).values()));
      }
    });
    Leanplum.track(eventName, eventParams);

    // validate request for track with event name and info
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);

        String requestEventName = (String) params.get("event");
        String requestEventInfo = (String) params.get("info");

        assertEquals(eventName, requestEventName);
        assertEquals(eventInfo, requestEventInfo);
      }
    });
    Leanplum.track(eventName, eventInfo);

    // Validate request for track with event name, value, info and params.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);

        String requestEventName = (String) params.get("event");
        String requestEventValue = (String) params.get("value");
        String requestEventInfo = (String) params.get("info");
        String requestEventParams = (String) params.get("params");

        assertEquals(eventName, requestEventName);
        assertEquals(String.valueOf(eventValue), requestEventValue);
        assertEquals(eventInfo, requestEventInfo);
        assertTrue(eventParams.keySet()
                .containsAll(JsonConverter.fromJson(requestEventParams).keySet()));
        assertTrue(eventParams.values()
                .containsAll(JsonConverter.fromJson(requestEventParams).values()));
      }
    });
    Leanplum.track(eventName, eventValue, eventInfo, eventParams);

    // Validate request for purchae.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);

        String requestEventName = (String) params.get("event");
        String requestPurchaseData = (String) params.get("googlePlayPurchaseData");
        String requestCurrencyCode = (String) params.get("currencyCode");
        String requestDataSignature = (String) params.get("googlePlayPurchaseDataSignature");

        assertEquals("Purchase", requestEventName);
        assertEquals("USD", requestCurrencyCode);
        assertEquals("signature", requestDataSignature);
        assertEquals("data", requestPurchaseData);
      }
    });
    Leanplum.trackGooglePlayPurchase(eventName, 10, "USD", "data", "signature");

    // Validate request for purchase.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);

        String requestEventName = (String) params.get("event");
        String requestPurchaseData = (String) params.get("googlePlayPurchaseData");
        String requestCurrencyCode = (String) params.get("currencyCode");
        String requestDataSignature = (String) params.get("googlePlayPurchaseDataSignature");

        assertEquals("Purchase", requestEventName);
        assertEquals("USD", requestCurrencyCode);
        assertEquals("signature", requestDataSignature);
        assertEquals("data", requestPurchaseData);
      }
    });
    Leanplum.trackGooglePlayPurchase(eventName, 10, "USD", "data", "signature", new HashMap<String, Object>());

    // Validate request for manual purchase.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);

        String requestEventName = (String) params.get("event");
        String requestEventValue = (String) params.get("value");
        String requestCurrencyCode = (String) params.get("currencyCode");

        assertEquals(eventName, requestEventName);
        assertEquals("1.99", requestEventValue);
        assertEquals("USD", requestCurrencyCode);
      }
    });
    Leanplum.trackPurchase(eventName, 1.99, "USD", new HashMap<String, Objects>());

    // Validate request for track geofence with event name and info
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_TRACK_GEOFENCE, apiMethod);

        String requestEventName = (String) params.get("event");
        String requestEventInfo = (String) params.get("info");

        assertEquals(GeofenceEventType.ENTER_REGION.getName(), requestEventName);
        assertEquals(String.valueOf(eventInfo), requestEventInfo);
      }
    });
    Leanplum.trackGeofence(GeofenceEventType.ENTER_REGION, eventInfo);
  }

  @Test
  public void testTrackEventsSamePriority() {

    // Setup sdk first.
    setupSDK(mContext, "/responses/simple_start_response.json");

    // Setup event values.
    final String eventName = "pushLocal";
    // Validate request for track with event name.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);
        String requestEventName = (String) params.get("event");
        assertEquals(eventName, requestEventName);
      }
    });

    // Validate response for event with same priority and countdown
    ResponseHelper.seedResponse("/responses/simple_start_response.json");

    Leanplum.start(mContext, new StartCallback() {
      @Override
      public void onResponse(boolean success) {
        //App successfully starts with local notifications with same priority and countdown
        assertTrue(success);
      }
    });

    assertTrue(Leanplum.hasStarted());
    Leanplum.track(eventName);

  }

  @Test
  public void testAdvance() throws Exception {
    setupSDK(RuntimeEnvironment.application, "/responses/simple_start_response.json");

    // Setup state values.
    final String stateName = "test_state_name";
    final String stateInfo = "test_state_info";
    final HashMap<String, Object> stateParams = CollectionUtil.newHashMap(
            "test_param_string", "string",
            "test_param_int", 42
    );

    // Validate request for advance with name.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_ADVANCE, apiMethod);

        String requestStateName = (String) params.get("state");

        assertEquals(stateName, requestStateName);
      }
    });
    Leanplum.advanceTo(stateName);

    // Validate request for advance with name and info.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_ADVANCE, apiMethod);

        String requestStateName = (String) params.get("state");
        String requestStateInfo = (String) params.get("info");

        assertEquals(stateName, requestStateName);
        assertEquals(stateInfo, requestStateInfo);
      }
    });
    Leanplum.advanceTo(stateName, stateInfo);

    // Validate request for advance with params.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_ADVANCE, apiMethod);

        String requestStateName = (String) params.get("state");
        String requestStateParams = (String) params.get("params");

        assertEquals(stateName, requestStateName);
        assertTrue(stateParams.keySet()
                .containsAll(JsonConverter.fromJson(requestStateParams).keySet()));
        assertTrue(stateParams.values()
                .containsAll(JsonConverter.fromJson(requestStateParams).values()));
      }
    });

    Leanplum.advanceTo(stateName, stateParams);

    // Validate request for advance with name, info and params.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_ADVANCE, apiMethod);

        String requestStateName = (String) params.get("state");
        String requestStateInfo = (String) params.get("info");
        String requestStateParams = (String) params.get("params");

        assertEquals(stateName, requestStateName);
        assertEquals(stateInfo, requestStateInfo);
        assertTrue(stateParams.keySet()
                .containsAll(JsonConverter.fromJson(requestStateParams).keySet()));
        assertTrue(stateParams.values()
                .containsAll(JsonConverter.fromJson(requestStateParams).values()));
      }
    });
    Leanplum.advanceTo(stateName, stateInfo, stateParams);
  }

  @Test
  public void testVariables() throws Exception {
    Var<String> stringVariable = Var.define("test_string", "string");
    Var<Double> doubleVariable = Var.define("test_double", 10.0);
    Var<Integer> integerVariable = Var.define("test_integer", 10);
    Var<Float> floatVariable = Var.define("test_float", 10.0f);
    Var<List<Integer>> listVariable = Var.define("test_list", Arrays.asList(0, 1, 2, 3, 4, 5));
    Var<HashMap<Object, Object>> dictionaryVariable = Var.define("test_dictionary",
            CollectionUtil.newHashMap(
                    "dictionary_test_string", "test_string",
                    "dictionary_test_integer", 5
            ));
    Var<Integer> colorVariable = Var.defineColor("test_color", 12345);
    Var<String> groupStringVariable = Var.define("groups.strings", "groups_string_test");
    Var<Integer> groupIntegerVariable = Var.define("groups.integers", 5);
    Var<Integer> integerVariableString = Var.define("test_integer_string_invalid", 10);

    setupSDK(mContext, "/responses/simple_start_response.json");

    // Validate names.
    assertEquals(stringVariable.name(), "test_string");
    assertEquals(groupStringVariable.name(), "groups.strings");

    // Validate default values.
    assertEquals(stringVariable.defaultValue(), VarCache.getVariable("test_string").value());
    assertEquals(doubleVariable.defaultValue(), VarCache.getVariable("test_double").value());
    assertEquals(integerVariable.defaultValue(), VarCache.getVariable("test_integer").value());
    assertEquals(floatVariable.defaultValue(), VarCache.getVariable("test_float").value());
    assertEquals(listVariable.defaultValue(), VarCache.getVariable("test_list").value());
    assertEquals(dictionaryVariable.defaultValue(), VarCache.getVariable("test_dictionary")
            .value());
    assertEquals(integerVariable.defaultValue(), VarCache.getVariable("test_integer_string_invalid").value());

    // Validate values.
    assertEquals(colorVariable.value(), VarCache.getVariable("test_color").value());
    assertEquals(groupStringVariable.value(), VarCache.getVariable("groups.strings").value());
    assertEquals(groupIntegerVariable.value(), VarCache.getVariable("groups.integers").value());

    // Validate groups.
    assertEquals(groupStringVariable.value(), Leanplum.objectForKeyPath("groups", "strings"));
    assertEquals(groupIntegerVariable.value(), Leanplum.objectForKeyPath("groups", "integers"));

    // Validate kinds.
    assertEquals(stringVariable.kind(), "string");
    assertEquals(groupIntegerVariable.kind(), "integer");
    assertEquals(listVariable.kind(), "list");

    // Validate components.
    assertArrayEquals(groupStringVariable.nameComponents(), new String[]{"groups", "strings"});
    assertArrayEquals(groupIntegerVariable.nameComponents(), new String[]{"groups", "integers"});
  }

  @Test
  public void shouldGetResponseAndReturnVariantDebugInfo() throws Exception {
    setupSDK(mContext, "/responses/start_with_variant_debug_info_response.json");
    Map<String, ?> v = Leanplum.getVariantDebugInfo();
    assertEquals(Leanplum.getVariantDebugInfo().size(), 1);
    assertNotNull(Leanplum.getVariantDebugInfo().get("abTests"));
  }

  @Test
  public void testVariableParser() throws Exception {
    // Parse test class.
    Parser.parseVariablesForClasses(VariablesTestClass.class);
    // Setup sdk.
    setupSDK(mContext, "/responses/simple_start_response.json");

    // Fetch all parsed variables.
    String stringVariable = VarCache.getVariable("stringVariable").stringValue();
    boolean booleanVariable = (boolean) VarCache.getVariable("Boolean Variable").value();
    float floatVariable = (float) VarCache.getVariable("numbers.floatVariable").value();
    double doubleVariable = (double) VarCache.getVariable("VariablesTestClass.doubleVariable")
            .value();
    List<?> listVariable = (List<?>) VarCache.getVariable("listVariable").value();
    HashMap<?, ?> dictionaryVariable = (HashMap<?, ?>) VarCache.getVariable("dictionaryVariable")
            .value();

    // Check if parsing is correct.
    assertEquals(VariablesTestClass.stringVariable, stringVariable);
    assertEquals(VariablesTestClass.booleanVariable, booleanVariable);
    assertEquals(VariablesTestClass.floatVariable, floatVariable, 0.1f);
    assertEquals(VariablesTestClass.doubleVariable, doubleVariable, 0.1);
    assertEquals(VariablesTestClass.listVariable, listVariable);
    assertEquals(VariablesTestClass.dictionaryVariable, dictionaryVariable);
  }

  @Test
  public void testStates() throws Exception {
    setupSDK(mContext, "/responses/simple_start_response.json");

    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_PAUSE_STATE, apiMethod);
      }
    });
    Leanplum.pauseState();

    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_RESUME_STATE, apiMethod);
      }
    });
    Leanplum.resumeState();
  }

  @Test
  public void testActions() throws Exception {
    setupSDK(mContext, "/responses/simple_start_response.json");

    final String actionName = "test_action";

    final String integerArgumentName = "number_argument";
    final String stringArgumentName = "string_argument";
    final String boolArgumentName = "bool_argument";
    final String fileArgumentName = "file_argument";
    final String dictionaryArgumentName = "dictionary_argument";
    final String arrayArgumentName = "array_argument";
    final String actionArgumentName = "action_argument";
    final String colorArgumentName = "color_argument";

    ActionArgs args = new ActionArgs();

    args.with(integerArgumentName, 5);
    args.with(stringArgumentName, "test_string");
    args.with(boolArgumentName, true);
    args.withAsset(fileArgumentName, "leanplum_watermark.jpg");
    args.with(dictionaryArgumentName, CollectionUtil.newHashMap(
            "test_value", "test"
    ));
    args.with(arrayArgumentName, CollectionUtil.newArrayList(1, 2, 3, 4));
    args.withAction(actionArgumentName, "action_test");
    args.withColor(colorArgumentName, Color.BLUE);

    ResponseHelper.seedResponse("/responses/action_response.json");

    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_SET_VARS, apiMethod);

        String actionDefinitionsJson = (String) params.get("actionDefinitions");
        Map<String, Object> actionDefinitions = JsonConverter.fromJson(actionDefinitionsJson);

        Map<?, ?> definedActions = (HashMap<?, ?>) actionDefinitions.get(actionName);

        Map<String, Object> kinds = CollectionUtil.newHashMap(
                "number_argument", "integer",
                "string_argument", "string",
                "color_argument", "color",
                "dictionary_argument", "group",
                "array_argument", "list",
                "action_argument", "action",
                "bool_argument", "bool"
        );
        Map<?, ?> requestedKinds = (HashMap<?, ?>) definedActions.get("kinds");


        assertEquals(2, definedActions.get("kind"));
        assertTrue(requestedKinds.keySet().containsAll(kinds.keySet()));
        assertTrue(requestedKinds.values().containsAll(kinds.values()));

        Map<?, ?> values = (HashMap<?, ?>) definedActions.get("values");

        assertEquals(5, values.get(integerArgumentName));
        assertEquals("test_string", values.get(stringArgumentName));
        assertEquals(true, values.get(boolArgumentName));
        assertNotNull(values.get(dictionaryArgumentName));
        assertNotNull(values.get(arrayArgumentName));
        assertEquals("action_test", values.get(actionArgumentName));
        assertEquals(Color.BLUE, values.get(colorArgumentName));
        assertEquals("leanplum_watermark.jpg", values.get(fileArgumentName));
      }
    });
    // Define action.
    Leanplum.defineAction(actionName, Leanplum.ACTION_KIND_ACTION, args);

    // Force action update.
    Method actionUpdateMethod = VarCache.class.getDeclaredMethod("sendActionsIfChanged");
    actionUpdateMethod.setAccessible(true);
    actionUpdateMethod.invoke(null);
  }

  @Test
  public void testVariablesCallbacks() throws Exception {
    // Test if callback is called when app is online.
    final CountDownLatch countDownLatch = new CountDownLatch(2);
    Leanplum.addVariablesChangedAndNoDownloadsPendingHandler(new VariablesChangedCallback() {
      @Override
      public void variablesChanged() {
        countDownLatch.countDown();
      }
    });

    Leanplum.addVariablesChangedHandler(new VariablesChangedCallback() {
      @Override
      public void variablesChanged() {
        countDownLatch.countDown();
      }
    });

    // Start and wait.
    setupSDK(mContext, "/responses/simple_start_response.json");
    countDownLatch.await();
  }

  /**
   * Test push notification registration for FCM.
   */
  @Test
  public void testPushNotificationRegistrationFcm() {
    setupSDK(mContext, "/responses/simple_start_response.json");
    String regId = "regId";

    // Verify request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_SET_DEVICE_ATTRIBUTES, apiMethod);
        assertNotNull(params);
        assertEquals(regId, params.get(Params.DEVICE_FCM_PUSH_TOKEN));
      }
    });
    // Register for push notification.
    Leanplum.setRegistrationId(PushProviderType.FCM, regId);
  }

  /**
   * Test push notification registration for MiPush.
   */
  @Test
  public void testPushNotificationRegistrationMiPush() {
    setupSDK(mContext, "/responses/simple_start_response.json");
    String regId = "regId";

    // Verify request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_SET_DEVICE_ATTRIBUTES, apiMethod);
        assertNotNull(params);
        assertEquals(regId, params.get(Params.DEVICE_MIPUSH_TOKEN));
      }
    });
    // Register for push notification.
    Leanplum.setRegistrationId(PushProviderType.MIPUSH, regId);
  }

  /**
   * Test location provider
   */
  @Test
  public void testLocationProvider() throws Exception {
    // Start leanplum first.
    setupSDK(mContext, "/responses/simple_start_response.json");

    // Test disable location collection.
    assertTrue(Leanplum.isLocationCollectionEnabled());
    Leanplum.disableLocationCollection();
    assertFalse(Leanplum.isLocationCollectionEnabled());

    // Test Location.
    Location location = new Location("test_location");
    location.setLatitude(37.324708);
    location.setLongitude(-122.020799);

    // Mock reverse geocoding.
    Address address = new Address(Locale.US);
    address.setAdminArea("California");
    address.setLocality("San Francisco");
    address.setCountryCode("US");
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);
    Geocoder geocoder = Mockito.mock(Geocoder.class);
    whenNew(Geocoder.class).withAnyArguments().thenReturn(geocoder);
    when(geocoder.getFromLocation(anyDouble(), anyDouble(), anyInt()))
            .thenReturn(addresses);

    // Validate set location request shorthand.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_SET_USER_ATTRIBUTES, apiMethod);
        assertEquals("37.324708,-122.020799", params.get("location"));
        assertEquals("cell", params.get("locationAccuracyType"));
      }
    });
    Leanplum.setDeviceLocation(location);

    // Validate set location request shorthand with region info.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(apiMethod, RequestBuilder.ACTION_SET_USER_ATTRIBUTES);
        assertEquals(params.get("location"), "37.324708,-122.020799");
        assertEquals(params.get("locationAccuracyType"), "gps");
        assertEquals(params.get("city"), "San Francisco");
        assertEquals(params.get("region"), "California");
        assertEquals(params.get("country"), "US");
      }
    });
    Leanplum.setDeviceLocation(location, LeanplumLocationAccuracyType.GPS);
  }

  /**
   * Test app id and access keys
   */
  @Test
  public void testConfigurationKeys() {
    setupSDK(mContext, "/responses/simple_start_response.json");

    Leanplum.setAppIdForDevelopmentMode("appid", "accesskey");
    assertEquals("appid", APIConfig.getInstance().appId());
    assertEquals("accesskey", APIConfig.getInstance().accessKey());

    Leanplum.setAppIdForDevelopmentMode(null, null);
    assertEquals("appid", APIConfig.getInstance().appId());
    assertEquals("accesskey", APIConfig.getInstance().accessKey());

    Leanplum.setAppIdForProductionMode("appid_prod", "accesskey_prod");
    assertEquals("appid_prod", APIConfig.getInstance().appId());
    assertEquals("accesskey_prod", APIConfig.getInstance().accessKey());

    Leanplum.setAppIdForProductionMode(null, null);
    assertEquals("appid_prod", APIConfig.getInstance().appId());
    assertEquals("accesskey_prod", APIConfig.getInstance().accessKey());
  }

  /**
   * Test device id mode
   */
  @Test
  public void testDeviceIdMode() {
    setupSDK(mContext, "/responses/simple_start_response.json");

    Leanplum.setDeviceIdMode(LeanplumDeviceIdMode.ADVERTISING_ID);
    assertEquals(LeanplumDeviceIdMode.ADVERTISING_ID, TestClassUtil.getField(Leanplum.class, "deviceIdMode"));
    assertTrue((Boolean) TestClassUtil.getField(Leanplum.class, "userSpecifiedDeviceId"));

    Leanplum.setDeviceIdMode(null);
    assertEquals(LeanplumDeviceIdMode.ADVERTISING_ID, TestClassUtil.getField(Leanplum.class, "deviceIdMode"));
    assertTrue((Boolean) TestClassUtil.getField(Leanplum.class, "userSpecifiedDeviceId"));
  }

  /**
   * Test resource syncing
   */
  @Test
  public void testSyncResources() {
    setupSDK(mContext, "/responses/simple_start_response.json");

    Leanplum.syncResources();
    assertTrue((Boolean) TestClassUtil.getField(FileManager.class, "isInitialized"));

    Leanplum.syncResourcesAsync();
    assertTrue((Boolean) TestClassUtil.getField(FileManager.class, "isInitialized"));
  }

  /**
   * Test client configuration
   */
  @Test
  public void testClient() {
    Leanplum.setClient("test_client", "0.0", "default");
    assertEquals("test_client", Constants.CLIENT);
    assertEquals("0.0", Constants.LEANPLUM_VERSION);
    assertEquals("default", Constants.defaultDeviceId);
  }

  /**
   * Test user attributes
   */
  @Test
  public void testUserAttributes() {
    setupSDK(mContext, "/responses/simple_start_response.json");
    HashMap<String, Object> attributes = new HashMap<String, Object>() {{
      put("test", 1);
      put("test_1", 2);
    }};

    Leanplum.setUserAttributes("testId", attributes);
    Map<String, Object> userAttributes = CollectionUtil.uncheckedCast(TestClassUtil.getField(VarCache.class, "userAttributes"));
    assertNotNull(userAttributes);
    assertEquals(1, userAttributes.get("test"));
  }

  /**
   * Test user id
   */
  @Test
  public void testUserId() {
    setupSDK(mContext, "/responses/simple_start_response.json");

    Leanplum.setUserId("test_id");
    assertEquals("test_id", APIConfig.getInstance().userId());
  }

  /**
   * Tests setting the device ID before Leanplum start.
   */
  @Test
  public void testDeviceIdBeforeStart() {
    RequestHelper.addRequestHandler((httpMethod, apiMethod, params) -> {
      assertNotEquals(
          "Not allowed to set the device ID before start",
          "setDeviceAttributes",
          apiMethod);
    });

    String newDeviceId = "device123";
    Leanplum.forceNewDeviceId(newDeviceId);

    setupSDK(mContext, "/responses/simple_start_response.json");

    assertNotEquals(newDeviceId, APIConfig.getInstance().deviceId());
  }

  /**
   * Tests that setting the same device ID is not initiating a network request.
   */
  @Test
  public void testSameDeviceId() {
    setupSDK(mContext, "/responses/simple_start_response.json");

    RequestHelper.addRequestHandler((httpMethod, apiMethod, params) -> {
      fail("Setting the same device ID should not initiate a network request!");
    });

    String deviceId = APIConfig.getInstance().deviceId();
    Leanplum.forceNewDeviceId(deviceId);

    assertEquals(deviceId, APIConfig.getInstance().deviceId());
  }

  /**
   * Tests setting the device ID after Leanplum has started.
   */
  @Test
  public void testDeviceIdAfterStart() {
    setupSDK(mContext, "/responses/simple_start_response.json");

    RequestHelper.addRequestHandler((httpMethod, apiMethod, params) -> {
      assertEquals("setDeviceAttributes", apiMethod);
      assertNotNull(params.get("versionName"));
      assertNotNull(params.get("deviceName"));
      assertNotNull(params.get("deviceModel"));
      assertNotNull(params.get("systemName"));
      assertNotNull(params.get("systemVersion"));
    });

    String deviceId = "device123";
    Leanplum.forceNewDeviceId(deviceId);

    assertEquals(deviceId, APIConfig.getInstance().deviceId());
  }

  /**
   * Test traffic source params
   */
  @Test
  public void testTrafficSourceInfo() {
    setupSDK(mContext, "/responses/simple_start_response.json");

    final Map<String, String> traffic = new HashMap<>();
    traffic.put("publisherId", "pubid");
    traffic.put("publisherSubAd", "subad");
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_SET_TRAFFIC_SOURCE_INFO, apiMethod);
        String trafficJson = (String) params.get("trafficSource");
        Map<String, Object> trafficParams = JsonConverter.fromJson(trafficJson);
        assertEquals("pubid", trafficParams.get("publisherId"));
        assertEquals("subad", trafficParams.get("publisherSubAd"));
      }
    });
    Leanplum.setTrafficSourceInfo(traffic);
  }

  /**
   * Test force content update
   */
  @Test
  public void testForceContentUpdate() throws Exception {
    setupSDK(mContext, "/responses/simple_start_response.json");

    final CountDownLatch countDownLatch = new CountDownLatch(1);

    Leanplum.forceContentUpdate(new VariablesChangedCallback() {
      @Override
      public void variablesChanged() {
        countDownLatch.countDown();
      }
    });
    countDownLatch.await();
  }

  /**
   * Test sdk test mode
   */
  @Test
  public void testTestMode() {
    Leanplum.setIsTestModeEnabled(true);
    assertTrue(Constants.isTestMode);
    assertTrue(Constants.isNoop());
    Leanplum.setIsTestModeEnabled(false);
  }

  /**
   * Test paths for resources
   */
  @Test
  public void testPathForResource() {
    String file = Leanplum.pathForResource("mario.png");
    assertNotNull(file);

    String nullFile = Leanplum.pathForResource(null);
    assertNull(nullFile);
  }

  /**
   * Test various handlers
   */
  @Test
  public void testHandlers() {
    VariablesChangedCallback variablesChangedCallback = new VariablesChangedCallback() {
      @Override
      public void variablesChanged() {

      }
    };
    Leanplum.addVariablesChangedHandler(variablesChangedCallback);

    ArrayList<VariablesChangedCallback> handlers = CollectionUtil.uncheckedCast(TestClassUtil.getField(Leanplum.class, "variablesChangedHandlers"));
    assertEquals(1, handlers.size());

    Leanplum.removeVariablesChangedHandler(variablesChangedCallback);
    handlers = CollectionUtil.uncheckedCast(TestClassUtil.getField(Leanplum.class, "variablesChangedHandlers"));
    assertEquals(0, handlers.size());

    Leanplum.addOnceVariablesChangedAndNoDownloadsPendingHandler(variablesChangedCallback);
    handlers = CollectionUtil.uncheckedCast(TestClassUtil.getField(Leanplum.class, "onceNoDownloadsHandlers"));
    assertEquals(1, handlers.size());

    Leanplum.removeOnceVariablesChangedAndNoDownloadsPendingHandler(variablesChangedCallback);
    handlers = CollectionUtil.uncheckedCast(TestClassUtil.getField(Leanplum.class, "onceNoDownloadsHandlers"));
    assertEquals(0, handlers.size());
  }

  /**
   * Test for {@link Leanplum#getDeviceId()}.
   */
  @Test
  public void testGetDeviceId() {
    String deviceId = Leanplum.getDeviceId();
    assertNull(deviceId);
    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());
    deviceId = Leanplum.getDeviceId();
    assertNotNull(deviceId);
  }

  /**
   * Tests for parsing counters
   */
  @Test
  public void testStartResponseShouldParseCounters() {
    // Setup sdk.
    setupSDK(mContext, "/responses/simple_start_response.json");

    // check that incrementing counters work
    Leanplum.countAggregator().incrementCount("testCounter1");
    assertEquals(1, Leanplum.countAggregator().getCounts().get("testCounter1").intValue());
    Leanplum.countAggregator().incrementCount("testCounter2");
    assertEquals(1, Leanplum.countAggregator().getCounts().get("testCounter2").intValue());
  }

  @Test
  public void testParseEmptySdkCounters() throws JSONException {
    JSONObject response = new JSONObject();
    Set<String> parsedCounters = Leanplum.parseSdkCounters(response);
    assertEquals(new HashSet<String>(), parsedCounters);
  }

  @Test
  public void testParseSdkCounters() throws JSONException {
    JSONObject response = new JSONObject();
    response.put(Constants.Keys.ENABLED_COUNTERS, new JSONArray("[\"test\"]"));
    Set<String> parsedCounters = Leanplum.parseSdkCounters(response);
    assertEquals(new HashSet<>(Arrays.asList("test")), parsedCounters);
  }

  @Test
  public void testParseFilenameToURLs() throws JSONException {
    JSONObject response = new JSONObject();
    JSONObject files = new JSONObject();
    files.put("file.jpg", "https://www.domain.com/file.jpg");
    response.put(Constants.Keys.FILES, files);

    Map<String, String> parsedFiles= Leanplum.parseFilenameToURLs(response);
    assertEquals(JsonConverter.mapFromJson(files), parsedFiles);
  }

  /**
   * Tests for parsing filenameToURLs
   */
  @Test
  public void testStartResponseShouldParseFilenameToURLs() {
    // Setup sdk.
    setupSDK(mContext, "/responses/simple_start_response.json");

    Map<String, String> files = new HashMap<>();
    files.put("file1.jpg", "http://www.domain.com/file1.jpg");
    files.put("file2.jpg", "http://www.domain.com/file2.jpg");

    assertEquals(files, FileManager.filenameToURLs);
  }

  /**
   * Tests for parsing feature flags
   */
  @Test
  public void testStartResponseShouldParseFeatureFlags() {
    // Setup sdk.
    setupSDK(mContext, "/responses/simple_start_response.json");

    assertEquals(true, FeatureFlagManager.INSTANCE.isFeatureFlagEnabled("testFeatureFlag1"));
    assertEquals(true, FeatureFlagManager.INSTANCE.isFeatureFlagEnabled("testFeatureFlag2"));
    assertEquals(false, FeatureFlagManager.INSTANCE.isFeatureFlagEnabled("missingFeatureFlag"));
  }

  @Test
  public void testParseEmptyFeatureFlags() {
    JSONObject response = new JSONObject();
    Set<String> parsedFeatureFlags = Leanplum.parseFeatureFlags(response);
    assertEquals(new HashSet<String>(), parsedFeatureFlags);
  }

  @Test
  public void testParseFeatureFlags() throws JSONException {
    JSONObject response = new JSONObject();
    response.put(Constants.Keys.ENABLED_FEATURE_FLAGS, new JSONArray("[\"test\"]"));
    Set<String> parsedFeatureFlags = Leanplum.parseFeatureFlags(response);
    assertEquals(new HashSet<>(Arrays.asList("test")), parsedFeatureFlags);
  }

  /**
   * Test trigger message displayed calls callback
   */
  @Test
  public void testTriggerMessageDisplayedCallbackCalled() throws Exception {
    final String messageID = "testMessageID";
    final String messageBody = "testMessageBody";
    final String userID = "testUserID";

    Map<String, Object> args = new HashMap<>();
    args.put("Message", messageBody);
    final ActionContext testActionContext = new ActionContext("test", args, messageID);

    doReturn(userID).when(Leanplum.class, "getUserId");

    class CallbackTest {
      public boolean callbackCalled = false;
      public MessageDisplayedCallback callback;

      CallbackTest() {
        callback = new MessageDisplayedCallback() {
          @Override
          public void messageDisplayed(MessageArchiveData messageArchiveData) {
            callbackCalled = true;
            assertTrue(messageArchiveData.messageID.equals(messageID));
            assertTrue(messageArchiveData.messageBody.equals(messageBody));
            assertTrue(messageArchiveData.recipientUserID.equals(userID));
            long timeDiff = new Date().getTime() - messageArchiveData.deliveryDateTime.getTime();
            assertTrue(timeDiff < 1000);
          }
        };
      }
    }

    CallbackTest callbackTest = new CallbackTest();

    Leanplum.addMessageDisplayedHandler(callbackTest.callback);
    Leanplum.triggerMessageDisplayed(testActionContext);
    assertTrue(callbackTest.callbackCalled);
  }

  /**
   * Test messageBody gets correct body from context for string.
   */
  @Test
  public void testMessageBodyFromContextGetsCorrectBodyForString() throws Exception {
    final String messageID = "testMessageID";
    final String messageBody = "testMessageBody";
    final String userID = "testUserID";

    Map<String, Object> args = new HashMap<>();
    args.put("Message", messageBody);
    final ActionContext testActionContext = new ActionContext("test", args, messageID);

    doReturn(userID).when(Leanplum.class, "getUserId");
    String body = Leanplum.messageBodyFromContext(testActionContext);
    assertEquals(body, messageBody);
  }

  /**
   * Test messageBody gets correct body from context for key text.
   */
  @Test
  public void testMessageBodyFromContextGetsCorrectBodyForKeyText() throws Exception {
    final String messageID = "testMessageID";
    final String messageBody = "testMessageBody";
    final String userID = "testUserID";

    Map<String, Object> messageObject = new HashMap<>();
    messageObject.put("Text", messageBody);

    Map<String, Object> args = new HashMap<>();
    args.put("Message", messageObject);

    final ActionContext testActionContext = new ActionContext("test", args, messageID);

    doReturn(userID).when(Leanplum.class, "getUserId");
    String body = Leanplum.messageBodyFromContext(testActionContext);
    assertEquals(body, messageBody);
  }

  /**
   * Test messageBody gets correct body from context for key text value.
   */
  @Test
  public void testMessageBodyFromContextGetsCorrectBodyForKeyTextValue() throws Exception {
    final String messageID = "testMessageID";
    final String messageBody = "testMessageBody";
    final String userID = "testUserID";

    Map<String, Object> messageObject = new HashMap<>();
    messageObject.put("Text value", messageBody);

    Map<String, Object> args = new HashMap<>();
    args.put("Message", messageObject);

    final ActionContext testActionContext = new ActionContext("test", args, messageID);

    doReturn(userID).when(Leanplum.class, "getUserId");
    String body = Leanplum.messageBodyFromContext(testActionContext);
    assertEquals(body, messageBody);
  }

  @Test
  public void testVariantsForceContentUpdate() throws Exception {
    final Semaphore semaphore = new Semaphore(1);
    semaphore.acquire();

    // Seed response from the file.
    ResponseHelper.seedResponse("/responses/simple_start_response.json");

    // Expected request params.
    final HashMap<String, Object> expectedRequestParams = CollectionUtil.newHashMap(
            "city", "(detect)",
            "country", "(detect)",
            "location", "(detect)",
            "region", "(detect)",
            "locale", "en_US"
    );

    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_START, apiMethod);
        assertTrue(params.keySet().containsAll(expectedRequestParams.keySet()));
        assertTrue(params.values().containsAll(expectedRequestParams.values()));
      }
    });

    Leanplum.start(mContext, new StartCallback() {
      @Override
      public void onResponse(boolean success) {
        assertTrue(success);
        semaphore.release();
      }
    });
    assertTrue(Leanplum.hasStarted());

    assertEquals(0, VarCache.variants().size());

    semaphore.acquire();

    // Seed getVars response.
    ResponseHelper.seedResponse("/responses/variants_response.json");

    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_GET_VARS, apiMethod);
      }
    });

    Leanplum.forceContentUpdate(new VariablesChangedCallback() {
      @Override
      public void variablesChanged() {
        semaphore.release();
      }
    });

    assertEquals(4, VarCache.variants().size());
  }

  @Test
  public void testStartChangeCallBackForOffline() throws Exception {
    //Offline Mode.
     ResponseHelper.seedResponseNull();

    // Expected request params.
    final HashMap<String, Object> expectedRequestParams = CollectionUtil.newHashMap(
        "city", "(detect)",
        "country", "(detect)",
        "location", "(detect)",
        "region", "(detect)",
        "locale", "en_US"
    );

    // Validate request.
    // Validate request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_START, apiMethod);
        assertTrue(params.keySet().containsAll(expectedRequestParams.keySet()));
        assertTrue(params.values().containsAll(expectedRequestParams.values()));
      }
    });


    Leanplum.start(mContext, new StartCallback() {
      @Override
      public void onResponse(boolean success) {
        assertFalse(success); // in offline mode the success flag is false and isStarted is true
      }
    });
    assertTrue(Leanplum.hasStarted());

  }

  @Test
  public void testVariableChangeCallBacksForOffline() throws Exception {
    final Semaphore semaphore = new Semaphore(1);

    semaphore.acquire();

    // Seed getVars response.
    ResponseHelper.seedResponseNull();

    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_GET_VARS, apiMethod);
      }
    });

    Leanplum.forceContentUpdate(new VariablesChangedCallback() {
      @Override
      public void variablesChanged() {
        semaphore.release();
      }
    });

  }

  /**
   * Tests if appId and accessKey are loaded from Android resources
   * if they aren't presented before calling start.
   */
  @Test
  public void testApiConfigLoadFromResources() throws Exception {
    // assure appId and accessKey are not set
    TestClassUtil.setField(APIConfig.getInstance(), "appId", null);
    TestClassUtil.setField(APIConfig.getInstance(), "accessKey", null);

    String appId = "app_id";
    String accessKey = "access_key";

    class MockedLoader extends ApiConfigLoader {
      private MockedLoader(Context context) {
        super(context);
      }
      @Override
      public void loadFromResources(KeyListener prodKeyListener, KeyListener devKeyListener) {
        prodKeyListener.onKeysLoaded(appId, accessKey);
      }
    }

    PowerMockito
        .whenNew(ApiConfigLoader.class)
        .withAnyArguments()
        .thenReturn(new MockedLoader(mContext));

    Leanplum.start(mContext);

    assertTrue(Leanplum.hasStarted());
    assertEquals(appId, APIConfig.getInstance().appId());
    assertEquals(accessKey, APIConfig.getInstance().accessKey());
  }

  /**
   * Tests if appId and accessKey are not overridden if they are presented before calling Start.
   */
  @Test
  public void testApiConfigNotOverridden() throws Exception {
    String appId = APIConfig.getInstance().appId();
    String accessKey = APIConfig.getInstance().accessKey();

    class MockedLoader extends ApiConfigLoader {
      private MockedLoader(Context context) {
        super(context);
      }
      @Override
      public void loadFromResources(KeyListener prodKeyListener, KeyListener devKeyListener) {
        prodKeyListener.onKeysLoaded("arbitrary_app_id", "arbitrary_access_key");
      }
    }

    PowerMockito
        .whenNew(ApiConfigLoader.class)
        .withAnyArguments()
        .thenReturn(new MockedLoader(mContext));

    Leanplum.start(mContext);

    assertTrue(Leanplum.hasStarted());
    assertEquals(appId, APIConfig.getInstance().appId());
    assertEquals(accessKey, APIConfig.getInstance().accessKey());
  }
}
