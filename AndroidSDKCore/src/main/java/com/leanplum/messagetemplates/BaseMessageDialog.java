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
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import androidx.core.view.ViewCompat;
import com.leanplum.core.R;
import com.leanplum.utils.SizeUtil;
import com.leanplum.views.CloseButton;

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
    super(activity, getTheme(activity));
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

    contentView.setAnimation(createFadeInAnimation());

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

    ShapeDrawable footerBackground = new ShapeDrawable();
    footerBackground.setShape(createRoundRect(fullscreen ? 0 : SizeUtil.dp20));
    footerBackground.getPaint().setColor(Color.TRANSPARENT);
    ViewCompat.setBackground(view, footerBackground);

    addMessageChildViews(view, fullscreen);
    return view;
  }

  /**
   * Positions the message view inside the dialog content view
   *
   * @param fullscreen
   * @return
   */
  abstract RelativeLayout.LayoutParams createLayoutParams(boolean fullscreen);

  /**
   * Creates and adds all message specific child views.
   *
   * @param parent
   * @param fullscreen
   */
  abstract void addMessageChildViews(RelativeLayout parent, boolean fullscreen);

  protected boolean hasDismissButton() {
    return false;
  }

  protected void applyWindowDecoration() {
    // no default implemetation
  }

  private Animation createFadeInAnimation() {
    Animation fadeIn = new AlphaAnimation(0, 1);
    fadeIn.setInterpolator(new DecelerateInterpolator());
    fadeIn.setDuration(350);
    return fadeIn;
  }

  private Animation createFadeOutAnimation() {
    Animation fadeOut = new AlphaAnimation(1, 0);
    fadeOut.setInterpolator(new AccelerateInterpolator());
    fadeOut.setDuration(350);
    return fadeOut;
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
    Animation animation = createFadeOutAnimation();
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
    closeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View arg0) {
        cancel();
      }
    });
    return closeButton;
  }

  protected Shape createRoundRect(int cornerRadius) {
    int c = cornerRadius;
    float[] outerRadii = new float[] {c, c, c, c, c, c, c, c};
    return new RoundRectShape(outerRadii, null, null);
  }

  private static int getTheme(Activity activity) {
    boolean full = (activity.getWindow().getAttributes().flags &
        WindowManager.LayoutParams.FLAG_FULLSCREEN) == WindowManager.LayoutParams.FLAG_FULLSCREEN;
    if (full) {
      return android.R.style.Theme_Translucent_NoTitleBar_Fullscreen;
    } else {
      return android.R.style.Theme_Translucent_NoTitleBar;
    }
  }
}
