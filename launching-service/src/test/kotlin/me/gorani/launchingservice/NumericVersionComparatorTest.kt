package me.gorani.launchingservice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumericVersionComparatorTest {
  @Test
  fun `equal versions compare equally`() {
    assertEquals(0, NumericVersionComparator.compare("1.2.0", "1.2"))
  }

  @Test
  fun `numeric components do not use lexicographic order`() {
    assertTrue(NumericVersionComparator.compare("1.10.0", "1.2.0") > 0)
  }

  @Test
  fun `missing components are padded with zero`() {
    assertEquals(0, NumericVersionComparator.compare("2", "2.0.0"))
  }

  @Test
  fun `non-numeric suffixes remain deterministic`() {
    assertTrue(NumericVersionComparator.compare("1.0.0-beta2", "1.0.0-beta10") < 0)
  }

  @Test
  fun `suffix sorts after its shorter prefix like Apple numeric comparison`() {
    assertTrue(NumericVersionComparator.compare("1.0.0-beta2", "1.0.0") > 0)
  }

  @Test
  fun `non-numeric tokens remain case sensitive like Apple numeric comparison`() {
    assertTrue(NumericVersionComparator.compare("1.0-RC", "1.0-rc") < 0)
  }
}
