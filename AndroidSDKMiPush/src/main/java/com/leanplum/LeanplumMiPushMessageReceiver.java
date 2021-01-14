package com.leanplum;

import android.content.Context;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;

public class LeanplumMiPushMessageReceiver extends PushMessageReceiver {

  private LeanplumMiPushHandler handler = new LeanplumMiPushHandler();

  @Override
  public void onReceivePassThroughMessage(Context context, MiPushMessage message) {
    handler.onReceivePassThroughMessage(context, message);
  }

  @Override
  public void onNotificationMessageClicked(Context context, MiPushMessage message) {
    handler.onNotificationMessageClicked(context, message);
  }

  @Override
  public void onNotificationMessageArrived(Context context, MiPushMessage message) {
    handler.onNotificationMessageArrived(context, message);
  }

  @Override
  public void onCommandResult(Context context, MiPushCommandMessage message) {
    handler.onCommandResult(context, message);
  }

  @Override
  public void onReceiveRegisterResult(Context context, MiPushCommandMessage message) {
    handler.onReceiveRegisterResult(context, message);
  }
}
