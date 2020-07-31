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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.callbacks.ActionCallback;
import com.leanplum.callbacks.PostponableAction;

/**
 * Registers a Leanplum action that displays a fullscreen Web Interstitial.
 *
 * @author Atanas Dobrev
 */
public class WebInterstitial extends BaseMessageDialog {
  private static final String NAME = "Web Interstitial";

  private @NonNull WebInterstitialOptions webOptions;

  public WebInterstitial(Activity activity, @NonNull WebInterstitialOptions options) {
    super(activity);
    this.webOptions = options;

    init(true);
  }

  @Override
  protected boolean hasDismissButton() {
    return webOptions.hasDismissButton();
  }

  @Override
  RelativeLayout.LayoutParams createLayoutParams(boolean fullscreen) {
    return new RelativeLayout.LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT);
  }

  @Override
  void addMessageChildViews(RelativeLayout parent, boolean fullscreen) {
    WebView webView = createWebView(activity);
    parent.addView(webView, webView.getLayoutParams());
  }

  private WebView createWebView(Context context) {
    WebView view = new WebView(context);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    view.setLayoutParams(layoutParams);
    view.setWebViewClient(new WebViewClient() {
      @SuppressWarnings("deprecation")
      @Override
      public boolean shouldOverrideUrlLoading(WebView wView, String url) {
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

        // handle Google Play URI
        Uri uri = Uri.parse(url);
        if ("market".equals(uri.getScheme())) {
          try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            wView.getContext().startActivity(intent);
            return true;
          } catch (ActivityNotFoundException e) {
            // Missing Google Play
            return false;
          }
        }
        return false;
      }
    });
    view.loadUrl(webOptions.getUrl());
    return view;
  }

  public static void register() {
    Leanplum.defineAction(NAME, Leanplum.ACTION_KIND_MESSAGE | Leanplum.ACTION_KIND_ACTION,
        WebInterstitialOptions.toArgs(), new ActionCallback() {
          @Override
          public boolean onResponse(final ActionContext context) {
            LeanplumActivityHelper.queueActionUponActive(new PostponableAction() {
              @Override
              public void run() {
                Activity activity = LeanplumActivityHelper.getCurrentActivity();
                if (activity == null) {
                  return;
                }
                WebInterstitial webInterstitial = new WebInterstitial(activity,
                    new WebInterstitialOptions(context));
                if (!activity.isFinishing()) {
                  webInterstitial.show();
                }
              }
            });
            return true;
          }
        });
  }
}
