package com.leanplum.messagetemplates;

public class MessageTemplateConstants {
  public static class Args {
    // Open URL
    public static final String URL = "URL";

    // Alert/confirm arguments.
    public static final String TITLE = "Title";
    public static final String MESSAGE = "Message";
    public static final String ACCEPT_TEXT = "Accept text";
    public static final String CANCEL_TEXT = "Cancel text";
    public static final String DISMISS_TEXT = "Dismiss text";
    public static final String ACCEPT_ACTION = "Accept action";
    public static final String CANCEL_ACTION = "Cancel action";
    public static final String DISMISS_ACTION = "Dismiss action";

    // Center popup/interstitial arguments.
    public static final String TITLE_TEXT = "Title.Text";
    public static final String TITLE_COLOR = "Title.Color";
    public static final String MESSAGE_TEXT = "Message.Text";
    public static final String MESSAGE_COLOR = "Message.Color";
    public static final String ACCEPT_BUTTON_TEXT = "Accept button.Text";
    public static final String ACCEPT_BUTTON_BACKGROUND_COLOR = "Accept button.Background color";
    public static final String ACCEPT_BUTTON_TEXT_COLOR = "Accept button.Text color";
    public static final String BACKGROUND_IMAGE = "Background image";
    public static final String BACKGROUND_COLOR = "Background color";
    public static final String LAYOUT_WIDTH = "Layout.Width";
    public static final String LAYOUT_HEIGHT = "Layout.Height";
    public static final String HTML_WIDTH = "HTML Width";
    public static final String HTML_HEIGHT = "HTML Height";
    public static final String HTML_Y_OFFSET = "HTML Y Offset";
    public static final String HTML_TAP_OUTSIDE_TO_CLOSE = "Tap Outside to Close";
    public static final String HTML_ALIGN = "HTML Align";
    public static final String HTML_ALIGN_TOP = "Top";
    public static final String HTML_ALIGN_BOTTOM = "Bottom";

    // Web interstitial arguments.
    public static final String CLOSE_URL = "Close URL";
    public static final String HAS_DISMISS_BUTTON = "Has dismiss button";

    // HTML Template arguments.
    public static final String OPEN_URL = "Open URL";
    public static final String TRACK_URL = "Track URL";
    public static final String ACTION_URL = "Action URL";
    public static final String TRACK_ACTION_URL = "Track Action URL";
  }

  public static class Values {
    public static final String ALERT_MESSAGE = "Alert message goes here.";
    public static final String CONFIRM_MESSAGE = "Confirmation message goes here.";
    public static final String POPUP_MESSAGE = "Popup message goes here.";
    public static final String INTERSTITIAL_MESSAGE = "Interstitial message goes here.";
    public static final String OK_TEXT = "OK";
    public static final String YES_TEXT = "Yes";
    public static final String NO_TEXT = "No";
    public static final int CENTER_POPUP_WIDTH = 300;
    public static final int CENTER_POPUP_HEIGHT = 250;
    public static final int DEFAULT_HTML_HEIGHT = 0;
    public static final String DEFAULT_HTML_ALING = Args.HTML_ALIGN_TOP;

    // Open URL.
    public static final String DEFAULT_URL = "http://www.example.com";
    public static final String DEFAULT_BASE_URL = "http://leanplum/";
    // Web interstitial values.
    public static final String DEFAULT_CLOSE_URL = DEFAULT_BASE_URL + "close";
    public static final boolean DEFAULT_HAS_DISMISS_BUTTON = true;

    // HTML Template values.
    public static final String FILE_PREFIX = "__file__";
    public static final String HTML_TEMPLATE_PREFIX = "__file__Template";
    public static final String DEFAULT_OPEN_URL = DEFAULT_BASE_URL + "loadFinished";
    public static final String DEFAULT_TRACK_URL = DEFAULT_BASE_URL + "track";
    public static final String DEFAULT_ACTION_URL = DEFAULT_BASE_URL + "runAction";
    public static final String DEFAULT_TRACK_ACTION_URL = DEFAULT_BASE_URL + "runTrackedAction";

  }
}
