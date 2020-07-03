/*
 * Copyright 2019, Leanplum, Inc. All rights reserved.
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

import com.leanplum.Leanplum;
import com.leanplum.__setup.LeanplumTestApp;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * Tests for {@link WebSocketClient} class.
 *
 * @author Grace Gu
 */
@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = 16,
    application = LeanplumTestApp.class
)
@PowerMockIgnore({
    "org.mockito.*",
    "org.robolectric.*",
    "org.json.*",
    "org.powermock.*",
    "android.*"
})
@PrepareForTest({Leanplum.class, Util.class, WebSocketClient.class, Request.class})
public class WebSocketClientTest {
  @Rule
  public PowerMockRule rule = new PowerMockRule();

  /**
   * Runs before every test case.
   */
  @Before
  public void setUp() {
    spy(WebSocketClient.class);
  }

  // java doesn't support wss so we expect WebSocketClient to respect http vs https
  @Test
  public void testIsSecure() throws URISyntaxException {
    WebSocketClient webSocketClient = new WebSocketClient(new URI("https://dev.leanplum.com"), null, null);
    assertTrue(webSocketClient.isSecure());
    assertTrue(true);
  }

  @Test
  public void testIsNotSecure() throws URISyntaxException {
    WebSocketClient webSocketClient = new WebSocketClient(new URI("http://dev.leanplum.com"), null, null);
    assertFalse(webSocketClient.isSecure());
  }
}
