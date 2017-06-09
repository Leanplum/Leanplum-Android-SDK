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

package com.leanplum.internal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LeanplumSQLiteHelper class to work with SQLite.
 *
 * @author Anna Orlova
 */
public class LeanplumSQLiteHelper {
  private static final String DATABASE_NAME = "__leanplum.db";
  private static final String COLUMN_ACTION = "action";
  private static final String KEY_ROWID = "rowid";

  static final String ACTIONS_TABLE_NAME = "actions";

  private static SQLiteDatabase database;
  private static ContentValues contentValues = new ContentValues();

  public static void init(Context context) {
    if (database != null) {
      Log.e("Database is already initialized.");
      return;
    }
    // Create database if needed.
    File dbFile = new File(context.getFilesDir(), DATABASE_NAME);
    try {
      database = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.CREATE_IF_NECESSARY);
    } catch (SQLiteDatabaseCorruptException e) {
      Log.e("Database is corrupted. Recreate.");
      try {
        if (!dbFile.exists() || dbFile.delete()) {
          database = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.CREATE_IF_NECESSARY);
        }
      } catch (Throwable t) {
        Log.e("Cannot create database. Retry failed.", t);
        Util.handleException(t);
      }
    } catch (Throwable t) {
      Log.e("Cannot create database.", t);
      Util.handleException(t);
    }
    if (database != null) {
      // Create table.
      try {
        database.execSQL("CREATE TABLE IF NOT EXISTS " + ACTIONS_TABLE_NAME + "(" + COLUMN_ACTION + " TEXT)");
        Request.moveOldDataFromSharedPreferences();
      } catch (Throwable t) {
        Log.e("Cannot create table.", t);
        Util.handleException(t);
      }
    }
  }

  static boolean isDatabaseInitialized() {
    return database != null;
  }

  /**
   * Inserts action to the table with name tableName.
   *
   * @param tableName Table name.
   * @param action String with json of action.
   */
  static void insertAction(String tableName, String action) {
    if (database == null) {
      return;
    }
    contentValues.put(COLUMN_ACTION, action);
    try {
      database.insert(tableName, null, contentValues);
    } catch (Throwable t) {
      Log.e("Unable to insert action to database.", t);
      Util.handleException(t);
    }
  }

  /**
   * Gets first numbersOfActions actions from a table with name tableName.
   *
   * @param numberOfActions Number of actions.
   * @param tableName Table name.
   * @return List of actions.
   */
  static List<Map<String, Object>> getFirstNActions(int numberOfActions, String tableName) {
    List<Map<String, Object>> actions = new ArrayList<>();
    if (database == null) {
      return actions;
    }
    Cursor cursor = null;
    try {
      cursor = database.query(tableName, new String[] {COLUMN_ACTION}, null, null, null, null,
          KEY_ROWID + " ASC", "" + numberOfActions);
      cursor.moveToFirst();

      while (!cursor.isAfterLast()) {
        Map<String, Object> requestArgs = JsonConverter.mapFromJson(new JSONObject(
            cursor.getString(cursor.getColumnIndex(COLUMN_ACTION))));
        actions.add(requestArgs);
        cursor.moveToNext();
      }
    } catch (Throwable t) {
      Log.e("Unable to get actions from the table.", t);
      Util.handleException(t);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return actions;
  }

  /**
   * Deletes first numbersOfActions elements from a table with name tableName.
   *
   * @param tableName Table name.
   * @param numberOfActions Number of actions that need to be deleted.
   */
  static void deleteFirstNActions(String tableName, int numberOfActions) {
    if (database == null) {
      return;
    }
    try {
      database.delete(ACTIONS_TABLE_NAME, KEY_ROWID + " in (select " +
          KEY_ROWID + " from " + tableName + " LIMIT " + numberOfActions + ")", null);
    } catch (Throwable t) {
      Log.e("Unable to delete actions from the table.", t);
      Util.handleException(t);
    }
  }

  /**
   * Gets number of rows in the table with name tableName.
   *
   * @param tableName Table name.
   * @return Number of rows in the table with name tableName.
   */
  static long getCount(String tableName) {
    long count = 0;
    if (database == null) {
      return count;
    }
    try {
      count = DatabaseUtils.queryNumEntries(database, tableName);
    } catch (Throwable t) {
      Log.e("Unable to get a number of rows in the table.", t);
      Util.handleException(t);
    }
    return count;
  }
}
