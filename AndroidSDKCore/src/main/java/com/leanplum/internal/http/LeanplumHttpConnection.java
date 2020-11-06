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

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import com.leanplum.Leanplum;
import com.leanplum.internal.APIConfig;
import com.leanplum.internal.Constants;
import com.leanplum.internal.RequestBuilder;
import com.leanplum.internal.Util;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Wrapper class around HttpURLConnection for Leanplum needs.
 */
public abstract class LeanplumHttpConnection {

  protected HttpURLConnection urlConnection;

  protected void initConnection(
      String hostName,
      String path,
      String httpMethod,
      boolean useSSL,
      int timeoutSeconds) throws IOException {

    String fullPath;
    if (path != null && path.startsWith("http")) {
      fullPath = path;
    } else {
      fullPath = (useSSL ? "https://" : "http://") + hostName + "/" + path;
    }

    initConnection(fullPath, httpMethod, useSSL, timeoutSeconds);
  }

  protected void initConnection(
      String fullPath,
      String httpMethod,
      boolean useSSL,
      int timeoutSeconds) throws IOException {

    URL url = new URL(fullPath);
    urlConnection = (HttpURLConnection) url.openConnection();
    if (useSSL) {
      SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
      ((HttpsURLConnection) urlConnection).setSSLSocketFactory(socketFactory);
    }
    urlConnection.setReadTimeout(timeoutSeconds * 1000);
    urlConnection.setConnectTimeout(timeoutSeconds * 1000);
    urlConnection.setRequestMethod(httpMethod);
    urlConnection.setDoOutput(RequestBuilder.POST.equals(httpMethod));
    urlConnection.setDoInput(true);
    urlConnection.setUseCaches(false);
    urlConnection.setInstanceFollowRedirects(true);

    /*
      Must include `Accept-Encoding: gzip` in the header
      Must include the phrase `gzip` in the `User-Agent` header
      https://cloud.google.com/appengine/kb/
    */
    urlConnection.setRequestProperty("User-Agent", createUserAgent());
    urlConnection.setRequestProperty("Accept-Encoding", Constants.LEANPLUM_SUPPORTED_ENCODING);
  }

  /**
   * Currently Android uses OkHttp as an HTTP client. We need to remove invalid characters from
   * the User-Agent value according to checkValue(String, String) from:
   *
   * https://github.com/square/okhttp/blob/dabbd56572089cfef00d358edcc87b3f5c73e580/okhttp/src/main/kotlin/okhttp3/Headers.kt#L431
   */
  private String createUserAgent() {
    String userAgentString = createUserAgentString();
    StringBuilder result = new StringBuilder();

    // Removing invalid characters
    for (int i = 0; i < userAgentString.length(); i++) {
      char c = userAgentString.charAt(i);
      if (c == '\t' || ('\u0020' <= c && c <= '\u007e')) {
        result.append(c);
      }
    }
    return result.toString();
  }

  private String createUserAgentString() {
    Context context = Leanplum.getContext();

    return Util.getApplicationName(context)
        + "/" + Util.getVersionName()
        + "/" + APIConfig.getInstance().appId()
        + "/" + Constants.CLIENT
        + "/" + Constants.LEANPLUM_VERSION
        + "/" + Util.getSystemName()
        + "/" + Util.getSystemVersion()
        + "/" + Constants.LEANPLUM_SUPPORTED_ENCODING
        + "/" + Constants.LEANPLUM_PACKAGE_IDENTIFIER;
  }

  public int getResponseCode() throws IOException {
    return urlConnection.getResponseCode();
  }

  public void connect() throws IOException {
    urlConnection.connect();
  }

  public void disconnect() {
    urlConnection.disconnect();
  }

  public URL getURL() {
    return urlConnection.getURL();
  }

  public JSONObject getJsonResponse()
      throws JSONException, IOException {
    String response = getResponse();
    JSONTokener tokenizer = new JSONTokener(response);
    return new JSONObject(tokenizer);
  }

  @VisibleForTesting
  public String getResponse() throws IOException {
    InputStream inputStream;
    if (urlConnection.getResponseCode() < 400) {
      inputStream = urlConnection.getInputStream();
    } else {
      inputStream = urlConnection.getErrorStream();
    }

    // If we have a gzipped response, de-compress it first
    if (isGzipCompressed()) {
      inputStream = new GZIPInputStream(inputStream);
    }

    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
    StringBuilder builder = new StringBuilder();
    for (String line; (line = reader.readLine()) != null; ) {
      builder.append(line).append("\n");
    }

    try {
      inputStream.close();
      reader.close();
    } catch (Exception ignored) {
    }

    return builder.toString();
  }

  private boolean isGzipCompressed() {
    String contentHeader = urlConnection.getHeaderField("content-encoding");
    if (contentHeader != null) {
      return contentHeader.trim().equalsIgnoreCase(Constants.LEANPLUM_SUPPORTED_ENCODING);
    }
    return false;
  }

  public void saveResponse(OutputStream outputStream) throws IOException {
    InputStream is = urlConnection.getInputStream();

    // If we have a gzipped response, de-compress it first
    if (isGzipCompressed()) {
      is = new GZIPInputStream(is);
    }

    byte[] buffer = new byte[4096];
    int bytesRead;
    while ((bytesRead = is.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }
    outputStream.close();
  }
}
