# Dependency resolution and security checks

The Maven manifest is `demo/pom.xml`. It inherits Spring Boot 4.0.8's dependency
management BOM. Dependencies managed by that BOM deliberately omit duplicate
versions; Lombok and ZXing have explicit versions. The Maven wrapper pins Maven
and verifies its distribution checksum. Maven has no native lockfile in this
project. A generated dependency list or SBOM is an inventory, not a frozen lockfile.

`demo/dependency-snapshot.json` records the sorted, versioned Maven package URLs
from the complete resolved SBOM (including test and transitive dependencies). CI
compares each build against this reviewed inventory, rejecting additions, removals
or version changes. Build timestamps and component ordering do not cause drift.
This is a drift gate, not a resolver lock or an artifact-integrity guarantee;
it does not pin Maven plugins or replace the OSV vulnerability scan.

After an intentional dependency update, run from the repository root:

```sh
cd demo
./mvnw --batch-mode --no-transfer-progress clean verify
cd ..
python3 scripts/check-dependency-snapshot.py demo/target/classes/META-INF/sbom/application.cdx.json demo/dependency-snapshot.json --update
git diff -- demo/dependency-snapshot.json
```

Review the changed coordinates and commit the snapshot with the manifest update
and its tests. CI only checks the snapshot; it never automatically accepts drift.
Missing/malformed/empty SBOMs and snapshot dependency versions fail the check.

Security overrides currently select Tomcat 11.0.27 and Jackson BOM 3.1.7 and Jackson 2 BOM 2.21.7. Keep them
until a tested Spring Boot parent manages equivalent or newer fixes. Before removing
an override, run the full build and scan the resulting dependency graph.

From `demo/`:

```sh
./mvnw clean verify
./mvnw dependency:tree
./mvnw versions:display-dependency-updates
```

The build produces `target/classes/META-INF/sbom/application.cdx.json`. CI uploads
this exact resolved CycloneDX inventory, including test dependencies, and passes it
to the pinned OSV scanner reusable workflow. The scanner fails on known advisories;
it does not infer absence of vulnerabilities from missing manifests. Dependabot
opens separate Maven, Docker and GitHub Actions updates. A green existing version
does not establish that a major version bump is compatible.

The Prometheus registry follows the Boot BOM. JaCoCo 0.8.13 measures service line
coverage and fails `verify` below 70%. Checkstyle and Spotless run in `validate`.
Maven build plugins and container OS packages are outside the application SBOM's
security coverage. Run an image scanner and review deployment-specific exposure
before production deployment; application dependency results alone are insufficient.

Flyway and Spring Session JDBC follow the Boot BOM. Flyway introduces Jackson 2 in
addition to MVC's Jackson 3, so both BOM lines are patched independently. The runtime
OS scanner runs separately from the Maven SBOM and preserves unfixed advisories.
The optional Flyway Maven CLI is configured with clean disabled and baseline version
1; use it only after the reviewed clone/backup/adoption procedure.
