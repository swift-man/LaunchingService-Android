package me.gorani.launchingservice.sample

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.gorani.launchingservice.AppUpdateStatus
import me.gorani.launchingservice.LaunchingServiceClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LaunchingViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun `successful refresh exposes ready state`() = runTest {
    val viewModel = LaunchingViewModel(LaunchingServiceClient { AppUpdateStatus.Valid })

    advanceUntilIdle()

    assertEquals(LaunchingUiState.Ready(AppUpdateStatus.Valid), viewModel.uiState.value)
  }

  @Test
  fun `failed refresh exposes recoverable error state`() = runTest {
    val viewModel = LaunchingViewModel(
      LaunchingServiceClient { error("Firebase is not configured") },
    )

    advanceUntilIdle()

    assertEquals(
      LaunchingUiState.Error("Firebase is not configured"),
      viewModel.uiState.value,
    )
  }

  @Test
  fun `cancellation is not converted to an error state`() = runTest {
    val viewModel = LaunchingViewModel(
      LaunchingServiceClient { throw CancellationException("cancelled") },
    )

    advanceUntilIdle()

    assertEquals(LaunchingUiState.Loading, viewModel.uiState.value)
  }

  @Test
  fun `refresh cancels prior in flight request`() = runTest {
    val firstStarted = CompletableDeferred<Unit>()
    val firstCancelled = CompletableDeferred<Unit>()
    val secondRelease = CompletableDeferred<Unit>()
    var requests = 0
    val viewModel = LaunchingViewModel(
      LaunchingServiceClient {
        requests += 1
        if (requests == 1) {
          firstStarted.complete(Unit)
          try {
            awaitCancellation()
          } finally {
            firstCancelled.complete(Unit)
          }
        } else {
          secondRelease.await()
          AppUpdateStatus.Valid
        }
      },
    )

    runCurrent()
    assertTrue(firstStarted.isCompleted)

    viewModel.refresh()
    runCurrent()

    assertTrue(firstCancelled.isCompleted)
    assertEquals(2, requests)

    secondRelease.complete(Unit)
    advanceUntilIdle()

    assertEquals(LaunchingUiState.Ready(AppUpdateStatus.Valid), viewModel.uiState.value)
  }
}
