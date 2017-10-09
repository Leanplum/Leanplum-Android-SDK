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

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unit Test class for CollectionUtil.
 *
 * @author Ben Marten
 */
public class CollectionUtilTest extends TestCase {
  /**
   * Tests the newArrayList method.
   */
  public static void testNewArrayList() {
    assertTrue(CollectionUtil.newArrayList(null, null).size() == 2);
    List emptyList = CollectionUtil.newArrayList();
    assertNotNull(emptyList);
    assertTrue(emptyList.size() == 0);
    assertTrue(CollectionUtil.newArrayList("").size() == 1);
    assertTrue(CollectionUtil.newArrayList("", "").size() == 2);
  }

  /**
   * Tests the newHashSet method.
   */
  public static void testNewHashSet() {
    assertTrue(CollectionUtil.newHashSet(null, null).size() == 1);
    Set emptySet = CollectionUtil.newHashSet();
    assertNotNull(emptySet);
    assertTrue(emptySet.size() == 0);
    assertTrue(CollectionUtil.newHashSet("").size() == 1);
    assertTrue(CollectionUtil.newHashSet("", "").size() == 1);
    assertTrue(CollectionUtil.newHashSet("a", "b").size() == 2);
  }

  /**
   * Tests the newHashMap method.
   */
  public static void testNewHashMap() {
    assertTrue(CollectionUtil.newHashMap(null, null).size() == 1);
    Map emptyMap = CollectionUtil.newHashMap();
    assertNotNull(emptyMap);
    assertTrue(HashMap.class.equals(emptyMap.getClass()));
    assertTrue(emptyMap.size() == 0);
    assertTrue(CollectionUtil.newHashMap("key", "value").size() == 1);
    assertTrue(CollectionUtil.newHashMap("key", "value").containsKey("key"));
    assertTrue(CollectionUtil.newHashMap("key", "value").containsValue("value"));
  }

  /**
   * Tests the newLinkedHashMap method.
   */
  public static void testNewLinkedHashMap() {
    assertTrue(CollectionUtil.newLinkedHashMap(null, null).size() == 1);
    Map emptyMap = CollectionUtil.newLinkedHashMap();
    assertNotNull(emptyMap);
    assertTrue(LinkedHashMap.class.equals(emptyMap.getClass()));
    assertTrue(emptyMap.size() == 0);
    assertTrue(CollectionUtil.newLinkedHashMap("key", "value").size() == 1);
    assertTrue(CollectionUtil.newLinkedHashMap("key", "value").containsKey("key"));
    assertTrue(CollectionUtil.newLinkedHashMap("key", "value").containsValue("value"));
  }

  /**
   * Tests the concatenateList method.
   */
  public static void testConcatenateList() {
    assertEquals(null, CollectionUtil.concatenateList(null, null));
    assertEquals("", CollectionUtil.concatenateList(CollectionUtil.newArrayList(), null));
    assertEquals("ab", CollectionUtil.concatenateList(CollectionUtil.newArrayList("a", "b"), null));
    assertEquals("", CollectionUtil.concatenateList(CollectionUtil.newArrayList(), " - "));
    assertEquals("a - b", CollectionUtil.concatenateList(CollectionUtil.newArrayList("a", "b"),
        " - "));
  }

  /**
   * Tests the concatenateArray method, which is essentially using concatenateList.
   */
  public static void testConcatenateArray() {
    assertEquals(null, CollectionUtil.concatenateArray(null, null));
    assertEquals("ab", CollectionUtil.concatenateArray(new String[] {"a", "b"}, null));
  }
}
