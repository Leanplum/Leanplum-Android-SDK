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

import static com.leanplum.utils.TestConstants.ROBOLECTRIC_CONFIG_SDK_VERSION;

import android.content.Context;
import android.content.SharedPreferences;

import com.leanplum.__setup.LeanplumTestApp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.security.Provider;
import java.security.Security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for encryption operations.
 * <p>
 * This test relies on package-private access to com.leanplum classes, and must be located in that
 * package.
 *
 * @author Ed Pizzi (ed@leanplum.com), Ben Marten (ben@leanplum.com)
 */
@SuppressWarnings("unused")
@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = ROBOLECTRIC_CONFIG_SDK_VERSION,
    application = LeanplumTestApp.class
)
@PowerMockIgnore({
    "org.mockito.*",
    "org.robolectric.*",
    "android.*",
    "javax.*",
    "org.bouncycastle.*",
    "jdk.internal.reflect.*"
})
public class AESCryptTest {
  private static final String APP_ID = "app_abcdefg1234567";
  private static final String TOKEN = "8416c513ddcfc27b";

  /**
   * A V0 ciphertext representing "oatmeal".
   * <p>
   * Encoded from a previous version of the SDK to ensure backward-compatibility.
   */
  private static final String V0_OATMEAL =
      "[-64, 16, 28, -106, -76, 40, 58, 43, -55, 86, -89, 100, -102, 70, -88, -26]";

  /**
   * A V1 ciphertext representing "oatmeal".
   * <p>
   * Hard-coded to ensure backward-compatibility.
   */
  private static final String V1_OATMEAL =
      "01[37, -94, -6, 110, -78, 79, 70, 118, -16, 4, -119, -96, 73, -33, -27, 5]";

  private final AESCrypt aesCrypt = new AESCrypt(APP_ID, TOKEN);
  @Mock
  Context context;
  private SharedPreferences preferences;

  private static String legacyEncrypt(String password, String plaintext) {
    return AESCrypt.encryptInternal(password, plaintext);
  }

  private static String legacyEncrypt(String plaintext) {
    return legacyEncrypt(TOKEN, plaintext);
  }

  @Before
  public void setUp() {
    Provider provider = new org.bouncycastle.jce.provider.BouncyCastleProvider();
    Security.addProvider(provider);
    preferences = RuntimeEnvironment.application.getSharedPreferences("__leanplum__",
        Context.MODE_PRIVATE);
  }

  @After
  public void tearDown() {
    SharedPreferences.Editor editor = preferences.edit();
    if (editor != null) {
      editor.clear();
      editor.apply();
    }
  }

  @Test
  public void testEncryptDecrypt() {
    String encrypted = new AESCrypt(APP_ID, TOKEN).encrypt("applesauce");
    assertNotNull(encrypted);
    assertTrue(encrypted.startsWith("01["));
    assertTrue(encrypted.endsWith("]"));
    assertEquals("applesauce", aesCrypt.decrypt(encrypted));
  }

  @Test
  public void testLegacyDecrypt() {
    String legacyEncrypted = legacyEncrypt("bananas");
    assertNotNull(legacyEncrypted);
    assertTrue(legacyEncrypted.startsWith("["));
    assertEquals("bananas", aesCrypt.decrypt(legacyEncrypted));
    assertTrue(!legacyEncrypted.equals(aesCrypt.encrypt("bananas")));
  }

  /**
   * Ensures that we can decrypt values encrypted by prior versions of the encryption code.
   * <p>
   * This uses hard-coded encrypted values produced by older versions of the SDK, rather than
   * simulating old code by encrypting old-style values within our current code. This is a subtle
   * difference, but technically our other tests only verify self-compatibility, rather than
   * compatibility with prior versions of the code.
   */
  @Test
  public void testDecryptCompatibility() {
    assertEquals("oatmeal", aesCrypt.decrypt(V0_OATMEAL));
    assertEquals("oatmeal", aesCrypt.decrypt(V1_OATMEAL));
  }

  @Test
  public void testDecryptFailureReturnsNull() {
    // Current encryption:
    String badValue = new AESCrypt("badKey", null).encrypt("bananas");
    assertEquals(null, aesCrypt.decrypt(badValue));
    // Legacy encryption:
    String legacyBadValue = legacyEncrypt("badKey", "bananas");
    assertEquals(null, aesCrypt.decrypt(legacyBadValue));
  }

  /**
   * Test that we discard legacy values that decode to what appears to be legacy ciphertext.
   * <p>
   * corruptedValue here reflects the state of many corrupt values on production SDKs. The value
   * couldn't be decrypted using at some time in the past, so the encrypted value was used directly.
   * That encrypted value is now stored, re-encrypted with the current key.
   */
  @Test
  public void testIgnoreLegacyCorruptedValue() {
    String legacyEncrypted = legacyEncrypt("lost password", "bananas");
    String corruptedValue = legacyEncrypt(legacyEncrypted);
    // We now reject legacy values that look like ciphertexts after decryption.
    assertEquals(null, aesCrypt.decrypt(corruptedValue));

    // Note that we do not do this kind of checking for non-legacy encryption.
    String doubleEncryptedValue = aesCrypt.encrypt(legacyEncrypted);
    assertEquals(legacyEncrypted, aesCrypt.decrypt(doubleEncryptedValue));
  }

  /**
   * Variants look a little like ciphertexts. Make sure that they are not ignored.
   */
  @Test
  public void testLegacyVariantsNotIgnored() {
    // A variant string, taken from the SDK test app.
    String variantString = "[{\"id\":5747559595245568},{\"id\":5780683523883008}," +
        "{\"id\":5303286097772544},{\"id\":5285333134475264}]";
    String legacyVariants = legacyEncrypt(variantString);
    assertEquals(variantString, aesCrypt.decrypt(legacyVariants));
  }

  @Test
  public void testDecodePreference() {
    SharedPreferences.Editor editor = preferences.edit();
    editor.putString("EncryptTest.foo", aesCrypt.encrypt("oatmeal"));
    editor.apply();
    assertEquals("oatmeal", aesCrypt.decodePreference(preferences, "EncryptTest.foo", "{}"));
  }

  /**
   * Verifies that defaults are passed through without attempting to decrypt them.
   */
  @Test
  public void testPreferenceDefault() {
    assertEquals("{}", aesCrypt.decodePreference(preferences, "EncryptTest.notFound", "{}"));
    assertEquals(null, aesCrypt.decodePreference(preferences, "EncryptTest.notFound", null));
  }

  @Test
  public void testNullSafety() {
    assertEquals(null, aesCrypt.encrypt(null));
    assertEquals(null, aesCrypt.decrypt(null));
    AESCrypt nullAesCrypt = new AESCrypt(null, null);
    assertEquals(null, nullAesCrypt.encrypt("foo"));
    assertEquals(null, nullAesCrypt.decrypt(V0_OATMEAL));
    assertEquals(null, nullAesCrypt.decrypt(V1_OATMEAL));
  }
}

