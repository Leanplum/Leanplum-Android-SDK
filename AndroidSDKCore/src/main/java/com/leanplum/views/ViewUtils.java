package com.leanplum.views;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import com.leanplum.utils.SizeUtil;

public class ViewUtils {

  public static int getThemeId(Activity activity) {
    boolean full = (activity.getWindow().getAttributes().flags &
        WindowManager.LayoutParams.FLAG_FULLSCREEN) == WindowManager.LayoutParams.FLAG_FULLSCREEN;
    if (full) {
      return android.R.style.Theme_Translucent_NoTitleBar_Fullscreen;
    } else {
      return android.R.style.Theme_Translucent_NoTitleBar;
    }
  }

  public static void applyBackground(@NonNull View view, int color, boolean roundedCorners) {
    SizeUtil.init(view.getContext()); // just in case

    ShapeDrawable footerBackground = new ShapeDrawable();
    int cornerRadius = roundedCorners ? SizeUtil.dp20 : 0;
    footerBackground.setShape(createRoundRect(cornerRadius));
    footerBackground.getPaint().setColor(color);
    ViewCompat.setBackground(view, footerBackground);
  }

  private static Shape createRoundRect(int cornerRadius) {
    int c = cornerRadius;
    float[] outerRadii = new float[] {c, c, c, c, c, c, c, c};
    return new RoundRectShape(outerRadii, null, null);
  }

  public static Animation createFadeInAnimation(long durationMillis) {
    Animation fadeIn = new AlphaAnimation(0, 1);
    fadeIn.setInterpolator(new DecelerateInterpolator());
    fadeIn.setDuration(durationMillis);
    return fadeIn;
  }

  public static Animation createFadeOutAnimation(long durationMillis) {
    Animation fadeOut = new AlphaAnimation(1, 0);
    fadeOut.setInterpolator(new AccelerateInterpolator());
    fadeOut.setDuration(durationMillis);
    return fadeOut;
  }
}
