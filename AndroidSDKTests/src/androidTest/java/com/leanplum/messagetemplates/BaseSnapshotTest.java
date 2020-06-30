package com.leanplum.messagetemplates;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import com.facebook.testing.screenshot.Screenshot;
import com.facebook.testing.screenshot.ViewHelpers;
import com.leanplum.tests.MainActivity;
import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public abstract class BaseSnapshotTest {
  private static final int SNAPSHOT_WIDTH_DP = 480;
  private static final int SNAPSHOT_HEIGHT_DP = 640;

  @Rule
  public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

  protected abstract String getSnapshotName();

  protected Activity getMainActivity() {
    return activityRule.getActivity();
  }

  protected String getApplicationName() {
    PackageManager pm = getMainActivity().getPackageManager();
    return getMainActivity().getApplicationInfo().loadLabel(pm).toString();
  }

  protected void setupView(@NonNull View view) {
    ViewHelpers.setupView(view)
        .setExactWidthDp(SNAPSHOT_WIDTH_DP)
        .setExactHeightDp(SNAPSHOT_HEIGHT_DP)
        .layout();
  }

  protected void snapshotView(@NonNull View view) {
    Screenshot.snap(view).setName(getSnapshotName()).record();
  }
}
