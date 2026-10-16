# Security verification

## Verified dependency audit — 2026-10-15

OSV-Scanner v2.7.0 scanned a freshly generated CycloneDX 1.6 SBOM covering
**167 resolved Maven components**, including compile/runtime and test dependencies.
The scanner completed with **exit code 0 and zero known vulnerability findings**.
TLS verification remained enabled. No advisory suppressions or severity exclusions
were used.

The initial test-inclusive scan of the previous Spring Boot 3.5.16 build found
17 advisory IDs across Jackson core/databind, Spring MVC, Tomcat, and Log4j API.
Remediation uses Spring Boot **4.0.8**, Spring Framework **7.0.9**, Jackson
**3.1.7** (plus Jackson 2 BOM **2.21.7** for Flyway), Tomcat **11.0.27**, and Log4j API **2.25.5**. The complete application
test suite passed after migration. The Tomcat and Jackson overrides are security
patches to the parent's managed versions; keep the related modules aligned.

## Reproduce and keep checking

With Java 21 installed:

```bash
cd demo
./mvnw clean verify
osv-scanner scan source --allow-no-lockfiles=false --format=json \
  -L target/classes/META-INF/sbom/application.cdx.json
```

Use OSV-Scanner v2.7.0 for reproducing this result. The scanner needs access to
`api.osv.dev`, `api.deps.dev`, and Maven Central; the application tests do not need
OSV connectivity. The scanner exits nonzero on findings or scan failures.

CI generates this SBOM during the test job and uploads the `resolved-sbom`
artifact. The pinned OSV reusable workflow scans that artifact, fails on findings,
and preserves its results. A missing SBOM fails artifact upload rather than
silently passing an empty scan. PR dependency review additionally rejects new
high/critical advisories. Dependabot proposes dependency updates weekly.

## Scope and limitations

This is a point-in-time software-composition scan, not a guarantee that the
application is bug-free or that future advisories cannot affect it. It does not
scan operating-system packages in container base images, Maven plugin internals,
or application logic for unknown vulnerabilities. The test suite separately
checks authorization, CSRF, registration identity/role protection, request
validation, inventory workflows and concurrent issue/return transactions. The new
concurrency regressions pass against H2 and MySQL 8.4 under repeatable-read isolation,
including oversell, repeated return, overflow and atomic rollback cases. This evidence
does not establish correctness for every possible production workload.

Management metrics require ADMIN on the application listener. The Kubernetes setup
uses a separate internal management port; only health and Prometheus are permitted
there, behind network policies. The evaluation cluster now explicitly installs
Calico; actual allowed/denied flows and a deny→allow→deny control were verified.
Never expose that listener publicly. Stock audit records are actor-attributed and
transactional, but database administrators can modify them. They are not tamper-proof.

Keep credentials out of issues, logs, commits, and shared reports. Coordinate
sensitive application vulnerability reports privately with the maintainer.

## Container OS audit — 2026-10-16

The application runtime now uses digest-pinned Temurin Java 21 on Alpine 3.24.2,
with signed vendor updates for libpng 1.6.59-r0 and zlib 1.3.2-r1. A fresh Trivy
scan of the completed image detected **73 OS packages and 0 advisories at all
severities**, with unfixed findings included. The previous patched Ubuntu runtime
had 50 residual MEDIUM/LOW package/advisory entries; the unpatched Alpine base had
two MEDIUM entries, both resolved by these updates.

This is a dated result for OS packages, not a guarantee of zero vulnerabilities.
The final container ran as UID 10001 with a read-only root filesystem, dropped
capabilities and temporary storage. Real MySQL-backed login/logout across two
containers passed. Re-scan as vendor fixes and advisories change.

CI preserves the complete OS JSON report and rejects any HIGH/CRITICAL advisory,
including unfixed ones. An unsupported/empty scan fails rather than passing silently.
Application Maven dependencies remain a separate OSV gate. Build/plugin internals,
Kubernetes/CNI images and deployment-provider controls are outside these two scan
scopes. The pinned kind/Calico compatibility lab must not be treated as a maintained
production Kubernetes distribution.

## Application security regressions

New tests reject USER access to personal record-book/detail endpoints, preserve
ADMIN-only reporting, and verify concurrent duplicate registration cannot merge or
overwrite another identity. JDBC session tests use independent repositories and
actual separate JVMs; logout on one process revokes access on the other. Backup
verification restores a consistent private archive and compares all data rows; corrupt
archives and nonempty targets are rejected. Network tests verify real enforcement.
CodeQL security-extended analysis now runs with a local SARIF severity gate and
retains complete reports as artifacts. Code scanning is not enabled in the repository,
so dashboard upload is disabled while analysis and the failure gate remain active.
The first hosted report identified three medium log-injection findings; raw
user-supplied identifiers were removed from those logs. Final hosted validation is
recorded in the Actions runs rather than inferred from workflow configuration.

Shared database authentication limits cover concurrent login/register attempts
across app replicas and fail closed during database outages. They use the observed
network address, with no implicit trust in forwarding headers. Basic limits do not
replace provider ingress/WAF controls for distributed traffic. A production profile
sets Secure session cookies; servlet and Alpine HTTP checks verify the flags.

Production HTTPS termination, trusted proxy configuration, managed HA,
encrypted offsite backup retention and ingress exposure require validation on the
actual provider. No external production service has been provisioned or certified.
