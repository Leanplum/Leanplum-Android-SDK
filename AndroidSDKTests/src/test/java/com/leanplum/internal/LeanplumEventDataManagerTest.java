/*
 * Copyright 2017, Leanplum, Inc. All rights reserved.
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
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

import com.leanplum.Leanplum;
import com.leanplum.__setup.LeanplumTestApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;
import java.util.Locale;
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
  public void setUp() {
    this.mContext = RuntimeEnvironment.application;
    assertNotNull(this.mContext);
    Leanplum.setApplicationContext(this.mContext);
  }

  @Test
  public void migrateFromSharedPreferencesTest() {
    setDatabaseToNull();

    SharedPreferences preferences = mContext.getSharedPreferences(
        "__leanplum__", Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = preferences.edit();
    int count = preferences.getInt(Constants.Defaults.COUNT_KEY, 0);
    assertEquals(0, count);

    // Insert 3 events to shared preferences.
    String itemKey = String.format(Locale.US, Constants.Defaults.ITEM_KEY, count);
    editor.putString(itemKey, "{event:0}");
    count++;
    itemKey = String.format(Locale.US, Constants.Defaults.ITEM_KEY, count);
    editor.putString(itemKey, "{event:1}");
    count++;
    itemKey = String.format(Locale.US, Constants.Defaults.ITEM_KEY, count);
    editor.putString(itemKey, "{event:2}");
    count++;
    editor.putInt(Constants.Defaults.COUNT_KEY, count);
    editor.commit();

    // Get count from shared preferences and assert count equals 3.
    count = preferences.getInt(Constants.Defaults.COUNT_KEY, 0);
    assertEquals(3, count);

    // Create database. That should also migrate data from shared preferences.
    LeanplumEventDataManager.sharedInstance();
    // Assert in database after migration 3 events.
    assertEquals(count, LeanplumEventDataManager.sharedInstance().getEventsCount());

    // Assert count 0 after data migration.
    count = preferences.getInt(Constants.Defaults.COUNT_KEY, 0);
    assertEquals(0, count);

    // Get list of events from SQLite.
    List<Map<String, Object>> events = LeanplumEventDataManager.sharedInstance().getEvents(3);
    assertNotNull(events);
    assertEquals(3, events.size());
    // Checks that all events inserted to SQLite in order.
    assertEquals(0, (int) events.get(0).get("event"));
    assertEquals(1, (int) events.get(1).get("event"));
    assertEquals(2, (int) events.get(2).get("event"));

    setDatabaseToNull();
  }

  public static void setDatabaseToNull(){
    ReflectionHelpers.setStaticField(LeanplumEventDataManager.class, "instance", null);
  }
}

