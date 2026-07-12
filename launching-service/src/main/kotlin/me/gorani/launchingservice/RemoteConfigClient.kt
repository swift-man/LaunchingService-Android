package me.gorani.launchingservice

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await

internal interface RemoteConfigClient {
  suspend fun fetchAndActivate()
  fun stringValue(key: String): String
  fun booleanValue(key: String): Boolean
}

internal class FirebaseRemoteConfigClient(
  remoteConfigProvider: () -> FirebaseRemoteConfig,
) : RemoteConfigClient {
  private val remoteConfig: FirebaseRemoteConfig by lazy(remoteConfigProvider)

  override suspend fun fetchAndActivate() {
    remoteConfig.fetchAndActivate().await()
  }

  override fun stringValue(key: String): String = remoteConfig.getString(key)

  override fun booleanValue(key: String): Boolean = remoteConfig.getBoolean(key)
}
