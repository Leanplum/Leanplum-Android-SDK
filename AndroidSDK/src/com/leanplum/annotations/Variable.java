// Copyright 2013, Leanplum, Inc.

package com.leanplum.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Leanplum variable annotation.
 * <p>
 * <p>Use this to make this variable changeable from the Leanplum dashboard. Variables must be of
 * type boolean, byte, short, int, long, float, double, char, String, List, or Map. Lists and maps
 * may contain other lists and maps.
 * <p>
 * <p>Variables with this annotation update when the API call for Leanplum.start completes
 * successfully or fails (in which case values are loaded from a cache stored on the device).
 *
 * @author Andrew First
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Variable {
  /**
   * (Optional). The group to put the variable in. Use "." to nest groups.
   */
  String group() default "";

  /**
   * (Optional). The name of the variable. If not set, then uses the actual name of the field.
   */
  String name() default "";
}
