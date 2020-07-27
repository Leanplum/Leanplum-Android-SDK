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

import com.leanplum.Leanplum;
import com.leanplum.__setup.AbstractTest;
import com.leanplum.__setup.LeanplumTestHelper;
import com.leanplum._whitebox.utilities.ResponseHelper;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.internal.APIConfig;
import com.leanplum.internal.Registration;
import com.leanplum.internal.Util;
import com.leanplum.tests.R;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.doReturn;

/**
 * Tests the Utilities class.
 *
 * @author Milos Jakovljevic
 */
public class UtilitiesTest extends AbstractTest {
  @Test
  public void testUserIdEncoding() {
    String userId = "my юзър 123";

    ResponseHelper.seedResponse("/responses/simple_start_response.json");

    // Start with user id.
    Leanplum.start(mContext, userId);
    assertTrue(Leanplum.hasStarted());

    LeanplumTestHelper.reset();

    // Start without user id and verify that it is properly encoded/decoded.
    Leanplum.start(mContext);
    assertEquals(userId, APIConfig.getInstance().userId());
  }

  @Test
  public void testDeviceIdEncoding() {
    ResponseHelper.seedResponse("/responses/simple_start_response.json");

    Leanplum.start(mContext);

    // Validate device id.
    String deviceId = APIConfig.getInstance().deviceId();
    assertNotNull(deviceId);

    LeanplumTestHelper.reset();

    // Start again and verify device id.
    Leanplum.start(mContext);
    assertEquals(deviceId, APIConfig.getInstance().deviceId());
  }

  @Test
  public void mockGetDeviceName() throws Exception {
    setupSDK(mContext, "/responses/simple_start_response.json");

    assertEquals("Unknown robolectric", Util.getDeviceName());
    doReturn("DeviceName").when(Util.class, "getDeviceName");
    assertEquals("DeviceName", Util.getDeviceName());
  }

  @Test
  public void mockIsConnected() throws Exception {
    setupSDK(mContext, "/responses/simple_start_response.json");

    assertTrue(Util.isConnected());
    doReturn(false).when(Util.class, "isConnected");
    assertFalse(Util.isConnected());
  }

  @Test
  public void mockIsValidDeviceId() throws Exception {
    setupSDK(mContext, "/responses/simple_start_response.json");

    assertFalse(Util.isValidDeviceId(""));
    doReturn(true).when(Util.class, "isValidDeviceId", "");
    assertTrue(Util.isValidDeviceId(""));
  }

  @Test
  public void testUtilGetDeviceName() throws Exception {
    setupSDK(mContext, "/responses/simple_start_response.json");

    assertEquals("Unknown robolectric", Util.getDeviceName());
    doReturn("DeviceName").when(Util.class, "getDeviceName");
    assertEquals("DeviceName", Util.getDeviceName());
  }

  @Test
  public void testRegistration() throws Exception {
    setupSDK(mContext, "/responses/simple_start_response.json");

    Registration.registerDevice("test@test.con", new StartCallback() {
      @Override
      public void onResponse(boolean success) {
        assertTrue(success);
      }
    });
  }

  @Test
  public void testResourceNameGeneration() throws Exception {
    int invalidResourceId = 0;
    String invalidName = Util.generateResourceNameFromId(invalidResourceId);
    assertNull(invalidName);

    int validResourceId = R.drawable.leanplum_watermark;
    String validName = Util.generateResourceNameFromId(validResourceId);
    assertNotNull(validName);
    assertEquals("drawable/leanplum_watermark.jpg", validName);
  }

  @Test
  public void testResourceIdGeneration() throws Exception {
    String invalidName = "test.png";
    int invalidResourceId = Util.generateIdFromResourceName(invalidName);
    assertEquals(0, invalidResourceId);

    String validName = "drawable/leanplum_watermark.jpg";
    int validResourceId = Util.generateIdFromResourceName(validName);
    assertEquals(R.drawable.leanplum_watermark, validResourceId);

    // Generated name can be without extension.
    String validNameWithoutExtension = "drawable/leanplum_watermark";
    validResourceId = Util.generateIdFromResourceName(validNameWithoutExtension);
    assertEquals(R.drawable.leanplum_watermark, validResourceId);
  }
}
