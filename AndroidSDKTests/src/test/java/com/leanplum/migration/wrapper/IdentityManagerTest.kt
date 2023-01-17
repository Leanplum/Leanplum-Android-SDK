package com.leanplum.migration.wrapper

import android.util.Log
import com.leanplum.utils.HashUtil
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.properties.Delegates

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [19])
@PrepareForTest(
  Log::class,
)
class IdentityManagerTest {
  val userId = "userId"
  val deviceId = "deviceId"

  val userId2 = "userId2"

  // 6ccb21214ffd60b0fc2c1607cf6a05be6a0fed9c74819eb6a92e1bd6717b28eb
  val userId_hash = "6ccb21214f"
  // c9430313f85740d3c62dd8bf8c8d275165e96f830e7b1e6ddf3a89ba17ee5cce
  val userId2_hash = "c9430313f8"

  var state: String? = null
  var mergedUserId: String? = null

  val totalIdLengthLimit = 61
  val deviceIdHashLength = 50

  @Before
  fun setup() {
    state = null
    mergedUserId = null
  }

  private fun createIdentityManager(deviceId: String, userId: String): IdentityManager {
    return IdentityManager(
      deviceId = deviceId,
      userId = userId,
      stateDelegate = Delegates.observable("undefined") { _, _, new ->
        state = new
      },
      mergeUserDelegate = Delegates.observable(null) { _, _, new ->
        mergedUserId = new
      }
    )
  }

  @Test
  fun testProfile() {
    val anonymousManager = createIdentityManager(deviceId, deviceId)
    assertEquals(mapOf("Identity" to deviceId), anonymousManager.profile())

    val identifiedManager = IdentityManager(deviceId, userId)
    assertEquals(mapOf("Identity" to userId), identifiedManager.profile())
  }

  @Test
  fun testUserIdHash() {
    val lpIdentity = LPIdentity(deviceId, userId)
    assertEquals(userId_hash, lpIdentity.userId())
  }

  @Test
  fun testAnonymous() {
    val identityManager = createIdentityManager(deviceId, deviceId)
    assertTrue(identityManager.isAnonymous())
    assertEquals("anonymous", state)
    assertEquals(deviceId, identityManager.cleverTapId())
  }

  @Test
  fun testIdentified() {
    val identityManager = createIdentityManager(deviceId, userId)
    assertFalse(identityManager.isAnonymous())
    assertEquals("identified", state)
    assertEquals("${deviceId}_${userId_hash}", identityManager.cleverTapId())
  }

  @Test
  fun testIdentifiedNewUser() {
    val identityManager = createIdentityManager(deviceId, userId)

    identityManager.setUserId(userId2)

    assertFalse(identityManager.isAnonymous())
    assertEquals("identified", state)
    assertEquals(mergedUserId, null)
    assertEquals("${deviceId}_${userId2_hash}", identityManager.cleverTapId())
    assertEquals(mapOf("Identity" to userId2), identityManager.profile())
    assertFalse(identityManager.isDeviceIdHashed())
  }

  @Test
  fun testAnonymousLogin() {
    val identityManager = createIdentityManager(deviceId, deviceId)

    identityManager.setUserId(userId)

    assertFalse(identityManager.isAnonymous())
    assertEquals("identified", state)
    assertEquals(mergedUserId, userId_hash)
    assertEquals(deviceId, identityManager.cleverTapId())
    assertEquals(mapOf("Identity" to userId), identityManager.profile())
  }

  @Test
  fun testAnonymousLoginNewUser() {
    val identityManager = createIdentityManager(deviceId, deviceId)

    identityManager.setUserId(userId)
    identityManager.setUserId(userId2)

    assertFalse(identityManager.isAnonymous())
    assertEquals("identified", state)
    assertEquals(mergedUserId, userId_hash)
    assertEquals("${deviceId}_$userId2_hash", identityManager.cleverTapId())
    assertEquals(mapOf("Identity" to userId2), identityManager.profile())
  }

  @Test
  fun testAnonymousLoginBack() {
    val identityManager = createIdentityManager(deviceId, deviceId)

    identityManager.setUserId(userId)
    identityManager.setUserId(userId2)
    identityManager.setUserId(userId)

    assertFalse(identityManager.isAnonymous())
    assertEquals("identified", state)
    assertEquals(deviceId, identityManager.cleverTapId())
  }

  // TODO testAnonymousLoginStart ?

  @Test
  fun testAnonymousLimitDeviceId() {
    val deviceId = "1".repeat(deviceIdHashLength)
    val identityManager = createIdentityManager(deviceId, deviceId)

    assertEquals(deviceId, identityManager.cleverTapId())
    assertEquals(deviceIdHashLength, identityManager.cleverTapId().length)
  }

  @Test
  fun testIdentifiedLimitDeviceId() {
    val deviceId = "1".repeat(deviceIdHashLength)
    val identityManager = createIdentityManager(deviceId, userId)

    assertEquals("${deviceId}_$userId_hash", identityManager.cleverTapId())
    assertEquals(totalIdLengthLimit, identityManager.cleverTapId().length)
    assertFalse(identityManager.isDeviceIdHashed())
  }

  @Test
  fun testAnonymousLongDeviceId() {
    val deviceId = "1".repeat(deviceIdHashLength+1)
    val identityManager = createIdentityManager(deviceId, deviceId)

    val deviceId_hash = HashUtil.sha256_200(deviceId)

    assertEquals(deviceId_hash, identityManager.cleverTapId())
    assertEquals(deviceIdHashLength, identityManager.cleverTapId().length)
  }

  @Test
  fun testIdentifiedLongDeviceId() {
    val deviceId = "1".repeat(deviceIdHashLength+1)
    val identityManager = createIdentityManager(deviceId, userId)

    val deviceId_hash = HashUtil.sha256_200(deviceId)

    assertEquals("${deviceId_hash}_$userId_hash", identityManager.cleverTapId())
    assertEquals(totalIdLengthLimit, identityManager.cleverTapId().length)
    assertTrue(identityManager.isDeviceIdHashed())
  }

  @Test
  fun testIdentifiedLongerDeviceId() {
    val deviceId = "1".repeat(deviceIdHashLength+10)
    val identityManager = createIdentityManager(deviceId, userId)

    val deviceId_hash = HashUtil.sha256_200(deviceId)

    assertEquals("${deviceId_hash}_$userId_hash", identityManager.cleverTapId())
    assertEquals(totalIdLengthLimit, identityManager.cleverTapId().length)
  }

  @Test
  fun testAnonymousInvalidDeviceId() {
    val deviceId = "&".repeat(10)
    val identityManager = createIdentityManager(deviceId, deviceId)

    val deviceId_hash = HashUtil.sha256_200(deviceId)

    assertEquals(deviceId_hash, identityManager.cleverTapId())
    assertEquals(deviceIdHashLength, identityManager.cleverTapId().length)
  }

  @Test
  fun testIdentifiedInvalidDeviceId() {
    val deviceId = "&".repeat(10)
    val identityManager = createIdentityManager(deviceId, userId)

    val deviceId_hash = HashUtil.sha256_200(deviceId)

    assertEquals("${deviceId_hash}_$userId_hash", identityManager.cleverTapId())
    assertEquals(totalIdLengthLimit, identityManager.cleverTapId().length)
    assertTrue(identityManager.isDeviceIdHashed())
  }

  @Test
  fun testIdentifiedEmailUserId() {
    val userId = "test@test.com"
    val identityManager = createIdentityManager(deviceId, userId)

    // f660ab912ec121d1b1e928a0bb4bc61b15f5ad44d5efdc4e1c92a25e99b8e44a
    val userId_hash = "f660ab912e"

    assertEquals("${deviceId}_$userId_hash", identityManager.cleverTapId())
    assertTrue(identityManager.cleverTapId().length <= totalIdLengthLimit)
  }

  @Test
  fun testInvalidDeviceIds() {
    listOf(
      // -\:\"fcea8952-0ae1-411d-b23c-50661050ded1\"
      "-\\:\\\"fcea8952-0ae1-411d-b23c-50661050ded1\\\"",
      // abd6039873\",4562412546555904
      "abd6039873\\\",4562412546555904",
      // !22113163828\""
      "!22113163828\\\"\"",
      // "22121327322\",4562412546555904<newline>117669683\""
      "\"22121327322\\\",4562412546555904\"\n117669683\\\"\"",
      // 117669683\""
      "117669683\\\"\"",
      "嘁脂Ήᔠ䦐ࠐ䤰†",
      "{{device.hardware_id}}",
      "116115935'2",
      "9d29641dc261454239456122f13de042b3a0cc3f45d4c27e7ddc97b300eb11aa"
    ).forEach { deviceId ->
        val hash = HashUtil.sha256_200(deviceId)
        val identityManager = createIdentityManager(deviceId, userId)

        println(hash)
        assertEquals("${hash}_$userId_hash", identityManager.cleverTapId())
      }
  }

  @Test
  fun testFirstStartFlag() {
    val identityManager = createIdentityManager(deviceId, userId)
    assertTrue(identityManager.isFirstTimeStart())
  }

}
