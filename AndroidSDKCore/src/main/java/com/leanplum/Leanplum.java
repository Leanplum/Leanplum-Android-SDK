/*
 * Copyright 2021, Leanplum, Inc. All rights reserved.
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

package com.leanplum;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.leanplum.ActionContext.ContextualValues;
import com.leanplum.callbacks.ActionCallback;
import com.leanplum.callbacks.ForceContentUpdateCallback;
import com.leanplum.callbacks.MessageDisplayedCallback;
import com.leanplum.callbacks.RegisterDeviceCallback;
import com.leanplum.callbacks.RegisterDeviceFinishedCallback;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.callbacks.VariablesChangedCallback;
import com.leanplum.internal.APIConfig;
import com.leanplum.internal.ApiConfigLoader;
import com.leanplum.internal.Constants;
import com.leanplum.internal.CountAggregator;
import com.leanplum.internal.FeatureFlagManager;
import com.leanplum.internal.FileManager;
import com.leanplum.internal.FileTransferManager;
import com.leanplum.internal.JsonConverter;
import com.leanplum.internal.LeanplumEventDataManager;
import com.leanplum.internal.LeanplumInternal;
import com.leanplum.internal.LeanplumMessageMatchFilter;
import com.leanplum.internal.Log;
import com.leanplum.internal.OperationQueue;
import com.leanplum.internal.Registration;
import com.leanplum.internal.Request;
import com.leanplum.internal.Request.RequestType;
import com.leanplum.internal.RequestBuilder;
import com.leanplum.internal.RequestSender;
import com.leanplum.internal.RequestSenderTimer;
import com.leanplum.internal.RequestUtil;
import com.leanplum.internal.Util;
import com.leanplum.internal.Util.DeviceIdInfo;
import com.leanplum.internal.VarCache;
import com.leanplum.messagetemplates.MessageTemplates;
import com.leanplum.models.GeofenceEventType;
import com.leanplum.models.MessageArchiveData;
import com.leanplum.utils.BuildUtil;
import com.leanplum.utils.SharedPreferencesUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import androidx.annotation.VisibleForTesting;

/**
 * Leanplum Android SDK.
 *
 * @author Andrew First, Ben Marten
 */
public class Leanplum {
  public static final int ACTION_KIND_MESSAGE = 1;
  public static final int ACTION_KIND_ACTION = 1 << 1;

  /**
   * Default event name to use for Purchase events.
   */
  public static final String PURCHASE_EVENT_NAME = "Purchase";
  private static final String LEANPLUM_PUSH_SERVICE = "com.leanplum.LeanplumPushService";

  private static final ArrayList<StartCallback> startHandlers = new ArrayList<>();
  private static final ArrayList<VariablesChangedCallback> variablesChangedHandlers =
      new ArrayList<>();
  private static final ArrayList<VariablesChangedCallback> noDownloadsHandlers =
      new ArrayList<>();
  private static final ArrayList<VariablesChangedCallback> onceNoDownloadsHandlers =
      new ArrayList<>();
  private static final ArrayList<MessageDisplayedCallback> messageDisplayedHandlers =
          new ArrayList<>();
  private static final Object heartbeatLock = new Object();
  private static final String LEANPLUM_NOTIFICATION_CHANNEL =
      "com.leanplum.LeanplumNotificationChannel";
  private static RegisterDeviceCallback registerDeviceHandler;
  private static RegisterDeviceFinishedCallback registerDeviceFinishedHandler;
  private static LeanplumDeviceIdMode deviceIdMode = LeanplumDeviceIdMode.MD5_MAC_ADDRESS;
  private static String customDeviceId;
  private static String customAppVersion = null;
  private static String customLocale = null;
  private static boolean userSpecifiedDeviceId;
  private static boolean locationCollectionEnabled = true;
  private static volatile boolean pushDeliveryTrackingEnabled = true;
  private static Context context;

  private static CountAggregator countAggregator = new CountAggregator();
  private static FeatureFlagManager featureFlagManager = FeatureFlagManager.INSTANCE;

  private Leanplum() {
  }

  /**
   * Optional. Sets the API server. The API path is of the form http[s]://hostName/apiPath
   *
   * @param hostName The name of the API host, such as www.leanplum.com
   * @param apiPath The name of the API servlet, such as api
   * @param ssl Whether to use SSL
   */
  public static void setApiConnectionSettings(String hostName, String apiPath, boolean ssl) {
    if (TextUtils.isEmpty(hostName)) {
      Log.i("setApiConnectionSettings - Empty hostName parameter provided.");
      return;
    }
    if (TextUtils.isEmpty(apiPath)) {
      Log.i("setApiConnectionSettings - Empty apiPath parameter provided.");
      return;
    }

    APIConfig.getInstance().setApiConfig(hostName, apiPath, ssl);
  }

  /**
   * Optional. Sets the socket server path for Development mode. Path is of the form hostName:port
   *
   * @param hostName The host name of the socket server.
   * @param port The port to connect to.
   */
  public static void setSocketConnectionSettings(String hostName, int port) {
    if (TextUtils.isEmpty(hostName)) {
      Log.i("setSocketConnectionSettings - Empty hostName parameter provided.");
      return;
    }
    if (port < 1 || port > 65535) {
      Log.i("setSocketConnectionSettings - Invalid port parameter provided.");
      return;
    }

    String currentSocketHost = APIConfig.getInstance().getSocketHost();
    if (!hostName.equals(currentSocketHost)) {
      APIConfig.getInstance().setSocketConfig(hostName, port);
      LeanplumInternal.connectDevelopmentServer();
    }
  }

  /**
   * Optional. Whether to enable file uploading in development mode.
   *
   * @param enabled Whether or not files should be uploaded. (Default: true)
   */
  public static void setFileUploadingEnabledInDevelopmentMode(boolean enabled) {
    Constants.enableFileUploadingInDevelopmentMode = enabled;
  }

  /**
   * Please use setLogLevel to enable logging.
   */
  @Deprecated
  public static void enableVerboseLoggingInDevelopmentMode() {
    setLogLevel(Log.Level.DEBUG);
  }

  /**
   * Sets log level to one of the following
   * <ul>
   *   <li>{@link Log.Level#OFF} - disables logging.</li>
   *   <li>{@link Log.Level#ERROR} - logs only SDK errors to console.</li>
   *   <li>{@link Log.Level#INFO} - logs general informational messages including all errors,
   *   enabled by default.</li>
   *   <li>{@link Log.Level#DEBUG} - logs SDK debug messages, including info and errors.</li>
   * </ul>
   *
   * @param level level to set
   */
  public static void setLogLevel(int level) {
    Log.setLogLevel(level);
  }

  /**
   * Optional. Adjusts the network timeouts. The default timeout is 10 seconds for requests, and 15
   * seconds for file downloads.
   */
  public static void setNetworkTimeout(int seconds, int downloadSeconds) {
    if (seconds < 0) {
      Log.i("setNetworkTimeout - Invalid seconds parameter provided.");
      return;
    }
    if (downloadSeconds < 0) {
      Log.i("setNetworkTimeout - Invalid downloadSeconds parameter provided.");
      return;
    }

    Constants.NETWORK_TIMEOUT_SECONDS = seconds;
    Constants.NETWORK_TIMEOUT_SECONDS_FOR_DOWNLOADS = downloadSeconds;
  }

  /**
   * Must call either this or {@link Leanplum#setAppIdForProductionMode} before issuing any calls to
   * the API, including start.
   *
   * @param appId Your app ID.
   * @param accessKey Your development key.
   */
  public static void setAppIdForDevelopmentMode(String appId, String accessKey) {
    if (TextUtils.isEmpty(appId)) {
      Log.e("setAppIdForDevelopmentMode - Empty appId parameter provided.");
      return;
    }
    if (TextUtils.isEmpty(accessKey)) {
      Log.e("setAppIdForDevelopmentMode - Empty accessKey parameter provided.");
      return;
    }

    Constants.isDevelopmentModeEnabled = true;
    APIConfig.getInstance().setAppId(appId, accessKey);
  }

  /**
   * Must call either this or {@link Leanplum#setAppIdForDevelopmentMode} before issuing any calls
   * to the API, including start.
   *
   * @param appId Your app ID.
   * @param accessKey Your production key.
   */
  public static void setAppIdForProductionMode(String appId, String accessKey) {
    if (TextUtils.isEmpty(appId)) {
      Log.e("setAppIdForProductionMode - Empty appId parameter provided.");
      return;
    }
    if (TextUtils.isEmpty(accessKey)) {
      Log.e("setAppIdForProductionMode - Empty accessKey parameter provided.");
      return;
    }

    Constants.isDevelopmentModeEnabled = false;
    APIConfig.getInstance().setAppId(appId, accessKey);
  }

  /**
   * Loads appId and accessKey from Android resources.
   */
  private static void loadApiConfigFromResources() {
    ApiConfigLoader loader = new ApiConfigLoader(getContext());
    loader.loadFromResources(
        Leanplum::setAppIdForProductionMode,
        Leanplum::setAppIdForDevelopmentMode);
  }

  /**
   * Enable screen tracking.
   */
  public static void trackAllAppScreens() {
    LeanplumInternal.enableAutomaticScreenTracking();
  }

  /**
   * Set this to true if you want details about the variable assignments
   * on the server.
   * Default is NO.
   */
  public static void setVariantDebugInfoEnabled(boolean variantDebugInfoEnabled) {
    LeanplumInternal.setIsVariantDebugInfoEnabled(variantDebugInfoEnabled);
  }

  /**
   * Whether screen tracking is enabled or not.
   *
   * @return Boolean - true if enabled
   */
  public static boolean isScreenTrackingEnabled() {
    return LeanplumInternal.getIsScreenTrackingEnabled();
  }

  /**
   * By default, Leanplum reports the version of your app using
   * getPackageManager().getPackageInfo, which can be used for reporting and targeting
   * on the Leanplum dashboard. If you wish to use any other string as the version,
   * you can call this before your call to Leanplum.start()
   */
  public static void setAppVersion(String appVersion) {
    customAppVersion = appVersion;
  }

  /**
   * Sets the type of device ID to use. Default: {@link LeanplumDeviceIdMode#MD5_MAC_ADDRESS}
   */
  public static void setDeviceIdMode(LeanplumDeviceIdMode mode) {
    if (mode == null) {
      Log.i("setDeviceIdMode - Invalid mode parameter provided.");
      return;
    }

    deviceIdMode = mode;
    userSpecifiedDeviceId = true;
  }

  /**
   * (Advanced) Sets a custom device ID. Normally, you should use setDeviceIdMode to change the type
   * of device ID provided.
   */
  public static void setDeviceId(String deviceId) {
    if (TextUtils.isEmpty(deviceId)) {
      Log.i("setDeviceId - Empty deviceId parameter provided.");
    }

    customDeviceId = deviceId;
    userSpecifiedDeviceId = true;
  }

  /**
   * (Advanced) Sets new device ID. Must be called after Leanplum finished starting.
   * This method allows multiple changes of device ID in opposite of
   * {@link Leanplum#setDeviceId(String)}, which allows only one.
   */
  public static void forceNewDeviceId(String deviceId) {
    if (TextUtils.isEmpty(deviceId)) {
      Log.i("forceNewDeviceId - Empty deviceId parameter provided.");
      return;
    }

    if (deviceId.equals(APIConfig.getInstance().deviceId())) {
      // same device ID, nothing to change
      return;
    }

    if (hasStarted()) {
      APIConfig.getInstance().setDeviceId(deviceId);
      APIConfig.getInstance().save();
      VarCache.saveDiffs(); // device ID is saved there

      Map<String, Object> params = new HashMap<>();
      attachDeviceParams(params);

      Request request = RequestBuilder
          .withSetDeviceAttributesAction()
          .andParams(params)
          .andType(RequestType.IMMEDIATE)
          .create();

      RequestSender.getInstance().send(request);
    }
  }

  /**
   * Sets a custom locale. You should call this before {@link Leanplum#start}.
   */
  public static void setLocale(String locale) {
    if (TextUtils.isEmpty(locale)) {
      Log.i("setLocale - Empty locale parameter provided.");
    }

    customLocale = locale;
  }

  /**
   * Gets the deviceId in the current Leanplum session. This should only be called after
   * {@link Leanplum#start}.
   *
   * @return String Returns the deviceId in the current Leanplum session.
   */
  public static String getDeviceId() {
    if (!LeanplumInternal.hasCalledStart()) {
      Log.i("Leanplum.start() must be called before calling getDeviceId.");
      return null;
    }
    return APIConfig.getInstance().deviceId();
  }

  /**
   * Sets the application context. This should be the first call to Leanplum.
   */
  public static void setApplicationContext(Context context) {
    if (context == null) {
      Log.i("setApplicationContext - Null context parameter provided.");
    }

    Leanplum.context = context;
  }

  /**
   * Gets the application context.
   */
  public static Context getContext() {
    if (context == null) {
      Log.e("Your application context is not set. "
          + "You should call Leanplum.setApplicationContext(this) or "
          + "LeanplumActivityHelper.enableLifecycleCallbacks(this) in your application's "
          + "onCreate method, or have your application extend LeanplumApplication.");
    }
    return context;
  }

  /**
   * Syncs resources between Leanplum and the current app. You should only call this once, and
   * before {@link Leanplum#start}. syncResourcesAsync should be used instead unless file variables
   * need to be defined early
   */
  public static void syncResources() {
    if (Constants.isNoop()) {
      return;
    }
    try {
      FileManager.enableResourceSyncing(null, null, false);
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  /**
   * Syncs resources between Leanplum and the current app. You should only call this once, and
   * before {@link Leanplum#start}.
   */
  public static void syncResourcesAsync() {
    if (Constants.isNoop()) {
      return;
    }
    try {
      FileManager.enableResourceSyncing(null, null, true);
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  /**
   * Syncs resources between Leanplum and the current app. You should only call this once, and
   * before {@link Leanplum#start}. syncResourcesAsync should be used instead unless file variables
   * need to be defined early
   *
   * @param patternsToInclude Limit paths to only those matching at least one pattern in this list.
   * Supply null to indicate no inclusion patterns. Paths start with the folder name within the res
   * folder, e.g. "layout/main.xml".
   * @param patternsToExclude Exclude paths matching at least one of these patterns. Supply null to
   * indicate no exclusion patterns.
   */
  public static void syncResources(
      List<String> patternsToInclude,
      List<String> patternsToExclude) {
    try {
      FileManager.enableResourceSyncing(patternsToInclude, patternsToExclude, false);
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  /**
   * Syncs resources between Leanplum and the current app. You should only call this once, and
   * before {@link Leanplum#start}. syncResourcesAsync should be used instead unless file variables
   * need to be defined early
   *
   * @param patternsToInclude Limit paths to only those matching at least one pattern in this list.
   * Supply null to indicate no inclusion patterns. Paths start with the folder name within the res
   * folder, e.g. "layout/main.xml".
   * @param patternsToExclude Exclude paths matching at least one of these patterns. Supply null to
   * indicate no exclusion patterns.
   */
  public static void syncResourcesAsync(
      List<String> patternsToInclude,
      List<String> patternsToExclude) {
    try {
      FileManager.enableResourceSyncing(patternsToInclude, patternsToExclude, true);
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  /**
   * Returns true if resource syncing is enabled. Resource syncing may not be fully initialized.
   */
  public static boolean isResourceSyncingEnabled() {
    return FileManager.isResourceSyncingEnabled();
  }

  /**
   * Call this when your application starts. This will initiate a call to Leanplum's servers to get
   * the values of the variables used in your app.
   */
  public static void start(Context context) {
    start(context, null, null, null, null);
  }

  /**
   * Call this when your application starts. This will initiate a call to Leanplum's servers to get
   * the values of the variables used in your app.
   */
  public static void start(Context context, StartCallback callback) {
    start(context, null, null, callback, null);
  }

  /**
   * Call this when your application starts. This will initiate a call to Leanplum's servers to get
   * the values of the variables used in your app.
   */
  public static void start(Context context, Map<String, ?> userAttributes) {
    start(context, null, userAttributes, null, null);
  }

  /**
   * Call this when your application starts. This will initiate a call to Leanplum's servers to get
   * the values of the variables used in your app.
   */
  public static void start(Context context, String userId) {
    start(context, userId, null, null, null);
  }

  /**
   * Call this when your application starts. This will initiate a call to Leanplum's servers to get
   * the values of the variables used in your app.
   */
  public static void start(Context context, String userId, StartCallback callback) {
    start(context, userId, null, callback, null);
  }

  /**
   * Call this when your application starts. This will initiate a call to Leanplum's servers to get
   * the values of the variables used in your app.
   */
  public static void start(Context context, String userId, Map<String, ?> userAttributes) {
    start(context, userId, userAttributes, null, null);
  }

  /**
   * Call this when your application starts. This will initiate a call to Leanplum's servers to get
   * the values of the variables used in your app.
   */
  public static synchronized void start(final Context context, String userId,
      Map<String, ?> attributes, StartCallback response) {
    start(context, userId, attributes, response, null);
  }

  static synchronized void start(final Context context, final String userId,
      final Map<String, ?> attributes, StartCallback response, final Boolean isBackground) {
    try {
      boolean appIdNotSet = TextUtils.isEmpty(APIConfig.getInstance().appId());
      if (appIdNotSet) {
        loadApiConfigFromResources();
      }

      LeanplumActivityHelper.setCurrentActivity(context);

      // Detect if app is in background automatically if isBackground is not set.
      final boolean actuallyInBackground;
      if (isBackground == null) {
        actuallyInBackground = LeanplumActivityHelper.getCurrentActivity() == null ||
            LeanplumActivityHelper.isActivityPaused();
      } else {
        actuallyInBackground = isBackground;
      }

      if (Constants.isNoop()) {
        LeanplumInternal.setHasStarted(true);
        LeanplumInternal.setStartSuccessful(true);
        triggerStartResponse(true);
        triggerVariablesChanged();
        triggerVariablesChangedAndNoDownloadsPending();
        VarCache.applyVariableDiffs(
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new HashMap<>(),
            "",
            "");
        LeanplumInbox.getInstance().update(new HashMap<>(), 0, false);
        return;
      }

      if (response != null) {
        addStartResponseHandler(response);
      }

      if (context != null) {
        Leanplum.setApplicationContext(context.getApplicationContext());
      }

      if (LeanplumInternal.hasCalledStart()) {
        if (!actuallyInBackground && LeanplumInternal.hasStartedInBackground()) {
          // Move to foreground.
          LeanplumInternal.setStartedInBackground(false);
          LeanplumInternal.moveToForeground();
        }
        return;
      }

      MessageTemplates.register(Leanplum.getContext());

      LeanplumInternal.setStartedInBackground(actuallyInBackground);

      final Map<String, ?> validAttributes = LeanplumInternal.validateAttributes(attributes,
          "userAttributes", true);
      LeanplumInternal.setCalledStart(true);

      if (validAttributes != null) {
        LeanplumInternal.getUserAttributeChanges().add(validAttributes);
      }

      APIConfig.getInstance(); // load prefs
      VarCache.setSilent(true);
      VarCache.loadDiffs();
      VarCache.setSilent(false);
      LeanplumInbox.getInstance().load();

      // Setup class members.
      VarCache.onUpdate(new CacheUpdateBlock() {
        @Override
        public void updateCache() {
          triggerVariablesChanged();
          if (FileTransferManager.getInstance().numPendingDownloads() == 0) {
            triggerVariablesChangedAndNoDownloadsPending();
          }
        }
      });
      FileTransferManager.getInstance().onNoPendingDownloads(
          new FileTransferManager.NoPendingDownloadsCallback() {
            @Override
            public void noPendingDownloads() {
              triggerVariablesChangedAndNoDownloadsPending();
            }
      });

      // Reduce latency by running the rest of the start call in a background thread.
      OperationQueue.sharedInstance().addParallelOperation(new Runnable() {
        @Override
        public void run() {
          try {
            startHelper(userId, validAttributes, actuallyInBackground);
          } catch (Throwable t) {
            Log.exception(t);
          }
        }
      });
      Util.initExceptionHandling(context);
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  /**
   * Checks for leanplum notifications modules and if someone present - invoke onStart method.
   */
  private static void checkAndStartNotificationsModules() {
    try {
      Log.d("Trying to start LeanplumPushService");

      Class.forName(LEANPLUM_PUSH_SERVICE)
          .getDeclaredMethod("onStart")
          .invoke(null);
    } catch (Throwable ignored) {
      // ignored
    }
  }

  private static void attachDeviceParams(@NonNull Map<String, Object> params) {
    String versionName = Util.getVersionName();
    if (customAppVersion != null) {
      versionName = customAppVersion;
    }
    if (versionName == null) {
      versionName = "";
    }

    String fcmRegistrationId = SharedPreferencesUtil.getString(context,
        Constants.Defaults.LEANPLUM_PUSH, Constants.Defaults.PROPERTY_FCM_TOKEN_ID);
    String miPushRegistrationId = SharedPreferencesUtil.getString(context,
        Constants.Defaults.LEANPLUM_PUSH, Constants.Defaults.PROPERTY_MIPUSH_TOKEN_ID);
    String hmsRegistrationId = SharedPreferencesUtil.getString(context,
        Constants.Defaults.LEANPLUM_PUSH, Constants.Defaults.PROPERTY_HMS_TOKEN_ID);

    params.put(Constants.Params.VERSION_NAME, versionName);
    params.put(Constants.Params.DEVICE_NAME, Util.getDeviceName());
    params.put(Constants.Params.DEVICE_MODEL, Util.getDeviceModel());
    params.put(Constants.Params.DEVICE_SYSTEM_NAME, Util.getSystemName());
    params.put(Constants.Params.DEVICE_SYSTEM_VERSION, Util.getSystemVersion());
    if (!TextUtils.isEmpty(fcmRegistrationId)) {
      params.put(Constants.Params.DEVICE_FCM_PUSH_TOKEN, fcmRegistrationId);
    }
    if (!TextUtils.isEmpty(miPushRegistrationId)) {
      params.put(Constants.Params.DEVICE_MIPUSH_TOKEN, miPushRegistrationId);
    }
    if (!TextUtils.isEmpty(hmsRegistrationId)) {
      params.put(Constants.Params.DEVICE_HMS_TOKEN, hmsRegistrationId);
    }
  }

  private static void startHelper(
      String userId, final Map<String, ?> attributes, final boolean isBackground) {
    LeanplumEventDataManager.sharedInstance();
    checkAndStartNotificationsModules();
    Boolean limitAdTracking = null;
    String deviceId = APIConfig.getInstance().deviceId();
    if (deviceId == null) {
      if (!userSpecifiedDeviceId && Constants.defaultDeviceId != null) {
        Log.d("Using default deviceID");
        deviceId = Constants.defaultDeviceId;
      } else if (customDeviceId != null) {
        Log.d("Using custom deviceID");
        deviceId = customDeviceId;
      } else {
        Log.d("Using deviceID for mode: %s", deviceIdMode);
        DeviceIdInfo deviceIdInfo = Util.getDeviceId(deviceIdMode);
        deviceId = deviceIdInfo.id;
        limitAdTracking = deviceIdInfo.limitAdTracking;
      }
      APIConfig.getInstance().setDeviceId(deviceId);
    }

    if (userId == null) {
      userId = APIConfig.getInstance().userId();
      if (userId == null) {
        Log.d("setting deviceID as userID");
        userId = APIConfig.getInstance().deviceId();
      }
    }
    APIConfig.getInstance().setUserId(userId);

    String locale = Util.getLocale();
    if (!TextUtils.isEmpty(customLocale)) {
      locale = customLocale;
    }

    TimeZone localTimeZone = TimeZone.getDefault();
    Date now = new Date();
    int timezoneOffsetSeconds = localTimeZone.getOffset(now.getTime()) / 1000;

    HashMap<String, Object> params = new HashMap<>();

    attachDeviceParams(params);

    params.put(Constants.Params.INCLUDE_DEFAULTS, Boolean.toString(false));
    if (isBackground) {
      params.put(Constants.Params.BACKGROUND, Boolean.toString(true));
    }
    params.put(Constants.Keys.TIMEZONE, localTimeZone.getID());
    params.put(Constants.Keys.TIMEZONE_OFFSET_SECONDS, Integer.toString(timezoneOffsetSeconds));
    params.put(Constants.Keys.LOCALE, locale);
    params.put(Constants.Keys.COUNTRY, Constants.Values.DETECT);
    params.put(Constants.Keys.REGION, Constants.Values.DETECT);
    params.put(Constants.Keys.CITY, Constants.Values.DETECT);
    params.put(Constants.Keys.LOCATION, Constants.Values.DETECT);
    if (Boolean.TRUE.equals(limitAdTracking)) {
      params.put(Constants.Params.LIMIT_TRACKING, limitAdTracking.toString());
    }
    if (attributes != null) {
      params.put(Constants.Params.USER_ATTRIBUTES, JsonConverter.toJson(attributes));
    }
    if (Constants.isDevelopmentModeEnabled) {
      params.put(Constants.Params.DEV_MODE, Boolean.TRUE.toString());
    }

    // Get the current inbox messages on the device.
    params.put(Constants.Params.INBOX_MESSAGES, LeanplumInbox.getInstance().messagesIds());

    params.put(Constants.Params.INCLUDE_VARIANT_DEBUG_INFO, LeanplumInternal.getIsVariantDebugInfoEnabled());

    Util.initializePreLeanplumInstall(params);

    // Issue start API call.
    RequestType requestType = isBackground ? RequestType.DEFAULT : RequestType.IMMEDIATE;
    Request request = RequestBuilder
        .withStartAction()
        .andParams(params)
        .andType(requestType)
        .create();
    request.onResponse(new Request.ResponseCallback() {
      @Override
      public void response(JSONObject response) {
        Log.d("Received start response: %s", response);
        handleStartResponse(response);
      }
    });
    request.onError(new Request.ErrorCallback() {
      @Override
      public void error(Exception e) {
        Log.e("Failed to receive start response", e);
        handleStartResponse(null);
      }
    });
    RequestSender.getInstance().send(request);

    LeanplumInternal.triggerStartIssued();
  }

  private static void handleStartResponse(final JSONObject response) {
    boolean success = RequestUtil.isResponseSuccess(response);
    if (!success) {
      try {
        LeanplumInternal.setHasStarted(true);
        LeanplumInternal.setStartSuccessful(false);

        // Load the variables that were stored on the device from the last session.
        VarCache.loadDiffs();

      } catch (Throwable t) {
        Log.exception(t);
      } finally {
        triggerStartResponse(success);
      }
    } else {
      try {
        LeanplumInternal.setHasStarted(true);
        LeanplumInternal.setStartSuccessful(true);

        JSONObject values = response.optJSONObject(Constants.Keys.VARS);
        if (values == null) {
          Log.e("No variable values were received from the server. " +
              "Please contact us to investigate.");
        }

        JSONObject messages = response.optJSONObject(Constants.Keys.MESSAGES);
        if (messages == null) {
          Log.d("No messages received from the server.");
        }

        JSONObject regions = response.optJSONObject(Constants.Keys.REGIONS);
        if (regions == null) {
          Log.d("No regions received from the server.");
        }

        JSONArray variants = response.optJSONArray(Constants.Keys.VARIANTS);
        if (variants == null) {
          Log.d("No variants received from the server.");
        }
        Map<String, String> filenameToURLs = parseFilenameToURLs(response);
        FileManager.setFilenameToURLs(filenameToURLs);

        if (BuildUtil.isNotificationChannelSupported(context)) {
          // Get notification channels and groups.
          JSONArray notificationChannels = response.optJSONArray(
              Constants.Keys.NOTIFICATION_CHANNELS);
          JSONArray notificationGroups = response.optJSONArray(
              Constants.Keys.NOTIFICATION_GROUPS);
          String defaultNotificationChannel = response.optString(
              Constants.Keys.DEFAULT_NOTIFICATION_CHANNEL);
          try {
            Class.forName(LEANPLUM_NOTIFICATION_CHANNEL)
                .getDeclaredMethod("configureChannels", Context.class, JSONArray.class,
                    JSONArray.class, String.class).invoke(new Object(), context,
                notificationGroups, notificationChannels, defaultNotificationChannel);
          } catch (Throwable ignored) {
          }
        }

        String token = response.optString(Constants.Keys.TOKEN, null);
        APIConfig.getInstance().setToken(token);

        applyContentInResponse(response);

        VarCache.saveUserAttributes();

        if (response.optBoolean(Constants.Keys.SYNC_INBOX, false)) {
          LeanplumInbox.getInstance().downloadMessages();
        }

        if (response.optBoolean(Constants.Keys.LOGGING_ENABLED, false)) {
          Constants.loggingEnabled = true;
        }

        Set<String> enabledCounters = parseSdkCounters(response);
        countAggregator.setEnabledCounters(enabledCounters);
        Set<String> enabledFeatureFlags = parseFeatureFlags(response);
        FeatureFlagManager.INSTANCE.setEnabledFeatureFlags((enabledFeatureFlags));
        parseVariantDebugInfo(response);

        // Allow bidirectional realtime variable updates.
        if (Constants.isDevelopmentModeEnabled) {

          final Context currentContext = (
              LeanplumActivityHelper.getCurrentActivity() != context &&
                  LeanplumActivityHelper.getCurrentActivity() != null) ?
              LeanplumActivityHelper.getCurrentActivity()
              : context;

          // Register device.
          if (!response.optBoolean(
              Constants.Keys.IS_REGISTERED) && registerDeviceHandler != null) {
            registerDeviceHandler.setResponseHandler(new RegisterDeviceCallback.EmailCallback() {
              @Override
              public void onResponse(String email) {
                try {
                  if (email != null) {
                    Registration.registerDevice(email, new StartCallback() {
                      @Override
                      public void onResponse(boolean success) {
                        if (registerDeviceFinishedHandler != null) {
                          registerDeviceFinishedHandler.setSuccess(success);
                          OperationQueue.sharedInstance().addUiOperation(registerDeviceFinishedHandler);
                        }
                        if (success) {
                          try {
                            LeanplumInternal.onHasStartedAndRegisteredAsDeveloper();
                          } catch (Throwable t) {
                            Log.exception(t);
                          }
                        }
                      }
                    });
                  }
                } catch (Throwable t) {
                  Log.exception(t);
                }
              }
            });
            OperationQueue.sharedInstance().addUiOperation(registerDeviceHandler);
          }

          // Show device is already registered.
          if (response.optBoolean(Constants.Keys.IS_REGISTERED_FROM_OTHER_APP)) {
            OperationQueue.sharedInstance().addUiOperation(new Runnable() {
              @Override
              public void run() {
                try {
                  Class.forName(Leanplum.LEANPLUM_PUSH_SERVICE)
                      .getDeclaredMethod("showDeviceRegistedPush", Context.class,
                          Context.class).invoke(new Object(), context, currentContext);
                } catch (Throwable ignored) {
                }
              }
            });
          }

          boolean isRegistered = response.optBoolean(Constants.Keys.IS_REGISTERED);

          // Check for SDK updates.
          String latestVersion = response.optString(Constants.Keys.LATEST_VERSION);
          if (isRegistered && !TextUtils.isEmpty(latestVersion)) {
            Log.i("Version %s of Leanplum SDK is available. " +
                "Update your gradle dependencies to use it.", latestVersion);
          }

          JSONObject valuesFromCode = response.optJSONObject(Constants.Keys.VARS_FROM_CODE);
          if (valuesFromCode == null) {
            valuesFromCode = new JSONObject();
          }

          JSONObject actionDefinitions =
              response.optJSONObject(Constants.Params.ACTION_DEFINITIONS);
          if (actionDefinitions == null) {
            actionDefinitions = new JSONObject();
          }

          JSONObject fileAttributes = response.optJSONObject(Constants.Params.FILE_ATTRIBUTES);
          if (fileAttributes == null) {
            fileAttributes = new JSONObject();
          }

          VarCache.setDevModeValuesFromServer(
              JsonConverter.mapFromJson(valuesFromCode),
              JsonConverter.mapFromJson(fileAttributes),
              JsonConverter.mapFromJson(actionDefinitions));

          if (isRegistered) {
            LeanplumInternal.onHasStartedAndRegisteredAsDeveloper();
          }
        }
        LeanplumInternal.moveToForeground();
        startRequestTimer();
      } catch (Throwable t) {
        Log.exception(t);
      } finally {
        triggerStartResponse(success);
      }
    }
  }

  /**
   * Applies the variables, messages, or update rules in a start or getVars response.
   *
   * @param response The response containing content.
   */
  private static void applyContentInResponse(JSONObject response) {
    Map<String, Object> values = JsonConverter.mapFromJson(
        response.optJSONObject(Constants.Keys.VARS));
    Map<String, Object> messages = JsonConverter.mapFromJsonOrDefault(
        response.optJSONObject(Constants.Keys.MESSAGES));
    Map<String, Object> regions = JsonConverter.mapFromJsonOrDefault(
        response.optJSONObject(Constants.Keys.REGIONS));
    List<Map<String, Object>> variants = JsonConverter.listFromJsonOrDefault(
        response.optJSONArray(Constants.Keys.VARIANTS));
    List<Map<String, Object>> localCaps = JsonConverter.listFromJsonOrDefault(
        response.optJSONArray(Constants.Keys.LOCAL_CAPS));
    Map<String, Object> variantDebugInfo = JsonConverter.mapFromJsonOrDefault(
            response.optJSONObject(Constants.Keys.VARIANT_DEBUG_INFO));
    JSONObject varsJsonObj = response.optJSONObject(Constants.Keys.VARS);
    String varsJson = (varsJsonObj != null) ? varsJsonObj.toString() : null;
    String varsSignature = response.optString(Constants.Keys.VARS_SIGNATURE);

    VarCache.applyVariableDiffs(
        values,
        messages,
        regions,
        variants,
        localCaps,
        variantDebugInfo,
        varsJson,
        varsSignature);
  }

  /**
   * Used by wrapper SDKs like Unity to override the SDK client name and version.
   */
  static void setClient(String client, String sdkVersion, String defaultDeviceId) {
    Constants.CLIENT = client;
    Constants.LEANPLUM_VERSION = sdkVersion;
    Constants.defaultDeviceId = defaultDeviceId;
  }

  /**
   * Call this when your activity pauses. This is called from LeanplumActivityHelper.
   */
  static void pause() {
    if (Constants.isNoop()) {
      return;
    }
    if (!LeanplumInternal.hasCalledStart()) {
      Log.e("You cannot call pause before calling start");
      return;
    }

    if (LeanplumInternal.issuedStart()) {
      pauseInternal();
    } else {
      LeanplumInternal.addStartIssuedHandler(new Runnable() {
        @Override
        public void run() {
          try {
            pauseInternal();
          } catch (Throwable t) {
            Log.exception(t);
          }
        }
      });
    }
  }

  private static void pauseInternal() {
    Request request = RequestBuilder
        .withPauseSessionAction()
        .andType(RequestType.IMMEDIATE)
        .create();
    RequestSender.getInstance().send(request);
    stopRequestTimer();
    LeanplumInternal.setIsPaused(true);
  }

  /**
   * Call this when your activity resumes. This is called from LeanplumActivityHelper.
   */
  static void resume() {
    if (Constants.isNoop()) {
      return;
    }
    if (!LeanplumInternal.hasCalledStart()) {
      Log.e("You cannot call resume before calling start");
      return;
    }

    if (LeanplumInternal.issuedStart()) {
      resumeInternal();
    } else {
      LeanplumInternal.addStartIssuedHandler(new Runnable() {
        @Override
        public void run() {
          try {
            resumeInternal();
          } catch (Throwable t) {
            Log.exception(t);
          }
        }
      });
    }
  }

  private static void resumeInternal() {
    Request request = RequestBuilder
        .withResumeSessionAction()
        .andType(RequestType.IMMEDIATE)
        .create();
    RequestSender.getInstance().send(request);

    if (LeanplumInternal.hasStartedInBackground()) {
      LeanplumInternal.setStartedInBackground(false);
    } else {
      LeanplumInternal.maybePerformActions("resume", null,
          LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL, null, null);
    }
    startRequestTimer();
    LeanplumInternal.setIsPaused(false);
  }

  private static void startRequestTimer() {
    RequestSenderTimer.get().start();
  }

  private static void stopRequestTimer() {
    RequestSenderTimer.get().stop();
  }

  /**
   * Call this to explicitly end the session. This should not be used in most cases, so we won't
   * make it public for now.
   */
  static void stop() {
    if (Constants.isNoop()) {
      return;
    }
    if (!LeanplumInternal.hasCalledStart()) {
      Log.e("You cannot call stop before calling start");
      return;
    }

    if (LeanplumInternal.issuedStart()) {
      stopInternal();
    } else {
      LeanplumInternal.addStartIssuedHandler(new Runnable() {
        @Override
        public void run() {
          try {
            stopInternal();
          } catch (Throwable t) {
            Log.exception(t);
          }
        }
      });
    }
  }

  private static void stopInternal() {
    Request request = RequestBuilder.withStopAction().andType(RequestType.IMMEDIATE).create();
    RequestSender.getInstance().send(request);
  }

  /**
   * Whether or not Leanplum has finished starting.
   */
  public static boolean hasStarted() {
    return LeanplumInternal.hasStarted();
  }

  /**
   * Returns the userId in the current Leanplum session. This should only be called after
   * Leanplum.start().
   */
  public static String getUserId() {
    if (hasStarted()) {
      return APIConfig.getInstance().userId();
    } else {
      Log.e("Leanplum.start() must be called before calling getUserId()");
    }
    return null;
  }

  /**
   * Returns an instance to the singleton LeanplumInbox object.
   */
  public static LeanplumInbox getInbox() {
    return LeanplumInbox.getInstance();
  }

  /**
   * Whether or not Leanplum has finished starting and the device is registered as a developer.
   */
  public static boolean hasStartedAndRegisteredAsDeveloper() {
    return LeanplumInternal.hasStartedAndRegisteredAsDeveloper();
  }

  /**
   * Add a callback for when the start call finishes, and variables are returned back from the
   * server.
   */
  public static void addStartResponseHandler(StartCallback handler) {
    if (handler == null) {
      Log.e("addStartResponseHandler - Invalid handler parameter provided.");
      return;
    }

    if (LeanplumInternal.hasStarted()) {
      if (LeanplumInternal.isStartSuccessful()) {
        handler.setSuccess(true);
      }
      handler.run();
    } else {
      synchronized (startHandlers) {
        if (startHandlers.indexOf(handler) == -1) {
          startHandlers.add(handler);
        }
      }
    }
  }

  /**
   * Removes a start response callback.
   */
  public static void removeStartResponseHandler(StartCallback handler) {
    if (handler == null) {
      Log.e("removeStartResponseHandler - Invalid handler parameter provided.");
      return;
    }

    synchronized (startHandlers) {
      startHandlers.remove(handler);
    }
  }

  private static void triggerStartResponse(boolean success) {
    synchronized (startHandlers) {
      for (StartCallback callback : startHandlers) {
        callback.setSuccess(success);
        OperationQueue.sharedInstance().addUiOperation(callback);
      }
      startHandlers.clear();
    }
  }

  /**
   * Add a callback for when the variables receive new values from the server. This will be called
   * on start, and also later on if the user is in an experiment that can updated in realtime.
   */
  public static void addVariablesChangedHandler(VariablesChangedCallback handler) {
    if (handler == null) {
      Log.e("addVariablesChangedHandler - Invalid handler parameter provided.");
      return;
    }

    synchronized (variablesChangedHandlers) {
      variablesChangedHandlers.add(handler);
    }
    if (VarCache.hasReceivedDiffs()) {
      handler.variablesChanged();
    }
  }

  /**
   * Removes a variables changed callback.
   */
  public static void removeVariablesChangedHandler(VariablesChangedCallback handler) {
    if (handler == null) {
      Log.e("removeVariablesChangedHandler - Invalid handler parameter provided.");
      return;
    }

    synchronized (variablesChangedHandlers) {
      variablesChangedHandlers.remove(handler);
    }
  }

  private static void triggerVariablesChanged() {
    synchronized (variablesChangedHandlers) {
      for (VariablesChangedCallback callback : variablesChangedHandlers) {
        OperationQueue.sharedInstance().addUiOperation(callback);
      }
    }
  }

  /**
   * Add a callback for when no more file downloads are pending (either when no files needed to be
   * downloaded or all downloads have been completed).
   */
  public static void addVariablesChangedAndNoDownloadsPendingHandler(
      VariablesChangedCallback handler) {
    if (handler == null) {
      Log.e("addVariablesChangedAndNoDownloadsPendingHandler - Invalid handler parameter " +
          "provided.");
      return;
    }

    synchronized (noDownloadsHandlers) {
      noDownloadsHandlers.add(handler);
    }
    if (VarCache.hasReceivedDiffs()
        && FileTransferManager.getInstance().numPendingDownloads() == 0) {
      handler.variablesChanged();
    }
  }

  /**
   * Removes a variables changed and no downloads pending callback.
   */
  public static void removeVariablesChangedAndNoDownloadsPendingHandler(
      VariablesChangedCallback handler) {
    if (handler == null) {
      Log.e("removeVariablesChangedAndNoDownloadsPendingHandler - Invalid handler parameter " +
          "provided.");
      return;
    }

    synchronized (noDownloadsHandlers) {
      noDownloadsHandlers.remove(handler);
    }
  }

  /**
   * Add a callback for when a message is displayed.
   */
  public static void addMessageDisplayedHandler(
          MessageDisplayedCallback handler) {
    if (handler == null) {
      Log.e("addMessageDisplayedHandler - Invalid handler parameter " +
              "provided.");
      return;
    }

    synchronized (messageDisplayedHandlers) {
      messageDisplayedHandlers.add(handler);
    }
  }

  /**
   * Removes a variables changed and no downloads pending callback.
   */
  public static void removeMessageDisplayedHandler(
          MessageDisplayedCallback handler) {
    if (handler == null) {
      Log.e("removeMessageDisplayedHandler - Invalid handler parameter " +
              "provided.");
      return;
    }

    synchronized (messageDisplayedHandlers) {
      messageDisplayedHandlers.remove(handler);
    }
  }

  public static void triggerMessageDisplayed(ActionContext actionContext) {
    synchronized (messageDisplayedHandlers) {
      for (MessageDisplayedCallback callback : messageDisplayedHandlers) {
        MessageArchiveData messageArchiveData = messageArchiveDataFromContext(actionContext);
        callback.setMessageArchiveData(messageArchiveData);
        OperationQueue.sharedInstance().addUiOperation(callback);
      }
    }
  }

  private static MessageArchiveData messageArchiveDataFromContext(ActionContext actionContext) {
    String messageID = actionContext.getMessageId();
    String messageBody = "";
    try {
      messageBody = messageBodyFromContext(actionContext);
    } catch (Throwable t) {
      Log.exception(t);
    }
    String recipientUserID = Leanplum.getUserId();
    Date deliveryDateTime = new Date();

    return new MessageArchiveData(messageID, messageBody, recipientUserID, deliveryDateTime);
  }

  @VisibleForTesting
  public static String messageBodyFromContext(ActionContext actionContext) {
    Object messageObject =  actionContext.getArgs().get("Message");
    if (messageObject == null) {
      return null;
    }

    if (messageObject instanceof String) {
      return (String) messageObject;
    } else {
      HashMap<String, String> messageDict = (HashMap<String, String>) messageObject;
      if (messageDict.get("Text") != null &&
              messageDict.get("Text") instanceof String) {
        return messageDict.get("Text");
      }
      if (messageDict.get("Text value") != null &&
              messageDict.get("Text value") instanceof String) {
        return messageDict.get("Text value");
      }
    }
    return null;
  }

  /**
   * Add a callback to call ONCE when no more file downloads are pending (either when no files
   * needed to be downloaded or all downloads have been completed).
   */
  public static void addOnceVariablesChangedAndNoDownloadsPendingHandler(
      VariablesChangedCallback handler) {
    if (handler == null) {
      Log.e("addOnceVariablesChangedAndNoDownloadsPendingHandler - Invalid handler parameter" +
          " provided.");
      return;
    }

    if (VarCache.hasReceivedDiffs()
        && FileTransferManager.getInstance().numPendingDownloads() == 0) {
      handler.variablesChanged();
    } else {
      synchronized (onceNoDownloadsHandlers) {
        onceNoDownloadsHandlers.add(handler);
      }
    }
  }

  /**
   * Removes a once variables changed and no downloads pending callback.
   */
  public static void removeOnceVariablesChangedAndNoDownloadsPendingHandler(
      VariablesChangedCallback handler) {
    if (handler == null) {
      Log.e("removeOnceVariablesChangedAndNoDownloadsPendingHandler - Invalid handler" +
          " parameter provided.");
      return;
    }

    synchronized (onceNoDownloadsHandlers) {
      onceNoDownloadsHandlers.remove(handler);
    }
  }

  static void triggerVariablesChangedAndNoDownloadsPending() {
    synchronized (noDownloadsHandlers) {
      for (VariablesChangedCallback callback : noDownloadsHandlers) {
        OperationQueue.sharedInstance().addUiOperation(callback);
      }
    }
    synchronized (onceNoDownloadsHandlers) {
      for (VariablesChangedCallback callback : onceNoDownloadsHandlers) {
        OperationQueue.sharedInstance().addUiOperation(callback);
      }
      onceNoDownloadsHandlers.clear();
    }
  }

  /**
   * Defines an action that is used within Leanplum Marketing Automation. Actions can be set up to
   * get triggered based on app opens, events, and states. Call {@link Leanplum#onAction} to handle
   * the action.
   *
   * @param name The name of the action to register.
   * @param kind Whether to display the action as a message and/or a regular action.
   * @param args User-customizable options for the action.
   */
  public static void defineAction(String name, int kind, ActionArgs args) {
    defineAction(name, kind, args, null, null);
  }

  /**
   * Defines an action that is used within Leanplum Marketing Automation. Actions can be set up to
   * get triggered based on app opens, events, and states.
   *
   * @param name The name of the action to register.
   * @param kind Whether to display the action as a message and/or a regular action.
   * @param args User-customizable options for the action.
   * @param responder Called when the action is triggered with a context object containing the
   * user-specified options.
   */
  public static void defineAction(String name, int kind, ActionArgs args,
      ActionCallback responder) {
    defineAction(name, kind, args, null, responder);
  }

  private static void defineAction(String name, int kind, ActionArgs args,
      Map<String, Object> options, ActionCallback responder) {
    if (TextUtils.isEmpty(name)) {
      Log.e("defineAction - Empty name parameter provided.");
      return;
    }
    if (args == null) {
      Log.e("defineAction - Invalid args parameter provided.");
      return;
    }

    try {
      if (options == null) {
        options = new HashMap<>();
      }
      LeanplumInternal.getActionHandlers().remove(name);
      VarCache.registerActionDefinition(name, kind, args.getValue(), options);
      if (responder != null) {
        onAction(name, responder);
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  /**
   * Adds a callback that handles an action with the given name.
   *
   * @param actionName The name of the type of action to handle.
   * @param handler The callback that runs when the action is triggered.
   */
  public static void onAction(String actionName, ActionCallback handler) {
    if (actionName == null) {
      Log.e("onAction - Invalid actionName parameter provided.");
      return;
    }
    if (handler == null) {
      Log.e("onAction - Invalid handler parameter provided.");
      return;
    }

    List<ActionCallback> handlers = LeanplumInternal.getActionHandlers().get(actionName);
    if (handlers == null) {
      handlers = new ArrayList<>();
      LeanplumInternal.getActionHandlers().put(actionName, handlers);
    }
    handlers.add(handler);
  }

  /**
   * Updates the user ID and adds or modifies user attributes.
   */
  public static void setUserAttributes(final String userId, Map<String, ?> userAttributes) {
    if (Constants.isNoop()) {
      return;
    }
    if (!LeanplumInternal.hasCalledStart()) {
      Log.e("You cannot call setUserAttributes before calling start");
      return;
    }
    try {
      final HashMap<String, Object> params = new HashMap<>();
      if (userId != null) {
        params.put(Constants.Params.NEW_USER_ID, userId);
      }
      if (userAttributes != null) {
        userAttributes = LeanplumInternal.validateAttributes(userAttributes, "userAttributes",
            true);
        params.put(Constants.Params.USER_ATTRIBUTES, JsonConverter.toJson(userAttributes));
        LeanplumInternal.getUserAttributeChanges().add(userAttributes);
      }

      if (LeanplumInternal.issuedStart()) {
        setUserAttributesInternal(userId, params);
      } else {
        LeanplumInternal.addStartIssuedHandler(new Runnable() {
          @Override
          public void run() {
            try {
              setUserAttributesInternal(userId, params);
            } catch (Throwable t) {
              Log.exception(t);
            }
          }
        });
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  private static void setUserAttributesInternal(String userId,
      HashMap<String, Object> requestArgs) {
    Request request = RequestBuilder.withSetUserAttributesAction().andParams(requestArgs).create();
    RequestSender.getInstance().send(request);
    if (userId != null && userId.length() > 0) {
      APIConfig.getInstance().setUserId(userId);
      if (LeanplumInternal.hasStarted()) {
        VarCache.saveDiffs();
      }
    }
    LeanplumInternal.recordAttributeChanges();
  }

  /**
   * Updates the user ID.
   */
  public static void setUserId(String userId) {
    if (userId == null) {
      Log.e("setUserId - Invalid userId parameter provided.");
      return;
    }

    setUserAttributes(userId, null);
  }

  /**
   * Adds or modifies user attributes.
   */
  public static void setUserAttributes(Map<String, Object> userAttributes) {
    if (userAttributes == null || userAttributes.isEmpty()) {
      Log.e("setUserAttributes - Invalid userAttributes parameter provided (null or empty).");
      return;
    }

    setUserAttributes(null, userAttributes);
  }

  /**
   * Sets the registration ID used for Cloud Messaging.
   */
  static void setRegistrationId(PushProviderType type, final String registrationId) {
    if (Constants.isNoop()) {
      return;
    }
    String attributeName;
    switch (type) {
      case FCM:
        attributeName = Constants.Params.DEVICE_FCM_PUSH_TOKEN;
        break;
      case MIPUSH:
        attributeName = Constants.Params.DEVICE_MIPUSH_TOKEN;
        break;
      case HMS:
        attributeName = Constants.Params.DEVICE_HMS_TOKEN;
        break;
      default:
        return;
    }
    Runnable startIssuedHandler = new Runnable() {
      @Override
      public void run() {
        if (Constants.isNoop()) {
          return;
        }
        try {
          Request request = RequestBuilder
              .withSetDeviceAttributesAction()
              .andParam(attributeName, registrationId)
              .andType(RequestType.IMMEDIATE)
              .create();
          RequestSender.getInstance().send(request);
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
    };
    LeanplumInternal.addStartIssuedHandler(startIssuedHandler);
  }

  /**
   * Sets the traffic source info for the current user. Keys in info must be one of: publisherId,
   * publisherName, publisherSubPublisher, publisherSubSite, publisherSubCampaign,
   * publisherSubAdGroup, publisherSubAd.
   */
  public static void setTrafficSourceInfo(Map<String, String> info) {
    if (Constants.isNoop()) {
      return;
    }
    if (!LeanplumInternal.hasCalledStart()) {
      Log.e("You cannot call setTrafficSourceInfo before calling start");
      return;
    }
    if (info == null || info.isEmpty()) {
      Log.e("setTrafficSourceInfo - Invalid info parameter provided (null or empty).");
      return;
    }

    try {
      final HashMap<String, Object> params = new HashMap<>();
      info = LeanplumInternal.validateAttributes(info, "info", false);
      params.put(Constants.Params.TRAFFIC_SOURCE, JsonConverter.toJson(info));
      if (LeanplumInternal.issuedStart()) {
        setTrafficSourceInfoInternal(params);
      } else {
        LeanplumInternal.addStartIssuedHandler(new Runnable() {
          @Override
          public void run() {
            try {
              setTrafficSourceInfoInternal(params);
            } catch (Throwable t) {
              Log.exception(t);
            }
          }
        });
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  private static void setTrafficSourceInfoInternal(HashMap<String, Object> params) {
    Request requets = RequestBuilder.withSetTrafficSourceInfoAction().andParams(params).create();
    RequestSender.getInstance().send(requets);
  }

  /**
   * Logs a particular event in your application. The string can be any value of your choosing, and
   * will show up in the dashboard.
   * <p>
   * <p>To track Purchase events, call {@link Leanplum#trackGooglePlayPurchase} instead for in-app
   * purchases, or use {@link Leanplum#PURCHASE_EVENT_NAME} as the event name for other types of
   * purchases.
   *
   * @param event Name of the event. Event may be empty for message impression events.
   * @param value The value of the event. The value is special in that you can use it for targeting
   * content and messages to users who have a particular lifetime value. For purchase events, the
   * value is the revenue associated with the purchase.
   * @param info Basic context associated with the event, such as the item purchased. info is
   * treated like a default parameter.
   * @param params Key-value pairs with metrics or data associated with the event. Parameters can be
   * strings or numbers. You can use up to 200 different parameter names in your app.
   */
  public static void track(final String event, double value, String info,
      Map<String, ?> params) {
    LeanplumInternal.track(event, value, info, params, null);
  }

  /**
   * Manually track purchase event with currency code in your application. It is advised to use
   * {@link Leanplum#trackGooglePlayPurchase} instead for in-app purchases.
   *
   * @param event Name of the event.
   * @param value The value of the event. Can be price.
   * @param currencyCode The currency code corresponding to the price.
   * @param params Key-value pairs with metrics or data associated with the event. Parameters can be
   * strings or numbers. You can use up to 200 different parameter names in your app.
   */
  public static void trackPurchase(final String event, double value, String currencyCode,
      Map<String, ?> params) {
    try {
      if (TextUtils.isEmpty(event)) {
        Log.i("Failed to trackPurchase, event name is null");
      }

      final Map<String, String> requestArgs = new HashMap<>();
      if (!TextUtils.isEmpty(currencyCode)) {
        requestArgs.put(Constants.Params.IAP_CURRENCY_CODE, currencyCode);
      }

      LeanplumInternal.track(event, value, null, params, requestArgs);
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  /**
   * Tracks an in-app purchase as a Purchase event.
   *
   * @param item The name of the item that was purchased.
   * @param priceMicros The price in micros in the user's local currency.
   * @param currencyCode The currency code corresponding to the price.
   * @param purchaseData Purchase data from purchase.getOriginalJson().
   * @param dataSignature Signature from purchase.getSignature().
   */
  public static void trackGooglePlayPurchase(String item, long priceMicros, String currencyCode,
      String purchaseData, String dataSignature) {
    trackGooglePlayPurchase(PURCHASE_EVENT_NAME, item, priceMicros, currencyCode, purchaseData,
        dataSignature, null);
  }

  /**
   * Tracks an in-app purchase as a Purchase event.
   *
   * @param item The name of the item that was purchased.
   * @param priceMicros The price in micros in the user's local currency.
   * @param currencyCode The currency code corresponding to the price.
   * @param purchaseData Purchase data from purchase.getOriginalJson().
   * @param dataSignature Signature from purchase.getSignature().
   * @param params Any additional parameters to track with the event.
   */
  public static void trackGooglePlayPurchase(String item, long priceMicros, String currencyCode,
      String purchaseData, String dataSignature, Map<String, ?> params) {
    trackGooglePlayPurchase(PURCHASE_EVENT_NAME, item, priceMicros, currencyCode,
        purchaseData, dataSignature, params);
  }

  /**
   * Tracks an in-app purchase.
   *
   * @param eventName The name of the event to record the purchase under. Normally, this would be
   * {@link Leanplum#PURCHASE_EVENT_NAME}.
   * @param item The name of the item that was purchased.
   * @param priceMicros The price in micros in the user's local currency.
   * @param currencyCode The currency code corresponding to the price.
   * @param purchaseData Purchase data from purchase.getOriginalJson().
   * @param dataSignature Signature from purchase.getSignature().
   * @param params Any additional parameters to track with the event.
   */
  @SuppressWarnings("SameParameterValue")
  public static void trackGooglePlayPurchase(String eventName, String item, long priceMicros,
      String currencyCode, String purchaseData, String dataSignature, Map<String, ?> params) {
    if (TextUtils.isEmpty(eventName)) {
      Log.i("Failed to trackGooglePlayPurchase, event name is null");
    }

    final Map<String, String> requestArgs = new HashMap<>();
    requestArgs.put(Constants.Params.GOOGLE_PLAY_PURCHASE_DATA, purchaseData);
    requestArgs.put(Constants.Params.GOOGLE_PLAY_PURCHASE_DATA_SIGNATURE, dataSignature);
    requestArgs.put(Constants.Params.IAP_CURRENCY_CODE, currencyCode);

    Map<String, Object> modifiedParams;
    if (params == null) {
      modifiedParams = new HashMap<>();
    } else {
      modifiedParams = new HashMap<>(params);
    }
    modifiedParams.put(Constants.Params.IAP_ITEM, item);

    LeanplumInternal.track(eventName, priceMicros / 1000000.0, null, modifiedParams, requestArgs);
  }

  /**
   * Logs a particular event in your application. The string can be any value of your choosing, and
   * will show up in the dashboard.
   * <p>
   * <p>To track Purchase events, use {@link Leanplum#PURCHASE_EVENT_NAME}.
   *
   * @param event Name of the event.
   */
  public static void track(String event) {
    track(event, 0.0, "", null);
  }

  /**
   * Logs a particular event in your application. The string can be any value of your choosing, and
   * will show up in the dashboard.
   * <p>
   * <p>To track Purchase events, use {@link Leanplum#PURCHASE_EVENT_NAME}.
   *
   * @param event Name of the event.
   * @param value The value of the event. The value is special in that you can use it for targeting
   * content and messages to users who have a particular lifetime value. For purchase events, the
   * value is the revenue associated with the purchase.
   */
  public static void track(String event, double value) {
    track(event, value, "", null);
  }

  /**
   * Logs a particular event in your application. The string can be any value of your choosing, and
   * will show up in the dashboard.
   * <p>
   * <p>To track Purchase events, use {@link Leanplum#PURCHASE_EVENT_NAME}.
   *
   * @param event Name of the event.
   * @param info Basic context associated with the event, such as the item purchased. info is
   * treated like a default parameter.
   */
  public static void track(String event, String info) {
    track(event, 0.0, info, null);
  }

  /**
   * Logs a particular event in your application. The string can be any value of your choosing, and
   * will show up in the dashboard.
   * <p>
   * <p>To track Purchase events, use {@link Leanplum#PURCHASE_EVENT_NAME}.
   *
   * @param event Name of the event.
   * @param params Key-value pairs with metrics or data associated with the event. Parameters can be
   * strings or numbers. You can use up to 200 different parameter names in your app.
   */
  public static void track(String event, Map<String, ?> params) {
    track(event, 0.0, "", params);
  }

  /**
   * Logs a particular event in your application. The string can be any value of your choosing, and
   * will show up in the dashboard.
   * <p>
   * <p>To track Purchase events, use {@link Leanplum#PURCHASE_EVENT_NAME}.
   *
   * @param event Name of the event.
   * @param value The value of the event. The value is special in that you can use it for targeting
   * content and messages to users who have a particular lifetime value. For purchase events, the
   * value is the revenue associated with the purchase.
   * @param params Key-value pairs with metrics or data associated with the event. Parameters can be
   * strings or numbers. You can use up to 200 different parameter names in your app.
   */
  public static void track(String event, double value, Map<String, ?> params) {
    track(event, value, "", params);
  }

  /**
   * Logs a particular event in your application. The string can be any value of your choosing, and
   * will show up in the dashboard.
   * <p>
   * <p>To track Purchase events, use {@link Leanplum#PURCHASE_EVENT_NAME}.
   *
   * @param event Name of the event.
   * @param value The value of the event. The value is special in that you can use it for targeting
   * content and messages to users who have a particular lifetime value. For purchase events, the
   * value is the revenue associated with the purchase.
   * @param info Basic context associated with the event, such as the item purchased. info is
   * treated like a default parameter.
   */
  public static void track(String event, double value, String info) {
    track(event, value, info, null);
  }

  /**
   * Advances to a particular state in your application. The string can be any value of your
   * choosing, and will show up in the dashboard. A state is a section of your app that the user is
   * currently in.
   *
   * @param event Event type.
   * @param info Basic context associated with the state, such as the item purchased. info is
   * treated like a default parameter.
   */

  public static void trackGeofence(GeofenceEventType event, String info) {
    if (featureFlagManager().isFeatureFlagEnabled("track_geofence")) {
      LeanplumInternal.trackGeofence(event, 0.0, info, null, null);
    }
  }

  public static void advanceTo(final String state, String info, final Map<String, ?> params) {
    if (Constants.isNoop()) {
      return;
    }
    if (!LeanplumInternal.hasCalledStart()) {
      Log.e("You cannot call advanceTo before calling start");
      return;
    }

    try {
      final Map<String, Object> requestParams = new HashMap<>();
      requestParams.put(Constants.Params.INFO, info);
      requestParams.put(Constants.Params.STATE, state);
      final Map<String, ?> validatedParams;
      if (params != null) {
        validatedParams = LeanplumInternal.validateAttributes(params, "params", false);
        requestParams.put(Constants.Params.PARAMS, JsonConverter.toJson(validatedParams));
      } else {
        validatedParams = null;
      }

      if (LeanplumInternal.issuedStart()) {
        advanceToInternal(state, validatedParams, requestParams);
      } else {
        LeanplumInternal.addStartIssuedHandler(new Runnable() {
          @Override
          public void run() {
            try {
              advanceToInternal(state, validatedParams, requestParams);
            } catch (Throwable t) {
              Log.exception(t);
            }
          }
        });
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  /**
   * Performs the advance API and any actions that are associated with the state.
   *
   * @param state The state name. State may be empty for message impression events.
   * @param params The state parameters.
   * @param requestParams The arguments to send with the API request.
   */
  private static void advanceToInternal(String state, Map<String, ?> params,
      Map<String, Object> requestParams) {
    Request request = RequestBuilder.withAdvanceAction().andParams(requestParams).create();
    RequestSender.getInstance().send(request);

    ContextualValues contextualValues = new ContextualValues();
    contextualValues.parameters = params;

    LeanplumInternal.maybePerformActions("state", state,
        LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL, null, contextualValues);
  }

  /**
   * Advances to a particular state in your application. The string can be any value of your
   * choosing, and will show up in the dashboard. A state is a section of your app that the user is
   * currently in.
   *
   * @param state Name of the state. State may be empty for message impression events.
   */
  public static void advanceTo(String state) {
    advanceTo(state, "", null);
  }

  /**
   * Advances to a particular state in your application. The string can be any value of your
   * choosing, and will show up in the dashboard. A state is a section of your app that the user is
   * currently in.
   *
   * @param state Name of the state. State may be empty for message impression events.
   * @param info Basic context associated with the state, such as the item purchased. info is
   * treated like a default parameter.
   */
  public static void advanceTo(String state, String info) {
    advanceTo(state, info, null);
  }

  /**
   * Advances to a particular state in your application. The string can be any value of your
   * choosing, and will show up in the dashboard. A state is a section of your app that the user is
   * currently in.
   *
   * @param state Name of the state. State may be empty for message impression events.
   * @param params Key-value pairs with metrics or data associated with the state. Parameters can be
   * strings or numbers. You can use up to 200 different parameter names in your app.
   */
  public static void advanceTo(String state, Map<String, ?> params) {
    advanceTo(state, "", params);
  }

  /**
   * Pauses the current state. You can use this if your game has a "pause" mode. You shouldn't call
   * it when someone switches out of your app because that's done automatically.
   */
  public static void pauseState() {
    if (Constants.isNoop()) {
      return;
    }
    if (!LeanplumInternal.hasCalledStart()) {
      Log.e("You cannot call pauseState before calling start");
      return;
    }

    try {
      if (LeanplumInternal.issuedStart()) {
        pauseStateInternal();
      } else {
        LeanplumInternal.addStartIssuedHandler(new Runnable() {
          @Override
          public void run() {
            try {
              pauseStateInternal();
            } catch (Throwable t) {
              Log.exception(t);
            }
          }
        });
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  private static void pauseStateInternal() {
    Request request = RequestBuilder.withPauseStateAction().create();
    RequestSender.getInstance().send(request);
  }

  /**
   * Resumes the current state.
   */
  public static void resumeState() {
    if (Constants.isNoop()) {
      return;
    }
    if (!LeanplumInternal.hasCalledStart()) {
      Log.e("You cannot call resumeState before calling start");
      return;
    }

    try {
      if (LeanplumInternal.issuedStart()) {
        resumeStateInternal();
      } else {
        LeanplumInternal.addStartIssuedHandler(new Runnable() {
          @Override
          public void run() {
            try {
              resumeStateInternal();
            } catch (Throwable t) {
              Log.exception(t);
            }
          }
        });
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  private static void resumeStateInternal() {
    Request request = RequestBuilder.withResumeStateAction().create();
    RequestSender.getInstance().send(request);
  }

  /**
   * Forces content to update from the server. If variables have changed, the appropriate callbacks
   * will fire. Use sparingly as if the app is updated, you'll have to deal with potentially
   * inconsistent state or user experience.
   */
  public static void forceContentUpdate() {
    forceContentUpdate(success -> {});
  }

  /**
   * Forces content to update from the server. If variables have changed, the appropriate callbacks
   * will fire. Use sparingly as if the app is updated, you'll have to deal with potentially
   * inconsistent state or user experience.
   *
   * @param callback The callback to invoke when the call completes from the server. The callback
   * will fire regardless of whether the variables have changed.
   */
  public static void forceContentUpdate(VariablesChangedCallback callback) {
    forceContentUpdate(success -> {
      if (callback != null) {
        callback.variablesChanged();
      }
    });
  }

  /**
   * Forces content to update from the server. If variables have changed, the appropriate callbacks
   * will fire. Use sparingly as if the app is updated, you'll have to deal with potentially
   * inconsistent state or user experience.
   *
   * @param callback The callback to invoke when the call completes from the server. The callback
   *                 will fire regardless of whether the variables have changed. Null value is not
   *                 permitted.
   */
  public static void forceContentUpdate(@NonNull ForceContentUpdateCallback callback) {
    if (Constants.isNoop()) {
      OperationQueue.sharedInstance().addUiOperation(() -> callback.onContentUpdated(false));
      return;
    }
    try {
      Request req = RequestBuilder
          .withGetVarsAction()
          .andParam(Constants.Params.INCLUDE_DEFAULTS, Boolean.toString(false))
          .andParam(Constants.Params.INBOX_MESSAGES, LeanplumInbox.getInstance().messagesIds())
          .andParam(Constants.Params.INCLUDE_VARIANT_DEBUG_INFO, LeanplumInternal.getIsVariantDebugInfoEnabled())
          .andType(RequestType.IMMEDIATE)
          .create();
      req.onResponse(new Request.ResponseCallback() {
        @Override
        public void response(JSONObject response) {
          try {
            if (response == null) {
              Log.e("No response received from the server. Please contact us to investigate.");
            } else {
              applyContentInResponse(response);
              if (response.optBoolean(Constants.Keys.SYNC_INBOX, false)) {
                LeanplumInbox.getInstance().downloadMessages();
              } else {
                LeanplumInbox.getInstance().triggerInboxSyncedWithStatus(true);
              }
              if (response.optBoolean(Constants.Keys.LOGGING_ENABLED, false)) {
                Constants.loggingEnabled = true;
              }

              parseVariantDebugInfo(response);
              Map<String, String> filenameToURLs = parseFilenameToURLs(response);
              FileManager.setFilenameToURLs(filenameToURLs);
            }
            OperationQueue.sharedInstance().addUiOperation(() -> callback.onContentUpdated(true));
          } catch (Throwable t) {
            Log.exception(t);
          }
        }
      });
      req.onError(new Request.ErrorCallback() {
        @Override
        public void error(Exception e) {
          OperationQueue.sharedInstance().addUiOperation(() -> callback.onContentUpdated(false));
          LeanplumInbox.getInstance().triggerInboxSyncedWithStatus(false);
        }
      });
      RequestSender.getInstance().send(req);
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  /**
   * This should be your first statement in a unit test. This prevents Leanplum from communicating
   * with the server.
   */
  public static void enableTestMode() {
    Constants.isTestMode = true;
  }

  public static boolean isTestModeEnabled() {
    return Constants.isTestMode;
  }

  /**
   * This should be your first statement in a unit test. This prevents Leanplum from communicating
   * with the server.
   */
  public static void setIsTestModeEnabled(boolean isTestModeEnabled) {
    Constants.isTestMode = isTestModeEnabled;
  }

  /**
   * Gets the path for a particular resource. The resource can be overridden by the server.
   */
  public static String pathForResource(String filename) {
    if (TextUtils.isEmpty(filename)) {
      Log.i("pathForResource - Empty filename parameter provided.");
      return null;
    }

    Var fileVar = Var.defineFile(filename, filename);
    return (fileVar != null) ? fileVar.fileValue() : null;
  }

  /**
   * Traverses the variable structure with the specified path. Path components can be either strings
   * representing keys in a dictionary, or integers representing indices in a list.
   */
  public static Object objectForKeyPath(Object... components) {
    return objectForKeyPathComponents(components);
  }

  /**
   * Traverses the variable structure with the specified path. Path components can be either strings
   * representing keys in a dictionary, or integers representing indices in a list.
   */
  public static Object objectForKeyPathComponents(Object[] pathComponents) {
    try {
      return VarCache.getMergedValueFromComponentArray(pathComponents);
    } catch (Throwable t) {
      Log.exception(t);
    }
    return null;
  }

  /**
   * Returns information about the active variants for the current user. Each variant will contain
   * an "id" key mapping to the numeric ID of the variant.
   */
  public static List<Map<String, Object>> variants() {
    List<Map<String, Object>> variants = VarCache.variants();
    if (variants == null) {
      return new ArrayList<>();
    }
    return variants;
  }

  /**
   * Returns the last received signed variables. If signature was not provided from server the
   * result of this method will be null.
   *
   * @return {@link SecuredVars} instance containing variable's JSON and signature. If signature
   * wasn't downloaded from server it will return null.
   */
  @Nullable
  public static SecuredVars securedVars() {
    return VarCache.getSecuredVars();
  }

  /**
   * Returns metadata for all active in-app messages. Recommended only for debugging purposes and
   * advanced use cases.
   */
  public static Map<String, Object> messageMetadata() {
    Map<String, Object> messages = VarCache.messages();
    if (messages == null) {
      return new HashMap<>();
    }
    return messages;
  }

  /**
   * Details about the variable assignments on the server.
   */
  public static Map<String, Object> getVariantDebugInfo() {
    return VarCache.getVariantDebugInfo();
  }

  /**
   * Set location manually. Calls setDeviceLocation with cell type. Best if used in after calling
   * disableLocationCollection.
   *
   * @param location Device location.
   */
  public static void setDeviceLocation(Location location) {
    setDeviceLocation(location, LeanplumLocationAccuracyType.CELL);
  }

  /**
   * Set location manually. Best if used in after calling disableLocationCollection. Useful if you
   * want to apply additional logic before sending in the location.
   *
   * @param location Device location.
   * @param type LeanplumLocationAccuracyType of the location.
   */
  public static void setDeviceLocation(Location location, LeanplumLocationAccuracyType type) {
    if (locationCollectionEnabled) {
      Log.i("Leanplum is automatically collecting device location, so there is no need to " +
          "call setDeviceLocation. If you prefer to always set location manually, " +
          "then call disableLocationCollection.");
    }
    LeanplumInternal.setUserLocationAttribute(location, type,
        new LeanplumInternal.locationAttributeRequestsCallback() {
          @Override
          public void response(boolean success) {
            if (success) {
              Log.d("setUserAttributes with location is successfully called");
            }
          }
        });
  }

  /**
   * Disable location collection by setting |locationCollectionEnabled| to false.
   */
  public static void disableLocationCollection() {
    locationCollectionEnabled = false;
  }

  /**
   * Returns whether a customer enabled location collection.
   *
   * @return The value of |locationCollectionEnabled|.
   */
  public static boolean isLocationCollectionEnabled() {
    return locationCollectionEnabled;
  }

  private static void parseVariantDebugInfo(JSONObject response) {
    Map<String, Object> variantDebugInfo = JsonConverter.mapFromJsonOrDefault(
            response.optJSONObject(Constants.Keys.VARIANT_DEBUG_INFO));
    if (variantDebugInfo.size() > 0) {
      VarCache.setVariantDebugInfo(variantDebugInfo);
    }
  }

  /**
   * Clears cached values for messages, variables and test assignments.
   * Use sparingly as if the app is updated, you'll have to deal with potentially
   * inconsistent state or user experience.
   */
  public static void clearUserContent() {
    VarCache.clearUserContent();
  }

  @VisibleForTesting
  public static Set<String> parseSdkCounters(JSONObject response) {
    JSONArray enabledCounters = response.optJSONArray(
            Constants.Keys.ENABLED_COUNTERS);
    Set<String> counterSet = toSet(enabledCounters);
    return counterSet;
  }

  @VisibleForTesting
  public static Set<String> parseFeatureFlags(JSONObject response) {
    JSONArray enabledFeatureFlags = response.optJSONArray(
            Constants.Keys.ENABLED_FEATURE_FLAGS);
    Set<String> featureFlagSet = toSet(enabledFeatureFlags);
    return featureFlagSet;
  }

  @VisibleForTesting
  public static Map<String, String> parseFilenameToURLs(JSONObject response) {
    JSONObject filesObject = response.optJSONObject(
            Constants.Keys.FILES);
    if (filesObject != null) {
      return JsonConverter.mapFromJson(filesObject);
    }
    return null;
  }

  private static Set<String> toSet(JSONArray array) {
    Set<String> set = new HashSet<>();
    if (array != null) {
      for (int i = 0; i < array.length(); i++) {
        set.add(array.optString(i));
      }
    }
    return set;
  }

  public static CountAggregator countAggregator() {
    return countAggregator;
  }

  public static FeatureFlagManager featureFlagManager() {
    return featureFlagManager;
  }

  /**
   * Sets the time interval to periodically upload events to server.
   * Default is {@link EventsUploadInterval#AT_MOST_15_MINUTES}.
   *
   * @param uploadInterval The time between uploads.
   */
  public static void setEventsUploadInterval(EventsUploadInterval uploadInterval) {
    if (uploadInterval != null) {
      RequestSenderTimer.get().setTimerInterval(uploadInterval);
    }
  }

  /**
   * Enable or disable push delivery tracking. It is enabled by default.
   */
  public static void setPushDeliveryTracking(boolean enable) {
    pushDeliveryTrackingEnabled = enable;
  }

  /**
   * Returns whether the push delivery tracking is enabled.
   *
   * @return True if push delivery tracking is enabled, false otherwise.
   */
  public static boolean isPushDeliveryTrackingEnabled() {
    return pushDeliveryTrackingEnabled;
  }
}
