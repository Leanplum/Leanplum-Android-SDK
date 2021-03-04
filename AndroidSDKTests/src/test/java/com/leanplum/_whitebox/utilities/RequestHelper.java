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

import com.leanplum.internal.Request;

import java.util.Map;

/**
 * @author Milos Jakovljevic
 */
public class RequestHelper extends Request {
  private static RequestHandler sRequestHandler = null;

  public RequestHelper(
      String httpMethod,
      String apiMethod,
      RequestType type,
      Map<String, Object> params) {
    super(httpMethod, apiMethod, type, params);

    // execute handler with request params
    if (sRequestHandler != null) {
      sRequestHandler.onRequest(httpMethod, apiMethod, params);
      sRequestHandler = null;
    }
  }

  /**
   * Adds request handler to be executed when sdk makes a request Handler will be automatically
   * removed after it is called
   *
   * @param handler handler to execute
   */
  public static void addRequestHandler(RequestHandler handler) {
    sRequestHandler = handler;
  }

  /**
   * Used to validate a request
   */
  public interface RequestHandler {
    /**
     * Called to validate request that will be made to a server
     *
     * @param httpMethod http method of the request
     * @param apiMethod api method that will be called
     * @param params params to send
     */
    @SuppressWarnings("UnusedParameters")
    void onRequest(String httpMethod, String apiMethod, Map<String, Object> params);
  }
}
