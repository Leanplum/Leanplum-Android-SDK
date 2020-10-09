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

package com.leanplum.internal.http;

import android.net.Uri;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Log;
import com.leanplum.internal.RequestBuilder;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

/**
 * Handles common http operation (GET and POST).
 * Attaches the parameters to the connection object or url accordingly.
 */
public class NetworkOperation extends LeanplumHttpConnection {

  public NetworkOperation(
      String fullPath,
      String httpMethod,
      boolean useSSL,
      int timeoutSeconds) throws IOException {

    initConnection(fullPath, httpMethod, useSSL, timeoutSeconds);
  }

  public NetworkOperation(
      String hostName,
      String path,
      Map<String, Object> params,
      String httpMethod,
      boolean useSSL,
      int timeoutSeconds) throws IOException {

    if (RequestBuilder.GET.equals(httpMethod)) {
      path = attachGetParameters(path, params);
    }

    initConnection(hostName, path, httpMethod, useSSL, timeoutSeconds);

    if (RequestBuilder.POST.equals(httpMethod)) {
      attachPostParameters(params);
    }

    if (Constants.enableVerboseLoggingInDevelopmentMode
        && Constants.isDevelopmentModeEnabled) {
      Log.d("Sending request at path " + path + " with parameters " + params);
    }
  }

  /**
   * Converts and attaches GET parameters to specified path.
   *
   * @param path Path on which to attach parameters.
   * @param params Params to convert and attach.
   * @return Path with attached parameters.
   */
  private static String attachGetParameters(String path, Map<String, Object> params) {
    if (params == null) {
      return path;
    }
    Uri.Builder builder = Uri.parse(path).buildUpon();
    for (Map.Entry<String, Object> pair : params.entrySet()) {
      if (pair.getValue() == null) {
        continue;
      }
      builder.appendQueryParameter(pair.getKey(), pair.getValue().toString());
    }
    return builder.build().toString();
  }

  /**
   * Converts and writes POST parameters directly to an option http connection.
   *
   * @param params Params to write in connection.
   * @throws IOException Throws in case it fails.
   */
  private void attachPostParameters(Map<String, Object> params) throws IOException {
    OutputStream os = urlConnection.getOutputStream();
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
    String query = getQuery(params);
    writer.write(query);
    writer.close();
    os.close();
  }

  /**
   * Builds a query from Map containing parameters.
   *
   * @param params Params used to build a query.
   * @return Query string or empty string in case params are null.
   */
  private static String getQuery(Map<String, Object> params) {
    if (params == null) {
      return "";
    }
    Uri.Builder builder = new Uri.Builder();
    for (Map.Entry<String, Object> pair : params.entrySet()) {
      if (pair.getValue() == null) {
        Log.d("Request parameter for key: " + pair.getKey() + " is null.");
        continue;
      }
      builder.appendQueryParameter(pair.getKey(), pair.getValue().toString());
    }
    return builder.build().getEncodedQuery();
  }

}
