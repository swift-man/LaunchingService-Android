package me.gorani.launchingservice

import java.math.BigInteger

internal object NumericVersionComparator {
  private val tokenPattern = Regex("[0-9]+|[^0-9]+")

  fun compare(left: String, right: String): Int {
    val leftComponents = left.split('.')
    val rightComponents = right.split('.')
    val componentCount = maxOf(leftComponents.size, rightComponents.size)

    for (index in 0 until componentCount) {
      val result = compareComponent(
        leftComponents.getOrNull(index) ?: "0",
        rightComponents.getOrNull(index) ?: "0",
      )
      if (result != 0) return result
    }
    return 0
  }

  private fun compareComponent(left: String, right: String): Int {
    val leftTokens = tokenPattern.findAll(left).map(MatchResult::value).toList()
    val rightTokens = tokenPattern.findAll(right).map(MatchResult::value).toList()
    val tokenCount = maxOf(leftTokens.size, rightTokens.size)

    for (index in 0 until tokenCount) {
      val leftToken = leftTokens.getOrNull(index) ?: ""
      val rightToken = rightTokens.getOrNull(index) ?: ""
      val result = when {
        leftToken.isAsciiDigits() && rightToken.isAsciiDigits() ->
          BigInteger(leftToken).compareTo(BigInteger(rightToken))
        else -> leftToken.compareTo(rightToken)
      }
      if (result != 0) return result
    }
    return 0
  }

  private fun String.isAsciiDigits(): Boolean = isNotEmpty() && all { it in '0'..'9' }
}
