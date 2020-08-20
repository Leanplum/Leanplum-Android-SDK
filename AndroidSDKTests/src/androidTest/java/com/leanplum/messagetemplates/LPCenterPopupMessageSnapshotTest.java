package com.leanplum.messagetemplates;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.graphics.Color;
import androidx.test.annotation.UiThreadTest;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.messagetemplates.MessageTemplateConstants.Args;
import com.leanplum.messagetemplates.MessageTemplateConstants.Values;
import com.leanplum.messagetemplates.controllers.CenterPopupController;
import com.leanplum.messagetemplates.options.CenterPopupOptions;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class LPCenterPopupMessageSnapshotTest extends BaseSnapshotTest {

  @Override
  public String getSnapshotName() {
    return "centerPopup";
  }

  @Test
  @UiThreadTest
  public void testCenterPopup() {
    Activity mainActivity = getMainActivity();
    Leanplum.setApplicationContext(mainActivity);

    String appName = getApplicationName();

    Map<String, Object> args = new HashMap<>();
    args.put(Args.TITLE_TEXT, appName);
    args.put(Args.TITLE_COLOR, Color.RED);
    args.put(Args.MESSAGE_TEXT, Values.POPUP_MESSAGE);
    args.put(Args.MESSAGE_COLOR, Color.BLACK);
    args.put(Args.BACKGROUND_COLOR, Color.WHITE);
    args.put(Args.ACCEPT_BUTTON_TEXT, Values.OK_TEXT);
    args.put(Args.ACCEPT_BUTTON_BACKGROUND_COLOR, Color.WHITE);
    args.put(Args.ACCEPT_BUTTON_TEXT_COLOR, Color.BLACK);
    args.put(Args.LAYOUT_WIDTH, Values.CENTER_POPUP_WIDTH);
    args.put(Args.LAYOUT_HEIGHT, Values.CENTER_POPUP_HEIGHT);
    ActionContext realContext = new ActionContext(getSnapshotName(), args, null);

    ActionContext mockedContext = spy(realContext);
    when(mockedContext.stringNamed(Args.TITLE_TEXT)).thenReturn(appName);
    when(mockedContext.numberNamed(Args.TITLE_COLOR)).thenReturn(Color.RED);
    when(mockedContext.stringNamed(Args.MESSAGE_TEXT)).thenReturn(Values.POPUP_MESSAGE);
    when(mockedContext.numberNamed(Args.MESSAGE_COLOR)).thenReturn(Color.BLACK);
    when(mockedContext.numberNamed(Args.BACKGROUND_COLOR)).thenReturn(Color.WHITE);
    when(mockedContext.stringNamed(Args.ACCEPT_BUTTON_TEXT)).thenReturn(Values.OK_TEXT);
    when(mockedContext.numberNamed(Args.ACCEPT_BUTTON_BACKGROUND_COLOR)).thenReturn(Color.WHITE);
    when(mockedContext.numberNamed(Args.ACCEPT_BUTTON_TEXT_COLOR)).thenReturn(Color.BLACK);
    when(mockedContext.numberNamed(Args.LAYOUT_WIDTH)).thenReturn(Values.CENTER_POPUP_WIDTH);
    when(mockedContext.numberNamed(Args.LAYOUT_HEIGHT)).thenReturn(Values.CENTER_POPUP_HEIGHT);

    CenterPopupOptions options = new CenterPopupOptions(mockedContext);
    CenterPopupController centerpopup = new CenterPopupController(mainActivity, options);

    setupView(centerpopup.getContentView());
    snapshotView(centerpopup.getContentView());
  }
}
