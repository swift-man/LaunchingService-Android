package me.gorani.launchingservice

import java.net.URI
import java.util.concurrent.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class LaunchingServiceTest {
  @Test
  fun `fetch failure falls back to active values`() = runTest {
    val remoteConfig = FakeRemoteConfigClient(
      strings = mapOf(
        "optionalUpdateAppVersionKey" to "2.0.0",
        "optionalUpdateAlertTitleKey" to "Optional",
        "optionalUpdateAlertMessageKey" to "Update available",
        "optionalUpdateAlertDoneLinkURLKey" to "https://example.com/update",
      ),
      fetchError = IllegalStateException("network unavailable"),
    )
    val service = LaunchingService(
      remoteConfigClient = remoteConfig,
      appVersionProvider = AppVersionProvider { "1.0.0" },
    )

    val status = service.fetchAppUpdateStatus()

    assertEquals(
      AppUpdateStatus.OptionalUpdateRequired(
        UpdateAlert("Optional", "Update available", URI("https://example.com/update")),
      ),
      status,
    )
    assertEquals(1, remoteConfig.fetchCount)
  }

  @Test
  fun `coroutine cancellation is never swallowed`() = runTest {
    val service = LaunchingService(
      remoteConfigClient = FakeRemoteConfigClient(fetchError = CancellationException()),
      appVersionProvider = AppVersionProvider { "1.0.0" },
    )

    try {
      service.fetchAppUpdateStatus()
      fail("Expected CancellationException")
    } catch (_: CancellationException) {
      // Expected.
    }
  }

  @Test
  fun `invalid app version remains an explicit error`() = runTest {
    val service = LaunchingService(
      remoteConfigClient = FakeRemoteConfigClient(),
      appVersionProvider = AppVersionProvider { throw LaunchingServiceException.InvalidAppVersion() },
    )

    try {
      service.fetchAppUpdateStatus()
      fail("Expected InvalidAppVersion")
    } catch (_: LaunchingServiceException.InvalidAppVersion) {
      // Expected.
    }
  }
}
