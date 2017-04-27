// Copyright 2013, Leanplum, Inc.

package com.leanplum.internal;

import com.leanplum.callbacks.StartCallback;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Registration {
  public static void registerDevice(String email, final StartCallback callback) {
    Map<String, Object> params = new HashMap<>();
    params.put(Constants.Params.EMAIL, email);
    Request request = Request.post(Constants.Methods.REGISTER_FOR_DEVELOPMENT, params);
    request.onResponse(new Request.ResponseCallback() {
      @Override
      public void response(final JSONObject response) {
        OsHandler.getInstance().post(new Runnable() {
          @Override
          public void run() {
            try {
              JSONObject registerResponse = Request.getLastResponse(response);
              boolean isSuccess = Request.isResponseSuccess(registerResponse);
              if (isSuccess) {
                if (callback != null) {
                  callback.onResponse(true);
                }
              } else {
                Log.e(Request.getResponseError(registerResponse));
                if (callback != null) {
                  callback.onResponse(false);
                }
              }
            } catch (Throwable t) {
              Util.handleException(t);
            }
          }
        });
      }
    });
    request.onError(new Request.ErrorCallback() {
      @Override
      public void error(final Exception e) {
        OsHandler.getInstance().post(new Runnable() {
          @Override
          public void run() {
            if (callback != null) {
              callback.onResponse(false);
            }
          }
        });
      }
    });
    request.sendIfConnected();
  }
}
