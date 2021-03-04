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
import com.leanplum.messagetemplates.controllers.RichHtmlController;
import com.leanplum.messagetemplates.options.RichHtmlOptions;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class LPRichInterstitialMessageSnapshotTest extends BaseSnapshotTest {

  private View messageView;
  private InputStream templateStream;

  @Override
  protected String getSnapshotName() {
    return "richInterstitial";
  }

  @Before
  public void setUp() {
    templateStream = getClass().getResourceAsStream("/messages/richInterstitialTemplate.html");
  }

  @After
  public void tearDown() throws IOException {
    templateStream.close();
  }

  @Test
  public void testRichInterstitial() throws InterruptedException {
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

  private View createMessageView()  {
    Map<String, Object> args = new HashMap<>();
    args.put(Args.LAYOUT_WIDTH, Values.CENTER_POPUP_WIDTH);
    args.put(Args.LAYOUT_HEIGHT, Values.CENTER_POPUP_HEIGHT);
    args.put(Args.CLOSE_URL, Values.DEFAULT_CLOSE_URL);
    args.put(Args.OPEN_URL, Values.DEFAULT_OPEN_URL);
    args.put(Args.TRACK_URL, Values.DEFAULT_TRACK_URL);
    args.put(Args.ACTION_URL, Values.DEFAULT_ACTION_URL);
    args.put(Args.TRACK_ACTION_URL, Values.DEFAULT_TRACK_ACTION_URL);
    args.put(Args.HTML_ALIGN, Values.DEFAULT_HTML_ALING);
    args.put(Args.HTML_HEIGHT, Values.DEFAULT_HTML_HEIGHT);
    args.put(Args.HTML_WIDTH, "100%");
    args.put(Args.HTML_TAP_OUTSIDE_TO_CLOSE, false);
    args.put(Args.HAS_DISMISS_BUTTON, false);
    ActionContext realContext = new ActionContext(getSnapshotName(), args, null);

    ActionContext mockedContext = spy(realContext);
    when(mockedContext.numberNamed(Args.LAYOUT_WIDTH)).thenReturn(Values.CENTER_POPUP_WIDTH);
    when(mockedContext.numberNamed(Args.LAYOUT_HEIGHT)).thenReturn(Values.CENTER_POPUP_HEIGHT);
    when(mockedContext.stringNamed(Args.CLOSE_URL)).thenReturn(Values.DEFAULT_CLOSE_URL);
    when(mockedContext.stringNamed(Args.OPEN_URL)).thenReturn(Values.DEFAULT_OPEN_URL);
    when(mockedContext.stringNamed(Args.TRACK_URL)).thenReturn(Values.DEFAULT_TRACK_URL);
    when(mockedContext.stringNamed(Args.ACTION_URL)).thenReturn(Values.DEFAULT_ACTION_URL);
    when(mockedContext.stringNamed(Args.TRACK_ACTION_URL)).thenReturn(Values.DEFAULT_TRACK_ACTION_URL);
    when(mockedContext.stringNamed(Args.HTML_ALIGN)).thenReturn(Values.DEFAULT_HTML_ALING);
    when(mockedContext.numberNamed(Args.HTML_HEIGHT)).thenReturn(Values.DEFAULT_HTML_HEIGHT);
    when(mockedContext.stringNamed(Args.HTML_WIDTH)).thenReturn("100%");
    when(mockedContext.booleanNamed(Args.HTML_TAP_OUTSIDE_TO_CLOSE)).thenReturn(false);
    when(mockedContext.booleanNamed(Args.HAS_DISMISS_BUTTON)).thenReturn(false);
    when(mockedContext.streamNamed(Mockito.anyString())).thenReturn(templateStream);

    RichHtmlOptions options = new RichHtmlOptions(mockedContext);
    RichHtmlController htmlTemplate = new RichHtmlController(getMainActivity(), options);
    return htmlTemplate.getContentView();
  }
}
