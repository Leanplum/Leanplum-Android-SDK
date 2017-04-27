// Copyright 2015, Leanplum, Inc. All rights reserved.

package com.leanplum;

import com.leanplum.callbacks.InboxChangedCallback;
import com.leanplum.callbacks.NewsfeedChangedCallback;

/**
 * Newsfeed class.
 *
 * @author Aleksandar Gyorev
 */
public class Newsfeed extends LeanplumInbox {

  /**
   * A private constructor, which prevents any other class from instantiating.
   */
  Newsfeed() {
  }

  /**
   * Static 'getInstance' method.
   */
  static Newsfeed getInstance() {
    return instance;
  }

  /**
   * Add a callback for when the newsfeed receives new values from the server.
   *
   * @deprecated use {@link #addChangedHandler(InboxChangedCallback)} instead
   */
  @Deprecated
  public void addNewsfeedChangedHandler(NewsfeedChangedCallback handler) {
    super.addChangedHandler(handler);
  }

  /**
   * Removes a newsfeed changed callback.
   *
   * @deprecated use {@link #removeChangedHandler(InboxChangedCallback)} instead
   */
  @Deprecated
  public void removeNewsfeedChangedHandler(NewsfeedChangedCallback handler) {
    super.removeChangedHandler(handler);
  }
}
