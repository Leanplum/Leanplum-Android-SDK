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

import static org.junit.Assert.assertEquals;

import android.os.Bundle;
import com.leanplum.__setup.AbstractTest;
import com.leanplum._whitebox.utilities.RequestHelper;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Constants.Keys;
import com.leanplum.internal.Constants.Params;
import com.leanplum.internal.RequestBuilder;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.powermock.api.mockito.PowerMockito;

public class PushTrackingTest extends AbstractTest {

  @After
  public void tearDown() {
    Leanplum.setPushDeliveryTracking(true);
  }

  @Test
  public void testTrackDelivery() {
    setupSDK(mContext, "/responses/simple_start_response.json");
    String messageId = "id";
    String expectedTrackParams = "{\"messageID\":\"id\"}";
    String expectedEvent = "Push Delivered";

    // Verify request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> requestParams) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);
        assertEquals(expectedEvent, requestParams.get(Params.EVENT));
        assertEquals(expectedTrackParams, requestParams.get(Constants.Params.PARAMS));
      }
    });

    Bundle notification = new Bundle();
    notification.putString(Keys.PUSH_MESSAGE_ID_NO_MUTE, messageId);
    PushTracking.trackDelivery(notification);
  }

  @Test
  public void testTrackDeliveryWithChannel() {
    setupSDK(mContext, "/responses/simple_start_response.json");
    String messageId = "id";
    String expectedTrackParams =
        "{\"channel\":\"FCM_SILENT_TRACK\",\"messageID\":\""+messageId+"\"}";
    String expectedEvent = "Push Delivered";

    // Verify request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> requestParams) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);
        assertEquals(expectedEvent, requestParams.get(Params.EVENT));
        assertEquals(expectedTrackParams, requestParams.get(Constants.Params.PARAMS));
      }
    });

    Bundle notification = new Bundle();
    notification.putString(Keys.PUSH_MESSAGE_ID_NO_MUTE, messageId);
    notification.putString(Keys.CHANNEL_INTERNAL_KEY, PushTracking.CHANNEL_FCM_SILENT_TRACK);
    PushTracking.trackDelivery(notification);
  }

  @Test
  public void testTrackDeliveryAllParams() {
    setupSDK(mContext, "/responses/simple_start_response.json");
    String messageId = "id";
    String occurrenceId = "occurrence id";
    String sentTime = "123";
    String expectedTrackParams =
        "{\"channel\":\"FCM_SILENT_TRACK\",\"messageID\":\"" + messageId
            + "\",\"sentTime\":\"" + sentTime
            + "\",\"occurrenceId\":\"" + occurrenceId + "\"}";
    String expectedEvent = "Push Delivered";

    // Verify request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> requestParams) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);
        assertEquals(expectedEvent, requestParams.get(Params.EVENT));
        assertEquals(expectedTrackParams, requestParams.get(Constants.Params.PARAMS));
      }
    });

    Bundle notification = new Bundle();
    notification.putString(Keys.PUSH_MESSAGE_ID_NO_MUTE, messageId);
    notification.putString(Keys.PUSH_OCCURRENCE_ID, occurrenceId);
    notification.putString(Keys.PUSH_SENT_TIME, sentTime);
    notification.putString(Keys.CHANNEL_INTERNAL_KEY, PushTracking.CHANNEL_FCM_SILENT_TRACK);
    PushTracking.trackDelivery(notification);
  }

  @Test
  public void testTrackDeliveryEnabled() {
    setupSDK(mContext, "/responses/simple_start_response.json");

    Bundle notification = new Bundle();
    notification.putString(Keys.PUSH_MESSAGE_ID_NO_MUTE, "id");
    PushTracking.trackDelivery(notification);

    PowerMockito.verifyStatic(times(1));
    Leanplum.track(anyString(), anyMap());
  }

  @Test
  public void testTrackDeliveryDisabled() {
    Leanplum.setPushDeliveryTracking(false);

    setupSDK(mContext, "/responses/simple_start_response.json");

    Bundle notification = new Bundle();
    notification.putString(Keys.PUSH_MESSAGE_ID_NO_MUTE, "id");
    PushTracking.trackDelivery(notification);

    PowerMockito.verifyStatic(times(0));
    Leanplum.track(anyString(), anyMap());
  }

  @Test
  public void testTrackOpen() {
    setupSDK(mContext, "/responses/simple_start_response.json");
    String messageId = "id";
    String expectedTrackParams = "{\"messageID\":\"id\"}";
    String expectedEvent = "Push Opened";

    // Verify request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> requestParams) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);
        assertEquals(expectedEvent, requestParams.get(Params.EVENT));
        assertEquals(expectedTrackParams, requestParams.get(Constants.Params.PARAMS));
      }
    });

    Bundle notification = new Bundle();
    notification.putString(Keys.PUSH_MESSAGE_ID_NO_MUTE, messageId);
    PushTracking.trackOpen(notification);
  }

  @Test
  public void testTrackOpenWithChannel() {
    setupSDK(mContext, "/responses/simple_start_response.json");
    String messageId = "id";
    String expectedTrackParams =
        "{\"channel\":\"FCM\",\"messageID\":\""+messageId+"\"}";
    String expectedEvent = "Push Opened";

    // Verify request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> requestParams) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);
        assertEquals(expectedEvent, requestParams.get(Params.EVENT));
        assertEquals(expectedTrackParams, requestParams.get(Constants.Params.PARAMS));
      }
    });

    Bundle notification = new Bundle();
    notification.putString(Keys.PUSH_MESSAGE_ID_NO_MUTE, messageId);
    notification.putString(Keys.CHANNEL_INTERNAL_KEY, PushTracking.CHANNEL_FCM);
    PushTracking.trackOpen(notification);
  }

  @Test
  public void testTrackOpenAllParams() {
    setupSDK(mContext, "/responses/simple_start_response.json");
    String messageId = "id";
    String occurrenceId = "occurrence id";
    String sentTime = "123";
    String expectedTrackParams =
        "{\"channel\":\"FCM\",\"messageID\":\"" + messageId
            + "\",\"sentTime\":\"" + sentTime
            + "\",\"occurrenceId\":\"" + occurrenceId + "\"}";
    String expectedEvent = "Push Opened";

    // Verify request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> requestParams) {
        assertEquals(RequestBuilder.ACTION_TRACK, apiMethod);
        assertEquals(expectedEvent, requestParams.get(Params.EVENT));
        assertEquals(expectedTrackParams, requestParams.get(Constants.Params.PARAMS));
      }
    });

    Bundle notification = new Bundle();
    notification.putString(Keys.PUSH_MESSAGE_ID_NO_MUTE, messageId);
    notification.putString(Keys.PUSH_OCCURRENCE_ID, occurrenceId);
    notification.putString(Keys.PUSH_SENT_TIME, sentTime);
    notification.putString(Keys.CHANNEL_INTERNAL_KEY, PushTracking.CHANNEL_FCM);
    PushTracking.trackOpen(notification);
  }
}