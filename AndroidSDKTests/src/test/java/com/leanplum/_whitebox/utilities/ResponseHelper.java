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

import com.leanplum.internal.Log;
import com.leanplum.internal.Util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.mockito.Matchers.anyObject;
import static org.powermock.api.mockito.PowerMockito.doReturn;

/**
 * @author Milos Jakovljevic
 */
public class ResponseHelper {
  /**
   * Parses the response from the file and seeds it to a url connection.
   *
   * @param filename Filename to open.
   * @return Parsed string.
   * @throws Exception
   */
  private static String parseResponse(String filename) throws Exception {
    InputStream inputStream = ResponseHelper.class.getResourceAsStream(filename);
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    StringBuilder builder = new StringBuilder();
    for (String line; (line = reader.readLine()) != null; ) {
      builder.append(line).append("\n");
    }
    return builder.toString();
  }

  /**
   * Seeds responds as input stream.
   *
   * @param filename File to seed.
   * @return Input stream of the file.
   */
  @SuppressWarnings("SameParameterValue")
  public static InputStream seedInputStream(String filename) {
    return ResponseHelper.class.getResourceAsStream(filename);
  }

  /**
   * Seeds the response to Util.getResponse method.
   *
   * @param filename File name of response to seed.
   */
  public static void seedResponse(String filename) {
    try {
      doReturn(parseResponse(filename)).when(Util.class, "getResponse", anyObject());
    } catch (Exception e) {
      Log.e("ResponseHelper", "Unable to seed response from file: " + filename);
    }
  }
}
