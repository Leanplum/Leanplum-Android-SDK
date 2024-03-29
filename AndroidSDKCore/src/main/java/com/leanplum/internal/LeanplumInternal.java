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

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import androidx.annotation.NonNull;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.LeanplumException;
import com.leanplum.LeanplumLocationAccuracyType;
import com.leanplum.actions.internal.ActionManagerTriggeringKt;
import com.leanplum.actions.internal.ActionsTrigger;
import com.leanplum.actions.internal.Priority;
import com.leanplum.callbacks.StartCallback;
import com.leanplum.internal.Request.RequestType;
import com.leanplum.migration.MigrationManager;
import com.leanplum.models.GeofenceEventType;

import java.util.Arrays;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Internal leanplum methods that are very generic but should not be exposed to the public.
 *
 * @author Ben Marten
 */
public class LeanplumInternal {
  private static final String ACTION = "action";
  private static final String LEANPLUM_LOCAL_PUSH_HELPER =
      "com.leanplum.internal.LeanplumLocalPushHelper";
  private static boolean hasStartedAndRegisteredAsDeveloper;
  private static boolean issuedStart;
  private static boolean hasStarted;
  private static boolean startSuccessful;
  private static boolean calledStart;
  private static boolean isPaused;

  private static boolean startedInBackground;
  private static boolean inForeground;
  private static final Object inForegroundLock = new Object();
  private static final Queue<Map<String, ?>> userAttributeChanges = new ConcurrentLinkedQueue<>();
  private static final ArrayList<Runnable> startIssuedHandlers = new ArrayList<>();
  private static boolean isScreenTrackingEnabled = false;
  private static boolean isVariantDebugInfoEnabled = false;

  private static void onHasStartedAndRegisteredAsDeveloperAndFinishedSyncing() {
    if (!hasStartedAndRegisteredAsDeveloper) {
      hasStartedAndRegisteredAsDeveloper = true;
    }
  }

  /**
   * Called when the development mode has started and the device is registered successfully.
   */
  public static void onHasStartedAndRegisteredAsDeveloper() {
    synchronized (FileManager.initializingLock) {
      if (FileManager.initializing()) {
        FileManager.setResourceSyncFinishBlock(new FileManager.ResourceUpdateCallback() {
          @Override
          public void onResourceSyncFinish() {
            try {
              onHasStartedAndRegisteredAsDeveloperAndFinishedSyncing();
            } catch (Throwable t) {
              Log.exception(t);
            }
          }
        });
      } else {
        onHasStartedAndRegisteredAsDeveloperAndFinishedSyncing();
      }
    }
  }

  public static void maybePerformActions(String when, String eventName, int filter,
      String fromMessageId, ActionContext.ContextualValues contextualValues) {
    maybePerformActions(new String[] {when}, eventName, filter, fromMessageId, contextualValues);
  }

  static void maybePerformActions(String[] whenConditions, String eventName, int filter,
      String fromMessageId, ActionContext.ContextualValues contextualValues) {
    Map<String, Object> messages = VarCache.messages();
    if (messages == null) {
      return;
    }

    ArrayList<ActionContext> actionContexts = new ArrayList<>();
    for (final String messageId : messages.keySet()) {
      if (messageId != null && messageId.equals(fromMessageId)) {
        continue;
      }

      Map<String, Object> messageConfig = CollectionUtil.uncheckedCast(messages.get(messageId));

      String actionType = (String) messageConfig.get(ACTION);
      if (actionType == null) {
        continue;
      }

      final String internalMessageId;
      if (actionType.equals(ActionManager.HELD_BACK_ACTION_NAME)) {
        // Spoof the message ID for held back messages,
        // and store the original ID for the track event.
        internalMessageId = Constants.HELD_BACK_MESSAGE_PREFIX + messageId;
      } else {
        // If this is not a holdback, the internal message ID is the same as the original.
        internalMessageId = messageId;
      }

      boolean isForeground = !actionType.equals(ActionManager.PUSH_NOTIFICATION_ACTION_NAME);
      if (isForeground &&
          ((filter & LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_FOREGROUND) == 0)) {
        continue;
      }

      ActionManager.MessageMatchResult result = new ActionManager.MessageMatchResult();
      for (String when : whenConditions) {
        ActionManager.MessageMatchResult conditionResult = ActionManager.getInstance()
            .shouldShowMessage(internalMessageId, messageConfig, when, eventName, contextualValues);
        result.matchedTrigger |= conditionResult.matchedTrigger;
        result.matchedUnlessTrigger |= conditionResult.matchedUnlessTrigger;
        result.matchedLimit |= conditionResult.matchedLimit;
        result.matchedActivePeriod |= conditionResult.matchedActivePeriod;
      }

      // Make sure we cancel before matching in case the criteria overlap.
      if (result.matchedUnlessTrigger) {
        // Currently Unless Trigger is possible for Local Notifications only
        if (ActionManager.PUSH_NOTIFICATION_ACTION_NAME.equals(messageConfig.get(ACTION))) {
          cancelLocalPush(messageId);
        }
      }

      // Make sure message is within the active period.
      if(!result.matchedActivePeriod){
        continue;
      }

      if (result.matchedTrigger) {
        ActionManager.getInstance().recordMessageTrigger(internalMessageId);

        if (result.matchedLimit) {
          int priority;
          if (messageConfig.containsKey("priority")) {
            priority = (int) messageConfig.get("priority");
          } else {
            priority = Constants.Messaging.DEFAULT_PRIORITY;
          }
          Map<String, Object> vars = CollectionUtil.uncheckedCast(messageConfig.get(Constants
              .Keys.VARS));
          ActionContext actionContext = new ActionContext(
              messageConfig.get(ACTION).toString(),
              vars,
              internalMessageId,
              messageId,
              priority);
          actionContext.setContextualValues(contextualValues);
          actionContexts.add(actionContext);
        }
      }
    }

    if (!actionContexts.isEmpty()) {
      Collections.sort(actionContexts, new Comparator<ActionContext>() {
        @Override
        public int compare(ActionContext o1, ActionContext o2) {
          return o1.getPriority() - o2.getPriority();
        }
      });
      List<ActionContext> triggerContexts = new ArrayList<>();
      for (final ActionContext actionContext : actionContexts) {
        if (actionContext.actionName().equals(ActionManager.HELD_BACK_ACTION_NAME)) {
          ActionManager.getInstance().recordHeldBackImpression(
              actionContext.getMessageId(), actionContext.getOriginalMessageId());
        } else {
          if (ActionManager.PUSH_NOTIFICATION_ACTION_NAME.equals(actionContext.actionName())) {
            scheduleLocalPush(actionContext);
          } else {
            if (shouldSuppressMessage(actionContext)) {
              Log.i("Local IAM caps reached, suppressing messageId=" + actionContext.getMessageId());
            } else {
              triggerContexts.add(actionContext);
            }
          }
        }
      }
      ActionsTrigger trigger = new ActionsTrigger(
          eventName,
          Arrays.asList(whenConditions),
          contextualValues);

      ActionManagerTriggeringKt.trigger(
          ActionManager.getInstance(),
          triggerContexts,
          Priority.DEFAULT,
          trigger);
    }
  }

  /**
   * Checks if message should be suppressed based on the local IAM caps.
   *
   * @param context The message context to check.
   * @return True if message should  be suppressed, false otherwise.
   */
  public static boolean shouldSuppressMessage(@NonNull ActionContext context) {
    if (ActionManager.PUSH_NOTIFICATION_ACTION_NAME.equals(context.actionName())) {
      // do not suppress local push
      return false;
    }

    // checks if message caps are reached
    return ActionManager.getInstance().shouldSuppressMessages();
  }

  private static void scheduleLocalPush(ActionContext actionContext) {
    try {
      boolean scheduled = (boolean) Class.forName(LEANPLUM_LOCAL_PUSH_HELPER)
          .getDeclaredMethod("scheduleLocalPush", ActionContext.class)
          .invoke(new Object(), actionContext);

      if (scheduled) {
        // Special case for local push notification. Do not want to count impression.
        ActionManager.getInstance().recordLocalPushImpression(actionContext.getMessageId());
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  private static void cancelLocalPush(@NonNull String messageId) {
    try {
      boolean canceled = (boolean) Class.forName(LEANPLUM_LOCAL_PUSH_HELPER)
          .getDeclaredMethod("cancelLocalPush", String.class)
          .invoke(null, messageId);

      if (canceled) {
        Map<String, String> requestArgs = new HashMap<>();
        requestArgs.put(Constants.Params.MESSAGE_ID, messageId);
        track("Cancel", 0.0, null, null, requestArgs);
      }
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  private static int fetchCountDown(ActionContext context, Map<String, Object> messages) {
    if (context == null || messages == null) {
      return 0;
    }

    // Get eta.
    if (((BaseActionContext) context).isPreview()) {
      return 5;
    } else {
      // We need to lookup by original message id, to avoid looking up messages with
      // added prefix of __held_back__ to message id.
      String originalMessageId = context.getOriginalMessageId();
      if (originalMessageId == null) {
        return 0;
      }
      Map<String, Object> messageConfig = CollectionUtil.uncheckedCast(messages.get(originalMessageId));
      Object countdown = messageConfig.get("countdown");
      if (countdown instanceof Number) {
        return ((Number) countdown).intValue();
      }
      return 0;
    }
  }

  private static Map<String, Object> makeTrackArgs(final String event, double value, String info,
      Map<String, ?> params, Map<String, String> args) {
    final Map<String, Object> requestParams = new HashMap<>();
    if (args != null) {
      requestParams.putAll(args);
    }
    requestParams.put(Constants.Params.VALUE, Double.toString(value));
    requestParams.put(Constants.Params.INFO, info);
    if (event != null) {
      requestParams.put(Constants.Params.EVENT, event);
    }
    if (params != null) {
      params = validateAttributes(params, "params", false);
      requestParams.put(Constants.Params.PARAMS, JsonConverter.toJson(params));
    }
    if (!inForeground || LeanplumActivityHelper.isActivityPaused()) {
      requestParams.put("allowOffline", Boolean.TRUE.toString());
    }
    return requestParams;
  }

  public static void track(final String event, double value, String info,
      Map<String, ?> params, Map<String, String> args) {
    if (Constants.isNoop()) {
      return;
    }

    try {
      final Map<String, Object> requestParams = makeTrackArgs(event, value, info, params, args);
      trackInternalWhenStarted(event, params, requestParams);
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  public static void trackGeofence(final GeofenceEventType event, double value, String info,
      Map<String, ?> params, Map<String, String> args) {
    if (Constants.isNoop()) {
      return;
    }

    try {
      final Map<String, Object> requestParams = makeTrackArgs(event.getName(), value, info, params, args);
      Request request = RequestBuilder
          .withTrackGeofenceAction()
          .andParams(requestParams)
          .andType(RequestType.IMMEDIATE)
          .create();
      RequestSender.getInstance().send(request);
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  /**
   * Performs the track operation once Leanplum has started. If Leanplum is already started, perform
   * the track immediately.
   *
   * @param event The event name. Event may be empty for message impression events.
   * @param params The event parameters.
   * @param requestArgs The arguments to send with the API request.
   */
  private static void trackInternalWhenStarted(final String event,
      final Map<String, ?> params, final Map<String, Object> requestArgs) {
    if (issuedStart) {
      trackInternal(event, params, requestArgs);
    } else {
      addStartIssuedHandler(new Runnable() {
        @Override
        public void run() {
          try {
            trackInternal(event, params, requestArgs);
          } catch (Throwable t) {
            Log.exception(t);
          }
        }
      });
    }
  }

  /**
   * Performs the track API and any actions that are associated with the event.
   *
   * @param event The event name. Event may be empty for message impression events.
   * @param params The event parameters.
   * @param requestArgs The arguments to send with the API request.
   */
  private static void trackInternal(String event, Map<String, ?> params,
      Map<String, Object> requestArgs) {
    Request request = RequestBuilder.withTrackAction().andParams(requestArgs).create();
    RequestSender.getInstance().send(request);

    String eventTriggerName = event;
    String messageId = null;
    if (requestArgs.get(Constants.Params.MESSAGE_ID) != null) {
      messageId = requestArgs.get(Constants.Params.MESSAGE_ID).toString();
      eventTriggerName = ".m" + messageId;
      if (event != null && event.length() > 0) {
        eventTriggerName += " " + event;
      }
    }

    ActionContext.ContextualValues contextualValues = new ActionContext.ContextualValues();
    contextualValues.parameters = params;
    contextualValues.arguments = requestArgs;
    if (contextualValues.arguments.containsKey(Constants.Params.PARAMS)) {
      try {
        contextualValues.arguments.put(Constants.Params.PARAMS,
            new JSONObject(contextualValues.arguments.get(Constants.Params.PARAMS).toString()));
      } catch (JSONException ignored) {
      }
    }
    LeanplumInternal.maybePerformActions("event", eventTriggerName,
        LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL, messageId, contextualValues);
  }

  public static void performTrackedAction(String actionName, String messageId) {
    Map<String, Object> messages = VarCache.messages();
    if (messages == null) {
      return;
    }
    Map<String, Object> messageConfig = CollectionUtil.uncheckedCast(messages.get(messageId));
    if (messageConfig == null) {
      return;
    }
    Map<String, Object> vars = CollectionUtil.uncheckedCast(messageConfig.get(Constants.Keys.VARS));
    ActionContext context = new ActionContext(
        messageConfig.get(ACTION).toString(),
        vars,
        messageId);
    context.runTrackedActionNamed(actionName);
  }

  /**
   * Callback for setUserLocationAttribute.
   */
  public interface locationAttributeRequestsCallback {
    void response(boolean success);
  }

  /**
   * Callback for setUserLocationAttribute.
   *
   * @param location Location of the device.
   * @param locationAccuracyType LocationAccuracyType of the location.
   * @param callback Callback for the requests was made.
   */
  public static void setUserLocationAttribute(final Location location,
      final LeanplumLocationAccuracyType locationAccuracyType,
      final locationAttributeRequestsCallback callback) {
    Leanplum.addStartResponseHandler(new StartCallback() {
      @Override
      public void onResponse(final boolean success) {
        OperationQueue.sharedInstance().addParallelOperation(new Runnable() {
          @Override
          public void run() {
            if (!success) {
              return;
            }
            if (location == null) {
              Log.e("Location can't be null in setUserLocationAttribute.");
              return;
            }
            String latLongLocation = String.format(Locale.US, "%.6f,%.6f", location.getLatitude(),
                location.getLongitude());
            HashMap<String, Object> params = new HashMap<>();
            params.put(Constants.Keys.LOCATION, latLongLocation);
            params.put(Constants.Keys.LOCATION_ACCURACY_TYPE,
                locationAccuracyType.name().toLowerCase());
            if (Leanplum.getContext() != null && Locale.US != null) {
              Geocoder geocoder = new Geocoder(Leanplum.getContext(), Locale.US);
              try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1);
                if (addresses != null && addresses.size() > 0) {
                  Address address = addresses.get(0);
                  params.put(Constants.Keys.CITY, address.getLocality());
                  params.put(Constants.Keys.REGION, address.getAdminArea());
                  params.put(Constants.Keys.COUNTRY, address.getCountryCode());
                }
              } catch (IOException e) {
                Log.e("Failed to connect to Geocoder: " + e);
              } catch (IllegalArgumentException e) {
                Log.e("Invalid latitude or longitude values: " + e);
              } catch (Throwable ignored) {
              }
            }
            Request req = RequestBuilder.withSetUserAttributesAction().andParams(params).create();
            req.onResponse(new Request.ResponseCallback() {
              @Override
              public void response(JSONObject response) {
                callback.response(true);
              }
            });
            req.onError(new Request.ErrorCallback() {
              @Override
              public void error(Exception e) {
                callback.response(false);
                Log.e("setUserAttributes failed when specifying location with error: " +
                    e.getMessage());
              }
            });
            RequestSender.getInstance().send(req);
          }
        });
      }
    });
  }

  public static void recordAttributeChanges() {
    boolean madeChanges = false;
    for (Map<String, ?> attributes : userAttributeChanges) {
      Map<String, Object> existingAttributes = VarCache.userAttributes();
      if (existingAttributes == null) {
        existingAttributes = new HashMap<>();
      }
      for (String attributeName : attributes.keySet()) {
        Object existingValue = existingAttributes.get(attributeName);
        Object value = attributes.get(attributeName);
        if ((existingValue == null && value != null) ||
            (existingValue != null && !existingValue.equals(value))) {
          ActionContext.ContextualValues contextualValues = new ActionContext.ContextualValues();
          contextualValues.previousAttributeValue = existingValue;
          contextualValues.attributeValue = value;
          existingAttributes.put(attributeName, value);
          LeanplumInternal.maybePerformActions("userAttribute", attributeName,
              LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL, null, contextualValues);
          madeChanges = true;
        }
      }
    }
    userAttributeChanges.clear();
    if (madeChanges) {
      VarCache.saveUserAttributes();
    }
  }

  public static void moveToForeground() {
    synchronized (inForegroundLock) {
      if (inForeground) {
        return;
      }
      inForeground = true;
    }

    Leanplum.addStartResponseHandler(new StartCallback() {
      @Override
      public void onResponse(boolean success) {
        if (!success) {
          return;
        }
        maybePerformActions(new String[] {"start", "resume"}, null,
            LeanplumMessageMatchFilter.LEANPLUM_ACTION_FILTER_ALL, null, null);
        recordAttributeChanges();
      }
    });

    connectDevelopmentServer();
  }

  public static void connectDevelopmentServer() {
    Leanplum.addStartResponseHandler(new StartCallback() {
      @Override
      public void onResponse(boolean success) {
        if (!success) {
          return;
        }
        if (!inForeground) {
          return;
        }
        if (!MigrationManager.getState().useLeanplum()) {
          return;
        }
        if (Constants.isDevelopmentModeEnabled && !Constants.isNoop()) {
          Socket.connectSocket();
        }
      }
    });
  }

  public static void addStartIssuedHandler(Runnable handler) {
    if (handler == null) {
      Log.e("addStartIssuedHandler - Invalid handler parameter provided.");
      return;
    }

    synchronized (startIssuedHandlers) {
      if (!issuedStart) {
        startIssuedHandlers.add(handler);
        return;
      }
    }
    handler.run();
  }

  public static void triggerStartIssued() {
    synchronized (startIssuedHandlers) {
      LeanplumInternal.setIssuedStart(true);
      for (Runnable callback : startIssuedHandlers) {
        OperationQueue.sharedInstance().addUiOperation(callback);
      }
      startIssuedHandlers.clear();
    }
  }

  /**
   * Validates that the attributes are of the correct format. User attributes must be Strings,
   * Booleans, or Numbers.
   *
   * @param argName Argument name used for error messages.
   * @param allowLists Whether attribute values can be lists.
   */
  public static <T> Map<String, T> validateAttributes(Map<String, T> attributes, String argName,
      boolean allowLists) {
    if (attributes == null) {
      return null;
    }
    Map<String, T> validAttributes = new HashMap<>();
    try {
      for (Map.Entry<String, T> entry : attributes.entrySet()) {
        T value = entry.getValue();

        // Validate lists.
        if (allowLists && value instanceof Iterable<?>) {
          boolean valid = true;
          Iterable<Object> iterable = CollectionUtil.uncheckedCast(value);
          for (Object item : iterable) {
            if (!isValidScalarValue(item, argName)) {
              valid = false;
              break;
            }
          }
          if (!valid) {
            continue;
          }

          // Validate scalars.
        } else {
          if (value instanceof Date) {
            Date date = CollectionUtil.uncheckedCast(value);
            value = CollectionUtil.uncheckedCast(date.getTime());
          }
          if (value != null && !isValidScalarValue(value, argName)) {
            continue;
          }
        }
        validAttributes.put(entry.getKey(), value);
      }
    } catch (ConcurrentModificationException e) {
      maybeThrowException(new LeanplumException("ConcurrentModificationException: You cannot " +
          "modify Map<String, ?> attributes/parameters. Will override with an empty map"));
      validAttributes = new HashMap<>();
    }
    return validAttributes;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private static boolean isValidScalarValue(Object value, String argName) {
    if (!(value instanceof Number) && !(value instanceof String) && !(value instanceof Boolean)) {
      maybeThrowException(new LeanplumException(
          argName + " values must be of type String, Number, or Boolean."));
      return false;
    }
    return true;
  }

  /**
   * Throws a LeanplumException when in development mode. Otherwise, logs the exception to prevent
   * the app from crashing.
   */
  public static void maybeThrowException(RuntimeException e) {
    if (Constants.isDevelopmentModeEnabled) {
      throw e;
    } else {
      Log.e(e.getMessage() + " This error is only thrown in development mode.", e);
    }
  }

  /***
   * Getters & Setters
   ***/
  public static boolean hasStartedAndRegisteredAsDeveloper() {
    return hasStartedAndRegisteredAsDeveloper;
  }

  public static boolean issuedStart() {
    return issuedStart;
  }

  @SuppressWarnings("SameParameterValue")
  private static void setIssuedStart(boolean issuedStart) {
    LeanplumInternal.issuedStart = issuedStart;
  }

  public static boolean hasStarted() {
    return hasStarted;
  }

  public static void setHasStarted(boolean hasStarted) {
    LeanplumInternal.hasStarted = hasStarted;
  }

  public static boolean isStartSuccessful() {
    return startSuccessful;
  }

  public static void setStartSuccessful(boolean startSuccessful) {
    LeanplumInternal.startSuccessful = startSuccessful;
  }

  public static boolean hasCalledStart() {
    return calledStart;
  }

  public static void setCalledStart(boolean calledStart) {
    LeanplumInternal.calledStart = calledStart;
  }

  public static boolean isPaused() {
    return isPaused;
  }

  public static void setIsPaused(boolean isPaused) {
    LeanplumInternal.isPaused = isPaused;
  }

  public static boolean hasStartedInBackground() {
    return startedInBackground;
  }

  public static void setStartedInBackground(boolean startedInBackground) {
    LeanplumInternal.startedInBackground = startedInBackground;
  }

  public static Queue<Map<String, ?>> getUserAttributeChanges() {
    return userAttributeChanges;
  }

  public static boolean getIsScreenTrackingEnabled() {
    return isScreenTrackingEnabled;
  }

  public static boolean getIsVariantDebugInfoEnabled() {
    return isVariantDebugInfoEnabled;
  }

  public static void setIsVariantDebugInfoEnabled(boolean isVariantDebugInfoEnabled) {
    LeanplumInternal.isVariantDebugInfoEnabled = isVariantDebugInfoEnabled;
  }

  public static void enableAutomaticScreenTracking() {
    isScreenTrackingEnabled = true;
  }
}
