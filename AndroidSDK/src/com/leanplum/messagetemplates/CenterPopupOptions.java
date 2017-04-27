// Copyright 2014, Leanplum, Inc.

package com.leanplum.messagetemplates;

import android.content.Context;

import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.messagetemplates.MessageTemplates.Args;

/**
 * Options used by {@link CenterPopup}.
 *
 * @author Martin Yanakiev
 */
public class CenterPopupOptions extends BaseMessageOptions {
  private int width;
  private int height;

  public CenterPopupOptions(ActionContext context) {
    super(context);
    setWidth(context.numberNamed(Args.LAYOUT_WIDTH).intValue());
    setHeight(context.numberNamed(Args.LAYOUT_HEIGHT).intValue());
  }

  public int getWidth() {
    return width;
  }

  private void setWidth(int width) {
    this.width = width;
  }

  public int getHeight() {
    return height;
  }

  private void setHeight(int height) {
    this.height = height;
  }

  public static ActionArgs toArgs(Context currentContext) {
    return BaseMessageOptions.toArgs(currentContext)
        .with(Args.LAYOUT_WIDTH, MessageTemplates.Values.CENTER_POPUP_WIDTH)
        .with(Args.LAYOUT_HEIGHT, MessageTemplates.Values.CENTER_POPUP_HEIGHT);
  }
}
