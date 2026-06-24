# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.3] - 2026-06-24

### Fixed
- **Manifest merge conflict with apps/libraries that use `FileProvider`.** NetLens
  registered the bare `androidx.core.content.FileProvider` class, which the manifest
  merger keys by class name — colliding with the host app's provider and failing the
  build with a duplicate-provider error. NetLens now registers its own
  `NetLensFileProvider` subclass, so it merges cleanly and consumers need no
  `tools:replace` or app-side workaround.

## [1.1.2] - 2026-06-24

Packaging-only release — no library or API changes.

### Changed
- **Maven group ID is now `com.github.hamza863.netlens`** (was
  `com.github.hamza863`). JitPack uses the `com.github.User.Repo` group for
  multi-module projects, so once `netlens-no-op` is published alongside `netlens`
  the coordinates change. Update your dependency notation:
  - `com.github.hamza863.netlens:netlens:1.1.2`
  - `com.github.hamza863.netlens:netlens-no-op:1.1.2`
- The published Gradle module metadata now matches this group, so resolution no
  longer fails with a coordinate-mismatch error.

## [1.1.1] - 2026-06-24

Packaging-only release — no library or API changes.

### Fixed
- Publish the `netlens-no-op` artifact on JitPack (it was missing from the 1.1.0
  build, so `releaseImplementation(...netlens-no-op)` could not resolve).
- Free disk space on the JitPack build runner to avoid intermittent
  "No space left on device" failures during the Android SDK install.

## [1.1.0] - 2026-06-24

### Added
- **Copy as cURL** — reproduce any captured request as a runnable `curl` command.
- **HAR export** — share a whole session as a `.har` file that opens in Chrome
  DevTools, Charles or Proxyman (via a namespaced `FileProvider`).
- **Filter chips** — quick status filters (All / 2xx / 3xx / 4xx / 5xx / Failed)
  alongside the existing text search.
- **Session stats** — header summary of total calls, failures and average duration.
- **Body sizes** — request/response sizes captured and shown per row.
- **Content-type aware rendering** — pretty-print JSON, XML and form-urlencoded
  bodies, preview image responses, and label binary payloads instead of dumping
  unreadable text.
- **Large-body handling** — long payloads are truncated with a one-tap
  "View full body" toggle.
- **Floating bubble** — optional draggable button to open the viewer, enabled with
  `NetLensConfig(showBubble = true)` (handy on emulators).
- **Close button** on the viewer and a full-screen detail view with cURL / Share /
  Copy actions.

### Changed
- Refactored the viewer to a unidirectional **MVI** architecture
  (`NetLensState` / `NetLensIntent` / `NetLensViewModel`), implemented with Compose
  snapshot state so the library stays dependency-free.
- Request duration is now measured after the response body is peeked, so it reflects
  the time until the response is actually usable.
- Increased the default shake debounce to 1000ms to reduce accidental double-triggers.

### Fixed
- The viewer could open twice on a single shake; opening is now guarded against
  duplicate triggers.
- `NetLensConfig.shakeThreshold` was ignored and is now applied to the detector.
- Entry ids are now unique, preventing Compose list key collisions when multiple
  calls were captured in the same millisecond.

[1.1.3]: https://github.com/hamza863/netlens/releases/tag/v1.1.3
[1.1.2]: https://github.com/hamza863/netlens/releases/tag/v1.1.2
[1.1.1]: https://github.com/hamza863/netlens/releases/tag/v1.1.1
[1.1.0]: https://github.com/hamza863/netlens/releases/tag/v1.1.0
