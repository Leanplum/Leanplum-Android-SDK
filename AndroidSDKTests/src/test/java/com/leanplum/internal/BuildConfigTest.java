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

package com.leanplum.internal;

import com.leanplum.__setup.AbstractTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests settings that are built from gradle.
 */
public class BuildConfigTest extends AbstractTest {
  /**
   * Test that a valid version is checked in.
   */
  @Test
  public void testVersion() {
    String version = Constants.LEANPLUM_VERSION;

    String[] parts = version.split("\\.");

    assertEquals(3, parts.length);
    // Ensure we can parse and do not go down in major version.
    int major = Integer.parseInt(parts[0]);
    assert(major >= 4);

    // Ensure minor and patch versions are parsable.
    int minor = Integer.parseInt(parts[1]);
    assert(minor >= 0);

    if (!isBeta(version)) {
      int patch = Integer.parseInt(parts[2]);
      assert(patch >= 0);
    }
  }

  private boolean isBeta(String version) {
    return version.contains("beta");
  }
}
