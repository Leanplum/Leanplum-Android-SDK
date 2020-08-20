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
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.VisibleForTesting;
import com.leanplum.core.R;
import com.leanplum.messagetemplates.options.BaseMessageOptions;
import com.leanplum.utils.BitmapUtil;
import com.leanplum.utils.SizeUtil;
import com.leanplum.views.BackgroundImageView;

/**
 * Base class for CenterPopup and Interstitial messages.
 */
abstract class AbstractPopupController extends BaseController {

  protected BaseMessageOptions options;

  protected AbstractPopupController(Activity activity, BaseMessageOptions options) {
    super(activity);
    this.options = options;

    init();
  }

  @Override
  protected boolean hasDismissButton() {
    return true;
  }

  @VisibleForTesting
  public BaseMessageOptions getOptions() {
    return options;
  }

  @Override
  void addMessageChildViews(RelativeLayout parent) {
    ImageView image = createBackgroundImageView(activity);
    parent.addView(image);

    View title = createTitleView(activity);
    parent.addView(title);

    View button = createAcceptButton(activity);
    parent.addView(button);

    View message = createMessageView(activity);
    ((RelativeLayout.LayoutParams) message.getLayoutParams())
        .addRule(RelativeLayout.BELOW, title.getId());
    ((RelativeLayout.LayoutParams) message.getLayoutParams())
        .addRule(RelativeLayout.ABOVE, button.getId());
    parent.addView(message);
  }

  private ImageView createBackgroundImageView(Context context) {
    boolean roundedCorners = !isFullscreen();
    BackgroundImageView view = new BackgroundImageView(
        context,
        options.getBackgroundColor(),
        options.getBackgroundImage(),
        roundedCorners);

    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(layoutParams);
    return view;
  }

  private RelativeLayout createTitleView(Context context) {
    RelativeLayout view = new RelativeLayout(context);
    view.setId(R.id.title_view);
    view.setLayoutParams(new LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

    TextView title = new TextView(context);
    title.setPadding(0, SizeUtil.dp5, 0, SizeUtil.dp5);
    title.setGravity(Gravity.CENTER);
    title.setText(options.getTitle());
    title.setTextColor(options.getTitleColor());
    title.setTextSize(TypedValue.COMPLEX_UNIT_SP, SizeUtil.textSize0);
    title.setTypeface(null, Typeface.BOLD);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
    layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
    title.setLayoutParams(layoutParams);

    view.addView(title);
    return view;
  }

  private TextView createAcceptButton(Context context) {
    TextView view = new TextView(context);
    view.setId(R.id.accept_button);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
    layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
    layoutParams.setMargins(0, 0, 0, SizeUtil.dp5);

    view.setPadding(SizeUtil.dp20, SizeUtil.dp5, SizeUtil.dp20, SizeUtil.dp5);
    view.setLayoutParams(layoutParams);
    view.setText(options.getAcceptButtonText());
    view.setTextColor(options.getAcceptButtonTextColor());
    view.setTypeface(null, Typeface.BOLD);

    BitmapUtil.stateBackgroundDarkerByPercentage(view,
        options.getAcceptButtonBackgroundColor(), 30);

    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, SizeUtil.textSize0_1);
    view.setOnClickListener(clickedView -> {
      if (!isClosing) {
        options.accept();
        cancel();
      }
    });
    return view;
  }

  private TextView createMessageView(Context context) {
    TextView view = new TextView(context);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(layoutParams);
    view.setGravity(Gravity.CENTER);
    view.setText(options.getMessageText());
    view.setTextColor(options.getMessageColor());
    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, SizeUtil.textSize0_1);
    return view;
  }
}
