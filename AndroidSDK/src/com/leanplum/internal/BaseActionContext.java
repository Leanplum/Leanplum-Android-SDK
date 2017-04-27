// Copyright 2016, Leanplum, Inc.

package com.leanplum.internal;

import java.util.Map;

/**
 * Base class for ActionContext that contains internal methods.
 *
 * @author Andrew First
 */
public abstract class BaseActionContext {
  protected String messageId = null;
  protected String originalMessageId = null;
  protected int priority;
  protected Map<String, Object> args;
  protected boolean isRooted = true;
  private boolean isPreview = false;

  public BaseActionContext(String messageId, String originalMessageId) {
    this.messageId = messageId;
    this.originalMessageId = originalMessageId;
  }

  void setIsRooted(boolean value) {
    isRooted = value;
  }

  void setIsPreview(boolean isPreview) {
    this.isPreview = isPreview;
  }

  boolean isPreview() {
    return isPreview;
  }

  public String getMessageId() {
    return messageId;
  }

  public String getOriginalMessageId() {
    return originalMessageId;
  }

  public int getPriority() {
    return priority;
  }

  public Map<String, Object> getArgs() {
    return args;
  }
}
