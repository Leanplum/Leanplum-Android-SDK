package com.leanplum

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType
import com.clevertap.android.sdk.pushnotification.PushNotificationHandler
import com.xiaomi.mipush.sdk.MiPushMessage

class CTMiPushHandler {

  fun createNotification(context: Context?, message: MiPushMessage?): Boolean {
    val messageBundle: Bundle = Utils.stringToBundle(message?.content)
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

  // TODO add processPushAmp method ?

}
