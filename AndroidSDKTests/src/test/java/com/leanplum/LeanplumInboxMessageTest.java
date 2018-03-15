package com.leanplum;

import com.leanplum.__setup.AbstractTest;
import com.leanplum._whitebox.utilities.RequestHelper;
import com.leanplum._whitebox.utilities.ResponseHelper;
import com.leanplum.internal.Constants;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * Created by sayaan on 3/15/18.
 */

public class LeanplumInboxMessageTest extends AbstractTest {
  @Before
  public void setUp() {
    spy(LeanplumInbox.class);
    setupSDK(mContext, "/responses/simple_start_response.json");
  }

  @Test
  public void testMessageCreate() {
    Date delivery = new Date(100);
    Date expiration = new Date(200);
    HashMap<String, Object> map = new HashMap<>();
    map.put(Constants.Keys.MESSAGE_DATA, new HashMap<String, Object>());
    map.put(Constants.Keys.DELIVERY_TIMESTAMP, delivery.getTime());
    map.put(Constants.Keys.EXPIRATION_TIMESTAMP, expiration.getTime());
    map.put(Constants.Keys.IS_READ, true);

    LeanplumInboxMessage message = LeanplumInboxMessage.createFromJsonMap("message##Id", map);
    assertEquals("message##Id", message.getMessageId());
    assertEquals(delivery, message.getDeliveryTimestamp());
    assertEquals(expiration, message.getExpirationTimestamp());
    assertTrue(message.isRead());
    assertNull(message.getData());

    assertNull(message.getImageFilePath());
    assertNull(message.getImageUrl());
  }

  @Test
  public void testReadAndUpdateMessageCount() {
    Date delivery = new Date(100);
    Date expiration = new Date(200);
    HashMap<String, Object> map = new HashMap<>();
    map.put(Constants.Keys.MESSAGE_DATA, new HashMap<String, Object>());
    map.put(Constants.Keys.DELIVERY_TIMESTAMP, delivery.getTime());
    map.put(Constants.Keys.EXPIRATION_TIMESTAMP, expiration.getTime());
    map.put(Constants.Keys.IS_READ, false);

    LeanplumInboxMessage message = LeanplumInboxMessage
        .createFromJsonMap("messageId##00", map);

    assertEquals(false, message.isRead());
    message.read();
    assertEquals(true, message.isRead());
  }

  @Test
  public void testRemoveMessage() {
    // Need to load a message into inbox with seeding a response
    ResponseHelper.seedResponse("/responses/newsfeed_response.json");
    Leanplum.getInbox().downloadMessages();
    assertEquals(2, Leanplum.getInbox().count());
    List<String> messageIds = Leanplum.getInbox().messagesIds();
    assertNotNull(messageIds);
    String idToBeRemoved = messageIds.get(1);
    Leanplum.getInbox().removeMessage(idToBeRemoved);
    assertEquals(1, Leanplum.getInbox().count());
  }
}
