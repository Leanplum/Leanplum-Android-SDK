/*
 * Copyright 2013, Leanplum, Inc. All rights reserved.
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

import com.leanplum.callbacks.StartCallback;

import com.leanplum.internal.Request.RequestType;
import org.json.JSONObject;

public class Registration {
  public static void registerDevice(String email, final StartCallback callback) {
    Request request = RequestBuilder
        .withRegisterForDevelopmentAction()
        .andParam(Constants.Params.EMAIL, email)
        .andType(RequestType.IMMEDIATE)
        .create();

    request.onResponse(new Request.ResponseCallback() {
      @Override
      public void response(JSONObject response) {
        try {
          boolean success = RequestUtil.isResponseSuccess(response);
          callback.setSuccess(success);

          if (!success) {
            String error = RequestUtil.getResponseError(response);
            Log.e(error);
          }

          OperationQueue.sharedInstance().addUiOperation(callback);
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
    });

    request.onError(new Request.ErrorCallback() {
      @Override
      public void error(final Exception e) {
        callback.setSuccess(false);
        OperationQueue.sharedInstance().addUiOperation(callback);
      }
    });
    RequestSender.getInstance().send(request);
  }
}
