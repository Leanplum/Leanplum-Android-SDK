// Copyright 2017, Leanplum, Inc. All rights reserved.

package com.leanplum.callbacks;

/**
 * Inbox changes callback.
 *
 * @author Anna Orlova
 */
public abstract class InboxChangedCallback implements Runnable {
  public void run() {
    this.inboxChanged();
  }

  public abstract void inboxChanged();
}
