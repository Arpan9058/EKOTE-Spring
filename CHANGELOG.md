# Changelog

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
