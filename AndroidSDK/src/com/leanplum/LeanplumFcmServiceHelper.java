/*
 * Copyright 2017, Leanplum. All rights reserved.
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

package com.leanplum;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * Helper class for enabling the FCM service declared in manifest without app restart.
 *
 * @author Anna Orlova
 */
public class LeanplumFcmServiceHelper extends IntentService {
  public LeanplumFcmServiceHelper() {
    super("LeanplumFcmServiceHelper");
  }

  @Override
  protected void onHandleIntent(@Nullable Intent intent) {
  }
}
