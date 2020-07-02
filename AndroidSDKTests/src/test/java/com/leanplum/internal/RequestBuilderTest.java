package com.leanplum.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = 16
)
public class RequestBuilderTest {
  @Test
  public void testCreate() {
    String method = "get";
    String action = "action";
    Map<String, Object> emptyParams = new HashMap<>();
    RequestBuilder builder = new RequestBuilder(method, action);
    RequestOld request = builder.create();
    assertEquals(action, request.getApiAction());
    assertEquals(method, request.getHttpMethod());
    assertEquals(emptyParams, request.getParams());
  }

  @Test
  public void testAndParam() {
    String method = "get";
    String action = "action";
    Map<String, Object> expectedParams = new HashMap<>();
    expectedParams.put("param", "value");

    RequestBuilder builder = new RequestBuilder(method, action);
    RequestBuilder returnedBuilder = builder.andParam("param", "value");

    assertSame(builder, returnedBuilder);
    assertEquals(expectedParams, builder.getParams());
  }

  @Test
  public void testAndParams() {
    String method = "get";
    String action = "action";
    Map<String, Object> expectedParams = new HashMap<>();
    expectedParams.put("param1", "value1");
    expectedParams.put("param2", "value2");

    RequestBuilder builder = new RequestBuilder(method, action);
    RequestBuilder returnedBuilder = builder.andParams(expectedParams);

    assertSame(builder, returnedBuilder);
    assertEquals(expectedParams, builder.getParams());
  }

  @Test
  public void testAndParamAndParams() {
    String method = "get";
    String action = "action";
    Map<String, Object> params = new HashMap<>();
    params.put("param1", "value1");
    params.put("param2", "value2");

    RequestBuilder builder = new RequestBuilder(method, action);
    builder.andParam("param3", "value3").andParams(params);

    Map<String, Object> expectedParams = new HashMap<>();
    expectedParams.put("param1", "value1");
    expectedParams.put("param2", "value2");
    expectedParams.put("param3", "value3");
    assertEquals(expectedParams, builder.getParams());
  }

  private void verifyActionMethod(String method, String action, Supplier<RequestBuilder> supplier) {
    RequestBuilder builder = supplier.get();
    assertEquals(method, builder.getHttpMethod());
    assertEquals(action, builder.getApiAction());
  }

  @Test
  public void testActions() {
    verifyActionMethod("POST", "start", RequestBuilder::withStartAction);
    verifyActionMethod("POST", "getVars", RequestBuilder::withGetVarsAction);
    verifyActionMethod("POST", "setVars", RequestBuilder::withSetVarsAction);
    verifyActionMethod("POST", "stop", RequestBuilder::withStopAction);
    verifyActionMethod("POST", "restart", RequestBuilder::withRestartAction);
    verifyActionMethod("POST", "track", RequestBuilder::withTrackAction);
    verifyActionMethod("POST", "trackGeofence", RequestBuilder::withTrackGeofenceAction);
    verifyActionMethod("POST", "advance", RequestBuilder::withAdvanceAction);
    verifyActionMethod("POST", "pauseSession", RequestBuilder::withPauseSessionAction);
    verifyActionMethod("POST", "pauseState", RequestBuilder::withPauseStateAction);
    verifyActionMethod("POST", "resumeSession", RequestBuilder::withResumeSessionAction);
    verifyActionMethod("POST", "resumeState", RequestBuilder::withResumeStateAction);
    verifyActionMethod("POST", "multi", RequestBuilder::withMultiAction);
    verifyActionMethod("POST", "registerDevice", RequestBuilder::withRegisterForDevelopmentAction);
    verifyActionMethod("POST", "setUserAttributes", RequestBuilder::withSetUserAttributesAction);
    verifyActionMethod("POST", "setDeviceAttributes", RequestBuilder::withSetDeviceAttributesAction);
    verifyActionMethod("POST", "setTrafficSourceInfo", RequestBuilder::withSetTrafficSourceInfoAction);
    verifyActionMethod("POST", "uploadFile", RequestBuilder::withUploadFileAction);
    verifyActionMethod("GET", "downloadFile", RequestBuilder::withDownloadFileAction);
    verifyActionMethod("POST", "heartbeat", RequestBuilder::withHeartbeatAction);
    verifyActionMethod("POST", "log", RequestBuilder::withLogAction);
    verifyActionMethod("POST", "getNewsfeedMessages", RequestBuilder::withGetInboxMessagesAction);
    verifyActionMethod("POST", "markNewsfeedMessageAsRead", RequestBuilder::withMarkInboxMessageAsReadAction);
    verifyActionMethod("POST", "deleteNewsfeedMessage", RequestBuilder::withDeleteInboxMessageAction);
  }
}
