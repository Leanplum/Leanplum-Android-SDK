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

import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RequestBatchFactory {

  static final int MAX_EVENTS_PER_API_CALL = (Build.VERSION.SDK_INT <= 17) ? 5000 : 10000;

  private final RequestUuidHelper uuidHelper = new RequestUuidHelper();

  /**
   * In the presence of errors we do not send any events but only the errors.
   */
  public RequestBatch createErrorBatch(List<Map<String, Object>> localErrors) {
    List<Map<String, Object>> requests = new ArrayList<>();
    String jsonEncodedRequestsToSend;

    uuidHelper.attachNewUuid(localErrors);
    jsonEncodedRequestsToSend = jsonEncodeRequests(requests);

    // for errors, we send all unsent requests so they are identical
    return new RequestBatch(requests, requests, jsonEncodedRequestsToSend);
  }

  /**
   * Creates batch with all saved events with count of up to {@link #MAX_EVENTS_PER_API_CALL}.
   */
  public RequestBatch createNextBatch() {
    return createNextBatch(1.0);
  }

  /**
   * @param fraction Decimal from 0 to 1. It says what part of all saved events to include in batch.
   */
  @VisibleForTesting
  protected RequestBatch createNextBatch(double fraction) {
    try {
      List<Map<String, Object>> requests;
      List<Map<String, Object>> requestsToSend;

      if (fraction < 0.01) { //base case
        requests = new ArrayList<>(0);
        requestsToSend = new ArrayList<>(0);
      } else {
        requests = getUnsentRequests(fraction);
        requestsToSend = removeIrrelevantBackgroundStartRequests(requests);
      }

      String jsonEncoded = jsonEncodeRequests(requestsToSend);

      return new RequestBatch(requests, requestsToSend, jsonEncoded);
    } catch (OutOfMemoryError oom) {
      // half the requests will need less memory, recursively
      return createNextBatch(0.5 * fraction);
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
  @VisibleForTesting
  protected List<Map<String, Object>> removeIrrelevantBackgroundStartRequests(
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
    int eventsCount = (int) (fraction * MAX_EVENTS_PER_API_CALL);
    List<Map<String, Object>> events =
        LeanplumEventDataManager.sharedInstance().getEvents(eventsCount);

    // start new batch id for subsequent requests
    uuidHelper.deleteUuid();

    // if less than 100% of requests are sent the uuid will be reset
    if (fraction < 1) {
      // Check PR#340. This is only if OutOfMemoryException is thrown.
      uuidHelper.attachNewUuid(events);
    }
    return events;
  }

  @VisibleForTesting
  protected String jsonEncodeRequests(List<Map<String, Object>> requestData) {
    Map<String, Object> data = new HashMap<>();
    data.put(Constants.Params.DATA, requestData);
    return JsonConverter.toJson(data);
  }

  public void deleteFinishedBatch(@NonNull RequestBatch batch) {
    // Currently no enumeration of the requests so removing the first ones in the queue
    int eventsCount = batch.getEventsCount();
    if (eventsCount == 0) {
      return;
    }
    LeanplumEventDataManager.sharedInstance().deleteEvents(eventsCount);
  }

}
