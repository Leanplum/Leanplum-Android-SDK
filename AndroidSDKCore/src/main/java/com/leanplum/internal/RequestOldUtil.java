package com.leanplum.internal;

import android.content.Context;
import android.content.SharedPreferences;

import com.leanplum.Leanplum;
import com.leanplum.utils.SharedPreferencesUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.leanplum.internal.RequestOld.LEANPLUM;
import static com.leanplum.internal.RequestOld.MAX_EVENTS_PER_API_CALL;
import static com.leanplum.internal.RequestOld.UUID_KEY;

public class RequestOldUtil {

    public String getStoredBatchUUID() {
        Context context = Leanplum.getContext();
        SharedPreferences preferences = context.getSharedPreferences(
                LEANPLUM, Context.MODE_PRIVATE);
        long count = LeanplumEventDataManager.getEventsCount();
        String uuid = preferences.getString(Constants.Defaults.UUID_KEY, null);
        if (uuid == null || count % MAX_EVENTS_PER_API_CALL == 0) {
            uuid = generateAndStoreBatchUUID();
        }
        return uuid;
    }

    public String generateAndStoreBatchUUID() {
        String uuid = UUID.randomUUID().toString();
        Context context = Leanplum.getContext();
        SharedPreferences preferences = context.getSharedPreferences(
                LEANPLUM, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.Defaults.UUID_KEY, uuid);
        SharedPreferencesUtil.commitChanges(editor);
        return uuid;
    }

    public void removeStoredBatchUUID() {
        Context context = Leanplum.getContext();
        SharedPreferences preferences = context.getSharedPreferences(
                LEANPLUM, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(Constants.Defaults.UUID_KEY);
        SharedPreferencesUtil.commitChanges(editor);
    }

    public void setNewBatchUUIDForRequests(List<Map<String, Object>> requests, String uuid) {
        for (Map<String, Object> request : requests) {
            request.put(UUID_KEY, uuid);
        }
    }
}
