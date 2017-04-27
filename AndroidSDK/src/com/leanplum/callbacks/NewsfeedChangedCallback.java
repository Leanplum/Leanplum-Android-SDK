// Copyright 2015, Leanplum, Inc. All rights reserved.

package com.leanplum.callbacks;

/**
 * Newsfeed changed callback.
 *
 * @author Aleksandar Gyorev
 */
public abstract class NewsfeedChangedCallback extends InboxChangedCallback {
  @Override
  public void inboxChanged() {
    newsfeedChanged();
  }

  public abstract void newsfeedChanged();
}
