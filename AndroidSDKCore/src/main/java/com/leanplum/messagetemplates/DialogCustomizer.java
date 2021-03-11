package com.leanplum.messagetemplates;

import android.app.Dialog;
import android.view.View;

/**
 * Interface that provides access to the view and dialog objects of the main templates before
 * drawing them. Called just before
 * {@link Dialog#setContentView(android.view.View, android.view.ViewGroup.LayoutParams)}.
 *
 * You can use it to change window flags.
 */
public interface DialogCustomizer {

  /**
   * Customize "Center Popup" template.
   *
   * @param messageDialog Dialog container of this message
   * @param messageContent Content view of the Dialog object
   */
  default void customizeCenterPopup(Dialog messageDialog, View messageContent) {
  }

  /**
   * Customize "Interstitial" template.
   *
   * @param messageDialog Dialog container of this message
   * @param messageContent Content view of the Dialog object
   */
  default void customizeInterstitial(Dialog messageDialog, View messageContent) {
  }

  /**
   * Customize "Web Interstitial" template.
   *
   * @param messageDialog Dialog container of this message
   * @param messageContent Content view of the Dialog object
   */
  default void customizeWebInterstitial(Dialog messageDialog, View messageContent) {
  }

  /**
   * Customize "Rich Interstitial" template.
   *
   * @param messageDialog Dialog container of this message
   * @param messageContent Content view of the Dialog object
   */
  default void customizeRichInterstitial(Dialog messageDialog, View messageContent) {
  }

  /**
   * Customize "Banner" template.
   *
   * @param messageDialog Dialog container of this message
   * @param messageContent Content view of the Dialog object
   */
  default void customizeBanner(Dialog messageDialog, View messageContent) {
  }
}
