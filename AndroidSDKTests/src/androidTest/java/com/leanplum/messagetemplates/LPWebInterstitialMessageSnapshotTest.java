package com.leanplum.messagetemplates;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Instrumentation;
import android.view.View;
import androidx.test.platform.app.InstrumentationRegistry;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.messagetemplates.MessageTemplateConstants.Args;
import com.leanplum.messagetemplates.MessageTemplateConstants.Values;
import com.leanplum.messagetemplates.controllers.WebInterstitialController;
import com.leanplum.messagetemplates.options.WebInterstitialOptions;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class LPWebInterstitialMessageSnapshotTest extends BaseSnapshotTest {

  private View messageView;

  @Override
  public String getSnapshotName() {
    return "webInterstitial";
  }

  @Test
  public void testWebInterstitial() throws InterruptedException {
    Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    Leanplum.setApplicationContext(getMainActivity());

    instrumentation.runOnMainSync(new Runnable() {
      @Override
      public void run() {
        messageView = createMessageView();
        setupView(messageView);
      }
    });

    Thread.sleep(7000); // wait until WebView is rendered

    instrumentation.runOnMainSync(new Runnable() {
      @Override
      public void run() {
        snapshotView(messageView);
      }
    });
  }

  private View createMessageView() {
    Map<String, Object> args = new HashMap<>();
    args.put(Args.URL, Values.DEFAULT_URL);
    args.put(Args.CLOSE_URL, Values.DEFAULT_CLOSE_URL);
    args.put(Args.HAS_DISMISS_BUTTON, Values.DEFAULT_HAS_DISMISS_BUTTON);
    ActionContext realContext = new ActionContext(getSnapshotName(), args, null);

    ActionContext mockedContext = spy(realContext);
    when(mockedContext.stringNamed(Args.URL)).thenReturn(Values.DEFAULT_URL);
    when(mockedContext.stringNamed(Args.CLOSE_URL)).thenReturn(Values.DEFAULT_CLOSE_URL);
    when(mockedContext.booleanNamed(Args.HAS_DISMISS_BUTTON)).thenReturn(Values.DEFAULT_HAS_DISMISS_BUTTON);

    WebInterstitialOptions options = new WebInterstitialOptions(mockedContext);
    WebInterstitialController webInterstitial = new WebInterstitialController(getMainActivity(), options);
    return webInterstitial.getContentView();
  }
}
