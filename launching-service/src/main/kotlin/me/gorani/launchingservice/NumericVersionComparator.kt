package me.gorani.launchingservice

import java.math.BigInteger

internal object NumericVersionComparator {
  private val tokenPattern = Regex("\\d+|\\D+")

  fun compare(left: String, right: String): Int {
    val leftComponents = left.split('.').toMutableList()
    val rightComponents = right.split('.').toMutableList()
    val componentCount = maxOf(leftComponents.size, rightComponents.size)

    while (leftComponents.size < componentCount) leftComponents += "0"
    while (rightComponents.size < componentCount) rightComponents += "0"

    for (index in 0 until componentCount) {
      val result = compareComponent(leftComponents[index], rightComponents[index])
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
        leftToken.all(Char::isDigit) && rightToken.all(Char::isDigit) ->
          BigInteger(leftToken.ifEmpty { "0" }).compareTo(BigInteger(rightToken.ifEmpty { "0" }))
        else -> leftToken.compareTo(rightToken)
      }
      if (result != 0) return result
    }
    return 0
  }
}
