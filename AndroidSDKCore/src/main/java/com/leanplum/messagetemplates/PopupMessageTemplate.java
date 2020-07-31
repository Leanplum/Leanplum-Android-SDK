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

package com.leanplum.messagetemplates;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import com.leanplum.core.R;
import com.leanplum.utils.BitmapUtil;
import com.leanplum.utils.SizeUtil;
import com.leanplum.views.BackgroundImageView;

/**
 * Base class for CenterPopup and Interstitial messages.
 */
abstract class PopupMessageTemplate extends BaseMessageDialog {

  protected BaseMessageOptions options;

  protected PopupMessageTemplate(Activity activity, BaseMessageOptions options, boolean fullscreen) {
    super(activity);
    this.options = options;

    init(fullscreen);
  }

  @Override
  protected boolean hasDismissButton() {
    return true;
  }

  @Override
  void addMessageChildViews(RelativeLayout parent, boolean fullscreen) {
    ImageView image = createBackgroundImageView(activity, fullscreen);
    parent.addView(image);

    View title = createTitleView(activity);
    title.setId(R.id.title_view);
    parent.addView(title);

    View button = createAcceptButton(activity);
    button.setId(R.id.accept_button);
    parent.addView(button);

    View message = createMessageView(activity);
    ((RelativeLayout.LayoutParams) message.getLayoutParams())
        .addRule(RelativeLayout.BELOW, title.getId());
    ((RelativeLayout.LayoutParams) message.getLayoutParams())
        .addRule(RelativeLayout.ABOVE, button.getId());
    parent.addView(message);
  }

  private ImageView createBackgroundImageView(Context context, boolean fullscreen) {
    BackgroundImageView view = new BackgroundImageView(context, fullscreen);
    view.setScaleType(ImageView.ScaleType.CENTER_CROP);
    int cornerRadius;
    if (!fullscreen) {
      cornerRadius = SizeUtil.dp20;
    } else {
      cornerRadius = 0;
    }
    view.setImageBitmap(options.getBackgroundImage());

    ShapeDrawable footerBackground = new ShapeDrawable();
    footerBackground.setShape(createRoundRect(cornerRadius));
    footerBackground.getPaint().setColor(options.getBackgroundColor());
    ViewCompat.setBackground(view, footerBackground);

    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(layoutParams);
    return view;
  }

  private RelativeLayout createTitleView(Context context) {
    RelativeLayout view = new RelativeLayout(context);
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
    view.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View arg0) {
        if (!isClosing) {
          options.accept();
          cancel();
        }
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
