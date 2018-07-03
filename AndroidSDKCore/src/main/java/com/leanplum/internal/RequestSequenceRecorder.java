package com.leanplum.internal;

/**
 * Records request call sequence of read/write operations to database.
 *
 */

public interface RequestSequenceRecorder {
  /**
   * Executes before database read in Request.
   */
  void beforeRead();

  /**
   * Executes after database read in Request.
   */
  void afterRead();

  /**
   * Executes before database write in Request.
   */
  void beforeWrite();

  /**
   * Executes after database write in Request.
   */
  void afterWrite();
}
