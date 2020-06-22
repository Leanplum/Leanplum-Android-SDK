/*
 * Copyright 2018, Leanplum, Inc. All rights reserved.
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

import android.os.Build.VERSION_CODES;
import com.leanplum.internal.Constants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests covering Inbox Messages.
 *
 * @author Sayaan Saha
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = VERSION_CODES.P) // temporarily fix issue with Robolectric and Android SDK 29
public class LeanplumInboxMessageTest {
  /**
   * Test creating a message from json.
   */
  @Test
  public void testCreateFromJsonMap() {
    Date delivery = new Date(100);
    Date expiration = new Date(200);
    HashMap<String, Object> map = new HashMap<>();
    map.put(Constants.Keys.MESSAGE_DATA, new HashMap<String, Object>());
    map.put(Constants.Keys.DELIVERY_TIMESTAMP, delivery.getTime());
    map.put(Constants.Keys.EXPIRATION_TIMESTAMP, expiration.getTime());
    map.put(Constants.Keys.IS_READ, true);

    LeanplumInboxMessage message = LeanplumInboxMessage.createFromJsonMap("message##Id", map);
    assertEquals("message##Id", message.getMessageId());
    assertEquals(delivery, message.getDeliveryTimestamp());
    assertEquals(expiration, message.getExpirationTimestamp());
    assertTrue(message.isRead());
    assertNull(message.getData());

    assertNull(message.getImageFilePath());
    assertNull(message.getImageUrl());
  }

  /**
   * Test that message without messageId is rejected.
   */
  @Test
  public void testCreateFromJsonMapInvalidMessageIdIsRejected() {
    Date delivery = new Date(100);
    Date expiration = new Date(200);
    HashMap<String, Object> map = new HashMap<>();
    map.put(Constants.Keys.MESSAGE_DATA, new HashMap<String, Object>());
    map.put(Constants.Keys.DELIVERY_TIMESTAMP, delivery.getTime());
    map.put(Constants.Keys.EXPIRATION_TIMESTAMP, expiration.getTime());
    map.put(Constants.Keys.IS_READ, true);

    LeanplumInboxMessage invalidMessage = LeanplumInboxMessage.createFromJsonMap("messageId", map);
    assertNull(invalidMessage);
  }

  /**
   * Test unread count is updated after reading a message.
   */
  @Test
  public void testReadAndUnreadCount() {
    Date delivery = new Date(100);
    Date expiration = new Date(200);
    HashMap<String, Object> map = new HashMap<>();
    map.put(Constants.Keys.MESSAGE_DATA, new HashMap<String, Object>());
    map.put(Constants.Keys.DELIVERY_TIMESTAMP, delivery.getTime());
    map.put(Constants.Keys.EXPIRATION_TIMESTAMP, expiration.getTime());
    map.put(Constants.Keys.IS_READ, false);
    LeanplumInboxMessage message = LeanplumInboxMessage
        .createFromJsonMap("messageId##00", map);
    int intialUnreadCount = LeanplumInbox.getInstance().unreadCount();

    assertEquals(false, message.isRead());

    message.read();

    assertEquals(true, message.isRead());
    assertEquals(intialUnreadCount - 1, LeanplumInbox.getInstance().unreadCount());
  }

  /**
   * Tests method isActive happy path.
   */
  @Test
  public void testIsActive() {
    Date delivery = new Date(100);
    Calendar date = Calendar.getInstance();
    Date expiration = new Date(date.getTimeInMillis() + 100000);

    HashMap<String, Object> map = new HashMap<>();
    map.put(Constants.Keys.MESSAGE_DATA, new HashMap<String, Object>());
    map.put(Constants.Keys.DELIVERY_TIMESTAMP, delivery.getTime());
    map.put(Constants.Keys.EXPIRATION_TIMESTAMP, expiration.getTime());
    map.put(Constants.Keys.IS_READ, false);
    LeanplumInboxMessage message = LeanplumInboxMessage
        .createFromJsonMap("messageId##00", map);

    Boolean active = message.isActive();

    assertTrue(active);
  }

  /**
   * Test isActive method with a message
   * after the expiration timestamp.
   */
  @Test
  public void testIsActiveWithExpiredMessage() {
    Date delivery = new Date(200);
    Date expiration = new Date(200);
    HashMap<String, Object> map = new HashMap<>();
    map.put(Constants.Keys.MESSAGE_DATA, new HashMap<String, Object>());
    map.put(Constants.Keys.DELIVERY_TIMESTAMP, delivery.getTime());
    map.put(Constants.Keys.EXPIRATION_TIMESTAMP, expiration.getTime());
    map.put(Constants.Keys.IS_READ, false);
    LeanplumInboxMessage message = LeanplumInboxMessage
        .createFromJsonMap("messageId##00", map);

    Boolean isActive = message.isActive();

    assertFalse(isActive);
  }

  /**
   * Test isActive method with null expiration timestamp.
   */
  @Test
  public void testIsActiveWithNullTimestamp() {
    Date delivery = new Date(100);
    HashMap<String, Object> map = new HashMap<>();
    map.put(Constants.Keys.MESSAGE_DATA, new HashMap<String, Object>());
    map.put(Constants.Keys.DELIVERY_TIMESTAMP, delivery.getTime());
    map.put(Constants.Keys.IS_READ, false);
    LeanplumInboxMessage message = LeanplumInboxMessage
        .createFromJsonMap("messageId##00", map);

    Boolean isActive = message.isActive();

    assertTrue(isActive);
  }
}
