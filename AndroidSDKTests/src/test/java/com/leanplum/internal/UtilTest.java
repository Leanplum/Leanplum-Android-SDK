package com.leanplum.internal;

import com.leanplum.__setup.LeanplumTestApp;
import com.leanplum._whitebox.utilities.ResponseHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.HttpURLConnection;

import static junit.framework.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests for {@link Util} class.
 *
 * @author Hrishi Amravatkar
 */
@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = 16,
    application = LeanplumTestApp.class
)
@PowerMockIgnore({
    "org.mockito.*",
    "org.robolectric.*",
    "org.json.*",
    "org.powermock.*",
    "android.*"
})
@PrepareForTest({Util.class})
public class UtilTest {

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

}
