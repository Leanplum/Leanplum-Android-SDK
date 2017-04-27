// Copyright 2013, Leanplum, Inc.

package com.leanplum.callbacks;

/**
 * Callback that gets run when the device needs to be registered.
 *
 * @author Andrew First
 */
public abstract class RegisterDeviceCallback implements Runnable {
  public static abstract class EmailCallback implements Runnable {
    private String email;

    public void setResponseHandler(String email) {
      this.email = email;
    }

    public void run() {
      this.onResponse(email);
    }

    public abstract void onResponse(String email);
  }

  private EmailCallback callback;

  public void setResponseHandler(EmailCallback callback) {
    this.callback = callback;
  }

  public void run() {
    this.onResponse(callback);
  }

  public abstract void onResponse(EmailCallback callback);
}
