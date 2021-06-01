package com.leanplum;

/**
 * TODO javadoc
 */
public class LPVars {
  private String json;
  private String signature;

  public LPVars(String json, String signature) {
    this.json = json;
    this.signature = signature;
  }

  /**
   * TODO javadoc
   * @return
   */
  public String getJson() {
    return json;
  }

  /**
   * TODO javadoc
   * @return
   */
  public String getSignature() {
    return signature;
  }
}
