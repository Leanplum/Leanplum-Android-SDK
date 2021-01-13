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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.leanplum.__setup.AbstractTest;
import com.leanplum._whitebox.utilities.ResponseHelper;
import com.leanplum.callbacks.InboxSyncedCallback;
import org.junit.Test;

/**
 * Unit tests for {@link LeanplumInbox}.
 */
public class LeanplumInboxTest extends AbstractTest {

  /**
   * Test for {@link LeanplumInbox#downloadMessages()}.
   */
  @Test
  public void testDownloadMessages() {
    setupSDK(mContext, "/responses/simple_start_response.json");

    ResponseHelper.seedResponse("/responses/newsfeed_response.json");
    Boolean[] inboxSyncResult = new Boolean[1]; // boolean holder

    LeanplumInbox.getInstance().addSyncedHandler(new InboxSyncedCallback() {
      @Override
      public void onForceContentUpdate(boolean success) {
        inboxSyncResult[0] = success;
      }
    });
    LeanplumInbox.getInstance().downloadMessages();

    assertNotNull(inboxSyncResult[0]);
    assertTrue(inboxSyncResult[0]);
  }

  /**
   * Test for {@link LeanplumInbox#downloadMessages(InboxSyncedCallback)}.
   */
  @Test
  public void testDownloadMessagesWithCallback() {
    setupSDK(mContext, "/responses/simple_start_response.json");

    ResponseHelper.seedResponse("/responses/newsfeed_response.json");
    Boolean[] inboxSyncResult = new Boolean[1]; // boolean holder

    LeanplumInbox.getInstance().downloadMessages(new InboxSyncedCallback() {
      @Override
      public void onForceContentUpdate(boolean success) {
        inboxSyncResult[0] = success;
      }
    });
    assertNotNull(inboxSyncResult[0]);
    assertTrue(inboxSyncResult[0]);
  }

}
