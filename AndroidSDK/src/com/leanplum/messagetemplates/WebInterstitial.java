// Copyright 2014, Leanplum, Inc.

package com.leanplum.messagetemplates;

import android.app.Activity;
import android.content.Context;

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

  public WebInterstitial(Activity activity, WebInterstitialOptions options) {
    super(activity, true, null, options, null);
    this.webOptions = options;
  }

  /**
   * Deprecated: Use {@link WebInterstitial#register()}.
   */
  @Deprecated
  @SuppressWarnings("unused")
  public static void register(Context currentContext) {
    register();
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
