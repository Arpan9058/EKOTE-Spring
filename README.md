# EKOTE

**Project type: backend/web application (Java + Spring Boot).** The Maven module is
`demo/pom.xml`; application tests are under `demo/src/test/java`. See
[CONTRIBUTING.md](CONTRIBUTING.md) and [CHANGELOG.md](CHANGELOG.md).

EKOTE is a web-based inventory, issue/return, maintenance, and record management system built with Spring Boot. The application provides a centralized interface for managing inventory records, tracking individual items, recording issue transactions, handling maintenance information, and generating QR codes.

## Core Features

### Inventory Management
- Manage inventory and stock information
- Store individual item information using unique identifiers
- Track available quantities
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
- Docker

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
MySQL container smoke check; they do not prove every MySQL-specific query or concurrent
inventory operation is correct.

GitHub Actions gates tests on lint, then packages the application after tests pass,
and uploads Surefire reports and the executable jar. `./mvnw validate` checks
Spotless formatting and Checkstyle; `./mvnw spotless:apply` formats Java locally.
OSV performs a full Maven dependency scan on pushes and PRs, while dependency review
rejects new high/critical advisories on PRs. Scanner failures remain failures; no
vulnerability-free claim follows from a passing application test suite. Dependabot checks Maven dependencies, Actions, and Docker images
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

The application currently uses Hibernate `ddl-auto=update`. Production deployments
should use reviewed database migrations and private deployment credentials.

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
workflow. Concurrent stock updates still require stronger locking/consistency work
before use in a production inventory system.

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
