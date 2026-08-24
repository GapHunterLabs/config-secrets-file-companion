# Demo data for screenshots

`application.yml` — `database.password` has a real high-entropy value
(flagged), `aws.access-key` is a placeholder (not flagged).

## How to get the screenshot

1. `./gradlew runIde` from `config-secrets-file-companion`, open this
   `demo/` folder as the project.
2. Full Screen, open `application.yml` — a warning should appear on
   the `database.password` value but not on `aws.access-key`.
3. Screenshot with both lines visible, save into
   `config-secrets-file-companion/docs/screenshots/`. Close the
   sandbox.
