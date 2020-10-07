package com.leanplum.messagetemplates.actions;

import android.app.Activity;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.widget.Toast;
import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.internal.Util;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class AppRatingAction {
  public static ActionArgs createActionArgs(Context context) {
    return new ActionArgs();
  }

  public static void onActionTriggered(ActionContext context) {
    Activity activity = LeanplumActivityHelper.getCurrentActivity();
    if (activity == null || activity.isFinishing())
      return;

    try {
      if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP && Util.hasPlayServices()) {
        showRatingFlow(activity);
      }
      else {
        // TODO show appropriate log for sdk version and play services
      }
    } catch (Throwable reflectionException) {
      // TODO show appropriate log to add dependency
    }
  }

//  README:
//  The code in the following method is executed by reflection to skip the big
//  'com.google.android.play:core:1.8.0' dependency inside the SDK.
//
//  private static void showRatingFlow(Activity activity) {
//    ReviewManager manager = ReviewManagerFactory.create(activity);
//    Task<ReviewInfo> reviewFlow = manager.requestReviewFlow();
//    reviewFlow.addOnCompleteListener(new OnCompleteListener<ReviewInfo>() {
//      @Override
//      public void onComplete(@NonNull Task<ReviewInfo> task) {
//        ReviewInfo reviewInfo = task.getResult();
//        manager.launchReviewFlow(activity, reviewInfo);
//      }
//    });
//  }

  private static void showRatingFlow(Activity activity) throws Throwable {
    Toast.makeText(Leanplum.getContext(), "Rating flow to be shown", Toast.LENGTH_SHORT).show(); // TODO remove Toast when done

    // Invoke the App Rating flow using reflection
    Object manager = callStatic(
        "com.google.android.play.core.review.ReviewManagerFactory",
        "create",
        activity);

    Object reviewFlow = call(
        "com.google.android.play.core.review.ReviewManager",
        manager,
        "requestReviewFlow");

    Object onCompleteListenerProxy = createOnCompleteListenerProxy(activity, manager);

    call(
        "com.google.android.play.core.tasks.Task",
        reviewFlow,
        "addOnCompleteListener",
        onCompleteListenerProxy);
  }

  private static Object callStatic(String className, String methodName, Object... args) throws Throwable {
    return call(className, null, methodName, args);
  }

  private static Object call(String className, Object instance, String methodName, Object... args) throws Throwable {
    Class<?> clazz = Class.forName(className);
    for (Method m : clazz.getMethods()) {
      if (m.getName().equals(methodName)) { // assume no method overloads
        return m.invoke(instance, args);
      }
    }
    return null;
  }

  private static class OnCompleteListenerImpl implements InvocationHandler {
    private final Object activity;
    private final Object manager;
    private OnCompleteListenerImpl(Object activity, Object manager) {
      this.activity = activity;
      this.manager = manager;
    }
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if ("onComplete".equals(method.getName())) {
        Object task = args[0];
        Object reviewInfo = call("com.google.android.play.core.tasks.Task", task, "getResult");

        call(
            "com.google.android.play.core.review.ReviewManager",
            manager,
            "launchReviewFlow",
            activity,
            reviewInfo);
      }
      return null;
    }
  }

  private static Object createOnCompleteListenerProxy(Object activity, Object manager) throws Throwable {
    Class<?> clazz = Class.forName("com.google.android.play.core.tasks.OnCompleteListener");
    return Proxy.newProxyInstance(
        clazz.getClassLoader(),
        new Class[] {clazz},
        new OnCompleteListenerImpl(activity, manager));
  }
}
