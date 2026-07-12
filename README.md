# LaunchingService Android

Firebase Remote Config를 사용해 앱 시작 시 강제 업데이트, 버전 블랙리스트, 선택 업데이트, 공지 상태를 판정하는 Android 라이브러리입니다.

이 저장소는 [LaunchingService for Apple platforms](https://github.com/swift-man/LaunchingService)의 Android 구현입니다. Kotlin Multiplatform을 사용하지 않으며 Android 소비자는 Maven AAR만 받습니다.

## Requirements

- Android API 23+
- JDK 17
- Firebase가 구성된 Android 애플리케이션

## Installation

`0.1.0` 릴리스가 Maven Central에 게시된 후 앱 모듈에 추가합니다.

```kotlin
dependencies {
  implementation("me.gorani:launching-service:0.1.0")
}
```

Firebase 의존성은 Firebase Android BoM으로 정렬하는 것을 권장합니다.

```kotlin
dependencies {
  implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
  implementation("com.google.firebase:firebase-config")
}
```

## Usage

Firebase를 초기화한 뒤 `LaunchingService`를 생성하고 상태를 가져옵니다.

```kotlin
val launchingService = LaunchingService(applicationContext)

when (val status = launchingService.fetchAppUpdateStatus()) {
  AppUpdateStatus.Valid -> Unit
  is AppUpdateStatus.ForcedUpdateRequired -> showForcedUpdate(status.alert)
  is AppUpdateStatus.OptionalUpdateRequired -> showOptionalUpdate(status.alert)
  is AppUpdateStatus.Notice -> showNotice(status.alert)
}
```

다른 Firebase app의 Remote Config 인스턴스를 사용할 수도 있습니다.

```kotlin
val launchingService = LaunchingService(
  context = applicationContext,
  remoteConfig = FirebaseRemoteConfig.getInstance(firebaseApp),
)
```

`fetchAndActivate()` 네트워크 요청이 실패하면 현재 activated 값, 앱 기본값, Firebase 정적 기본값 순서로 계속 판정합니다. coroutine cancellation은 삼키지 않습니다. Firebase default app 자체가 구성되지 않은 경우 `LaunchingServiceException.FirebaseNotConfigured`가 발생합니다.

## Architecture

`launching-service` 모듈은 Compose, ViewModel, Activity, Fragment에 의존하지 않습니다. 화면 구성은 소비 앱의 책임입니다.

```text
Firebase Remote Config
        |
        v
RemoteConfigParser
        |
        v
LaunchingStatusComparator
        |
        v
AppUpdateStatus
        |
        v
ViewModel -> StateFlow -> Compose
```

`sample` 모듈은 Google 권장 방식에 맞춰 screen-level ViewModel이 단일 `StateFlow`를 노출하고 Compose가 `collectAsStateWithLifecycle()`로 수집합니다.

## Remote Config

iOS와 Android는 같은 키 이름을 사용합니다. 값은 Firebase Remote Config의 App ID 조건으로 분리합니다. Parameter Group은 콘솔 정리용일 뿐 namespace가 아니므로 그룹별로 같은 키를 중복 생성할 수 없습니다.

```text
forceUpdateAppVersionKey
|- iOS App ID value:     1.9.0
`- Android App ID value: 2.3.0
```

조건이 여러 개 일치하면 Firebase Console의 위쪽 조건이 우선합니다. 각 앱에 대한 App ID 조건을 만들고 기본값은 빈 문자열 또는 안전한 값으로 설정하세요.

### Required Values

| Feature | Required values | Optional values |
| --- | --- | --- |
| Force update | `forceUpdateAppVersionKey`, absolute `forceUpdateAlertDoneLinkURLKey` | title, message |
| Blacklist | comma-separated `blackListVersionsKey`, absolute `forceUpdateAlertDoneLinkURLKey` | title, message |
| Optional update | `optionalUpdateAppVersionKey`, absolute `optionalUpdateAlertDoneLinkURLKey` | title, message |
| Notice | ISO 8601 `noticeStartDateKey`, `noticeEndDateKey` with start before end | title, message, done URL, terminate flag |

### Default Keys

| Group | Key | Type | Missing or invalid value |
| --- | --- | --- | --- |
| Force | `forceUpdateAppVersionKey` | String | Force version check is skipped; blacklist can still run |
| Force | `forceUpdateAlertTitleKey` | String | Empty title is returned |
| Force | `forceUpdateAlertMessageKey` | String | Empty message is returned |
| Force | `forceUpdateAlertDoneLinkURLKey` | Absolute URI String | Force and blacklist are disabled |
| Force | `blackListVersionsKey` | Comma-separated String | Blacklist is skipped |
| Optional | `optionalUpdateAppVersionKey` | String | Optional update is disabled |
| Optional | `optionalUpdateAlertTitleKey` | String | Empty title is returned |
| Optional | `optionalUpdateAlertMessageKey` | String | Empty message is returned |
| Optional | `optionalUpdateAlertDoneLinkURLKey` | Absolute URI String | Optional update is disabled |
| Notice | `noticeAlertTitleKey` | String | Empty title is returned |
| Notice | `noticeAlertMessageKey` | String | Empty message is returned |
| Notice | `noticeStartDateKey` | ISO 8601 String | Notice is disabled |
| Notice | `noticeEndDateKey` | ISO 8601 String | Notice is disabled |
| Notice | `noticeAlertDoneURLKey` | Absolute URI String | Notice remains active with `doneUri = null` |
| Notice | `noticeAlertDismissedTerminateKey` | Boolean | `false` |

Accepted notice timestamps include UTC or explicit offsets, with optional millisecond precision:

```text
2026-07-12T00:00:00Z
2026-07-12T09:00:00+09:00
2026-07-12T09:00:00+0900
2026-07-12T00:00:00.123Z
```

The start must be earlier than the end. Once parsed, both notice boundaries are inclusive when the current time is evaluated.

### Evaluation Order

```text
force update
  -> blacklist force update
  -> optional update
  -> notice
  -> valid
```

Version comparison follows the Apple implementation's numeric dotted-string behavior. Missing components are padded with zero, so `1.2` equals `1.2.0`, and numeric tokens make `1.10.0` newer than `1.2.0`. This is not a full Semantic Versioning parser.

### Custom Keys

```kotlin
val keys = RemoteConfigKeys(
  forceUpdate = RemoteConfigKeys.ForceUpdateKeys(
    appVersion = "android_force_update_version",
    alertDoneUri = "android_play_store_url",
  ),
)

val launchingService = LaunchingService(
  context = applicationContext,
  keys = keys,
)
```

## Sample App

The sample compiles without Firebase credentials and shows a recoverable configuration error. To use a real Firebase project, configure Firebase in the sample application before running it. The library never bundles `google-services.json`.

## Development

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
./gradlew :launching-service:publishToMavenLocal
```

The test suite uses injected Remote Config, app-version, and clock boundaries and never calls Firebase over the network.

## Release

GitHub Releases trigger Maven Central publication. The repository requires these secrets:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `MAVEN_SIGNING_KEY`
- `MAVEN_SIGNING_PASSWORD`

The release tag must equal `v$(cat VERSION.txt)`.

## License

MIT License. See [LICENSE](LICENSE).
