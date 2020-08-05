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

import android.content.Context;
import android.content.SharedPreferences;

import com.google.common.collect.ImmutableMap;
import com.leanplum.__setup.AbstractTest;
import com.leanplum.internal.Constants;
import com.leanplum.internal.JsonConverter;
import com.leanplum.internal.LeanplumInternal;
import com.leanplum.internal.Log;
import com.leanplum.internal.Util;
import com.leanplum.internal.VarCache;

import org.apache.maven.artifact.ant.shaded.IOUtil;
import org.junit.Assert;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * @author Ben Marten
 */
public class LeanplumActionContextTest extends AbstractTest {
    @Override
    public void after() {
        // do nothing
    }

    /**
     * Test different cases when a named Boolean is found. Expectation is no exception is thrown and
     * the boolean is returned.
     *
     * @throws Exception
     */
    @Test
    public void testBoolean() throws Exception {
        ActionContext actionContext = new ActionContext("name", new HashMap<String, Object>() {{
            put("1", true);
            put("2", false);
            put("3", false);
            put("4", "true");
            put("5", "false");
            put("6", "foo");
            put("7", 0);
            put("8", 1);
            put("9", 2);
            put("10", new Object());
        }}, "messageId");
        assertTrue(actionContext.booleanNamed("1"));
        assertFalse(actionContext.booleanNamed("2"));
        assertFalse(actionContext.booleanNamed("3"));
        assertTrue(actionContext.booleanNamed("4"));
        assertFalse(actionContext.booleanNamed("5"));
        assertFalse(actionContext.booleanNamed("6"));
        assertFalse(actionContext.booleanNamed("7"));
        assertTrue(actionContext.booleanNamed("8"));
        assertFalse(actionContext.booleanNamed("9"));
        assertFalse(actionContext.booleanNamed("10"));

        // Verify Log.exception method is never called.
        verifyStatic(never());
        Log.exception(any(Throwable.class));
    }

    /**
     * Test when a named Boolean is not found. Expectation is no exception is thrown and null is
     * returned.
     *
     * @throws Exception
     */
    @Test
    public void testUnknownBoolean() throws Exception {
        // Test Code
        ActionContext actionContext = new ActionContext("name", new HashMap<String, Object>() {{
        }}, "messageId");
        assertFalse(actionContext.booleanNamed(""));

        // Verify Log.exception method is never called.
        verifyStatic(never());
        Log.exception(any(Throwable.class));
    }

    /**
     * Test for messages chained to existing message
     * {@link ActionContext#isChainToExistingMessageStarted(Map, String)}.
     *
     * @throws Exception
     */
    @Test
    public void testIsChainToExistingMessageStarted() throws Exception {
        spy(VarCache.class);

        InputStream inputStream = getClass().getResourceAsStream("/test_files/single_message.json");
        Assert.assertNotNull(inputStream);
        String jsonMessages = IOUtil.toString(inputStream);
        // Messages will contains one message with messageId 1.
        Map<String, Object> testMessages = JsonConverter.fromJson(jsonMessages);
        doReturn(testMessages).when(VarCache.class, "messages");

        // Arguments for message chained to message with messageId 1.
        Map<String, Object> map = new HashMap<>();
        map.put("Chained message", "1");
        map.put("__name__", "Chain to Existing Message");

        ActionContext actionContext = new ActionContext("name", new HashMap<String, Object>() {{
        }}, "messageId");
        Method isChainToExistingMessageStartedMethod = ActionContext.class.
                getDeclaredMethod("isChainToExistingMessageStarted", Map.class, String.class);
        isChainToExistingMessageStartedMethod.setAccessible(true);

        // Test if VarCache.messages contains no message with messageId 1.
        boolean result = (Boolean) isChainToExistingMessageStartedMethod.invoke(actionContext, map,
                "Open Action");
        assertTrue(result);

        // Arguments for message chained to message with messageId 10.
        Map<String, Object> newMap = new HashMap<>();
        newMap.put("Chained message", "10");
        newMap.put("__name__", "Chain to Existing Message");
        // Test if VarCache.messages contains no message with messageId 10.
        result = (Boolean) isChainToExistingMessageStartedMethod.invoke(actionContext, newMap,
                "Open Action");
        assertFalse(result);

        // Test if VarCache.messages is null.
        doReturn(null).when(VarCache.class, "messages");
        result = (Boolean) isChainToExistingMessageStartedMethod.invoke(actionContext, map,
                "Open Action");
        assertFalse(result);

        // Test for null parameters.
        result = (Boolean) isChainToExistingMessageStartedMethod.invoke(actionContext, null,
                null);
        assertFalse(result);
    }

    @Test
    public void testActionArgs() {
        ActionArgs args = new ActionArgs();
        args.with(null, "default");
        assertTrue(args.getValue().size() == 0);

        args.withColor(null, 1000);
        assertTrue(args.getValue().size() == 0);

        args.withAction(null, "default");
        assertTrue(args.getValue().size() == 0);

        args.withFile(null, "default");
        assertTrue(args.getValue().size() == 0);

        args.withAsset(null, "default");
        assertTrue(args.getValue().size() == 0);
    }

    @Test
    public void testValues() throws Exception {
        ActionContext actionContext = new ActionContext("name", new HashMap<String, Object>() {{
            put("1", true);
            put("2", false);
            put("3", false);
            put("4", "true");
            put("5", "false");
            put("6", "foo");
            put("7", 0);
            put("8", 1);
            put("9", 2);
            put("10", new Object());
        }}, "messageId");

        assertNotNull(actionContext.objectNamed("1"));
        assertNull(actionContext.objectNamed("none"));
        assertNull(actionContext.objectNamed(null));

        assertNull(actionContext.stringNamed(null));
        assertNotNull(actionContext.stringNamed("6"));

        assertNull(actionContext.streamNamed("10"));

        assertNotNull(actionContext.numberNamed("7"));
        assertNull(actionContext.numberNamed(null));

        resumeLeanplumExceptionHandling();
        assertEquals(0.0, actionContext.numberNamed("6"));
    }

    @Test
    public void testMuteFutureMessages() {
        ActionContext actionContext = new ActionContext("name", new HashMap<String, Object>() {{
            put("1", true);
            put("2", false);
            put("3", false);
            put("4", "true");
            put("5", "false");
            put("6", "foo");
            put("7", 0);
            put("8", 1);
            put("9", 2);
            put("10", new Object());
        }}, "messageId");

        actionContext.muteFutureMessagesOfSameKind();

        SharedPreferences preferences = RuntimeEnvironment.application.getSharedPreferences(
                "__leanplum_messaging__", Context.MODE_PRIVATE);
        boolean muted = preferences.getBoolean(String.format(Constants.Defaults.MESSAGE_MUTED_KEY,
                "messageId"), false);
        assertTrue(muted);
    }

    @Test
    public void testTrack() {
        spy(LeanplumInternal.class);

        ActionContext actionContext = new ActionContext("name", new HashMap<String, Object>() {{
            put("1", true);
            put("2", false);
            put("3", false);
            put("4", "true");
            put("5", "false");
            put("6", "foo");
            put("7", 0);
            put("8", 1);
            put("9", 2);
            put("10", new Object());
        }}, "messageId");

        HashMap<String, Object> map = new HashMap<String, Object>() {{
            put("test", 5);
            put("test_2", 10);
        }};

        Map<String, String> requestArgs = new HashMap<>();
        requestArgs.put(Constants.Params.MESSAGE_ID, "messageId");

        actionContext.track("test_event", 0.0, map);
        verifyStatic(times(1));
        LeanplumInternal.track(
            eq("test_event"),
            eq(0.0),
            isNull(String.class),
            eq(map),
            eq(requestArgs));

        actionContext.track(null, 0.0, map);
        verifyStatic(never());
        LeanplumInternal.track(
            isNull(String.class),
            any(Double.class),
            any(String.class),
            any(Map.class),
            any(Map.class));
    }

    @Test
    public void testPrefetchingChainedMessage() {
        final String template = "{\"__name__\":\"Chain to Existing Message\",\"Chained message\":\"%d\"}";
        final long chainedMessageId = 1337;
        final String jsonData = String.format(template, chainedMessageId);

        // Should be null safe and return false.
        Assert.assertFalse(
                ActionContext.shouldForceContentUpdateForChainedMessage(JsonConverter.fromJson(null)));
        // Chained messageId should also be null.
        Assert.assertNull(ActionContext.getChainedMessageId(JsonConverter.fromJson(null)));
        // We should return true if we have a chained message that is not in our VarCache yet.
        Assert.assertTrue(ActionContext.shouldForceContentUpdateForChainedMessage(
                JsonConverter.fromJson(jsonData)));
        // Add chained message to VarCache.
        VarCache.applyVariableDiffs(null, new HashMap<>(
                ImmutableMap.<String, Object>of(Long.toString(chainedMessageId),
                ImmutableMap.<String, Object>of())), null, null, null);
        // Since it now exists locally, we should return false for forceContentUpdate.
        Assert.assertFalse(ActionContext.shouldForceContentUpdateForChainedMessage(
                JsonConverter.fromJson(jsonData)));
        // However, we should still get proper messageId.
        Assert.assertEquals(ActionContext.getChainedMessageId(JsonConverter.fromJson(jsonData)),
                Long.toString(chainedMessageId));
        VarCache.reset();
    }
}
