# Migration Drafts

This directory is for **draft migration SQL files only**.

## Purpose

This directory is used to store migration drafts for review before they are promoted to official runtime migrations.

Draft files in this directory are:

- for review only
- not executed by Spring Boot
- not loaded by `schema.sql`
- not part of the current H2 file initialization
- not part of the current H2 mem test initialization
- not Flyway migrations
- not Liquibase changelogs
- not production migration scripts

## Important Boundary

Do not configure this directory as a Flyway location.

This directory must not be used as:

```properties
spring.flyway.locations=classpath:docs/db/migration-drafts
```
