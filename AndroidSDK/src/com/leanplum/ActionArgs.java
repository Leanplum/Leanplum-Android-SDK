// Copyright 2014, Leanplum, Inc.

package com.leanplum;

import com.leanplum.internal.ActionArg;
import com.leanplum.internal.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents arguments for a message or action.
 *
 * @author Andrew First
 */
public class ActionArgs {
  private List<ActionArg<?>> args = new ArrayList<>();

  /**
   * Adds a basic argument of type T.
   *
   * @param <T> Type of the argument. Can be Boolean, Byte, Short, Integer, Long, Float, Double,
   * Character, String, List, or Map.
   * @param name The name of the argument.
   * @param defaultValue The default value of the argument.
   */
  public <T> ActionArgs with(String name, T defaultValue) {
    if (name == null) {
      Log.e("with - Invalid name parameter provided.");
      return this;
    }
    args.add(ActionArg.argNamed(name, defaultValue));
    return this;
  }

  /**
   * Adds a color argument with an integer value.
   *
   * @param name The name of the argument.
   * @param defaultValue The integer value representing the color.
   */
  public ActionArgs withColor(String name, int defaultValue) {
    if (name == null) {
      Log.e("withColor - Invalid name parameter provided.");
      return this;
    }
    args.add(ActionArg.colorArgNamed(name, defaultValue));
    return this;
  }

  /**
   * Adds a file argument.
   *
   * @param name The name of the argument.
   * @param defaultFilename The path of the default file value. Use null to indicate no default
   * value.
   */
  public ActionArgs withFile(String name, String defaultFilename) {
    if (name == null) {
      Log.e("withFile - Invalid name parameter provided.");
      return this;
    }
    args.add(ActionArg.fileArgNamed(name, defaultFilename));
    return this;
  }

  /**
   * Adds an asset argument. Same as {@link ActionArgs#withFile} except that the filename is
   * relative to the assets directory.
   *
   * @param name The name of the argument.
   * @param defaultFilename The path of the default file value relative to the assets directory. Use
   * null to indicate no default value.
   */
  public ActionArgs withAsset(String name, String defaultFilename) {
    if (name == null) {
      Log.e("withAsset - Invalid name parameter provided.");
      return this;
    }
    args.add(ActionArg.assetArgNamed(name, defaultFilename));
    return this;
  }

  /**
   * Adds an action argument.
   *
   * @param name The name of the argument.
   * @param defaultValue The default action name. Use null to indicate no action.
   */
  public ActionArgs withAction(String name, String defaultValue) {
    if (name == null) {
      Log.e("withAction - Invalid name parameter provided.");
      return this;
    }
    args.add(ActionArg.actionArgNamed(name, defaultValue));
    return this;
  }

  List<ActionArg<?>> getValue() {
    return args;
  }
}
