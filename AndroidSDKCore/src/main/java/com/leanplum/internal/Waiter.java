package com.leanplum.internal;

/**
 * Created by sayaan on 6/28/18.
 */

public interface Waiter {
  void beforeRead();

  void afterRead();

  void beforeWrite();

  void afterWrite();
}
