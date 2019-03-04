package com.leanplum.internal;

import com.leanplum.Leanplum;

import java.util.Map;

public class RequestFactory {

  private static final String API_METHOD_START = "start";
  private static final String API_METHOD_GET_VARS = "getVars";
  private static final String API_METHOD_SET_VARS = "setVars";
  private static final String API_METHOD_STOP = "stop";
  private static final String API_METHOD_RESTART = "restart";
  private static final String API_METHOD_TRACK = "track";
  private static final String API_METHOD_ADVANCE = "advance";
  private static final String API_METHOD_PAUSE_SESSION = "pauseSession";
  private static final String API_METHOD_PAUSE_STATE = "pauseState";
  private static final String API_METHOD_RESUME_SESSION = "resumeSession";
  private static final String API_METHOD_RESUME_STATE = "resumeState";
  private static final String API_METHOD_MULTI = "multi";
  private static final String API_METHOD_REGISTER_FOR_DEVELOPMENT = "registerDevice";
  private static final String API_METHOD_SET_USER_ATTRIBUTES = "setUserAttributes";
  private static final String API_METHOD_SET_DEVICE_ATTRIBUTES = "setDeviceAttributes";
  private static final String API_METHOD_SET_TRAFFIC_SOURCE_INFO = "setTrafficSourceInfo";
  private static final String API_METHOD_UPLOAD_FILE = "uploadFile";
  private static final String API_METHOD_DOWNLOAD_FILE = "downloadFile";
  private static final String API_METHOD_HEARTBEAT = "heartbeat";
  private static final String API_METHOD_SAVE_VIEW_CONTROLLER_VERSION = "saveInterface";
  private static final String API_METHOD_SAVE_VIEW_CONTROLLER_IMAGE = "saveInterfaceImage";
  private static final String API_METHOD_GET_VIEW_CONTROLLER_VERSIONS_LIST = "getViewControllerVersionsList";
  private static final String API_METHOD_LOG = "log";
  private static final String API_METHOD_GET_INBOX_MESSAGES = "getNewsfeedMessages";
  private static final String API_METHOD_MARK_INBOX_MESSAGE_AS_READ = "markNewsfeedMessageAsRead";
  private static final String API_METHOD_DELETE_INBOX_MESSAGE = "deleteNewsfeedMessage";

  private final CountAggregator countAggregator;
  private final FeatureFlagManager featureFlagManager;

  public RequestFactory(CountAggregator countAggregator, FeatureFlagManager featureFlagManager) {
    this.countAggregator = countAggregator;
    this.featureFlagManager = featureFlagManager;
  }

  public RequestOld createRequest(
      String httpMethod, String apiMethod, Map<String, Object> params) {
    Leanplum.countAggregator().incrementCount("createRequest");
    return new RequestOld(httpMethod, apiMethod, params);
  }

  public Requesting startWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_START, params);
  }

  public Requesting getVarsWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_GET_VARS, params);
  }

  public Requesting setVarsWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_SET_VARS, params);
  }

  public Requesting stopWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_STOP, params);
  }

  public Requesting restartWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_RESTART, params);
  }

  public Requesting trackWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_TRACK, params);
  }

  public Requesting advanceWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_ADVANCE, params);
  }

  public Requesting pauseSessionWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_PAUSE_SESSION, params);
  }

  public Requesting pauseStateWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_PAUSE_STATE, params);
  }

  public Requesting resumeSessionWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_RESUME_SESSION, params);
  }

  public Requesting resumeStateWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_RESUME_STATE, params);
  }

  public Requesting multiWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_MULTI, params);
  }

  public Requesting registerDeviceWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_REGISTER_FOR_DEVELOPMENT, params);
  }

  public Requesting setUserAttributesWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_SET_USER_ATTRIBUTES, params);
  }

  public Requesting setDeviceAttributesWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_SET_DEVICE_ATTRIBUTES, params);
  }

  public Requesting setTrafficSourceInfoWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_SET_TRAFFIC_SOURCE_INFO, params);
  }

  public Requesting uploadFileWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_UPLOAD_FILE, params);
  }

  public Requesting downloadFileWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_DOWNLOAD_FILE, params);
  }

  public Requesting heartbeatWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_HEARTBEAT, params);
  }

  public Requesting saveInterfaceWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_SAVE_VIEW_CONTROLLER_VERSION, params);
  }

  public Requesting saveInterfaceImageWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_SAVE_VIEW_CONTROLLER_IMAGE, params);
  }

  public Requesting getViewControllerVersionsListWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_GET_VIEW_CONTROLLER_VERSIONS_LIST, params);
  }

  public Requesting logWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_LOG, params);
  }

  public Requesting getNewsfeedMessagesWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_GET_INBOX_MESSAGES, params);
  }

  public Requesting markNewsfeedMessageAsReadWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_MARK_INBOX_MESSAGE_AS_READ, params);
  }

  public Requesting deleteNewsfeedMessagesWithParams(Map<String, Object> params) {
    return createPostForApiMethod(API_METHOD_DELETE_INBOX_MESSAGE, params);
  }


  private Requesting createGetForApiMethod(String apiMethod, Map<String, Object> params) {
    if (shouldReturnLPRequestClass()) {
      return Request.get(apiMethod, params);
    }
    return RequestOld.get(apiMethod, params);
  }

  private Requesting createPostForApiMethod(String apiMethod, Map<String, Object> params) {
    if (shouldReturnLPRequestClass()) {
      return Request.post(apiMethod, params);
    }
    return RequestOld.post(apiMethod, params);
  }

  private Boolean shouldReturnLPRequestClass() {
    return Leanplum.featureFlagManager().isFeatureFlagEnabled(Leanplum.featureFlagManager().FEATURE_FLAG_REQUEST_REFACTOR);
  }

}
