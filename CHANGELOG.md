# Changelog

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
