package com.leanplum.utils;

import com.leanplum.__setup.LeanplumTestApp;
import com.leanplum.internal.Util;

import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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
}
