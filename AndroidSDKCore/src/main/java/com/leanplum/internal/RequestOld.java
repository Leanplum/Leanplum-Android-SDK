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

import com.leanplum.Leanplum;

import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Leanplum request class.
 *
 * @author Andrew First
 */
public class RequestOld {

  private String requestId = UUID.randomUUID().toString();

  private final String httpMethod;
  private final String apiAction;
  private final Map<String, Object> params;
  ResponseCallback response;
  ErrorCallback error;
  private boolean saved;

  public String requestId() {
    return requestId;
  }

  public RequestOld(String httpMethod, String apiAction, Map<String, Object> params) {
    this.httpMethod = httpMethod;
    this.apiAction = apiAction;
    this.params = params != null ? params : new HashMap<String, Object>();
    // Check if it is error and here was SQLite exception.
    if (RequestBuilder.ACTION_LOG.equals(apiAction) && LeanplumEventDataManager.sharedInstance().willSendErrorLogs()) {
      RequestSender.getInstance().addLocalError(this);
    }
  }

  public void onResponse(ResponseCallback response) {
    this.response = response;
    Leanplum.countAggregator().incrementCount("on_response");
  }

  public void onError(ErrorCallback error) {
    this.error = error;
    Leanplum.countAggregator().incrementCount("on_error");
  }

  public Map<String, Object> createArgsDictionary() {
    Map<String, Object> args = new HashMap<>();
    args.put(Constants.Params.DEVICE_ID, APIConfig.getInstance().deviceId());
    args.put(Constants.Params.USER_ID, APIConfig.getInstance().userId());
    args.put(Constants.Params.ACTION, apiAction);
    args.put(Constants.Params.SDK_VERSION, Constants.LEANPLUM_VERSION);
    args.put(Constants.Params.DEV_MODE, Boolean.toString(Constants.isDevelopmentModeEnabled));
    args.put(Constants.Params.TIME, Double.toString(new Date().getTime() / 1000.0));
    args.put(Constants.Params.REQUEST_ID, requestId);
    String token = APIConfig.getInstance().token();
    if (token != null) {
      args.put(Constants.Params.TOKEN, token);
    }
    args.putAll(params);
    return args;
  }

  public interface ResponseCallback {
    void response(JSONObject response);
  }

  public interface ErrorCallback {
    void error(Exception e);
  }

  public void setSaved(boolean saved) {
    this.saved = saved;
  }

  public boolean isSaved() {
    return saved;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public String getApiAction() {
    return apiAction;
  }

  public Map<String, Object> getParams() {
    return params;
  }
}
