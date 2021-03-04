/*
 * Copyright 2017, Leanplum, Inc. All rights reserved.
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

import com.leanplum.Leanplum;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.NonNull;

/**
 * LeanplumEventCallbackManager class to handle event callbacks.
 *
 * @author Anna Orlova
 */
class LeanplumEventCallbackManager {

  private final ConcurrentHashMap<String, LeanplumEventCallbacks> callbacks = new ConcurrentHashMap<>();

  /**
   * Add callbacks to the event callbacks Map.
   *
   * @param request Event.
   * @param responseCallback Response callback.
   * @param errorCallback Error callback.
   */
  void addCallbacks(Request request, Request.ResponseCallback responseCallback,
      Request.ErrorCallback errorCallback) {
    if (request == null) {
      return;
    }

    if (responseCallback == null && errorCallback == null) {
      return;
    }

    callbacks.put(request.getRequestId(), new LeanplumEventCallbacks(responseCallback, errorCallback));
  }

  /**
   * Invoke all callbacks for response body. Callbacks will be executed based on success flag for
   * each request
   *
   * @param body response body
   */
  void invokeCallbacks(JSONObject body) {
    if (body == null) {
      return;
    }

    if (callbacks.size() == 0) {
      return;
    }

    ArrayList<String> keys = new ArrayList<>();

    for (Map.Entry<String, LeanplumEventCallbacks> pair : callbacks.entrySet()) {
      final String reqId = pair.getKey();
      final LeanplumEventCallbacks callbacks = pair.getValue();

      if (reqId != null && callbacks != null) {
        // get the response for specified reqId
        final JSONObject response = RequestUtil.getResponseForId(body, reqId);
        if (response != null) {
          boolean isSuccess = RequestUtil.isResponseSuccess(response);

          // if response for event is successful, execute success callback
          if (isSuccess) {

            OperationQueue.sharedInstance().addParallelOperation(new Runnable() {
              @Override
              public void run() {
                if (callbacks.responseCallback != null) {
                  callbacks.responseCallback.response(response);
                }
              }
            });
          } else {
            // otherwise find the error message and execute error callback
            final String responseError = RequestUtil.getResponseError(response);
            final String msg = RequestUtil.getReadableErrorMessage(responseError);

            OperationQueue.sharedInstance().addParallelOperation(new Runnable() {
              @Override
              public void run() {
                if (callbacks.errorCallback != null) {
                  callbacks.errorCallback.error(new Exception(msg));
                }
              }
            });
          }

          // add key for removal
          keys.add(reqId);
        }
      }
    }

    // remove all keys for which callbacks were executed
    for (String key : keys) {
      callbacks.remove(key);
    }
  }

  /**
   * Invoke potential error callbacks for all events which have added callbacks.
   *
   * @param error Exception.
   */
  void invokeAllCallbacksWithError(@NonNull final Exception error) {
    if (callbacks.size() == 0) {
      return;
    }

    ArrayList<String> keys = new ArrayList<>();

    for (Map.Entry<String, LeanplumEventCallbacks> pair : callbacks.entrySet()) {
      String reqId = pair.getKey();
      final LeanplumEventCallbacks callbacks = pair.getValue();
      if (callbacks != null) {
        // executed all error callbacks in parallel
        OperationQueue.sharedInstance().addParallelOperation(new Runnable() {
          @Override
          public void run() {
            if (callbacks.errorCallback != null) {
              callbacks.errorCallback.error(error);
            }
          }
        });

        // add key for removal
        keys.add(reqId);
      }
    }

    // remove all keys for which callbacks were executed
    for (String key : keys) {
      callbacks.remove(key);
    }
  }

  private static class LeanplumEventCallbacks {
    private Request.ResponseCallback responseCallback;
    private Request.ErrorCallback errorCallback;

    LeanplumEventCallbacks(Request.ResponseCallback responseCallback, Request.ErrorCallback
        errorCallback) {
      this.responseCallback = responseCallback;
      this.errorCallback = errorCallback;
    }
  }
}
