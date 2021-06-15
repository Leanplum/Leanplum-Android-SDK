package com.leanplum;

import androidx.annotation.NonNull;

/**
 * Represents the variables in JSON format, cryptographically signed from Leanplum server.
 */
public class SecuredVars {
  private String json;
  private String signature;

  public SecuredVars(@NonNull String json, @NonNull String signature) {
    this.json = json;
    this.signature = signature;
  }

  /**
   * Get JSON of the variables.
   *
   * @return The JSON representation of the variables as received from Leanplum server.
   */
  @NonNull
  public String getJson() {
    return json;
  }

  /**
   * Get the cryptographic signature of the variables.
   *
   * @return The signature of the variables.
   */
  @NonNull
  public String getSignature() {
    return signature;
  }
}
