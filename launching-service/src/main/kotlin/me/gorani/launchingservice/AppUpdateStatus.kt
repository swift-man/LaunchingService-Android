package me.gorani.launchingservice

import java.net.URI

public sealed interface AppUpdateStatus {
  public data object Valid : AppUpdateStatus

  public data class ForcedUpdateRequired(
    val alert: UpdateAlert,
  ) : AppUpdateStatus

  public data class OptionalUpdateRequired(
    val alert: UpdateAlert,
  ) : AppUpdateStatus

  public data class Notice(
    val alert: NoticeAlert,
  ) : AppUpdateStatus
}

public data class UpdateAlert(
  val title: String,
  val message: String,
  val doneUri: URI,
)

public data class NoticeAlert(
  val title: String,
  val message: String,
  val terminateOnDismiss: Boolean,
  val doneUri: URI?,
)
