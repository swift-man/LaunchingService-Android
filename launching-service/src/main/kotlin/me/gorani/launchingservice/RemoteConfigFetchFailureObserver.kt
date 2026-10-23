package me.gorani.launchingservice

/**
 * Receives recoverable Firebase Remote Config fetch failures.
 *
 * Observer exceptions are ignored so diagnostics cannot interrupt cached-value fallback.
 */
public fun interface RemoteConfigFetchFailureObserver {
  /**
   * Called once per failed fetch, from the call that ran that fetch, before LaunchingService
   * falls back to the currently available values.
   *
   * Concurrent [LaunchingService.fetchAppUpdateStatus] calls share one in-flight fetch, so calls
   * that waited for it reuse its outcome without triggering another notification. The observer is
   * invoked outside the internal lock, so it may call back into LaunchingService.
   */
  public fun onFailure(error: Exception)
}
