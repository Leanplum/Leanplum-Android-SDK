package com.leanplum.models;

public enum GeofenceEventType {
  ENTER_REGION("enter_region"),
  EXIT_REGION("exit_region");

  private final String name;

  GeofenceEventType(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
