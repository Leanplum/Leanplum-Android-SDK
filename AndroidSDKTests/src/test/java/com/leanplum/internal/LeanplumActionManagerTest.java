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
package com.leanplum.internal;

import androidx.annotation.NonNull;
import com.leanplum.Leanplum;
import com.leanplum.__setup.AbstractTest;

import com.leanplum._whitebox.utilities.ResponseHelper;
import java.util.Date;
import java.util.HashMap;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Milos Jakovljevic
 */
public class LeanplumActionManagerTest extends AbstractTest {
  private static final long DAY_MILLIS = 24 * 60 * 60 * 1000;

  private static class DayAheadClock extends Clock {
    @Override
    long currentTimeMillis() {
      return System.currentTimeMillis() + DAY_MILLIS;
    }

    @NonNull
    @Override
    Date newDate() {
      Date date = new Date();
      date.setTime(date.getTime() + DAY_MILLIS);
      return date;
    }
  }

  private static class WeekAheadClock extends Clock {
    @Override
    long currentTimeMillis() {
      return System.currentTimeMillis() + 7 * DAY_MILLIS;
    }

    @NonNull
    @Override
    Date newDate() {
      Date date = new Date();
      date.setTime(date.getTime() + 7 * DAY_MILLIS);
      return date;
    }
  }

  @After
  public void tearDown() {
    Clock.setInstance(Clock.SYSTEM);
  }

  @Test
  public void testImpressions() {
    spy(LeanplumInternal.class);

    String messageId = "message##id";
    Map<String, String> trackArgs = new HashMap<>();
    trackArgs.put("messageId", messageId);

    ActionManager.getInstance().recordMessageImpression(messageId);
    ActionManager.getInstance().recordMessageImpression(messageId);
    Map<String, Number> impressions = ActionManager.getInstance().getMessageImpressionOccurrences(messageId);
    assertEquals(4, impressions.size());
    assertEquals(0, impressions.get("min").intValue());
    assertEquals(1, impressions.get("max").intValue());

    verifyStatic(times(2));
    LeanplumInternal.track(eq(null), eq(0.0), eq(null), eq(null), eq(trackArgs));
  }

  @Test
  public void testMessageTriggers() {
    ActionManager.getInstance().saveMessageTriggerOccurrences(5, "message##id");
    int occurences = ActionManager.getInstance().getMessageTriggerOccurrences("message##id");
    assertEquals(5, occurences);
  }

  @Test
  public void testRecordChainedActionImpression() {
    spy(LeanplumInternal.class);

    String actionId = "action##id";
    Map<String, String> trackArgs = new HashMap<>();
    trackArgs.put("messageId", actionId);

    ActionManager.getInstance().recordChainedActionImpression(actionId);
    Map<String, Number> impressions =
        ActionManager.getInstance().getMessageImpressionOccurrences(actionId);
    assertTrue(impressions.isEmpty());

    verifyStatic(times(1));
    LeanplumInternal.track(eq(null), eq(0.0), eq(null), eq(null), eq(trackArgs));
  }

  @Test
  public void testDailyOccurrences() {
    ResponseHelper.seedResponse("/responses/local_caps_response.json");
    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());

    ActionManager.getInstance().recordMessageImpression("message#1");
    ActionManager.getInstance().recordMessageImpression("message#2");
    ActionManager.getInstance().recordMessageImpression("message#3");
    assertEquals(3, ActionManager.getInstance().dailyOccurrencesCount());

    Clock.setInstance(new DayAheadClock());
    assertEquals(0, ActionManager.getInstance().dailyOccurrencesCount());

  }

  @Test
  public void testWeeklyOccurrences() {
    ResponseHelper.seedResponse("/responses/local_caps_response.json");
    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());

    ActionManager.getInstance().recordMessageImpression("message#1");
    ActionManager.getInstance().recordMessageImpression("message#2");
    assertEquals(2, ActionManager.getInstance().weeklyOccurrencesCount());

    Clock.setInstance(new DayAheadClock());
    ActionManager.getInstance().recordMessageImpression("message#3");
    assertEquals(3, ActionManager.getInstance().weeklyOccurrencesCount());

    Clock.setInstance(new WeekAheadClock());
    ActionManager.getInstance().recordMessageImpression("message#4");
    assertEquals(2, ActionManager.getInstance().weeklyOccurrencesCount());
  }

  @Test
  public void testOccurrencesCounting() {
    ResponseHelper.seedResponse("/responses/local_caps_response.json");
    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());

    ActionManager.getInstance().recordMessageImpression("message#1");
    ActionManager.getInstance().recordMessageImpression("message#2");
    ActionManager.getInstance().recordMessageImpression("message#3");
    ActionManager.getInstance().recordMessageImpression("message#1");
    ActionManager.getInstance().recordMessageImpression("message#2");
    ActionManager.getInstance().recordMessageImpression("message#3");

    assertEquals(6, ActionManager.getInstance().sessionOccurrencesCount());
    assertEquals(6, ActionManager.getInstance().dailyOccurrencesCount());
    assertEquals(6, ActionManager.getInstance().weeklyOccurrencesCount());
  }

  @Test
  public void testMaxOccurrencesPerMessage() {
    ResponseHelper.seedResponse("/responses/local_caps_response.json");
    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());

    int maxOccurrences = 100;
    int occurrences = maxOccurrences + 5;

    for (int i = 0; i < occurrences; i++) {
      ActionManager.getInstance().recordMessageImpression("message#00");
    }

    assertEquals(maxOccurrences, ActionManager.getInstance().dailyOccurrencesCount());
    assertEquals(maxOccurrences, ActionManager.getInstance().weeklyOccurrencesCount());
  }

  @Test
  public void testShouldSuppressMessagesSessionLimit() {
    ResponseHelper.seedResponse("/responses/local_caps_session_response.json");
    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());

    ActionManager.getInstance().recordMessageImpression("message#1");
    ActionManager.getInstance().recordMessageImpression("message#2");
    assertFalse(ActionManager.getInstance().shouldSuppressMessages());

    ActionManager.getInstance().recordMessageImpression("message#3");
    assertTrue(ActionManager.getInstance().shouldSuppressMessages());
  }

  @Test
  public void testShouldSuppressMessagesDayLimit() {
    ResponseHelper.seedResponse("/responses/local_caps_day_response.json");
    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());

    ActionManager.getInstance().recordMessageImpression("message#1");
    ActionManager.getInstance().recordMessageImpression("message#2");
    assertFalse(ActionManager.getInstance().shouldSuppressMessages());

    ActionManager.getInstance().recordMessageImpression("message#3");
    assertTrue(ActionManager.getInstance().shouldSuppressMessages());

    Clock.setInstance(new DayAheadClock());
    assertFalse(ActionManager.getInstance().shouldSuppressMessages());
  }

  @Test
  public void testShouldSuppressMessagesWeekLimit() {
    ResponseHelper.seedResponse("/responses/local_caps_week_response.json");
    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());

    ActionManager.getInstance().recordMessageImpression("message#1");
    ActionManager.getInstance().recordMessageImpression("message#2");
    assertFalse(ActionManager.getInstance().shouldSuppressMessages());

    ActionManager.getInstance().recordMessageImpression("message#3");
    assertTrue(ActionManager.getInstance().shouldSuppressMessages());

    Clock.setInstance(new WeekAheadClock());
    assertFalse(ActionManager.getInstance().shouldSuppressMessages());
  }

}
