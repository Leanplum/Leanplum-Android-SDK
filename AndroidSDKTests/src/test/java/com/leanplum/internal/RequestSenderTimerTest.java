package com.leanplum.internal;

import com.leanplum.EventsUploadInterval;
import static org.junit.Assert.*;

import com.leanplum.__setup.AbstractTest;
import org.junit.Test;
import org.mockito.Mockito;

public class RequestSenderTimerTest extends AbstractTest {

  private boolean operationExecuted;

  @Test
  public void testDefaultInterval() {
    RequestSenderTimer timer = new RequestSenderTimer();
    assertEquals(15*60*1000, timer.getIntervalMillis()); // default is 15min
  }

  @Test
  public void testInterval() {
    RequestSenderTimer timer = new RequestSenderTimer();
    timer.setTimerInterval(EventsUploadInterval.AT_MOST_5_MINUTES);
    assertEquals(5*60*1000, timer.getIntervalMillis());

    timer.setTimerInterval(EventsUploadInterval.AT_MOST_10_MINUTES);
    assertEquals(10*60*1000, timer.getIntervalMillis());

    timer.setTimerInterval(EventsUploadInterval.AT_MOST_15_MINUTES);
    assertEquals(15*60*1000, timer.getIntervalMillis());
  }

  /**
   * Tests if timer operation is scheduled successfully.
   */
  @Test
  public void testStartTimer() {
    Runnable timerOperation = new Runnable() {
      @Override
      public void run() {
        operationExecuted = true;
      }
    };

    RequestSenderTimer timer = Mockito.spy(RequestSenderTimer.class);
    Mockito.when(timer.createTimerOperation()).thenReturn(timerOperation);

    operationExecuted = false;
    timer.start();
    assertTrue(operationExecuted);
  }
}
