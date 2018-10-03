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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Map;
import java.util.prefs.Preferences;

public class RequestFactory {
  private static final String REQUEST_ID_KEY = "LEANPLUM_REQUEST_ID_KEY";
  private static final int MAX_REQUEST_ID = 99999;

  public static RequestFactory defaultFactory;

  private Context context;
  private int requestId;

  public synchronized static RequestFactory getInstance(Context context) {
    if (defaultFactory == null) {
      defaultFactory = new RequestFactory(context);
      defaultFactory.requestId = defaultFactory.loadRequestIdFromDisk();
    }
    return defaultFactory;
  }

  public RequestFactory(Context context) {
    this.context = context;
  }

  public Request createRequest(
      String httpMethod, String apiMethod, Map<String, Object> params) {
    return new Request(httpMethod, apiMethod, params, incrementedRequestId());
  }

  private int incrementedRequestId() {
    requestId = (requestId + 1) % MAX_REQUEST_ID;
    saveRequestIdToDisk(requestId);
    return requestId;
  }

  private int loadRequestIdFromDisk() {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    return preferences.getInt(REQUEST_ID_KEY, 0);
  }

  private void saveRequestIdToDisk(int requestIdToSave) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putInt(REQUEST_ID_KEY, requestIdToSave);
  }


}
