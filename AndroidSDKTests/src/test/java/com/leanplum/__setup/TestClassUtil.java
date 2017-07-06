/*
 * Copyright 2017, Leanplum, Inc. All rights reserved.
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
package com.leanplum.__setup;

import java.lang.reflect.Field;

/**
 * Class utilities for unit tests.
 *
 * @author Ben Marten
 */
public class TestClassUtil {
  /**
   * Gets the value of a field from a given class.
   *
   * @param clazz The class.
   * @param fieldName The name of the field.
   * @return Returns the value of the field.
   */
  public static Object getField(Class clazz, String fieldName) {
    return getField(clazz, null, fieldName);
  }

  /**
   * Gets the value of a field from a given object.
   *
   * @param object The object to retrieve the value from.
   * @param fieldName The name of the field.
   * @return Returns the value of the field.
   */
  @SuppressWarnings("unused")
  public static Object getField(Object object, String fieldName) {
    return getField(null, object, fieldName);
  }

  /**
   * Gets the value of a field from a class or an object.
   *
   * @param clazz The class.
   * @param object The object to retrieve the value from.
   * @param fieldName The name of the field.
   * @return Returns the value of the field.
   */
  public static Object getField(Class clazz, Object object, String fieldName) {
    if (clazz == null && object == null) {
      return null;
    }
    if (clazz == null) {
      clazz = object.getClass();
    }
    try {
      Field field = clazz.getDeclaredField(fieldName);
      try {
        field.setAccessible(true);
        return field.get(object);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Returns the value of the given field from the given object via reflection.
   *
   * @param fieldName The fieldName to check.
   * @param target The target object to retrieve the value from.
   * @return The value, otherwise throws exceptions on error.
   * @throws NoSuchFieldException
   * @throws IllegalAccessException
   */
  public static Object getFieldValueRecursivily(String fieldName, Object target)
      throws NoSuchFieldException, IllegalAccessException {
    if (target == null) {
      return null;
    }
    Field field = findField(target.getClass(), fieldName);

    field.setAccessible(true);
    return field.get(target);
  }

  /**
   * Finds a field of a given class recursively by stepping up the inheritance chain. This method
   * also finds private fields, for public methods use: clazz.getField().
   *
   * @param name The name of the field to be found.
   * @param clazz The class of the field to be found.
   * @return The Field.
   * @throws NoSuchFieldException
   */
  private static Field findField(Class clazz, String name) throws NoSuchFieldException {
    Class currentClass = clazz;
    while (currentClass != Object.class) {
      for (Field field : currentClass.getDeclaredFields()) {
        if (name.equals(field.getName())) {
          return field;
        }
      }
      currentClass = currentClass.getSuperclass();
    }
    throw new NoSuchFieldException("Field " + name + " not found for class " + clazz);
  }

  /**
   * Assigns a value to a field of a given class.
   *
   * @param clazz The class.
   * @param name The name of the field.
   * @param value The new value of the field.
   */
  public static void setField(Class clazz, String name, Object value) {
    setField(clazz, null, name, value);
  }

  /**
   * Assigns a value to a field of a given object.
   *
   * @param object The object.
   * @param name The name of the field.
   * @param value The new value of the field.
   */
  @SuppressWarnings("unused")
  public static void setField(Object object, String name, Object value) {
    setField(null, object, name, value);
  }

  /**
   * Assigns a value to a field of a given class or object.
   *
   * @param clazz The class.
   * @param object The object.
   * @param name The name of the field.
   * @param value The new value of the field.
   */
  public static void setField(Class clazz, Object object, String name, Object value) {
    if (clazz == null && object == null) {
      return;
    }
    if (clazz == null) {
      clazz = object.getClass();
    }
    try {
      Field field = clazz.getDeclaredField(name);
      try {
        field.setAccessible(true);
        field.set(object, value);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
  }
}
