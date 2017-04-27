// Copyright 2013, Leanplum, Inc.

package com.leanplum.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Leanplum variable annotation. Use this to make this variable changeable from the Leanplum
 * dashboard.
 *
 * @author Andrew First
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface File {
  /**
   * (Optional). The group to put the variable in. Use "." to nest groups.
   */
  String group() default "";

  /**
   * (Optional). The name of the variable. If not set, then uses the actual name of the field.
   */
  String name() default "";
}
