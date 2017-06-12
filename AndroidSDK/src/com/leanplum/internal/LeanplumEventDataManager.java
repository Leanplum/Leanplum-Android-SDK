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

import android.database.DatabaseErrorHandler;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * LeanplumEventDataManager class to work with SQLite.
 *
 * @author Anna Orlova
 */
public class LeanplumEventDataManager {
  private static final String DATABASE_NAME = "__leanplum.db";
  private static final int DATABASE_VERSION = 1;
  private static final String EVENT_TABLE_NAME = "event";
  private static final String COLUMN_DATA = "data";
  private static final String KEY_ROWID = "rowid";

  private static SQLiteDatabase database;
  private static LeanplumDataBaseHelper databaseHelper;
  private static ContentValues contentValues = new ContentValues();

  public static void init(Context context) {
    if (database != null) {
      Log.e("Database is already initialized.");
      return;
    }
    File dbFile = context.getDatabasePath(DATABASE_NAME);
    boolean needMigration = !dbFile.exists();

    // Create database if needed.
    try {
      if(databaseHelper==null) {
        databaseHelper = new LeanplumDataBaseHelper(context, DATABASE_NAME);
      }
      database = databaseHelper.getWritableDatabase();
    } catch (Throwable t) {
      Log.e("Cannot create database.", t);
      Util.handleException(t);
    }

    // Migrate old data from shared preferences if needed.
    if (database != null && needMigration) {
      try {
        Request.moveOldDataFromSharedPreferences();
      } catch (Throwable t) {
        Log.e("Cannot move old data from shared preferences to SQLite table.", t);
        Util.handleException(t);
      }
    }
  }

  /**
   * Inserts event to event table.
   *
   * @param event String with json of event.
   */
  static void insertEvent(String event) {
    if (database == null) {
      return;
    }
    contentValues.put(COLUMN_DATA, event);
    try {
      database.insert(EVENT_TABLE_NAME, null, contentValues);
    } catch (Throwable t) {
      Log.e("Unable to insert event to database.", t);
      Util.handleException(t);
    }
  }

  /**
   * Gets first count events from event table.
   *
   * @param count Number of events.
   * @return List of events.
   */
  static List<Map<String, Object>> getEvents(int count) {
    List<Map<String, Object>> events = new ArrayList<>();
    if (database == null) {
      return events;
    }
    Cursor cursor = null;
    try {
      cursor = database.query(EVENT_TABLE_NAME, new String[] {COLUMN_DATA}, null, null, null,
          null, KEY_ROWID + " ASC", "" + count);
      if (cursor.moveToFirst()) {
        while (!cursor.isAfterLast()) {
          Map<String, Object> requestArgs = JsonConverter.mapFromJson(new JSONObject(
              cursor.getString(cursor.getColumnIndex(COLUMN_DATA))));
          events.add(requestArgs);
          cursor.moveToNext();
        }
      }
    } catch (Throwable t) {
      Log.e("Unable to get events from the table.", t);
      Util.handleException(t);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return events;
  }

  /**
   * Deletes first count elements from event table.
   *
   * @param count Number of event that need to be deleted.
   */
  static void deleteEvents(int count) {
    if (database == null) {
      return;
    }
    try {
      database.delete(EVENT_TABLE_NAME, KEY_ROWID + " in (select " +
          KEY_ROWID + " from " + EVENT_TABLE_NAME + " LIMIT " + count + ")", null);
    } catch (Throwable t) {
      Log.e("Unable to delete events from the table.", t);
      Util.handleException(t);
    }
  }

  /**
   * Gets number of rows in the event table.
   *
   * @return Number of rows in the event table.
   */
  static long getEventsCount() {
    long count = 0;
    if (database == null) {
      return count;
    }
    try {
      count = DatabaseUtils.queryNumEntries(database, EVENT_TABLE_NAME);
    } catch (Throwable t) {
      Log.e("Unable to get a number of rows in the table.", t);
      Util.handleException(t);
    }
    return count;
  }

  private static class LeanplumDataBaseHelper extends SQLiteOpenHelper {
    LeanplumDataBaseHelper(final Context context, final String name) {
      super(context, name, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      // Create event table.
      db.execSQL("CREATE TABLE IF NOT EXISTS " + EVENT_TABLE_NAME + "(" + COLUMN_DATA +
          " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      // No used for now.
    }
  }
}
