package me.gorani.launchingservice

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public class LaunchingService internal constructor(
  private val remoteConfigClient: RemoteConfigClient,
  private val appVersionProvider: AppVersionProvider,
  private val keys: RemoteConfigKeys = RemoteConfigKeys(),
  private val comparator: LaunchingStatusComparator = LaunchingStatusComparator(),
  private val fetchFailureObserver: RemoteConfigFetchFailureObserver = ignoreFetchFailure,
) : LaunchingServiceClient {
  @JvmOverloads
  public constructor(
    context: Context,
    keys: RemoteConfigKeys = RemoteConfigKeys(),
  ) : this(
    remoteConfigClient = FirebaseRemoteConfigClient(FirebaseRemoteConfig::getInstance),
    appVersionProvider = defaultAppVersionProvider(context),
    keys = keys,
    comparator = LaunchingStatusComparator(),
  )

  @JvmOverloads
  public constructor(
    context: Context,
    fetchFailureObserver: RemoteConfigFetchFailureObserver,
    keys: RemoteConfigKeys = RemoteConfigKeys(),
  ) : this(
    remoteConfigClient = FirebaseRemoteConfigClient(FirebaseRemoteConfig::getInstance),
    appVersionProvider = defaultAppVersionProvider(context),
    keys = keys,
    comparator = LaunchingStatusComparator(),
    fetchFailureObserver = fetchFailureObserver,
  )

  @JvmOverloads
  public constructor(
    context: Context,
    remoteConfig: FirebaseRemoteConfig,
    keys: RemoteConfigKeys = RemoteConfigKeys(),
  ) : this(
    remoteConfigClient = FirebaseRemoteConfigClient { remoteConfig },
    appVersionProvider = defaultAppVersionProvider(context),
    keys = keys,
    comparator = LaunchingStatusComparator(),
  )

  @JvmOverloads
  public constructor(
    context: Context,
    remoteConfig: FirebaseRemoteConfig,
    fetchFailureObserver: RemoteConfigFetchFailureObserver,
    keys: RemoteConfigKeys = RemoteConfigKeys(),
  ) : this(
    remoteConfigClient = FirebaseRemoteConfigClient { remoteConfig },
    appVersionProvider = defaultAppVersionProvider(context),
    keys = keys,
    comparator = LaunchingStatusComparator(),
    fetchFailureObserver = fetchFailureObserver,
  )

  private val fetchMutex = Mutex()

  @Volatile
  private var completedFetchCount = 0L

  override suspend fun fetchAppUpdateStatus(): AppUpdateStatus {
    fetchAndActivateSharingInFlight()

    val releaseVersion = appVersionProvider.releaseVersion()
    val launching = try {
      RemoteConfigParser(remoteConfigClient, keys).parse()
    } catch (error: IllegalStateException) {
      throw LaunchingServiceException.FirebaseNotConfigured(error)
    }

    return comparator.compare(releaseVersion, launching)
  }

  /**
   * Runs one fetch at a time. A call that arrives while another fetch is in flight waits for
   * that fetch and reuses its outcome instead of starting a second one. Cancelling a waiting or
   * fetching call only affects that call; the next waiter then fetches on its own if needed.
   */
  private suspend fun fetchAndActivateSharingInFlight() {
    val observedFetchCount = completedFetchCount
    fetchMutex.withLock {
      if (completedFetchCount != observedFetchCount) return

      try {
        remoteConfigClient.fetchAndActivate()
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        notifyFetchFailure(error)
      }
      completedFetchCount += 1
    }
  }

  private fun notifyFetchFailure(error: Exception) {
    try {
      fetchFailureObserver.onFailure(error)
    } catch (_: Exception) {
      // Diagnostics must not interrupt fallback to currently available values.
    }
  }

  private companion object {
    private val ignoreFetchFailure = RemoteConfigFetchFailureObserver { }

    private fun defaultAppVersionProvider(context: Context): AppVersionProvider =
      CachingAppVersionProvider(PackageManagerAppVersionProvider(context.applicationContext))
  }
}
