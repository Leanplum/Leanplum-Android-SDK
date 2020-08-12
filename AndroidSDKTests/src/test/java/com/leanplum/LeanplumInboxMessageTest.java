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
package com.leanplum;

import android.os.Build.VERSION_CODES;
import com.leanplum.internal.Constants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;
import java.util.HashMap;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests covering Inbox Messages.
 *
 * @author Sayaan Saha
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = VERSION_CODES.P) // temporarily fix issue with Robolectric and Android SDK 29
public class LeanplumInboxMessageTest {

  private LeanplumInboxMessage createMessageTestData(
      String messageId,
      boolean isRead,
      long deliveryMillis,
      long expirationMillis) {

    HashMap<String, Object> map = new HashMap<>();
    map.put(Constants.Keys.MESSAGE_DATA, new HashMap<String, Object>());
    if (deliveryMillis != 0)
      map.put(Constants.Keys.DELIVERY_TIMESTAMP, deliveryMillis);
    if (expirationMillis != 0)
      map.put(Constants.Keys.EXPIRATION_TIMESTAMP, expirationMillis);
    map.put(Constants.Keys.IS_READ, isRead);
    LeanplumInboxMessage message = LeanplumInboxMessage.createFromJsonMap(messageId, map);
    return message;
  }

  private LeanplumInboxMessage createMessageTestData(String messageId, boolean isRead) {
    return createMessageTestData(messageId, isRead, 100, 200);
  }

  /**
   * Test creating a message from json.
   */
  @Test
  public void testCreateFromJsonMap() {
    String messageId = "message##Id";
    long deliveryMillis = 100;
    long expirationMillis = 200;

    LeanplumInboxMessage message =
        createMessageTestData(messageId, true, deliveryMillis, expirationMillis);

    assertEquals(messageId, message.getMessageId());
    assertEquals(deliveryMillis, message.getDeliveryTimestamp().getTime());
    assertEquals(expirationMillis, message.getExpirationTimestamp().getTime());
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
    String invalidMessageId = "messageId";
    LeanplumInboxMessage invalidMessage = createMessageTestData(invalidMessageId, true);

    assertNull(invalidMessage);
  }

  /**
   * Test unread count is updated after reading a message.
   */
  @Test
  public void testReadAndUnreadCount() {
    LeanplumInboxMessage message = createMessageTestData("messageId##00", false);
    int initialUnreadCount = LeanplumInbox.getInstance().unreadCount();

    assertFalse(message.isRead());

    message.read();

    assertTrue(message.isRead());
    assertEquals(initialUnreadCount - 1, LeanplumInbox.getInstance().unreadCount());
  }

  /**
   * Tests method isActive happy path.
   */
  @Test
  public void testIsActive() {
    long deliveryMillis = 100;
    long expirationMillis = new Date().getTime() + 100000; // current time + 100000

    LeanplumInboxMessage message =
        createMessageTestData("messageId##00", false, deliveryMillis, expirationMillis);

    assertTrue(message.isActive());
  }

  /**
   * Test isActive method with a message
   * after the expiration timestamp.
   */
  @Test
  public void testIsActiveWithExpiredMessage() {
    LeanplumInboxMessage message = createMessageTestData("messageId##00", false, 200, 200);
    assertFalse(message.isActive());
  }

  /**
   * Test isActive method with null expiration timestamp.
   */
  @Test
  public void testIsActiveWithNullTimestamp() {
    long deliveryMillis = 100;
    long noExpiration = 0;
    LeanplumInboxMessage message =
        createMessageTestData("messageId##00", false, deliveryMillis, noExpiration);

    assertTrue(message.isActive());
  }

  /**
   * Test markAsRead does not execute Open Action.
   */
  @Test
  public void testMarkAsRead() {
    LeanplumInboxMessage realMessage = createMessageTestData("messageId##00", false);
    LeanplumInboxMessage message = Mockito.spy(realMessage);

    ActionContext context = Mockito.mock(ActionContext.class);
    Mockito.when(message.getContext()).thenReturn(context);

    assertFalse(message.isRead());

    message.markAsRead();

    assertTrue(message.isRead());
    verify(context, never()).runTrackedActionNamed("Open action");
  }

  /**
   * Test read() calls markAsRead and executes Open Action.
   */
  @Test
  public void testReadCallsMarkAsRead() {
    LeanplumInboxMessage realMessage = createMessageTestData("messageId##00", false);
    LeanplumInboxMessage message = Mockito.spy(realMessage);

    ActionContext context = Mockito.mock(ActionContext.class);
    Mockito.when(message.getContext()).thenReturn(context);

    assertFalse(message.isRead());

    message.read();

    assertTrue(message.isRead());
    verify(message, times(1)).markAsRead();
    verify(context, times(1)).runTrackedActionNamed("Open action");
  }
}
