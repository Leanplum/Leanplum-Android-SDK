package com.leanplum.migration.push

import com.clevertap.android.sdk.pushnotification.fcm.CTFcmMessageHandler

class FcmMigrationHandler internal constructor() : CTFcmMessageHandler() {
  /**
   * Flag used in testing app to stop FCM messages forwarding to CT. It can't be implemented here
   * due to FCM dependencies in the method signature.
   */
  var forwardingEnabled = true
}
