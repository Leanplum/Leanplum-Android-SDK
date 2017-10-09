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

import android.view.View;

import com.leanplum.__setup.AbstractTest;
import com.leanplum.tests.R;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertNotNull;

/**
 * @author Milos Jakovljevic
 */
public class LeanplumInflaterTest extends AbstractTest {

  /**
   * Test Inflator configuration
   */
  @Test
  public void testInflater() {
    LeanplumInflater inflater = LeanplumInflater.from(RuntimeEnvironment.application.getApplicationContext());
    assertNotNull(inflater);
    assertNotNull(inflater.getLeanplumResources());
  }

  /**
   * Test inflate
   */
  @Test
  public void testInflate() {
    LeanplumInflater inflater = LeanplumInflater.from(RuntimeEnvironment.application.getApplicationContext());
    View root = inflater.inflate(R.layout.activity_main);
    assertNotNull(root);
  }
}
