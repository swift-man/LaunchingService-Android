package me.gorani.launchingservice

import java.net.URI

public data class Launching(
  val forceUpdate: AppUpdateInfo,
  val optionalUpdate: AppUpdateInfo,
  val blackListVersions: List<String>,
  val notice: NoticeInfo?,
)

public data class AppUpdateInfo(
  val version: String,
  val alertTitle: String,
  val alertMessage: String,
  val alertDoneUri: URI,
)

public data class NoticeInfo(
  val title: String,
  val message: String,
  val terminateOnDismiss: Boolean,
  val startEpochMillis: Long,
  val endEpochMillis: Long,
  val doneUri: URI?,
)
