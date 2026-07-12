package me.gorani.launchingservice

public sealed class LaunchingServiceException(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause) {
  public class InvalidAppVersion(cause: Throwable? = null) : LaunchingServiceException(
    message = "The application versionName is missing or blank.",
    cause = cause,
  )

  public class FirebaseNotConfigured(cause: Throwable) : LaunchingServiceException(
    message = "Firebase is not configured for the application.",
    cause = cause,
  )
}
