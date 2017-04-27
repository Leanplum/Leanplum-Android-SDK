// Copyright 2017, Leanplum, Inc.

package com.leanplum;

/**
 * LeanplumLocationAccuracyType enum used for Leanplum.setUserLocationAttribute.
 *
 * @author Alexis Oyama
 */
public enum LeanplumLocationAccuracyType {
  /**
   * Lowest accuracy. Reserved for internal use.
   */
  IP(0),

  /**
   * Default accuracy.
   */
  CELL(1),

  /**
   * Highest accuracy.
   */
  GPS(2);

  private int value;

  LeanplumLocationAccuracyType(int value) {
    this.value = value;
  }

  public int value() {
    return value;
  }
}
