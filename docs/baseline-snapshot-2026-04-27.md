# Baseline Snapshot (2026-04-27)

## Purpose

Freeze the current stable repository baseline so future development and fixes can align with the same reference point.

## Key Commits (Newest -> Oldest)

1. `5d765b2` - Add realistic event contract snapshot regression cases.
2. `d00b5cd` - Normalize event impact description format with stable templates.
3. `8e7d534` - Extract event impact threshold and penalty values into shared constants.
4. `5db97be` - Extract severe event trigger types into shared constants.
5. `e038393` - Refine event impact score with severity-aware penalties.
6. `82c5086` - Add regression coverage for macro and event score boundaries.
7. `debcc26` - Add baseline snapshot document for current stable branch.
8. `fdf90d2` - Fix Java runtime warning on macOS (mvnw optimization).
9. `cc0f600` - Stop tracking build artifacts (target, .m2-local, .m2).
10. `319964b` - Add baseline .gitignore for Maven and IDE artifacts.
11. `4a18517` - Add freeze index entry and mark status as Implemented + Verified + Observation.

## Score-8 Rule Snapshot (Event Impact)

- Base score: `50`.
- Base penalty on event hit: `-10` (`eventFactHit=true` or event evidence fallback hit).
- Extra penalty when `eventFactCount >= 3`: `-5`.
- Extra penalty when `eventTriggerType` in severe list: `-5`.
  - Severe trigger types are centralized in `EvidenceTypeConstants.EVENT_IMPACT_SEVERE_TRIGGER_TYPES`:
    - `CIRCUIT_BREAKER`
    - `EXCHANGE_OUTAGE`
    - `LIQUIDATION_CASCADE`
- Tunable score-8 parameters are centralized in `EvidenceTypeConstants`:
  - `EVENT_IMPACT_MULTI_HIT_THRESHOLD`
  - `EVENT_IMPACT_MULTI_HIT_EXTRA_PENALTY`
  - `EVENT_IMPACT_SEVERE_TRIGGER_EXTRA_PENALTY`
- Description output is normalized by stable templates in `ScoreServiceImpl`.
- Regression coverage status:
  - macro/event boundary regressions added
  - score-8 description snapshot tests added
  - realistic analysis input/evidence fallback contract snapshots added

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
