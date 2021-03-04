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

import com.leanplum.internal.Constants;
import com.leanplum.internal.Log;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes files using the multipart form data format.
 */
public class UploadOperation extends LeanplumHttpConnection {

  private static final String BOUNDARY = "==================================leanplum";
  private static final String LINE_END = "\r\n";
  private static final String TWO_HYPHENS = "--";
  private static final String CONTENT_TYPE = "Content-Type: application/octet-stream";

  public UploadOperation(
      String hostName,
      String path,
      String httpMethod,
      boolean useSSL,
      int timeoutSeconds) throws IOException {

    initConnection(hostName, path, httpMethod, useSSL, timeoutSeconds);
  }

  public boolean uploadFiles(
      List<File> filesToUpload,
      List<InputStream> streams,
      Map<String, Object> params) throws IOException {

    urlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
    urlConnection.setRequestProperty("Connection", "Keep-Alive");

    DataOutputStream writer = new DataOutputStream(urlConnection.getOutputStream());

    // Create the header for the request with the parameters
    writeHeader(writer, params);

    // Main file writing loop
    for (int i = 0; i < filesToUpload.size(); i++) {
      File file = filesToUpload.get(i);

      InputStream is;
      if (i < streams.size()) {
        is = streams.get(i);
      } else {
        is = new FileInputStream(file);
      }

      if (!writeFile(writer, file.getName(), file.getPath(), is, i)) {
        return false;
      }
    }

    // End the output for the request
    writeEnd(writer);

    writer.flush();
    writer.close();
    return true;
  }

  private void writeHeader(DataOutputStream writer, Map<String, Object> params)
      throws IOException {

    for (Map.Entry<String, Object> entry : params.entrySet()) {
      String paramData = TWO_HYPHENS + BOUNDARY + LINE_END
          + "Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + LINE_END
          + LINE_END
          + entry.getValue() + LINE_END;
      writer.writeBytes(paramData);
    }
  }

  private void writeEnd(DataOutputStream writer) throws IOException {
    String endOfRequest = TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END;
    writer.writeBytes(endOfRequest);
  }

  private boolean writeFile(
      DataOutputStream writer,
      String fileName,
      String filePath,
      InputStream is,
      int i)
      throws IOException {

    writeFileHeader(writer, fileName, i);

    if (!writeFileContent(writer, filePath, is)) {
      return false;
    }

    writeFileEnd(writer);
    return true;
  }

  private void writeFileHeader(DataOutputStream writer, String fileName, int i) throws IOException {
    String contentDisposition = String.format(
        Locale.getDefault(),
        "Content-Disposition: form-data; name=\"%s%d\";filename=\"%s\"",
        Constants.Params.FILE,
        i,
        fileName);

    String fileHeader = TWO_HYPHENS + BOUNDARY + LINE_END
        + contentDisposition + LINE_END
        + CONTENT_TYPE + LINE_END
        + LINE_END;
    writer.writeBytes(fileHeader);
  }

  private boolean writeFileContent(DataOutputStream writer, String filePath, InputStream is)
      throws IOException {
    // Read in the actual file
    byte[] buffer = new byte[4096];
    int bytesRead;
    try {
      while ((bytesRead = is.read(buffer)) != -1) {
        writer.write(buffer, 0, bytesRead);
      }
    } catch (NullPointerException e) {
      Log.d("Unable to read file while uploading " + filePath);
      return false;
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          Log.d("Failed to close InputStream: " + e);
        }
      }
    }
    return true;
  }

  private void writeFileEnd(DataOutputStream writer) throws IOException {
    writer.writeBytes(LINE_END);
  }
}
