# Config Secrets File Companion

Warning on a suspected real secret (AWS access key, GitHub/Slack
token, JWT, PEM private key, or a high-entropy value assigned to a
secret-shaped key name) hardcoded directly in a config file —
`.properties`, `.env` (and its variants), or a `.yml`/`.yaml` file.
Config files are a real, common, separate place secrets get committed
by accident — a real value pasted into `application.yml` "just for
local testing" and never removed.

## Why it exists

Source-code secret scanners (this catalog's own
[api-security-companion](https://github.com/GapHunterLabs/api-security-companion)
included) only ever look at Java/Kotlin files. Config files are a
different, equally real leak point, and nothing in the IDE flags a
secret pasted straight into `application.yml` or `.env` today.

## Why built this way

- **100% static text analysis** — known credential-format signatures
  (AWS/GitHub/Slack/JWT/PEM) plus a variable-name + Shannon-entropy
  heuristic for the generic case, no network calls, works offline.
- **Scoped strictly to config file names** — `.properties`, `.env*`,
  `.yml`/`.yaml` — so it never duplicates what a source-code scanner
  already covers.

## v0.1 scope — stated honestly, not exhaustively

Single-line `key=value`/`key: value` pairs only — YAML block scalars
and multi-line values aren't specially handled. The entropy heuristic
is a real heuristic, not a guarantee — it can miss a real secret with
unusually low entropy, or (rarely) flag a long random-looking
non-secret value.

## Usage

Open any `.properties`, `.env`, or `.yml`/`.yaml` file. A line whose
value looks like a real secret shows a warning.

## Support

- **Bugs and feature requests:** [GitHub Issues](https://github.com/GapHunterLabs/config-secrets-file-companion/issues)
- **Questions, or custom rules for a team's codebase:** **gaphunterlabs@gmail.com**
- **Security vulnerabilities:** report privately as described in [SECURITY.md](SECURITY.md), not in a public issue.
- **Privacy and network behavior:** [PRIVACY.md](PRIVACY.md)

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
