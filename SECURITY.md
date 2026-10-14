# Security verification

## Verified dependency audit — 2026-10-14

OSV-Scanner v2.7.0 scanned a freshly generated CycloneDX 1.6 SBOM covering
**149 resolved Maven components**, including compile/runtime and test dependencies.
The scanner completed with **exit code 0 and zero known vulnerability findings**.
TLS verification remained enabled. No advisory suppressions or severity exclusions
were used.

The initial test-inclusive scan of the previous Spring Boot 3.5.16 build found
17 advisory IDs across Jackson core/databind, Spring MVC, Tomcat, and Log4j API.
Remediation uses Spring Boot **4.0.8**, Spring Framework **7.0.9**, Jackson
**3.1.7**, Tomcat **11.0.27**, and Log4j API **2.25.5**. The complete application
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
validation, and inventory workflows. Concurrent stock-update correctness still
needs further locking/consistency work before production use.

Keep credentials out of issues, logs, commits, and shared reports. Coordinate
sensitive application vulnerability reports privately with the maintainer.
