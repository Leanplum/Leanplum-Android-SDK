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

import androidx.annotation.NonNull;
import java.util.List;
import java.util.Map;

/**
 * This class wraps the unsent requests, requests that we need to send
 * and the JSON encoded string. Wrapping it in the class allows us to
 * retain consistency in the requests we are sending and the actual
 * JSON string.
 */
public class RequestBatch {
  // all persisted requests
  List<Map<String, Object>> requests;
  // filtered requests that will be sent
  List<Map<String, Object>> requestsToSend;
  String jsonEncoded;

  public RequestBatch(
      @NonNull List<Map<String, Object>> requests,
      @NonNull List<Map<String, Object>> requestsToSend,
      @NonNull String jsonEncoded) {
    this.requests = requests;
    this.requestsToSend = requestsToSend;
    this.jsonEncoded = jsonEncoded;
  }

  public int getEventsCount() {
    return requests.size();
  }

  public boolean isEmpty() {
    return requestsToSend.isEmpty();
  }

  public boolean isFull() {
    return getEventsCount() == RequestBatchFactory.MAX_EVENTS_PER_API_CALL;
  }

  public String getJson() {
    return jsonEncoded;
  }
}
