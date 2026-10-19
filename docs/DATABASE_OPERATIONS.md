# Database lifecycle, sessions and recovery

The application now uses Flyway migrations and `hibernate.ddl-auto=validate`.
Hibernate no longer performs uncontrolled production schema changes. Flyway clean
and automatic baseline are disabled. Multiple app processes coordinate migrations
through Flyway's database lock and share JDBC-backed authentication sessions.

## Fresh installations

Use a new empty MySQL 8.4 database. Set the three `SPRING_DATASOURCE_*` environment
variables as described in README, then start the jar or Compose. Flyway applies:

1. V1: the reviewed inventory schema, including stock audit snapshots.
2. V2: JDBC session tables plus active-issue, maintenance and audit query indexes.
3. V3: a unique email constraint preventing concurrent duplicate registrations.
4. V4: shared authentication-attempt quotas and an expiry index.

History reports read the canonical `guns_info`, `guns`, `issue` and `register`
tables through typed DTO projections. The obsolete `GunDetails` ORM entity and
its unused CRUD operations have been removed; the legacy `guns_details` table
is retained in existing databases and V1 for compatibility and data preservation.
No automatic table drop or historical data conversion is performed. Report dates
now serialize consistently as ISO dates/date-times, with newest issues first.

Startup fails on migration/schema validation errors. Do not use `ddl-auto=update`,
`baseline-on-migrate=true` or an advisory suppression to hide failures. H2 unit/HTTP
tests intentionally use an isolated generated schema; the separate MySQL suite
verifies fresh migration, repeat startup, explicit legacy adoption and shared sessions.

## Existing databases: deliberate adoption

Do not point this release at an unknown nonempty database and enable automatic
baseline. Preserve the old application/image and stop writes during the upgrade.
Take a private backup, restore it into a disposable database, and verify its data.
Inspect schema differences against V1. V1 describes the previous release's six
inventory tables, including `stock_movements`. Older releases without that table,
with duplicate models, or with different column types need a reviewed schema repair
on the clone before adoption. Baseline is a declaration, not a schema repair.

Check duplicate non-null email addresses and `(gun_name, model)` identities on the
clone. Resolve duplicates with the data owner; never delete accounts or inventory
just to get migrations to pass. Confirm required foreign keys and the unique model
constraint exist. Verify the repaired clone matches V1 and keep a record of repairs.

For a **verified V1-compatible clone**, set its datasource variables securely, then:

```sh
cd demo
./mvnw flyway:baseline
./mvnw flyway:migrate
./mvnw flyway:validate
```

The CLI baseline version is explicitly 1; it skips V1 and applies V2–V4. Test the
new jar with schema validation, exercise registration/inventory workflows, then
perform the same reviewed procedure in the stopped target database. Secrets belong
in the environment, never Maven command-line arguments or committed settings.

This procedure is verified against a V1 fixture retaining its inventory rows. It
does not certify every historical or production schema. Never edit applied migration
files. Add a new migration for each later change. DDL in MySQL can commit implicitly;
a failed migration needs diagnosis and a reviewed repair, not automatic rollback.
Rollback after a destructive data change means restoring a verified backup into a
fresh database and switching the stopped application to it. Do not run an old image
against an incompatible newer schema.

## Private backups and verified restore

From the repository root, targeting your owned MySQL Docker container:

```sh
python3 scripts/database-backup.py backup --container YOUR_DB_CONTAINER --file /PRIVATE/PATH/backup.sql.gz
python3 scripts/database-backup.py restore --container EMPTY_RESTORE_CONTAINER --file /PRIVATE/PATH/backup.sql.gz --allow-restore
```

The container supplies its own database/user/password internally; credentials are
not passed on the host command line. Backups use a consistent InnoDB transaction,
stream gzip, include routines/events/triggers, and produce SHA-256 metadata. Files
are created exclusively with mode 0600. Restore checks checksum and gzip CRC before
import, requires explicit opt-in, and refuses a nonempty target. Concurrent DDL or
nontransactional tables invalidate the consistency assumptions: quiesce schema
changes while exporting. A failed import may leave a partial target; discard that
disposable target and investigate before retrying into a fresh one.

For a repeatable exercise on a quiescent disposable source:

```sh
python3 scripts/verify-backup-restore.py --source-container YOUR_TEST_DB_CONTAINER
```

This creates its own temporary MySQL instance, compares all restored rows, verifies
private permissions, and proves corrupt archives and destructive second imports
are rejected. It removes its own temporary container, credentials and backup files.
The integration smoke leaves test accounts in the disposable source database.

A checksum is not an authenticated signature. Protect backup and metadata together
against modification. Local private files do not provide encrypted offsite storage,
retention, monitoring, or disaster recovery. For production, configure encrypted
backup storage with restricted access, scheduled exports, retention and independent
restore drills on your chosen provider. Credentials/provider destinations are not
available in this repository, so no external backup service is provisioned here.

## Sessions and application availability

Spring Session JDBC stores sessions in MySQL with a 30-minute timeout, HttpOnly
cookies and SameSite=Lax. Login on one instance and logout on another were verified
using two separate JVMs. Database access is part of the authentication trust boundary;
restrict it and use TLS for remote production MySQL. Serve the application behind
HTTPS. Set `SPRING_PROFILES_ACTIVE=production` and a canonical HTTPS
`EKOTE_PUBLIC_BASE_URL`; that profile enforces Secure session cookies. Real servlet
and Alpine container checks verify the cookie flags. The TLS edge itself still
needs deployment-specific verification.

The Kubernetes app now has two replicas, RollingUpdate with zero unavailable pods,
and a PodDisruptionBudget retaining one replica during voluntary disruption. This
provides process redundancy; the local cluster has one node and one MySQL instance.
It is not complete infrastructure HA. Production needs multiple failure domains,
a supported Kubernetes release, a managed/replicated database, TLS ingress, private
secrets, backups and monitoring. Serialized sessions should be compatibility-tested
before rolling major framework upgrades; forced logout may be required.

## Authentication-limit migration

V4 adds `authentication_rate_limits`, shared across replicas. It uses atomic JDBC
updates rather than per-process counters, and never locks a named user's account.
The migration adds a table and expiry index without rewriting inventory or existing
sessions. As with other upgrades, back up the database first and grant the migration
user the required DDL permissions. Old instances ignore the new table during a
rolling upgrade; limits apply to requests served by upgraded instances.
