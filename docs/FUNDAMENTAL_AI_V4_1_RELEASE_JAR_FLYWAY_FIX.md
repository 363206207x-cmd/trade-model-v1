# Fundamental AI v4.1 Standard Release JAR Flyway Fix

Status: `IMPLEMENTED_PENDING_INDEPENDENT_AUDIT`

## Ownership

The existing Flyway V1-V13 directory at `src/main/resources/db/migration` is
the only migration owner. `flyway-core` and
`flyway-database-postgresql` are ordinary release dependencies. The removed
`flyway-migration` profile is no longer required by packaging or controlled
migration scripts.

## Runtime Contract

- Default H2 development/test keeps `schema.sql` and disables Flyway.
- `prod` enables Flyway at `classpath:db/migration`.
- `prod` sets SQL initialization to `never` and disables the H2 console.
- Standard release command: `./mvnw clean package`.
- Standard startup command: `java -jar target/<application>.jar`.
- Migration checksum or SQL failure prevents a ready business runtime.

## Executable Evidence

`scripts/standard-release-postgresql-smoke.sh`:

1. builds the ordinary Spring Boot JAR without a Maven profile;
2. verifies Flyway core, PostgreSQL support and V1-V13 resources in the JAR;
3. starts PostgreSQL 16 with an empty database;
4. starts the packaged JAR and verifies 13 successful migration rows;
5. restarts the same JAR against the existing V13 database;
6. corrupts the V13 checksum and verifies startup/readiness fail closed;
7. destroys the disposable database and all temporary credential/log files.

No migration was rewritten and no second schema initialization mechanism was
introduced.
