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

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Milos Jakovljevic
 */
public class LeanplumActionManagerTest extends AbstractTest {

  @Test
  public void testImpressions() {
    ActionManager.getInstance().recordMessageImpression("message##id");
    ActionManager.getInstance().recordMessageImpression("message##id");
    Map<String, Number> impressions = ActionManager.getInstance().getMessageImpressionOccurrences("message##id");
    assertEquals(4, impressions.size());
    assertEquals(0, impressions.get("min").intValue());
    assertEquals(1, impressions.get("max").intValue());
  }

  @Test
  public void testMessageTriggers() {
    ActionManager.getInstance().saveMessageTriggerOccurrences(5, "message##id");
    int occurences = ActionManager.getInstance().getMessageTriggerOccurrences("message##id");
    assertEquals(5, occurences);
  }
}