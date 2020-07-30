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
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import com.leanplum.core.R;
import com.leanplum.utils.SizeUtil;
import com.leanplum.views.CloseButton;

/**
 * Base dialog used to display the Center Popup, Interstitial, Web Interstitial, HTML template.
 *
 * @author Martin Yanakiev, Anna Orlova
 */
public class BaseMessageDialog extends Dialog {
  protected RelativeLayout dialogView;
  protected Activity activity;
  protected WebView webView;

  protected boolean isClosing = false;

  protected BaseMessageDialog(Activity activity) {
    super(activity, getTheme(activity));
    this.activity = activity;
  }

  protected Animation createFadeInAnimation() {
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

  protected void onFadeOutAnimationEnded() {
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
        onFadeOutAnimationEnded();
      }
    });
    dialogView.startAnimation(animation);
  }

  protected CloseButton createCloseButton(Activity context, boolean fullscreen, View parent) {
    CloseButton closeButton = new CloseButton(context);
    closeButton.setId(R.id.close_button);
    RelativeLayout.LayoutParams closeLayout = new RelativeLayout.LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    if (fullscreen) {
      closeLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP, dialogView.getId());
      closeLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, dialogView.getId());
      closeLayout.setMargins(0, SizeUtil.dp5, SizeUtil.dp5, 0);
    } else {
      closeLayout.addRule(RelativeLayout.ALIGN_TOP, parent.getId());
      closeLayout.addRule(RelativeLayout.ALIGN_RIGHT, parent.getId());
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
