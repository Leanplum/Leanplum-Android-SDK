// Copyright 2016, Leanplum, Inc.

package com.leanplum;

/**
 * Enum for describing the Editor Mode.
 *
 * @author Ben Marten
 */
public enum LeanplumEditorMode {
  LP_EDITOR_MODE_INTERFACE(0),
  LP_EDITOR_MODE_EVENT(1);

  private final int value;

  /**
   * Creates a new EditorMode enum with given value.
   */
  LeanplumEditorMode(final int newValue) {
    value = newValue;
  }

  /**
   * Returns the value of the enum entry.
   *
   * @return The value of the entry.
   */
  public int getValue() {
    return value;
  }
}
