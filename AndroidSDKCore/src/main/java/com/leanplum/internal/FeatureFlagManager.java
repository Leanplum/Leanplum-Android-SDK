package com.leanplum.internal;

import androidx.annotation.VisibleForTesting;

import com.leanplum.Leanplum;

import java.util.HashSet;
import java.util.Set;

public class FeatureFlagManager {
    public static final FeatureFlagManager INSTANCE = new FeatureFlagManager();

    public static final String FEATURE_FLAG_REQUEST_REFACTOR = "request_refactor";

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
