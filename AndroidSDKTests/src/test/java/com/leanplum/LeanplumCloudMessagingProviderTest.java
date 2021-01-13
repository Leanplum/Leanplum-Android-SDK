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
package com.leanplum;

import android.app.Application;

import com.leanplum.__setup.LeanplumTestApp;
import com.leanplum.internal.Constants;
import com.leanplum.utils.SharedPreferencesUtil;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Tests for {@link LeanplumCloudMessagingProvider} class.
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
@PrepareForTest({Leanplum.class, LeanplumCloudMessagingProvider.class, SharedPreferencesUtil.class})
public class LeanplumCloudMessagingProviderTest {
  @Rule
  public PowerMockRule rule = new PowerMockRule();

  // The target context of the instrumentation.
  private Application context;

  /**
   * Runs before every test case.
   */
  @Before
  public void setUp() {
    spy(LeanplumCloudMessagingProvider.class);

    this.context = RuntimeEnvironment.application;
    assertNotNull(this.context);
    Leanplum.setApplicationContext(this.context);
  }

  /**
   * Test if registrationId is sent to the server when a new registrationId is received. {@link
   * LeanplumCloudMessagingProvider#setRegistrationId}
   *
   * @throws Exception
   */
  @Test
  public void testOnRegistrationIdReceived() throws Exception {
    mockStatic(Leanplum.class);
    mockStatic(SharedPreferencesUtil.class);
    when(Leanplum.class, "getContext").thenReturn(RuntimeEnvironment.application);

    LeanplumCloudMessagingProvider cloudMessagingProvider = new LeanplumCloudMessagingProvider() {
      @Override
      protected String getSharedPrefsPropertyName() {
        return Constants.Defaults.PROPERTY_FCM_TOKEN_ID;
      }
      @Override
      public void updateRegistrationId() {
      }
      @Override
      public PushProviderType getType() {
        return PushProviderType.FCM;
      }
      @Override
      public void unregister() {
      }
    };
    LeanplumCloudMessagingProvider cloudMessagingProviderMock = PowerMockito.mock(
        LeanplumCloudMessagingProvider.class);
    whenNew(LeanplumCloudMessagingProvider.class).withNoArguments().thenReturn(
        cloudMessagingProviderMock);
    when(SharedPreferencesUtil.class, "getString", context, Constants.Defaults.LEANPLUM_PUSH,
        Constants.Defaults.PROPERTY_FCM_TOKEN_ID).thenReturn("stored_token");
    doNothing().when(cloudMessagingProviderMock).storeRegistrationId(anyString());

    // Test if a token gets send to the backend when no previous token exists.
    cloudMessagingProvider.setRegistrationId("new_token");
    verifyStatic(times(1));
    Leanplum.setRegistrationId(any(PushProviderType.class), eq("new_token"));

    // Test if new token gets send to backend when a previous token exists.
    cloudMessagingProvider.setRegistrationId("new_token1");
    verifyStatic(times(1));
    Leanplum.setRegistrationId(any(PushProviderType.class), eq("new_token1"));

    // Test if a new token is not sent to the backend when a token equals to the stored token.
    cloudMessagingProvider.setRegistrationId("stored_token");
    verifyStatic(never());
    Leanplum.setRegistrationId(any(PushProviderType.class), eq("stored_token"));
  }
}
