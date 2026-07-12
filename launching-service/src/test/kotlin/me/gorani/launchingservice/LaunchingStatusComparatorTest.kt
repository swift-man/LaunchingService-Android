package me.gorani.launchingservice

import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchingStatusComparatorTest {
  private val now = 1_700_000_000_000L
  private val comparator = LaunchingStatusComparator { now }

  @Test
  fun `force update has highest priority`() {
    val status = comparator.compare(
      releaseVersion = "1.0.0",
      launching = launching(
        forceVersion = "2.0.0",
        optionalVersion = "3.0.0",
        blackList = listOf("1.0.0"),
        notice = activeNotice(),
      ),
    )

    assertEquals(AppUpdateStatus.ForcedUpdateRequired(forceInfo("2.0.0").toAlert()), status)
  }

  @Test
  fun `blacklist precedes optional update and notice`() {
    val status = comparator.compare(
      releaseVersion = "1.0",
      launching = launching(
        optionalVersion = "3.0.0",
        blackList = listOf(" 1.0.0 "),
        notice = activeNotice(),
      ),
    )

    assertEquals(AppUpdateStatus.ForcedUpdateRequired(forceInfo("").toAlert()), status)
  }

  @Test
  fun `optional update precedes notice`() {
    val status = comparator.compare(
      releaseVersion = "1.0.0",
      launching = launching(optionalVersion = "2.0.0", notice = activeNotice()),
    )

    assertEquals(AppUpdateStatus.OptionalUpdateRequired(optionalInfo("2.0.0").toAlert()), status)
  }

  @Test
  fun `notice range includes both boundaries`() {
    val notice = activeNotice(start = now, end = now)

    assertEquals(
      AppUpdateStatus.Notice(
        NoticeAlert(notice.title, notice.message, notice.terminateOnDismiss, notice.doneUri),
      ),
      comparator.compare("1.0.0", launching(notice = notice)),
    )
  }

  @Test
  fun `valid is returned when every feature is inactive`() {
    assertEquals(AppUpdateStatus.Valid, comparator.compare("1.0.0", launching()))
  }

  private fun launching(
    forceVersion: String = "",
    optionalVersion: String = "",
    blackList: List<String> = emptyList(),
    notice: NoticeInfo? = null,
  ) = Launching(
    forceUpdate = forceInfo(forceVersion),
    optionalUpdate = optionalInfo(optionalVersion),
    blackListVersions = blackList,
    notice = notice,
  )

  private fun forceInfo(version: String) = AppUpdateInfo(
    version,
    "Force",
    "Update required",
    URI("https://example.com/force"),
  )

  private fun optionalInfo(version: String) = AppUpdateInfo(
    version,
    "Optional",
    "Update available",
    URI("https://example.com/optional"),
  )

  private fun activeNotice(
    start: Long = now - 1,
    end: Long = now + 1,
  ) = NoticeInfo(
    title = "Notice",
    message = "Maintenance",
    terminateOnDismiss = false,
    startEpochMillis = start,
    endEpochMillis = end,
    doneUri = null,
  )

  private fun AppUpdateInfo.toAlert() = UpdateAlert(alertTitle, alertMessage, alertDoneUri)
}
