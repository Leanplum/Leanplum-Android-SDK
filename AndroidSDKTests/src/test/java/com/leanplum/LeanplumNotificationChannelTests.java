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

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;

import com.leanplum.__setup.AbstractTest;

import org.json.JSONArray;
import org.junit.Test;
import org.mockito.Matchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;

@Config(
    sdk = 26
)
@PrepareForTest(value = {
    LeanplumNotificationChannel.class
})
/**
 * Notifiction channels tests
 * @author Milos Jakovljevic
 */
public class LeanplumNotificationChannelTests extends AbstractTest {

  @Override
  public void after() {
    // Do nothing.
  }

  @Override
  public void before() throws Exception {
    super.before();
    spy(LeanplumNotificationChannel.class);
    doReturn(26).when(LeanplumNotificationChannel.class, "getTargetSdkVersion", Matchers.anyObject());
  }

  @Test
  public void testNotificationChannels() throws Exception {
    List<HashMap<String, Object>> channelList = new ArrayList<>();
    List<HashMap<String, Object>> groupList = new ArrayList<>();

    HashMap<String, Object> group1 = new HashMap<>();
    group1.put("id", "id_1");
    group1.put("name", "name_1");

    HashMap<String, Object> group2 = new HashMap<>();
    group2.put("id", "id_2");
    group2.put("name", "name_2");

    HashMap<String, Object> group3 = new HashMap<>();
    group3.put("id", "id_3");
    group3.put("name", "name_3");

    HashMap<String, Object> channel1 = new HashMap<>();
    channel1.put("id", "id_1");
    channel1.put("name", "name_1");
    channel1.put("importance", 1);
    channel1.put("description", "description_1");
    channel1.put("enable_vibration", true);
    channel1.put("vibration_pattern", new long[] {1, 2, 3, 4, 5});

    HashMap<String, Object> channel2 = new HashMap<>();
    channel2.put("id", "id_2");
    channel2.put("name", "name_2");
    channel2.put("importance", 1);
    channel2.put("description", "description_2");

    HashMap<String, Object> channel3 = new HashMap<>();
    channel3.put("id", "id_3");
    channel3.put("name", "name_3");
    channel3.put("importance", 1);
    channel3.put("description", "description_3");

    groupList.add(group1);
    groupList.add(group2);
    groupList.add(group3);

    channelList.add(channel1);
    channelList.add(channel2);
    channelList.add(channel3);

    JSONArray groups = new JSONArray(groupList);
    JSONArray channels = new JSONArray(channelList);
    LeanplumNotificationChannel.configureNotificationChannels(Leanplum.getContext(), channels);
    LeanplumNotificationChannel.configureNotificationGroups(Leanplum.getContext(), groups);

    List<HashMap<String, Object>> retrievedChannels = Whitebox.invokeMethod(
        LeanplumNotificationChannel.class, "retrieveNotificationChannels", Leanplum.getContext());
    List<HashMap<String, Object>> retrievedGroups = Whitebox.invokeMethod(
        LeanplumNotificationChannel.class, "retrieveNotificationGroups", Leanplum.getContext());

    assertEquals(3, retrievedChannels.size());
    assertEquals(3, retrievedGroups.size());

    List<NotificationChannel> notificationChannels = LeanplumNotificationChannel.
        getNotificationChannels(Leanplum.getContext());
    List<NotificationChannelGroup> notificationGroups = LeanplumNotificationChannel.
        getNotificationGroups(Leanplum.getContext());

    assertNotNull(notificationChannels);
    assertNotNull(notificationGroups);

    assertEquals(3, notificationChannels.size());
    assertEquals(3, notificationGroups.size());

    groupList.clear();
    groupList.add(group1);
    groupList.add(group3);
    groups = new JSONArray(groupList);

    channelList.clear();
    channelList.add(channel1);
    channelList.add(channel3);
    channels = new JSONArray(channelList);

    LeanplumNotificationChannel.configureNotificationChannels(Leanplum.getContext(), channels);
    LeanplumNotificationChannel.configureNotificationGroups(Leanplum.getContext(), groups);

    retrievedChannels = Whitebox.invokeMethod(
        LeanplumNotificationChannel.class, "retrieveNotificationChannels", Leanplum.getContext());
    retrievedGroups = Whitebox.invokeMethod(
        LeanplumNotificationChannel.class, "retrieveNotificationGroups", Leanplum.getContext());

    assertEquals(2, retrievedChannels.size());
    assertEquals(2, retrievedGroups.size());

    notificationChannels = LeanplumNotificationChannel.
        getNotificationChannels(Leanplum.getContext());
    notificationGroups = LeanplumNotificationChannel.
        getNotificationGroups(Leanplum.getContext());

    assertNotNull(notificationChannels);
    assertNotNull(notificationGroups);

    assertEquals(2, notificationChannels.size());
    // Uncomment when robolectric fixes notification group deletion.
    // assertEquals(2, notificationGroups.size());
  }

  @Test
  public void testDefaultNotificationChannels() throws Exception {
    String defaultChannelId = "id_1";

    LeanplumNotificationChannel.configureDefaultNotificationChannel(Leanplum.getContext(),
        defaultChannelId);

    String channelId = Whitebox.invokeMethod(
        LeanplumNotificationChannel.class, "retrieveDefaultNotificationChannel", Leanplum.getContext());

    assertNotNull(channelId);
    assertEquals(defaultChannelId, channelId);
  }
}
