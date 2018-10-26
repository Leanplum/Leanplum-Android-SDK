/*
 * Copyright 2014, Leanplum, Inc. All rights reserved.
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

import java.util.Map;

public class RequestFactory {

  private static final String API_METHOD_START = "start";
  private static final String API_METHOD_GET_VARS = "getVars";
  private static final String API_METHOD_SET_VARS = "setVars";
  private static final String API_METHOD_STOP = "stop";
  private static final String API_METHOD_RESTART = "restart";
  private static final String API_METHOD_TRACK = "track";
  private static final String API_METHOD_ADVANCE = "advance";
  private static final String API_METHOD_PAUSE_SESSION = "pauseSession";
  private static final String API_METHOD_PAUSE_STATE = "pauseState";
  private static final String API_METHOD_RESUME_SESSION = "resumeSession";
  private static final String API_METHOD_RESUME_STATE = "resumeState";
  private static final String API_METHOD_MULTI = "multi";
  private static final String API_METHOD_REGISTER_FOR_DEVELOPMENT = "registerDevice";
  private static final String API_METHOD_SET_USER_ATTRIBUTES = "setUserAttributes";
  private static final String API_METHOD_SET_DEVICE_ATTRIBUTES = "setDeviceAttributes";
  private static final String API_METHOD_SET_TRAFFIC_SOURCE_INFO = "setTrafficSourceInfo";
  private static final String API_METHOD_UPLOAD_FILE = "uploadFile";
  private static final String PI_METHOD_DOWNLOAD_FILE = "downloadFile";
  private static final String API_METHOD_HEARTBEAT = "heartbeat";
  private static final String API_METHOD_SAVE_VIEW_CONTROLLER_VERSION = "saveInterface";
  private static final String API_METHOD_SAVE_VIEW_CONTROLLER_IMAGE = "saveInterfaceImage";
  private static final String API_METHOD_GET_VIEW_CONTROLLER_VERSIONS_LIST = "getViewControllerVersionsList";
  private static final String API_METHOD_LOG = "log";
  private static final String API_METHOD_GET_INBOX_MESSAGES = "getNewsfeedMessages";
  private static final String API_METHOD_MARK_INBOX_MESSAGE_AS_READ = "markNewsfeedMessageAsRead";
  private static final String API_METHOD_DELETE_INBOX_MESSAGE = "deleteNewsfeedMessage";

  public static RequestFactory defaultFactory;

  public synchronized static RequestFactory getInstance() {
    if (defaultFactory == null) {
      defaultFactory = new RequestFactory();
    }
    return defaultFactory;
  }

  public Request createRequest(
      String httpMethod, String apiMethod, Map<String, Object> params) {
    Leanplum.countAggregator().incrementCount("createRequest");
    return new Request(httpMethod, apiMethod, params);
  }

  LPRequesting createGetForApiMethod(String apiMethod, Map<String, Object> params) {
    if (shouldReturnLPRequestClass()) {
      return LPRequest.get(apiMethod, params);
    }
    return Request.get(apiMethod, params);
  }

  LPRequesting createPostForApiMethod(String apiMethod, Map<String, Object> params) {
    if (shouldReturnLPRequestClass()) {
      return LPRequest.post(apiMethod, params);
    }
    return Request.post(apiMethod, params);
  }

  Boolean shouldReturnLPRequestClass() {
    return Leanplum.featureFlagManager().isFeatureFlagEnabled(Leanplum.featureFlagManager().FEATURE_FLAG_REQUEST_REFACTOR);
  }

}
