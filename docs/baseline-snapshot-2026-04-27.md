# Baseline Snapshot (2026-04-27)

## Purpose

Freeze the current stable repository baseline so future development and fixes can align with the same reference point.

## Key Commits (Newest -> Oldest)

1. `fdf90d2` - Fix Java runtime warning on macOS (mvnw optimization).
2. `cc0f600` - Stop tracking build artifacts (target, .m2-local, .m2).
3. `319964b` - Add baseline .gitignore for Maven and IDE artifacts.
4. `4a18517` - Add freeze index entry and mark status as Implemented + Verified + Observation.

## Baseline Constraints

- Keep frozen constraints unchanged:
  - no UI expansion
  - no formula expansion
  - no decision-path expansion
- Build artifacts and local caches must not be tracked by Git.
- Keep commit history atomic and focused (one intent per commit).

## Repository Hygiene Rules

- Ignore generated output and local caches via `.gitignore`.
- Do not re-track `target/`, `.m2-local/`, or `.m2/`.
- Run health checks before functional delivery:
  - `./mvnw -DskipTests compile`
  - `./mvnw test` (when preparing acceptance-ready changes)

## Current Baseline Status

- Working tree expected: clean before new feature work.
- Maven wrapper on macOS: warning noise handled in `mvnw`.
- This snapshot document is the baseline reference for subsequent tasks.
