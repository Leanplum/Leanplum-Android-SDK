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
package com.leanplum.internal;

import com.leanplum.__setup.AbstractTest;
import com.leanplum._whitebox.utilities.ResponseHelper;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Tests the file manager.
 *
 * @author Milos Jakovljevic
 */
public class FileManagerTest extends AbstractTest {
  @Override
  public void after() {
    // Do nothing.
  }

  @Test
  public void testFileRelativeToAppBundle() {
    String file = FileManager.fileRelativeToAppBundle("file.png");
    assertEquals("/file.png", file);
  }

  @Test
  public void testFileRelativeToDocuments() {
    String file = FileManager.fileRelativeToDocuments("leanplum_watermark.jpg");
    assertNotNull(file);
  }

  @Test
  public void testFileRelativeToLPBundle() {
    String file = FileManager.fileRelativeToLPBundle("leanplum_watermark.jpg");
    assertNotNull(file);
  }

  @Test
  public void testFileExists() {
    assertFalse(FileManager.fileExistsAtPath("leanplum_watermark.jpg"));
    assertTrue(FileManager.fileExistsAtPath(getClass()
        .getResource("/responses/simple_start_response.json").getPath()));
  }

  @Test
  public void testFileDownload() {
    setupSDK(mContext, "/responses/simple_start_response.json");

    // Seed a file as response which will be downloaded.
    ResponseHelper.seedResponse("/responses/simple_start_response.json");

    // Download file and assert if it is saved.
    FileManager.maybeDownloadFile(false, "test.png", "test_default.png", null, new Runnable() {
      @Override
      public void run() {
        String path = FileManager.fileRelativeToDocuments("test.png");
        assertTrue(FileManager.fileExistsAtPath(path));
      }
    });
  }
}
