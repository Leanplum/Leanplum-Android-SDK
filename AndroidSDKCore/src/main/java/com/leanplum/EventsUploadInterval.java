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

/**
 * The enumeration represents time interval to periodically upload events to server.
 * Possible values are 5, 10, or 15 minutes.
 */
public enum EventsUploadInterval {
  /**
   * 5 minutes interval
   */
  AT_MOST_5_MINUTES(5),

  /**
   * 10 minutes interval
   */
  AT_MOST_10_MINUTES(10),

  /**
   * 15 minutes interval
   */
  AT_MOST_15_MINUTES(15);

  private final int minutes;

  EventsUploadInterval(int minutes) {
    this.minutes = minutes;
  }

  public int getMinutes() {
    return minutes;
  }
}
