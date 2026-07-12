package me.gorani.launchingservice

import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteConfigParserTest {
  @Test
  fun `force update keeps alert data when version is missing`() {
    val launching = parser(
      strings = mapOf(
        "forceUpdateAlertTitleKey" to "Force update",
        "forceUpdateAlertMessageKey" to "Please update",
        "forceUpdateAlertDoneLinkURLKey" to "https://example.com/force",
      ),
    ).parse()

    assertEquals("", launching.forceUpdate.version)
    assertEquals("Force update", launching.forceUpdate.alertTitle)
    assertEquals(URI("https://example.com/force"), launching.forceUpdate.alertDoneUri)
  }

  @Test
  fun `missing force URL disables force update and blacklist`() {
    val launching = parser(
      strings = mapOf(
        "forceUpdateAppVersionKey" to "2.0.0",
        "blackListVersionsKey" to "1.0.0, 1.1.0",
      ),
    ).parse()

    assertEquals("", launching.forceUpdate.version)
    assertEquals(URI("about:blank"), launching.forceUpdate.alertDoneUri)
    assertTrue(launching.blackListVersions.isEmpty())
  }

  @Test
  fun `blacklist entries are trimmed and blank entries removed`() {
    val launching = parser(
      strings = mapOf(
        "forceUpdateAlertDoneLinkURLKey" to "https://example.com/force",
        "blackListVersionsKey" to " 1.0.0, , 1.1.0 ",
      ),
    ).parse()

    assertEquals(listOf("1.0.0", "1.1.0"), launching.blackListVersions)
  }

  @Test
  fun `valid force URL keeps blacklist even when alert values match inactive defaults`() {
    val launching = parser(
      strings = mapOf(
        "forceUpdateAlertDoneLinkURLKey" to "about:blank",
        "blackListVersionsKey" to "1.0.0",
      ),
    ).parse()

    assertEquals(listOf("1.0.0"), launching.blackListVersions)
  }

  @Test
  fun `optional update requires both version and absolute URL`() {
    val noVersion = parser(
      strings = mapOf("optionalUpdateAlertDoneLinkURLKey" to "https://example.com/update"),
    ).parse()
    val invalidUrl = parser(
      strings = mapOf(
        "optionalUpdateAppVersionKey" to "2.0.0",
        "optionalUpdateAlertDoneLinkURLKey" to "relative/path",
      ),
    ).parse()

    assertEquals("", noVersion.optionalUpdate.version)
    assertEquals("", invalidUrl.optionalUpdate.version)
  }

  @Test
  fun `notice supports extended and compact UTC offsets`() {
    val extended = parser(
      strings = noticeStrings(
        start = "2026-07-12T00:00:00+00:00",
        end = "2026-07-13T00:00:00+00:00",
      ),
    ).parse().notice
    val compact = parser(
      strings = noticeStrings(
        start = "2026-07-12T00:00:00+0000",
        end = "2026-07-13T00:00:00+0000",
      ),
    ).parse().notice

    assertEquals(extended?.startEpochMillis, compact?.startEpochMillis)
    assertEquals(extended?.endEpochMillis, compact?.endEpochMillis)
  }

  @Test
  fun `notice preserves millisecond precision for UTC timestamps`() {
    val notice = parser(
      strings = noticeStrings(
        start = "2026-07-12T00:00:00.123Z",
        end = "2026-07-12T00:00:01.456Z",
      ),
    ).parse().notice

    assertEquals(1_783_814_400_123L, notice?.startEpochMillis)
    assertEquals(1_783_814_401_456L, notice?.endEpochMillis)
  }

  @Test
  fun `notice normalizes variable fractional seconds like Apple ISO8601 parsing`() {
    val oneDigit = noticeStartEpochMillis("2026-07-12T00:00:00.1Z")
    val twoDigits = noticeStartEpochMillis("2026-07-12T00:00:00.12Z")
    val microseconds = noticeStartEpochMillis("2026-07-12T00:00:00.123456Z")

    assertEquals(1_783_814_400_100L, oneDigit)
    assertEquals(1_783_814_400_120L, twoDigits)
    assertEquals(1_783_814_400_123L, microseconds)
  }

  @Test
  fun `invalid or reversed notice range disables notice`() {
    val invalid = parser(
      strings = noticeStrings("invalid", "2026-07-13T00:00:00Z"),
    ).parse()
    val reversed = parser(
      strings = noticeStrings("2026-07-13T00:00:00Z", "2026-07-12T00:00:00Z"),
    ).parse()

    assertNull(invalid.notice)
    assertNull(reversed.notice)
  }

  @Test
  fun `notice URL is optional and missing boolean defaults false`() {
    val launching = parser(
      strings = noticeStrings(
        start = "2026-07-12T00:00:00Z",
        end = "2026-07-13T00:00:00Z",
      ),
    ).parse()

    assertNull(launching.notice?.doneUri)
    assertFalse(launching.notice?.terminateOnDismiss ?: true)
  }

  @Test
  fun `custom key mapping is honored`() {
    val keys = RemoteConfigKeys(
      forceUpdate = RemoteConfigKeys.ForceUpdateKeys(
        appVersion = "android_force_version",
        alertDoneUri = "android_store_url",
      ),
    )
    val launching = parser(
      strings = mapOf(
        "android_force_version" to "3.0.0",
        "android_store_url" to "https://play.google.com/store/apps/details?id=example",
      ),
      keys = keys,
    ).parse()

    assertEquals("3.0.0", launching.forceUpdate.version)
  }

  private fun parser(
    strings: Map<String, String>,
    booleans: Map<String, Boolean> = emptyMap(),
    keys: RemoteConfigKeys = RemoteConfigKeys(),
  ): RemoteConfigParser = RemoteConfigParser(
    values = FakeRemoteConfigClient(strings, booleans),
    keys = keys,
  )

  private fun noticeStrings(start: String, end: String): Map<String, String> = mapOf(
    "noticeAlertTitleKey" to "Notice",
    "noticeAlertMessageKey" to "Maintenance",
    "noticeStartDateKey" to start,
    "noticeEndDateKey" to end,
  )

  private fun noticeStartEpochMillis(start: String): Long? = parser(
    strings = noticeStrings(start, "2026-07-13T00:00:00Z"),
  ).parse().notice?.startEpochMillis
}
