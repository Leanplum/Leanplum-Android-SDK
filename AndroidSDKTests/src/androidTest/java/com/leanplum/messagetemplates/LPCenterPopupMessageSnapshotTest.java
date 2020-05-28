package com.leanplum.messagetemplates;

import android.app.Activity;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import com.facebook.testing.screenshot.Screenshot;
import com.facebook.testing.screenshot.ViewHelpers;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.tests.MainActivity;
import java.util.HashMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LPCenterPopupMessageSnapshotTest {

  @Rule
  public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

  @Test
  @UiThreadTest
  public void testCenterPopup() {
    final Activity mainActivity = activityRule.getActivity();
    Leanplum.setApplicationContext(mainActivity);

    HashMap<String, Object> map = new HashMap<>();
    HashMap<String, Object> a = new HashMap<>();
    a.put("Text", "text");
    a.put("Color", 0xff000000);
//    map.put(MessageTemplates.Args.TITLE_TEXT, "text");
//    map.put(MessageTemplates.Args.TITLE_COLOR, 100);
    map.put("Title", a);
//    map.put(MessageTemplates.Args.MESSAGE_TEXT, "message_text");
//    map.put(MessageTemplates.Args.MESSAGE_COLOR, 1200);
    a = new HashMap<>();
    a.put("Text", "message_text");
    a.put("Color", 0xff000000);
    map.put("Message", a);

    map.put(MessageTemplates.Args.BACKGROUND_COLOR, 0xffffffff);
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_TEXT, "button_text");
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_BACKGROUND_COLOR, 123);
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_TEXT_COLOR, 123);
//    map.put(MessageTemplates.Args.LAYOUT_WIDTH, 128);
//    map.put(MessageTemplates.Args.LAYOUT_HEIGHT, 128);
    a = new HashMap<>();
    a.put("Width", 300);
    a.put("Height", 300);
    map.put("Layout", a);

    ActionContext actionContext = new ActionContext("center_popup", map, "message_id");
    CenterPopupOptions options = new CenterPopupOptions(actionContext);
    CenterPopup centerpopup = new CenterPopup(mainActivity, options);

    ViewHelpers.setupView(
        centerpopup.dialogView).setExactWidthDp(300).setExactHeightDp(300).layout();
    Screenshot.snap(centerpopup.dialogView).setName("centerPopup").record();
  }
}
