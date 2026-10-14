# Contributing

EKOTE is a Java backend/web application. Work from the existing checkout and the
`demo/` Maven module with a Java 21 JDK. Tests use H2 and require no MySQL server.
For the actual MySQL application, follow the Docker Compose instructions in README.

Before submitting a change:

```bash
cd demo
./mvnw spotless:apply
./mvnw clean verify
```

`validate` enforces Google Java Format through Spotless and Checkstyle rules for
empty catch blocks, fall-through, equals/hashCode pairs, modifier order, source
filenames, and direct console printing. CI checks formatting; it does not edit files.

Include tests that pin the changed behavior. Unit tests mock repository boundaries;
repository tests execute real queries against H2; HTTP integration tests cover
security and validation, including a real servlet error-dispatch test. A passing H2
suite does not prove MySQL-specific behavior or concurrent stock-update correctness.

Keep commits focused. Use descriptive conventional messages such as
`fix: reject incomplete item batches` or `test: cover missing inventory`. Avoid
mixing repository-wide formatting with feature changes. Do not rewrite previous
history, create empty commits, or change author identities to improve mining metrics.

CI separates lint, tests, and packaging. OSV scans Maven dependencies on pushes and
pull requests; dependency review rejects new high/critical advisories in PRs. An
advisory or scanner/network failure should be investigated, not suppressed merely
to get a green run. Dependency review requires GitHub's dependency graph support
(public repositories, or the applicable security feature for private repositories).
OSV needs access to its public vulnerability and dependency metadata APIs.

Keep secret values out of code, tests, logs, and commits. `.env.example` contains only
local-development defaults. Public registration must not choose administrator roles;
CSRF tokens and server-side validation are required for mutations. Report sensitive
security issues through a private channel agreed with the maintainer rather than
posting credentials or exploit details in public issues.
