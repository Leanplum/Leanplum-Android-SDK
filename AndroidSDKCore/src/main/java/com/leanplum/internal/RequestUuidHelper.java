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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.leanplum.Leanplum;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class that keeps in one place the code that manipulates the uuid parameter of
 * the requests. This parameter specifies an identifier of the batch of whom the request is part.
 *
 * Uuid needs to be persisted in the DB for each request for the server 'dedup' logic to work.
 * It checks both the uuid and reqId of the requests to ignore them if they had been sent multiple
 * times.
 */
public class RequestUuidHelper {

  /**
   * Removes the uuid from shared prefs.
   */
  public void deleteUuid() {
    Context context = Leanplum.getContext();
    if (context == null)
      return;

    SharedPreferences preferences = context.getSharedPreferences(
        Constants.Defaults.LEANPLUM, Context.MODE_PRIVATE);

    SharedPreferences.Editor editor = preferences.edit();
    editor.remove(Constants.Defaults.UUID_KEY);
    editor.apply();
  }

  /**
   * Creates and saves new uuid in shared prefs.
   * @return the newly created uuid.
   */
  public @NonNull String saveNewUuid(Context context) {
    String newUuid = UUID.randomUUID().toString();

    SharedPreferences prefs = context.getSharedPreferences(
        Constants.Defaults.LEANPLUM, Context.MODE_PRIVATE);

    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(Constants.Defaults.UUID_KEY, newUuid);
    editor.apply();

    return newUuid;
  }

  /**
   * Loads the uuid from shared prefs.
   */
  public @Nullable String loadUuid() {
    Context context = Leanplum.getContext();
    if (context == null)
      return null;

    SharedPreferences prefs = context.getSharedPreferences(
        Constants.Defaults.LEANPLUM, Context.MODE_PRIVATE);

    return prefs.getString(Constants.Defaults.UUID_KEY, null);
  }

  /**
   * Attaches uuid parameter to the request arguments. If number of saved requests is divisible
   * by {@link RequestBatchFactory#MAX_EVENTS_PER_API_CALL} it creates new uuid.
   */
  public boolean attachUuid(@NonNull Map<String, Object> requestArgs) {
    Context context = Leanplum.getContext();
    if (context == null)
      return false;

    long events = LeanplumEventDataManager.sharedInstance().getEventsCount();

    String uuid = loadUuid();
    if (uuid == null || events % RequestBatchFactory.MAX_EVENTS_PER_API_CALL == 0) {
      uuid = saveNewUuid(context);
    }

    requestArgs.put(Constants.Params.UUID, uuid);
    return true;
  }

  public void attachNewUuid(@NonNull List<Map<String, Object>> events) {
    String uuid = UUID.randomUUID().toString();
    for (Map<String, Object> event : events) {
      event.put(Constants.Params.UUID, uuid);
    }
  }

}
