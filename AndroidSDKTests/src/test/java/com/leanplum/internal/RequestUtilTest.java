package com.leanplum.internal;

import android.app.Application;

import com.leanplum.Leanplum;
import com.leanplum.__setup.LeanplumTestApp;

import com.leanplum.internal.Request.RequestType;
import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(
        sdk = 16,
        application = LeanplumTestApp.class
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "org.json.*", "org.powermock.*"})
public class RequestUtilTest extends TestCase {
    private final String POST = "POST";
    /**
     * Runs before every test case.
     */
    @Before
    public void setUp() throws Exception {
        Application context = RuntimeEnvironment.application;
        assertNotNull(context);
        Leanplum.setApplicationContext(context);

        ShadowOperationQueue shadowOperationQueue = new ShadowOperationQueue();

        Field instance = OperationQueue.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(instance, shadowOperationQueue);
    }

    @Test
    public void testSetNewBatchUUID() {
        LeanplumEventDataManager.sharedInstance();

        Request request1 =
            new Request(this.POST, RequestBuilder.ACTION_START, RequestType.DEFAULT, null);
        RequestSender.getInstance().send(request1);
        Request request2 =
            new Request(this.POST, RequestBuilder.ACTION_TRACK, RequestType.DEFAULT, null);
        RequestSender.getInstance().send(request2);
        List<Map<String, Object>> unsentRequests1 =
            new RequestBatchFactory().getUnsentRequests(1.0);
        String oldUUID1 = (String) unsentRequests1.get(0).get(Constants.Params.UUID);

        List<Map<String, Object>> unsentRequests2 =
            new RequestBatchFactory().getUnsentRequests(1.0);
        String oldUUID2 = (String) unsentRequests2.get(0).get(Constants.Params.UUID);

        List<Map<String, Object>> unsentRequests3 =
            new RequestBatchFactory().getUnsentRequests(0.5);
        String newUUID = (String) unsentRequests3.get(0).get(Constants.Params.UUID);

        assertTrue(oldUUID1.equals(oldUUID2));
        assertFalse(oldUUID1.equals(newUUID));
    }
}
