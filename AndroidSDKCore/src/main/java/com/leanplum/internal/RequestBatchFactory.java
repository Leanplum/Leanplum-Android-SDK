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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.VisibleForTesting;
import com.leanplum.Leanplum;
import com.leanplum.internal.RequestSender.RequestsWithEncoding;
import com.leanplum.utils.SharedPreferencesUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RequestBatchFactory {

  static final int MAX_EVENTS_PER_API_CALL = (Build.VERSION.SDK_INT <= 17) ? 5000 : 10000;

// TODO fix the methods for the errors
  public RequestsWithEncoding getRequestsWithEncodedString(List<Map<String, Object>> localErrors) {
    RequestsWithEncoding requestsWithEncoding;
    // Check if we have localErrors, if yes then we will send only errors to the server.
    if (localErrors.size() != 0) {
      requestsWithEncoding = getRequestsWithEncodedStringForErrors(localErrors);
    } else {
      requestsWithEncoding = getRequestsWithEncodedStringStoredRequests(1.0);
    }

    return requestsWithEncoding;
  }

  private RequestsWithEncoding getRequestsWithEncodedStringForErrors(List<Map<String, Object>> localErrors) {
    List<Map<String, Object>> unsentRequests = new ArrayList<>();
    List<Map<String, Object>> requestsToSend;
    String jsonEncodedRequestsToSend;

    String uuid = UUID.randomUUID().toString();
    for (Map<String, Object> error : localErrors) {
      error.put(Constants.Params.UUID, uuid);
      unsentRequests.add(error);
    }
    requestsToSend = unsentRequests;
    jsonEncodedRequestsToSend = jsonEncodeRequests(unsentRequests);

    RequestsWithEncoding requestsWithEncoding = new RequestsWithEncoding();
    // for errors, we send all unsent requests so they are identical
    requestsWithEncoding.unsentRequests = unsentRequests;
    requestsWithEncoding.requestsToSend = requestsToSend;
    requestsWithEncoding.jsonEncodedString = jsonEncodedRequestsToSend;

    return requestsWithEncoding;
  }

  protected RequestsWithEncoding getRequestsWithEncodedStringStoredRequests(double fraction) {
    try {
      List<Map<String, Object>> unsentRequests;
      List<Map<String, Object>> requestsToSend;
      String jsonEncodedRequestsToSend;
      RequestsWithEncoding requestsWithEncoding = new RequestsWithEncoding();

      if (fraction < 0.01) { //base case
        unsentRequests = new ArrayList<>(0);
        requestsToSend = new ArrayList<>(0);
      } else {
        unsentRequests = getUnsentRequests(fraction);
        requestsToSend = removeIrrelevantBackgroundStartRequests(unsentRequests);
      }

      jsonEncodedRequestsToSend = jsonEncodeRequests(requestsToSend);
      requestsWithEncoding.unsentRequests = unsentRequests;
      requestsWithEncoding.requestsToSend = requestsToSend;
      requestsWithEncoding.jsonEncodedString = jsonEncodedRequestsToSend;

      return requestsWithEncoding;
    } catch (OutOfMemoryError E) {
      // half the requests will need less memory, recursively
      return getRequestsWithEncodedStringStoredRequests(0.5 * fraction);
    }
  }

  /**
   * In various scenarios we can end up batching a big number of requests (e.g. device is offline,
   * background sessions), which could make the stored API calls batch look something like:
   * <p>
   * <code>start(B), start(B), start(F), track, start(B), track, start(F), resumeSession</code>
   * <p>
   * where <code>start(B)</code> indicates a start in the background, and <code>start(F)</code>
   * one in the foreground.
   * <p>
   * In this case the first two <code>start(B)</code> can be dropped because they don't contribute
   * any relevant information for the batch call.
   * <p>
   * Essentially we drop every <code>start(B)</code> call, that is directly followed by any kind of
   * a <code>start</code> call.
   *
   * @param requestData A list of the requests, stored on the device.
   * @return A list of only these requests, which contain relevant information for the API call.
   */
  private static List<Map<String, Object>> removeIrrelevantBackgroundStartRequests(
      List<Map<String, Object>> requestData) {
    List<Map<String, Object>> relevantRequests = new ArrayList<>();

    int requestCount = requestData.size();
    if (requestCount > 0) {
      for (int i = 0; i < requestCount; i++) {
        Map<String, Object> currentRequest = requestData.get(i);
        if (i < requestCount - 1
            && RequestBuilder.ACTION_START.equals(requestData.get(i + 1).get(Constants.Params.ACTION))
            && RequestBuilder.ACTION_START.equals(currentRequest.get(Constants.Params.ACTION))
            && Boolean.TRUE.toString().equals(currentRequest.get(Constants.Params.BACKGROUND))) {
          continue;
        }
        relevantRequests.add(currentRequest);
      }
    }

    return relevantRequests;
  }

  @VisibleForTesting
  public List<Map<String, Object>> getUnsentRequests(double fraction) {
    Context context = Leanplum.getContext();
    SharedPreferences preferences = context.getSharedPreferences(
        Constants.Defaults.LEANPLUM, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = preferences.edit();
    int count = (int) (fraction * MAX_EVENTS_PER_API_CALL);
    List<Map<String, Object>> requestData =
        LeanplumEventDataManager.sharedInstance().getEvents(count);
    editor.remove(Constants.Defaults.UUID_KEY);
    SharedPreferencesUtil.commitChanges(editor);
    // if we send less than 100% of requests, we need to reset the batch
    // UUID for the next batch
    if (fraction < 1) {
      RequestUtil.setNewBatchUUID(requestData);
    }
    return requestData;
  }

  protected static String jsonEncodeRequests(List<Map<String, Object>> requestData) {
    Map<String, Object> data = new HashMap<>();
    data.put(Constants.Params.DATA, requestData);
    return JsonConverter.toJson(data);
  }

}
