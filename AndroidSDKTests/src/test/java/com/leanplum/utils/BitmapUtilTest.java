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
package com.leanplum.utils;

import static com.leanplum.utils.TestConstants.ROBOLECTRIC_CONFIG_SDK_VERSION;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.leanplum.Leanplum;
import com.leanplum.__setup.LeanplumTestApp;
import com.leanplum.__setup.LeanplumTestHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * Tests for {@link BitmapUtil} class.
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
@PrepareForTest({BitmapUtil.class})
public class BitmapUtilTest {
  @Rule
  public PowerMockRule rule = new PowerMockRule();
  // The target context of the instrumentation.
  private Application context;

  /**
   * Runs before every test case.
   */
  @Before
  public void setUp() {
    spy(BitmapUtil.class);

    this.context = RuntimeEnvironment.getApplication();
    assertNotNull(this.context);
    Leanplum.setApplicationContext(this.context);
  }

  /**
   * Test for {@link BitmapUtil#getScaledBitmap(Context, String)} that returns scaled bitmap.
   */
  @Test
  public void getScaledBitmapTest() throws Exception {
    int width = 1000;
    int height = 400;
    File file = LeanplumTestHelper.createFile(context, "test_file.png", Bitmap.createBitmap(width, height,
            Bitmap.Config.ARGB_8888));

    String path = "file://" + file;


    Bitmap scaledBitMap = BitmapUtil.getScaledBitmap(context, path);
    assertNotNull(scaledBitMap);

    assertTrue(scaledBitMap.getWidth() < width);
    assertTrue(scaledBitMap.getHeight() < height);
  }

  /**
   * Test for {@link BitmapUtil#getBitmapFromUrl(String, int, int)} that downloads smaller bitmap.
   */
  @Test
  public void getBitmapFromUrlTest() throws Exception {
    File file = LeanplumTestHelper.createFile(context, "test_file.png", Bitmap.createBitmap(100, 20,
        Bitmap.Config.ARGB_8888));
    BitmapUtil bitmapUtil = new BitmapUtil();
    Method getBitmapFromUrlMethod = BitmapUtil.class.getDeclaredMethod("getBitmapFromUrl",
        String.class, int.class, int.class);
    getBitmapFromUrlMethod.setAccessible(true);

    // Test when bitmap can be downloaded.
    Bitmap bitmap = (Bitmap) getBitmapFromUrlMethod.invoke(bitmapUtil, "file://" +
        file.getAbsolutePath(), 10, 2);
    assertNotNull(getBitmapFromUrlMethod);
    assertNotNull(bitmap);
    assertTrue(bitmap.getWidth() < 100 || bitmap.getHeight() < 20);
    assertTrue(file.delete());

    // Test when bitmap can't be downloaded.
    Bitmap badBitmap = (Bitmap) getBitmapFromUrlMethod.invoke(bitmapUtil, file.getAbsolutePath(),
        10, 2);
    assertNull(badBitmap);
  }

  /**
   * Test for {@link BitmapUtil#calculateInSampleSize(BitmapFactory.Options, int, int)} that
   * calculates a simple size.
   */
  @Test
  public void calculateInSampleSizeTest() throws Exception {
    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.outHeight = 100;
    opts.outWidth = 20;
    assertEquals(100, opts.outHeight);
    assertEquals(20, opts.outWidth);
    BitmapUtil bitmapUtil = new BitmapUtil();
    Method calculateInSampleSizeMethod = BitmapUtil.class.getDeclaredMethod("calculateInSampleSize",
        BitmapFactory.Options.class, int.class, int.class);
    calculateInSampleSizeMethod.setAccessible(true);
    int simpleSize = (int) calculateInSampleSizeMethod.invoke(bitmapUtil, opts, 10, 2);
    assertNotNull(calculateInSampleSizeMethod);
    assertEquals(2, simpleSize);
  }

  /**
   * Test for {@link BitmapUtil#closeStream(InputStream)} that closes InputStream.
   */
  @Test
  public void closeStreamTest() throws Exception {
    File file = LeanplumTestHelper.createFile(context, "test_file.txt", "test");
    assertNotNull(file);
    InputStream inputStream = new FileInputStream(file.getAbsolutePath());
    assertNotNull(inputStream);
    BitmapUtil bitmapUtil = new BitmapUtil();
    Method closeStreamMethod = BitmapUtil.class.getDeclaredMethod("closeStream", InputStream.class);
    closeStreamMethod.setAccessible(true);
    closeStreamMethod.invoke(bitmapUtil, inputStream);
    assertNotNull(closeStreamMethod);
    assertTrue(file.delete());
    try {
      //noinspection ResultOfMethodCallIgnored
      inputStream.read();
    } catch (IOException e) {
      assertEquals("Stream Closed", e.getLocalizedMessage());
    }
  }
}
