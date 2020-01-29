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
package com.leanplum;

import android.annotation.SuppressLint;
import android.app.Activity;

import com.leanplum.__setup.LeanplumTestHelper;
import com.leanplum.callbacks.PostponableAction;

import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Tests {@link LeanplumActivityHelper}.
 *
 * @author Ben Marten
 */
public class LeanplumActivityHelperTest extends TestCase {
  /**
   * Tests the testQueueActionUponActive method.
   */
  public static void testQueueActionUponActive() throws InterruptedException, NoSuchFieldException,
      IllegalAccessException {
    // Test no activity.
    final CountDownLatch latch1 = new CountDownLatch(1);
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        latch1.countDown();
      }
    };
    LeanplumActivityHelper.queueActionUponActive(runnable);
    LeanplumTestHelper.assertLatchIsNotCalled(latch1, 1);
    assertTrue(isRunnableQueued(runnable));

    FakeActivity fakeActivity = new FakeActivity();
    LeanplumActivityHelper.setCurrentActivity(fakeActivity);

    // Test activity is finishing.
    final CountDownLatch latch2 = new CountDownLatch(1);
    runnable = new Runnable() {
      @Override
      public void run() {
        latch2.countDown();
      }
    };
    LeanplumActivityHelper.queueActionUponActive(runnable);
    LeanplumTestHelper.assertLatchIsNotCalled(latch1, 1);
    assertTrue(isRunnableQueued(runnable));

    fakeActivity.setFinishing(false);
    LeanplumActivityHelper.isActivityPaused = true;

    // Test activity is paused.
    final CountDownLatch latch3 = new CountDownLatch(1);
    runnable = new Runnable() {
      @Override
      public void run() {
        latch3.countDown();
      }
    };
    LeanplumActivityHelper.queueActionUponActive(runnable);
    LeanplumTestHelper.assertLatchIsNotCalled(latch1, 1);
    assertTrue(isRunnableQueued(runnable));

    LeanplumActivityHelper.isActivityPaused = false;

    // Test regular action.
    final CountDownLatch latch4 = new CountDownLatch(1);
    runnable = new Runnable() {
      @Override
      public void run() {
        latch4.countDown();
      }
    };
    LeanplumActivityHelper.queueActionUponActive(runnable);
    LeanplumTestHelper.assertLatchIsCalled(latch4, 1);
    assertFalse(isRunnableQueued(runnable));

    // Test Postponable action & activity not ignored.
    final CountDownLatch latch5 = new CountDownLatch(1);
    runnable = new PostponableAction() {
      @Override
      public void run() {
        latch5.countDown();
      }
    };
    LeanplumActivityHelper.queueActionUponActive(runnable);
    LeanplumTestHelper.assertLatchIsCalled(latch5, 1);
    assertFalse(isRunnableQueued(runnable));

    // Test Postponable action & activity ignored.
    final CountDownLatch latch6 = new CountDownLatch(1);
    runnable = new PostponableAction() {
      @Override
      public void run() {
        latch6.countDown();
      }
    };
    LeanplumActivityHelper.deferMessagesForActivities(FakeActivity.class);
    LeanplumActivityHelper.queueActionUponActive(runnable);
    LeanplumTestHelper.assertLatchIsNotCalled(latch6, 1);
    assertTrue(isRunnableQueued(runnable));
  }

  private static boolean isRunnableQueued(Runnable runnable) throws NoSuchFieldException,
      IllegalAccessException {
    Field field = LeanplumActivityHelper.class.getDeclaredField("pendingActions");
    field.setAccessible(true);
    List pendingActions = (List) field.get(null);
    return pendingActions.contains(runnable);
  }

  @SuppressLint("Registered")
  static class FakeActivity extends Activity {
    boolean isFinishing = true;

    @Override
    public boolean isFinishing() {
      return isFinishing;
    }

    @SuppressWarnings("SameParameterValue")
    public void setFinishing(boolean finishing) {
      isFinishing = finishing;
    }
  }
}
