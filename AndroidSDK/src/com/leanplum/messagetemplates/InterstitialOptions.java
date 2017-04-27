// Copyright 2014, Leanplum, Inc.

package com.leanplum.messagetemplates;

import android.content.Context;

import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;

/**
 * Options used by {@link Interstitial}.
 *
 * @author Martin Yanakiev
 */
public class InterstitialOptions extends BaseMessageOptions {
  public InterstitialOptions(ActionContext context) {
    super(context);
    // Set specific properties for interstitial popup.
  }

  public static ActionArgs toArgs(Context currentContext) {
    return BaseMessageOptions.toArgs(currentContext)
        .with(MessageTemplates.Args.MESSAGE_TEXT, MessageTemplates.Values.INTERSTITIAL_MESSAGE);
    // Add specific args for interstitial popup.
  }
}
