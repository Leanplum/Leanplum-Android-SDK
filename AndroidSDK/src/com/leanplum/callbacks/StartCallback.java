// Copyright 2013, Leanplum, Inc.

package com.leanplum.callbacks;

/**
 * Callback that gets run when Leanplum is started.
 *
 * @author Andrew First
 */
public abstract class StartCallback implements Runnable {
  private boolean success;

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public void run() {
    this.onResponse(success);
  }

  public abstract void onResponse(boolean success);
}
