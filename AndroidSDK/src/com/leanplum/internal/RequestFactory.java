// Copyright 2014, Leanplum, Inc.

package com.leanplum.internal;

import java.util.Map;

public class RequestFactory {
  public static RequestFactory defaultFactory;

  public synchronized static RequestFactory getInstance() {
    if (defaultFactory == null) {
      defaultFactory = new RequestFactory();
    }
    return defaultFactory;
  }

  public Request createRequest(
      String httpMethod, String apiMethod, Map<String, Object> params) {
    return new Request(httpMethod, apiMethod, params);
  }
}
