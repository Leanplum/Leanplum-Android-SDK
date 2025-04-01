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

import static com.leanplum.utils.TestConstants.ROBOLECTRIC_CONFIG_SDK_VERSION;

import com.leanplum.Leanplum;
import com.leanplum.__setup.LeanplumTestApp;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests for {@link LeanplumInternal} class.
 *
 * @author Anna Orlova
 */
@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = ROBOLECTRIC_CONFIG_SDK_VERSION,
    application = LeanplumTestApp.class
)
@PowerMockIgnore({
    "org.mockito.*",
    "org.robolectric.*",
    "org.json.*",
    "org.powermock.*",
    "android.*",
    "jdk.internal.reflect.*"
})
@PrepareForTest({Leanplum.class, LeanplumInternal.class, Constants.class, Socket.class})
public class LeanplumInternalTest {
  @Rule
  public PowerMockRule rule = new PowerMockRule();

  /**
   * Runs before every test case.
   */
  @Before
  public void setUp() {
    spy(LeanplumInternal.class);
  }

  /**
   * Test for {@link LeanplumInternal#moveToForeground()} that should connect to Socket in
   * development mode only after successful connection to Leanplum.
   */
  @Test
  public void testMoveToForeground() throws Exception {
    mockStatic(Constants.class);
    mockStatic(Socket.class);
    Constants.isDevelopmentModeEnabled = true;
    when(Constants.class, "isNoop").thenReturn(false);
    when(LeanplumInternal.class, "hasStarted").thenReturn(true);

    // Test for failed connection to Leanplum.
    when(LeanplumInternal.class, "isStartSuccessful").thenReturn(false);
    LeanplumInternal.moveToForeground();
    verifyStatic(never());
    Socket.connectSocket();

    // Test for successful connection to Leanplum.
    when(LeanplumInternal.class, "isStartSuccessful").thenReturn(true);
    assertTrue(LeanplumInternal.isStartSuccessful());
    Field inForegroundField = LeanplumInternal.class.getDeclaredField("inForeground");
    assertNotNull(inForegroundField);
    inForegroundField.setAccessible(true);
    inForegroundField.set(LeanplumInternal.class, false);

    // Stubbing the recordAttributeChanges method to do nothing when its called.
    PowerMockito.doNothing().when(LeanplumInternal.class);
    LeanplumInternal.recordAttributeChanges();

    // Stubbing the maybePerformActions method to do nothing when its called.
    PowerMockito.doNothing().when(LeanplumInternal.class);
    LeanplumInternal.maybePerformActions(new String[] {"start", "resume"}, null,
        LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL, null, null);

    LeanplumInternal.moveToForeground();
    verifyStatic();
    Socket.connectSocket();
  }
}
