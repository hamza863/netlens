# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[1.1.0]: https://github.com/hamza863/netlens/releases/tag/v1.1.0
