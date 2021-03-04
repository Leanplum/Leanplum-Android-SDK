/*
 * Copyright 2020, Leanplum, Inc. All rights reserved.
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


import com.leanplum.LeanplumException;
import com.leanplum.internal.Request.RequestType;
import com.leanplum.monitoring.ExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Log class used to log messages to console and backend, as well as logging to backend.
 */
public class Log {

  private static int level = Level.INFO;

  /**
   * Sets log level.
   * @param level level to set
   */
  public static void setLogLevel(int level) {
    Log.level = level;
  }

  public static void e(String msg, Object... args) {
    log(LogType.ERROR, msg, args);
  }

  public static void e(String msg, Throwable throwable) {
    if (msg != null && msg.contains("%s")) {
      log(LogType.ERROR, msg, getStackTraceString(throwable));
    } else {
      log(LogType.ERROR, msg + "\n" + getStackTraceString(throwable));
    }
  }

  public static void i(String msg, Object... args) {
    log(LogType.INFO, msg, args);
  }

  public static void d(String msg, Object... args) {
    log(LogType.DEBUG, msg, args);
  }

  /**
   * Logs exception to server
   * @param throwable to log
   */
  public static void exception(Throwable throwable) {
    ExceptionHandler.getInstance().reportException(throwable);

    if (throwable instanceof OutOfMemoryError) {
      if (Constants.isDevelopmentModeEnabled) {
        throw (OutOfMemoryError) throwable;
      }
      return;
    }

    // Propagate Leanplum generated exceptions.
    if (throwable instanceof LeanplumException) {
      if (Constants.isDevelopmentModeEnabled) {
        throw (LeanplumException) throwable;
      }
      return;
    }

    Log.e("Internal error: %s", throwable.getMessage());

    String versionName;
    try {
      versionName = Util.getVersionName();
    } catch (Throwable t2) {
      versionName = "(Unknown)";
    }

    try {
      String message = throwable.getMessage();
      if (message != null) {
        message = throwable.toString() + " (" + message + ')';
      } else {
        message = throwable.toString();
      }

      StringWriter stringWriter = new StringWriter();
      PrintWriter writer = new PrintWriter(stringWriter);
      writer.println(message);

      throwable.printStackTrace(writer);

      Request request = RequestBuilder.withLogAction()
          .andParam(Constants.Params.TYPE, Constants.Values.SDK_LOG)
          .andParam(Constants.Params.VERSION_NAME, versionName)
          .andParam(Constants.Params.MESSAGE, stringWriter.toString())
          .andType(RequestType.IMMEDIATE)
          .create();
      RequestSender.getInstance().send(request);
    } catch (Throwable t2) {
      Log.e("Unable to send error report: %s", t2.getMessage());
    }
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
  public static void log(LogType type, String message, Object... args) {
    try {
      String tag = formatTag(type);
      String msg = String.format(message, args);

      switch (type) {
        case ERROR:
          if (level >= Level.ERROR) {
            android.util.Log.e(tag, msg);
            break;
          }
        case INFO:
          if (level >= Level.INFO) {
            android.util.Log.i(tag, msg);
          }
          break;
        case DEBUG:
          if (level >= Level.DEBUG) {
            android.util.Log.d(tag, msg);
          }
          break;
      }
      handleLogMessage(tag, msg);
    } catch (Throwable t) {
      // ignored
    }
  }

  private static String formatTag(LogType type) {
    return "[Leanplum][" + type.name() + "]";
  }

  /**
   * Handles logs that are supposed to be sent to backend
   * @param tag message tag
   * @param msg message to log
   */
  private static void handleLogMessage(String tag, String msg) {
    if (Constants.loggingEnabled) {
      Request request = RequestBuilder.withLogAction()
          .andParam(Constants.Params.TYPE, Constants.Values.SDK_LOG)
          .andParam(Constants.Params.MESSAGE, tag + msg)
          .create();
      RequestSender.getInstance().send(request);
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

  public enum LogType {
    DEBUG,
    INFO,
    ERROR
  }

  public static class Level {
    /**
     * Disables logging.
     */
    public static final int OFF = 0;
    /**
     * Logs only errors, enabled by default.
     */
    public static final int ERROR = 1;
    /**
     * Logs informational messages including errors.
     */
    public static final int INFO = 2;
    /**
     * Enables all levels including DEBUG logging of the SDK.
     */
    public static final int DEBUG = 3;
  }
}
