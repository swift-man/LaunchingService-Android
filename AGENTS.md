# AGENTS.md

## Repository Guidelines

- This repository contains the native Android implementation of LaunchingService.
- Do not introduce Kotlin Multiplatform.
- Keep the `launching-service` module free of Compose, ViewModel, Activity, and Fragment dependencies.
- Keep presentation examples in the `sample` module using MVVM, StateFlow, and Jetpack Compose.
- Preserve the observable Remote Config behavior documented in README.md and the iOS package.
- Follow SOLID design principles and inject Firebase, app-version, and clock boundaries for tests.

## Build And Test

Run these commands after code changes:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
./gradlew :launching-service:publishToMavenLocal
```

Tests must not make Firebase network calls. Add regression coverage for every behavior change.

## Pull Requests

- Create Ready pull requests, never draft pull requests.
- Prefix PR and commit titles with an appropriate type such as `feat.`, `fix.`, `docs.`, or `chore.`.
- Do not use `[Codex]` or `[codex]` in PR titles.
- Address review threads with evidence and identify false positives explicitly.
- Delete merged feature branches.
