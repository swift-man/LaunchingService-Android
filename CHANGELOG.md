# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- Added Dokka HTML generation and deployment to the shared documentation site.

### Fixed

- Limited numeric version token parsing to ASCII digits so non-ASCII digits remain deterministic text tokens.
- Cached package manager app-version lookup after the first successful read.
- Cancelled an in-flight sample refresh before starting a new refresh request.

## [0.1.0] - 2026-07-12

### Added

- Added the first native Android implementation of LaunchingService.
- Added an optional observer for recoverable Firebase Remote Config fetch failures.
- Added a Compose sample using MVVM and StateFlow.
- Added Android CI and Maven Central publication configuration.

### Changed

- Added Java-friendly constructor overloads for default arguments.
- Reused a single Firebase Remote Config instance per service client.
