// Copyright 2013, Leanplum, Inc.

package com.leanplum;

/**
 * Leanplum exception.
 *
 * @author Andrew First
 */
public class LeanplumException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public LeanplumException(String message) {
    super(message);
  }
}
