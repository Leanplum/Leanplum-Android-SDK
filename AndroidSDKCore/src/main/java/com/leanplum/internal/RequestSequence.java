package com.leanplum.internal;

/**
 * Public interface for during the call sequence of database
 * read/write operations.
 *
 * @author sayaan
 */

public interface RequestSequence {
  void beforeRead();

  void afterRead();

  void beforeWrite();

  void afterWrite();
}
