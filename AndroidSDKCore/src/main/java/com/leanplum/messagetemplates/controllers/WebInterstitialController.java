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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import com.leanplum.Leanplum;
import com.leanplum.messagetemplates.options.WebInterstitialOptions;

/**
 * Registers a Leanplum action that displays a fullscreen Web Interstitial.
 *
 * @author Atanas Dobrev
 */
public class WebInterstitialController extends BaseController {
  private @NonNull WebInterstitialOptions webOptions;

  public WebInterstitialController(Activity activity, @NonNull WebInterstitialOptions options) {
    super(activity);
    this.webOptions = options;

    init();
  }

  @Override
  protected boolean hasDismissButton() {
    return webOptions.hasDismissButton();
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
  RelativeLayout.LayoutParams createLayoutParams() {
    return new RelativeLayout.LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT);
  }

  @Override
  void addMessageChildViews(RelativeLayout parent) {
    WebView webView = createWebView(activity);
    parent.addView(webView);
  }

  private WebView createWebView(Context context) {
    WebView view = new WebView(context);
    view.setLayoutParams(
        new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    view.setWebViewClient(new WebViewClient() {
      @SuppressWarnings("deprecation")
      @Override
      public boolean shouldOverrideUrlLoading(WebView wView, String url) {
        if (handleCloseEvent(url)) {
          return true;
        }

        if (handleGooglePlayUri(wView.getContext(), url)) {
          return true;
        }
        return false;
      }
    });
    view.loadUrl(webOptions.getUrl());
    return view;
  }

  private boolean handleCloseEvent(String url) {
    if (url.contains(webOptions.getCloseUrl())) {
      cancel();
      String[] urlComponents = url.split("\\?");
      if (urlComponents.length > 1) {
        String queryString = urlComponents[1];
        String[] parameters = queryString.split("&");
        for (String parameter : parameters) {
          String[] parameterComponents = parameter.split("=");
          if (parameterComponents.length > 1 && parameterComponents[0].equals("result")) {
            Leanplum.track(parameterComponents[1]);
          }
        }
      }
      return true;
    }
    return false;
  }

  private boolean handleGooglePlayUri(Context context, String url) {
    Uri uri = Uri.parse(url);
    if ("market".equals(uri.getScheme())) {
      try {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        context.startActivity(intent);
        return true;
      } catch (ActivityNotFoundException e) {
        // Missing Google Play
        return false;
      }
    }
    return false;
  }

  @NonNull
  public WebInterstitialOptions getWebOptions() {
    return webOptions;
  }
}
