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
import com.leanplum.actions.internal.ActionManagerExecutionKt;
import com.leanplum.internal.ActionManager;

import com.leanplum.messagetemplates.MessageTemplates;
import com.leanplum.messagetemplates.actions.AlertMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Tests in-app message triggers.
 *
 * @author Yilong Chang
 */
@PrepareForTest(value = {
    AlertMessage.class,
    MessageTemplates.class
})
public class LeanplumInAppMessageTriggerTest extends AbstractTest {

  @Before
  public void before() throws Exception {
    super.before();
    AlertMessage alertMessage = PowerMockito.spy(new AlertMessage());
    doReturn(true).when(alertMessage).present(Mockito.any(ActionContext.class));
    whenNew(AlertMessage.class).withNoArguments().thenReturn(alertMessage);
  }

  @After
  public void after() {
    super.after();
    ActionManager.getInstance().setCurrentAction(null);
  }

  /**
   * Tests message triggers on start and impression is recorded. No active period provided.
   */
  @Test
  public void testTriggerOnStart() {
    final String messageId = "Trigger on start";
    ActionManager actionManager = ActionManager.getInstance();
    assertEquals(0, actionManager.getMessageTriggerOccurrences(messageId));
    assertTrue(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());

    setupSDK(mContext, "/responses/start_message_response.json");
    // Assert the trigger and message impression occurred.
    assertEquals(1, actionManager.getMessageTriggerOccurrences(messageId));
    assertFalse(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());
  }


  /**
   * Tests message triggers on start and impression is recorded. Message within active period.
   */
  @Test
  public void testTriggerOnStartInActivePeriod() {
    final String messageId = "Trigger on start";
    ActionManager actionManager = ActionManager.getInstance();
    assertEquals(0, actionManager.getMessageTriggerOccurrences(messageId));
    assertTrue(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());

    setupSDK(mContext, "/responses/start_message_response_in_active_period.json");
    // Assert the trigger and message impression occurred.
    assertEquals(1, actionManager.getMessageTriggerOccurrences(messageId));
    assertFalse(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());
  }


  /**
   * Tests outdated message does not trigger on start.
   */
  @Test
  public void testTriggerOnStartOutActivePeriod() {
    final String messageId = "Trigger on start";
    ActionManager actionManager = ActionManager.getInstance();
    assertEquals(0, actionManager.getMessageTriggerOccurrences(messageId));
    assertTrue(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());

    setupSDK(mContext, "/responses/start_message_response_out_active_period.json");
    // Assert the trigger and message impression occurred.
    assertEquals(0, actionManager.getMessageTriggerOccurrences(messageId));
    assertTrue(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());
  }

  /**
   * Tests message triggers on start or resume, and impression is recorded. Starts in background.
   */
  @Test
  public void testTriggerOnStartOrResumeBackground() throws Exception {
    final String messageId = "Trigger on start or resume";
    ActionManager actionManager = ActionManager.getInstance();
    assertEquals(0, actionManager.getMessageTriggerOccurrences(messageId));
    assertTrue(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());

    // Start in background.
    setupSDK(mContext, "/responses/resume_message_response.json");

    // Assert the trigger and message impression occurred after start.
    assertEquals(1, actionManager.getMessageTriggerOccurrences(messageId));
    assertEquals(3, actionManager.getMessageImpressionOccurrences(messageId).size());
    assertEquals(0L, actionManager.getMessageImpressionOccurrences(messageId).get("max"));

    actionManager.setCurrentAction(null); // clear the Alert

    Leanplum.pause();
    Leanplum.resume();
    // Assert the trigger and message impression didn't occur after first resume.
    assertEquals(1, actionManager.getMessageTriggerOccurrences(messageId));
    assertEquals(3, actionManager.getMessageImpressionOccurrences(messageId).size());
    assertEquals(0L, actionManager.getMessageImpressionOccurrences(messageId).get("max"));

    actionManager.setCurrentAction(null); // clear the Alert

    Leanplum.pause();
    Leanplum.resume();
    // Assert the trigger and message impression occurred after second resume.
    assertEquals(2, actionManager.getMessageTriggerOccurrences(messageId));
    assertEquals(4, actionManager.getMessageImpressionOccurrences(messageId).size());
    assertEquals(1L, actionManager.getMessageImpressionOccurrences(messageId).get("max"));
  }

  /**
   * Tests message triggers on start or resume, and impression is recorded. Starts in foreground.
   */
  @Test
  public void testTriggerOnStartOrResumeForeground() {
    final String messageId = "Trigger on start or resume";
    ActionManager actionManager = ActionManager.getInstance();
    assertEquals(0, actionManager.getMessageTriggerOccurrences(messageId));
    assertTrue(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());

    ResponseHelper.seedResponse("/responses/resume_message_response.json");
    // Start in foreground so that the message can be triggered on resumeSession.
    Leanplum.start(mContext, null, null, null, false);

    // Assert the trigger and message impression occurred after start.
    assertEquals(1, actionManager.getMessageTriggerOccurrences(messageId));
    assertEquals(3, actionManager.getMessageImpressionOccurrences(messageId).size());
    assertEquals(0L, actionManager.getMessageImpressionOccurrences(messageId).get("max"));

    actionManager.setCurrentAction(null); // clear the Alert

    Leanplum.pause();
    Leanplum.resume();
    // Assert the trigger and message impression occurred after resume after resume.
    assertEquals(2, actionManager.getMessageTriggerOccurrences(messageId));
    assertEquals(4, actionManager.getMessageImpressionOccurrences(messageId).size());
    assertEquals(1L, actionManager.getMessageImpressionOccurrences(messageId).get("max"));

    actionManager.setCurrentAction(null); // clear the Alert

    Leanplum.pause();
    Leanplum.resume();
    // Assert the trigger and message impression occurrence incremented after resume.
    assertEquals(3, actionManager.getMessageTriggerOccurrences(messageId));
    assertEquals(5, actionManager.getMessageImpressionOccurrences(messageId).size());
    assertEquals(2L, actionManager.getMessageImpressionOccurrences(messageId).get("max"));
  }

  /**
   * Tests message triggers on advance to state, and impression is recorded.
   */
  @Test
  public void testTriggerOnAdvance() throws Exception {
    final String messageId = "Trigger on advance";
    final String state = "Registered";
    ActionManager actionManager = ActionManager.getInstance();
    assertEquals(0, actionManager.getMessageTriggerOccurrences(messageId));
    assertTrue(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());

    setupSDK(mContext, "/responses/start_message_response.json");
    actionManager.setCurrentAction(null); // clear the Alert on start
    Leanplum.advanceTo(state);
    // Assert the trigger and message impression occurred after advanced to state.
    assertEquals(1, actionManager.getMessageTriggerOccurrences(messageId));
    assertFalse(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());
  }

  /**
   * Tests message triggers on attribute changes, and impression is recorded.
   */
  @Test
  public void testTriggerOnAttributeChanges() throws Exception {
    final String messageId = "Trigger on Attribute changes";
    final String attribute = "Nickname";
    final  String attributeValue = "Android";
    ActionManager actionManager = ActionManager.getInstance();
    assertEquals(0, actionManager.getMessageTriggerOccurrences(messageId));
    assertTrue(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());

    setupSDK(mContext, "/responses/start_message_response.json");
    actionManager.setCurrentAction(null); // clear the Alert on start
    Leanplum.setUserAttributes(new HashMap<String, Object>(){{
      put(attribute, attributeValue);
    }});
    // Assert the trigger and message impression occurred after attribute changed.
    assertEquals(1, actionManager.getMessageTriggerOccurrences(messageId));
    assertFalse(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());
  }
}
