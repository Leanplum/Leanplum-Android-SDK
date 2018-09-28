package com.leanplum.internal;

import android.support.annotation.VisibleForTesting;

import java.util.HashSet;
import java.util.Set;

public class FeatureFlagManager {
    public static final FeatureFlagManager INSTANCE = new FeatureFlagManager();

    private Set<String> enabledFeatureFlags = new HashSet<>();

    @VisibleForTesting
    FeatureFlagManager() {
        super();
    }

    public void setEnabledFeatureFlags(Set<String> enabledFeatureFlags) {
        this.enabledFeatureFlags = enabledFeatureFlags;
    }

    public Boolean isFeatureFlagEnabled(String featureFlagName) {
        return this.enabledFeatureFlags.contains(featureFlagName);
    }
}
