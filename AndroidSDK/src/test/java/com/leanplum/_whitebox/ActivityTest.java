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
package com.leanplum._whitebox;

import android.content.res.Resources;

import com.leanplum.__setup.AbstractTest;
import com.leanplum.activities.LeanplumAccountAuthenticatorActivity;
import com.leanplum.activities.LeanplumActivityGroup;
import com.leanplum.activities.LeanplumExpandableListActivity;
import com.leanplum.activities.LeanplumLauncherActivity;
import com.leanplum.activities.LeanplumListActivity;
import com.leanplum.activities.LeanplumPreferenceActivity;
import com.leanplum.activities.LeanplumTabActivity;
import com.leanplum.activities.LeanplumTestActivity;
import com.leanplum.activities.LeanplumTestFragmentActivity;

import org.junit.Test;
import org.robolectric.Robolectric;

import static org.junit.Assert.assertNotNull;

/**
 * Tests the creation of test activities.
 *
 * @author Milos Jakovljevic
 */
public class ActivityTest extends AbstractTest {
  @Override
  public void after() {
    // Do nothing.
  }

  @Test
  public void testLeanplumAccountAuthenticatorActivity() throws Exception {
    LeanplumAccountAuthenticatorActivity activity = Robolectric
        .buildActivity(LeanplumAccountAuthenticatorActivity.class)
        .create().start().resume().pause().stop().visible().get();
    setActivityVisibility(activity);

    Resources resources = activity.getResources();
    assertNotNull(resources);

    resetViews(activity);
  }

  @Test
  public void testLeanplumActivity() throws Exception {
    LeanplumTestActivity activity = Robolectric.buildActivity(LeanplumTestActivity.class).
        create().start().resume().pause().stop().visible().get();
    setActivityVisibility(activity);

    Resources resources = activity.getResources();
    assertNotNull(resources);

    resetViews(activity);
  }


  @Test
  public void testLeanplumActivityGroup() throws Exception {
    LeanplumActivityGroup activity = Robolectric.buildActivity(LeanplumActivityGroup.class).
        create().start().resume().pause().stop().visible().get();
    setActivityVisibility(activity);

    Resources resources = activity.getResources();
    assertNotNull(resources);

    resetViews(activity);
  }

  @Test
  public void testLeanplumExpandableListActivity() throws Exception {
    LeanplumExpandableListActivity activity = Robolectric
        .buildActivity(LeanplumExpandableListActivity.class).
            create().start().resume().pause().stop().visible().get();
    setActivityVisibility(activity);

    Resources resources = activity.getResources();
    assertNotNull(resources);

    resetViews(activity);
  }

  @Test
  public void testLeanplumFragmentActivity() throws Exception {
    LeanplumTestFragmentActivity activity = Robolectric
        .buildActivity(LeanplumTestFragmentActivity.class).
            create().start().resume().pause().stop().visible().get();
    setActivityVisibility(activity);

    Resources resources = activity.getResources();
    assertNotNull(resources);

    resetViews(activity);
  }

  @Test
  public void testLeanplumLauncherActivity() throws Exception {
    LeanplumLauncherActivity activity = Robolectric
        .buildActivity(LeanplumLauncherActivity.class).
            create().start().resume().pause().stop().visible().get();
    setActivityVisibility(activity);

    Resources resources = activity.getResources();
    assertNotNull(resources);

    resetViews(activity);
  }

  @Test
  public void testLeanplumListActivity() throws Exception {
    LeanplumListActivity activity = Robolectric.buildActivity(LeanplumListActivity.class).
        create().start().resume().pause().stop().visible().get();
    setActivityVisibility(activity);

    Resources resources = activity.getResources();
    assertNotNull(resources);

    resetViews(activity);
  }

  @Test
  public void testLeanplumPreferenceActivity() throws Exception {
    LeanplumPreferenceActivity activity = Robolectric
        .buildActivity(LeanplumPreferenceActivity.class).
            create().start().resume().pause().stop().visible().get();
    setActivityVisibility(activity);

    Resources resources = activity.getResources();
    assertNotNull(resources);

    resetViews(activity);
  }

  @Test
  public void testLeanplumTabActivity() throws Exception {
    LeanplumTabActivity activity = Robolectric.buildActivity(LeanplumTabActivity.class).
        create().start().resume().pause().stop().visible().get();
    setActivityVisibility(activity);

    Resources resources = activity.getResources();
    assertNotNull(resources);

    resetViews(activity);
  }
}
