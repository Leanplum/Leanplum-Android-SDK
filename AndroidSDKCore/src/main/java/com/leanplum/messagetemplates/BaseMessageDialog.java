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
import android.app.Dialog;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import com.leanplum.core.R;
import com.leanplum.utils.SizeUtil;
import com.leanplum.views.CloseButton;
import com.leanplum.views.ViewUtils;

/**
 * Base dialog used to display the Center Popup, Interstitial, Web Interstitial, HTML template.
 *
 * @author Martin Yanakiev, Anna Orlova
 */
abstract class BaseMessageDialog extends Dialog {
  protected RelativeLayout contentView;
  protected Activity activity;
  protected WebView webView;

  protected boolean isClosing = false;

  protected BaseMessageDialog(Activity activity) {
    super(activity, ViewUtils.getThemeId(activity));
    this.activity = activity;
    SizeUtil.init(activity);
  }

  protected void init(boolean fullscreen) {
    contentView = createContentView();

    RelativeLayout messageView = createMessageView(fullscreen);
    contentView.addView(messageView);

    if (hasDismissButton()) {
      CloseButton closeButton = createCloseButton(fullscreen, messageView);
      contentView.addView(closeButton);
    }
    setContentView(contentView, contentView.getLayoutParams());

    contentView.setAnimation(ViewUtils.createFadeInAnimation(350));

    if (!fullscreen) {
      applyWindowDecoration();
    }
  }

  private RelativeLayout createContentView() {
    RelativeLayout view = new RelativeLayout(activity);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    view.setBackgroundColor(Color.TRANSPARENT);
    view.setLayoutParams(layoutParams);
    return view;
  }

  private RelativeLayout createMessageView(boolean fullscreen) {
    RelativeLayout view = new RelativeLayout(activity);
    view.setId(R.id.container_view);

    // Position the message
    RelativeLayout.LayoutParams layoutParams = createLayoutParams(fullscreen);
    view.setLayoutParams(layoutParams);

    ViewUtils.applyBackground(view, Color.TRANSPARENT, !fullscreen);

    addMessageChildViews(view, fullscreen);
    return view;
  }

  /**
   * Positions the message view inside the dialog content view
   */
  abstract RelativeLayout.LayoutParams createLayoutParams(boolean fullscreen);

  /**
   * Creates and adds all message specific child views.
   */
  abstract void addMessageChildViews(RelativeLayout parent, boolean fullscreen);

  protected boolean hasDismissButton() {
    return false;
  }

  protected void applyWindowDecoration() {
    // no default implementation
  }

  protected void onFadeOutAnimationEnd() {
    super.cancel();
  }

  @Override
  public void cancel() {
    if (isClosing) {
      return;
    }
    isClosing = true;
    Animation animation = ViewUtils.createFadeOutAnimation(350);
    animation.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {
      }
      @Override
      public void onAnimationRepeat(Animation animation) {
      }
      @Override
      public void onAnimationEnd(Animation animation) {
        onFadeOutAnimationEnd();
      }
    });
    contentView.startAnimation(animation);
  }

  private CloseButton createCloseButton(boolean fullscreen, View alignView) {
    CloseButton closeButton = new CloseButton(activity);
    closeButton.setId(R.id.close_button);
    RelativeLayout.LayoutParams closeLayout = new RelativeLayout.LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    if (fullscreen) {
      closeLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP, contentView.getId());
      closeLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, contentView.getId());
      closeLayout.setMargins(0, SizeUtil.dp5, SizeUtil.dp5, 0);
    } else {
      closeLayout.addRule(RelativeLayout.ALIGN_TOP, alignView.getId());
      closeLayout.addRule(RelativeLayout.ALIGN_RIGHT, alignView.getId());
      closeLayout.setMargins(0, -SizeUtil.dp7, -SizeUtil.dp7, 0);
    }
    closeButton.setLayoutParams(closeLayout);
    closeButton.setOnClickListener(clickedView -> cancel());
    return closeButton;
  }

}
