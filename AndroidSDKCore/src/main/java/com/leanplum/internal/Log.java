/*
 * Copyright 2016, Leanplum, Inc. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.leanplum.internal;


import com.leanplum.core.BuildConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles logging within the Leanplum SDK.
 *
 * @author Ben Marten
 */
public class Log {
  public enum LeanplumLogType {
    /**
     * Always visible to customers. Sent to us when remote logging is enabled.
     */
    ERROR,

    /**
     * Always visible to customers. Sent to us when remote logging is enabled.
     */
    WARNING,

    /**
     * Always visible to customers. Sent to us when remote logging is enabled.
     */
    INFO,

    /**
     * Visible to customers only when verbose logging is enabled. Sent to us when remote logging is
     * enabled.
     */
    VERBOSE,

    /**
     * Not visible to customers. Sent to us when remote logging is enabled.
     */
    PRIVATE,

    /**
     * Used only for Leanplum SDK debugging. Not visible to customers. Not sent to us when remote
     * logging is enabled.
     */
    DEBUG,

    /**
     * Not visible to customers. Sent to us when remote COUNTING is enabled.
     */
    COUNT

  }

  private static final ThreadLocal<Boolean> isLogging = new ThreadLocal<Boolean>() {
    @Override
    protected Boolean initialValue() {
      return false;
    }
  };

  private static Set<String> enabledCounters = new HashSet<>();
  private static Map<String, Integer> counts = new HashMap<>();

  public static void e(Object... objects) {
    log(LeanplumLogType.ERROR, CollectionUtil.concatenateArray(objects, ", "));
  }

  public static void w(Object... objects) {
    log(LeanplumLogType.WARNING, CollectionUtil.concatenateArray(objects, ", "));
  }

  public static void i(Object... objects) {
    log(LeanplumLogType.INFO, CollectionUtil.concatenateArray(objects, ", "));
  }

  public static void v(Object... objects) {
    log(LeanplumLogType.VERBOSE, CollectionUtil.concatenateArray(objects, ", "));
  }

  public static void p(Object... objects) {
    log(LeanplumLogType.PRIVATE, CollectionUtil.concatenateArray(objects, ", "));
  }

  public static void d(Object... objects) {
    log(LeanplumLogType.DEBUG, CollectionUtil.concatenateArray(objects, ", "));
  }

  public static void count(String name) {
    log(LeanplumLogType.COUNT, name);
  }

  /**
   * Handle Leanplum log messages, which may be sent to the server for remote logging if
   * Constants.loggingEnabled is set.
   * <p/>
   * This will format the string in all cases, and is therefore less efficient than checking the
   * conditionals inline. Avoid this in performance-critical code.
   *
   * @param type The log type level of the message.
   * @param message The message to be logged.
   */
  public static void log(LeanplumLogType type, String message) {
    String tag = generateTag(type);
    String prefix = generateMessagePrefix();

    switch (type) {
      case ERROR:
        android.util.Log.e(tag, prefix + message);
        maybeSendLog(tag + prefix + message);
        return;
      case WARNING:
        android.util.Log.w(tag, prefix + message);
        maybeSendLog(tag + prefix + message);
        return;
      case INFO:
        android.util.Log.i(tag, prefix + message);
        maybeSendLog(tag + prefix + message);
        return;
      case VERBOSE:
        if (Constants.isDevelopmentModeEnabled
            && Constants.enableVerboseLoggingInDevelopmentMode) {
          android.util.Log.v(tag, prefix + message);
          maybeSendLog(tag + prefix + message);
        }
        return;
      case PRIVATE:
        maybeSendLog(tag + prefix + message);
        return;
      case COUNT:
        incrementCount(message);
        return;
      default: // DEBUG
        if (BuildConfig.DEBUG) {
          android.util.Log.d(tag, prefix + message);
        }
    }
  }

  /**
   * Enables certain counters to send metrics to server.
   */
  public static void setEnabledCounters(Set enabledCounters) {
    Log.enabledCounters = enabledCounters;
  }

  /**
   * Create request for sending all counters.
   */
  public static void sendAllCounts() {
    for(Map.Entry<String, Integer> entry : counts.entrySet()) {
      String name = entry.getKey();
      Integer count = entry.getValue();
        sendCount(name, count);
    }

  }

  private static void sendCount(String name, Integer count) {
    try {
      HashMap<String, Object> params = new HashMap<>();
      params.put(Constants.Params.TYPE, Constants.Values.SDK_COUNT);
      params.put(Constants.Params.MESSAGE, name);
      params.put(Constants.Params.COUNT, count);
      // TODO: figure this out
      Request.post(Constants.Methods.LOG, params).sendEventually();
    } catch (Throwable t) {
      android.util.Log.e("Leanplum", "Unable to send count.", t);
    }

  }
  /**
   * Generates tag for logging purpose in format [LogType][Leanplum]
   *
   * @param type log type
   * @return generated tag
   */
  private static String generateTag(LeanplumLogType type) {
    return "[" + type.name() + "][Leanplum]";
  }

  /**
   * Generates a log message prefix based on current class and method name in format
   * [ClassName::MethodName::LineNumber].
   * This shouldn't be called directly, since getting className and methodName is hardcoded and
   * extracted based on StackTrace.
   *
   * @return a message prefix for logging purpose
   */
  private static String generateMessagePrefix() {
    // Since this is called from log method, caller method should be on index 5.
    int callerIndex = 5;
    int minimumStackTraceIndex = 5;

    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    if (stackTraceElements.length >= minimumStackTraceIndex) {
      String tag = "[";
      tag += stackTraceElements[callerIndex].getClassName();
      tag += "::";
      tag += stackTraceElements[callerIndex].getMethodName();
      tag += "::";
      tag += stackTraceElements[callerIndex].getLineNumber();
      tag += "]: ";
      return tag;
    }
    return "";
  }

  private static void maybeSendLog(String message) {
    if (!Constants.loggingEnabled || isLogging.get()) {
      return;
    }

    isLogging.set(true);
    try {
      HashMap<String, Object> params = new HashMap<>();
      params.put(Constants.Params.TYPE, Constants.Values.SDK_LOG);
      params.put(Constants.Params.MESSAGE, message);
      Request.post(Constants.Methods.LOG, params).sendEventually();
    } catch (Throwable t) {
      android.util.Log.e("Leanplum", "Unable to send log.", t);
    } finally {
      isLogging.remove();
    }
  }

  private static void incrementCount(String name) {
    if (enabledCounters.contains(name)) {
      Integer count = 0;
      if (counts.containsKey(name)) {
        count = counts.get(name);
      }
      count = count + 1;
      counts.put(name, count);
    }
  }

  /**
   * Handy function to get a loggable stack trace from a Throwable.
   *
   * @param throwable An exception to log.
   */
  public static String getStackTraceString(Throwable throwable) {
    return android.util.Log.getStackTraceString(throwable);
  }
}
