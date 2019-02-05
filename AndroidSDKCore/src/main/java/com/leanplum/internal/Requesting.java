package com.leanplum.internal;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;


public interface Requesting {
  String apiMethod = null;
  Map<String, Object> params = null;
  ResponseCallback response = null;
  ErrorCallback error = null;
  boolean sent = false;
  String requestId = null;
  long dataBaseIndex = -1;

  void setSent(boolean sent);

  void setDataBaseIndex(long dataBaseIndex);

  long getDataBaseIndex();

  interface ResponseCallback {
    void response(JSONObject response);
  }

  interface ApiResponseCallback {
    void response(List<Map<String, Object>> requests, JSONObject response, int countOfEvents);
  }

  interface ErrorCallback {
    void error(Exception e);
  }

  interface NoPendingDownloadsCallback {
    void noPendingDownloads();
  }

}
