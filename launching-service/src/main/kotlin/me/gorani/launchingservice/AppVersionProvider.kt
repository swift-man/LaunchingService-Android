package me.gorani.launchingservice

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

internal fun interface AppVersionProvider {
  fun releaseVersion(): String
}

/**
 * Caches the first successful [AppVersionProvider.releaseVersion] result.
 *
 * A failed read is not cached, so the next call retries the delegate.
 */
internal class CachingAppVersionProvider(
  private val delegate: AppVersionProvider,
) : AppVersionProvider {
  private val cachedReleaseVersion: String by lazy { delegate.releaseVersion() }

  override fun releaseVersion(): String = cachedReleaseVersion
}

internal class PackageManagerAppVersionProvider(
  private val context: Context,
) : AppVersionProvider {
  override fun releaseVersion(): String {
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
