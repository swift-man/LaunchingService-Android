package me.gorani.launchingservice

public class LaunchingStatusComparator(
  private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
  public fun compare(
    releaseVersion: String,
    launching: Launching,
  ): AppUpdateStatus {
    compareUpdate(releaseVersion, launching.forceUpdate)?.let { return it }

    val isBlacklisted = launching.blackListVersions.any { version ->
      NumericVersionComparator.compare(version.trim(), releaseVersion) == 0
    }
    if (isBlacklisted) {
      return AppUpdateStatus.ForcedUpdateRequired(launching.forceUpdate.toAlert())
    }

    compareUpdate(releaseVersion, launching.optionalUpdate, forced = false)?.let { return it }

    val notice = launching.notice
    if (notice != null && nowEpochMillis() in notice.startEpochMillis..notice.endEpochMillis) {
      return AppUpdateStatus.Notice(
        NoticeAlert(
          title = notice.title,
          message = notice.message,
          terminateOnDismiss = notice.terminateOnDismiss,
          doneUri = notice.doneUri,
        ),
      )
    }

    return AppUpdateStatus.Valid
  }

  private fun compareUpdate(
    releaseVersion: String,
    update: AppUpdateInfo,
    forced: Boolean = true,
  ): AppUpdateStatus? {
    if (update.version.isBlank()) return null
    if (NumericVersionComparator.compare(releaseVersion, update.version) >= 0) return null

    return if (forced) {
      AppUpdateStatus.ForcedUpdateRequired(update.toAlert())
    } else {
      AppUpdateStatus.OptionalUpdateRequired(update.toAlert())
    }
  }

  private fun AppUpdateInfo.toAlert(): UpdateAlert = UpdateAlert(
    title = alertTitle,
    message = alertMessage,
    doneUri = alertDoneUri,
  )
}
