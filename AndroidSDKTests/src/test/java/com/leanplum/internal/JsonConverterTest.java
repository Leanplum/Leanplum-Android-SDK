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
package com.leanplum.internal;

import com.leanplum.__setup.LeanplumTestApp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests the internally used JsonConverter class.
 *
 * @author Aleksandar Gyorev, Ben Marten
 */
@SuppressWarnings("unused")
@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = 21,
    application = LeanplumTestApp.class
)
@PowerMockIgnore({
    "org.mockito.*",
    "org.robolectric.*",
    "org.json.*",
    "org.powermock.*",
    "jdk.internal.reflect.*"
})
public class JsonConverterTest {
  private static final Map<String, Object> map =
      CollectionUtil.newLinkedHashMap(
          "integer", 1,
          "string", "my string",
          "map", CollectionUtil.<String, Object>newLinkedHashMap(
              "a", 1,
              "b", 2,
              "array", Arrays.asList(1, 2, 3)
          ),
          "array", Arrays.asList(1, "string",
              CollectionUtil.<String, String>newLinkedHashMap("a", "b")
          )
      );
  private static final String json = "{\"integer\":1,\"string\":\"my string\"," +
      "\"map\":{\"a\":1,\"b\":2,\"array\":[1,2,3]},\"array\":[1,\"string\",{\"a\":\"b\"}]}";

  @Test
  public void testToJson() {
    assertNull(JsonConverter.toJson(null));
    assertEquals(json, JsonConverter.toJson(map));
  }

  @Test
  public void testFromJson() {
    assertNull(JsonConverter.fromJson(null));
    assertEquals(map, JsonConverter.fromJson(json));
  }
}
