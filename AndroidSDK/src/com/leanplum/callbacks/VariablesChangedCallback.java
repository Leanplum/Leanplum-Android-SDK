//Copyright 2013, Leanplum, Inc.

package com.leanplum.callbacks;

/**
 * Variables changed callback.
 *
 * @author Andrew First
 */
public abstract class VariablesChangedCallback implements Runnable {
  public void run() {
    this.variablesChanged();
  }

  public abstract void variablesChanged();
}
