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

package com.leanplum.internal;

import androidx.annotation.VisibleForTesting;
import com.leanplum.internal.Request.RequestType;
import java.util.HashMap;
import java.util.Map;

public class RequestBuilder {
  public static final String GET = "GET";
  public static final String POST = "POST";

  public static final String ACTION_START = "start";
  public static final String ACTION_GET_VARS = "getVars";
  public static final String ACTION_SET_VARS = "setVars";
  public static final String ACTION_STOP = "stop";
  public static final String ACTION_RESTART = "restart";
  public static final String ACTION_TRACK = "track";
  public static final String ACTION_TRACK_GEOFENCE = "trackGeofence";
  public static final String ACTION_ADVANCE = "advance";
  public static final String ACTION_PAUSE_SESSION = "pauseSession";
  public static final String ACTION_PAUSE_STATE = "pauseState";
  public static final String ACTION_RESUME_SESSION = "resumeSession";
  public static final String ACTION_RESUME_STATE = "resumeState";
  public static final String ACTION_MULTI = "multi";
  public static final String ACTION_REGISTER_FOR_DEVELOPMENT = "registerDevice";
  public static final String ACTION_SET_USER_ATTRIBUTES = "setUserAttributes";
  public static final String ACTION_SET_DEVICE_ATTRIBUTES = "setDeviceAttributes";
  public static final String ACTION_SET_TRAFFIC_SOURCE_INFO = "setTrafficSourceInfo";
  public static final String ACTION_UPLOAD_FILE = "uploadFile";
  public static final String ACTION_DOWNLOAD_FILE = "downloadFile";
  public static final String ACTION_HEARTBEAT = "heartbeat";
  public static final String ACTION_LOG = "log";
  public static final String ACTION_GET_INBOX_MESSAGES = "getNewsfeedMessages";
  public static final String ACTION_MARK_INBOX_MESSAGE_AS_READ = "markNewsfeedMessageAsRead";
  public static final String ACTION_DELETE_INBOX_MESSAGE = "deleteNewsfeedMessage";

  private String httpMethod;
  private String apiAction;
  private RequestType type = RequestType.DEFAULT;
  private Map<String, Object> params;

  @VisibleForTesting
  protected RequestBuilder(String httpMethod, String apiAction) {
    this.httpMethod = httpMethod;
    this.apiAction = apiAction;
  }

  public static RequestBuilder withStartAction() {
    return new RequestBuilder(POST, ACTION_START);
  }

  public static RequestBuilder withGetVarsAction() {
    return new RequestBuilder(POST, ACTION_GET_VARS);
  }

  public static RequestBuilder withSetVarsAction() {
    return new RequestBuilder(POST, ACTION_SET_VARS);
  }

  public static RequestBuilder withStopAction() {
    return new RequestBuilder(POST, ACTION_STOP);
  }

  public static RequestBuilder withRestartAction() {
    return new RequestBuilder(POST, ACTION_RESTART);
  }

  public static RequestBuilder withTrackAction() {
    return new RequestBuilder(POST, ACTION_TRACK);
  }

  public static RequestBuilder withTrackGeofenceAction() {
    return new RequestBuilder(POST, ACTION_TRACK_GEOFENCE);
  }

  public static RequestBuilder withAdvanceAction() {
    return new RequestBuilder(POST, ACTION_ADVANCE);
  }

  public static RequestBuilder withPauseSessionAction() {
    return new RequestBuilder(POST, ACTION_PAUSE_SESSION);
  }

  public static RequestBuilder withPauseStateAction() {
    return new RequestBuilder(POST, ACTION_PAUSE_STATE);
  }

  public static RequestBuilder withResumeSessionAction() {
    return new RequestBuilder(POST, ACTION_RESUME_SESSION);
  }

  public static RequestBuilder withResumeStateAction() {
    return new RequestBuilder(POST, ACTION_RESUME_STATE);
  }

  public static RequestBuilder withMultiAction() {
    return new RequestBuilder(POST, ACTION_MULTI);
  }

  public static RequestBuilder withRegisterForDevelopmentAction() {
    return new RequestBuilder(POST, ACTION_REGISTER_FOR_DEVELOPMENT);
  }

  public static RequestBuilder withSetUserAttributesAction() {
    return new RequestBuilder(POST, ACTION_SET_USER_ATTRIBUTES);
  }

  public static RequestBuilder withSetDeviceAttributesAction() {
    return new RequestBuilder(POST, ACTION_SET_DEVICE_ATTRIBUTES);
  }

  public static RequestBuilder withSetTrafficSourceInfoAction() {
    return new RequestBuilder(POST, ACTION_SET_TRAFFIC_SOURCE_INFO);
  }

  public static RequestBuilder withUploadFileAction() {
    return new RequestBuilder(POST, ACTION_UPLOAD_FILE);
  }

  public static RequestBuilder withDownloadFileAction() {
    return new RequestBuilder(GET, ACTION_DOWNLOAD_FILE);
  }

  public static RequestBuilder withHeartbeatAction() {
    return new RequestBuilder(POST, ACTION_HEARTBEAT);
  }

  public static RequestBuilder withLogAction() {
    return new RequestBuilder(POST, ACTION_LOG);
  }

  public static RequestBuilder withGetInboxMessagesAction() {
    return new RequestBuilder(POST, ACTION_GET_INBOX_MESSAGES);
  }

  public static RequestBuilder withMarkInboxMessageAsReadAction() {
    return new RequestBuilder(POST, ACTION_MARK_INBOX_MESSAGE_AS_READ);
  }

  public static RequestBuilder withDeleteInboxMessageAction() {
    return new RequestBuilder(POST, ACTION_DELETE_INBOX_MESSAGE);
  }

  public RequestBuilder andParam(String param, Object value) {
    if (params == null)
      params = new HashMap<>();
    params.put(param, value);
    return this;
  }

  public RequestBuilder andParams(Map<String, Object> params) {
    if (this.params == null)
      this.params = params;
    else
      this.params.putAll(params);
    return this;
  }

  public RequestBuilder andType(RequestType type) {
    this.type = type;
    return this;
  }

  public Request create() {
    Log.d("Will call API method: %s with params: %s", apiAction, params);
    return RequestFactory.getInstance().createRequest(httpMethod, apiAction, type, params);
  }

  @VisibleForTesting
  public String getHttpMethod() {
    return httpMethod;
  }

  @VisibleForTesting
  public String getApiAction() {
    return apiAction;
  }

  @VisibleForTesting
  public Map<String, Object> getParams() {
    return params;
  }

  @VisibleForTesting
  public RequestType getType() {
    return type;
  }
}
