/*
 * Copyright 2020, Leanplum, Inc. All rights reserved.
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

import com.leanplum.internal.Constants.Params;
import com.leanplum.internal.RequestOld;
import com.leanplum.internal.RequestSender;
import org.json.JSONArray;
import org.json.JSONObject;

public class ImmediateRequestSender extends RequestSender {

  private String currentRequestId;

  @Override
  public void sendEventually(RequestOld request) {
    currentRequestId = request.requestId();
    super.sendEventually(request);

    // immediately send current request
    sendRequests();
  }

  @Override
  protected void triggerCallbackManager(JSONObject responseBody, Exception error) {
    try {
      // attach the uuid we generated
      JSONArray jsonArray = responseBody.getJSONArray(Params.RESPONSE);
      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject jsonObject = jsonArray.getJSONObject(i);
        jsonObject.put(Params.REQUEST_ID, currentRequestId);
      }
    } catch (Exception e) {
      // ignore
    }
    super.triggerCallbackManager(responseBody, error);
  }
}
