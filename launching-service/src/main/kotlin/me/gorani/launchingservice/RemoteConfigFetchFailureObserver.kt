package me.gorani.launchingservice

/**
 * Receives recoverable Firebase Remote Config fetch failures.
 *
 * Observer exceptions are ignored so diagnostics cannot interrupt cached-value fallback.
 */
public fun interface RemoteConfigFetchFailureObserver {
  /**
   * Called synchronously before LaunchingService falls back to the currently available values.
   */
  public fun onFailure(error: Exception)
}
