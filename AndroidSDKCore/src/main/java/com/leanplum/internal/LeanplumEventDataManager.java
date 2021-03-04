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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.leanplum.Leanplum;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LeanplumEventDataManager class to work with SQLite.
 *
 * @author Anna Orlova
 */
public class LeanplumEventDataManager {

  private static LeanplumEventDataManager instance;

  private static final String DATABASE_NAME = "__leanplum.db";
  private static final int DATABASE_VERSION = 1;
  private static final String EVENT_TABLE_NAME = "event";
  private static final String COLUMN_DATA = "data";
  private static final String KEY_ROWID = "rowid";

  private SQLiteDatabase database;
  private LeanplumDataBaseManager databaseManager;
  private ContentValues contentValues = new ContentValues();
  private boolean hasDatabaseError = false;

  private LeanplumEventDataManager() {
    try {
      Context context = Leanplum.getContext();

      if (context == null) {
        return;
      }

      if (databaseManager == null) {
        databaseManager = new LeanplumDataBaseManager(Leanplum.getContext());
      }
      database = databaseManager.getWritableDatabase();

    } catch (Throwable t) {
      handleSQLiteError("Cannot create database.", t);
    }
  }

  public static LeanplumEventDataManager sharedInstance() {
    if (instance == null) {
      instance = new LeanplumEventDataManager();
    }
    return instance;
  }

  /**
   * Inserts event to event table.
   *
   * @param event String with json of event.
   */
  void insertEvent(String event) {
    if (database == null) {
      return;
    }
    contentValues.put(COLUMN_DATA, event);
    try {
      database.insert(EVENT_TABLE_NAME, null, contentValues);
      hasDatabaseError = false;
    } catch (Throwable t) {
      handleSQLiteError("Unable to insert event to database.", t);
    }
    contentValues.clear();
  }

  /**
   * Gets first count events from event table.
   *
   * @param count Number of events.
   * @return List of events.
   */
  List<Map<String, Object>> getEvents(int count) {
    List<Map<String, Object>> events = new ArrayList<>();
    if (database == null) {
      return events;
    }
    Cursor cursor = null;
    try {
      cursor = database.query(EVENT_TABLE_NAME, new String[] {COLUMN_DATA}, null, null, null,
          null, KEY_ROWID + " ASC", "" + count);
      hasDatabaseError = false;
      while (cursor.moveToNext()) {
        Map<String, Object> requestArgs = JsonConverter.mapFromJson(new JSONObject(
            cursor.getString(cursor.getColumnIndex(COLUMN_DATA))));
        events.add(requestArgs);
      }
    } catch (Throwable t) {
      handleSQLiteError("Unable to get events from the table.", t);
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
  void deleteEvents(int count) {
    if (database == null) {
      return;
    }
    try {
      database.delete(EVENT_TABLE_NAME, KEY_ROWID + " in (select " + KEY_ROWID + " from " +
          EVENT_TABLE_NAME + " ORDER BY " + KEY_ROWID + " ASC LIMIT " + count + ")", null);
      hasDatabaseError = false;
    } catch (Throwable t) {
      handleSQLiteError("Unable to delete events from the table.", t);
    }
  }

  /**
   * Gets number of rows in the event table.
   *
   * @return Number of rows in the event table.
   */
  long getEventsCount() {
    long count = 0;
    if (database == null) {
      return count;
    }
    try {
      count = DatabaseUtils.queryNumEntries(database, EVENT_TABLE_NAME);
      hasDatabaseError = false;
    } catch (Throwable t) {
      handleSQLiteError("Unable to get a number of rows in the table.", t);
    }
    return count;
  }

  /**
   * Whether we are going to send error log or not.
   */
  boolean hasDatabaseError() {
    return hasDatabaseError;
  }

  /**
   * Helper function that logs and sends errors to the server.
   */
  private void handleSQLiteError(String log, Throwable t) {
    Log.e(log, t);
    if (!hasDatabaseError) {
      hasDatabaseError = true;
      // Sending error log. It will be intercepted when requests are sent.
      Log.exception(t);
    }
  }

  private static class LeanplumDataBaseManager extends SQLiteOpenHelper {

    LeanplumDataBaseManager(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
