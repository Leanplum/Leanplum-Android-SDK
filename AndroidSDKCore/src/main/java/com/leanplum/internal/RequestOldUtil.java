package com.leanplum.internal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.leanplum.internal.RequestOld.UUID_KEY;

public class RequestOldUtil {

    public void setNewBatchUUID(List<Map<String, Object>> requests) {
        String uuid = UUID.randomUUID().toString();
        for (Map<String, Object> request : requests) {
            request.put(UUID_KEY, uuid);
        }
    }
}
