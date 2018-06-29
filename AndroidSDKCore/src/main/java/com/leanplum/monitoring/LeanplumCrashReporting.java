package com.leanplum.monitoring;

public interface LeanplumCrashReporting {
    void reportException(Throwable t);
}
