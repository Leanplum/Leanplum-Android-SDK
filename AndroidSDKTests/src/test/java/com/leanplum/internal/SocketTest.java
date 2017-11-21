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

package com.leanplum.internal;

import com.leanplum.Var;
import com.leanplum.__setup.LeanplumTestApp;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests for {@link Socket} class.
 *
 * @author Lev Neiman
 */
@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = 16,
    application = LeanplumTestApp.class
)
@PowerMockIgnore({
    "org.mockito.*",
    "org.robolectric.*",
    "org.json.*",
    "org.powermock.*",
    "android.*"
})
public class SocketTest {
  @Test
  public void testApplyVars() throws Exception {
    // Given:  We have a string variable foo -> baz
    VarCache.registerVariable(Var.define("foo", "baz"));
    Var<String> val =  VarCache.<String>getVariable("foo");
    Assert.assertEquals("baz", val.stringValue);

    String jsonString = "[{\"foo\":\"bar\"}]";

    // When:  Socket calls handleApplyVarsEvent with JSON array of foo -> bar
    Socket.handleApplyVarsEvent(new JSONArray(jsonString));

    // Then:  VarCache now contains new mapping of foo -> bar
    val = VarCache.<String>getVariable("foo");
    Assert.assertNotNull(val);
    Assert.assertEquals("bar", val.stringValue);
  }
}
