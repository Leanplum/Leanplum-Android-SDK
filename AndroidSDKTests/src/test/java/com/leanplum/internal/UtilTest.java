package com.leanplum.internal;

import com.leanplum.__setup.AbstractTest;

import com.leanplum.internal.http.LeanplumHttpConnection;
import com.leanplum.internal.http.NetworkOperation;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

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

  private NetworkOperation createNetworkOp() throws IOException {
    return new NetworkOperation("a", "b", true, 1);
  }


  /**
   * Test for {@link LeanplumHttpConnection#getJsonResponse()} that returns no gzip unmarshalled data.
   */
  @Test
  public void getNonGzipEncodedResponseWithNoContentEncodingTest() throws Exception {
    NetworkOperation op = createNetworkOp();
    assertNotNull(op.getJsonResponse());
  }

  /**
   * Test for {@link LeanplumHttpConnection#getJsonResponse()} that returns gzip unmarshalled data.
   */
  @Test
  public void getGzipEncodedResponseWithContentEndingTest() throws Exception {
    boolean gzip = true;
    prepareHttpsURLConnection(200, "/responses/simple_start_response.json.gz", null, gzip);
    NetworkOperation op = createNetworkOp();
    assertNotNull(op.getJsonResponse());
  }


  /**
   * Test for {@link LeanplumHttpConnection#getJsonResponse()} that returns gzip unmarshalled error data.
   */
  @Test
  public void getGzipEncodedErrorResponseWithContentEndingTest() throws Exception {
    boolean gzip = true;
    prepareHttpsURLConnection(403, null, "/responses/simple_start_response.json.gz", gzip);
    NetworkOperation op = createNetworkOp();
    assertNotNull(op.getJsonResponse());
  }

  /**
   * Test that {@link Log#exception(Throwable)} is successfully mocked to rethrow the
   * argument exception.
   */
  @Test
  public void testHandleExceptionMocked() {
    Assert.assertThrows(Throwable.class, () -> Log.exception(new Exception()));
  }

  /**
   * Test that {@link AbstractTest#resumeLeanplumExceptionHandling()} is returning the default
   * behaviour of {@link Log#exception(Throwable)}.
   */
  @Test
  public void testHandleExceptionDefault() throws Exception {
    resumeLeanplumExceptionHandling();
    Log.exception(new Exception());
  }

}
