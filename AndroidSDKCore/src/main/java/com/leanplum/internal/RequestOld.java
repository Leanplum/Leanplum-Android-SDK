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

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.leanplum.Leanplum;
import com.leanplum.utils.SharedPreferencesUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

/**
 * Leanplum request class.
 *
 * @author Andrew First
 */
public class RequestOld implements Requesting {

  static final String LEANPLUM = "__leanplum__";
  static final String UUID_KEY = "uuid";

  private static String appId;
  private static String accessKey;
  private static String deviceId;
  private static String userId;
  private String requestId = UUID.randomUUID().toString();

  private static final Map<String, Boolean> fileTransferStatus = new HashMap<>();
  private static int pendingDownloads;
  private static NoPendingDownloadsCallback noPendingDownloadsBlock;

  // The token is saved primarily for legacy SharedPreferences decryption. This could
  // likely be removed in the future.
  private static String token = null;
  private static final Map<File, Long> fileUploadSize = new HashMap<>();
  private static final Map<File, Double> fileUploadProgress = new HashMap<>();
  private static String fileUploadProgressString = "";
  private static final Object uploadFileLock = new Object();

  private final String httpMethod;
  private final String apiMethod;
  private final Map<String, Object> params;
  ResponseCallback response;
  ErrorCallback error;
  private boolean saved;

  public static void setAppId(String appId, String accessKey) {
    if (!TextUtils.isEmpty(appId)) {
      RequestOld.appId = appId.trim();
    }
    if (!TextUtils.isEmpty(accessKey)) {
      RequestOld.accessKey = accessKey.trim();
    }
    Leanplum.countAggregator().incrementCount("set_app_id");
  }

  public static void setDeviceId(String deviceId) {
    RequestOld.deviceId = deviceId;
  }

  public static void setUserId(String userId) {
    RequestOld.userId = userId;
  }

  public static void setToken(String token) {
    RequestOld.token = token;
    Leanplum.countAggregator().incrementCount("set_token");
  }

  public static String token() {
    return token;
  }

  public static void loadToken() {
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
        LEANPLUM, Context.MODE_PRIVATE);
    String token = defaults.getString(Constants.Defaults.TOKEN_KEY, null);
    if (token == null) {
      return;
    }
    setToken(token);
    Leanplum.countAggregator().incrementCount("load_token");
  }

  public static void saveToken() {
    Context context = Leanplum.getContext();
    SharedPreferences defaults = context.getSharedPreferences(
        LEANPLUM, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = defaults.edit();
    editor.putString(Constants.Defaults.TOKEN_KEY, RequestOld.token());
    SharedPreferencesUtil.commitChanges(editor);
  }

  public String requestId() {
    return requestId;
  }

  public static String appId() {
    return appId;
  }

  public static String accessKey() {
    return accessKey;
  }

  public static String deviceId() {
    return deviceId;
  }

  public static String userId() {
    return RequestOld.userId;
  }

  public RequestOld(String httpMethod, String apiMethod, Map<String, Object> params) {
    this.httpMethod = httpMethod;
    this.apiMethod = apiMethod;
    this.params = params != null ? params : new HashMap<String, Object>();
    // Check if it is error and here was SQLite exception.
    if (Constants.Methods.LOG.equals(apiMethod) && LeanplumEventDataManager.sharedInstance().willSendErrorLogs()) {
      RequestSender.getInstance().addLocalError(this);
    }
  }

  public static RequestOld get(String apiMethod, Map<String, Object> params) {
    Log.LeanplumLogType level = Constants.Methods.LOG.equals(apiMethod) ?
        Log.LeanplumLogType.DEBUG : Log.LeanplumLogType.VERBOSE;
    Log.log(level, "Will call API method " + apiMethod + " with arguments " + params);
    Leanplum.countAggregator().incrementCount("get_request");
    return RequestFactory.getInstance().createRequest("GET", apiMethod, params);
  }

  public static RequestOld post(String apiMethod, Map<String, Object> params) {
    Log.LeanplumLogType level = Constants.Methods.LOG.equals(apiMethod) ?
        Log.LeanplumLogType.DEBUG : Log.LeanplumLogType.VERBOSE;
    Log.log(level, "Will call API method " + apiMethod + " with arguments " + params);
    Leanplum.countAggregator().incrementCount("post_request");
    return RequestFactory.getInstance().createRequest("POST", apiMethod, params);
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
    args.put(Constants.Params.DEVICE_ID, deviceId);
    args.put(Constants.Params.USER_ID, userId);
    args.put(Constants.Params.ACTION, apiMethod);
    args.put(Constants.Params.SDK_VERSION, Constants.LEANPLUM_VERSION);
    args.put(Constants.Params.DEV_MODE, Boolean.toString(Constants.isDevelopmentModeEnabled));
    args.put(Constants.Params.TIME, Double.toString(new Date().getTime() / 1000.0));
    args.put(Constants.Params.REQUEST_ID, requestId);
    if (token != null) {
      args.put(Constants.Params.TOKEN, token);
    }
    args.putAll(params);
    return args;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  static boolean attachApiKeys(Map<String, Object> dict) {
    if (appId == null || accessKey == null) {
      Log.e("API keys are not set. Please use Leanplum.setAppIdForDevelopmentMode or "
          + "Leanplum.setAppIdForProductionMode.");
      return false;
    }
    dict.put(Constants.Params.APP_ID, appId);
    dict.put(Constants.Params.CLIENT_KEY, accessKey);
    dict.put(Constants.Params.CLIENT, Constants.CLIENT);
    return true;
  }

  public interface ResponseCallback {
    void response(JSONObject response);
  }

  public interface ErrorCallback {
    void error(Exception e);
  }

  public interface NoPendingDownloadsCallback {
    void noPendingDownloads();
  }

  /**
   * This class wraps the unsent requests, requests that we need to send
   * and the JSON encoded string. Wrapping it in the class allows us to
   * retain consistency in the requests we are sending and the actual
   * JSON string.
   */
  static class RequestsWithEncoding {
    List<Map<String, Object>> unsentRequests;
    List<Map<String, Object>> requestsToSend;
    String jsonEncodedString;
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

  private static void printUploadProgress() {
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
      Log.i(progressString);
    }
  }

  public void sendFilesNow(final List<String> filenames, final List<InputStream> streams) {
    if (Constants.isTestMode) {
      return;
    }
    final Map<String, Object> dict = createArgsDictionary();
    if (!attachApiKeys(dict)) {
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

    Leanplum.countAggregator().incrementCount("send_files_now");

    printUploadProgress();

    OperationQueue.sharedInstance().addParallelOperation(new Runnable() {
      @Override
      public void run() {
        synchronized (uploadFileLock) {  // Don't overload app and server with many upload tasks
          JSONObject result;
          HttpURLConnection op = null;

          try {
            op = Util.uploadFilesOperation(
                Constants.Params.FILE,
                filesToUpload,
                streams,
                Constants.API_HOST_NAME,
                Constants.API_SERVLET,
                dict,
                httpMethod,
                Constants.API_SSL,
                60);

            if (op != null) {
              result = Util.getJsonResponse(op);
              int statusCode = op.getResponseCode();
              if (statusCode != 200) {
                throw new Exception("Leanplum: Error sending request: " + statusCode);
              }
              if (RequestOld.this.response != null) {
                RequestOld.this.response.response(result);
              }
            } else {
              if (error != null) {
                error.error(new Exception("Leanplum: Unable to read file."));
              }
            }
          } catch (JSONException e) {
            Log.e("Unable to convert to JSON.", e);
            if (error != null) {
              error.error(e);
            }
          } catch (SocketTimeoutException e) {
            Log.e("Timeout uploading files. Try again or limit the number of files " +
                "to upload with parameters to syncResourcesAsync.");
            if (error != null) {
              error.error(e);
            }
          } catch (Exception e) {
            Log.e("Unable to send file.", e);
            if (error != null) {
              error.error(e);
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

  void downloadFile(final String path, final String url) {
    if (Constants.isTestMode) {
      return;
    }
    if (Boolean.TRUE.equals(fileTransferStatus.get(path))) {
      return;
    }
    pendingDownloads++;
    Log.i("Downloading resource " + path);
    fileTransferStatus.put(path, true);
    final Map<String, Object> dict = createArgsDictionary();
    dict.put(Constants.Keys.FILENAME, path);
    if (!attachApiKeys(dict)) {
      return;
    }

    Leanplum.countAggregator().incrementCount("download_file");

    OperationQueue.sharedInstance().addParallelOperation(new Runnable() {
      @Override
      public void run() {
        try {
          downloadHelper(Constants.API_HOST_NAME, Constants.API_SERVLET, path, url, dict);
        } catch (Throwable t) {
          Util.handleException(t);
        }
      }
    });
  }

  private void downloadHelper(String hostName, String servlet, final String path, final String url,
      final Map<String, Object> dict) {
    HttpURLConnection op = null;
    URL originalURL = null;
    try {
      if (url == null) {
        op = Util.operation(
            hostName,
            servlet,
            dict,
            httpMethod,
            Constants.API_SSL,
            Constants.NETWORK_TIMEOUT_SECONDS_FOR_DOWNLOADS);
      } else {
        op = Util.createHttpUrlConnection(url, httpMethod, url.startsWith("https://"),
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
          Log.w("Failed to create directory: ", directory);
        }
      }

      FileOutputStream out = new FileOutputStream(
          new File(FileManager.fileRelativeToDocuments(path)));
      Util.saveResponse(op, out);
      pendingDownloads--;
      if (RequestOld.this.response != null) {
        RequestOld.this.response.response(null);
      }
      if (pendingDownloads == 0 && noPendingDownloadsBlock != null) {
        noPendingDownloadsBlock.noPendingDownloads();
      }
    } catch (Exception e) {
      if (e instanceof EOFException) {
        if (op != null && !op.getURL().equals(originalURL)) {
          downloadHelper(null, op.getURL().toString(), path, url, new HashMap<String, Object>());
          return;
        }
      }
      Log.e("Error downloading resource:" + path, e);
      pendingDownloads--;
      if (error != null) {
        error.error(e);
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

  public static int numPendingDownloads() {
    return pendingDownloads;
  }

  public static void onNoPendingDownloads(NoPendingDownloadsCallback block) {
    noPendingDownloadsBlock = block;
  }

  /**
   * Get response json object for request Id
   *
   * @param response response body
   * @param reqId request id
   * @return JSONObject for specified request id.
   */
  public static JSONObject getResponseForId(JSONObject response, String reqId) {
    try {
      JSONArray jsonArray = response.getJSONArray(Constants.Params.RESPONSE);
      if (jsonArray != null) {
        for (int i = 0; i < jsonArray.length(); i++) {
          JSONObject jsonObject = jsonArray.getJSONObject(i);
          if (jsonObject != null) {
            String requestId = jsonObject.getString(Constants.Params.REQUEST_ID);
            if (reqId.equalsIgnoreCase(requestId)) {
              return jsonObject;
            }
          }
        }
      }
    } catch (JSONException e) {
      Log.e("Could not get response for id: ", reqId, e);
      return null;
    }
    return null;
  }

  /**
   * Checks whether particular response is successful or not
   *
   * @param response JSONObject to check
   * @return true if successful, false otherwise
   */
  public static boolean isResponseSuccess(JSONObject response) {
    Leanplum.countAggregator().incrementCount("is_response_success");
    if (response == null) {
      return false;
    }
    try {
      return response.getBoolean("success");
    } catch (JSONException e) {
      Log.e("Could not parse JSON response.", e);
      return false;
    }
  }

  /**
   * Get response error from JSONObject
   *
   * @param response JSONObject to get error from
   * @return request error
   */
  public static String getResponseError(JSONObject response) {
    Leanplum.countAggregator().incrementCount("get_response_error");
    if (response == null) {
      return null;
    }
    try {
      JSONObject error = response.optJSONObject("error");
      if (error == null) {
        return null;
      }
      return error.getString("message");
    } catch (JSONException e) {
      Log.e("Could not parse JSON response.", e);
      return null;
    }
  }

  /**
   * Parse error message from server response and return readable error message.
   *
   * @param errorMessage String of error from server response.
   * @return String of readable error message.
   */
  public static String getReadableErrorMessage(String errorMessage) {
    if (errorMessage == null || errorMessage.length() == 0) {
      errorMessage = "API error";
    } else if (errorMessage.startsWith("App not found")) {
      errorMessage = "No app matching the provided app ID was found.";
      Constants.isInPermanentFailureState = true;
    } else if (errorMessage.startsWith("Invalid access key")) {
      errorMessage = "The access key you provided is not valid for this app.";
      Constants.isInPermanentFailureState = true;
    } else if (errorMessage.startsWith("Development mode requested but not permitted")) {
      errorMessage = "A call to Leanplum.setAppIdForDevelopmentMode "
          + "with your production key was made, which is not permitted.";
      Constants.isInPermanentFailureState = true;
    } else {
      errorMessage = "API error: " + errorMessage;
    }
    return errorMessage;
  }

  public void setSaved(boolean saved) {
    this.saved = saved;
  }

  public boolean isSaved() {
    return saved;
  }
}
