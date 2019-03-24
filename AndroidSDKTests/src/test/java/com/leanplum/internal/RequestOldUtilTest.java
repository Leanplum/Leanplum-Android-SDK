package com.leanplum.internal;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.leanplum.Leanplum;
import com.leanplum.__setup.LeanplumTestApp;
import com.leanplum._whitebox.utilities.SynchronousExecutor;
import com.leanplum.utils.SharedPreferencesUtil;

import junit.framework.TestCase;

import org.bouncycastle.cert.ocsp.Req;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;
import java.util.Map;

import static com.leanplum.internal.RequestOld.LEANPLUM;

@RunWith(RobolectricTestRunner.class)
@Config(
        sdk = 16,
        application = LeanplumTestApp.class
)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "org.json.*", "org.powermock.*"})
public class RequestOldUtilTest extends TestCase {
    private final String POST = "POST";
    /**
     * Runs before every test case.
     */
    @Before
    public void setUp() {
        Application context = RuntimeEnvironment.application;
        assertNotNull(context);
        Leanplum.setApplicationContext(context);

        // Mock this so async things run synchronously
        ReflectionHelpers.setStaticField(Util.class, "singleThreadExecutor", new SynchronousExecutor());
    }

    @Test
    public void testStoringBatchUUID() {
        RequestOldUtil util = new RequestOldUtil();
        String uuid = util.generateAndStoreBatchUUID();
        assertTrue(uuid.equals(uuidFromPreferences()));
    }

    @Test
    public void testStoringAndRetrievingBatchUUID() {
        RequestOldUtil util = new RequestOldUtil();
        String uuid = util.generateAndStoreBatchUUID();
        String retrievedUUID = util.getStoredBatchUUID();
        assertTrue(uuid.equals(retrievedUUID));
    }

    @Test
    public void testRemovingStoredBatchUUID() {
        RequestOldUtil util = new RequestOldUtil();
        util.generateAndStoreBatchUUID();
        assertNotNull(uuidFromPreferences());

        util.removeStoredBatchUUID();
        assertNull(uuidFromPreferences());
    }

    @Test
    public void testSetNewBatchUUIDForRequests() {
        LeanplumEventDataManager.init(Leanplum.getContext());

        RequestOld request1 = new RequestOld(this.POST, Constants.Methods.START, null);
        request1.sendEventually();
        RequestOld request2 = new RequestOld(this.POST, Constants.Methods.TRACK, null);
        request2.sendEventually();
        List<Map<String, Object>> unsentRequests1 = request1.getUnsentRequests(1.0);
        String oldUUID1 = (String) unsentRequests1.get(0).get(request1.UUID_KEY);

        List<Map<String, Object>> unsentRequests2 = request1.getUnsentRequests(1.0);
        String oldUUID2 = (String) unsentRequests2.get(0).get(request1.UUID_KEY);

        List<Map<String, Object>> unsentRequests3 = request1.getUnsentRequests(0.5);
        String newUUID = (String) unsentRequests3.get(0).get(request1.UUID_KEY);

        assertTrue(oldUUID1.equals(oldUUID2));
        assertFalse(oldUUID1.equals(newUUID));
    }

    private String uuidFromPreferences() {
        Context context = Leanplum.getContext();
        SharedPreferences preferences = context.getSharedPreferences(
                LEANPLUM, Context.MODE_PRIVATE);
        return preferences.getString(Constants.Defaults.UUID_KEY, null);
    }
}
