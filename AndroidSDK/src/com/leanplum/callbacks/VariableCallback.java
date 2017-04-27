// Copyright 2013, Leanplum, Inc.

package com.leanplum.callbacks;

import com.leanplum.Var;

/**
 * Leanplum variable callback.
 *
 * @author Andrew First
 */
public abstract class VariableCallback<T> implements Runnable {
  private Var<T> variable;

  public void setVariable(Var<T> variable) {
    this.variable = variable;
  }

  public void run() {
    this.handle(variable);
  }

  public abstract void handle(Var<T> variable);
}
