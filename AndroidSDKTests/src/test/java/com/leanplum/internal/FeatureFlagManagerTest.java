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

import java.util.HashSet;

import static org.junit.Assert.assertEquals;

/**
 * @author Grace Gu
 */

public class FeatureFlagManagerTest extends AbstractTest {
    @Test
    public void testIsFeatureFlagEnabledShouldBeTrueForEnabledFlag() {
        FeatureFlagManager featureFlagManager = new FeatureFlagManager();
        String testString = "test";
        HashSet<String> enabledFeatureFlags = new HashSet<>();
        enabledFeatureFlags.add(testString);
        featureFlagManager.setEnabledFeatureFlags(enabledFeatureFlags);
        assertEquals(true, featureFlagManager.isFeatureFlagEnabled(testString));
    }

    @Test
    public void testIsFeatureFlagEnabledShouldBeFalseForDisabledFlag() {
        FeatureFlagManager featureFlagManager = new FeatureFlagManager();
        String testString = "test";
        assertEquals(false, featureFlagManager.isFeatureFlagEnabled(testString));
    }

    @Test
    public void testIsFeatureFlagEnabledShouldResetWhenSetToEmpty() {
        FeatureFlagManager featureFlagManager = new FeatureFlagManager();
        String testString = "test";
        HashSet<String> enabledFeatureFlags = new HashSet<>();
        enabledFeatureFlags.add(testString);
        featureFlagManager.setEnabledFeatureFlags(enabledFeatureFlags);
        assertEquals(true, featureFlagManager.isFeatureFlagEnabled(testString));
        enabledFeatureFlags = new HashSet<>();
        featureFlagManager.setEnabledFeatureFlags(enabledFeatureFlags);
        assertEquals(false, featureFlagManager.isFeatureFlagEnabled(testString));
    }
}
