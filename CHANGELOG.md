# Changelog

## Unreleased

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
