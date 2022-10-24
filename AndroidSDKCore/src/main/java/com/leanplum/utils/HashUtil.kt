package com.leanplum.utils

import java.security.MessageDigest

object HashUtil {

  private fun ByteArray.toHex(limit: Int): String =
    joinToString(separator = "", limit = limit, truncated = "") { eachByte ->
      "%02x".format(eachByte)
    }

  /**
   * Returns hexadecimal representation of sha256 on the input string.
   */
  fun sha256(text: String, limit: Int = 32): String {
    return MessageDigest
      .getInstance("SHA-256")
      .digest(text.toByteArray(Charsets.UTF_8))
      .toHex(limit)
  }

  /**
   * Get first 10 symbols from hexadecimal representation of sha256.
   */
  fun sha256_40(text: String) = sha256(text, 5)

  /**
   * Get first 32 symbols from hexadecimal representation of sha256.
   */
  fun sha256_128(text: String) = sha256(text, 16)
}
