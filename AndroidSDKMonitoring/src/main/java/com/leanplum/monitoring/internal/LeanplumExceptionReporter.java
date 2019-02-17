// Copyright 2018, Leanplum, Inc.
package com.leanplum.monitoring.internal;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.leanplum.Leanplum;
import com.leanplum.monitoring.ExceptionHandler;
import com.leanplum.monitoring.ExceptionReporting;

import java.util.ArrayList;
import java.util.List;

import main.java.com.mindscapehq.android.raygun4android.RaygunClient;
import main.java.com.mindscapehq.android.raygun4android.RaygunOnBeforeSend;
import main.java.com.mindscapehq.android.raygun4android.messages.RaygunErrorMessage;
import main.java.com.mindscapehq.android.raygun4android.messages.RaygunErrorStackTraceLineMessage;
import main.java.com.mindscapehq.android.raygun4android.messages.RaygunMessage;
import main.java.com.mindscapehq.android.raygun4android.messages.RaygunMessageDetails;

/**
 * Raygun Exception Reporter.
 *
 * This class is instantiated through reflection.
 * Don't change the package path without updating
 * the reflection.
 *
 * @author Mayank Sanganeria
 */
public class LeanplumExceptionReporter implements ExceptionReporting, RaygunOnBeforeSend {
    private Context context;

    private static final LeanplumExceptionReporter instance = new LeanplumExceptionReporter();

    static  {
        ExceptionHandler.getInstance().exceptionReporter = LeanplumExceptionReporter.instance;
    }

    public LeanplumExceptionReporter() {
    }

    public void setContext(Context context) {
        this.context = context;
        if (RaygunClient.getApiKey() == null) {
            RaygunClient.init(context, APIKeys.RAYGUN_API_KEY);
            RaygunClient.setOnBeforeSend(this);
            RaygunClient.attachExceptionHandler();
        }
    }

    public void reportException(Throwable t) {
        List<Object> tags = new ArrayList<>();
        RaygunClient.send(t, tags);
    }

    @Override
    public RaygunMessage onBeforeSend(RaygunMessage message) {
        try {
            RaygunMessageDetails details = message.getDetails();

            List<Object> tags = details.getTags();
            if (tags == null) {
                tags = new ArrayList<>();
            }
            tags.add(getAppName());
            tags.add(getAppVersion());
            details.setTags(tags);

            if (doesErrorIncludeLeanplum(details.getError())) {
                return message;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Deprecated
    @Override
    public RaygunMessage OnBeforeSend(RaygunMessage message) {
        return onBeforeSend(message);
    }

    private boolean doesErrorIncludeLeanplum(RaygunErrorMessage errorMessage) {
        return containsLeanplum(errorMessage.getMessage())
                || doesStackTraceIncludeLeanplum(errorMessage.getStackTrace())
                || containsLeanplum(errorMessage.getClassName());
    }

    private boolean doesStackTraceIncludeLeanplum(RaygunErrorStackTraceLineMessage[] stacktrace) {
        if (stacktrace != null) {
            for (RaygunErrorStackTraceLineMessage line : stacktrace) {
                if (containsLeanplum(line.getClassName())
                        || containsLeanplum(line.getFileName())
                        || containsLeanplum(line.getMethodName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getAppName() {
        String appName = "unknown_app";
        try {
            appName = context.getPackageName();
        } catch (Throwable ignored) {
        }
        return appName;
    }

    private String getAppVersion() {
        String version = "unknown_version";
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (Throwable ignored) {
        }
        return version;
    }

    private boolean containsLeanplum(String string) {
        return string.toLowerCase().contains("leanplum");
    }
}
