//Copyright 2014, Leanplum, Inc.

package com.leanplum.callbacks;

import com.leanplum.ActionContext;

/**
 * Callback that gets run when an action is triggered.
 *
 * @author Andrew First
 */
public abstract class ActionCallback {
  public abstract boolean onResponse(ActionContext context);
}
