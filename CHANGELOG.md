# Changelog

## 2026-10-20 — Runtime logging and dependency drift checks

- Enable built-in structured JSON console logging in the production profile.
- Verify production log encoding, request correlation and MDC cleanup in a separate JVM.
- Record the resolved Maven dependency inventory and reject unreviewed drift in CI;
  keep the full OSV scan and reject missing, malformed or empty inventories.
- Patch runtime libexpat to 2.9.0-r0 for CVE-2026-77214 and CVE-2026-102633.

## 2026-10-16 — Runtime and authentication hardening

- Remove the CDN-hosted Materialize 1.0.0 bundle affected by four XSS advisories;
  lock and serve maintained browser assets locally with licenses and checksums.
- Preserve navigation/form styling during the component API migration and add npm
  auditing, deterministic asset checks and real Chromium security regressions.
- Extend CodeQL to first-party browser JavaScript as well as Java.

- Replace the Ubuntu runtime with pinned Alpine Java 21 and signed libpng/zlib fixes;
  scan all 73 OS packages without severity or unfixed-advisory suppression.
- Make QR destinations deployment-specific, encode identifiers, and test PNG decoding.
- Add a production HTTPS-cookie profile with real servlet/container flag checks.
- Share atomic login/register attempt limits across replicas, with expiry cleanup,
  fail-closed behavior, a V4 migration and H2/real-MySQL concurrency regressions.
- Remove three CodeQL log-injection sources, validate CodeQL query-pack SARIF metadata,
  and retain security reports without requiring the code-scanning dashboard feature.
- Fix direct-network Docker builds when optional proxy variables are absent.

## 2026-10-15 — Operational hardening

- Add Flyway schema migrations, explicit legacy adoption tests and shared JDBC
  sessions; verify authentication/logout across two JVMs.
- Add private streaming backups, checksum/CRC verification, empty-target safeguards
  and an exact-row restore drill in a disposable MySQL database.
- Restrict personal record pages to ADMIN and prevent registration identity merges
  under concurrency using INSERT and database uniqueness.
- Patch Jackson 2 transitives and runtime OS packages; keep complete residual image
  findings visible and gate HIGH/CRITICAL advisories in CI.
- Verify Calico network-policy enforcement with allow/deny traffic and a controlled
  deny→allow→deny flow. Add two app replicas, rolling updates and a disruption budget.
- Configure release integration, image audit and CodeQL workflows.

## 2026-10-15

- Serialize stock mutations and active-issue reads under row locks; commit stock,
  issue status and actor-attributed audit records atomically. Concurrent issue,
  return, oversell, overflow and rollback regression tests cover H2 and real MySQL.
- Add bounded admin inventory summary, low-stock, due-maintenance and audit reports,
  plus formula-safe CSV export with an explicit 5,000-row maximum.
- Fix maintenance scheduling to use the performed date and validate intervals.
- Expose protected metrics and count only committed stock movements; add request IDs.
- Enforce 70% service line coverage in verify and publish JaCoCo reports in CI.
- Add restricted Kubernetes workloads, MySQL PVC, separate probes/management listener,
  network policies, policy regression checks and disposable-cluster HTTP smoke tests.
- Remove unused AuthService and fake user issue/return methods.

## Unreleased

- Migrate to Spring Boot 4.0.8 / Spring Framework 7 to remediate Spring MVC advisories.
- Pin patched Jackson 3.1.7 and Tomcat 11.0.27; the parent manages patched Log4j 2.25.5.
- Adapt modular Spring Boot test annotations and remove the obsolete explicit MySQL dialect.
- Generate a test-inclusive CycloneDX SBOM and scan resolved components in CI.
- Verify an OSV scan of 149 Maven components with no known findings on 2026-10-14.

- Move issue validation and stock updates into a transactional service, retaining HTTP behavior.
- Validate entire item batches before saving; reject duplicate identifiers and invalid dates.
- Correct inventory record queries and handle JDBC date and numeric values safely.
- Add unit and repository regression tests for issue, item batch, and record workflows.
- Enforce Java formatting and Checkstyle; separate CI lint, test, and package jobs.
- Add OSV dependency scanning and pull-request dependency review.
- Update compatible Spring Boot, Lombok, and ZXing versions and remove the unused Firebase SDK.

## Initial quality improvements

- Add isolated H2 and HTTP tests, including embedded servlet error dispatch.
- Enable CSRF protection and restrict administrative mutations.
- Prevent public administrator registration and registration over an existing identity.
- Add Java 21 CI, Dependabot, a health endpoint, and digest-pinned Docker Compose development setup.
