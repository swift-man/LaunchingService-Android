package me.gorani.launchingservice

import kotlinx.coroutines.CompletableDeferred

internal class FakeRemoteConfigClient(
  private val strings: Map<String, String> = emptyMap(),
  private val booleans: Map<String, Boolean> = emptyMap(),
  private val fetchError: Exception? = null,
  private val valueError: Exception? = null,
  private val fetchGate: CompletableDeferred<Unit>? = null,
) : RemoteConfigClient {
  var fetchCount: Int = 0
    private set

  override suspend fun fetchAndActivate() {
    fetchCount += 1
    fetchGate?.await()
    fetchError?.let { throw it }
  }

  override fun stringValue(key: String): String {
    valueError?.let { throw it }
    return strings[key].orEmpty()
  }

  override fun booleanValue(key: String): Boolean {
    valueError?.let { throw it }
    return booleans[key] ?: false
  }
}
