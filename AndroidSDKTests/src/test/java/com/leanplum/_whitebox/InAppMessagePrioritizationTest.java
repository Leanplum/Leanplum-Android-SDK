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
package com.leanplum._whitebox;

import com.leanplum.ActionContext;
import com.leanplum.__setup.AbstractTest;
import com.leanplum.callbacks.VariablesChangedCallback;
import com.leanplum.internal.ActionManager;
import com.leanplum.internal.JsonConverter;
import com.leanplum.internal.LeanplumInternal;
import com.leanplum.internal.LeanplumMessageMatchFilter;
import com.leanplum.internal.VarCache;

import org.apache.maven.artifact.ant.shaded.IOUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * Unit Tests for in-app message prioritization.
 *
 * @author Kyu Hyun Chang
 */
public class InAppMessagePrioritizationTest extends AbstractTest {
  private ActionManager.MessageMatchResult mMessageMatchResult;
  private ActionContext.ContextualValues mContextualValues;
  private ActionManager mMockActionManager;

  @Override
  public void before() throws Exception {
    super.before();

    mMessageMatchResult = new ActionManager.MessageMatchResult();
    Whitebox.setInternalState(mMessageMatchResult, "matchedTrigger", true);
    Whitebox.setInternalState(mMessageMatchResult, "matchedLimit", true);
    assertTrue(mMessageMatchResult.matchedTrigger);
    assertTrue(mMessageMatchResult.matchedLimit);


    mContextualValues = new ActionContext.ContextualValues();
    // create mock object
    mMockActionManager = mock(ActionManager.class);
    // workaround for singleton objects
    Whitebox.setInternalState(ActionManager.class, "instance", mMockActionManager);
  }

  @Override
  public void after() {
    // Do nothing.
  }

  /**
   * Tests in-app message prioritization by mocking VarCache, ActionManager, and Leanplum.
   *
   * @param jsonMessages message configuration in JSON format
   * @param expectedMessageIds a set of expected message ids to be triggered
   */
  private void assertExpectedMessagesAreTriggered(String jsonMessages,
      Set<String> expectedMessageIds) throws Exception {
    // Stub VarCache.message() to return testMessages.
    spy(VarCache.class);
    Map<String, Object> testMessages = JsonConverter.fromJson(jsonMessages);
    doReturn(testMessages).when(VarCache.class, "messages");

    // Stub actionManager.shouldShowMessage(...) to return true.
    for (String messageId : testMessages.keySet()) {
      @SuppressWarnings("unchecked")
      Map<String, Object> messageConfig = (Map<String, Object>) testMessages.get(messageId);
      doReturn(mMessageMatchResult).when(mMockActionManager)
          .shouldShowMessage(messageId, messageConfig, "event", "TestActivity", mContextualValues);
    }

    // Stub Leanplum.triggerAction(...) to keep track of triggered messageIds.
    spy(LeanplumInternal.class);
    final Set<String> triggeredMessageIds = new HashSet<>();
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        ActionContext actionContext = (ActionContext) args[0];
        triggeredMessageIds.add(actionContext.getMessageId());
        return null;
      }
    }).when(LeanplumInternal.class, "triggerAction", any(ActionContext.class),
        any(VariablesChangedCallback.class));

    LeanplumInternal.maybePerformActions("event", "TestActivity",
        LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL, null, mContextualValues);

    assertEquals(expectedMessageIds, triggeredMessageIds);
  }

  /**
   * Testing a single message with priority 1.
   */
  @Test
  public void testSingleMessage() throws Exception {
    Assert.assertNotNull(RuntimeEnvironment.application); //Getting the application context
    InputStream inputStream = getClass().getResourceAsStream("/test_files/single_message.json");
    Assert.assertNotNull(inputStream);
    String jsonMessages = IOUtil.toString(inputStream);
    Set<String> expectedMessageIds = new HashSet<>();
    expectedMessageIds.add("1");
    assertExpectedMessagesAreTriggered(jsonMessages, expectedMessageIds);
  }

  /**
   * Testing three messages with no priorities. Only one should be called.
   */
  @Test
  public void testNoPriorities() throws Exception {
    InputStream inputStream = getClass().getResourceAsStream("/test_files/no_priorities.json");
    String jsonMessages = IOUtil.toString(inputStream);
    Set<String> expectedMessageIds = new HashSet<>(Arrays.asList("1"));
    assertExpectedMessagesAreTriggered(jsonMessages, expectedMessageIds);
  }

  /**
   * Testing messages with different priorities.
   */
  @Test
  public void testDifferentPriorities() throws Exception {
    // Three messages with priorities of 1, 2, and 3. Only the first one should be triggered.
    InputStream inputStream = getClass()
        .getResourceAsStream("/test_files/different_priorities_1.json");
    String jsonMessages = IOUtil.toString(inputStream);
    Set<String> expectedMessageIds = new HashSet<>();
    expectedMessageIds.add("1");
    assertExpectedMessagesAreTriggered(jsonMessages, expectedMessageIds);

    // Three messages with priorities of 10, 1000, and 5. Only the third one should be triggered.
    inputStream = getClass().getResourceAsStream("/test_files/different_priorities_2.json");
    jsonMessages = IOUtil.toString(inputStream);
    expectedMessageIds = new HashSet<>();
    expectedMessageIds.add("3");
    assertExpectedMessagesAreTriggered(jsonMessages, expectedMessageIds);
  }

  /**
   * Testing messages with tied priorities.
   */
  @Test
  public void testTiedPriorities() throws Exception {
    // Three messages with priorities of 5, no value, and 5.
    // The first one should be triggered.
    InputStream inputStream = getClass().getResourceAsStream("/test_files/tied_priorities_1.json");
    String jsonMessages = IOUtil.toString(inputStream);
    Set<String> expectedMessageIds = new HashSet<>(Arrays.asList("1"));
    assertExpectedMessagesAreTriggered(jsonMessages, expectedMessageIds);

    // Three messages with same priority. Only one should be triggered.
    inputStream = getClass().getResourceAsStream("/test_files/tied_priorities_2.json");
    jsonMessages = IOUtil.toString(inputStream);
    expectedMessageIds = new HashSet<>(Arrays.asList("1"));
    assertExpectedMessagesAreTriggered(jsonMessages, expectedMessageIds);
  }

  /**
   * Testing messages with different priorities along with no priority (10, 30, no value). Only the
   * one with priority value of 10 should be called.
   */
  @Test
  public void testDifferentPrioritiesWithMissingValues() throws Exception {
    InputStream inputStream = getClass()
        .getResourceAsStream("/test_files/different_priorities_missing_values.json");
    String jsonMessages = IOUtil.toString(inputStream);
    Set<String> expectedMessageIds = new HashSet<>();
    expectedMessageIds.add("1");
    assertExpectedMessagesAreTriggered(jsonMessages, expectedMessageIds);
  }
}
