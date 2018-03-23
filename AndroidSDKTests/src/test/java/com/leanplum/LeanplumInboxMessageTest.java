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

import com.leanplum.__setup.AbstractTest;
import com.leanplum._whitebox.utilities.ResponseHelper;
import com.leanplum.internal.Constants;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static org.junit.Assert.assertTrue;

/**
 * Tests covering Inbox Messages.
 * @author Sayaan Saha
 */
public class LeanplumInboxMessageTest extends AbstractTest {
  @Before
  public void setUp() {
    setupSDK(mContext, "/responses/simple_start_response.json");
  }

  @Test
  public void testMessageCreate() {
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

  @Test
  public void testInvalidMessageIdIsRejected() {
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

  @Test
  public void testReadAndUpdateMessageCount() {
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
    assertEquals(intialUnreadCount-1, LeanplumInbox.getInstance().unreadCount());
  }

  @Test
  public void testImageURL() {
    LeanplumInbox.getInstance().disableImagePrefetching();
    ResponseHelper.seedResponse("/responses/newsfeed_response.json");
    LeanplumInbox.getInstance().downloadMessages();
    List<LeanplumInboxMessage> messagesList = LeanplumInbox.getInstance().allMessages();
    LeanplumInboxMessage imageMessage = messagesList.get(0);

    String actualUrl = imageMessage.getImageUrl().toString();

    assertEquals("http://bit.ly/2GzJxxx", actualUrl);
  }

  @Test
  public void testImageFilepathIsReturnedIfPrefetchingEnabled() {
    ResponseHelper.seedResponse("/responses/newsfeed_response.json");
    LeanplumInbox.getInstance().downloadMessages();
    LeanplumInboxMessage imageMessage = LeanplumInbox.getInstance().allMessages().get(0);

    String imageFilePath = imageMessage.getImageFilePath();

    assertNotNull(imageFilePath);
  }

  @Test
  public void testOpenAction() {
    ResponseHelper.seedResponse("/responses/newsfeed_response.json");
    LeanplumInbox.getInstance().downloadMessages();
    LeanplumInboxMessage imageMessage = LeanplumInbox.getInstance().allMessages().get(1);
    imageMessage.read();

    HashMap actionName = imageMessage.getContext().objectNamed("Open action");

    assertEquals("Alert", actionName.get("__name__"));
  }
}
