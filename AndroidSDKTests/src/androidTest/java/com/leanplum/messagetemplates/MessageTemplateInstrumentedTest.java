package com.leanplum.messagetemplates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import androidx.test.annotation.UiThreadTest;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import com.facebook.testing.screenshot.Screenshot;
import com.facebook.testing.screenshot.ViewHelpers;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.tests.MainActivity;
import com.leanplum.tests.R;
import java.util.HashMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MessageTemplateInstrumentedTest {

  @Rule
  public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

//  @Test
//  public void viewDisplayedTest() {
//    // Context of the app under test.
//    Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
//
//    //assertEquals("com.leanplum", appContext.getPackageName());
//
//    Espresso.onView(ViewMatchers.withText("Schedule")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
//  }
//
//  @Test
//  public void viewSnapshotTest() {
////        Context appContext = InstrumentationRegistry.getTargetContext();
//    Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
//    LinearLayout view = new LinearLayout(appContext);
//    view.setBackgroundColor(appContext.getResources().getColor(R.color.colorPrimary));
//
//    //ViewHelpers.setupView(view).setExactWidthDp(300).setExactHeightDp(300).layout();
//
//    int width = View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY);
//    int height = View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY);
//    view.measure(width, height);
//
//    //Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
//    //Canvas canvas = new Canvas(bitmap);
//    view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
//    //view.draw(canvas);
//
//
//    Screenshot.snap(view).setName("edinfail.txt").record();
////    Bitmap bitmap = Screenshot.snap(view).getBitmap();
//
////    System.out.println("canvas wid "+bitmap.getWidth());
////
////    assertEquals(800, bitmap.getWidth());
//  }
CenterPopup centerpopup;
  @Test
  @UiThreadTest
  public void testCenterPopup() throws Exception {
    final Activity mainActivity = activityRule.getActivity();
//    Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    Leanplum.setApplicationContext(mainActivity);
    //Leanplum.forceContentUpdate();

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
    a.put("Height", 250);
    map.put("Layout", a);

    ActionContext actionContext = new ActionContext("center_popup", map, "message_id");
    final CenterPopupOptions options = new CenterPopupOptions(actionContext);
//    final CenterPopup centerpopup = new CenterPopup(mainActivity, options);

    mainActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        centerpopup = new CenterPopup(mainActivity, options);
        //centerpopup.show();
      }
    });
    //Thread.sleep(2000);
    mainActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        ViewHelpers.setupView(centerpopup.dialogView).setExactWidthDp(300).setExactHeightDp(300).layout();
        Screenshot.snap(centerpopup.dialogView).setName("centerPopup").record();
        //Bitmap bmp = Screenshot.snap(centerpopup.dialogView).getBitmap();
        //bmp.getWidth();
      }
    });

//    Espresso.onView(ViewMatchers.withText("message_text");

//    AlertDialog.Builder builder = new Builder(mainActivity);
//    builder.setMessage("Hello");
//    AlertDialog a = builder.create();
//    a.show();

//    Espresso.onView()

//    Handler handler = new Handler(Looper.getMainLooper());
//
//    handler.post(new Runnable() {
//      @Override
//      public void run() {
//        centerpopup.show();
//      }
//    });
//    Looper.prepare();
//    mainActivity.runOnUiThread(new Runnable() {
//      @Override
//      public void run() {
//        centerpopup.show();
//      }
//    });

//    mainActivity.runOnUiThread(new Runnable() {
//      public void run() {
//        // UI code goes here
//        Bitmap bmp = Screenshot.snap(centerpopup.dialogView).getBitmap();
//        System.out.println("bmp wid = " + bmp.getWidth());
//
//        centerpopup.hide();
//      }
//    });

    Thread.sleep(5000);
//    assertNotNull(centerpopup);
//    assertEquals(options, centerpopup.options);
  }

//  @Test
//  //@UiThreadTest
//  public void testAlertDialog() throws Exception {
//    final Activity mainActivity = activityRule.getActivity();
//
//    mainActivity.runOnUiThread(new Runnable() {
//      @Override
//      public void run() {
//        AlertDialog.Builder builder = new Builder(mainActivity);
//        builder.setMessage("Hello");
//        AlertDialog a = builder.create();
//        a.show();
//      }
//    });
//    Thread.sleep(4000);
//  }
}
