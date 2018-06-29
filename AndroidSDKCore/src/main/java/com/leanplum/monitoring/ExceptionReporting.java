package com.leanplum.monitoring;

public interface ExceptionReporting {
    void reportException(Throwable t);
}
