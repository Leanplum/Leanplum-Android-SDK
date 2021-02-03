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

import android.util.Log;

import com.leanplum.internal.RequestOld;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author Milos Jakovljevic
 */
public class RequestHelper extends RequestOld {
  private static RequestHandler sRequestHandler = null;

  public RequestHelper(String httpMethod, String apiMethod, Map<String, Object> params) {
    super(httpMethod, apiMethod, params);

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

  @Override
  public void sendEventually() {
    // workaround to send request now and not save it into prefs for later use
    try {
      Field sentField = RequestOld.class.getDeclaredField("sent");
      sentField.setAccessible(true);

      boolean sent = sentField.getBoolean(this);

      if (!sent) {
        super.sendEventually();
        super.sendIfConnected();
      }

    } catch (Exception e) {
      Log.e(RequestHelper.class.getSimpleName(), "Could not access private field \"sent\"");
    }
  }

  @Override
  protected void parseResponseBody(JSONObject responseBody, Exception error) {
    try {
      // attach the uuid we generated
      JSONArray jsonArray = responseBody.getJSONArray("response");
      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject jsonObject = jsonArray.getJSONObject(i);
        jsonObject.put("reqId", requestId());
      }
    } catch (Exception e) {
      // ignore
    }
    super.parseResponseBody(responseBody, error);
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
