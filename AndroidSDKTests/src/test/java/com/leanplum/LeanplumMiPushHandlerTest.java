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
import android.content.SharedPreferences;
import com.leanplum.__setup.AbstractTest;
import com.leanplum.internal.Util;
import com.leanplum.utils.SharedPreferencesUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;

public class LeanplumMiPushHandlerTest extends AbstractTest {

  @After
  public void tearDown() {
    LeanplumMiPushHandler.setApplication(null, null);
  }

  /**
   * Test parsing of {}
   */
  @Test
  public void testParsePayloadEmpty() {
    LeanplumMiPushHandler handler = new LeanplumMiPushHandler();
    Map<String, String> empty = new HashMap<>();
    assertEquals(empty, handler.parsePayload("{}"));
  }

  /**
   * Test parsing of {"a":"b"}
   */
  @Test
  public void testParsePayloadOneItem() {
    LeanplumMiPushHandler handler = new LeanplumMiPushHandler();
    Map<String, String> oneItem = new HashMap<>();
    oneItem.put("a", "b");
    assertEquals(oneItem, handler.parsePayload("{\"a\":\"b\"}"));
  }

  /**
   * Test parsing of {"x":"a", "y":"b", "z":"c"}
   */
  @Test
  public void testParsePayloadSeveralItems() {
    LeanplumMiPushHandler handler = new LeanplumMiPushHandler();
    Map<String, String> oneItem = new HashMap<>();
    oneItem.put("x", "a");
    oneItem.put("y", "b");
    oneItem.put("z", "c");
    assertEquals(oneItem, handler.parsePayload("{\"x\":\"a\", \"y\":\"b\", \"z\":\"c\"}"));
  }

  /**
   * Test parsing of {"a":{"b":"c"}}
   */
  @Test
  public void testParsePayloadNested() {
    LeanplumMiPushHandler handler = new LeanplumMiPushHandler();
    Map<String, String> nestedItem = new HashMap<>();
    nestedItem.put("a", "{\"b\":\"c\"}");
    assertEquals(nestedItem, handler.parsePayload("{\"a\":{\"b\":\"c\"}}"));
  }

  /**
   * Test parsing of
   * {
   *   "lp_version":"1",
   *   "lp_channel":{"importance":3,"name":"rondo-channel","id":"123"},
   *   "lp_message":"Push message goes here.",
   *   "_lpm":"4700540011347968",
   *   "_lpx":{"__name__":"Request App Rating"}
   * }
   */
  @Test
  public void testParsePayloadMessage() {
    LeanplumMiPushHandler handler = new LeanplumMiPushHandler();
    Map<String, String> nestedItem = new HashMap<>();
    nestedItem.put("_lpm", "4700540011347968");
    nestedItem.put("lp_version", "1");
    nestedItem.put("lp_channel", "{\"importance\":3,\"name\":\"rondo-channel\",\"id\":\"123\"}");
    nestedItem.put("lp_message", "Push message goes here.");
    nestedItem.put("_lpx", "{\"__name__\":\"Request App Rating\"}");
    assertEquals(nestedItem, handler.parsePayload("{\"lp_version\":\"1\", \"lp_channel\":{\"importance\":3,\"name\":\"rondo-channel\",\"id\":\"123\"}, \"lp_message\":\"Push message goes here.\", \"_lpm\":\"4700540011347968\", \"_lpx\":{\"__name__\":\"Request App Rating\"}}"));
  }

  @Test
  public void testSetApplicationFail() {
    String appId = "";
    String appKey = "";
    LeanplumMiPushHandler.setApplication(appId, appKey);
    LeanplumMiPushProvider provider = new LeanplumMiPushProvider();
    assertFalse(provider.appRegistered);
  }

  @Test
  public void testSetApplication() {
    String appId = "id";
    String appKey = "key";
    LeanplumMiPushHandler.setApplication(appId, appKey);
    LeanplumMiPushProvider provider = new LeanplumMiPushProvider();
    assertTrue(provider.appRegistered);
  }

}
