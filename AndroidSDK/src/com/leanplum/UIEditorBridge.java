// Copyright 2017, Leanplum, Inc.

package com.leanplum;

import com.leanplum.internal.FileManager;
import com.leanplum.internal.Socket;
import com.leanplum.internal.Util;
import com.leanplum.internal.VarCache;

import java.util.List;
import java.util.Map;

/**
 * Bridge class for the UI editor package to access LP internal methods.
 *
 * @author Ben Marten
 */
public class UIEditorBridge {
  public static void setInterfaceUpdateBlock(CacheUpdateBlock block) {
    VarCache.onInterfaceUpdate(block);
  }

  public static void setEventsUpdateBlock(CacheUpdateBlock block) {
    VarCache.onEventsUpdate(block);
  }

  public static List<Map<String, Object>> getUpdateRuleDiffs() {
    return VarCache.getUpdateRuleDiffs();
  }

  public static List<Map<String, Object>> getEventRuleDiffs() {
    return VarCache.getEventRuleDiffs();
  }

  public static boolean isSocketConnected() {
    return Socket.getInstance() != null && Socket.getInstance().isConnected();
  }

  public static <T> void socketSendEvent(String eventName, Map<String, T> data) {
    if (Socket.getInstance() != null && eventName != null) {
      Socket.getInstance().sendEvent(eventName, data);
    }
  }

  public static String fileRelativeToDocuments(String path) {
    return FileManager.fileRelativeToDocuments(path);
  }

  public static void handleException(Throwable t) {
    Util.handleException(t);
  }
}
