package me.gorani.launchingservice

import java.net.URI
import java.util.concurrent.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class LaunchingServiceTest {
  @Test
  fun `fetch failure falls back to active values`() = runTest {
    val fetchError = IllegalStateException("network unavailable")
    var observedError: Exception? = null
    val remoteConfig = FakeRemoteConfigClient(
      strings = mapOf(
        "optionalUpdateAppVersionKey" to "2.0.0",
        "optionalUpdateAlertTitleKey" to "Optional",
        "optionalUpdateAlertMessageKey" to "Update available",
        "optionalUpdateAlertDoneLinkURLKey" to "https://example.com/update",
      ),
      fetchError = fetchError,
    )
    val service = LaunchingService(
      remoteConfigClient = remoteConfig,
      appVersionProvider = AppVersionProvider { "1.0.0" },
      fetchFailureObserver = RemoteConfigFetchFailureObserver { observedError = it },
    )

    val status = service.fetchAppUpdateStatus()

    assertEquals(
      AppUpdateStatus.OptionalUpdateRequired(
        UpdateAlert("Optional", "Update available", URI("https://example.com/update")),
      ),
      status,
    )
    assertEquals(1, remoteConfig.fetchCount)
    assertSame(fetchError, observedError)
  }

  @Test
  fun `observer failure does not interrupt fallback`() = runTest {
    val service = LaunchingService(
      remoteConfigClient = FakeRemoteConfigClient(
        fetchError = IllegalStateException("network unavailable"),
      ),
      appVersionProvider = AppVersionProvider { "1.0.0" },
      fetchFailureObserver = RemoteConfigFetchFailureObserver {
        throw IllegalStateException("diagnostics unavailable")
      },
    )

    assertEquals(AppUpdateStatus.Valid, service.fetchAppUpdateStatus())
  }

  @Test
  fun `observer errors are never swallowed`() = runTest {
    val observerError = AssertionError("fatal observer failure")
    val service = LaunchingService(
      remoteConfigClient = FakeRemoteConfigClient(
        fetchError = IllegalStateException("network unavailable"),
      ),
      appVersionProvider = AppVersionProvider { "1.0.0" },
      fetchFailureObserver = RemoteConfigFetchFailureObserver { throw observerError },
    )

    try {
      service.fetchAppUpdateStatus()
      fail("Expected AssertionError")
    } catch (error: AssertionError) {
      assertSame(observerError, error)
    }
  }

  @Test
  fun `coroutine cancellation is never swallowed`() = runTest {
    var observedFailureCount = 0
    val service = LaunchingService(
      remoteConfigClient = FakeRemoteConfigClient(fetchError = CancellationException()),
      appVersionProvider = AppVersionProvider { "1.0.0" },
      fetchFailureObserver = RemoteConfigFetchFailureObserver { observedFailureCount += 1 },
    )

    try {
      service.fetchAppUpdateStatus()
      fail("Expected CancellationException")
    } catch (_: CancellationException) {
      // Expected.
    }
    assertEquals(0, observedFailureCount)
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

  @Test
  fun `missing Firebase configuration is mapped to the documented error`() = runTest {
    val configurationError = IllegalStateException("FirebaseApp is not initialized")
    val service = LaunchingService(
      remoteConfigClient = FakeRemoteConfigClient(valueError = configurationError),
      appVersionProvider = AppVersionProvider { "1.0.0" },
    )

    try {
      service.fetchAppUpdateStatus()
      fail("Expected FirebaseNotConfigured")
    } catch (error: LaunchingServiceException.FirebaseNotConfigured) {
      assertSame(configurationError, error.cause)
    }
  }
}
