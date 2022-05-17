package com.leanplum.internal;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import com.leanplum.Leanplum;
import com.leanplum.__setup.AbstractTest;
import com.leanplum._whitebox.utilities.ImmediateRequestSender;
import java.lang.reflect.Field;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ChangeHostTest extends AbstractTest {

   @Before
   public void setUp() throws Exception {
      // Timer will call sendRequests because of the shadow operation queue
      // Replacing timer instance to prevent it
      RequestSenderTimer spiedTimer = Mockito.spy(RequestSenderTimer.get());
      Mockito.doNothing().when(spiedTimer).start();
      Field instance = RequestSenderTimer.class.getDeclaredField("INSTANCE");
      instance.setAccessible(true);
      instance.set(instance, spiedTimer);
   }

   @After
   public void tearDown() throws Exception {
      Field instance = RequestSenderTimer.class.getDeclaredField("INSTANCE");
      instance.setAccessible(true);
      instance.set(instance, new RequestSenderTimer());

      RequestSender.setInstance(new RequestSender());
   }

   @Test
   public void testApiHostResponse() throws Exception {
      RequestSender sender = Mockito.spy(ImmediateRequestSender.class);
      RequestSender.setInstance(sender);

      setupSDK(Leanplum.getContext(), "/responses/change_host_response.json");

      // verify number of calls
      verifyStatic(Mockito.times(2));
      Leanplum.setApiConnectionSettings(anyString(), anyString(), anyBoolean());
      verifyStatic(Mockito.times(2));
      Leanplum.setSocketConnectionSettings(anyString(), eq(443));

      // default case from setup method
      verifyStatic(Mockito.times(1));
      Leanplum.setApiConnectionSettings("www.leanplum.com", "api", true);
      verifyStatic(Mockito.times(1));
      Leanplum.setSocketConnectionSettings("dev.leanplum.com", 443);

      // changed config from request
      verifyStatic(Mockito.times(1));
      Leanplum.setApiConnectionSettings("api2.leanplum.com", "new-api", true);
      verifyStatic(Mockito.times(1));
      Leanplum.setSocketConnectionSettings("dev2.leanplum.com", 443);

      // verify sendRequests called for second time recursively
      Mockito.verify(sender, Mockito.times(2)).sendRequests();
   }

   @Test
   public void testSameConfigResponse() throws Exception {
      RequestSender sender = Mockito.spy(ImmediateRequestSender.class);
      RequestSender.setInstance(sender);

      // response file contains the default config, that is used in the setup method
      setupSDK(Leanplum.getContext(), "/responses/change_host_loop_response.json");

      // verify number of calls
      verifyStatic(Mockito.times(1));
      Leanplum.setApiConnectionSettings(anyString(), anyString(), eq(true));
      verifyStatic(Mockito.times(1));
      Leanplum.setSocketConnectionSettings(anyString(), eq(443));

      // default case from setup method
      verifyStatic(Mockito.times(1));
      Leanplum.setApiConnectionSettings("www.leanplum.com", "api", true);
      verifyStatic(Mockito.times(1));
      Leanplum.setSocketConnectionSettings("dev.leanplum.com", 443);

      // verify sendRequests called only once
      Mockito.verify(sender, Mockito.times(1)).sendRequests();
   }
}
