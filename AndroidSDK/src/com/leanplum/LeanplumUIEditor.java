// Copyright 2017 Leanplum, Inc. All Rights Reserved.
package com.leanplum;

import android.app.Activity;

/**
 * Describes the API of the visual editor package.
 */
public interface LeanplumUIEditor {
  /**
   * Enable interface editing via Leanplum.com Visual Editor.
   */
  void allowInterfaceEditing(Boolean isDevelopmentModeEnabled);

  /**
   * Enables Interface editing for the desired activity.
   *
   * @param activity The activity to enable interface editing for.
   */
  void applyInterfaceEdits(Activity activity);

  /**
   * Sets the update flag to true.
   */
  void startUpdating();

  /**
   * Sets the update flag to false.
   */
  void stopUpdating();

  /**
   * Send an immediate update of the UI to the LP server.
   */
  void sendUpdate();

  /**
   * Send an update with given delay of the UI to the LP server.
   */
  void sendUpdateDelayed(int delay);

  /**
   * Send an update of the UI to the LP server, delayed by the default time.
   */
  void sendUpdateDelayedDefault();

  /**
   * Returns the current editor mode.
   *
   * @return The current editor mode.
   */
  LeanplumEditorMode getMode();

  /**
   * Sets the current editor mode.
   *
   * @param mode The editor mode to set.
   */
  void setMode(LeanplumEditorMode mode);
}
