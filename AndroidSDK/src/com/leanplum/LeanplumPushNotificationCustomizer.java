// Copyright 2015, Leanplum, Inc.

package com.leanplum;

import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

/**
 * Implement LeanplumPushNotificationCustomizer to customize the appearance of notifications.
 */
public interface LeanplumPushNotificationCustomizer {
  void customize(NotificationCompat.Builder builder, Bundle notificationPayload);
}
