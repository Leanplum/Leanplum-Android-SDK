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
import android.graphics.Point;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import com.leanplum.messagetemplates.options.CenterPopupOptions;
import com.leanplum.utils.SizeUtil;

/**
 * Registers a Leanplum action that displays a custom center popup dialog.
 *
 * @author Andrew First
 */
public class CenterPopupController extends AbstractPopupController {

  public CenterPopupController(Activity activity, CenterPopupOptions options) {
    super(activity, options);
  }

  @Override
  boolean isFullscreen() {
    return false;
  }

  @Override
  protected void applyWindowDecoration() {
    Window window = getWindow();
    if (window == null) {
      return;
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    if (Build.VERSION.SDK_INT >= 14) {
      window.setDimAmount(0.7f);
    }
  }

  @Override
  protected RelativeLayout.LayoutParams createLayoutParams() {
    RelativeLayout.LayoutParams layoutParams;

    // Make sure the dialog fits on screen.
    Point size = SizeUtil.getDisplaySize(activity);
    int width = SizeUtil.dpToPx(activity, ((CenterPopupOptions) options).getWidth());
    int height = SizeUtil.dpToPx(activity, ((CenterPopupOptions) options).getHeight());

    int maxWidth = size.x - SizeUtil.dp20;
    int maxHeight = size.y - SizeUtil.dp20;
    double aspectRatio = width / (double) height;
    if (width > maxWidth && (int) (width / aspectRatio) < maxHeight) {
      width = maxWidth;
      height = (int) (width / aspectRatio);
    }
    if (height > maxHeight && (int) (height * aspectRatio) < maxWidth) {
      height = maxHeight;
      width = (int) (height * aspectRatio);
    }

    layoutParams = new RelativeLayout.LayoutParams(width, height);
    layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
    return layoutParams;
  }
}
