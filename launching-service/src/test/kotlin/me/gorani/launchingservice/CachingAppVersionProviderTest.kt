package me.gorani.launchingservice

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class CachingAppVersionProviderTest {
  @Test
  fun `successful read is cached and the delegate is not called again`() {
    var reads = 0
    val provider = CachingAppVersionProvider(
      AppVersionProvider {
        reads += 1
        "1.2.3"
      },
    )

    repeat(3) {
      assertEquals("1.2.3", provider.releaseVersion())
    }

    assertEquals(1, reads)
  }

  @Test
  fun `failed read is retried on the next call`() {
    var reads = 0
    val provider = CachingAppVersionProvider(
      AppVersionProvider {
        reads += 1
        if (reads == 1) throw LaunchingServiceException.InvalidAppVersion()
        "2.0.0"
      },
    )

    try {
      provider.releaseVersion()
      fail("Expected InvalidAppVersion")
    } catch (_: LaunchingServiceException.InvalidAppVersion) {
      // Expected.
    }

    assertEquals("2.0.0", provider.releaseVersion())
    assertEquals("2.0.0", provider.releaseVersion())
    assertEquals(2, reads)
  }
}
