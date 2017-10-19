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
package com.leanplum;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.leanplum.__setup.LeanplumTestApp;
import com.leanplum.__setup.LeanplumTestRunner;
import com.leanplum.__setup.TestClassUtil;
import com.leanplum.internal.CollectionUtil;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Request;
import com.leanplum.internal.Util;
import com.leanplum.tests.MainActivity;
import com.leanplum.utils.SharedPreferencesUtil;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Queue;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.robolectric.Shadows.shadowOf;

/**
 * Tests for {@link LeanplumPushService} class.
 *
 * @author Anna Orlova
 */
@RunWith(LeanplumTestRunner.class)
@Config(
    constants = BuildConfig.class,
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
@PrepareForTest({LeanplumPushService.class, LeanplumFcmProvider.class, LeanplumGcmProvider.class,
    SharedPreferencesUtil.class, Util.class})
public class LeanplumPushServiceTest {
  @Rule
  public PowerMockRule rule = new PowerMockRule();

  // The target context of the instrumentation.
  private Application context;

  /**
   * Runs before every test case.
   */
  @Before
  public void setUp() {
    mockStatic(LeanplumPushService.class);
    spy(LeanplumPushService.class);
    spy(Util.class);
    spy(LeanplumGcmProvider.class);

    this.context = RuntimeEnvironment.application;
    assertNotNull(this.context);
    Leanplum.setApplicationContext(this.context);
  }

  /**
   * Test for {@link LeanplumPushService#initPushService} that should start {@link
   * LeanplumPushService#registerInBackground}.
   *
   * @throws Exception
   */
  @Test
  public void testInitPushService() throws Exception {
    // Mock for LeanplumFcmProvider.
    LeanplumFcmProvider fcmProviderMock = spy(new LeanplumFcmProvider());
    whenNew(LeanplumFcmProvider.class).withNoArguments().thenReturn(fcmProviderMock);
    // Mock for LeanplumGcmProvider.
    LeanplumGcmProvider gcmProviderMock = spy(new LeanplumGcmProvider());
    whenNew(LeanplumGcmProvider.class).withNoArguments().thenReturn(gcmProviderMock);

    Request.setAppId("1", "1");
    when(LeanplumPushService.class, "hasAppIDChanged", "1").thenReturn(false);

    LeanplumPushService pushService = new LeanplumPushService();
    Method initPushServiceMethod = LeanplumPushService.class.
        getDeclaredMethod("initPushService");
    initPushServiceMethod.setAccessible(true);
    when(LeanplumPushService.class, "enableFcmServices").thenReturn(true);
    when(LeanplumPushService.class, "enableGcmServices").thenReturn(true);

    // Tests for Firebase.
    when(LeanplumPushService.isFirebaseEnabled()).thenReturn(true);

    // Test if Manifest is not set up and provider is initialized.
    doReturn(false).when(fcmProviderMock).isManifestSetup();
    doReturn(true).when(fcmProviderMock).isInitialized();
    initPushServiceMethod.invoke(pushService);
    assertNotNull(initPushServiceMethod);
    verifyPrivate(LeanplumPushService.class, times(0)).invoke("registerInBackground");

    // Test if Manifest is set up and provider is initialized.
    doReturn(true).when(fcmProviderMock).isManifestSetup();
    doReturn(true).when(fcmProviderMock).isInitialized();
    initPushServiceMethod.invoke(pushService);
    assertNotNull(initPushServiceMethod);
    verifyPrivate(LeanplumPushService.class, times(1)).invoke("registerInBackground");

    // Tests for GCM.
    when(LeanplumPushService.isFirebaseEnabled()).thenReturn(false);

    // Test if Manifest is not set up and provider is initialized.
    doReturn(false).when(gcmProviderMock).isManifestSetup();
    doReturn(true).when(gcmProviderMock).isInitialized();
    initPushServiceMethod.invoke(pushService);
    assertNotNull(initPushServiceMethod);
    verifyPrivate(LeanplumPushService.class, times(1)).invoke("registerInBackground");

    // Test if Manifest is set up and provider not initialized.
    doReturn(true).when(gcmProviderMock).isManifestSetup();
    doReturn(false).when(gcmProviderMock).isInitialized();
    initPushServiceMethod.invoke(pushService);
    assertNotNull(initPushServiceMethod);
    verifyPrivate(LeanplumPushService.class, times(1)).invoke("registerInBackground");

    // Test if Manifest is set up and provider is initialized.
    doReturn(true).when(gcmProviderMock).isManifestSetup();
    doReturn(true).when(gcmProviderMock).isInitialized();
    initPushServiceMethod.invoke(pushService);
    assertNotNull(initPushServiceMethod);
    verifyPrivate(LeanplumPushService.class, times(2)).invoke("registerInBackground");
  }

  /**
   * Test for {@link LeanplumPushService#hasAppIDChanged(String)} that should return true if
   * application id was stored before and doesn't equal to current.
   *
   * @throws Exception
   */
  @Test
  public void testHasAppIDChanged() throws Exception {
    mockStatic(SharedPreferencesUtil.class);
    Request.setAppId("1", "1");
    doNothing().when(SharedPreferencesUtil.class, "setString", context,
        Constants.Defaults.LEANPLUM_PUSH, Constants.Defaults.APP_ID, "1");
    LeanplumPushService pushService = new LeanplumPushService();
    Method hasAppIDChangedMethod = LeanplumPushService.class.
        getDeclaredMethod("hasAppIDChanged", String.class);
    hasAppIDChangedMethod.setAccessible(true);

    // Test for application id was not stored before.
    when(SharedPreferencesUtil.class, "getString", context, Constants.Defaults.LEANPLUM_PUSH,
        Constants.Defaults.APP_ID).thenReturn("");
    Object result = hasAppIDChangedMethod.invoke(pushService, "1");
    assertNotNull(hasAppIDChangedMethod);
    assertTrue(!(Boolean) result);

    // Test for application id was stored before and doesn't equal to current.
    when(SharedPreferencesUtil.class, "getString", context, Constants.Defaults.LEANPLUM_PUSH,
        Constants.Defaults.APP_ID).thenReturn("2");
    result = hasAppIDChangedMethod.invoke(pushService, "1");
    assertNotNull(hasAppIDChangedMethod);
    assertTrue((Boolean) result);

    // Test for application id was stored before and equal to current.
    when(SharedPreferencesUtil.class, "getString", context, Constants.Defaults.LEANPLUM_PUSH,
        Constants.Defaults.APP_ID).thenReturn("1");
    result = hasAppIDChangedMethod.invoke(pushService, "1");
    assertNotNull(hasAppIDChangedMethod);
    assertTrue(!(Boolean) result);
  }

  /**
   * Test for {@link LeanplumPushService#getDeepLinkIntent(Bundle)} that should return Intent from
   * Push Notification Bundle
   *
   * @throws Exception
   */
  @Test
  public void getDeepLinkIntentTest() throws Exception {
    LeanplumPushService pushService = new LeanplumPushService();
    Method getDeepLinkIntentMethod = LeanplumPushService.class.
        getDeclaredMethod("getDeepLinkIntent", Bundle.class);
    getDeepLinkIntentMethod.setAccessible(true);

    Bundle b = new Bundle();
    Object result = getDeepLinkIntentMethod.invoke(pushService, b);
    assertNull(result);

    String openUrlAction = "{\"__name__\":\"Open URL\",\"URL\":\"https://www.google.com/\"}";
    b.putString(Constants.Keys.PUSH_MESSAGE_ACTION, openUrlAction);
    result = getDeepLinkIntentMethod.invoke(pushService, b);
    assertNotNull(result);
    Intent deepLinkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/"));
    deepLinkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    assertEquals(deepLinkIntent.toString().trim(), result.toString().trim());
  }


  /**
   * Test for {@link LeanplumPushService#activityHasIntent(Context, Intent)}  that should return
   * false if there is no activity that can handle intent.
   *
   * @throws Exception
   */
  @Test
  public void activityHasIntentTest() throws Exception {
    LeanplumPushService pushService = new LeanplumPushService();
    Method activityHasIntentMethod = LeanplumPushService.class.
        getDeclaredMethod("activityHasIntent", Context.class, Intent.class);
    activityHasIntentMethod.setAccessible(true);
    Intent deepLinkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/"));
    deepLinkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    Boolean result = (Boolean) activityHasIntentMethod.invoke(pushService, context, deepLinkIntent);
    assertTrue(!result);
  }

  @Test
  public void testGcmSenderId() {
    LeanplumPushService.setGcmSenderId("sender_id");
    assertNotNull(TestClassUtil.getField(LeanplumGcmProvider.class, "senderIds"));

    LeanplumPushService.setGcmSenderIds("sender_id_1", "sender_id_2");
    assertNotNull(TestClassUtil.getField(LeanplumGcmProvider.class, "senderIds"));
  }

  @Test
  public void testHandleNotification() {
    Bundle bundle = new Bundle();
    bundle.putString("_lpm", "message_id");
    bundle.putString("title", "title_string");
    bundle.putString(Constants.Keys.PUSH_MESSAGE_TEXT, "message_text");
    LeanplumPushService.handleNotification(RuntimeEnvironment.application.getApplicationContext(), bundle);

    NotificationManager notificationManager = (NotificationManager)
        RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
    assertEquals(1, shadowOf(notificationManager).size());
  }

  @Test
  public void testOpenNotification() {
    Context mock = mock(Context.class);
    Leanplum.setApplicationContext(mock);

    Bundle bundle = new Bundle();
    bundle.putString("_lpm", "message_id");
    bundle.putString("_lpx", "{ __Push Notification: \"message\" }");
    bundle.putString("title", "title_string");
    bundle.putString(Constants.Keys.PUSH_MESSAGE_TEXT, "message_text");

    Intent intent = new Intent(mock, MainActivity.class);
    intent.putExtras(bundle);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    LeanplumPushService.setDefaultCallbackClass(MainActivity.class);
    LeanplumPushService.openNotification(mock, intent);
    verify(mock, times(1)).startActivity(any(Intent.class));

    Queue<Runnable> handlers = CollectionUtil.uncheckedCast(
        TestClassUtil.getField(LeanplumActivityHelper.class, "pendingActions"));

    for (Runnable handler : handlers) {
      handler.run();
    }
  }

  @Test
  public void testParseNotificationBundle() {
    Bundle bundle = new Bundle();
    bundle.putString(Constants.Keys.PUSH_MESSAGE_ACTION, "{ action: \"actions\" }");
    bundle.putString(Constants.Keys.PUSH_MESSAGE_TEXT, "text");
    bundle.putString("_lpu", "message##id");
    Map<String, Object> parsed = LeanplumPushService.parseNotificationBundle(bundle);
    assertEquals("{action=actions}", parsed.get(LeanplumPushService.LEANPLUM_ACTION_PARAM).toString());
    assertEquals("text", parsed.get(LeanplumPushService.LEANPLUM_MESSAGE_PARAM));
    assertEquals("message##id", parsed.get(LeanplumPushService.LEANPLUM_MESSAGE_ID));
  }

  @Test
  public void testUnregister() {
    Context mock = mock(Context.class);
    Leanplum.setApplicationContext(mock);
    LeanplumPushService.unregister();

    verify(mock, times(1)).startService(any(Intent.class));
  }

  @Test
  public void testRegister() throws Exception {
    Context mock = mock(Context.class);
    Leanplum.setApplicationContext(mock);
    Request.setAppId(null, null);

    PowerMockito.doReturn(true).when(Util.class, "hasPlayServices");
    PowerMockito.doReturn(true).when(LeanplumPushService.class, "enableFcmServices");
    PowerMockito.doReturn(true).when(LeanplumPushService.class, "enableGcmServices");
    PowerMockito.doReturn(false).when(LeanplumPushService.class, "hasAppIDChanged", any());

    LeanplumGcmProvider gcmProviderMock = spy(new LeanplumGcmProvider());
    whenNew(LeanplumGcmProvider.class).withNoArguments().thenReturn(gcmProviderMock);
    doReturn(true).when(gcmProviderMock).isManifestSetup();
    doReturn(true).when(gcmProviderMock).isInitialized();

    LeanplumPushService.onStart();

    LeanplumFcmProvider fcmProviderMock = spy(new LeanplumFcmProvider());
    whenNew(LeanplumFcmProvider.class).withNoArguments().thenReturn(fcmProviderMock);
    doReturn(true).when(fcmProviderMock).isManifestSetup();
    doReturn(true).when(fcmProviderMock).isInitialized();

    PowerMockito.doReturn(true).when(LeanplumPushService.class, "isFirebaseEnabled");

    LeanplumPushService.onStart();
    verify(mock, times(2)).startService(any(Intent.class));
  }
}
