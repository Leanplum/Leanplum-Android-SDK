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
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.leanplum.__setup.LeanplumTestApp;
import com.leanplum.__setup.TestClassUtil;
import com.leanplum.internal.APIConfig;
import com.leanplum.internal.CollectionUtil;
import com.leanplum.internal.Constants;
import com.leanplum.internal.FileManager;
import com.leanplum.internal.OperationQueue;
import com.leanplum.internal.ShadowOperationQueue;
import com.leanplum.internal.Util;
import com.leanplum.tests.MainActivity;
import com.leanplum.utils.SharedPreferencesUtil;

import org.junit.After;
import org.junit.Assert;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Queue;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
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
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.robolectric.Shadows.shadowOf;

/**
 * Tests for {@link LeanplumPushService} class.
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
@PrepareForTest({LeanplumPushService.class, LeanplumFcmProvider.class,
    SharedPreferencesUtil.class, Util.class, PushProviders.class})
public class LeanplumPushServiceTest {
  @Rule
  public PowerMockRule rule = new PowerMockRule();

  // The target context of the instrumentation.
  private Application context;

  private boolean customizeNotificationBuilderCalled = false;
  private boolean customizeNotificationBuilderCompatCalled = false;

  /**
   * Runs before every test case.
   */
  @Before
  public void setUp() {
    mockStatic(LeanplumPushService.class);
    spy(LeanplumPushService.class);
    spy(Util.class);
    spy(PushProviders.class);

    this.context = RuntimeEnvironment.application;
    assertNotNull(this.context);
    Leanplum.setApplicationContext(this.context);

    customizeNotificationBuilderCalled = false;
    customizeNotificationBuilderCompatCalled = false;

    TestClassUtil.setField(OperationQueue.class, "instance", new ShadowOperationQueue());
  }

  /**
   * Runs after every test case.
   */
  @After
  public void tearDown() throws Exception {
    PowerMockito.doCallRealMethod().when(PushProviders.class, "createFcm");
    TestClassUtil.setField(LeanplumPushService.class, "pushProviders", new PushProviders());
  }

  /**
   * Test for {@link LeanplumPushService#onStart()} that should call {@link
   * PushProviders#updateRegistrationIdsAndBackend()}.
   *
   * @throws Exception
   */
  @Test
  public void testOnStartPushService() throws Exception {
    PushProviders pushProviders = mock(PushProviders.class);
    TestClassUtil.setField(LeanplumPushService.class, "pushProviders", pushProviders);
    Method onStartMethod = LeanplumPushService.class.getDeclaredMethod("onStart");
    onStartMethod.setAccessible(true);
    assertNotNull(onStartMethod);

    onStartMethod.invoke(LeanplumPushService.class);
    verify(pushProviders, times(1)).updateRegistrationIdsAndBackend();
  }

  /**
   * Tests that {@link LeanplumPushService#onStart()} calls the {@link
   * IPushProvider#updateRegistrationId()}.
   *
   * @throws Exception
   */
  @Test
  public void testOnStartUpdatesRegistrationIds() throws Exception {
    spy(PushProviders.class);

    LeanplumFcmProvider fcmProviderMock = spy(new LeanplumFcmProvider());
    doNothing().when(fcmProviderMock).updateRegistrationId();
    PowerMockito.doReturn(fcmProviderMock).when(PushProviders.class, "createFcm");
    PushProviders pushProviders = new PushProviders();
    TestClassUtil.setField(LeanplumPushService.class, "pushProviders", pushProviders);

    Method onStartMethod = LeanplumPushService.class.getDeclaredMethod("onStart");
    onStartMethod.setAccessible(true);
    assertNotNull(onStartMethod);

    onStartMethod.invoke(LeanplumPushService.class);
    verify(fcmProviderMock, times(1)).updateRegistrationId();
  }

  /**
   * Test for {@link PushProviders#hasAppIDChanged(String)} that should return true if
   * application id was stored before and doesn't equal to current.
   *
   * @throws Exception
   */
  @Test
  public void testHasAppIDChanged() throws Exception {
    mockStatic(SharedPreferencesUtil.class);
    APIConfig.getInstance().setAppId("1", "1");
    doNothing().when(SharedPreferencesUtil.class, "setString", context,
        Constants.Defaults.LEANPLUM_PUSH, Constants.Defaults.APP_ID, "1");

    // Test for application id was not stored before.
    when(SharedPreferencesUtil.class, "getString", context, Constants.Defaults.LEANPLUM_PUSH,
        Constants.Defaults.APP_ID).thenReturn("");
    assertFalse(PushProviders.hasAppIDChanged("1"));

    // Test for application id was stored before and doesn't equal to current.
    when(SharedPreferencesUtil.class, "getString", context, Constants.Defaults.LEANPLUM_PUSH,
        Constants.Defaults.APP_ID).thenReturn("2");
    assertTrue(PushProviders.hasAppIDChanged("1"));

    // Test for application id was stored before and equal to current.
    when(SharedPreferencesUtil.class, "getString", context, Constants.Defaults.LEANPLUM_PUSH,
        Constants.Defaults.APP_ID).thenReturn("1");
    assertFalse(PushProviders.hasAppIDChanged("1"));
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
  public void testSetCustomizer() throws NoSuchFieldException, IllegalAccessException {
    CustomCustomizer customCustomizer = new CustomCustomizer();
    LeanplumPushService.setCustomizer(customCustomizer, false);

    Field customizer = LeanplumPushService.class.getDeclaredField("customizer");
    Assert.assertNotNull(customizer);
    Field useNotificationBuilderCustomizer =
        LeanplumPushService.class.getDeclaredField("useNotificationBuilderCustomizer");
    Assert.assertNotNull(useNotificationBuilderCustomizer);
    Assert.assertNotNull(customizer);
    customizer.setAccessible(true);
    useNotificationBuilderCustomizer.setAccessible(true);
    Assert.assertFalse((boolean)useNotificationBuilderCustomizer.get(null));
    assertEquals(customizer.get(null), customCustomizer);

    // Sets LeanplumPushNotificationCustomizer back to null.
    customizer.set(LeanplumPushService.class, null);
    useNotificationBuilderCustomizer.set(LeanplumPushService.class, false);

    LeanplumPushService.setCustomizer(customCustomizer, true);
    assertEquals(customizer.get(null), customCustomizer);
    Assert.assertTrue((boolean)useNotificationBuilderCustomizer.get(null));

    // Sets LeanplumPushNotificationCustomizer back to null.
    customizer.set(LeanplumPushService.class, null);
    useNotificationBuilderCustomizer.set(LeanplumPushService.class, false);

    LeanplumPushService.setCustomizer(customCustomizer);
    assertEquals(customizer.get(null), customCustomizer);
    Assert.assertFalse((boolean)useNotificationBuilderCustomizer.get(null));

    // Sets LeanplumPushNotificationCustomizer back to null.
    customizer.set(LeanplumPushService.class, null);
    useNotificationBuilderCustomizer.set(LeanplumPushService.class, false);
  }

  @Test
  public void testShowNotification() throws Exception {
    Bundle bundle = new Bundle();
    bundle.putString("_lpm", "message_id");
    bundle.putString("_lpx", "{ __Push Notification: \"message\" }");
    bundle.putString("title", "title_string");
    CustomCustomizer customCustomizer = new CustomCustomizer();
    Method showNotification = LeanplumPushService.class.
        getDeclaredMethod("showNotification", Context.class, Bundle.class);
    showNotification.setAccessible(true);

    // Test for Bundle without imageUrl with use useNotificationBuilderCustomizer false.
    LeanplumPushService.setCustomizer(customCustomizer);
    showNotification.invoke(LeanplumPushService.class, context, bundle);
    assertTrue(customizeNotificationBuilderCompatCalled);
    assertFalse(customizeNotificationBuilderCalled);
    customizeNotificationBuilderCompatCalled=false;

    Bundle bundleWithImage = new Bundle();
    bundleWithImage.putString("_lpm", "message_id");
    bundleWithImage.putString("_lpx", "{ __Push Notification: \"message\" }");
    bundleWithImage.putString("title", "title_string");
    bundleWithImage.putString(" lp_imageUrl",
        FileManager.fileRelativeToDocuments("Mario.png"));

    // Test for Bundle with imageUrl.
    showNotification.invoke(LeanplumPushService.class, context, bundleWithImage);

    assertTrue(customizeNotificationBuilderCompatCalled);
    assertFalse(customizeNotificationBuilderCalled);
    customizeNotificationBuilderCompatCalled=false;


    Field customizer = LeanplumPushService.class.getDeclaredField("customizer");
    Assert.assertNotNull(customizer);
    customizer.setAccessible(true);

    // Sets LeanplumPushNotificationCustomizer back to null.
    customizer.set(LeanplumPushService.class, null);


    // Test for Bundle without imageUrl.
    LeanplumPushService.setCustomizer(customCustomizer,false);
    showNotification.invoke(LeanplumPushService.class, context, bundle);
    assertTrue(customizeNotificationBuilderCompatCalled);
    assertFalse(customizeNotificationBuilderCalled);
    customizeNotificationBuilderCompatCalled=false;


    // Test for Bundle with imageUrl.
    showNotification.invoke(LeanplumPushService.class, context, bundleWithImage);

    assertTrue(customizeNotificationBuilderCompatCalled);
    assertFalse(customizeNotificationBuilderCalled);
    customizeNotificationBuilderCompatCalled=false;


    // Sets LeanplumPushNotificationCustomizer back to null.
    customizer.set(LeanplumPushService.class, null);

    // Test for Bundle without imageUrl and useNotificationBuilderCustomizer.
    LeanplumPushService.setCustomizer(customCustomizer, true);

    showNotification.invoke(LeanplumPushService.class, context, bundle);
    assertFalse(customizeNotificationBuilderCompatCalled);
    assertTrue(customizeNotificationBuilderCalled);
    customizeNotificationBuilderCalled = false;

    // Test for Bundle with imageUrl.
    showNotification.invoke(LeanplumPushService.class, context, bundleWithImage);
    assertFalse(customizeNotificationBuilderCompatCalled);
    assertTrue(customizeNotificationBuilderCalled);
    customizeNotificationBuilderCalled = false;


    // Test for Bundle with imageUrl and Api 15.
    setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 15);
    showNotification.invoke(LeanplumPushService.class, context, bundleWithImage);
    assertFalse(customizeNotificationBuilderCompatCalled);
    assertTrue(customizeNotificationBuilderCalled);
    customizeNotificationBuilderCalled=false;
    // Sets LeanplumBigPicturePushNotificationCustomizer back to null.
    customizer.set(LeanplumPushService.class, null);
    setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 16);
  }

  private static void setFinalStatic(Field field, Object newValue) throws Exception {
    field.setAccessible(true);

    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

    field.set(null, newValue);
  }

  class CustomCustomizer implements LeanplumPushNotificationCustomizer {
    @Override
    public void customize(NotificationCompat.Builder builder, Bundle notificationPayload) {
      customizeNotificationBuilderCompatCalled = true;
    }

    @Override
    public void customize(Notification.Builder builder, Bundle notificationPayload, @Nullable Notification.Style bigPictureStyle) {
      customizeNotificationBuilderCalled = true;
    }
  }
}
