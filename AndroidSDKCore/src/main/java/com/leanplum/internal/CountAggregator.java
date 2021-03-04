package com.leanplum.internal;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Counter is not currently used and will be removed in the future.
 * It is still functional both on client and server side.
 */
public class CountAggregator {
    private Set<String> enabledCounters = new HashSet<>();
    private final Map<String, Integer> counts = new HashMap<>();

    public void setEnabledCounters(Set<String> enabledCounters) {
        this.enabledCounters = enabledCounters;
    }

    public void incrementCount(@NonNull String name) {
        incrementCount(name, 1);
    }

    public void incrementCount(@NonNull String name, int incrementCount) {
        if (enabledCounters.contains(name)) {
            Integer count = 0;
            if (counts.containsKey(name)) {
                count = counts.get(name);
            }
            count = count + incrementCount;
            counts.put(name, count);
        }
    }

    @VisibleForTesting
    public Map<String, Integer> getAndClearCounts() {
        Map<String, Integer> previousCounts = new HashMap<>();
        previousCounts.putAll(counts);
        counts.clear();
        return previousCounts;
    }

    @VisibleForTesting
    public Map<String, Object> makeParams(@NonNull String name, int count) {
        Map<String, Object> params = new HashMap<>();

        params.put(Constants.Params.TYPE, Constants.Values.SDK_COUNT);
        params.put(Constants.Params.NAME, name);
        params.put(Constants.Params.COUNT, count);

        return params;
    }

    public void sendAllCounts() {
        Map<String, Integer> counts = getAndClearCounts();

        for(Map.Entry<String, Integer> entry : counts.entrySet()) {
            String name = entry.getKey();
            Integer count = entry.getValue();
            Map<String, Object> params = makeParams(name, count);
            try {
                Request request = RequestBuilder.withLogAction().andParams(params).create();
                RequestSender.getInstance().send(request);
            } catch (Throwable t) {
                android.util.Log.e("Leanplum", "Unable to send count.", t);
            }
        }
    }

    public Map<String, Integer> getCounts() {
        return counts;
    }
}
