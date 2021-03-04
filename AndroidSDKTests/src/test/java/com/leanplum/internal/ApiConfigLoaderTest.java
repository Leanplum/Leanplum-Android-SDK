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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Resources;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = 16
)
public class ApiConfigLoaderTest {
  private String appId;
  private String accessKey;
  private boolean prodEnvironment;
  private boolean devEnvironment;

  private Context context;
  private Resources resources;

  @Before
  public void setup() {
    appId = null;
    accessKey = null;
    prodEnvironment = false;
    devEnvironment = false;

    resources = Mockito.mock(Resources.class);
    context = Mockito.mock(Context.class);
    when(context.getResources()).thenReturn(resources);
  }

  private void mockStringResource(String key, String value) {
    int resourceId = new Random().nextInt();
    when(resources.getIdentifier(eq(key), eq("string"), anyString())).thenReturn(resourceId);
    when(context.getString(resourceId)).thenReturn(value);
  }

  private void callLoadMethod(ApiConfigLoader loader) {
    loader.loadFromResources(
        (appId, prodKey) -> {
          this.appId = appId;
          this.accessKey = prodKey;
          this.prodEnvironment = true;
        },
        (appId, devKey) -> {
          this.appId = appId;
          this.accessKey = devKey;
          this.devEnvironment = true;
        }
    );
  }

  @Test
  public void testProdEnvironment() {
    String wantedAppId = "app_id";
    String wantedAccessKey = "access_key";

    mockStringResource("leanplum_app_id", wantedAppId);
    mockStringResource("leanplum_prod_key", wantedAccessKey);
    mockStringResource("leanplum_environment", "production");

    ApiConfigLoader loader = new ApiConfigLoader(context);
    callLoadMethod(loader);

    assertTrue(prodEnvironment);
    assertEquals(appId, wantedAppId);
    assertEquals(accessKey, wantedAccessKey);
  }

  @Test
  public void testDevEnvironment() {
    String wantedAppId = "app_id";
    String wantedAccessKey = "access_key";

    mockStringResource("leanplum_app_id", wantedAppId);
    mockStringResource("leanplum_dev_key", wantedAccessKey);
    mockStringResource("leanplum_environment", "development");

    ApiConfigLoader loader = new ApiConfigLoader(context);
    callLoadMethod(loader);

    assertTrue(devEnvironment);
    assertEquals(appId, wantedAppId);
    assertEquals(accessKey, wantedAccessKey);
  }

  @Test
  public void testAccessKeyMissing() {
    String wantedAppId = "app_id";

    mockStringResource("leanplum_app_id", wantedAppId);
    mockStringResource("leanplum_environment", "development");

    ApiConfigLoader loader = new ApiConfigLoader(context);
    callLoadMethod(loader);

    assertFalse(prodEnvironment);
    assertFalse(devEnvironment);
  }
}
