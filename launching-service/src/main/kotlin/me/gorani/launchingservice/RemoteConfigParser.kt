package me.gorani.launchingservice

import java.net.URI

internal class RemoteConfigParser(
  private val values: RemoteConfigClient,
  private val keys: RemoteConfigKeys,
) {
  fun parse(): Launching {
    val forceUpdate = parseForceUpdate()
    return Launching(
      forceUpdate = forceUpdate ?: inactiveUpdate,
      optionalUpdate = parseOptionalUpdate(),
      blackListVersions = parseBlackListVersions(forceUpdate != null),
      notice = parseNotice(),
    )
  }

  private fun parseForceUpdate(): AppUpdateInfo? {
    val doneUri = parseUri(values.stringValue(keys.forceUpdate.alertDoneUri))
      ?: return null

    return AppUpdateInfo(
      version = values.stringValue(keys.forceUpdate.appVersion).trim(),
      alertTitle = values.stringValue(keys.forceUpdate.alertTitle),
      alertMessage = values.stringValue(keys.forceUpdate.alertMessage),
      alertDoneUri = doneUri,
    )
  }

  private fun parseBlackListVersions(hasForceUpdateDestination: Boolean): List<String> {
    if (!hasForceUpdateDestination) return emptyList()

    return values.stringValue(keys.forceUpdate.blackListVersions)
      .split(',')
      .map(String::trim)
      .filter(String::isNotEmpty)
  }

  private fun parseOptionalUpdate(): AppUpdateInfo {
    val version = values.stringValue(keys.optionalUpdate.appVersion).trim()
      .takeIf(String::isNotEmpty)
      ?: return inactiveUpdate
    val doneUri = parseUri(values.stringValue(keys.optionalUpdate.alertDoneUri))
      ?: return inactiveUpdate

    return AppUpdateInfo(
      version = version,
      alertTitle = values.stringValue(keys.optionalUpdate.alertTitle),
      alertMessage = values.stringValue(keys.optionalUpdate.alertMessage),
      alertDoneUri = doneUri,
    )
  }

  private fun parseNotice(): NoticeInfo? {
    val start = Iso8601Parser.parseEpochMillis(values.stringValue(keys.notice.startDate))
      ?: return null
    val end = Iso8601Parser.parseEpochMillis(values.stringValue(keys.notice.endDate))
      ?: return null
    if (start >= end) return null

    return NoticeInfo(
      title = values.stringValue(keys.notice.alertTitle),
      message = values.stringValue(keys.notice.alertMessage),
      terminateOnDismiss = values.booleanValue(keys.notice.terminateOnDismiss),
      startEpochMillis = start,
      endEpochMillis = end,
      doneUri = parseUri(values.stringValue(keys.notice.alertDoneUri)),
    )
  }

  private fun parseUri(value: String): URI? {
    val candidate = value.trim().takeIf(String::isNotEmpty) ?: return null
    return runCatching { URI(candidate) }
      .getOrNull()
      ?.takeIf { !it.scheme.isNullOrBlank() }
  }

  private companion object {
    val inactiveUpdate = AppUpdateInfo(
      version = "",
      alertTitle = "",
      alertMessage = "",
      alertDoneUri = URI("about:blank"),
    )
  }
}
