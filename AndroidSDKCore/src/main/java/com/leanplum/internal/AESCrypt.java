/*
 * Copyright 2013, Leanplum, Inc. All rights reserved.
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

import android.content.SharedPreferences;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

/**
 * AES Encryption as detailed at
 * http://nelenkov.blogspot.com/2012/04/using-password-based-encryption-on.html
 *
 * @author Aakash Patel
 */
public class AESCrypt {
    static {
        //Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }
    private static enum EncryptionType {
        /**
         * Encryption based on a token received from the server. Used in SDK versions prior to 1.2.20.
         * <p>
         * Corresponds to ciphertexts of the format "[12, -33, 52]", corresponding to Arrays.toString()
         * of an encrypted byte[].
         * <p>
         * Legacy values may decrypt to ciphertexts. We ignore what appear to be double-encrypted
         * ciphertexts that are stored in a legacy format.
         */
        LEGACY_TOKEN(0),

        /**
         * Encryption based on the app ID. Used in SDK versions since 1.2.20.
         * <p>
         * Corresponds to ciphertexts of the format "01[12, -33, 52]". The format adds a version
         * identifier ("01") prefix to ciphertexts, allowing us to change the format in the future.
         * <p>
         * With the exception of LEGACY_TOKEN ciphertexts, which must continue to be supported, we will
         * use the first two characters to determine the encryption protocol.
         */
        APP_ID_KEY(1);

        public final int id;
        public final String prefix;
        public final String prefixWithBracket;

        EncryptionType(int id) {
            this.id = id;
            prefix = String.format("%02d", id);
            prefixWithBracket = prefix + "[";
        }

        private static EncryptionType forId(int id) {
            if (id == 1) {
                return APP_ID_KEY;
            }
            return null;
        }

        public static Pair<EncryptionType, String> parseCipherText(String cipherText) {
            if (cipherText == null || cipherText.isEmpty()) {
                return null;
            }
            if (cipherText.startsWith("[")) {
                return Pair.create(LEGACY_TOKEN, cipherText);
            }
            if (cipherText.startsWith(APP_ID_KEY.prefixWithBracket)) {
                return Pair.create(
                        APP_ID_KEY, cipherText.substring(APP_ID_KEY.prefixWithBracket.length() - 1));
            }
            return null;
        }
    }

    // Build prefix and suffix strings longhand, to obfuscate them slightly.
    // Probably doesn't matter.
    private static final String APP_ID_KEY_PREFIX = new StringBuilder()
            .append("L").append("q").append(3).append("f").append("z").toString();
    private static final String APP_ID_KEY_SUFFIX = new StringBuilder()
            .append("b").append("L").append("t").append("i").append(2).toString();

    private final String appId;
    private final String token;

    /**
     * Creates an AESCrypt encryption context.
     * <p>
     * Intended for short-term use, since the encryption token can change.
     */
    public AESCrypt(String appId, String token) {
        this.appId = appId;
        this.token = token;
    }

    private String appIdKeyPassword() {
        return APP_ID_KEY_PREFIX + appId + APP_ID_KEY_SUFFIX;
    }

    /**
     * Creates a ciphertext using a password based on current context parameters.
     *
     * @param plaintext
     * @return A cipher text string, or null if encryption fails (unexpected).
     */
    public String encrypt(String plaintext) {
        Log.e("Encrypt called with " + plaintext);
        assertNotNull(plaintext);
        if (plaintext == null) {
            return null;
        }
        Log.e("2 Encrypt called with " + plaintext);
        // Always encrypt using the APP_ID_KEY method.
        assertNotNull(appId);
        assertFalse(appId.isEmpty());
        if (appId == null || appId.isEmpty()) {
            Log.e("Encrypt called with null appId.");
            return null;
        }
        Log.e("3 Encrypt called with " + plaintext);
        String cipherText = encryptInternal(appIdKeyPassword(), plaintext);
        assertNotNull(cipherText);
        if (cipherText == null) {
            Log.w("Failed to encrypt.");
            return null;
        }
        Log.e("4 Encrypt called with " + cipherText);
        if (cipherText.isEmpty() || cipherText.equals(plaintext) || !cipherText.startsWith("[")) {
            Log.w("Invalid ciphertext: " + cipherText);
            assertEquals("foo", cipherText);
            return null;
        }
        Log.e("5 Encrypt called with " + plaintext);
        return EncryptionType.APP_ID_KEY.prefix + cipherText;
    }

    public String decodePreference(SharedPreferences preferences, String key, String defaultValue) {
        String cipherText = preferences.getString(key, null);
        if (cipherText == null) {
            return defaultValue;
        }
        String decoded = decrypt(cipherText);
        if (decoded == null) {
            return defaultValue;
        }
        return decoded;
    }

    /**
     * Decrypts a ciphertext in either legacy or current format, using a password based on context
     * parameters.
     *
     * @param cipherText The value to encrypt; tolerates null.
     * @return A cipher text string, or null if the value can't be decrypted.
     */
    public String decrypt(String cipherText) {
        Pair<EncryptionType, String> encryptionSpec = EncryptionType.parseCipherText(cipherText);
        String result = null;
        if (encryptionSpec == null) {
            Log.v("Got null encryptionSpec for encrypted: " + cipherText);
        } else {
            switch (encryptionSpec.first) {
                case LEGACY_TOKEN:
                    if (token == null || token.isEmpty()) {
                        Log.e("Decrypt called with null token.");
                    } else {
                        result = decryptInternal(token, encryptionSpec.second);
                        // For legacy keys only -- detect if the value we decode is a valid legacy ciphertext.
                        // If so, it was almost certainly produced by legacy decryption, which would return
                        // ciphertext on decryption failure. Discard the value and return null.
                        if (result != null && parseCiphertextInternal(result) != null) {
                            Log.e("Discarding legacy value that appears to be an encrypted value: " +
                                    result);
                            return null;
                        }
                    }
                    break;
                case APP_ID_KEY:
                    if (appId == null || appId.isEmpty()) {
                        Log.e("Decrypt called with null appId.");
                    } else {
                        result = decryptInternal(appIdKeyPassword(), encryptionSpec.second);
                    }
                    break;
            }
        }
        if (result == null) {
            Log.w("Unable to decrypt " + cipherText);
        }
        return result;
    }

    /**
     * Encrypts the plaintext using password. In case of exception, returns null.
     */
    // VisibleForTesting
    public static String encryptInternal(String password, String plaintext) {
        try {
            Log.e("3 encryptInternal password with " + password);
            Log.e("3 encryptInternal plaintext with " + plaintext);
            byte[] textBytes = plaintext.getBytes("UTF-8");
            Log.e("3 encryptInternal textBytes with " + textBytes);
            byte[] encryptedBytes = performCryptOperation(Cipher.ENCRYPT_MODE, password, textBytes);
            Log.e("3 encryptInternal encryptedBytes with " + encryptedBytes);
            if (encryptedBytes == null) {
                Log.e("3 encryptInternal called null encryptedBytes! ");
            }
            return Arrays.toString(encryptedBytes);
        } catch (UnsupportedEncodingException e) {
            Log.w("Unable to encrypt " + plaintext, e);
            assertEquals(password, "UnsupportedEncodingException");
            return null;
        }
    }

    private static byte[] parseCiphertextInternal(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        ciphertext = ciphertext.trim();
        if (ciphertext.length() < 2) {
            return null;
        }
        try {
            String[] byteStrings =
                    ciphertext.substring(1, ciphertext.length() - 1).trim().split("\\s*,\\s*");
            byte[] bytes = new byte[byteStrings.length];
            for (int i = 0; i < byteStrings.length; i++) {
                bytes[i] = Byte.parseByte(byteStrings[i]);
            }
            return bytes;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Decrypts the ciphertext using password. In case of exception, returns null.
     *
     * @param ciphertext Must be a valid byte array represented as a string as returned by
     *                   Arrays.toString().
     */
    private static String decryptInternal(String password, String ciphertext) {
        try {
            byte[] bytes = parseCiphertextInternal(ciphertext);
            if (bytes == null) {
                Log.w("Invalid ciphertext: " + ciphertext);
                return null;
            }
            byte[] byteResult = performCryptOperation(Cipher.DECRYPT_MODE, password, bytes);
            if (byteResult != null) {
                return new String(byteResult, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            // Unreachable on android, which guarantees UTF-8 support.
            Log.w("Could not encode UTF8 string.\n" + Log.getStackTraceString(e));
        }
        return null;
    }

    /**
     * Performs either an encryption or a decryption based on the mode. In case of exception, returns
     * null.
     *
     * @param mode     Should be either Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
     * @param password The password to crypt.
     * @param text     The text to crypt.
     * @return The result of the crypt.
     */
    private static byte[] performCryptOperation(int mode, String password, byte[] text) {
        byte[] result = null;
        try {
            byte[] SALT = Constants.Crypt.SALT.getBytes("UTF-8");
            Log.e("performCryptOperation SALT " + SALT);
            byte[] IV = Constants.Crypt.IV.getBytes("UTF-8");
            Log.e("performCryptOperation IV " + IV);
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), SALT, Constants.Crypt.ITER_COUNT,
                    Constants.Crypt.KEY_LENGTH);
            Log.e("performCryptOperation keySpec " + keySpec);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEwithMD5AND128BITAES-CBC-OPENSSL", "BC");
            Log.e("performCryptOperation keyFactory " + keyFactory);
            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            Log.e("performCryptOperation keyBytes " + keyBytes);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            Log.e("performCryptOperation key " + key);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            Log.e("performCryptOperation cipher " + cipher);
            IvParameterSpec ivParams = new IvParameterSpec(IV);
            Log.e("performCryptOperation ivParams " + ivParams);
            cipher.init(mode, key, ivParams);

            Log.e("performCryptOperation cipher " + cipher);
            result = cipher.doFinal(text);
        } catch (InvalidKeyException e) {
            Log.e("performCryptOperation InvalidKeyException");
            Log.e(e.getLocalizedMessage());
            Log.e(e);
            e.printStackTrace();

        } catch (NoSuchAlgorithmException e) {
            Log.e("performCryptOperation NoSuchAlgorithmException");
            Log.e(e);
            Provider[] providers = Security.getProviders();
            for (Provider provider : providers) {
                boolean printedProvider = false;
                Set<Provider.Service> services = provider.getServices();
                for (Provider.Service service : services) {
                    String algorithm = service.getAlgorithm();
                    String type = service.getType();
                    if (type.equalsIgnoreCase("SecretKeyFactory")) {
                        if (!printedProvider) {
                            System.out.printf("%n === %s ===%n%n", provider.getName());
                            printedProvider = true;
                        }
                        System.out.printf("Type: %s alg: %s%n", type, algorithm);
                    }
                }
            }
        } catch (NoSuchProviderException e) {
            Log.e("performCryptOperation NoSuchProviderException");
            Log.e(e);
            e.printStackTrace();
            Provider[] providers = Security.getProviders();
            for (Provider provider : providers) {
                System.out.printf("%n === %s ===%n%n", provider.toString());
                boolean printedProvider = false;
                Set<Provider.Service> services = provider.getServices();
                for (Provider.Service service : services) {
                    String algorithm = service.getAlgorithm();
                    String type = service.getType();
                    if (type.equalsIgnoreCase("SecretKeyFactory")) {
                        if (!printedProvider) {
                            System.out.printf("%n === %s ===%n%n", provider.getName());
                            printedProvider = true;
                        }
                        System.out.printf("Type: %s alg: %s%n", type, algorithm);
                    }
                }
            }
        } catch (NoSuchPaddingException e) {
            Log.e("performCryptOperation NoSuchPaddingException");
        } catch (InvalidAlgorithmParameterException e) {
            Log.e("performCryptOperation InvalidAlgorithmParameterException");
        } catch (IllegalBlockSizeException e) {
            Log.e("performCryptOperation IllegalBlockSizeException");
        } catch (BadPaddingException e) {
            Log.e("performCryptOperation BadPaddingException");
        } catch (UnsupportedEncodingException e) {
            Log.e("performCryptOperation UnsupportedEncodingException");
        } catch (InvalidKeySpecException e) {
            Log.e("performCryptOperation InvalidKeySpecException");
        }
        Log.e("performCryptOperation result " + result);
        return result;
    }
}
