package me.gorani.launchingservice

import java.net.URI
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)

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

  @Test
  fun `concurrent calls share one in-flight fetch`() = runTest {
    val fetchGate = CompletableDeferred<Unit>()
    val remoteConfig = FakeRemoteConfigClient(fetchGate = fetchGate)
    val service = LaunchingService(
      remoteConfigClient = remoteConfig,
      appVersionProvider = AppVersionProvider { "1.0.0" },
    )

    val first = async { service.fetchAppUpdateStatus() }
    val second = async { service.fetchAppUpdateStatus() }
    runCurrent()
    assertEquals(1, remoteConfig.fetchCount)

    fetchGate.complete(Unit)
    advanceUntilIdle()

    assertEquals(AppUpdateStatus.Valid, first.await())
    assertEquals(AppUpdateStatus.Valid, second.await())
    assertEquals(1, remoteConfig.fetchCount)
  }

  @Test
  fun `calls after a completed fetch start a new fetch`() = runTest {
    val remoteConfig = FakeRemoteConfigClient()
    val service = LaunchingService(
      remoteConfigClient = remoteConfig,
      appVersionProvider = AppVersionProvider { "1.0.0" },
    )

    service.fetchAppUpdateStatus()
    service.fetchAppUpdateStatus()

    assertEquals(2, remoteConfig.fetchCount)
  }

  @Test
  fun `cancelling one call does not break a concurrent call`() = runTest {
    val fetchGate = CompletableDeferred<Unit>()
    val remoteConfig = FakeRemoteConfigClient(fetchGate = fetchGate)
    val service = LaunchingService(
      remoteConfigClient = remoteConfig,
      appVersionProvider = AppVersionProvider { "1.0.0" },
    )

    val first = async { service.fetchAppUpdateStatus() }
    val second = async { service.fetchAppUpdateStatus() }
    runCurrent()
    assertEquals(1, remoteConfig.fetchCount)

    first.cancel()
    runCurrent()
    assertTrue(first.isCancelled)

    fetchGate.complete(Unit)
    advanceUntilIdle()

    assertEquals(AppUpdateStatus.Valid, second.await())
    assertEquals(2, remoteConfig.fetchCount)
  }
}
