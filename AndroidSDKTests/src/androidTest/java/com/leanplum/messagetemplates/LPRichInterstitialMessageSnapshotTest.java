package com.leanplum.messagetemplates;

import android.app.Activity;
import android.app.Instrumentation;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import com.facebook.testing.screenshot.Screenshot;
import com.facebook.testing.screenshot.ViewHelpers;
import com.leanplum.ActionContext;
import com.leanplum.FileUtils;
import com.leanplum.Leanplum;
import com.leanplum.internal.FileManager;
import com.leanplum.tests.MainActivity;
import java.util.HashMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LPRichInterstitialMessageSnapshotTest {

  @Rule
  public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

  private View messageView;

  @Test
  public void testRichInterstitial() throws Exception {
    Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    Leanplum.setApplicationContext(activityRule.getActivity());

    final String templateName = "richInterstitialTemplate.html";
    String templatePathOnDevice = FileManager.fileRelativeToDocuments(templateName);
    FileUtils.uploadResource("/messages/" + templateName, templatePathOnDevice);

    instrumentation.runOnMainSync(new Runnable() {
      @Override
      public void run() {
        messageView = createHtmlTemplateMessage(templateName);
      }
    });

    Thread.sleep(2000); // wait until WebView is rendered

    instrumentation.runOnMainSync(new Runnable() {
      @Override
      public void run() {
        Screenshot.snap(messageView).setName("richInterstitial").record();
      }
    });
  }

  private View createHtmlTemplateMessage(String htmlFileName)  {

    final Activity mainActivity = activityRule.getActivity();

    HashMap<String, Object> map = new HashMap<>();
    map.put("Width", "100%");
    map.put("Height", "100%");
    map.put(MessageTemplates.Args.CLOSE_URL, "http://leanplum/close");
    map.put(MessageTemplates.Args.OPEN_URL, "http://leanplum/loadFinished");
    map.put(MessageTemplates.Args.TRACK_URL, "http://leanplum/track");
    map.put(MessageTemplates.Args.ACTION_URL, "http://leanplum/runAction");
    map.put(MessageTemplates.Args.TRACK_ACTION_URL, "http://leanplum/runTrackedAction");
    map.put(MessageTemplates.Args.HTML_ALIGN, "Top");
    map.put(MessageTemplates.Args.HTML_HEIGHT, 0);
    map.put(MessageTemplates.Args.HTML_TAP_OUTSIDE_TO_CLOSE, false);
    map.put(MessageTemplates.Values.HTML_TEMPLATE_PREFIX, htmlFileName);

    ActionContext actionContext = new ActionContext("HTML", map, "message_id");
    HTMLOptions options = new HTMLOptions(actionContext);
    HTMLTemplate htmlTemplate = new HTMLTemplate(mainActivity, options);

    ViewHelpers.setupView(htmlTemplate.dialogView)
        .setExactWidthDp(480)
        .setExactHeightDp(640)
        .layout();

    return htmlTemplate.dialogView;
  }
}
