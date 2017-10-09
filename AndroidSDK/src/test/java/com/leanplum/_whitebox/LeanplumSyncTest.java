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
package com.leanplum._whitebox;

import com.leanplum.Leanplum;
import com.leanplum.Var;
import com.leanplum.__setup.AbstractTest;
import com.leanplum.__setup.LeanplumTestHelper;
import com.leanplum._whitebox.utilities.RequestHelper;
import com.leanplum.callbacks.VariableCallback;
import com.leanplum.internal.JsonConverter;
import com.leanplum.internal.Socket;
import com.leanplum.internal.VarCache;
import com.leanplum.BuildConfig;

import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests Leanplum SDK calls and general functionality.
 *
 * @author Kiril Kafadarov, Aleksandar Gyorev, Ben Marten
 */
public class LeanplumSyncTest extends AbstractTest {
  /**
   * Tests the functionality of a file variable.
   * <p>
   * 1. Create a test txt file variable 2. Start Leanplum 3. Check if Leanplum sends a "setVars"
   * request containing the created test txt file.
   */
  @Test
  public void testFileVariables() throws Exception {
    // This test is not valid for RELEASE builds, as the feature only exists for DEV.
    if (!BuildConfig.DEBUG) {
      return;
    }

    setupSDK(mContext, "/responses/files_start_response.json");

    final String fileName = "test_file.txt";
    final String fileContent = "test";

    final File file = LeanplumTestHelper.createFile(mContext, fileName, fileContent);

    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        Var<String> varTestFile = VarCache.getVariable(fileName);
        assertNotNull(params);
        assertEquals(file.getAbsolutePath(), varTestFile.value());

        Map<String, Object> fileAttributes = JsonConverter.fromJson(
            params.get("fileAttributes").toString());
        assertNotNull(fileAttributes);

        @SuppressWarnings("unchecked") Map<String, Map<String, Integer>> fileData =
            (Map<String, Map<String, Integer>>) fileAttributes.get(file.getAbsolutePath());
        assertEquals(4, (int) fileData.get("").get("size"));

        String varTestFileContents = LeanplumTestHelper.readFile(varTestFile);

        assertNotNull(varTestFileContents);
        assertEquals("test", varTestFileContents);

        assertTrue(file.delete());
      }
    });

    Var<String> fileVar = Var.defineFile(fileName, file.getAbsolutePath());

    fileVar.addFileReadyHandler(new VariableCallback<String>() {
      @Override
      public void handle(Var<String> variable) {
        assertNotNull(variable);
        assertEquals("test_file.txt", variable.name());
      }
    });

    // Invoke method "getVariables" that is being called on the similar named socket event.
    // Ignore connection errors, because we only want to simulate a response here.
    //noinspection EmptyCatchBlock
    try {
      (new Socket()).handleGetVariablesEvent();
    } catch (Exception e) {
    }
  }

  /**
   * Tests if a file is picked up by LP resource syncing mechanism and synced to server.
   * <p>
   * 1. Create a xml layout file inside the apk bundle. 2. Start Leanplum. 3. Check if Leanplum
   * sends a "setVars" request containing the created test xml file.
   */
  @Test
  public void testResourceSyncing() throws Exception {
    // starts the SDK first
    setupSDK(mContext, "/responses/simple_start_response.json");

    // This test is not valid for RELEASE builds, as the feature only exists for DEV.
    if (!BuildConfig.DEBUG) {
      return;
    }

    final String testLayoutFile = "res/layout/sampleLayout.xml";
    final String testLayoutData = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
        "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\">" +
        "</LinearLayout>";

    // Create a zip app apk bundle to simulate running on device.
    LeanplumTestHelper.createZipFile(mContext, testLayoutFile, testLayoutData);

    RequestHelper.addRequestHandler(new RequestHelper.RequestHandler() {
      @Override
      public void onRequest(String httpMethod, String apiMethod, Map<String, Object> params) {
        String attr = params.get("fileAttributes").toString();
        Map<String, Object> fileAttributes = JsonConverter.fromJson(attr);
        assertNotNull(fileAttributes);

        assertNotNull(fileAttributes.get(testLayoutFile));
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Number>> fileDetailsRaw =
            (Map<String, Map<String, Number>>) fileAttributes.get(testLayoutFile);
        @SuppressWarnings("unchecked")
        Map.Entry<String, Map<String, Number>> fileDetailsEntry =
            (Map.Entry<String, Map<String, Number>>) fileDetailsRaw.entrySet().toArray()[0];
        Map<String, Number> fileDetails = fileDetailsEntry.getValue();
        assertNotNull(fileDetails);
        int fileSize = (int) fileDetails.get("size");
        assertEquals(126, fileSize);
      }
    });

    Leanplum.syncResources(Collections.singletonList(".*.xml"), null);

    // Invoke method "getVariables" that is being called on the similar named socket event.
    // Ignore connection errors, because we only want to simulate a response here.
    //noinspection EmptyCatchBlock
    try {
      (new Socket()).handleGetVariablesEvent();
    } catch (Exception e) {
    }
  }
}
