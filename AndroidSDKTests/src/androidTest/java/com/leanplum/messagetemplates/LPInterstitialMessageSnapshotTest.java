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
import com.leanplum.messagetemplates.controllers.InterstitialController;
import com.leanplum.messagetemplates.options.InterstitialOptions;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class LPInterstitialMessageSnapshotTest extends BaseSnapshotTest {
  @Override
  public String getSnapshotName() {
    return "interstitial";
  }

  @Test
  @UiThreadTest
  public void testInterstitial() {
    Activity mainActivity = getMainActivity();
    Leanplum.setApplicationContext(mainActivity);

    String appName = getApplicationName();

    Map<String, Object> args = new HashMap<>();
    args.put(Args.TITLE_TEXT, appName);
    args.put(Args.TITLE_COLOR, Color.BLACK);
    args.put(Args.MESSAGE_TEXT, Values.INTERSTITIAL_MESSAGE);
    args.put(Args.MESSAGE_COLOR, Color.BLACK);
    args.put(Args.BACKGROUND_COLOR, Color.WHITE);
    args.put(Args.ACCEPT_BUTTON_TEXT, Values.OK_TEXT);
    args.put(Args.ACCEPT_BUTTON_BACKGROUND_COLOR, Color.WHITE);
    args.put(Args.ACCEPT_BUTTON_TEXT_COLOR, Color.BLACK);
    ActionContext realContext = new ActionContext(getSnapshotName(), args, null);

    ActionContext mockedContext = spy(realContext);
    when(mockedContext.stringNamed(Args.TITLE_TEXT)).thenReturn(appName);
    when(mockedContext.numberNamed(Args.TITLE_COLOR)).thenReturn(Color.BLACK);
    when(mockedContext.stringNamed(Args.MESSAGE_TEXT)).thenReturn(Values.INTERSTITIAL_MESSAGE);
    when(mockedContext.numberNamed(Args.MESSAGE_COLOR)).thenReturn(Color.BLACK);
    when(mockedContext.numberNamed(Args.BACKGROUND_COLOR)).thenReturn(Color.WHITE);
    when(mockedContext.stringNamed(Args.ACCEPT_BUTTON_TEXT)).thenReturn(Values.OK_TEXT);
    when(mockedContext.numberNamed(Args.ACCEPT_BUTTON_BACKGROUND_COLOR)).thenReturn(Color.WHITE);
    when(mockedContext.numberNamed(Args.ACCEPT_BUTTON_TEXT_COLOR)).thenReturn(Color.BLACK);

    InterstitialOptions options = new InterstitialOptions(mockedContext);
    InterstitialController interstitial = new InterstitialController(mainActivity, options);

    setupView(interstitial.getContentView());
    snapshotView(interstitial.getContentView());
  }
}
