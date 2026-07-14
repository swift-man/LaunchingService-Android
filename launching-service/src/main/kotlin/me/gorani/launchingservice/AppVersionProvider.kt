package me.gorani.launchingservice

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

internal fun interface AppVersionProvider {
  fun releaseVersion(): String
}

internal class PackageManagerAppVersionProvider(
  private val context: Context,
) : AppVersionProvider {
  private val cachedReleaseVersion: String by lazy { readReleaseVersion() }

  override fun releaseVersion(): String = cachedReleaseVersion

  private fun readReleaseVersion(): String {
    val packageInfo = try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(
          context.packageName,
          PackageManager.PackageInfoFlags.of(0),
        )
      } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
      }
    } catch (error: PackageManager.NameNotFoundException) {
      throw LaunchingServiceException.InvalidAppVersion(error)
    }

    return packageInfo.versionName
      ?.trim()
      ?.takeIf(String::isNotEmpty)
      ?: throw LaunchingServiceException.InvalidAppVersion()
  }
}
