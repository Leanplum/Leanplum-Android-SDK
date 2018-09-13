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

import com.leanplum.__setup.AbstractTest;

import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Grace Gu
 */
public class LPCountAggregatorTest extends AbstractTest {

  @Test
  public void testIncrementDisabledCount() {
    LPCountAggregator countAggregator = new LPCountAggregator();
    String testString = "test";

    countAggregator.incrementCount(testString);
    HashMap<String, Integer> counts = countAggregator.getCounts();
    int count = counts.containsKey(testString) ? counts.get(testString) : 0;
    assertEquals(0, count);

    countAggregator.incrementCount(testString);
    counts = countAggregator.getCounts();
    count = counts.containsKey(testString) ? counts.get(testString) : 0;
    assertEquals(0, count);

  }

  @Test
  public void testIncrementCount() {
    LPCountAggregator countAggregator = new LPCountAggregator();
    String testString = "test";
    HashSet<String> testSet = new HashSet<String>(Arrays.asList(testString));
    countAggregator.setEnabledCounters(testSet);

    countAggregator.incrementCount(testString);
    HashMap<String, Integer> counts = countAggregator.getCounts();
    int count = counts.containsKey(testString) ? counts.get(testString) : 0;
    assertEquals(1, count);

    countAggregator.incrementCount(testString);
    counts = countAggregator.getCounts();
    count = counts.containsKey(testString) ? counts.get(testString) : 0;
    assertEquals(2, count);

  }

  @Test
  public void testIncrementDisabledCountMultiple() {
    LPCountAggregator countAggregator = new LPCountAggregator();
    String testString = "test";

    countAggregator.incrementCount(testString, 2);
    HashMap<String, Integer> counts = countAggregator.getCounts();
    int count = counts.containsKey(testString) ? counts.get(testString) : 0;
    assertEquals(0, count);

    countAggregator.incrementCount(testString, 15);
    counts = countAggregator.getCounts();
    count = counts.containsKey(testString) ? counts.get(testString) : 0;
    assertEquals(0, count);

  }

  @Test
  public void testIncrementCountMultiple() {
    LPCountAggregator countAggregator = new LPCountAggregator();
    String testString = "test";
    HashSet<String> testSet = new HashSet<String>(Arrays.asList(testString));
    countAggregator.setEnabledCounters(testSet);

    countAggregator.incrementCount(testString, 2);
    HashMap<String, Integer> counts = countAggregator.getCounts();
    int count = counts.containsKey(testString) ? counts.get(testString) : 0;
    assertEquals(2, count);

    countAggregator.incrementCount(testString, 15);
    counts = countAggregator.getCounts();
    count = counts.containsKey(testString) ? counts.get(testString) : 0;
    assertEquals(17, count);

  }
}