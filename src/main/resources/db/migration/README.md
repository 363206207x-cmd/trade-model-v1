# Flyway Migration Placeholder

This directory is reserved for future Flyway migrations.

Do not add executable `V*.sql` migration files until PDR-2C creates a PostgreSQL-compatible baseline migration.

Current `src/main/resources/schema.sql` remains the local/test bootstrap. The default local and test classpath must continue to use the existing H2 + `schema.sql` behavior.

Production target database: PostgreSQL.

Flyway is selected as the SQL-first migration framework, but this PDR-2B skeleton does not create a real baseline migration and does not connect a production database.

Production Deployment Readiness remains `BLOCKED`.
