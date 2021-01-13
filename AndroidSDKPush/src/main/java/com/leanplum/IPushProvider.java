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

package com.leanplum;

interface IPushProvider {

  /**
   * Returns the type of this push provider.
   */
  PushProviderType getType();

  /**
   * Returns the stored registration ID.
   */
  String getRegistrationId();

  /**
   * Stores the registration ID and sends it to backend.
   */
  void setRegistrationId(String regId);

  /**
   * Unregister from cloud messaging. Main usage is for testing purposes.
   */
  void unregister();

  /**
   * Updates the current registration ID from the cloud messaging's API.
   */
  void updateRegistrationId();
}
