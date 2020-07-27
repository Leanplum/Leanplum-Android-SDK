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

import android.content.Context;

import com.leanplum.Leanplum;
import com.leanplum.__setup.LeanplumTestApp;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.net.URI;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests for {@link SocketIOClient} class.
 *
 * @author Anna Orlova
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
@PrepareForTest({
    Leanplum.class,
    Util.class,
    SocketIOClient.class,
    APIConfig.class
})
public class SocketIOClientTest {
  @Rule
  public PowerMockRule rule = new PowerMockRule();

  /**
   * Runs before every test case.
   */
  @Before
  public void setUp() {
    spy(SocketIOClient.class);
  }

  /**
   * Test for {@link SocketIOClient#userAgentString()} that should return user-agent.
   *
   * @throws Exception
   */
  @Test
  public void testUserAgentString() throws Exception {
    mockStatic(Util.class);
    mockStatic(APIConfig.class);
    spy(Leanplum.class);
    SocketIOClient socketIOClient = new SocketIOClient(new URI(""), new SocketIOClient.Handler() {
      @Override
      public void onConnect() {

      }

      @Override
      public void on(String event, JSONArray arguments) {

      }

      @Override
      public void onDisconnect(int code, String reason) {

      }

      @Override
      public void onError(Exception error) {

      }
    });
    Method userAgentStringMethod = SocketIOClient.class.getDeclaredMethod("userAgentString");
    userAgentStringMethod.setAccessible(true);
    assertNotNull(userAgentStringMethod);
    Constants.LEANPLUM_VERSION = "1";
    Constants.CLIENT = "android";
    APIConfig config = new APIConfig();
    config.setAppId("app_id", "access_key");
    when(APIConfig.class, "getInstance").thenReturn(config);
    when(Util.class, "getVersionName").thenReturn("app_version");
    when(Util.class, "getApplicationName", Matchers.any(Context.class)).thenReturn("app_name");

    // Test with a non-null Context.
    doReturn(RuntimeEnvironment.application).when(Leanplum.class, "getContext");
    assertEquals("app_name/app_version(app_id; android; 1/s)",
        (String) userAgentStringMethod.invoke(socketIOClient));

    // Test with a null Context.
    doReturn(null).when(Leanplum.class, "getContext");
    assertEquals("websocket(app_id; android; 1/s)",
        (String) userAgentStringMethod.invoke(socketIOClient));
  }
}
