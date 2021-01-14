package com.leanplum;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.leanplum.internal.OperationQueue;
import com.xiaomi.mipush.sdk.ErrorCode;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import java.util.List;

// TODO javadoc as in fcm
public class LeanplumMiPushHandler {

  static String APP_ID;
  static String APP_KEY;

  public static void setApplication(String miAppId, String miAppKey) {
    APP_ID = miAppId;
    APP_KEY = miAppKey;
  }

  public void onReceivePassThroughMessage(Context context, MiPushMessage message) {
    // Handle data message
    toastAndLog(context, "onReceivePassThroughMessage: id=" + message.getMessageId() + " content=" + message.getContent() + " notifyId="+message.getNotifyId());
  }

  public void onNotificationMessageClicked(Context context, MiPushMessage message) {
    // Handle notification message
    toastAndLog(context, "onNotificationMessageClicked = " + message.getMessageId() + " content="+message.getContent() + " notifyId="+message.getNotifyId());
  }

  public void onNotificationMessageArrived(Context context, MiPushMessage message) {
    //MiPushClient.clearNotification(context, message.getNotifyId()); TODO clean notification if app is in foreground and "mute inside app" is set
    toastAndLog(context, "onNotificationMessageArrived: id=" + message.getMessageId() + " content=" + message.getContent() + " notifyId="+message.getNotifyId());
  }

  public void onCommandResult(Context context, MiPushCommandMessage message) {
    toastAndLog(context, "onCommandResult = " + message.getCommand());
  }

  public void onReceiveRegisterResult(Context context, MiPushCommandMessage message) {
    if (message != null && MiPushClient.COMMAND_REGISTER.equals(message.getCommand())) {
      if (message.getResultCode() == ErrorCode.SUCCESS) {
        List<String> args = message.getCommandArguments();
        if (args != null && args.size() > 0) {
          String registrationId = args.get(0);

          LeanplumPushService.getPushProviders()
              .setRegistrationId(PushProviderType.MIPUSH, registrationId);
          toastAndLog(context, "regid = " + registrationId);
        }
      }
    }

    toastAndLog(context, "onReceiveRegisterResult = " + message.getCommand());
  }

  private void toastAndLog(Context context, String message) { // TODO remove logging
    OperationQueue.sharedInstance().addUiOperation(
        () -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    Log.e("Xiaomi", message);
  }
}
