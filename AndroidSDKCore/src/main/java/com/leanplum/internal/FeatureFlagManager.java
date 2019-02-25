package com.leanplum.internal;

import androidx.annotation.VisibleForTesting;

import com.leanplum.Leanplum;

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
        Leanplum.countAggregator().incrementCount("is_feature_flag_enabled");
        return this.enabledFeatureFlags.contains(featureFlagName);
    }
}
