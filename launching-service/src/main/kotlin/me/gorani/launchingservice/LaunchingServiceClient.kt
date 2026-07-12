package me.gorani.launchingservice

public fun interface LaunchingServiceClient {
  public suspend fun fetchAppUpdateStatus(): AppUpdateStatus
}
