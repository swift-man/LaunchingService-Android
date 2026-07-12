package me.gorani.launchingservice

internal class FakeRemoteConfigClient(
  private val strings: Map<String, String> = emptyMap(),
  private val booleans: Map<String, Boolean> = emptyMap(),
  private val fetchError: Exception? = null,
) : RemoteConfigClient {
  var fetchCount: Int = 0
    private set

  override suspend fun fetchAndActivate() {
    fetchCount += 1
    fetchError?.let { throw it }
  }

  override fun stringValue(key: String): String = strings[key].orEmpty()

  override fun booleanValue(key: String): Boolean = booleans[key] ?: false
}
