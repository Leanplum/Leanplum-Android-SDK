package com.leanplum;

import com.leanplum.__setup.AbstractTest;
import com.leanplum._whitebox.utilities.ResponseHelper;
import com.leanplum.internal.ActionManager;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests in-app message triggers.
 */
public class LeanplumInAppMessageTriggerTest extends AbstractTest {
  @Test
  public void testTriggerOnStart() throws Exception {
    final String messageId = "12345";
    ActionManager actionManager = ActionManager.getInstance();
    assertEquals(0, actionManager.getMessageTriggerOccurrences(messageId));
    assertTrue(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());

    setupSDK(mContext, "/responses/start_message_response.json");

    // Assert the trigger and message impression occurred.
    assertEquals(1, actionManager.getMessageTriggerOccurrences(messageId));
    assertFalse(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());
  }

  @Test
  public void testTriggerOnStartOrResumeBackground() throws Exception {
    final String messageId = "12345";
    ActionManager actionManager = ActionManager.getInstance();
    assertEquals(0, actionManager.getMessageTriggerOccurrences(messageId));
    assertTrue(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());

    // Start in background.
    setupSDK(mContext, "/responses/resume_message_response.json");

    // Assert the trigger and message impression occurred after start.
    assertEquals(1, actionManager.getMessageTriggerOccurrences(messageId));
    assertEquals(3, actionManager.getMessageImpressionOccurrences(messageId).size());
    assertEquals(0L, actionManager.getMessageImpressionOccurrences(messageId).get("max"));

    Leanplum.pause();
    Leanplum.resume();
    // Assert the trigger and message impression didn't occur after first resume.
    assertEquals(1, actionManager.getMessageTriggerOccurrences(messageId));
    assertEquals(3, actionManager.getMessageImpressionOccurrences(messageId).size());
    assertEquals(0L, actionManager.getMessageImpressionOccurrences(messageId).get("max"));

    Leanplum.pause();
    Leanplum.resume();
    // Assert the trigger and message impression occurred after second resume.
    assertEquals(2, actionManager.getMessageTriggerOccurrences(messageId));
    assertEquals(4, actionManager.getMessageImpressionOccurrences(messageId).size());
    assertEquals(1L, actionManager.getMessageImpressionOccurrences(messageId).get("max"));
  }

  @Test
  public void testTriggerOnStartOrResumeForeground() throws Exception {
    final String messageId = "12345";
    ActionManager actionManager = ActionManager.getInstance();
    assertEquals(0, actionManager.getMessageTriggerOccurrences(messageId));
    assertTrue(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());

    ResponseHelper.seedResponse("/responses/resume_message_response.json");
    // Start in foreground so that the message can be triggered on resumeSession.
    Leanplum.start(mContext, null, null, null, false);

    // Assert the trigger and message impression occurred after start.
    assertEquals(1, actionManager.getMessageTriggerOccurrences(messageId));
    assertEquals(3, actionManager.getMessageImpressionOccurrences(messageId).size());
    assertEquals(0L, actionManager.getMessageImpressionOccurrences(messageId).get("max"));

    Leanplum.pause();
    Leanplum.resume();
    // Assert the trigger and message impression occurred after resume after resume.
    assertEquals(2, actionManager.getMessageTriggerOccurrences(messageId));
    assertEquals(4, actionManager.getMessageImpressionOccurrences(messageId).size());
    assertEquals(1L, actionManager.getMessageImpressionOccurrences(messageId).get("max"));

    Leanplum.pause();
    Leanplum.resume();
    // Assert the trigger and message impression occurrence incremented after resume.
    assertEquals(3, actionManager.getMessageTriggerOccurrences(messageId));
    assertEquals(5, actionManager.getMessageImpressionOccurrences(messageId).size());
    assertEquals(2L, actionManager.getMessageImpressionOccurrences(messageId).get("max"));
  }

  @Test
  public void testTriggerOnAdvance() throws Exception {
    final String messageId = "12346";
    final String state = "Registered";
    ActionManager actionManager = ActionManager.getInstance();
    assertEquals(0, actionManager.getMessageTriggerOccurrences(messageId));
    assertTrue(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());

    setupSDK(mContext, "/responses/start_message_response.json");
    Leanplum.advanceTo(state);
    // Assert the trigger and message impression occurred after advanced to state.
    assertEquals(1, actionManager.getMessageTriggerOccurrences(messageId));
    assertFalse(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());
  }

  @Test
  public void testTriggerOnAttributeChange() throws Exception {
    final String messageId = "12347";
    final String attribute = "Nickname";
    final  String attributeValue = "Android";
    ActionManager actionManager = ActionManager.getInstance();
    assertEquals(0, actionManager.getMessageTriggerOccurrences(messageId));
    assertTrue(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());

    setupSDK(mContext, "/responses/start_message_response.json");
    Leanplum.setUserAttributes(new HashMap<String, Object>(){{
      put(attribute, attributeValue);
    }});
    // Assert the trigger and message impression occurred after attribute changed.
    assertEquals(1, actionManager.getMessageTriggerOccurrences(messageId));
    assertFalse(actionManager.getMessageImpressionOccurrences(messageId).isEmpty());
  }
}
