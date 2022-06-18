/*
 * Copyright 2022, Leanplum, Inc. All rights reserved.
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.text.TextUtils;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.actions.Action;
import com.leanplum.actions.ActionManagerDefinitionKt;
import com.leanplum.actions.ActionManagerExecutionKt;
import com.leanplum.callbacks.VariablesChangedCallback;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Leanplum socket class, that handles connections to the Leanplum remote socket.
 *
 * @author Andrew First, Ben Marten
 */
// Suppressing deprecated apache dependency.
@SuppressWarnings("deprecation")
public class Socket {
  private static final String TAG = "Leanplum";
  private static final String EVENT_CONTENT_RESPONSE = "getContentResponse";
  private static final String EVENT_UPDATE_VARS = "updateVars";
  private static final String EVENT_TRIGGER = "trigger";
  private static final String EVENT_GET_VARIABLES = "getVariables";
  private static final String EVENT_GET_ACTIONS = "getActions";
  private static final String EVENT_REGISTER_DEVICE = "registerDevice";
  private static final String EVENT_APPLY_VARS = "applyVars";

  private static Socket instance;
  private static boolean requestNewConnection;

  private volatile SocketIOClient sio;
  private Timer reconnectTimer;
  private boolean authSent;
  private boolean connected = false;
  private boolean connecting = false;

  private String socketHost;
  private int socketPort;

  public Socket() {
    createSocketClient();
  }

  /**
   * Start socket, if it hasn't been, or reconnect it in case of a host or port change.
   */
  public static synchronized void connectSocket() {
    if (instance == null) {
      instance = new Socket();
    } else {
      // Reconnect socket if host or port are changed

      String newHost = APIConfig.getInstance().getSocketHost();
      int newPort = APIConfig.getInstance().getSocketPort();

      String currentHost = instance.socketHost;
      int currentPort = instance.socketPort;

      boolean reconnect = false;
      if (!TextUtils.isEmpty(currentHost) && !currentHost.equals(newHost)) {
        reconnect = true;
      }
      if (currentPort != 0 && currentPort != newPort) {
        reconnect = true;
      }

      if (reconnect) {
        reconnectSocket();
      }
    }
  }

  private static synchronized void reconnectSocket() {
    if (instance != null) {
      if (instance.connecting) {
        // wait until connected, then disconnect
        requestNewConnection = true;
      } else {
        // already connected, no problem to disconnect
        instance.disconnect();
        instance = new Socket();
        requestNewConnection = false;
      }
    }
  }

  /**
   * For testing purposes only.
   *
   * Before disconnecting make sure connection process is done.
   */
  public static synchronized void disconnectSocket() {
    if (instance != null) {
      instance.disconnect();
      instance = null;
    }
  }

  private void updateConnectionStatus(boolean flag) {
    connecting = flag;
    if (!connecting && requestNewConnection) {
      reconnectSocket();
    }
  }

  private void createSocketClient() {
    SocketIOClient.Handler socketIOClientHandler = new SocketIOClient.Handler() {
      @Override
      public void onError(Exception error) {
        Log.e("Development socket error", error);

        // if error happens during connecting, reset flag
        updateConnectionStatus(false);
      }

      @Override
      public void onDisconnect(int code, String reason) {
        Log.d("Disconnected from development server");
        connected = false;
        updateConnectionStatus(false);
        authSent = false;
      }

      @Override
      public void onConnect() {
        if (!authSent) {
          Log.d("Connected to development server " + socketHost + ":" + socketPort);
          try {
            Map<String, String> args = Util.newMap(
                Constants.Params.APP_ID, APIConfig.getInstance().appId(),
                Constants.Params.DEVICE_ID, APIConfig.getInstance().deviceId());
            try {
              if (sio != null) {
                sio.emit("auth", new JSONArray(Collections.singletonList(new JSONObject(args))));
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          } catch (Throwable t) {
            Log.exception(t);
          }
          authSent = true;
          connected = true;
          updateConnectionStatus(false);
        }
      }

      @Override
      public void on(String event, JSONArray arguments) {
        try {
          switch (event) {
            case EVENT_UPDATE_VARS:
              Leanplum.forceContentUpdate();
              break;
            case EVENT_TRIGGER:
              handleTriggerEvent(arguments);
              break;
            case EVENT_GET_VARIABLES:
              handleGetVariablesEvent();
              break;
            case EVENT_GET_ACTIONS:
              handleGetActionsEvent();
              break;
            case EVENT_REGISTER_DEVICE:
              handleRegisterDeviceEvent(arguments);
              break;
            case EVENT_APPLY_VARS:
              handleApplyVarsEvent(arguments);
              break;
            default:
              break;
          }
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
    };

    try {
      socketHost = APIConfig.getInstance().getSocketHost();
      socketPort = APIConfig.getInstance().getSocketPort();
      URI socketUri = new URI("https://" + socketHost + ":" + socketPort);
      sio = new SocketIOClient(socketUri, socketIOClientHandler);
    } catch (URISyntaxException e) {
      Log.e(e.getMessage());
    }
    connect();
    reconnectTimer = new Timer();
    reconnectTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          reconnect();
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
    }, 0, 5000);
  }

  /**
   * Connect to the remote socket.
   */
  private void connect() {
    updateConnectionStatus(true);
    if (sio != null) {
      sio.connect();
    }
  }

  private void disconnect() {
    try {
      if (reconnectTimer != null) {
        reconnectTimer.cancel();
        reconnectTimer = null;
      }
      if (sio != null) {
        sio.disconnect();
        sio = null;
      }
    } catch (IOException e) {
      Log.e("Disconnect error", e);
    }
  }

  /**
   * Disconnect from the remote socket.
   */
  private void reconnect() {
    if (!connected && !connecting) {
      connect();
    }
  }

  /**
   * Send a given event and data to the remote socket server.
   *
   * @param eventName The name of the event.
   * @param data The data to be sent to the remote server.
   */
  public <T> void sendEvent(String eventName, Map<String, T> data) {
    try {
      Log.d("Sending event: %s with data: %s over socket", eventName, data);
      if (sio != null) {
        sio.emit(eventName,
            new JSONArray(Collections.singletonList(JsonConverter.mapToJsonObject(data))));
      }
    } catch (JSONException e) {
      Log.d("Failed to create JSON data object: " + e.getMessage());
    }
  }

  /**
   * Handles the "trigger" event received from server.
   *
   * @param arguments The arguments received from server.
   */
  void handleTriggerEvent(JSONArray arguments) {
    // Trigger a custom action.
    try {
      JSONObject payload = arguments.getJSONObject(0);
      JSONObject actionJson = payload.getJSONObject(Constants.Params.ACTION);
      if (actionJson != null) {
        String messageId = payload.getString(Constants.Params.MESSAGE_ID);
        boolean isRooted = payload.getBoolean("isRooted");
        String actionType = actionJson.getString(Constants.Values.ACTION_ARG);
        Map<String, Object> defaultDefinition = CollectionUtil.uncheckedCast(
            ActionManagerDefinitionKt.getActionDefinitionMap(
                ActionManager.getInstance(), actionType));
        Map<String, Object> defaultArgs = null;
        if (defaultDefinition != null) {
          defaultArgs = CollectionUtil.uncheckedCast(defaultDefinition.get("values"));
        }
        Map<String, Object> action = JsonConverter.mapFromJson(actionJson);
        action = CollectionUtil.uncheckedCast(VarCache.mergeHelper(defaultArgs, action));
        ActionContext context = new ActionContext(actionType, action, messageId);
        context.preventRealtimeUpdating();
        ((BaseActionContext) context).setIsRooted(isRooted);
        ((BaseActionContext) context).setIsPreview(true);
        context.update();
        ActionManagerExecutionKt.appendAction(ActionManager.getInstance(), Action.create(context));
//        ActionManager.getInstance().recordMessageImpression(context.getMessageId()); // TODO fix with new architecture
      }
    } catch (JSONException e) {
      Log.e("Error getting action info", e);
    }
  }

  /**
   * Handles the "getVariables" event received from server.
   */
  public void handleGetVariablesEvent() {
    boolean sentValues = VarCache.sendVariablesIfChanged();
    VarCache.maybeUploadNewFiles();
    sendEvent(EVENT_CONTENT_RESPONSE, Util.newMap("updated", sentValues));
  }

  /**
   * Handles the "getActions" event received from server.
   */
  void handleGetActionsEvent() {
    boolean sentValues = VarCache.sendActionsIfChanged();
    VarCache.maybeUploadNewFiles();
    sendEvent(EVENT_CONTENT_RESPONSE, Util.newMap("updated", sentValues));
  }

  /**
   * Handles the "registerDevice" event received from server.
   *
   * @param arguments The arguments received from server.
   */
  void handleRegisterDeviceEvent(JSONArray arguments) {
    LeanplumInternal.onHasStartedAndRegisteredAsDeveloper();
    String emailArg = null;
    try {
      emailArg = arguments.getJSONObject(0).getString("email");
    } catch (JSONException e) {
      Log.d("Socket - No developer e-mail provided.");
    }
    final String email = (emailArg == null) ? "a Leanplum account" : emailArg;
    showDeviceRegisteredDialog(email);
  }

  private void showDeviceRegisteredDialog(String email) {
    OperationQueue.sharedInstance().addUiOperation(new Runnable() {
      @Override
      public void run() {
        LeanplumActivityHelper.queueActionUponActive(new VariablesChangedCallback() {
          @Override
          public void variablesChanged() {
            // Stop inapp messages and dismiss any presented
            ActionManager.getInstance().setPaused(true);
            ActionManagerExecutionKt.dismissCurrentAction(ActionManager.getInstance());

            // Show alert
            Activity activity = LeanplumActivityHelper.getCurrentActivity();
            new AlertDialog.Builder(activity)
                .setTitle(TAG)
                .setMessage("Your device is registered to " + email + ".")
                .setCancelable(false)
                .setPositiveButton(
                    "OK",
                    (dialog, id) -> {
                      // Resume inapp messages
                      ActionManager.getInstance().setPaused(false);
                    })
                .show();
          }
        });
      }
    });
  }

  /**
   * Apply variables passed in from applyVars endpoint.
   */
  static void handleApplyVarsEvent(JSONArray args) {
    if (args == null) {
      return;
    }

    try {
      JSONObject object = args.getJSONObject(0);
      if (object == null) {
        return;
      }
      VarCache.applyVariableDiffs(
          JsonConverter.mapFromJson(object), null, null, null, null, null, null, null);
    } catch (JSONException e) {
      Log.e("Couldn't applyVars for preview.", e);
    } catch (Throwable e) {
      Log.exception(e);
    }
  }

  /**
   * Returns whether the socket connection is established
   *
   * @return true if connected
   */
  public boolean isConnected() {
    return connected;
  }
}
