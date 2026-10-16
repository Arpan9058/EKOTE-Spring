# EKOTE

**Project type: backend/web application (Java + Spring Boot).** The Maven module is
`demo/pom.xml`; application tests are under `demo/src/test/java`. See
[CONTRIBUTING.md](CONTRIBUTING.md) and [CHANGELOG.md](CHANGELOG.md).

EKOTE is a web-based inventory, issue/return, maintenance, and record management system built with Spring Boot. The application provides a centralized interface for managing inventory records, tracking individual items, recording issue transactions, handling maintenance information, and generating QR codes.

## Core Features

### Inventory Management
- Manage inventory and stock information
- Store individual item information using unique identifiers
- Track available quantities and immutable stock-movement snapshots with actor attribution
- Serialize issue, return and adjustment transactions with database row locks
- Prevent items from being issued when inventory is unavailable

### Issue and Return Management
- Search and verify users before issuing an item
- Identify individual items using unique identifiers
- Prevent the same item from being issued multiple times simultaneously
- Automatically update inventory quantity after issue
- Store issue date and current issue status
- Support issue/return record management

### QR Code Support
- Generate QR codes for individual records
- QR functionality powered by ZXing
- Provide QR-based access to stored information

### User Management
- User registration
- Login functionality
- Spring Security integration
- User lookup during issue operations

### Maintenance
- Maintenance management interface
- Store and access maintenance-related information for managed inventory

### Record Management
- Record book functionality
- Central dashboard for accessing major application modules
- Individual inventory information management

## Technology Stack

### Backend
- Java 21
- Spring Boot 4.0.8
- Spring MVC
- Spring Data JPA
- Spring Security
- Maven

### Frontend
- Thymeleaf
- HTML
- CSS
- JavaScript

### Database
- MySQL
- MySQL Connector/J
- Spring Data JPA

### Additional Libraries
- ZXing Core
- ZXing JavaSE
- Lombok

### Deployment
- Docker Compose
- Kubernetes (Kustomize manifests, isolated kind smoke test, workload policy checks)

## Application Modules

The application contains controllers and modules for:

- Home and dashboard
- Registration and authentication
- Inventory management
- Item information management
- Issue and return operations
- Maintenance
- QR code generation
- Record book management

## Project Structure

```text
EKOTE-Spring/
└── demo/
    ├── src/
    │   ├── main/
    │   │   ├── java/com/ekote/
    │   │   │   ├── config/
    │   │   │   ├── controller/
    │   │   │   ├── entities/
    │   │   │   └── repositories/
    │   │   └── resources/
    │   │       ├── static/
    │   │       └── templates/
    │   └── test/
    ├── pom.xml
    ├── Dockerfile
    ├── mvnw
    └── mvnw.cmd
```

## Development and tests

This is a Spring Boot backend web application, not an infrastructure-as-code project.
Use a **Java 21 JDK** (including `javac`), not just a Java runtime. Maven 3.9.11
is pinned by the committed Maven Wrapper; a separate Maven install is unnecessary.

From a fresh clone, run all unit and integration tests and build the executable jar:

```bash
cd demo
./mvnw --batch-mode --no-transfer-progress clean verify
```

Tests use the `test` profile and an in-memory H2 database in MySQL compatibility
mode. They do not need MySQL, Docker, Firebase credentials, or datasource environment
variables. Coverage includes inventory adjustments, issue rejection and stock updates,
password hashing and duplicate registration, HTTP validation, authorization, CSRF,
registration/login, and database-backed inventory requests. A real embedded-Tomcat
test also verifies HTTP 403 survives servlet error dispatch. H2 tests supplement the
MySQL container smoke check; they do not prove every MySQL-specific query is correct. A separate real-MySQL suite verifies migrations,
concurrent stock updates and JDBC session sharing.

GitHub Actions gates tests on lint and enforces at least 70% service line coverage with JaCoCo, then packages the application after tests pass,
and uploads Surefire reports, coverage and the executable jar. A separate MySQL job checks real repeatable-read concurrency; a disposable Kubernetes job validates manifests and exercises the deployed container. `./mvnw validate` checks
Spotless formatting and Checkstyle; `./mvnw spotless:apply` formats Java locally.
The build generates a CycloneDX SBOM, including test dependencies, at
`demo/target/classes/META-INF/sbom/application.cdx.json`. CI uploads it and OSV scans
those actual resolved components on pushes and PRs, while dependency review
rejects new high/critical advisories on PRs. Scanner failures remain failures; no
vulnerability-free claim follows from a passing application test suite. See
[SECURITY.md](SECURITY.md) for the verified audit scope and current results. Dependabot checks Maven dependencies, Actions, and Docker images
weekly. Spring Boot's parent BOM pins versions of managed dependencies; direct
libraries and the Maven Wrapper are pinned in `demo/pom.xml` and
`demo/.mvn/wrapper/maven-wrapper.properties`. Maven does not use an npm-style lockfile.

## Run with Docker Compose

Install Docker with Compose v2, then run:

```bash
cd demo
docker compose up --build -d
docker compose logs -f app
```

Compose starts MySQL 8.4, waits for a successful database query, and starts the app.
The Java and MySQL base images are pinned by digest and monitored by Dependabot.
The Docker image build runs the test suite, and the runtime runs as an unprivileged
user. MySQL data persists in a named volume; the database port is not published.
The application listens on `127.0.0.1:8080` on the host.

Check readiness with an actual application request:

```bash
curl --fail http://localhost:8080/actuator/health
# Expected: status "UP"; includes database health without public component details.
```

Open the application at `http://localhost:8080`, register a user, and sign in.
To stop services while retaining data, run `docker compose down`.
`docker compose down --volumes` deletes the local database.

Optional overrides are documented in `demo/.env.example`: copy it to `.env` to
change `APP_PORT` or `DB_PASSWORD`. The default database password is deliberately
for local development only. Use private credentials for any shared deployment;
`.env` files are ignored by Git and excluded from Docker's build context. Changing
the password after a database volume has been initialized also requires changing
that database user's password; changing an environment variable alone does not do it.
The unused Firebase SDK dependency has been removed; no Firebase credentials are required.

## Run with your own MySQL

Create a MySQL database and a user with access to it, then supply your own credentials
through process environment variables (never commit them):

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/ekote'
export SPRING_DATASOURCE_USERNAME='ekote'
# Set SPRING_DATASOURCE_PASSWORD securely in your shell/environment settings.
cd demo
./mvnw spring-boot:run
```

The application uses versioned Flyway migrations and Hibernate schema validation.
Fresh databases migrate automatically; nonempty legacy databases require explicit,
reviewed adoption. See [database operations](docs/DATABASE_OPERATIONS.md) before
upgrading an existing installation.

## Authentication and request security

Public registration always creates a `USER`; posting `user_role=ADMIN` cannot grant
administrator access. Administrators manage inventory, item batches, issue/return,
and maintenance. Provision an administrator through a trusted database operator:
after registering that user, update **that user's** `register.role` to `ADMIN` using
their verified ID. There is no default administrator or committed administrator
password. Normal login returns to the home page unless an authorized page was requested.

CSRF protection is enabled. Thymeleaf POST forms include tokens automatically;
inventory AJAX requests send the CSRF header, and logout uses POST. Inventory and
batch input validation rejects blank names, nonpositive quantities, unsupported
operations, and incomplete batches with HTTP 400. Batch generation is limited to
1,000 items per request. API clients must use an authenticated session and its CSRF
token for mutations. The public health endpoint exposes only aggregate status.

The browser UI uses external CDN assets, so it requires internet access for those
assets. Offline builds/tests do not exercise browser rendering or every business
workflow. Concurrent issue/return and stock adjustments now lock each model row before mutation.
The stock change, issue status and audit entry commit or roll back together. Audit records
are snapshots retained even if an unreferenced model is removed; they are not tamper-proof
against database administrators. Existing stock history is not fabricated or backfilled.

## How the Issue Workflow Works

When an item is issued, EKOTE:

1. Verifies that the requested user exists.
2. Looks up the individual item using its unique identifier.
3. Confirms that the associated inventory record exists.
4. Checks that the item has not already been issued.
5. Verifies that inventory quantity is available.
6. Decreases the available inventory quantity.
7. Creates an issue record containing the user, item identifier, issue date, and issue status.

This provides traceability between inventory quantities and individual issued items.

## Purpose

EKOTE was developed as a centralized management application for maintaining inventory records and operational workflows such as registration, item tracking, issue/return processing, maintenance, and QR-based identification.

## Repository Status

The project contains the application source code, Maven configuration, frontend templates and resources, database integration, security configuration, and Docker support.

## Inventory reports and observability

Administrators can use these session-authenticated GET endpoints. Normal users receive
403. Pages are zero-based and `size` is bounded to 1–100, with stable secondary sorting:

| Endpoint | Purpose | Parameters |
| --- | --- | --- |
| `/api/inventory/summary` | Model, available-unit, active-issue and maintenance-due counts | none |
| `/api/inventory/low-stock` | Stock at or below a threshold | `threshold` (default 5), `page`, `size` |
| `/api/inventory/maintenance-due` | Scheduled maintenance due today or earlier (UTC) | `page`, `size` |
| `/api/inventory/movements` | Actor-attributed stock audit history | optional `gunId`, ISO instant `from`/`to`, `page`, `size` |
| `/api/inventory/movements.csv` | CSV audit export | optional `gunId`, ISO instant `from`/`to` |

CSV export rejects more than 5,000 matching rows; narrow the filters instead of losing
records silently. Text cells escape quotes/newlines and spreadsheet formula prefixes.
Maintenance completion schedules the next date from today's UTC date, with intervals
of 1–120 months. Unscheduled items are excluded from maintenance-due reports.

`/actuator/metrics` and `/actuator/prometheus` require ADMIN on the normal application
port. The `inventory.movements` and `inventory.units` counters use bounded operation
tags and increment only after transaction commit, so rollbacks are excluded. Counts
reset on process restart; persisted audit records provide the durable history.
`X-Request-ID` is validated/generated and included in responses and log context.
Database conflicts return a generic 409 without SQL details.

For an isolated cluster, probes, non-root workloads, PVC, policy checks and network
limitations, see [Kubernetes setup](deploy/kubernetes/README.md). For dependency pins,
SBOM scope and update commands, see [dependency management](demo/DEPENDENCIES.md).

## Release verification and operational safety

Spring Session JDBC shares login state across application instances. Two independent
JVMs verify cross-instance login, role preservation and logout revocation. Kubernetes
runs two app replicas with rolling updates and a disruption budget; local MySQL and
the local single-node cluster remain single points of failure.

Run the real-MySQL migration suite only against its explicitly disposable database:

```sh
# Supply its database credentials securely through datasource environment variables.
export EKOTE_MIGRATION_TEST_URL='jdbc:mysql://127.0.0.1:3306/ekote_migration_test'
cd demo
./mvnw test -Dtest=MySqlMigrationIntegrationTest
```

The suite resets **only** the explicitly named `ekote_migration_test` schema. Normal
H2 test runs skip these four conditional tests; CI executes them in a separate
MySQL job. CI also verifies multi-process sessions, private backup/restore with exact
row comparison, image OS advisories, and network-policy enforcement with Calico.
CodeQL security-extended analysis is configured separately; configuring its workflow
does not establish a passing hosted analysis run.

Personal record-book and individual detail pages require ADMIN. Registration uses
INSERT plus database uniqueness, preventing an account from being merged/overwritten
when concurrent identity checks race. The runtime uses digest-pinned Alpine Java 21 with signed libpng/zlib security
updates; the latest complete OS scan found zero advisories across 73 packages. Both Jackson 2 (used
by Flyway) and Jackson 3 (used by MVC) have aligned security overrides.

See [security audit scope and remaining advisories](SECURITY.md) and
[database/session/recovery operations](docs/DATABASE_OPERATIONS.md). Production
infrastructure, TLS, encrypted offsite retention and provider credentials must be
configured and verified on the actual deployment target.

## Deployment URL and HTTPS sessions

Set `EKOTE_PUBLIC_BASE_URL` to the canonical address users can reach. Generated QR
codes use this configured origin and encode the item identifier as a path segment;
they no longer point every deployment at the original Railway instance. The local
default is `http://localhost:8080`. With a different Compose `APP_PORT`, set the URL
to that port explicitly. Kubernetes deployments should supply their own canonical
URL through an environment override. Client-supplied Host headers are never used
to choose QR destinations.

For a deployment served through HTTPS, set `SPRING_PROFILES_ACTIVE=production` and
`EKOTE_PUBLIC_BASE_URL=https://your-real-host`. This profile requires the URL and
marks session cookies Secure, HttpOnly and SameSite=Lax. Do not enable it for plain
HTTP development: browsers cannot return Secure cookies over HTTP. The profile
does not provision certificates or an ingress. Forwarded headers remain disabled;
only enable proxy header handling after configuring a trusted proxy that strips
client-supplied forwarding headers.

## Authentication attempt limits

POST login attempts are limited to 20 per minute per observed network address;
registration is limited to 5 per 10 minutes. Both successful and failed attempts
count. Counters live in MySQL and apply across replicas, including concurrent
requests. Excess requests return HTTP 429 with `Retry-After`; database failure
returns a generic 503 and cannot bypass the limit. CSRF validation runs first.
The limiter stores SHA-256 network/route keys rather than raw addresses, usernames
or passwords. Every ten minutes it removes up to 1,000 buckets older than an hour.

Requests behind a proxy share its observed address unless trusted proxy handling
is configured. Forwarding headers from arbitrary clients are ignored by default.
Coordinate limits and trusted headers with your ingress, especially for NAT/shared
networks. These limits supplement edge/WAF controls; they do not prevent distributed
attacks or provision provider-level traffic protection. Keep replica clocks synced.

The dependency security gate scans the complete resolved Maven SBOM with OSV on
every push and pull request, including transitive and test dependencies. GitHub's
separate dependency-review feature is unavailable for this repository, so CI does
not depend on that API. The complete SBOM is retained for review; OSV findings
remain blocking rather than being ignored to work around a platform setting.

## Browser dependencies and checks

The UI serves pinned Materialize 2.4.0, jQuery and Font Awesome assets locally,
with license notices and SHA-256 asset metadata. It no longer downloads the
vulnerable Materialize 1.0.0 bundle or requires a CDN/Google Fonts to render.
`frontend/package-lock.json` records npm integrity hashes; Dependabot checks it
weekly. Vendored libraries are third-party code, not additional original project LOC.

To update browser dependencies with Node 24, run:

```sh
npm ci --ignore-scripts --prefix frontend
npm audit --prefix frontend --audit-level=moderate
npm run sync --prefix frontend
```

Commit the package files together with regenerated assets. Maven/Docker builds
consume those checked-in assets and require no Node runtime. CI verifies the asset
snapshot, audits npm dependencies, and runs headless Chromium against the disposable
MySQL-backed application. The browser tests cover public forms without external
requests, real registration/login, safe inventory text and text-only notifications.
Run `npx playwright install chromium --only-shell` inside `frontend`, then
`EKOTE_BROWSER_BASE_URL=http://127.0.0.1:18082 npm test --prefix frontend` against a
fresh local test app. Tests register a disposable account and never accept external
production URLs.
