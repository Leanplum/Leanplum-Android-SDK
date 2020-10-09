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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationServices;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LocationManager;
import com.leanplum._whitebox.utilities.RequestHelper;
import com.leanplum._whitebox.utilities.ResponseHelper;
import com.leanplum.internal.Constants;
import com.leanplum.internal.LeanplumEventDataManager;
import com.leanplum.internal.LeanplumInternal;
import com.leanplum.internal.Log;
import com.leanplum.internal.OperationQueue;
import com.leanplum.internal.RequestBuilder;
import com.leanplum.internal.Request;
import com.leanplum.internal.ShadowOperationQueue;
import com.leanplum.internal.Util;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = 16,
    application = LeanplumTestApp.class,
    packageName = "com.leanplum.tests",
    shadows = {
        ShadowLooper.class,
    }
)
@PowerMockIgnore({
    "org.mockito.*",
    "org.robolectric.*",
    "org.json.*",
    "org.powermock.*",
    "android.*",
    "javax.net.ssl.*",
    "javax.xml.*",
    "org.xml.sax.*",
    "org.w3c.dom.*"
})
@PrepareForTest(value = {
    Leanplum.class,
    LeanplumActivityHelper.class,
    URL.class,
    LocationManager.class,
    LocationServices.class,
    FusedLocationProviderApi.class,
}, fullyQualifiedNames = {"com.leanplum.internal.*"})
/**
 * AbstractTest class which holds methods to properly setup test environment.
 * @author Milos Jakovljevic
 */
public abstract class AbstractTest {
  @Rule
  public PowerMockRule rule = new PowerMockRule();

  // The target context of the instrumentation.
  protected Application mContext;

  @SuppressWarnings("WeakerAccess")
  @Before
  public void before() throws Exception {
    spy(Log.class);
    spy(Util.class);
    spy(LeanplumEventDataManager.class);
    spy(Leanplum.class);
    spy(LeanplumActivityHelper.class);
    spy(OperationQueue.class);

    ReflectionHelpers.setStaticField(LeanplumEventDataManager.class, "instance", null);
    // Get and set application context.
    mContext = RuntimeEnvironment.application;
    Leanplum.setApplicationContext(mContext);

    // Display logs in console.
    ShadowLog.stream = System.out;

    // We are always connected.
    doReturn(true).when(Util.class, "isConnected");
    assertTrue(Util.isConnected());

    doReturn(mContext).when(Leanplum.class, "getContext");
    assertNotNull(Leanplum.getContext());

    // Setup the sdk.
    LeanplumTestHelper.setUp();

    // To be able to run tests offline and not depend on a server we have to mock URLConnection to
    // return proper status code.
    prepareHttpsURLConnection(200);

    stopLeanplumExceptionHandling();

    ShadowOperationQueue shadowOperationQueue = new ShadowOperationQueue();

    Field instance = OperationQueue.class.getDeclaredField("instance");
    instance.setAccessible(true);
    instance.set(instance, shadowOperationQueue);
  }

  /**
   * Leanplum SDK is handling the uncaught exceptions in
   * {@link Log#exception(Throwable)} but for test purposes uncaught exceptions
   * need not to be caught. In a lot of tests there are assert statements in the callbacks that are
   * added in the SDK.
   */
  protected void stopLeanplumExceptionHandling() throws Exception {
    String message = "\n" + "com.leanplum.internal.Log.exception(Throwable) is called and "
        + "exception parameter is rethrown intentionally." + "\n"
        + "Call AbstractTest.resumeLeanplumExceptionHandling() to allow "
        + "Log.exception(Throwable) to work normally." + "\n" + "\n"
        + "Scroll down to see the original stacktrace.";

    PowerMockito.doAnswer(invocation -> {
      Object[] args = invocation.getArguments();
      throw new Exception(message, (Throwable) args[0]);
    }).when(Log.class, "exception", any(Throwable.class));
  }

  /**
   * Use this method to resume normal behaviour for
   * {@link Log#exception(java.lang.Throwable)} and catch all uncaught exceptions in SDK.
   */
  protected void resumeLeanplumExceptionHandling() throws Exception {
    PowerMockito.doNothing().when(Log.class, "exception", any(Throwable.class));
  }

  protected void prepareHttpsURLConnection(int responseCode) throws Exception {
    prepareHttpsURLConnection(responseCode, "/responses/simple_start_response.json", null, false);
  }

  protected void prepareHttpsURLConnection(
      int responseCode,
      String inputStreamJsonFile,
      String errorStreamJsonFile,
      boolean gzip) throws Exception {

    // Mock url connection to work offline.
    URL mockedURL = mock(URL.class);
    HttpsURLConnection httpsURLConnection = mock(HttpsURLConnection.class);

    whenNew(URL.class).withParameterTypes(String.class).withArguments(anyString())
        .thenReturn(mockedURL);
    when(mockedURL.openConnection()).thenReturn(httpsURLConnection);
    when(httpsURLConnection.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    when(httpsURLConnection.getResponseCode()).thenReturn(responseCode);
    if (gzip) {
      when(httpsURLConnection.getHeaderField("content-encoding"))
          .thenReturn(Constants.LEANPLUM_SUPPORTED_ENCODING);
    }
    // We are just seeding a random file as a InputStream of a mocked httpConnection which will be
    // used in FileManager tests other tests depends on Util.getResponse() to mock response.
    if (inputStreamJsonFile != null) {
      when(httpsURLConnection.getInputStream())
          .thenReturn(ResponseHelper.seedInputStream(inputStreamJsonFile));
    }
    if (errorStreamJsonFile != null) {
      when(httpsURLConnection.getErrorStream())
          .thenReturn(ResponseHelper.seedInputStream(errorStreamJsonFile));
    }
  }

  @After
  public void after() {
    LeanplumTestHelper.tearDown();
  }

  /**
   * Utility method to quickly start the SDK.
   *
   * @param context Surrounding context.
   * @param responseFile Response file to seed to httpConnection.
   */
  protected void setupSDK(Context context, String responseFile) {
    ResponseHelper.seedResponse(responseFile);

    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_START, apiMethod);
      }
    });

    Leanplum.start(context, null, null, null);
    assertTrue(Leanplum.hasStarted());
  }

  /**
   * Due to nature of robolectric we have to manually set App visibility to be able to traverse view
   * tree and find all the views.
   *
   * @param activity Activity to change.
   * @throws Exception
   */
  protected void setActivityVisibility(Activity activity) throws Exception {

    Object globalWindowManager;
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
      globalWindowManager = TestClassUtil.getFieldValueRecursivily("mWindowManager", activity.getWindowManager());
    } else {
      globalWindowManager = TestClassUtil.getFieldValueRecursivily("mGlobal", activity.getWindowManager());
    }

    Object rootObjects = TestClassUtil.getField(globalWindowManager, "mRoots");
    Object[] roots;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && rootObjects != null) {
      roots = ((List) rootObjects).toArray();
    } else {
      roots = (Object[]) rootObjects;
    }
    if (roots == null) {
      return;
    }

    for (Object view : roots) {
      setField(view, "mAppVisible", true);
    }
  }

  /**
   * Resets GlobalWindowManager which removes all views, roots and params. This is needed because
   * GlobalViewManager holds all activities created in tests, which causes our tests to fail.
   *
   * @param activity Activity needed to access windowManager.
   * @throws Exception
   */
  protected void resetViews(Activity activity) throws Exception {
    Object globalWindowManager;
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
      globalWindowManager = TestClassUtil.getFieldValueRecursivily("mWindowManager", activity.getWindowManager());
      setField(globalWindowManager, "mViews", null);
      setField(globalWindowManager, "mRoots", null);
      setField(globalWindowManager, "mParams", null);
    } else {
      globalWindowManager = TestClassUtil.getFieldValueRecursivily("mGlobal", activity.getWindowManager());
      setField(globalWindowManager, "mViews", new ArrayList<View>());
      setField(globalWindowManager, "mRoots", new ArrayList<View>());
      setField(globalWindowManager, "mParams", new ArrayList<WindowManager.LayoutParams>());
    }
  }

  /**
   * Utility method to set a private field value using reflection.
   *
   * @param object Object on which we are setting the value.
   * @param fieldName Name of the field we want to set.
   * @param fieldValue Value we want to set.
   * @throws Exception If field is not found.
   */
  private void setField(Object object, String fieldName, Object fieldValue) throws Exception {
    Class<?> clazz = object.getClass();
    if (clazz != null) {
      Field field = clazz.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(object, fieldValue);
    }
  }
}
