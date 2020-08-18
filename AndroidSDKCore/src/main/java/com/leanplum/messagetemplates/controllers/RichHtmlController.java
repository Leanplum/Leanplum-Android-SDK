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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.messagetemplates.options.RichHtmlOptions;
import com.leanplum.utils.SizeUtil;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import org.json.JSONObject;

/**
 * Registers a Leanplum action that displays a HTML message.
 *
 * @author Anna Orlova
 */
public class RichHtmlController extends BaseController {
  private WebView webView;
  private @NonNull
  RichHtmlOptions richOptions;

  public RichHtmlController(Activity activity, @NonNull RichHtmlOptions richOptions) {
    super(activity);
    this.richOptions = richOptions;

    init();
  }

  @Override
  boolean hasDismissButton() {
    return false;
  }

  @Override
  boolean isFullscreen() {
    return richOptions.isFullScreen();
  }

  @Override
  protected void applyWindowDecoration() {
    if (isFullscreen())
      return;

    Window window = getWindow();
    if (window == null) {
      return;
    }

    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

    if (richOptions.isBannerWithTapOutsideFalse()) {
      // banners need to be positioned at the top manually
      // (unless they get repositioned to the bottom later)
      window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
      window.setGravity(Gravity.TOP);

      // use the html y offset to determine the y location of the window; this is different
      // from non-banners because we don't want to make the window too big (e.g. via a margin
      // in the layout) and block other things on the screen (e.g. dialogs)
      WindowManager.LayoutParams windowLayoutParams = window.getAttributes();
      windowLayoutParams.y = richOptions.getHtmlYOffset(activity);
      window.setAttributes(windowLayoutParams);

      window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
          WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    } else {
      window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
          WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
    }

    if (richOptions.isHtmlAlignBottom()) {
      if (richOptions.isBannerWithTapOutsideFalse()) {
        window.setGravity(Gravity.BOTTOM);
      } else {
        contentView.setGravity(Gravity.BOTTOM);
      }
    }
  }

  @Override
  RelativeLayout.LayoutParams createLayoutParams() {
    RelativeLayout.LayoutParams layoutParams;
    if (isFullscreen()) {
      layoutParams = new RelativeLayout.LayoutParams(
          LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    } else {
      int height = SizeUtil.dpToPx(activity, richOptions.getHtmlHeight());
      RichHtmlOptions.Size htmlWidth = richOptions.getHtmlWidth();
      if (htmlWidth == null || TextUtils.isEmpty(htmlWidth.type)) {
        layoutParams = new RelativeLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, height);
      } else {
        int width = htmlWidth.value;
        if ("%".equals(htmlWidth.type)) {
          Point size = SizeUtil.getDisplaySize(activity);
          width = size.x * width / 100;
        } else {
          width = SizeUtil.dpToPx(activity, width);
        }
        layoutParams = new RelativeLayout.LayoutParams(width, height);
      }

      layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
      int htmlYOffset = richOptions.getHtmlYOffset(activity);
      if (!richOptions.isBannerWithTapOutsideFalse()) {
        if (richOptions.isHtmlAlignBottom()) {
          layoutParams.bottomMargin = htmlYOffset;
        } else {
          layoutParams.topMargin = htmlYOffset;
        }
      }
    }
    return layoutParams;
  }

  @Override
  void addMessageChildViews(RelativeLayout parent) {
    webView = createHtml(activity);
    parent.addView(webView);
  }

  /**
   * Create WebView with HTML template.
   *
   * @param context Current context.
   * @return WebVew WebVew with HTML template.
   */
  private WebView createHtml(Context context) {
    contentView.setVisibility(View.GONE);
    WebView webView = new WebView(context);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    webView.setLayoutParams(layoutParams);

    initWebSettings(webView);
    webView.setBackgroundColor(Color.TRANSPARENT);
    webView.setLongClickable(false); // Disable long click
    webView.setHapticFeedbackEnabled(false);
    webView.setOnLongClickListener(v -> true);
    webView.setWebChromeClient(new WebChromeClient());
    webView.setWebViewClient(new WebViewClient() {
      // shouldOverrideUrlLoading(WebView wView, String url) was deprecated at API 24.
      @SuppressWarnings("deprecation")
      @Override
      public boolean shouldOverrideUrlLoading(WebView wView, String url) {
        if (isClosing) // prevent multiple clicks on same button
          return true;

        // Open URL event.
        if (handleOpenEvent(url)){
          return true;
        }

        // Close URL event.
        if (handleCloseEvent(url)) {
          return true;
        }

        // Track URL event.
        if (handleTrackEvent(url)) {
          return true;
        }

        // Action URL or track action URL event.
        if (handleActionEvent(url)) {
          return true;
        }

        return false;
      }
    });
    String html = richOptions.getHtmlTemplate();

    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

    return webView;
  }

  private boolean handleOpenEvent(String url) {
    if (url.contains(richOptions.getOpenUrl())) {
      contentView.setVisibility(View.VISIBLE);
      if (activity != null && !activity.isFinishing()) {
        show();
      }
      return true;
    }
    return false;
  }

  private boolean handleCloseEvent(String url) {
    if (url.contains(richOptions.getCloseUrl())) {
      cancel();
      String queryComponentsFromUrl = queryComponentsFromUrl(url, "result");
      if (!TextUtils.isEmpty(queryComponentsFromUrl)) {
        Leanplum.track(queryComponentsFromUrl);
      }
      return true;
    }
    return false;
  }

  private boolean handleTrackEvent(String url) {
    if (url.contains(richOptions.getTrackUrl())) {
      String eventName = queryComponentsFromUrl(url, "event");
      if (!TextUtils.isEmpty(eventName)) {
        Double value = Double.parseDouble(queryComponentsFromUrl(url, "value"));
        String info = queryComponentsFromUrl(url, "info");
        Map<String, Object> paramsMap = null;

        try {
          paramsMap = ActionContext.mapFromJson(new JSONObject(queryComponentsFromUrl(url,
              "parameters")));
        } catch (Exception ignored) {
        }

        if (queryComponentsFromUrl(url, "isMessageEvent").equals("true")) {
          ActionContext actionContext = richOptions.getActionContext();
          actionContext.trackMessageEvent(eventName, value, info, paramsMap);
        } else {
          Leanplum.track(eventName, value, info, paramsMap);
        }
      }
      return true;
    }
    return false;
  }

  private boolean handleActionEvent(String url) {
    if (url.contains(richOptions.getActionUrl()) ||
        url.contains(richOptions.getTrackActionUrl())) {
      cancel();
      String queryComponentsFromUrl = queryComponentsFromUrl(url, "action");
      try {
        queryComponentsFromUrl = URLDecoder.decode(queryComponentsFromUrl, "UTF-8");
      } catch (UnsupportedEncodingException ignored) {
      }

      ActionContext actionContext = richOptions.getActionContext();
      if (!TextUtils.isEmpty(queryComponentsFromUrl) && actionContext != null) {
        if (url.contains(richOptions.getActionUrl())) {
          actionContext.runActionNamed(queryComponentsFromUrl);
        } else {
          actionContext.runTrackedActionNamed(queryComponentsFromUrl);
        }
      }
      return true;
    }
    return false;
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void initWebSettings(WebView webView) {
    WebSettings webViewSettings = webView.getSettings();
    if (Build.VERSION.SDK_INT >= 17) {
      webViewSettings.setMediaPlaybackRequiresUserGesture(false);
    }
    webViewSettings.setAppCacheEnabled(true);
    webViewSettings.setAllowFileAccess(true);
    webViewSettings.setJavaScriptEnabled(true);
    webViewSettings.setDomStorageEnabled(true);
    webViewSettings.setJavaScriptCanOpenWindowsAutomatically(true);
    webViewSettings.setLoadWithOverviewMode(true);
    webViewSettings.setLoadsImagesAutomatically(true);

    if (Build.VERSION.SDK_INT >= 16) {
      webViewSettings.setAllowFileAccessFromFileURLs(true);
      webViewSettings.setAllowUniversalAccessFromFileURLs(true);
    }

    webViewSettings.setBuiltInZoomControls(false);
    webViewSettings.setDisplayZoomControls(false);
    webViewSettings.setSupportZoom(false);
  }

  /**
   * Get query components from URL.
   *
   * @param url URL string.
   * @param components Name of components.
   * @return String String with query components.
   */
  private String queryComponentsFromUrl(String url, String components) {
    String componentsFromUrl = "";
    String[] urlComponents = url.split("\\?");
    if (urlComponents.length > 1) {
      String queryString = urlComponents[1];
      String[] parameters = queryString.split("&");
      for (String parameter : parameters) {
        String[] parameterComponents = parameter.split("=");
        if (parameterComponents.length > 1 && parameterComponents[0].equals(components)) {
          componentsFromUrl = parameterComponents[1];
        }
      }
    }
    try {
      componentsFromUrl = URLDecoder.decode(componentsFromUrl, "UTF-8");
    } catch (Exception ignored) {
    }
    return componentsFromUrl;
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    try {
      if (webView != null) {
        if (hasFocus) {
          webView.onResume();
        } else {
          webView.onPause();
        }
      }
    } catch (Throwable ignore) {
    }
    super.onWindowFocusChanged(hasFocus);
  }

  @Override
  protected void onFadeOutAnimationEnd() {
    super.onFadeOutAnimationEnd();

    Handler handler = new Handler();
    handler.postDelayed(() -> {
      if (webView != null) {
        webView.stopLoading();
        webView.loadUrl("");
        if (contentView != null) {
          contentView.removeAllViews();
        }
        webView.removeAllViews();
        webView.destroy();
      }
    }, 10);
  }

  @Override
  public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
    if (!richOptions.isFullScreen()) {
      if (richOptions.isBannerWithTapOutsideFalse()) {
        return super.dispatchTouchEvent(ev);
      }

      Point size = SizeUtil.getDisplaySize(activity);
      int dialogWidth = webView.getWidth();
      int left = (size.x - dialogWidth) / 2;
      int right = (size.x + dialogWidth) / 2;
      int height = SizeUtil.dpToPx(Leanplum.getContext(), richOptions.getHtmlHeight());
      int statusBarHeight = SizeUtil.getStatusBarHeight(Leanplum.getContext());
      int htmlYOffset = richOptions.getHtmlYOffset(activity);
      int top;
      int bottom;
      if (richOptions.isHtmlAlignBottom()) {
        top = size.y - height - statusBarHeight - htmlYOffset;
        bottom = size.y - htmlYOffset - statusBarHeight;
      } else {
        top = htmlYOffset + statusBarHeight;
        bottom = height + statusBarHeight + htmlYOffset;
      }

      if (ev.getY() < top || ev.getY() > bottom || ev.getX() < left || ev.getX() > right) {
        if (richOptions.isHtmlTabOutsideToClose()) {
          cancel();
        }
        activity.dispatchTouchEvent(ev);
      }
    }
    return super.dispatchTouchEvent(ev);
  }

  @NonNull
  public RichHtmlOptions getRichOptions() {
    return richOptions;
  }
}
