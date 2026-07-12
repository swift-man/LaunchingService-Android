package me.gorani.launchingservice

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await

internal interface RemoteConfigClient {
  suspend fun fetchAndActivate()
  fun stringValue(key: String): String
  fun booleanValue(key: String): Boolean
}

internal class FirebaseRemoteConfigClient(
  private val remoteConfigProvider: () -> FirebaseRemoteConfig,
) : RemoteConfigClient {
  override suspend fun fetchAndActivate() {
    remoteConfigProvider().fetchAndActivate().await()
  }

  override fun stringValue(key: String): String = remoteConfigProvider().getString(key)

  override fun booleanValue(key: String): Boolean = remoteConfigProvider().getBoolean(key)
}
