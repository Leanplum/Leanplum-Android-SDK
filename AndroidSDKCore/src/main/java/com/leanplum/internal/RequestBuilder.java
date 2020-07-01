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
import java.util.HashMap;
import java.util.Map;

public class RequestBuilder {
  private static final String GET = "GET";
  private static final String POST = "POST";

  private static final String API_METHOD_START = "start";
  private static final String API_METHOD_GET_VARS = "getVars";
  private static final String API_METHOD_SET_VARS = "setVars";
  private static final String API_METHOD_STOP = "stop";
  private static final String API_METHOD_RESTART = "restart"; // TODO remove?
  private static final String API_METHOD_TRACK = "track";
  private static final String API_METHOD_TRACK_GEOFENCE = "trackGeofence";
  private static final String API_METHOD_ADVANCE = "advance";
  private static final String API_METHOD_PAUSE_SESSION = "pauseSession";
  private static final String API_METHOD_PAUSE_STATE = "pauseState";
  private static final String API_METHOD_RESUME_SESSION = "resumeSession";
  private static final String API_METHOD_RESUME_STATE = "resumeState";
  private static final String API_METHOD_MULTI = "multi"; // TODO remove?
  private static final String API_METHOD_REGISTER_FOR_DEVELOPMENT = "registerDevice";
  private static final String API_METHOD_SET_USER_ATTRIBUTES = "setUserAttributes";
  private static final String API_METHOD_SET_DEVICE_ATTRIBUTES = "setDeviceAttributes";
  private static final String API_METHOD_SET_TRAFFIC_SOURCE_INFO = "setTrafficSourceInfo";
  private static final String API_METHOD_UPLOAD_FILE = "uploadFile";
  private static final String API_METHOD_DOWNLOAD_FILE = "downloadFile";
  private static final String API_METHOD_HEARTBEAT = "heartbeat";
  private static final String API_METHOD_LOG = "log";
  private static final String API_METHOD_GET_INBOX_MESSAGES = "getNewsfeedMessages";
  private static final String API_METHOD_MARK_INBOX_MESSAGE_AS_READ = "markNewsfeedMessageAsRead";
  private static final String API_METHOD_DELETE_INBOX_MESSAGE = "deleteNewsfeedMessage";

  private String httpMethod;
  private String apiAction;
  private Map<String, Object> params;

  private RequestBuilder(String httpMethod, String apiAction) {
    this.httpMethod = httpMethod;
    this.apiAction = apiAction;
  }

  public static RequestBuilder withStartAction() {
    return new RequestBuilder(POST, API_METHOD_START);
  }

  public static RequestBuilder withGetVarsAction() {
    return new RequestBuilder(POST, API_METHOD_GET_VARS);
  }

  public static RequestBuilder withSetVarsAction() {
    return new RequestBuilder(POST, API_METHOD_SET_VARS);
  }

  public static RequestBuilder withStopAction() {
    return new RequestBuilder(POST, API_METHOD_STOP);
  }

  public static RequestBuilder withRestartAction() {
    return new RequestBuilder(POST, API_METHOD_RESTART);
  }

  public static RequestBuilder withTrackAction() {
    return new RequestBuilder(POST, API_METHOD_TRACK);
  }

  public static RequestBuilder withTrackGeofenceAction() {
    return new RequestBuilder(POST, API_METHOD_TRACK_GEOFENCE);
  }

  public static RequestBuilder withAdvanceAction() {
    return new RequestBuilder(POST, API_METHOD_ADVANCE);
  }

  public static RequestBuilder withPauseSessionAction() {
    return new RequestBuilder(POST, API_METHOD_PAUSE_SESSION);
  }

  public static RequestBuilder withPauseStateAction() {
    return new RequestBuilder(POST, API_METHOD_PAUSE_STATE);
  }

  public static RequestBuilder withResumeSessionAction() {
    return new RequestBuilder(POST, API_METHOD_RESUME_SESSION);
  }

  public static RequestBuilder withResumeStateAction() {
    return new RequestBuilder(POST, API_METHOD_RESUME_STATE);
  }

  public static RequestBuilder withMultiAction() {
    return new RequestBuilder(POST, API_METHOD_MULTI);
  }

  public static RequestBuilder withRegisterForDevelopmentAction() {
    return new RequestBuilder(POST, API_METHOD_REGISTER_FOR_DEVELOPMENT);
  }

  public static RequestBuilder withSetUserAttributesAction() {
    return new RequestBuilder(POST, API_METHOD_SET_USER_ATTRIBUTES);
  }

  public static RequestBuilder withSetDeviceAttributesAction() {
    return new RequestBuilder(POST, API_METHOD_SET_DEVICE_ATTRIBUTES);
  }

  public static RequestBuilder withSetTrafficSourceInfoAction() {
    return new RequestBuilder(POST, API_METHOD_SET_TRAFFIC_SOURCE_INFO);
  }

  public static RequestBuilder withUploadFileAction() {
    return new RequestBuilder(POST, API_METHOD_UPLOAD_FILE);
  }

  public static RequestBuilder withDownloadFileAction() {
    return new RequestBuilder(POST, API_METHOD_DOWNLOAD_FILE);
  }

  public static RequestBuilder withHeartbeatAction() {
    return new RequestBuilder(POST, API_METHOD_HEARTBEAT);
  }

  public static RequestBuilder withLogAction() {
    return new RequestBuilder(POST, API_METHOD_LOG);
  }

  public static RequestBuilder withGetInboxMessagesAction() {
    return new RequestBuilder(POST, API_METHOD_GET_INBOX_MESSAGES);
  }

  public static RequestBuilder withMarkInboxMessageAsReadAction() {
    return new RequestBuilder(POST, API_METHOD_MARK_INBOX_MESSAGE_AS_READ);
  }

  public static RequestBuilder withDeleteInboxMessageAction() {
    return new RequestBuilder(POST, API_METHOD_DELETE_INBOX_MESSAGE);
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

  public RequestOld create() {
    Log.LeanplumLogType level = Log.LeanplumLogType.VERBOSE;
    if (Constants.Methods.LOG.equals(apiAction))
        level = Log.LeanplumLogType.DEBUG;
    Log.log(level, "Will call API method " + apiAction + " with arguments " + params);

    if (GET.equals(this.httpMethod))
      Leanplum.countAggregator().incrementCount("get_request");
    else if (POST.equals(this.httpMethod))
      Leanplum.countAggregator().incrementCount("post_request");

    Leanplum.countAggregator().incrementCount("createRequest");
    return RequestFactory.getInstance().createRequest(httpMethod, apiAction, params);
  }
}
