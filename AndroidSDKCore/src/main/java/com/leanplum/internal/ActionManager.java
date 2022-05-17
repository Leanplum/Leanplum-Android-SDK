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

import android.content.Context;
import android.content.SharedPreferences;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.leanplum.ActionContext.ContextualValues;
import com.leanplum.Leanplum;
import com.leanplum.LocationManager;
import com.leanplum.actions.Action;
import com.leanplum.actions.ActionManagerExecutionKt;
import com.leanplum.actions.ActionQueue;
import com.leanplum.actions.ActionScheduler;
import com.leanplum.actions.Definitions;
import com.leanplum.actions.MessageDisplayController;
import com.leanplum.actions.MessageDisplayListener;
import com.leanplum.internal.Constants.Defaults;
import com.leanplum.utils.SharedPreferencesUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles in-app and push messaging.
 *
 * @author Andrew First
 */
public class ActionManager {
  private static final long HOUR_MILLIS = 60 * 60 * 1000;
  private static final long DAY_MILLIS = 24 * HOUR_MILLIS;
  private static final long WEEK_MILLIS = 7 * DAY_MILLIS;

  private final Map<String, Map<String, Number>> messageImpressionOccurrences = new HashMap<>();
  private final Map<String, Number> messageTriggerOccurrences = new HashMap<>();
  private final Map<String, Number> sessionOccurrences = new HashMap<>();

  private MessageDisplayListener messageDisplayListener;
  private MessageDisplayController messageDisplayController;
  private final ActionScheduler scheduler = new ActionScheduler() {
    @Override
    public void schedule(@NonNull Action action, int delaySeconds) {
      OperationQueue.sharedInstance().addOperationAfterDelay(
          () -> ActionManagerExecutionKt.appendAction(ActionManager.this, action),
          delaySeconds * 1000L
      );
    }
  };

  private boolean enabled = true; // when manager is disabled it will stop adding actions in queue
  private boolean paused = true; // variable used when fetching chained action, paused until Activity is presented
  private final ActionQueue queue = new ActionQueue();
  private final ActionQueue delayedQueue = new ActionQueue();
  private final Definitions definitions = new Definitions();
  private Action currentAction;

  private static ActionManager instance;

  public static final String PUSH_NOTIFICATION_ACTION_NAME = "__Push Notification";
  public static final String HELD_BACK_ACTION_NAME = "__held_back";
  private static LocationManager locationManager;
  private static boolean loggedLocationManagerFailure = false;

  public static class MessageMatchResult {
    public boolean matchedTrigger;
    public boolean matchedUnlessTrigger;
    public boolean matchedLimit;
    public boolean matchedActivePeriod;
  }

  public static synchronized ActionManager getInstance() {
    if (instance == null) {
      instance = new ActionManager();
    }
    return instance;
  }

  public static synchronized LocationManager getLocationManager() {
    if (locationManager != null) {
      return locationManager;
    }

    if (Util.hasPlayServices()) {
      try {
        // Reflection here prevents linker errors
        // if Google Play Services is not used in the client app.
        locationManager = (LocationManager) Class
            .forName("com.leanplum.LocationManagerImplementation")
            .getMethod("instance").invoke(null);
        return locationManager;
      } catch (Throwable t) {
        if (!loggedLocationManagerFailure) {
          Log.i("Geofencing support requires leanplum-location module and Google Play " +
              "Services v8.1 and higher.\n" +
              "Add this to your build.gradle file:\n" +
              "implementation 'com.google.android.gms:play-services-location:8.3.0+'\n" +
              "implementation 'com.leanplum:leanplum-location:+'");
          loggedLocationManagerFailure = true;
        }
      }
    }
    return null;
  }

  private ActionManager() {
  }

  public Map<String, Number> getMessageImpressionOccurrences(String messageId) {
    Map<String, Number> occurrences = messageImpressionOccurrences.get(messageId);
    if (occurrences != null) {
      return occurrences;
    }
    Context context = Leanplum.getContext();
    SharedPreferences preferences = context.getSharedPreferences(
        Defaults.MESSAGING_PREF_NAME, Context.MODE_PRIVATE);
    String savedValue = preferences.getString(
        String.format(Constants.Defaults.MESSAGE_IMPRESSION_OCCURRENCES_KEY, messageId),
        "{}");
    occurrences = CollectionUtil.uncheckedCast(JsonConverter.fromJson(savedValue));
    messageImpressionOccurrences.put(messageId, occurrences);
    return occurrences;
  }

  public void saveMessageImpressionOccurrences(Map<String, Number> occurrences, String messageId) {
    Context context = Leanplum.getContext();
    SharedPreferences preferences = context.getSharedPreferences(
        Defaults.MESSAGING_PREF_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putString(
        String.format(Constants.Defaults.MESSAGE_IMPRESSION_OCCURRENCES_KEY, messageId),
        JsonConverter.toJson(occurrences));
    messageImpressionOccurrences.put(messageId, occurrences);
    SharedPreferencesUtil.commitChanges(editor);
  }

  public int getMessageTriggerOccurrences(String messageId) {
    Number occurrences = messageTriggerOccurrences.get(messageId);
    if (occurrences != null) {
      return occurrences.intValue();
    }
    Context context = Leanplum.getContext();
    SharedPreferences preferences = context.getSharedPreferences(
        Defaults.MESSAGING_PREF_NAME, Context.MODE_PRIVATE);
    int savedValue = preferences.getInt(
        String.format(Constants.Defaults.MESSAGE_TRIGGER_OCCURRENCES_KEY, messageId), 0);
    messageTriggerOccurrences.put(messageId, savedValue);
    return savedValue;
  }

  public void saveMessageTriggerOccurrences(int occurrences, String messageId) {
    Context context = Leanplum.getContext();
    SharedPreferences preferences = context.getSharedPreferences(
        Defaults.MESSAGING_PREF_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putInt(
        String.format(Constants.Defaults.MESSAGE_TRIGGER_OCCURRENCES_KEY, messageId), occurrences);
    messageTriggerOccurrences.put(messageId, occurrences);
    SharedPreferencesUtil.commitChanges(editor);
  }

  public MessageMatchResult shouldShowMessage(String messageId, Map<String, Object> messageConfig,
      String when, String eventName, ContextualValues contextualValues) {
    MessageMatchResult result = new MessageMatchResult();

    // 1. Must not be muted.
    Context context = Leanplum.getContext();
    SharedPreferences preferences = context.getSharedPreferences(
        Defaults.MESSAGING_PREF_NAME, Context.MODE_PRIVATE);
    if (preferences.getBoolean(
        String.format(Constants.Defaults.MESSAGE_MUTED_KEY, messageId), false)) {
      return result;
    }

    // 2. Must match at least one trigger.
    result.matchedTrigger = matchedTriggers(messageConfig.get("whenTriggers"), when, eventName,
        contextualValues);
    result.matchedUnlessTrigger =
        matchedTriggers(messageConfig.get("unlessTriggers"), when, eventName, contextualValues);
    if (!result.matchedTrigger && !result.matchedUnlessTrigger) {
      return result;
    }

    // 3. Must match all limit conditions.
    Object limitConfigObj = messageConfig.get("whenLimits");
    Map<String, Object> limitConfig = null;
    if (limitConfigObj instanceof Map<?, ?>) {
      limitConfig = CollectionUtil.uncheckedCast(limitConfigObj);
    }
    result.matchedLimit = matchesLimits(messageId, limitConfig);

    // 4. Must be within active period.
    Object messageStartTime = messageConfig.get("startTime");
    Object messageEndTime = messageConfig.get("endTime");
    if (messageStartTime == null || messageEndTime == null) {
      result.matchedActivePeriod = true;
    } else {
      long currentTime = Clock.getInstance().newDate().getTime();
      result.matchedActivePeriod = currentTime >= (long) messageStartTime &&
          currentTime <= (long) messageEndTime;
    }

    return result;
  }

  private boolean matchesLimits(String messageId, Map<String, Object> limitConfig) {
    if (limitConfig == null) {
      return true;
    }
    List<Object> limits = CollectionUtil.uncheckedCast(limitConfig.get("children"));
    if (limits.isEmpty()) {
      return true;
    }
    Map<String, Number> impressionOccurrences = getMessageImpressionOccurrences(messageId);
    int triggerOccurrences = getMessageTriggerOccurrences(messageId) + 1;
    for (Object limitObj : limits) {
      Map<String, Object> limit = CollectionUtil.uncheckedCast(limitObj);
      String subject = limit.get("subject").toString();
      String noun = limit.get("noun").toString();
      String verb = limit.get("verb").toString();

      // E.g. 5 times per session; 2 times per 7 minutes.
      if (subject.equals("times")) {
        List<Object> objects = CollectionUtil.uncheckedCast(limit.get("objects"));
        int perTimeUnit = objects.size() > 0 ?
            Integer.parseInt(objects.get(0).toString()) : 0;
        if (!matchesLimitTimes(Integer.parseInt(noun),
            perTimeUnit, verb, impressionOccurrences, messageId)) {
          return false;
        }

        // E.g. On the 5th occurrence.
      } else if (subject.equals("onNthOccurrence")) {
        int amount = Integer.parseInt(noun);
        if (triggerOccurrences != amount) {
          return false;
        }

        // E.g. Every 5th occurrence.
      } else if (subject.equals("everyNthOccurrence")) {
        int multiple = Integer.parseInt(noun);
        if (multiple == 0 || triggerOccurrences % multiple != 0) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean matchesLimitTimes(int amount, int time, String units,
      Map<String, Number> occurrences, String messageId) {
    Number existing = 0L;
    if (units.equals("limitSession")) {
      existing = sessionOccurrences.get(messageId);
      if (existing == null) {
        existing = 0L;
      }
    } else {
      if (occurrences == null || occurrences.isEmpty()) {
        return true;
      }
      Number min = occurrences.get("min");
      Number max = occurrences.get("max");
      if (min == null) {
        min = 0L;
      }
      if (max == null) {
        max = 0L;
      }
      if (units.equals("limitUser")) {
        existing = max.longValue() - min.longValue() + 1;
      } else {
        if (units.equals("limitMinute")) {
          time *= 60;
        } else if (units.equals("limitHour")) {
          time *= 3600;
        } else if (units.equals("limitDay")) {
          time *= 86400;
        } else if (units.equals("limitWeek")) {
          time *= 604800;
        } else if (units.equals("limitMonth")) {
          time *= 2592000;
        }
        long now = Clock.getInstance().currentTimeMillis();
        int matchedOccurrences = 0;
        for (long i = max.longValue(); i >= min.longValue(); i--) {
          if (occurrences.containsKey("" + i)) {
            long timeAgo = (now - occurrences.get("" + i).longValue()) / 1000;
            if (timeAgo > time) {
              break;
            }
            matchedOccurrences++;
            if (matchedOccurrences >= amount) {
              return false;
            }
          }
        }
      }
    }
    return existing.longValue() < amount;
  }

  private boolean matchedTriggers(Object triggerConfigObj, String when, String eventName,
      ContextualValues contextualValues) {
    if (triggerConfigObj instanceof Map<?, ?>) {
      Map<String, Object> triggerConfig = CollectionUtil.uncheckedCast(triggerConfigObj);
      List<Object> triggers = CollectionUtil.uncheckedCast(triggerConfig.get("children"));
      for (Object triggerObj : triggers) {
        Map<String, Object> trigger = CollectionUtil.uncheckedCast(triggerObj);
        if (matchedTrigger(trigger, when, eventName, contextualValues)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean matchedTrigger(Map<String, Object> trigger, String when, String eventName,
      ContextualValues contextualValues) {
    String subject = (String) trigger.get("subject");
    if (subject.equals(when)) {
      String noun = (String) trigger.get("noun");
      if ((noun == null && eventName == null) || (noun != null && noun.equals(eventName))) {
        String verb = (String) trigger.get("verb");
        List<Object> objects = CollectionUtil.uncheckedCast(trigger.get("objects"));

        // Evaluate user attribute changed to value.
        if ("changesTo".equals(verb)) {
          if (contextualValues != null && objects != null) {
            for (Object object : objects) {
              if ((object == null && contextualValues.attributeValue == null) ||
                  (object != null && object.toString().equalsIgnoreCase(
                      contextualValues.attributeValue.toString()))) {
                return true;
              }
            }
          }
          return false;
        }

        // Evaluate user attribute changed from value to value.
        if ("changesFromTo".equals(verb)) {
          return contextualValues != null &&
              objects.size() == 2 && objects.get(0) != null && objects.get(1) != null &&
              contextualValues.previousAttributeValue != null &&
              contextualValues.attributeValue != null &&
              objects.get(0).toString().equalsIgnoreCase(
                  contextualValues.previousAttributeValue.toString()) &&
              objects.get(1).toString().equalsIgnoreCase(
                  contextualValues.attributeValue.toString());
        }

        // Evaluate event parameter is value.
        if ("triggersWithParameter".equals(verb)) {
          if (contextualValues != null &&
              objects.size() == 2 && objects.get(0) != null && objects.get(1) != null &&
              contextualValues.parameters != null) {
            Object parameterValue = contextualValues.parameters.get(objects.get(0));
            return parameterValue != null && parameterValue.toString().equalsIgnoreCase(
                objects.get(1).toString());
          }
          return false;
        }

        return true;
      }
    }
    return false;
  }

  public void recordMessageTrigger(String messageId) {
    int occurrences = getMessageTriggerOccurrences(messageId);
    occurrences++;
    saveMessageTriggerOccurrences(occurrences, messageId);
  }

  /**
   * Tracks the "Held Back" event for a message and records the held back occurrences.
   *
   * @param messageId The spoofed ID of the message.
   * @param originalMessageId The original ID of the held back message.
   */
  public void recordHeldBackImpression(String messageId, String originalMessageId) {
    trackHeldBackEvent(originalMessageId);
    recordImpression(messageId);
  }

  /**
   * Tracks the "Open" event for a message and records it's occurrence.
   *
   * @param messageId The ID of the message
   */
  public void recordMessageImpression(String messageId) {
    trackImpressionEvent(messageId);
    recordImpression(messageId);
  }

  /**
   * Tracks the "Open" event for an action.
   *
   * @param messageId The ID of the action
   */
  public void recordChainedActionImpression(String messageId) {
    trackImpressionEvent(messageId);
  }

  /**
   * Tracks the event for local push notification.
   * Do not want to track impression occurrence in such case.
   *
   * @param messageId The ID of the action
   */
  public void recordLocalPushImpression(String messageId) {
    trackImpressionEvent(messageId);
  }

  /**
   * Tracks the correct held back event.
   *
   * @param originalMessageId The original message ID of the held back message.
   */
  private void trackHeldBackEvent(String originalMessageId) {
    Map<String, String> requestArgs = new HashMap<>();
    requestArgs.put(Constants.Params.MESSAGE_ID, originalMessageId);
    LeanplumInternal.track(Constants.HELD_BACK_EVENT_NAME, 0.0, null, null, requestArgs);
  }

  private void trackImpressionEvent(String messageId) {
    Map<String, String> requestArgs = new HashMap<>();
    requestArgs.put(Constants.Params.MESSAGE_ID, messageId);
    LeanplumInternal.track(null, 0.0, null, null, requestArgs);
  }

  /**
   * Records the occurrence of a message.
   *
   * @param messageId The ID of the message.
   */
  private void recordImpression(String messageId) {
    // Record session occurrences.
    Number existing = sessionOccurrences.get(messageId);
    if (existing == null) {
      existing = 0L;
    }
    existing = existing.longValue() + 1L;
    sessionOccurrences.put(messageId, existing);

    // Record cross-session occurrences.
    Map<String, Number> occurrences = getMessageImpressionOccurrences(messageId);
    if (occurrences == null || occurrences.isEmpty()) {
      occurrences = new HashMap<>();
      occurrences.put("min", 0L);
      occurrences.put("max", 0L);
      occurrences.put("0", Clock.getInstance().currentTimeMillis());
    } else {
      Number min = occurrences.get("min");
      Number max = occurrences.get("max");
      if (min == null) {
        min = 0L;
      }
      if (max == null) {
        max = 0L;
      }
      max = max.longValue() + 1L;
      occurrences.put("" + max, Clock.getInstance().currentTimeMillis());
      if (max.longValue() - min.longValue() + 1 >
          Constants.Messaging.MAX_STORED_OCCURRENCES_PER_MESSAGE) {
        occurrences.remove("" + min);
        min = min.longValue() + 1L;
        occurrences.put("min", min);
      }
      occurrences.put("max", max);
    }
    saveMessageImpressionOccurrences(occurrences, messageId);
  }

  public void muteFutureMessagesOfKind(String messageId) {
    if (messageId != null) {
      Context context = Leanplum.getContext();
      SharedPreferences preferences = context.getSharedPreferences(
          Defaults.MESSAGING_PREF_NAME, Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = preferences.edit();
      editor.putBoolean(
          String.format(Constants.Defaults.MESSAGE_MUTED_KEY, messageId),
          true);
      SharedPreferencesUtil.commitChanges(editor);
    }
  }


  public static void getForegroundandBackgroundRegionNames(Set<String> foregroundRegionNames,
      Set<String> backgroundRegionNames) {
    Map<String, Object> messages = VarCache.messages();
    for (String messageId : messages.keySet()) {
      Map<String, Object> messageConfig = CollectionUtil.uncheckedCast(messages.get(messageId));
      Set<String> regionNames;
      Object action = messageConfig.get("action");
      if (action instanceof String) {
        if (action.equals(PUSH_NOTIFICATION_ACTION_NAME)) {
          regionNames = backgroundRegionNames;
        } else {
          regionNames = foregroundRegionNames;
        }

        Map<String, Object> whenTriggers = CollectionUtil.uncheckedCast(messageConfig.get
            ("whenTriggers"));
        Map<String, Object> unlessTriggers = CollectionUtil.uncheckedCast(messageConfig.get
            ("unlessTriggers"));

        addRegionNamesFromTriggersToSet(whenTriggers, regionNames);
        addRegionNamesFromTriggersToSet(unlessTriggers, regionNames);
      }
    }
  }

  public static void addRegionNamesFromTriggersToSet(
      Map<String, Object> triggerConfig, Set<String> set) {
    if (triggerConfig == null) {
      return;
    }
    List<Map<String, Object>> triggers = CollectionUtil.uncheckedCast(triggerConfig.get
        ("children"));
    for (Map<String, Object> trigger : triggers) {
      String subject = (String) trigger.get("subject");
      if (subject.equals("enterRegion") || subject.equals("exitRegion")) {
        set.add((String) trigger.get("noun"));
      }
    }
  }

  /**
   * Checks if message occurrences have reached limits coming from local IAM caps data.
   *
   * @return True to suppress messages, false otherwise.
   */
  public boolean shouldSuppressMessages() {
    int dayLimit = 0;
    int weekLimit = 0;
    int sessionLimit = 0;

    for (Map<String, Object> cap : VarCache.localCaps()) {
      if (!"IN_APP".equals(cap.get("channel"))) {
        continue;
      }
      String type = (String) cap.get("type");
      Integer limit = (Integer) cap.get("limit");
      if (limit == null) {
        continue;
      }

      if ("DAY".equals(type)) {
        dayLimit = limit;
      } else if ("WEEK".equals(type)) {
        weekLimit = limit;
      } else if ("SESSION".equals(type)) {
        sessionLimit = limit;
      }
    }

    return (weekLimit > 0 && weeklyOccurrencesCount() >= weekLimit)
        || (dayLimit > 0 && dailyOccurrencesCount() >= dayLimit)
        || (sessionLimit > 0 && sessionOccurrencesCount() >= sessionLimit);
  }

  @VisibleForTesting
  int dailyOccurrencesCount() {
    long endTime = Clock.getInstance().currentTimeMillis();
    long startTime = endTime - DAY_MILLIS;
    return countOccurrences(startTime, endTime);
  }

  @VisibleForTesting
  int weeklyOccurrencesCount() {
    long endTime = Clock.getInstance().currentTimeMillis();
    long startTime = endTime - WEEK_MILLIS;
    return countOccurrences(startTime, endTime);
  }

  private int countOccurrences(long startTime, long endTime) {
    String prefix = String.format(Constants.Defaults.MESSAGE_IMPRESSION_OCCURRENCES_KEY, "");

    Context context = Leanplum.getContext();
    SharedPreferences prefs =
        context.getSharedPreferences(Defaults.MESSAGING_PREF_NAME, Context.MODE_PRIVATE);
    Map<String, ?> all = prefs.getAll();

    int occurrenceCount = 0;
    for (Map.Entry<String, ?> entry : all.entrySet()) {
      if (entry.getKey().startsWith(prefix)) {
        String json = (String) entry.getValue();
        if (!TextUtils.isEmpty(json) && !json.equals("{}")) {
          occurrenceCount += countOccurrences(startTime, endTime, json);
        }
      }
    }

    return occurrenceCount;
  }

  private int countOccurrences(long startTime, long endTime, String json) {
    Map<String, Number> occurrences = CollectionUtil.uncheckedCast(JsonConverter.fromJson(json));
    Number min = occurrences.get("min");
    Number max = occurrences.get("max");

    if (min == null || max == null) {
      return 0;
    }

    long minId = min.longValue();
    long maxId = max.longValue();
    int count = 0;

    for (long id = maxId; id >= minId; id--) {
      Number time = occurrences.get("" + id);
      if (time != null) {
        if (startTime <= time.longValue() && time.longValue() <= endTime) {
          count++;
        } else {
          // occurrences with smaller ids would fall out of time interval
          return count;
        }
      }
    }

    return count;
  }

  @VisibleForTesting
  int sessionOccurrencesCount() {
    int count = 0;
    for (Map.Entry<String, Number> entry : sessionOccurrences.entrySet()) {
      Number value = entry.getValue();
      if (value != null) {
        count += value.intValue();
      }
    }
    return count;
  }

  /**
   * Use method to disable queue. That would stop queue from receiving new actions.
   *
   * @param value True to enable adding actions to queue and false otherwise.
   */
  public void setEnabled(boolean value) {
    Log.i("[ActionManager] isEnabled: " + value);
    enabled = value;
  }

  /**
   * @return True if queue is able to add elements, false otherwise.
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Use method to pause queue.
   * Pausing the queue means stopping execution of actions until queue is resumed.
   *
   * @param value True to pause queue, false otherwise.
   */
  public void setPaused(boolean value) {
    Log.i("[ActionManager] isPaused: " + value);
    paused = value;
    if (!paused) {
      ActionManagerExecutionKt.performActions(ActionManager.getInstance());
    }
  }

  /**
   * @return True if queue is in resume state, false otherwise.
   */
  public boolean isPaused() {
    return paused;
  }


  /**
   * Current action is set by the queue when it is popped.
   */
  public void setCurrentAction(Action action) {
    if (action == null) {
      String name = currentAction != null ? currentAction.getContext().actionName() : null;
      Log.e("Clear currentAction from name=" + name); // TODO remove log before releasing
    } else {
      Log.e("Assign currentAction name=" + action.getContext().actionName()); // TODO remove log before releasing
    }
    this.currentAction = action;
  }

  /**
   * Returns currently executing action in the queue.
   */
  public Action getCurrentAction() {
    return this.currentAction;
  }

  public MessageDisplayListener getMessageDisplayListener() {
    return messageDisplayListener;
  }

  public void setMessageDisplayListener(MessageDisplayListener messageDisplayListener) {
    this.messageDisplayListener = messageDisplayListener;
  }

  public MessageDisplayController getMessageDisplayController() {
    return messageDisplayController;
  }

  public void setMessageDisplayController(MessageDisplayController messageDisplayController) {
    this.messageDisplayController = messageDisplayController;
  }

  public ActionScheduler getScheduler() {
    return scheduler;
  }

  public ActionQueue getQueue() {
    return queue;
  }

  public ActionQueue getDelayedQueue() {
    return delayedQueue;
  }

  public Definitions getDefinitions() {
    return definitions;
  }
}
