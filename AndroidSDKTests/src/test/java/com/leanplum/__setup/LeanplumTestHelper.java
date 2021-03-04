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
package com.leanplum.__setup;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumDeviceIdMode;
import com.leanplum.LeanplumInbox;
import com.leanplum.Var;
import com.leanplum._whitebox.utilities.RequestHelper;
import com.leanplum._whitebox.utilities.ImmediateRequestSender;
import com.leanplum.internal.APIConfig;
import com.leanplum.internal.ActionManager;
import com.leanplum.internal.LeanplumInternal;
import com.leanplum.internal.Log;
import com.leanplum.internal.Request;
import com.leanplum.internal.Request.RequestType;
import com.leanplum.internal.RequestFactory;
import com.leanplum.internal.RequestSender;
import com.leanplum.internal.VarCache;
import com.leanplum.tests.BuildConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Utility functions for SDK unit testing.
 *
 * @author Kiril Kafadarov, Aleksandar Gyorev
 */
@SuppressWarnings("SameParameterValue")
public class LeanplumTestHelper {
  private static final String APP_ID = "app_LPmDHFQkw7g3bGDEbu32rDDQFsKeqEHIFMv8cE2p2Us";
  private static final String PRODUCTION_KEY = "prod_WJonYGclmjGimzjLp9CDJBLaor2J2ntwXfwvTl2BMKo";
  private static final String DEVELOPMENT_KEY = "dev_ybNfdmBGKriHjikEq6dBJ0lVLQXLwCyVWATSisBifrU";

  private static final String API_HOST_NAME = "www.leanplum.com";
  private static final Boolean API_SSL = true;

  private static final String SOCKET_HOST_NAME = "dev.leanplum.com";
  private static final Integer SOCKET_PORT = 443;
  public static final LeanplumDeviceIdMode DEVICE_ID_MODE = LeanplumDeviceIdMode.MD5_MAC_ADDRESS;

  /**
   * Sets up Leanplum for a single unit test.
   */
  public static void setUp() {
    RequestFactory.defaultFactory = new RequestFactory() {
      @Override
      public Request createRequest(
          String httpMethod,
          String apiMethod,
          RequestType type,
          Map<String, Object> params) {
        return new RequestHelper(httpMethod, apiMethod, type, params);
      }
    };
    RequestSender.setInstance(new ImmediateRequestSender());

    if (BuildConfig.DEBUG) {
      Leanplum.setAppIdForDevelopmentMode(APP_ID, DEVELOPMENT_KEY);
    } else {
      Leanplum.setAppIdForProductionMode(APP_ID, PRODUCTION_KEY);
    }
    Leanplum.setDeviceId("leanplum-unit-test-20527411-BF1E-4E84-91AE-2E98CBCF30AF");
    Leanplum.setApiConnectionSettings(API_HOST_NAME, "api", API_SSL);
    Leanplum.setSocketConnectionSettings(SOCKET_HOST_NAME, SOCKET_PORT);
    Leanplum.setLogLevel(Log.Level.DEBUG);
  }

  /**
   * Cleans up after a test has been executes and resets Leanplum related data.
   */
  public static void tearDown() {
    reset();
    clear();
    APIConfig.getInstance().setAppId(null, null);
    APIConfig.getInstance().setDeviceId(null);
    APIConfig.getInstance().setToken(null);
    APIConfig.getInstance().setUserId(null);
    Leanplum.setApplicationContext(null);
  }

  /**
   * Resets all data related to the currently running Leanplum getInstance.
   */
  public static void reset() {
    LeanplumInternal.setCalledStart(false);
    LeanplumInternal.setHasStarted(false);
    TestClassUtil.setField(LeanplumInternal.class, "issuedStart", false);
    TestClassUtil.setField(LeanplumInternal.class, "hasStartedAndRegisteredAsDeveloper", false);
    LeanplumInternal.setStartSuccessful(false);
    List startHandlersField = (List) TestClassUtil.getField(Leanplum.class, "startHandlers");
    startHandlersField.clear();
    List list = (List) TestClassUtil.getField(LeanplumInternal.class, "startIssuedHandlers");
    list.clear();

    List variablesChangedHandlers = (List) TestClassUtil.getField(Leanplum.class,
        "variablesChangedHandlers");
    variablesChangedHandlers.clear();
    List noDownloadsHandlers = (List) TestClassUtil.getField(Leanplum.class, "noDownloadsHandlers");
    noDownloadsHandlers.clear();
    List onceNoDownloadsHandlers = (List) TestClassUtil.getField(Leanplum.class,
        "onceNoDownloadsHandlers");
    onceNoDownloadsHandlers.clear();
    List messageDisplayedHandlers =
        (List) TestClassUtil.getField(Leanplum.class, "messageDisplayedHandlers");
    messageDisplayedHandlers.clear();

    LeanplumInternal.getActionHandlers().clear();
    LeanplumInternal.getUserAttributeChanges().clear();
    Leanplum.countAggregator().getAndClearCounts();

    TestClassUtil.setField(Leanplum.class, "registerDeviceHandler", null);
    TestClassUtil.setField(Leanplum.class, "registerDeviceFinishedHandler", null);
    LeanplumInternal.setIsPaused(false);
    TestClassUtil.setField(Leanplum.class, "deviceIdMode", LeanplumDeviceIdMode.MD5_MAC_ADDRESS);
    TestClassUtil.setField(Leanplum.class, "customDeviceId", null);
    TestClassUtil.setField(Leanplum.class, "userSpecifiedDeviceId", false);
    LeanplumInternal.setStartedInBackground(false);
    TestClassUtil.setField(LeanplumInternal.class, "inForeground", false);

    TestClassUtil.setField(Leanplum.class, "context", null);
    TestClassUtil.setField(Leanplum.class, "pushStartCallback", null);

    LeanplumInbox newsfeed = (LeanplumInbox) TestClassUtil.getField(LeanplumInbox.class,
        "instance");
    TestClassUtil.setField(LeanplumInbox.class, newsfeed, "unreadCount", 0);
    Map messages = (Map) TestClassUtil.getField(LeanplumInbox.class, newsfeed, "messages");
    messages.clear();
    List newsfeedChangedHandlers = (List) TestClassUtil.getField(LeanplumInbox.class, newsfeed,
        "changedCallbacks");
    newsfeedChangedHandlers.clear();
    TestClassUtil.setField(LeanplumInbox.class, newsfeed, "didLoad", false);

    VarCache.reset();
    // Reset the map values in ActionManager.
    TestClassUtil.setField(ActionManager.getInstance(), "messageImpressionOccurrences", new HashMap<>());
    TestClassUtil.setField(ActionManager.getInstance(), "messageTriggerOccurrences", new HashMap<>());
    TestClassUtil.setField(ActionManager.getInstance(), "sessionOccurrences", new HashMap<>());
  }

  /**
   * Clears all Leanplum related saved data on the device.
   */
  private static void clear() {
    Context context = Leanplum.getContext();
    if (context != null) {
      SharedPreferences.Editor editor = context.getSharedPreferences(
          "__leanplum__", Context.MODE_PRIVATE).edit();
      if (editor != null) {
        editor.clear();
        editor.apply();
      }

      editor = context.getSharedPreferences("__leanplum_push__", Context.MODE_PRIVATE).edit();
      if (editor != null) {
        editor.clear();
        editor.apply();
      }
    }
  }

  public static File createFile(Context context, String filename, String data) {
    File file = new File(context.getFilesDir(), filename);
    try {
      FileOutputStream outStream = new FileOutputStream(file);
      outStream.write(data.getBytes());
      outStream.close();
    } catch (IOException e) {
      fail("Could not create test file.");
    }
    return file;
  }

  public static void createZipFile(Context context, String filename, String data) {
    String packagePath = context.getPackageResourcePath();
    File file = new File(packagePath);
    try {
      ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
      ZipEntry e = new ZipEntry(filename);
      out.putNextEntry(e);

      byte[] byteData = data.getBytes();
      out.write(byteData, 0, byteData.length);
      out.closeEntry();

      out.close();
    } catch (IOException e) {
      fail("Could not create zip file.");
    }
  }

  public static String readFile(Var<String> file) {
    InputStream inputStream = file.stream();
    StringBuilder builder = new StringBuilder();
    Reader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF8")));
    int c;
    try {
      while ((c = reader.read()) != -1) {
        builder.append((char) c);
      }
    } catch (IOException e) {
      fail("Could not read file.");
    }
    return builder.toString();
  }

  public static File createFile(Context context, String filename, Bitmap bitmap) {
    File file = new File(context.getFilesDir(), filename);
    try {
      FileOutputStream outStream = new FileOutputStream(file);
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
      outStream.close();
    } catch (IOException e) {
      fail("Could not create test file.");
    }
    return file;
  }

  public static void assertLatchIsCalled(CountDownLatch latch, long waitTime) {
    try {
      assertTrue(latch.await(waitTime, TimeUnit.SECONDS));
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException("CountDownLatch interrupted while waiting for response.");
    }
  }

  public static void assertLatchIsNotCalled(CountDownLatch latch, long waitTime) {
    try {
      assertFalse(latch.await(waitTime, TimeUnit.SECONDS));
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException("CountDownLatch interrupted while waiting for response.");
    }
  }
}
