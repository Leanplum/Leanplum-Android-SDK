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

package com.leanplum.messagetemplates.controllers;

import android.app.Activity;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import com.leanplum.messagetemplates.options.InterstitialOptions;

/**
 * Registers a Leanplum action that displays a fullscreen interstitial.
 *
 * @author Andrew First
 */
public class InterstitialController extends AbstractPopupController {

  public InterstitialController(Activity activity, InterstitialOptions options) {
    super(activity, options);
  }

  @Override
  boolean isFullscreen() {
    return true;
  }

  @Override
  void applyWindowDecoration() {
    // no implementation
  }

  @Override
  protected RelativeLayout.LayoutParams createLayoutParams() {
    return new RelativeLayout.LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT);
  }
}
