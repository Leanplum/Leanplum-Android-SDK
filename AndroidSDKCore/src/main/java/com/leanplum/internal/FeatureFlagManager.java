package com.leanplum.internal;

import android.support.annotation.VisibleForTesting;

import java.util.HashSet;

public class FeatureFlagManager {
    public static final FeatureFlagManager INSTANCE = new FeatureFlagManager();

    private HashSet<String> enabledFeatureFlags = new HashSet<>();

    public void setEnabledFeatureFlags(HashSet<String> enabledFeatureFlags) {
        this.enabledFeatureFlags = enabledFeatureFlags;
    }

    public Boolean isFeatureFlagEnabled(String featureFlagName) {
        return this.enabledFeatureFlags.contains(featureFlagName);
    }
}
