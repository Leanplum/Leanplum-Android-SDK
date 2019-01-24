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
import com.leanplum.internal.CollectionUtil;
import com.leanplum.internal.Constants;
import com.leanplum.internal.FileManager;
import com.leanplum.internal.RequestOld;
import com.leanplum.internal.Util;
import com.leanplum.tests.MainActivity;
import com.leanplum.utils.SharedPreferencesUtil;

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
@PrepareForTest({LeanplumPushService.class, LeanplumFcmProvider.class, LeanplumGcmProvider.class,
    SharedPreferencesUtil.class, Util.class, LeanplumPushServiceGcm.class,
    LeanplumPushServiceFcm.class})
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
    spy(LeanplumGcmProvider.class);

    this.context = RuntimeEnvironment.application;
    assertNotNull(this.context);
    Leanplum.setApplicationContext(this.context);

    customizeNotificationBuilderCalled = false;
    customizeNotificationBuilderCompatCalled = false;
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

    RequestOld.setAppId("1", "1");
    when(LeanplumPushService.class, "hasAppIDChanged", "1").thenReturn(false);

    LeanplumPushService pushService = new LeanplumPushService();
    Method initPushServiceMethod = LeanplumPushService.class.
        getDeclaredMethod("initPushService");
    initPushServiceMethod.setAccessible(true);

//    when(LeanplumPushService.class, "enableFcmServices").thenReturn(true);
//    when(LeanplumPushService.class, "enableGcmServices").thenReturn(true);

    // Tests for Firebase.
    when(LeanplumPushService.isFirebaseEnabled()).thenReturn(true);
    LeanplumPushService.setCloudMessagingProvider(fcmProviderMock);

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
    LeanplumPushService.setCloudMessagingProvider(gcmProviderMock);

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
   * Test for {@link LeanplumPushService#onStart}
   *
   * @throws Exception
   */
  @Test
  public void testOnStart() throws Exception {
    LeanplumPushService pushService = new LeanplumPushService();
    Method onStartMethod = LeanplumPushService.class.getDeclaredMethod("onStart");
    onStartMethod.setAccessible(true);

    mockStatic(LeanplumPushServiceGcm.class);
    mockStatic(LeanplumPushServiceFcm.class);

    // Don't call GCM onStart or FCM onStart if both FCM and GCM enabled.
    onStartMethod.invoke(pushService);
    assertNotNull(onStartMethod);
    verifyStatic(times(0));
    LeanplumPushServiceGcm.class.getDeclaredMethod("onStart");
    verifyStatic(times(0));
    LeanplumPushServiceFcm.class.getDeclaredMethod("onStart");
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
    RequestOld.setAppId("1", "1");
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
    RequestOld.setAppId(null, null);

    PowerMockito.doReturn(true).when(Util.class, "hasPlayServices");
    PowerMockito.doReturn(false).when(LeanplumPushService.class, "hasAppIDChanged", any());

    LeanplumGcmProvider gcmProviderMock = spy(new LeanplumGcmProvider());
    whenNew(LeanplumGcmProvider.class).withNoArguments().thenReturn(gcmProviderMock);
    doReturn(true).when(gcmProviderMock).isManifestSetup();
    doReturn(true).when(gcmProviderMock).isInitialized();
    LeanplumPushService.setCloudMessagingProvider(gcmProviderMock);
    LeanplumPushService.initPushService();

    LeanplumFcmProvider fcmProviderMock = spy(new LeanplumFcmProvider());
    whenNew(LeanplumFcmProvider.class).withNoArguments().thenReturn(fcmProviderMock);
    doReturn(true).when(fcmProviderMock).isManifestSetup();
    doReturn(true).when(fcmProviderMock).isInitialized();

    PowerMockito.doReturn(true).when(LeanplumPushService.class, "isFirebaseEnabled");
    LeanplumPushService.setCloudMessagingProvider(fcmProviderMock);

    LeanplumPushService.initPushService();
    verify(mock, times(2)).startService(any(Intent.class));
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
