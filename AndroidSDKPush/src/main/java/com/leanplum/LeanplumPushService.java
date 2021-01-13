/*
 * Copyright 2020, Leanplum, Inc. All rights reserved.
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import com.leanplum.callbacks.VariablesChangedCallback;
import com.leanplum.internal.ActionManager;
import com.leanplum.internal.Constants;
import com.leanplum.internal.Constants.Keys;
import com.leanplum.internal.Constants.Params;
import com.leanplum.internal.JsonConverter;
import com.leanplum.internal.LeanplumInternal;
import com.leanplum.internal.Log;
import com.leanplum.internal.Request.RequestType;
import com.leanplum.internal.RequestBuilder;
import com.leanplum.internal.Request;
import com.leanplum.internal.RequestSender;
import com.leanplum.internal.Util;
import com.leanplum.internal.VarCache;
import com.leanplum.utils.BuildUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.core.app.NotificationCompat;

/**
 * Leanplum push notification service class, handling initialization, opening, showing, integration
 * verification and registration for push notifications.
 *
 * @author Andrew First, Anna Orlova
 */
public class LeanplumPushService {
  /**
   * Leanplum's built-in Google Cloud Messaging sender ID.
   */
  public static final String LEANPLUM_SENDER_ID = "44059457771";
  /**
   * Intent action used when broadcast is received in custom BroadcastReceiver.
   */
  public static final String LEANPLUM_NOTIFICATION = "LP_NOTIFICATION";
  /**
   * Action param key contained when Notification Bundle is parsed with {@link
   * LeanplumPushService#parseNotificationBundle(Bundle)}.
   */
  public static final String LEANPLUM_ACTION_PARAM = "lp_action_param";
  /**
   * Message title param key contained when Notification Bundle is parsed with {@link
   * LeanplumPushService#parseNotificationBundle(Bundle)}.
   */
  public static final String LEANPLUM_MESSAGE_PARAM = "lp_message_param";
  /**
   * Message id param key contained when Notification Bundle is parsed with {@link
   * LeanplumPushService#parseNotificationBundle(Bundle)}.
   */
  public static final String LEANPLUM_MESSAGE_ID = "lp_message_id";

  private static final int NOTIFICATION_ID = 1;
  private static final String OPEN_URL = "Open URL";
  private static final String URL = "URL";
  private static final String OPEN_ACTION = "Open";
  private static Class<? extends Activity> callbackClass;
  private static final PushProviders pushProviders = new PushProviders();
  private static LeanplumPushNotificationCustomizer customizer;
  private static boolean useNotificationBuilderCustomizer = false;

  /**
   * Get Cloud Messaging provider. By default - FCM.
   *
   * @return LeanplumCloudMessagingProvider - current provider
   */
  @NonNull
  static PushProviders getPushProviders() {
    return pushProviders;
  }

  /**
   * Changes the default activity to launch if the user opens a push notification.
   *
   * @param callbackClass The activity class.
   */
  public static void setDefaultCallbackClass(Class<? extends Activity> callbackClass) {
    LeanplumPushService.callbackClass = callbackClass;
  }

  /**
   * Sets an object used to customize the appearance of notifications. Call this from your
   * Application class's onCreate method so that the customizer is set when your application starts
   * in the background.
   *
   * @param customizer LeanplumPushNotificationCustomizer push notification customizer.
   */
  public static void setCustomizer(LeanplumPushNotificationCustomizer customizer) {
    setCustomizer(customizer, false);
  }

  /**
   * Sets an object used to customize the appearance of notifications. Call this from your
   * Application class's onCreate method so that the customizer is set when your application starts
   * in the background.
   *
   * @param customizer LeanplumPushNotificationCustomizer push notification customizer.
   * @param useNotificationBuilderCustomizer True if if you want to support 2 lines of text on
   * BigPicture style push notification.
   */
  public static void setCustomizer(LeanplumPushNotificationCustomizer customizer,
      boolean useNotificationBuilderCustomizer) {
    LeanplumPushService.customizer = customizer;
    LeanplumPushService.useNotificationBuilderCustomizer = useNotificationBuilderCustomizer;
  }


  private static Class<? extends Activity> getCallbackClass() {
    return callbackClass;
  }

  private static boolean areActionsEmbedded(final Bundle message) {
    return message.containsKey(Keys.PUSH_MESSAGE_ACTION);
  }

  private static void requireMessageContent(
      final String messageId, final VariablesChangedCallback onComplete) {
    Leanplum.addOnceVariablesChangedAndNoDownloadsPendingHandler(new VariablesChangedCallback() {
      @Override
      public void variablesChanged() {
        try {
          Map<String, Object> messages = VarCache.messages();
          if (messageId == null || (messages != null && messages.containsKey(messageId))) {
            onComplete.variablesChanged();
          } else {
            // Try downloading the messages again if it doesn't exist.
            // Maybe the message was created while the app was running.
            Request req = RequestBuilder
                .withGetVarsAction()
                .andParam(Params.INCLUDE_DEFAULTS, Boolean.toString(false))
                .andParam(Params.INCLUDE_MESSAGE_ID, messageId)
                .andType(RequestType.IMMEDIATE)
                .create();
            req.onResponse(new Request.ResponseCallback() {
              @Override
              public void response(JSONObject response) {
                try {
                  if (response == null) {
                    Log.e("No response received from the server. Please contact us to " +
                        "investigate.");
                  } else {
                    Map<String, Object> values = JsonConverter.mapFromJson(
                        response.optJSONObject(Constants.Keys.VARS));
                    Map<String, Object> messages = JsonConverter.mapFromJson(
                        response.optJSONObject(Constants.Keys.MESSAGES));
                    Map<String, Object> regions = JsonConverter.mapFromJson(
                        response.optJSONObject(Constants.Keys.REGIONS));
                    List<Map<String, Object>> variants = JsonConverter.listFromJson(
                        response.optJSONArray(Constants.Keys.VARIANTS));
                    if (!Constants.canDownloadContentMidSessionInProduction ||
                        VarCache.getDiffs().equals(values)) {
                      values = null;
                    }
                    if (VarCache.getMessageDiffs().equals(messages)) {
                      messages = null;
                    }
                    if (values != null || messages != null) {
                      VarCache.applyVariableDiffs(values, messages, regions, variants, null);
                    }
                  }
                  onComplete.variablesChanged();
                } catch (Throwable t) {
                  Log.exception(t);
                }
              }
            });
            req.onError(new Request.ErrorCallback() {
              @Override
              public void error(Exception e) {
                onComplete.variablesChanged();
              }
            });
            RequestSender.getInstance().send(req);
          }
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
    });
  }

  private static String getMessageId(Bundle message) {
    String messageId = message.getString(Keys.PUSH_MESSAGE_ID_NO_MUTE_WITH_ACTION);
    if (messageId == null) {
      messageId = message.getString(Keys.PUSH_MESSAGE_ID_MUTE_WITH_ACTION);
      if (messageId == null) {
        messageId = message.getString(Keys.PUSH_MESSAGE_ID_NO_MUTE);
        if (messageId == null) {
          messageId = message.getString(Keys.PUSH_MESSAGE_ID_MUTE);
        }
      }
    }
    if (messageId != null) {
      message.putString(Keys.PUSH_MESSAGE_ID, messageId);
    }
    return messageId;
  }

  static void handleNotification(final Context context, final Bundle message) {
    Map<String,String> properties = new HashMap<String, String>();
    properties.put("messageID",getMessageId(message));
    Leanplum.track("Push Delivered", properties);
    if (LeanplumActivityHelper.getCurrentActivity() != null
        && !LeanplumActivityHelper.isActivityPaused
        && (message.containsKey(Keys.PUSH_MESSAGE_ID_MUTE_WITH_ACTION)
        || message.containsKey(Keys.PUSH_MESSAGE_ID_MUTE))) {
      // Mute notifications that have "Mute inside app" set if the app is open.
      return;
    }

    final String messageId = LeanplumPushService.getMessageId(message);
    if (messageId == null || !LeanplumInternal.hasCalledStart()) {
      showNotification(context, message);
      return;
    }

    // Can only track displays if we call Leanplum.start explicitly above where it says
    // if (!Leanplum.calledStart). However, this is probably not worth it.
    //
    // Map<String, String> requestArgs = new HashMap<String, String>();
    // requestArgs.put(Constants.Params.MESSAGE_ID, getMessageId);
    // Leanplum.track("Displayed", 0.0, null, null, requestArgs);

    showNotification(context, message);
  }

  /**
   * Put the message into a notification and post it.
   */
  @TargetApi(16)
  private static void showNotification(Context context, final Bundle message) {
    if (context == null || message == null) {
      return;
    }

    int defaultIconId = 0;
    if (!LeanplumNotificationHelper.isApplicationIconValid(context)) {
      defaultIconId = LeanplumNotificationHelper.getDefaultPushNotificationIconResourceId(context);
    }

    final NotificationManager notificationManager = (NotificationManager)
        context.getSystemService(Context.NOTIFICATION_SERVICE);

    if (notificationManager == null) {
      return;
    }

    Intent intent = new Intent(context, LeanplumPushReceiver.class);
    intent.addCategory("lpAction");
    intent.putExtras(message);
    PendingIntent contentIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
        new Random().nextInt(), intent, 0);

    String title = Util.getApplicationName(context.getApplicationContext());
    if (message.getString("title") != null) {
      title = message.getString("title");
    }
    NotificationCompat.Builder notificationCompatBuilder = null;
    Notification.Builder notificationBuilder = null;

    final String messageText = message.getString(Keys.PUSH_MESSAGE_TEXT);
    Bitmap bigPicture = LeanplumNotificationHelper.getBigPictureBitmap(context,
        message.getString(Keys.PUSH_MESSAGE_IMAGE_URL));

    if (customizer != null && !useNotificationBuilderCustomizer) {
      notificationCompatBuilder = LeanplumNotificationHelper.getNotificationCompatBuilder(context,
          message, contentIntent, title, messageText, bigPicture, defaultIconId);
    } else {
      notificationBuilder = LeanplumNotificationHelper.getNotificationBuilder(context, message,
          contentIntent, title, messageText, defaultIconId);
    }

    if ((notificationCompatBuilder == null && notificationBuilder == null) ||
        (customizer != null & !useNotificationBuilderCustomizer &&
            notificationCompatBuilder == null) ||
        (customizer != null && useNotificationBuilderCustomizer && notificationBuilder == null) ||
        (customizer == null && notificationBuilder == null)) {
      return;
    }

    if (customizer != null) {
      try {
        if (useNotificationBuilderCustomizer) {
          if (bigPicture != null) {
            Notification.BigPictureStyle bigPictureStyle =
                LeanplumNotificationHelper.getBigPictureStyle(message, bigPicture, title,
                    messageText);
            customizer.customize(notificationBuilder, message, bigPictureStyle);
            LeanplumNotificationHelper.setModifiedBigPictureStyle(notificationBuilder,
                bigPictureStyle);
          } else {
            customizer.customize(notificationBuilder, message, null);
          }
        } else {
          customizer.customize(notificationCompatBuilder, message);
        }
      } catch (Throwable t) {
        Log.e("Unable to customize push notification: %s", Log.getStackTraceString(t));
        return;
      }
    } else {
      if (bigPicture != null) {
        Notification.BigPictureStyle bigPictureStyle =
            LeanplumNotificationHelper.getBigPictureStyle(message, bigPicture, title, messageText);
        LeanplumNotificationHelper.setModifiedBigPictureStyle(notificationBuilder, bigPictureStyle);
      }
    }

    int notificationId = LeanplumPushService.NOTIFICATION_ID;
    Object notificationIdObject = message.get("lp_notificationId");
    if (notificationIdObject instanceof Number) {
      notificationId = ((Number) notificationIdObject).intValue();
    } else if (notificationIdObject instanceof String) {
      try {
        notificationId = Integer.parseInt((String) notificationIdObject);
      } catch (NumberFormatException e) {
        notificationId = LeanplumPushService.NOTIFICATION_ID;
      }
    } else if (message.containsKey(Keys.PUSH_MESSAGE_ID)) {
      String value = message.getString(Keys.PUSH_MESSAGE_ID);
      if (value != null) {
        notificationId = value.hashCode();
      }
    }

    try {
      // Check if we have a chained message, and if it exists in var cache.
      if (ActionContext.shouldForceContentUpdateForChainedMessage(
          JsonConverter.fromJson(message.getString(Keys.PUSH_MESSAGE_ACTION)))) {

        // try to fetch referenced chained message and wait a bit for result
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Leanplum.forceContentUpdate(new VariablesChangedCallback() {
          @Override
          public void variablesChanged() {
            countDownLatch.countDown();
          }
        });

        // continue after 3 seconds and post the notification
        countDownLatch.await(3, TimeUnit.SECONDS);
      }

      // always post notification
      if (notificationBuilder != null) {
        notificationManager.notify(notificationId, notificationBuilder.build());
      } else {
        notificationManager.notify(notificationId, notificationCompatBuilder.build());
      }

    } catch (NullPointerException e) {
      Log.e("Unable to show push notification.", e);
    } catch (Throwable t) {
      Log.e("Unable to show push notification.", t);
      Log.exception(t);
    }
  }

  static void openNotification(Context context, Intent intent) {
    Log.d("Opening push notification action.");
    Map<String,String> properties = new HashMap<String, String>();
    properties.put("messageID",getMessageId(intent.getExtras()));
    Leanplum.track("Push Opened", properties);
    // Pre handles push notification.
    Bundle notification = preHandlePushNotification(context, intent);
    if (notification == null) {
      return;
    }

    // Checks if open action is "Open URL" and there is some activity that can handle intent.
    if (isActivityWithIntentStarted(context, notification)) {
      return;
    }

    // Start activity.
    Class<? extends Activity> callbackClass = LeanplumPushService.getCallbackClass();
    boolean shouldStartActivity = true;
    Activity currentActivity = LeanplumActivityHelper.getCurrentActivity();
    if (currentActivity != null && !LeanplumActivityHelper.isActivityPaused) {
      if (callbackClass == null) {
        shouldStartActivity = false;
      } else if (callbackClass.isInstance(currentActivity)) {
        shouldStartActivity = false;
      }
    }

    if (shouldStartActivity) {
      Intent actionIntent = getActionIntent(context);
      if (actionIntent == null) {
        return;
      }
      actionIntent.putExtras(notification);
      actionIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(actionIntent);
    }
    // Post handles push notification.
    postHandlePushNotification(context, intent);
  }

  /**
   * Parse notification bundle. Use this method to get parsed bundle to decide next step. Parsed
   * data will contain {@link LeanplumPushService#LEANPLUM_ACTION_PARAM}, {@link
   * LeanplumPushService#LEANPLUM_MESSAGE_PARAM} and {@link LeanplumPushService#LEANPLUM_MESSAGE_ID}
   *
   * @param notificationBundle Bundle to be parsed.
   * @return Map containing Actions, Message title and Message Id.
   */
  public static Map<String, Object> parseNotificationBundle(Bundle notificationBundle) {
    try {
      String notificationActions = notificationBundle.getString(Keys.PUSH_MESSAGE_ACTION);
      String notificationMessage = notificationBundle.getString(Keys.PUSH_MESSAGE_TEXT);
      String notificationMessageId = LeanplumPushService.getMessageId(notificationBundle);

      Map<String, Object> arguments = new HashMap<>();
      arguments.put(LEANPLUM_ACTION_PARAM, JsonConverter.fromJson(notificationActions));
      arguments.put(LEANPLUM_MESSAGE_PARAM, notificationMessage);
      arguments.put(LEANPLUM_MESSAGE_ID, notificationMessageId);

      return arguments;
    } catch (Throwable ignored) {
      Log.d("Failed to parse notification bundle.");
    }
    return null;
  }

  /**
   * Must be called before deciding which activity will be opened, to allow Leanplum SDK to track
   * stats, open events etc.
   *
   * @param context Surrounding context.
   * @param intent Received Intent.
   * @return Bundle containing push notification data.
   */
  public static Bundle preHandlePushNotification(Context context, Intent intent) {
    if (intent == null) {
      Log.d("Unable to pre handle push notification, Intent is null.");
      return null;
    }
    Bundle notification = intent.getExtras();
    if (notification == null) {
      Log.d("Unable to pre handle push notification, extras are null.");
      return null;
    }
    return notification;
  }

  /**
   * Must be called after deciding which activity will be opened, to allow Leanplum SDK to track
   * stats, open events etc.
   *
   * @param context Surrounding context.
   * @param intent Received Intent.
   */
  public static void postHandlePushNotification(Context context, Intent intent) {
    final Bundle notification = intent.getExtras();
    if (notification == null) {
      Log.d("Could not post handle push notification, extras are null.");
      return;
    }
    // Perform action.
    LeanplumActivityHelper.queueActionUponActive(new VariablesChangedCallback() {
      @Override
      public void variablesChanged() {
        try {
          final String messageId = LeanplumPushService.getMessageId(notification);
          final String actionName = Constants.Values.DEFAULT_PUSH_ACTION;

          // Make sure content is available.
          if (messageId != null) {
            if (LeanplumPushService.areActionsEmbedded(notification)) {
              Map<String, Object> args = new HashMap<>();
              args.put(actionName, JsonConverter.fromJson(
                  notification.getString(Keys.PUSH_MESSAGE_ACTION)));
              ActionContext context = new ActionContext(ActionManager.PUSH_NOTIFICATION_ACTION_NAME,
                  args, messageId);
              context.preventRealtimeUpdating();
              context.update();
              context.runTrackedActionNamed(actionName);
            } else {
              Leanplum.addOnceVariablesChangedAndNoDownloadsPendingHandler(
                  new VariablesChangedCallback() {
                    @Override
                    public void variablesChanged() {
                      try {
                        LeanplumPushService.requireMessageContent(messageId,
                            new VariablesChangedCallback() {
                              @Override
                              public void variablesChanged() {
                                try {
                                  LeanplumInternal.performTrackedAction(actionName, messageId);
                                } catch (Throwable t) {
                                  Log.exception(t);
                                }
                              }
                            });
                      } catch (Throwable t) {
                        Log.exception(t);
                      }
                    }
                  });
            }
          }
        } catch (Throwable t) {
          Log.exception(t);
        }
      }
    });
  }

  /**
   * Return true if we found an activity to handle Intent and started it.
   */
  private static boolean isActivityWithIntentStarted(Context context, Bundle notification) {
    String action = notification.getString(Keys.PUSH_MESSAGE_ACTION);
    if (action != null && action.contains(OPEN_URL)) {
      Intent deepLinkIntent = getDeepLinkIntent(notification);
      if (deepLinkIntent != null && activityHasIntent(context, deepLinkIntent)) {
        String messageId = LeanplumPushService.getMessageId(notification);
        if (messageId != null) {
          ActionContext actionContext = new ActionContext(
              ActionManager.PUSH_NOTIFICATION_ACTION_NAME, null, messageId);
          actionContext.track(OPEN_ACTION, 0.0, null);
          context.startActivity(deepLinkIntent);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Gets Intent from Push Notification Bundle.
   */
  private static Intent getDeepLinkIntent(Bundle notification) {
    try {
      String actionString = notification.getString(Keys.PUSH_MESSAGE_ACTION);
      if (actionString != null) {
        JSONObject openAction = new JSONObject(actionString);
        Intent deepLinkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
            openAction.getString(URL)));
        deepLinkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return deepLinkIntent;
      }
    } catch (JSONException ignored) {
    }
    return null;
  }

  /**
   * Checks if there is some activity that can handle intent.
   */
  private static Boolean activityHasIntent(Context context, Intent deepLinkIntent) {
    List<ResolveInfo> resolveInfoList =
        context.getPackageManager().queryIntentActivities(deepLinkIntent, 0);
    if (resolveInfoList != null && !resolveInfoList.isEmpty()) {
      for (ResolveInfo resolveInfo : resolveInfoList) {
        if (resolveInfo != null && resolveInfo.activityInfo != null &&
            resolveInfo.activityInfo.name != null) {
          if (resolveInfo.activityInfo.name.contains(context.getPackageName())) {
            // If url can be handled by current app - set package name to intent, so url will be
            // open by current app. Skip chooser dialog.
            deepLinkIntent.setPackage(resolveInfo.activityInfo.packageName);
          }
          return true;
        }
      }
    }
    return false;
  }

  private static Intent getActionIntent(Context context) {
    Class<? extends Activity> callbackClass = LeanplumPushService.getCallbackClass();
    if (callbackClass != null) {
      return new Intent(context, callbackClass);
    } else {
      PackageManager pm = context.getPackageManager();
      return pm.getLaunchIntentForPackage(context.getPackageName());
    }
  }

  /**
   * Unregisters the device from all FCM push notifications. You shouldn't need to call this method
   * in production.
   */
  public static void unregister() {
    try {
      Intent unregisterIntent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
      Context context = Leanplum.getContext();
      unregisterIntent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
      unregisterIntent.setPackage("com.google.android.gms");
      context.startService(unregisterIntent);
    } catch (Throwable t) {
      Log.exception(t);
    }
  }

  /**
   * Call this when Leanplum starts. This method will call by reflection from AndroidSDKCore.
   */
  static void onStart() {
    pushProviders.updateRegistrationIdsAndBackend();
  }

  /**
   * Show notification that device was registered for push notifications. This method will call by
   * reflection from AndroidSDKCore.
   *
   * @param context The application context.
   * @param currentContext Current application context.
   */
  static void showDeviceRegisteredPush(Context context, Context currentContext) {
    try {
      NotificationCompat.Builder builder =
          LeanplumNotificationHelper.getDefaultCompatNotificationBuilder(context,
              BuildUtil.isNotificationChannelSupported(context));
      if (builder == null) {
        return;
      }
      builder.setSmallIcon(android.R.drawable.star_on)
          .setContentTitle("Leanplum")
          .setContentText("Your device is registered.");
      builder.setContentIntent(PendingIntent.getActivity(
          currentContext.getApplicationContext(), 0, new Intent(), 0));
      NotificationManager mNotificationManager =
          (NotificationManager) currentContext.getSystemService(
              Context.NOTIFICATION_SERVICE);
      // mId allows you to update the notification later on.
      mNotificationManager.notify(0, builder.build());
    } catch (Throwable t) {
      // ignore
    }
  }
}
