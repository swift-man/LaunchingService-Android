package me.gorani.launchingservice

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal object Iso8601Parser {
  private val formatters = listOf(
    createFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
    createFormatter("yyyy-MM-dd'T'HH:mm:ssZ"),
  )
  private val timestampPattern = Regex(
    "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(?:\\.(\\d+))?(Z|[+-]\\d{2}:?\\d{2})$",
    RegexOption.IGNORE_CASE,
  )

  fun parseEpochMillis(value: String): Long? {
    val candidate = normalize(value.trim()) ?: return null

    return synchronized(formatters) {
      formatters.firstNotNullOfOrNull { formatter ->
        val position = ParsePosition(0)
        formatter.parse(candidate, position)
          ?.takeIf { position.index == candidate.length }
          ?.time
      }
    }
  }

  private fun createFormatter(pattern: String): SimpleDateFormat =
    SimpleDateFormat(pattern, Locale.US).apply {
      isLenient = false
      timeZone = TimeZone.getTimeZone("UTC")
    }

  private fun normalize(value: String): String? {
    val match = timestampPattern.matchEntire(value) ?: return null
    val dateAndTime = match.groupValues[1]
    val fraction = match.groupValues[2]
      .takeIf(String::isNotEmpty)
      ?.padEnd(3, '0')
      ?.take(3)
      ?.let { ".$it" }
      .orEmpty()
    val offset = match.groupValues[3].let { rawOffset ->
      if (rawOffset.equals("Z", ignoreCase = true)) "+0000" else rawOffset.replace(":", "")
    }
    return dateAndTime + fraction + offset
  }
}
