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



import android.content.Intent;

import com.leanplum.__setup.AbstractTest;
import com.leanplum._whitebox.utilities.ResponseHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.mockito.Mockito.mock;


/**
 * Whitebox tests covering Inbox Messages.
 *
 * @author Sayaan Saha
 */
@PrepareForTest({ActionContext.class})
public class LeanplumInboxMessageWhiteboxTest extends AbstractTest {
  @Rule
  public PowerMockRule rule = new PowerMockRule();

  @Before
  public void setUp() {
    spy(ActionContext.class);
    setupSDK(mContext, "/responses/simple_start_response.json");
  }

  /**
   * Tests getting local file path for prefetched image assests.
   */
  @Test
  public void testImageFilepathIsReturnedIfPrefetchingEnabled() {
    ResponseHelper.seedResponse("/responses/newsfeed_response.json");
    LeanplumInbox.getInstance().downloadMessages();
    LeanplumInboxMessage imageMessage = LeanplumInbox.getInstance().allMessages().get(0);

    String imageFilePath = imageMessage.getImageFilePath();

    assertNotNull(imageFilePath);
  }

  /**
   * Tests getting image url from Inbox message object.
   */
  @Test
  public void testImageURL() {
    LeanplumInbox.disableImagePrefetching();
    ResponseHelper.seedResponse("/responses/newsfeed_response.json");
    LeanplumInbox.getInstance().downloadMessages();
    List<LeanplumInboxMessage> messagesList = LeanplumInbox.getInstance().allMessages();
    LeanplumInboxMessage imageMessage = messagesList.get(0);

    String actualUrl = imageMessage.getImageUrl().toString();

    assertEquals("http://bit.ly/2GzJxxx", actualUrl);
  }

  /**
   * Tests method remove, to remove a message from inbox.
   */
  @Test
  public void testRemove() {
    ResponseHelper.seedResponse("/responses/newsfeed_response.json");
    LeanplumInbox.getInstance().downloadMessages();
    LeanplumInboxMessage message = LeanplumInbox.getInstance().allMessages().get(1);
    int intialCount = LeanplumInbox.getInstance().count();

    message.remove();

    assertEquals(intialCount - 1, LeanplumInbox.getInstance().count());
  }

  /**
   * Tests open action has been correctly handed off to ActionContext
   * where it is then executed.
   */
  @Test
  public void testOpenAction() throws Exception {
    ActionContext mock = mock(ActionContext.class);
    Constructor<LeanplumInboxMessage> constructor= (Constructor<LeanplumInboxMessage>) LeanplumInboxMessage.class.getDeclaredConstructors()[0];
    constructor.setAccessible(true);

    LeanplumInboxMessage imageMessage = constructor.newInstance("mesageId##11", 100L,
        200L, false, mock);
    imageMessage.read();

    verify(mock, times(1)).runTrackedActionNamed("Open action");
  }
}
