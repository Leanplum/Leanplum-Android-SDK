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

public class LPBannerMessageSnapshotTest extends BaseSnapshotTest {

  private View messageView;
  private InputStream templateStream;

  @Override
  protected String getSnapshotName() {
    return "banner";
  }

  @Before
  public void setUp() {
    templateStream = getClass().getResourceAsStream("/messages/bannerTemplate.html");
  }

  @After
  public void tearDown() throws IOException {
    templateStream.close();
  }

  @Test
  public void testBanner() throws InterruptedException {
    Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    Leanplum.setApplicationContext(getMainActivity());

    instrumentation.runOnMainSync(() -> {
      messageView = createMessageView();
      setupView(messageView);
    });

    Thread.sleep(7000); // wait until WebView is rendered

    instrumentation.runOnMainSync(() -> {
      snapshotView(messageView);
    });
  }

  private View createMessageView()  {
    Map<String, Object> args = new HashMap<>();
    args.put(Args.CLOSE_URL, Values.DEFAULT_CLOSE_URL);
    args.put(Args.OPEN_URL, Values.DEFAULT_OPEN_URL);
    args.put(Args.TRACK_URL, Values.DEFAULT_TRACK_URL);
    args.put(Args.ACTION_URL, Values.DEFAULT_ACTION_URL);
    args.put(Args.TRACK_ACTION_URL, Values.DEFAULT_TRACK_ACTION_URL);
    args.put(Args.HTML_ALIGN, Values.DEFAULT_HTML_ALING);
    args.put(Args.HTML_HEIGHT, 60);
    args.put(Args.HTML_WIDTH, "100%");
    args.put(Args.HTML_TAP_OUTSIDE_TO_CLOSE, false);
    args.put(Values.HTML_TEMPLATE_PREFIX, "banner");
    ActionContext realContext = new ActionContext(getSnapshotName(), args, null);

    ActionContext mockedContext = spy(realContext);
    when(mockedContext.stringNamed(Args.CLOSE_URL)).thenReturn(Values.DEFAULT_CLOSE_URL);
    when(mockedContext.stringNamed(Args.OPEN_URL)).thenReturn(Values.DEFAULT_OPEN_URL);
    when(mockedContext.stringNamed(Args.TRACK_URL)).thenReturn(Values.DEFAULT_TRACK_URL);
    when(mockedContext.stringNamed(Args.ACTION_URL)).thenReturn(Values.DEFAULT_ACTION_URL);
    when(mockedContext.stringNamed(Args.TRACK_ACTION_URL)).thenReturn(Values.DEFAULT_TRACK_ACTION_URL);
    when(mockedContext.stringNamed(Args.HTML_ALIGN)).thenReturn(Values.DEFAULT_HTML_ALING);
    when(mockedContext.numberNamed(Args.HTML_HEIGHT)).thenReturn(60);
    when(mockedContext.stringNamed(Args.HTML_WIDTH)).thenReturn("100%");
    when(mockedContext.booleanNamed(Args.HTML_TAP_OUTSIDE_TO_CLOSE)).thenReturn(false);
    when(mockedContext.streamNamed(Mockito.anyString())).thenReturn(templateStream);

    RichHtmlOptions options = new RichHtmlOptions(mockedContext);
    RichHtmlController htmlTemplate = new RichHtmlController(getMainActivity(), options);
    return htmlTemplate.getContentView();
  }
}
