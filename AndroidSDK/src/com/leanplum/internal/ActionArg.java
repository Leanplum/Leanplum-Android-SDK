// Copyright 2014, Leanplum, Inc.

package com.leanplum.internal;

import android.text.TextUtils;

import java.io.InputStream;

/**
 * Represents an argument for a message or action.
 *
 * @param <T> Type of the argument. Can be Boolean, Byte, Short, Integer, Long, Float, Double,
 * Character, String, List, or Map.
 * @author Andrew First
 */
public class ActionArg<T> {
  private String name;
  private String kind;
  private T defaultValue;
  private boolean isAsset;

  private ActionArg() {
  }

  private static <T> ActionArg<T> argNamed(String name, T defaultValue, String kind) {
    ActionArg<T> arg = new ActionArg<>();
    arg.name = name;
    arg.kind = kind;
    arg.defaultValue = defaultValue;
    return arg;
  }

  /**
   * Creates an instance of a new arg with a default value.
   *
   * @param name Name of the arg.
   * @param defaultValue Default value of the arg. Can't be null.
   */
  public static <T> ActionArg<T> argNamed(String name, T defaultValue) {
    return argNamed(name, defaultValue, VarCache.kindFromValue(defaultValue));
  }

  public static ActionArg<Integer> colorArgNamed(String name, int defaultValue) {
    return argNamed(name, defaultValue, Constants.Kinds.COLOR);
  }

  public static ActionArg<String> fileArgNamed(String name, String defaultFilename) {
    if (TextUtils.isEmpty(defaultFilename)) {
      defaultFilename = "";
    }
    ActionArg<String> arg = argNamed(name, defaultFilename, Constants.Kinds.FILE);
    VarCache.registerFile(arg.defaultValue, arg.defaultValue,
        arg.defaultStream(), false, null, 0);
    return arg;
  }

  public static ActionArg<String> assetArgNamed(String name, String defaultFilename) {
    ActionArg<String> arg = argNamed(name, defaultFilename, Constants.Kinds.FILE);
    arg.isAsset = true;
    VarCache.registerFile(arg.defaultValue, arg.defaultValue,
        arg.defaultStream(), false, null, 0);
    return arg;
  }

  public static ActionArg<String> actionArgNamed(String name, String defaultValue) {
    if (TextUtils.isEmpty(defaultValue)) {
      defaultValue = "";
    }
    return argNamed(name, defaultValue, Constants.Kinds.ACTION);
  }

  public String name() {
    return name;
  }

  public String kind() {
    return kind;
  }

  public T defaultValue() {
    return defaultValue;
  }

  public InputStream defaultStream() {
    if (!kind.equals(Constants.Kinds.FILE)) {
      return null;
    }
    return FileManager.stream(false, isAsset, isAsset,
        (String) defaultValue, (String) defaultValue, null);
  }
}
