<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Config Secrets File Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- Warning on a suspected real secret hardcoded in a `.properties`,
  `.env`, or `.yml`/`.yaml` file -- known credential-format signatures
  plus a variable-name + Shannon-entropy heuristic.
- 100% static text analysis, no network calls, no telemetry. Free.

[Unreleased]: https://github.com/GapHunterLabs/config-secrets-file-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/config-secrets-file-companion/commits/0.1.0
