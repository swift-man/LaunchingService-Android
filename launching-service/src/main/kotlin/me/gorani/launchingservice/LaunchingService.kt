package me.gorani.launchingservice

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.CancellationException

public class LaunchingService internal constructor(
  private val remoteConfigClient: RemoteConfigClient,
  private val appVersionProvider: AppVersionProvider,
  private val keys: RemoteConfigKeys = RemoteConfigKeys(),
  private val comparator: LaunchingStatusComparator = LaunchingStatusComparator(),
  private val fetchFailureObserver: RemoteConfigFetchFailureObserver = ignoreFetchFailure,
) : LaunchingServiceClient {
  public constructor(
    context: Context,
    keys: RemoteConfigKeys = RemoteConfigKeys(),
  ) : this(
    remoteConfigClient = FirebaseRemoteConfigClient(FirebaseRemoteConfig::getInstance),
    appVersionProvider = PackageManagerAppVersionProvider(context.applicationContext),
    keys = keys,
    comparator = LaunchingStatusComparator(),
  )

  public constructor(
    context: Context,
    fetchFailureObserver: RemoteConfigFetchFailureObserver,
    keys: RemoteConfigKeys = RemoteConfigKeys(),
  ) : this(
    remoteConfigClient = FirebaseRemoteConfigClient(FirebaseRemoteConfig::getInstance),
    appVersionProvider = PackageManagerAppVersionProvider(context.applicationContext),
    keys = keys,
    comparator = LaunchingStatusComparator(),
    fetchFailureObserver = fetchFailureObserver,
  )

  public constructor(
    context: Context,
    remoteConfig: FirebaseRemoteConfig,
    keys: RemoteConfigKeys = RemoteConfigKeys(),
  ) : this(
    remoteConfigClient = FirebaseRemoteConfigClient { remoteConfig },
    appVersionProvider = PackageManagerAppVersionProvider(context.applicationContext),
    keys = keys,
    comparator = LaunchingStatusComparator(),
  )

  public constructor(
    context: Context,
    remoteConfig: FirebaseRemoteConfig,
    fetchFailureObserver: RemoteConfigFetchFailureObserver,
    keys: RemoteConfigKeys = RemoteConfigKeys(),
  ) : this(
    remoteConfigClient = FirebaseRemoteConfigClient { remoteConfig },
    appVersionProvider = PackageManagerAppVersionProvider(context.applicationContext),
    keys = keys,
    comparator = LaunchingStatusComparator(),
    fetchFailureObserver = fetchFailureObserver,
  )

  override suspend fun fetchAppUpdateStatus(): AppUpdateStatus {
    try {
      remoteConfigClient.fetchAndActivate()
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      notifyFetchFailure(error)
    }

    val releaseVersion = appVersionProvider.releaseVersion()
    val launching = try {
      RemoteConfigParser(remoteConfigClient, keys).parse()
    } catch (error: IllegalStateException) {
      throw LaunchingServiceException.FirebaseNotConfigured(error)
    }

    return comparator.compare(releaseVersion, launching)
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
  }
}
