/*
 * Copyright 2017, Leanplum, Inc. All rights reserved.
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
package com.leanplum.messagetemplates;

import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.__setup.AbstractTest;
import com.leanplum.activities.LeanplumTestActivity;

import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * @author Milos Jakovljevic
 */
@PrepareForTest({MessageTemplates.class, HTMLOptions.class})
public class LeanplumMessageTemplatesTest extends AbstractTest {

  @Override
  public void before() throws Exception {
    super.before();
    spy(MessageTemplates.class);
  }

  @Test
  public void testCenterPopup() throws Exception {
    LeanplumTestActivity activity = Robolectric.buildActivity(LeanplumTestActivity.class).
        create().start().resume().visible().get();
    setActivityVisibility(activity);
    Leanplum.setApplicationContext(activity);

    HashMap<String, Object> map = new HashMap<>();
    map.put(MessageTemplates.Args.TITLE_TEXT, "text");
    map.put(MessageTemplates.Args.TITLE_COLOR, 100);
    map.put(MessageTemplates.Args.MESSAGE_TEXT, "message_text");
    map.put(MessageTemplates.Args.MESSAGE_COLOR, 1200);
    map.put(MessageTemplates.Args.BACKGROUND_COLOR, 123);
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_TEXT, "button_text");
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_BACKGROUND_COLOR, 123);
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_TEXT_COLOR, 123);
    map.put(MessageTemplates.Args.LAYOUT_WIDTH, 128);
    map.put(MessageTemplates.Args.LAYOUT_HEIGHT, 128);

    ActionContext actionContext = new ActionContext("center_popup", map, "message_id");
    CenterPopupOptions options = new CenterPopupOptions(actionContext);
    CenterPopup centerpopup = new CenterPopup(activity, options);
    assertNotNull(centerpopup);
    assertEquals(options, centerpopup.options);
  }

  @Test
  public void testHTML() throws Exception {
    spy(HTMLOptions.class);
    PowerMockito.doReturn("<body></body>").when(HTMLOptions.class, "getTemplate", anyObject());

    LeanplumTestActivity activity = Robolectric.buildActivity(LeanplumTestActivity.class).
        create().start().resume().visible().get();
    setActivityVisibility(activity);
    Leanplum.setApplicationContext(activity);

    HashMap<String, Object> map = new HashMap<>();
    map.put(MessageTemplates.Args.TITLE_TEXT, "text");
    map.put(MessageTemplates.Args.TITLE_COLOR, 100);
    map.put(MessageTemplates.Args.MESSAGE_TEXT, "message_text");
    map.put(MessageTemplates.Args.MESSAGE_COLOR, 1200);
    map.put(MessageTemplates.Args.BACKGROUND_COLOR, 123);
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_TEXT, "button_text");
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_BACKGROUND_COLOR, 123);
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_TEXT_COLOR, 123);
    map.put(MessageTemplates.Args.LAYOUT_WIDTH, 128);
    map.put(MessageTemplates.Args.LAYOUT_HEIGHT, 128);
    map.put(MessageTemplates.Args.CLOSE_URL, "www.google.com");
    map.put(MessageTemplates.Args.OPEN_URL, "www.google.com");
    map.put(MessageTemplates.Args.TRACK_URL, "www.google.com");
    map.put(MessageTemplates.Args.ACTION_URL, "www.google.com");
    map.put(MessageTemplates.Args.TRACK_ACTION_URL, "www.google.com");
    map.put(MessageTemplates.Args.HTML_ALIGN, "top");
    map.put(MessageTemplates.Args.HTML_HEIGHT, 100);
    map.put(MessageTemplates.Values.HTML_TEMPLATE_PREFIX, "file");

    ActionContext actionContext = new ActionContext("center_popup", map, "message_id");
    HTMLOptions options = new HTMLOptions(actionContext);
    HTMLTemplate htmlTemplate = new HTMLTemplate(activity, options);
    assertNotNull(htmlTemplate);
    assertEquals(options, htmlTemplate.htmlOptions);
  }

  @Test
  public void testInterstitial() throws Exception {
    LeanplumTestActivity activity = Robolectric.buildActivity(LeanplumTestActivity.class).
        create().start().resume().visible().get();
    setActivityVisibility(activity);
    Leanplum.setApplicationContext(activity);

    HashMap<String, Object> map = new HashMap<>();
    map.put(MessageTemplates.Args.TITLE_TEXT, "text");
    map.put(MessageTemplates.Args.TITLE_COLOR, 100);
    map.put(MessageTemplates.Args.MESSAGE_TEXT, "message_text");
    map.put(MessageTemplates.Args.MESSAGE_COLOR, 1200);
    map.put(MessageTemplates.Args.BACKGROUND_COLOR, 123);
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_TEXT, "button_text");
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_BACKGROUND_COLOR, 123);
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_TEXT_COLOR, 123);
    map.put(MessageTemplates.Args.LAYOUT_WIDTH, 128);
    map.put(MessageTemplates.Args.LAYOUT_HEIGHT, 128);
    map.put(MessageTemplates.Args.CLOSE_URL, "www.google.com");
    map.put(MessageTemplates.Args.OPEN_URL, "www.google.com");
    map.put(MessageTemplates.Args.TRACK_URL, "www.google.com");
    map.put(MessageTemplates.Args.ACTION_URL, "www.google.com");
    map.put(MessageTemplates.Args.TRACK_ACTION_URL, "www.google.com");
    map.put(MessageTemplates.Args.HTML_ALIGN, "top");
    map.put(MessageTemplates.Args.HTML_HEIGHT, 100);
    map.put(MessageTemplates.Values.HTML_TEMPLATE_PREFIX, "file");

    ActionContext actionContext = new ActionContext("center_popup", map, "message_id");
    InterstitialOptions options = new InterstitialOptions(actionContext);
    Interstitial interstitial = new Interstitial(activity, options);
    assertNotNull(interstitial);
    assertEquals(options, interstitial.options);
  }

  @Test
  public void testWebInterstitial() throws Exception {
    LeanplumTestActivity activity = Robolectric.buildActivity(LeanplumTestActivity.class).
        create().start().resume().visible().get();
    setActivityVisibility(activity);
    Leanplum.setApplicationContext(activity);

    HashMap<String, Object> map = new HashMap<>();
    map.put(MessageTemplates.Args.TITLE_TEXT, "text");
    map.put(MessageTemplates.Args.TITLE_COLOR, 100);
    map.put(MessageTemplates.Args.MESSAGE_TEXT, "message_text");
    map.put(MessageTemplates.Args.MESSAGE_COLOR, 1200);
    map.put(MessageTemplates.Args.BACKGROUND_COLOR, 123);
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_TEXT, "button_text");
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_BACKGROUND_COLOR, 123);
    map.put(MessageTemplates.Args.ACCEPT_BUTTON_TEXT_COLOR, 123);
    map.put(MessageTemplates.Args.LAYOUT_WIDTH, 128);
    map.put(MessageTemplates.Args.LAYOUT_HEIGHT, 128);
    map.put(MessageTemplates.Args.CLOSE_URL, "www.google.com");
    map.put(MessageTemplates.Args.OPEN_URL, "www.google.com");
    map.put(MessageTemplates.Args.TRACK_URL, "www.google.com");
    map.put(MessageTemplates.Args.ACTION_URL, "www.google.com");
    map.put(MessageTemplates.Args.TRACK_ACTION_URL, "www.google.com");
    map.put(MessageTemplates.Args.HTML_ALIGN, "top");
    map.put(MessageTemplates.Args.HTML_HEIGHT, 100);
    map.put(MessageTemplates.Args.URL, "www.google.com");
    map.put(MessageTemplates.Args.HAS_DISMISS_BUTTON, true);
    map.put(MessageTemplates.Args.CLOSE_URL, "www.leanplum.com");

    ActionContext actionContext = new ActionContext("center_popup", map, "message_id");
    WebInterstitialOptions options = new WebInterstitialOptions(actionContext);
    WebInterstitial interstitial = new WebInterstitial(activity, options);
    assertNotNull(interstitial);
    assertEquals(options, interstitial.webOptions);
  }
}
