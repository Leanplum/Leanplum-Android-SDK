package com.leanplum.internal;

import com.leanplum.__setup.AbstractTest;
import com.leanplum._whitebox.utilities.ResponseHelper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.HttpURLConnection;

import static junit.framework.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests for {@link Util} class.
 *
 * @author Hrishi Amravatkar
 */
public class UtilTest extends AbstractTest {

  /**
   * Runs before every test case.
   */
  @Before
  public void setUp() {
  }


  /**
   * Test for {@link Util#getJsonResponse(HttpURLConnection op)} that returns no gzip unmarshalled data.
   */
  @Test
  public void getNonGzipEncodedResponseWithNoContentEncodingTest() throws Exception {
    HttpURLConnection mockHttpUrlConnection = mock(HttpURLConnection.class);
    when(mockHttpUrlConnection.getInputStream()).thenReturn(ResponseHelper.class.getResourceAsStream("/responses/simple_start_response.json"));
    when(mockHttpUrlConnection.getResponseCode()).thenReturn(200);
    assertNotNull(Util.getJsonResponse(mockHttpUrlConnection));
  }

  /**
   * Test for {@link Util#getJsonResponse(HttpURLConnection op)} that returns gzip unmarshalled data.
   */
  @Test
  public void getGzipEncodedResponseWithContentEndingTest() throws Exception {
    HttpURLConnection mockHttpUrlConnection = mock(HttpURLConnection.class);
    when(mockHttpUrlConnection.getInputStream()).thenReturn(ResponseHelper.class.getResourceAsStream("/responses/simple_start_response.json.gz"));
    when(mockHttpUrlConnection.getResponseCode()).thenReturn(200);
    when(mockHttpUrlConnection.getHeaderField("content-encoding")).thenReturn("gzip");
    assertNotNull(Util.getJsonResponse(mockHttpUrlConnection));
  }


  /**
   * Test for {@link Util#getJsonResponse(HttpURLConnection op)} that returns gzip unmarshalled error data.
   */
  @Test
  public void getGzipEncodedErrorResponseWithContentEndingTest() throws Exception {
    HttpURLConnection mockHttpUrlConnection = mock(HttpURLConnection.class);
    when(mockHttpUrlConnection.getErrorStream()).thenReturn(ResponseHelper.class.getResourceAsStream("/responses/simple_start_response.json.gz"));
    when(mockHttpUrlConnection.getResponseCode()).thenReturn(403);
    when(mockHttpUrlConnection.getHeaderField("content-encoding")).thenReturn("gzip");
    assertNotNull(Util.getJsonResponse(mockHttpUrlConnection));
  }

  /**
   * Test that {@link Util#handleException(Throwable)} is successfully mocked to rethrow the
   * argument exception.
   */
  @Test
  public void testHandleExceptionMocked() {
    Assert.assertThrows(Throwable.class, () -> Log.exception(new Exception()));
  }

  /**
   * Test that {@link AbstractTest#resumeLeanplumExceptionHandling()} is returning the default
   * behaviour of {@link Util#handleException(Throwable)}.
   */
  @Test
  public void testHandleExceptionDefault() throws Exception {
    resumeLeanplumExceptionHandling();
    Log.exception(new Exception());
  }

}
