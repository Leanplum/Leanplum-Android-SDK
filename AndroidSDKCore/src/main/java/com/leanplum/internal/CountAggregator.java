package com.leanplum.internal;

import android.support.annotation.VisibleForTesting;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;

public class CountAggregator {
    public static final CountAggregator INSTANCE = new CountAggregator();

    private HashSet<String> enabledCounters = new HashSet<>();
    private final HashMap<String, Integer> counts = new HashMap<>();

    public void setEnabledCounters(HashSet<String> enabledCounters) {
        this.enabledCounters = enabledCounters;
    }

    public void incrementCount(String name) {
        incrementCount(name, 1);
    }

    public void incrementCount(String name, int incrementCount) {
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
    public HashMap<String, Integer> getAndClearCounts() {
        HashMap<String, Integer> previousCounts = new HashMap<>();
        previousCounts.putAll(counts);
        counts.clear();
        return previousCounts;
    }

    @VisibleForTesting
    public HashMap<String, Object> makeParams(String name, int count) {
        HashMap<String, Object> params = new HashMap<>();

        params.put(Constants.Params.TYPE, Constants.Values.SDK_COUNT);
        params.put(Constants.Params.MESSAGE, name);
        params.put(Constants.Params.COUNT, count);

        return params;
    }

    public void sendAllCounts() {
        HashMap<String, Integer> counts = getAndClearCounts();

        for(Map.Entry<String, Integer> entry : counts.entrySet()) {
            String name = entry.getKey();
            Integer count = entry.getValue();
            HashMap<String, Object> params = makeParams(name, count);
            try {
                Request.post(Constants.Methods.LOG, params).sendEventually();
            } catch (Throwable t) {
                android.util.Log.e("Leanplum", "Unable to send count.", t);
            }
        }
    }

    public HashMap<String, Integer> getCounts() {
        return counts;
    }
}
