package com.leanplum.migration.push

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler

class MiPushMigrationHandler internal constructor() {

  fun createNotification(context: Context?, messageContent: String?): Boolean {
    val messageBundle: Bundle = Utils.stringToBundle(messageContent)
    val isSuccess = try {
      PushNotificationHandler.getPushNotificationHandler().onMessageReceived(
        context,
        messageBundle,
        PushType.XPS.toString())
    } catch (t: Throwable) {
      t.printStackTrace()
      false
    }
    return isSuccess
  }

  fun onNewToken(context: Context?, token: String?): Boolean {
    var isSuccess = false
    try {
      PushNotificationHandler.getPushNotificationHandler().onNewToken(
        context,
        token,
        PushType.XPS.type
      )
      isSuccess = true
    } catch (t: Throwable) {
      t.printStackTrace()
    }
    return isSuccess
  }
}
