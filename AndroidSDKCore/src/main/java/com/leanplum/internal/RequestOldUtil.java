package com.leanplum.internal;

import android.content.Context;
import android.content.SharedPreferences;

import com.leanplum.Leanplum;
import com.leanplum.utils.SharedPreferencesUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.leanplum.internal.RequestOld.LEANPLUM;
import static com.leanplum.internal.RequestOld.UUID_KEY;

public class RequestOldUtil {

    public void setNewBatchUUID(List<Map<String, Object>> requests) {
        String uuid = UUID.randomUUID().toString();
        Context context = Leanplum.getContext();
        SharedPreferences preferences = context.getSharedPreferences(
                LEANPLUM, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.Defaults.UUID_KEY, uuid);
        SharedPreferencesUtil.commitChanges(editor);
        for (Map<String, Object> request : requests) {
            request.put(UUID_KEY, uuid);
        }
    }
}
