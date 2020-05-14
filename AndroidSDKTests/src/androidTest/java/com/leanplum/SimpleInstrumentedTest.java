package com.leanplum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.LinearLayout;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import com.facebook.testing.screenshot.Screenshot;
import com.leanplum.messagetemplates.CenterPopup;
import com.leanplum.messagetemplates.CenterPopupOptions;
import com.leanplum.messagetemplates.MessageTemplates;
import com.leanplum.tests.MainActivity;
import com.leanplum.tests.R;
import java.util.HashMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SimpleInstrumentedTest {

  @Rule
  public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

  @Test
  public void viewDisplayedTest() {
//    // Context of the app under test.
//    Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
//
//    //assertEquals("com.leanplum", appContext.getPackageName());
//
//    Espresso.onView(ViewMatchers.withText("Schedule")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

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
}
