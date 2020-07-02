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

import com.leanplum.__setup.AbstractTest;
import com.leanplum._whitebox.utilities.RequestHelper;
import com.leanplum._whitebox.utilities.ResponseHelper;
import com.leanplum.callbacks.InboxChangedCallback;
import com.leanplum.callbacks.InboxSyncedCallback;
import com.leanplum.internal.Constants;
import com.leanplum.internal.RequestBuilder;
import com.leanplum.internal.Util;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.doReturn;

/**
 * Tests the inbox.
 *
 * @author Milos Jakovljevic
 */
public class LeanplumInboxWhiteboxTest extends AbstractTest {
  @Before
  public void setUp() {
    setupSDK(mContext, "/responses/simple_start_response.json");
  }

  @Test
  public void testInbox() throws Exception {
    // Seed inbox response which contains messages.
    ResponseHelper.seedResponse("/responses/newsfeed_response.json");

    // Validate downloadMessages() request.
    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        assertEquals(RequestBuilder.ACTION_GET_INBOX_MESSAGES, apiMethod);
      }
    });

    // Validate inbox synced callback when messages changes.
    InboxSyncedCallback inboxSyncedCallback = new InboxSyncedCallback() {
      @Override
      public void onForceContentUpdate(boolean success) {
        assertTrue(success);
      }
    };

    // Add synced callback to be able to validate.
    Leanplum.getInbox().addSyncedHandler(inboxSyncedCallback);

    // Download messages.
    Leanplum.getInbox().downloadMessages();
    assertEquals(2, Leanplum.getInbox().count());

    //  Remove synced callback afterwards so we don't get synced callbacks anymore.
    Leanplum.getInbox().removeSyncedHandler(inboxSyncedCallback);

    // Validate inbox callback when messages changes.
    InboxChangedCallback callback = new InboxChangedCallback() {
      @Override
      public void inboxChanged() {
        assertEquals(2, Leanplum.getInbox().unreadCount());
        assertEquals(2, Leanplum.getInbox().count());

        List<LeanplumInboxMessage> messageList = Leanplum.getInbox().unreadMessages();

        LeanplumInboxMessage message1 = messageList.get(0);
        LeanplumInboxMessage message2 = messageList.get(1);

        assertEquals("5231495977893888##1", message1.getMessageId());
        assertEquals("This is a test inbox message", message1.getTitle());
        assertEquals("This is a subtitle", message1.getSubtitle());
        assertNull(message1.getExpirationTimestamp());
        assertFalse(message1.isRead());

        assertEquals("4682943996362752##2", message2.getMessageId());
        assertEquals("This is a second test message", message2.getTitle());
        assertEquals("This is a second test message subtitle", message2.getSubtitle());
        assertNull(message2.getExpirationTimestamp());
        assertFalse(message2.isRead());
      }
    };

    // Add callback to be able to validate.
    Leanplum.getInbox().addChangedHandler(callback);

    // Remove it afterwards so we don't get callbacks anymore.
    Leanplum.getInbox().removeChangedHandler(callback);

    // Validate message state.
    List<LeanplumInboxMessage> messageList = Leanplum.getInbox().unreadMessages();

    LeanplumInboxMessage message1 = messageList.get(0);
    LeanplumInboxMessage message2 = messageList.get(1);

    message1.read();
    assertTrue(message1.isRead());
    assertEquals(1, Leanplum.getInbox().unreadCount());

    message2.read();
    assertTrue(message2.isRead());
    assertEquals(0, Leanplum.getInbox().unreadCount());

    assertEquals(2, Leanplum.getInbox().count());

    LeanplumInboxMessage messageById = Leanplum.getInbox().messageForId(message1.getMessageId());
    assertNotNull(messageById);
    assertEquals(message1, messageById);

    Leanplum.getInbox().removeMessage(messageById.getMessageId());
    assertEquals(1, Leanplum.getInbox().allMessages().size());

    // Validate inbox synced callback with false when there is no internet .
    InboxSyncedCallback inboxSyncedCallbackFalse = new InboxSyncedCallback() {
      @Override
      public void onForceContentUpdate(boolean success) {
        assertFalse(success);
      }
    };

    // Add synced callback  to be able to validate.
    Leanplum.getInbox().addSyncedHandler(inboxSyncedCallbackFalse);

    // Return false when checks for internet connection.
    doReturn(false).when(Util.class, "isConnected");

    // Download messages.
    Leanplum.getInbox().downloadMessages();

    //  Remove synced callback afterwards so we don't get synced callbacks anymore.
    Leanplum.getInbox().removeSyncedHandler(inboxSyncedCallbackFalse);
  }

  /**
   * Tests method disablePrefetching.
   */
  @Test
  public void testDisablePrefetching() {
    LeanplumInbox.getInstance().disableImagePrefetching();
    assertFalse(LeanplumInbox.getInstance().isInboxImagePrefetchingEnabled());
  }

  /**
   * Tests that inbox clears all cached messages when downloading messages
   * from serverside.
   */
  @Test
  public void testInboxRemovesACachedMessageAfterDownloading() {
    Date delivery = new Date(100);
    Date expiration = new Date(200);
    HashMap<String, Object> map = new HashMap<>();
    map.put(Constants.Keys.MESSAGE_DATA, new HashMap<String, Object>());
    map.put(Constants.Keys.DELIVERY_TIMESTAMP, delivery.getTime());
    map.put(Constants.Keys.EXPIRATION_TIMESTAMP, expiration.getTime());
    map.put(Constants.Keys.IS_READ, true);
    LeanplumInboxMessage message1 = LeanplumInboxMessage.createFromJsonMap("messageId##1", map);
    Map<String, LeanplumInboxMessage> messages = new HashMap<>();
    messages.put("message##1", message1);
    LeanplumInbox.getInstance().update(messages, 1, true);
    ResponseHelper.seedResponse("/responses/newsfeed_response.json");

    assertEquals(1, LeanplumInbox.getInstance().count());

    LeanplumInbox.getInstance().downloadMessages();

    assertEquals(2, LeanplumInbox.getInstance().count());
  }

  /**
   * Tests method unreadMessages.
   */
  @Test
  public void testGettingUnreadMessagesAfterARead() {
    ResponseHelper.seedResponse("/responses/newsfeed_response.json");
    LeanplumInbox.getInstance().downloadMessages();
    List<LeanplumInboxMessage> messages = LeanplumInbox.getInstance().allMessages();
    messages.get(0).read();
    messages.remove(0);

    List<LeanplumInboxMessage> unreadMessages = LeanplumInbox.getInstance().unreadMessages();

    assertEquals(messages, unreadMessages);
  }
}
