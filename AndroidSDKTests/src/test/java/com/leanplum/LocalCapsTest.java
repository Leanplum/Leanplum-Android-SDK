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
import static org.junit.Assert.assertTrue;

import com.leanplum.__setup.AbstractTest;
import com.leanplum._whitebox.utilities.ResponseHelper;
import com.leanplum.internal.VarCache;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class LocalCapsTest extends AbstractTest {

  @Test
  public void testParseSessionLimit() {
    ResponseHelper.seedResponse("/responses/local_caps_session_response.json");
    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());

    Map<String, Object> session = new HashMap<>();
    session.put("channel", "IN_APP");
    session.put("limit", 3);
    session.put("type", "SESSION");

    List<Map<String, Object>> caps = VarCache.localCaps();
    assertEquals(caps.size(), 1);
    assertTrue(caps.contains(session));
  }

  @Test
  public void testParseDayLimit() {
    ResponseHelper.seedResponse("/responses/local_caps_day_response.json");
    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());

    Map<String, Object> day = new HashMap<>();
    day.put("channel", "IN_APP");
    day.put("limit", 3);
    day.put("type", "DAY");

    List<Map<String, Object>> caps = VarCache.localCaps();
    assertEquals(caps.size(), 1);
    assertTrue(caps.contains(day));
  }

  @Test
  public void testParseWeekLimit() {
    ResponseHelper.seedResponse("/responses/local_caps_week_response.json");
    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());

    Map<String, Object> week = new HashMap<>();
    week.put("channel", "IN_APP");
    week.put("limit", 3);
    week.put("type", "WEEK");

    List<Map<String, Object>> caps = VarCache.localCaps();
    assertEquals(caps.size(), 1);
    assertTrue(caps.contains(week));
  }

  @Test
  public void testParseAll() {
    Map<String, Object> session = new HashMap<>();
    session.put("channel", "IN_APP");
    session.put("limit", 1);
    session.put("type", "SESSION");

    Map<String, Object> day = new HashMap<>();
    day.put("channel", "IN_APP");
    day.put("limit", 2);
    day.put("type", "DAY");

    Map<String, Object> week = new HashMap<>();
    week.put("channel", "IN_APP");
    week.put("limit", 3);
    week.put("type", "WEEK");

    ResponseHelper.seedResponse("/responses/local_caps_response.json");
    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());

    List<Map<String, Object>> caps = VarCache.localCaps();

    assertEquals(caps.size(), 3);
    assertTrue(caps.contains(session));
    assertTrue(caps.contains(day));
    assertTrue(caps.contains(week));
  }

}
