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
package com.leanplum._whitebox.utilities;

import com.leanplum.annotations.Variable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test class holding variables to be used with parser.
 *
 * @author Milos Jakovljevic
 */
public class VariablesTestClass {
  @Variable
  public static final String stringVariable = "test_string";

  @Variable(name = "Boolean Variable")
  public static final boolean booleanVariable = false;

  @Variable(group = "numbers")
  public static final float floatVariable = 5.0f;

  @Variable(name = "VariablesTestClass.doubleVariable")
  public static final double doubleVariable = 10.0;

  @Variable
  public static final List<Integer> listVariable = Arrays.asList(1, 2, 3, 4, 5);

  @Variable
  public static final Map<String, Object> dictionaryVariable = new HashMap<String, Object>() {
    {
      put("test_string", "string");
      put("test_int", 10);
      put("test_double", 10.0);
      put("test_float", 10.0f);
      put("test_array", Arrays.asList(1, 2, 3));
    }
  };
}
