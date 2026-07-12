package me.gorani.launchingservice

public data class RemoteConfigKeys(
  val forceUpdate: ForceUpdateKeys = ForceUpdateKeys(),
  val optionalUpdate: OptionalUpdateKeys = OptionalUpdateKeys(),
  val notice: NoticeKeys = NoticeKeys(),
) {
  public data class ForceUpdateKeys(
    val appVersion: String = "forceUpdateAppVersionKey",
    val alertTitle: String = "forceUpdateAlertTitleKey",
    val alertMessage: String = "forceUpdateAlertMessageKey",
    val alertDoneUri: String = "forceUpdateAlertDoneLinkURLKey",
    val blackListVersions: String = "blackListVersionsKey",
  )

  public data class OptionalUpdateKeys(
    val appVersion: String = "optionalUpdateAppVersionKey",
    val alertTitle: String = "optionalUpdateAlertTitleKey",
    val alertMessage: String = "optionalUpdateAlertMessageKey",
    val alertDoneUri: String = "optionalUpdateAlertDoneLinkURLKey",
  )

  public data class NoticeKeys(
    val alertTitle: String = "noticeAlertTitleKey",
    val alertMessage: String = "noticeAlertMessageKey",
    val startDate: String = "noticeStartDateKey",
    val endDate: String = "noticeEndDateKey",
    val alertDoneUri: String = "noticeAlertDoneURLKey",
    val terminateOnDismiss: String = "noticeAlertDismissedTerminateKey",
  )
}
