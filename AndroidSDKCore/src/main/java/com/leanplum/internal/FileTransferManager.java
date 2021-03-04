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

import com.leanplum.internal.http.NetworkOperation;
import com.leanplum.internal.http.UploadOperation;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.json.JSONException;
import org.json.JSONObject;

public class FileTransferManager {

  private static final FileTransferManager INSTANCE = new FileTransferManager();

  private final Map<String, Boolean> fileTransferStatus = new HashMap<>();
  private final Map<File, Long> fileUploadSize = new HashMap<>();
  private final Map<File, Double> fileUploadProgress = new HashMap<>();

  private int pendingDownloads;
  private NoPendingDownloadsCallback noPendingDownloadsBlock;

  private final Object uploadFileLock = new Object();
  private String fileUploadProgressString = "";

  public interface NoPendingDownloadsCallback {
    void noPendingDownloads();
  }

  public static FileTransferManager getInstance() {
    return INSTANCE;
  }

  private FileTransferManager() {
  }

  void downloadFile(
      final String path,
      final String url,
      Runnable onResponse,
      Runnable onError) {

    if (Constants.isTestMode) {
      return;
    }
    if (Boolean.TRUE.equals(fileTransferStatus.get(path))) {
      return;
    }

    final Request request = RequestBuilder.withDownloadFileAction().create();
    request.onResponse(responseJson -> {
      if (onResponse != null)
        onResponse.run();
    });
    request.onError(exception -> {
      if (onError != null)
        onError.run();
    });

    pendingDownloads++;
    Log.d("Downloading resource: %s", path);
    fileTransferStatus.put(path, true);
    final Map<String, Object> dict = RequestSender.createArgsDictionary(request);
    dict.put(Constants.Keys.FILENAME, path);
    if (!APIConfig.getInstance().attachApiKeys(dict)) {
      return;
    }

    OperationQueue.sharedInstance().addParallelOperation(new Runnable() {
      @Override
      public void run() {
        try {
          downloadHelper(request, Constants.API_HOST_NAME, Constants.API_SERVLET, path, url, dict);
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
    });
  }

  private void downloadHelper(
      Request request,
      String hostName,
      String servlet,
      final String path,
      final String url,
      final Map<String, Object> dict) {

    NetworkOperation op = null;
    URL originalURL = null;
    String httpMethod = request.getHttpMethod();
    try {
      if (url == null) {
        op = new NetworkOperation(
            hostName,
            servlet,
            dict,
            httpMethod,
            Constants.API_SSL,
            Constants.NETWORK_TIMEOUT_SECONDS_FOR_DOWNLOADS);
      } else {
        op = new NetworkOperation(
            url,
            httpMethod,
            url.startsWith("https://"),
            Constants.NETWORK_TIMEOUT_SECONDS_FOR_DOWNLOADS);
      }
      originalURL = op.getURL();
      op.connect();
      int statusCode = op.getResponseCode();
      if (statusCode != 200) {
        throw new Exception("Leanplum: Error sending request to: " + hostName +
            ", HTTP status code: " + statusCode);
      }
      Stack<String> dirs = new Stack<>();
      String currentDir = path;
      while ((currentDir = new File(currentDir).getParent()) != null) {
        dirs.push(currentDir);
      }
      while (!dirs.isEmpty()) {
        String directory = FileManager.fileRelativeToDocuments(dirs.pop());
        boolean isCreated = new File(directory).mkdir();
        if (!isCreated) {
          Log.d("Failed to create directory: ", directory);
        }
      }

      FileOutputStream out = new FileOutputStream(
          new File(FileManager.fileRelativeToDocuments(path)));
      op.saveResponse(out);
      pendingDownloads--;
      if (request.response != null) {
        request.response.response(null);
      }
      if (pendingDownloads == 0 && noPendingDownloadsBlock != null) {
        noPendingDownloadsBlock.noPendingDownloads();
      }
    } catch (Exception e) {
      if (e instanceof EOFException) {
        if (op != null && !op.getURL().equals(originalURL)) {
          downloadHelper(
              request, null, op.getURL().toString(), path, url, new HashMap<String, Object>());
          return;
        }
      }
      Log.e("Error downloading resource:" + path, e);
      pendingDownloads--;
      if (request.error != null) {
        request.error.error(e);
      }
      if (pendingDownloads == 0 && noPendingDownloadsBlock != null) {
        noPendingDownloadsBlock.noPendingDownloads();
      }
    } finally {
      if (op != null) {
        op.disconnect();
      }
    }
  }

  public int numPendingDownloads() {
    return pendingDownloads;
  }

  public void onNoPendingDownloads(NoPendingDownloadsCallback block) {
    noPendingDownloadsBlock = block;
  }

  public void sendFilesNow(
      List<JSONObject> fileData,
      final List<String> filenames,
      final List<InputStream> streams) {

    final Request request = RequestBuilder
        .withUploadFileAction()
        .andParam(Constants.Params.DATA, fileData.toString())
        .create();

    if (Constants.isTestMode) {
      return;
    }
    final Map<String, Object> dict = RequestSender.createArgsDictionary(request);
    if (!APIConfig.getInstance().attachApiKeys(dict)) {
      return;
    }
    final List<File> filesToUpload = new ArrayList<>();

    // First set up the files for upload
    for (int i = 0; i < filenames.size(); i++) {
      String filename = filenames.get(i);
      if (filename == null || Boolean.TRUE.equals(fileTransferStatus.get(filename))) {
        continue;
      }
      File file = new File(filename);
      long size;
      try {
        size = streams.get(i).available();
      } catch (IOException e) {
        size = file.length();
      } catch (NullPointerException e) {
        // Not good. Can't read asset.
        Log.e("Unable to read file " + filename);
        continue;
      }
      fileTransferStatus.put(filename, true);
      filesToUpload.add(file);
      fileUploadSize.put(file, size);
      fileUploadProgress.put(file, 0.0);
    }
    if (filesToUpload.size() == 0) {
      return;
    }

    printUploadProgress();

    OperationQueue.sharedInstance().addParallelOperation(new Runnable() {
      @Override
      public void run() {
        synchronized (uploadFileLock) {  // Don't overload app and server with many upload tasks
          JSONObject result;
          UploadOperation op = null;

          try {
            op = new UploadOperation(
                Constants.API_HOST_NAME,
                Constants.API_SERVLET,
                request.getHttpMethod(),
                Constants.API_SSL,
                60);

            if (op.uploadFiles(filesToUpload, streams, dict)) {
              result = op.getJsonResponse();
              int statusCode = op.getResponseCode();
              if (statusCode != 200) {
                throw new Exception("Leanplum: Error sending request: " + statusCode);
              }
              if (request.response != null) {
                request.response.response(result);
              }
            } else {
              if (request.error != null) {
                request.error.error(new Exception("Leanplum: Unable to read file."));
              }
            }
          } catch (JSONException e) {
            Log.e("Unable to convert to JSON.", e);
            if (request.error != null) {
              request.error.error(e);
            }
          } catch (SocketTimeoutException e) {
            Log.e("Timeout uploading files. Try again or limit the number of files " +
                "to upload with parameters to syncResourcesAsync.");
            if (request.error != null) {
              request.error.error(e);
            }
          } catch (Exception e) {
            Log.e("Unable to send file.", e);
            if (request.error != null) {
              request.error.error(e);
            }
          } finally {
            if (op != null) {
              op.disconnect();
            }
          }

          for (File file : filesToUpload) {
            fileUploadProgress.put(file, 1.0);
          }
          printUploadProgress();
        }
      }
    });
  }

  private void printUploadProgress() {
    int totalFiles = fileUploadSize.size();
    int sentFiles = 0;
    int totalBytes = 0;
    int sentBytes = 0;
    for (Map.Entry<File, Long> entry : fileUploadSize.entrySet()) {
      File file = entry.getKey();
      long fileSize = entry.getValue();
      double fileProgress = fileUploadProgress.get(file);
      if (fileProgress == 1) {
        sentFiles++;
      }
      sentBytes += (int) (fileSize * fileProgress);
      totalBytes += fileSize;
    }
    String progressString = "Uploading resources. " +
        sentFiles + '/' + totalFiles + " files completed; " +
        getSizeAsString(sentBytes) + '/' + getSizeAsString(totalBytes) + " transferred.";
    if (!fileUploadProgressString.equals(progressString)) {
      fileUploadProgressString = progressString;
      Log.d(progressString);
    }
  }

  private static String getSizeAsString(int bytes) {
    if (bytes < (1 << 10)) {
      return bytes + " B";
    } else if (bytes < (1 << 20)) {
      return (bytes >> 10) + " KB";
    } else {
      return (bytes >> 20) + " MB";
    }
  }
}
