/*
 * Copyright 2020, Leanplum, Inc. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.leanplum.messagetemplates.options;

import android.app.Activity;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.Log;

import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.messagetemplates.MessageTemplateConstants.Args;
import com.leanplum.messagetemplates.MessageTemplateConstants.Values;
import com.leanplum.messagetemplates.controllers.RichHtmlController;
import com.leanplum.utils.SizeUtil;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Options used by {@link RichHtmlController}.
 *
 * @author Anna Orlova
 */
public class RichHtmlOptions {
  private String closeUrl;
  private String openUrl;
  private String trackUrl;
  private String actionUrl;
  private String trackActionUrl;
  private String htmlTemplate;
  private ActionContext actionContext;
  private String htmlAlign;
  private int htmlHeight;
  private Size htmlWidth;
  private Size htmlYOffset;
  private boolean htmlTabOutsideToClose;

  public RichHtmlOptions(ActionContext context) {
    this.setActionContext(context);
    this.setHtmlTemplate(getTemplate(context));
    this.setCloseUrl(context.stringNamed(Args.CLOSE_URL));
    this.setOpenUrl(context.stringNamed(Args.OPEN_URL));
    this.setTrackUrl(context.stringNamed(Args.TRACK_URL));
    this.setActionUrl(context.stringNamed(Args.ACTION_URL));
    this.setTrackActionUrl(context.stringNamed(Args.TRACK_ACTION_URL));
    this.setHtmlAlign(context.stringNamed(Args.HTML_ALIGN));
    this.setHtmlHeight(context.numberNamed(Args.HTML_HEIGHT).intValue());
    this.setHtmlWidth(context.stringNamed(Args.HTML_WIDTH));
    this.setHtmlYOffset(context.stringNamed(Args.HTML_Y_OFFSET));
    this.setHtmlTabOutsideToClose(context.booleanNamed(Args.HTML_TAP_OUTSIDE_TO_CLOSE));
  }

  /**
   * Read data from file as String.
   *
   * @param context ActionContext.
   * @param name Name of file.
   * @return String String with data of file.
   */
  @SuppressWarnings("SameParameterValue")
  private static String readFileAsString(ActionContext context, String name) {
    if (context == null || TextUtils.isEmpty(name)) {
      return null;
    }

    String str;
    InputStream inputStream = context.streamNamed(name);
    StringBuilder buf = new StringBuilder();
    BufferedReader reader = null;

    try {
      reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
      while ((str = reader.readLine()) != null) {
        buf.append(str).append("\n");
      }
      reader.close();
    } catch (IOException e) {
      Log.e("Leanplum", "Fail to get HTML template.");
    } finally {
      try {
        if (inputStream != null) {
          inputStream.close();
        }
        if (reader != null) {
          reader.close();
        }
      } catch (Exception e) {
        Log.w("Leanplum", "Failed to close InputStream or BufferedReader: " + e);
      }
    }
    return buf.toString();
  }

  /**
   * Replace all keys with __file__ prefix to keys without __file__ prefix and replace value of
   * those keys with local path to file.
   *
   * @param map Map with arguments from ActionContext.
   * @param htmlTemplateName Name of file with HTML template.
   * @return Map Map with updated arguments.
   */
  private static Map<String, Object> replaceFileToLocalPath(Map<String, Object> map,
      String htmlTemplateName) {
    if (map == null) {
      return null;
    }
    String[] keyArray = map.keySet().toArray(new String[map.keySet().size()]);
    for (String key : keyArray) {
      if (map.get(key) instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> mapValue = (Map<String, Object>) map.get(key);
        replaceFileToLocalPath(mapValue, htmlTemplateName);
      } else if (key.contains(Values.FILE_PREFIX) &&
          !key.equals(htmlTemplateName)) {
        String filePath = ActionContext.filePath((String) map.get(key));
        if (filePath == null) {
          continue;
        }
        File f = new File(filePath);
        String localPath = "file://" + f.getAbsolutePath();
        if (localPath.contains(Leanplum.getContext().getPackageName())) {
          map.put(key.replace(Values.FILE_PREFIX, ""),
              localPath.replace(" ", "%20"));
        }
        map.remove(key);
      }
    }
    return map;
  }

  /**
   * Get HTML template file.
   *
   * @param context ActionContext.
   * @return String String with data of HTML template file.
   */
  private static String getTemplate(ActionContext context) {
    if (context == null) {
      return null;
    }

    String htmlTemplate = readFileAsString(context, Values.HTML_TEMPLATE_PREFIX);
    Map<String, Object> htmlArgs = replaceFileToLocalPath(context.getArgs(),
        Values.HTML_TEMPLATE_PREFIX);
    if (htmlArgs == null || TextUtils.isEmpty(htmlTemplate)) {
      return null;
    }

    htmlArgs.put("messageId", context.getMessageId());
    if (context.getContextualValues() != null && context.getContextualValues().arguments != null) {
      htmlArgs.put("displayEvent", context.getContextualValues().arguments);
    }

    String htmlString = "";
    try {
      htmlString = (htmlTemplate.replace("##Vars##",
          ActionContext.mapToJsonObject(htmlArgs).toString()));
      try {
        htmlString = context.fillTemplate(htmlString);
      } catch (Throwable ignored) {
      }
    } catch (JSONException e) {
      Log.e("Leanplum", "Cannot convert map of arguments to JSON object.");
    } catch (Throwable t) {
      Log.e("Leanplum", "Cannot get html template.", t);
    }
    return htmlString.replace("\\/", "/");
  }

  /**
   * @return boolean True if it's full screen template.
   */
  public boolean isFullScreen() {
    return htmlHeight == 0;
  }

  public int getHtmlHeight() {
    return htmlHeight;
  }

  // Gets html width.
  public Size getHtmlWidth() {
    return htmlWidth;
  }

  private void setHtmlWidth(String htmlWidth) {
    this.htmlWidth = getSizeValueAndType(htmlWidth);
  }

  //Gets html y offset in pixels.
  public int getHtmlYOffset(Activity context) {
    int yOffset = 0;
    if (context == null) {
      return yOffset;
    }

    if (htmlYOffset != null && !TextUtils.isEmpty(htmlYOffset.type)) {
      yOffset = htmlYOffset.value;
      if ("%".equals(htmlYOffset.type)) {
        Point size = SizeUtil.getDisplaySize(context);
        yOffset = (size.y - SizeUtil.getStatusBarHeight(context)) * yOffset / 100;
      } else {
        yOffset = SizeUtil.dpToPx(context, yOffset);
      }
    }
    return yOffset;
  }

  private void setHtmlYOffset(String htmlYOffset) {
    this.htmlYOffset = getSizeValueAndType(htmlYOffset);
  }

  private Size getSizeValueAndType(String stringValue) {
    if (TextUtils.isEmpty(stringValue)) {
      return null;
    }

    Size out = new Size();
    if (stringValue.contains("px")) {
      String[] sizeValue = stringValue.split("px");
      if (sizeValue.length != 0) {
        out.value = Integer.parseInt(sizeValue[0]);
      }
      out.type = "px";
    } else if (stringValue.contains("%")) {
      String[] sizeValue = stringValue.split("%");
      if (sizeValue.length != 0) {
        out.value = Integer.parseInt(sizeValue[0]);
      }
      out.type = "%";
    }
    return out;
  }

  public boolean isHtmlTabOutsideToClose() {
    return htmlTabOutsideToClose;
  }

  private void setHtmlTabOutsideToClose(boolean htmlTabOutsideToClose) {
    this.htmlTabOutsideToClose = htmlTabOutsideToClose;
  }

  private void setHtmlHeight(int htmlHeight) {
    this.htmlHeight = htmlHeight;
  }

  public String getHtmlAlign() {
    return htmlAlign;
  }

  public boolean isHtmlAlignBottom() {
    return Args.HTML_ALIGN_BOTTOM.equals(getHtmlAlign());
  }

  private void setHtmlAlign(String htmlAlign) {
    this.htmlAlign = htmlAlign;
  }

  public ActionContext getActionContext() {
    return actionContext;
  }

  private void setActionContext(ActionContext actionContext) {
    //noinspection AccessStaticViaInstance
    this.actionContext = actionContext;
  }

  public String getHtmlTemplate() {
    return htmlTemplate;
  }

  private void setHtmlTemplate(String htmlTemplate) {
    this.htmlTemplate = htmlTemplate;
  }

  public String getTrackActionUrl() {
    return trackActionUrl;
  }

  private void setTrackActionUrl(String trackActionUrl) {
    this.trackActionUrl = trackActionUrl;
  }

  public String getTrackUrl() {
    return trackUrl;
  }

  private void setTrackUrl(String trackUrl) {
    this.trackUrl = trackUrl;
  }

  public String getOpenUrl() {
    return openUrl;
  }

  private void setOpenUrl(String openUrl) {
    this.openUrl = openUrl;
  }

  public String getActionUrl() {
    return actionUrl;
  }

  private void setActionUrl(String actionUrl) {
    this.actionUrl = actionUrl;
  }

  public String getCloseUrl() {
    return closeUrl;
  }

  private void setCloseUrl(String closeUrl) {
    this.closeUrl = closeUrl;
  }

  public static ActionArgs toArgs() {
    return new ActionArgs()
        .with(Args.CLOSE_URL, Values.DEFAULT_CLOSE_URL)
        .with(Args.OPEN_URL, Values.DEFAULT_OPEN_URL)
        .with(Args.ACTION_URL, Values.DEFAULT_ACTION_URL)
        .with(Args.TRACK_ACTION_URL, Values.DEFAULT_TRACK_ACTION_URL)
        .with(Args.TRACK_URL, Values.DEFAULT_TRACK_URL)
        .with(Args.HTML_ALIGN, Values.DEFAULT_HTML_ALING)
        .with(Args.HTML_HEIGHT, Values.DEFAULT_HTML_HEIGHT);
  }

  public static class Size {
    public int value;
    public String type;
  }

  /**
   * Banners with property TabOutsideToClose = false need to be treated differently
   * so they do not block interaction with other dialogs and the keyboard.
   * Banners with property TabOutsideToClose = true do not need to be treated this way.
   * The original way banners worked was fine because they need to be aware of any touch events
   * in its container window
   */
  public boolean isBannerWithTapOutsideFalse() {
    String templateName = getActionContext().getArgs().get(Values.HTML_TEMPLATE_PREFIX).toString();
    return templateName.toLowerCase().contains("banner") && !isHtmlTabOutsideToClose();
  }
}
