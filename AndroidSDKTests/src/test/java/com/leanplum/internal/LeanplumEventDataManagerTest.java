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

import java.lang.reflect.Field;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * Tests for {@link LeanplumEventDataManager} class.
 *
 * @author Anna Orlova
 */
@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = 16,
    application = LeanplumTestApp.class
)
public class LeanplumEventDataManagerTest {
  // The target context of the instrumentation.
  private Application mContext;

  /**
   * Runs before every test case.
   */
  @Before
  public void setUp() throws Exception {
    this.mContext = RuntimeEnvironment.application;
    assertNotNull(this.mContext);
    Leanplum.setApplicationContext(this.mContext);

    ShadowOperationQueue shadowOperationQueue = new ShadowOperationQueue();
    Field instance = OperationQueue.class.getDeclaredField("instance");
    instance.setAccessible(true);
    instance.set(instance, shadowOperationQueue);
  }

  @After
  public void tearDown() {
    setDatabaseToNull();
  }

  @Test
  public void testInsertAndGetEvents() {
    // Insert 3 events in the database
    int count = 3;
    LeanplumEventDataManager.sharedInstance().insertEvent("{event:0}");
    LeanplumEventDataManager.sharedInstance().insertEvent("{event:1}");
    LeanplumEventDataManager.sharedInstance().insertEvent("{event:2}");

    // Test getEventsCount method
    assertEquals(count, LeanplumEventDataManager.sharedInstance().getEventsCount());

    // Get list of events from SQLite.
    List<Map<String, Object>> events = LeanplumEventDataManager.sharedInstance().getEvents(3);
    assertNotNull(events);
    assertEquals(3, events.size());
    // Checks that all events inserted to SQLite in order.
    assertEquals(0, (int) events.get(0).get("event"));
    assertEquals(1, (int) events.get(1).get("event"));
    assertEquals(2, (int) events.get(2).get("event"));
  }

  public static void setDatabaseToNull(){
    ReflectionHelpers.setStaticField(LeanplumEventDataManager.class, "instance", null);
  }
}
