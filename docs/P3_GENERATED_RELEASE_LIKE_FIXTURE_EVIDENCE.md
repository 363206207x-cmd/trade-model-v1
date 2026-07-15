# P3 Generated Release-Like Fixture Evidence

## Decision

- Package: `P3.1 Evidence Integrity Hardening`
- Base merged-main commit: `c94c99dfa72843e558ac4ce87037bfe71bd5dfaf`
- Original P3.1 evidence head: `58fb6eb70e8ce3dab65e8e4ccbf852f72c9d02dc`
- Evidence-hardening starting head: `49fbf42cdab115acb25d3599104e1fefde4025bf`
- Fixture seed: `20260715`
- Generated fixture: `PASS`
- Generated P3 rehearsal: `PASS_GENERATED_RELEASE_LIKE_REHEARSAL`
- Final sanitized-clone gate: `BLOCKED_NOT_RUN`
- P4 allowed: `NO`
- Production readiness: `BLOCKED`

This is deterministic, repository-generated, disposable evidence. It is not a
sanitized current-state clone, does not contain real user/account/provider
data, and cannot close the final P3 sanitized-clone gate.

## Commands Run

The fixture was generated with:

```bash
bash scripts/generate-p3-release-like-fixture.sh
```

The bounded rehearsal used the ignored generated dump and attestation with:

```bash
P3_DATASET_CLASS=GENERATED_RELEASE_LIKE \
P3_CONFIRM=I_CONFIRM_GENERATED_NON_PRODUCTION_RELEASE_LIKE_DATASET \
P3_LOCAL_DB_RECREATE_CONFIRM=I_UNDERSTAND_ONLY_LOCAL_P3_DATABASES_ARE_DROPPED \
P3_SANITIZED_DUMP_FILE=<ignored-generated-dump> \
P3_SANITIZATION_ATTESTATION_FILE=<ignored-generated-attestation> \
P3_DATASET_ID=<non-sensitive-generated-id> \
bash scripts/controlled-current-state-clone-rehearsal-p3.sh
```

No credential, connection secret, raw record, dump content, or attestation
content is stored in this document.

## Fixture Evidence

| Evidence | Result |
|---|---|
| Dataset class | `GENERATED_RELEASE_LIKE` |
| Source dataset status | `GENERATED_RELEASE_LIKE_NOT_SANITIZED_CLONE` |
| PostgreSQL image | fixed PostgreSQL 16.14 digest |
| Bind target | disposable localhost `127.0.0.1:55434` |
| Source Flyway version | `6` |
| Seed | `20260715` |
| Source structure fingerprint | `dc2f3d64b0a3b5cfc23e980528a77741e538ae8582150438a58536e23f277cda` |
| Source content fingerprint | `3b77c183a8cd99693f0a582eae8dd608a2bef89d47877ecfa89b0467e9f4b179` |
| Same-row-count mutation detection | `PASS` for status, timestamp, and plan-boundary changes; rollback restores the original fingerprint |
| Session-timezone stability | `PASS` for UTC, Asia/Shanghai, and America/New_York |
| Generated dump SHA-256 | `450ab83c73a7016f425a24a6ced09e50a3bc5e989ef3077ab5782a17bf01cc3f` |
| Generated attestation SHA-256 | `8a0ea380c7b288803e5e4aab367c80df096a28333780a81c2a2b1038c0d42443` |
| Fixture generator SHA-256 | `c15ca7891ea303c137fb720dd4e8e416349999308a2edc43fd1b4c4764677710` |
| Attestation uniqueness / version cross-check | `PASS / PASS` |
| Secret candidates | `0` |
| PII candidates | `0` |
| Production-reference candidates | `0` |
| Container cleanup | `PASS` |

The dump and attestation are ignored runtime artifacts. Their hashes can vary
between exports because the archive and attestation carry generation-time
metadata; deterministic database content is proven by repeated structure and
content captures, not by archive-byte equality.

The structure fingerprint records schema shape, row counts, indexes,
constraints, sequences, and Flyway history. The separate content fingerprint
records, for every `tm_*` table, only its name, row count, and sum/XOR
aggregates from `hashtextextended(to_jsonb(row)::text, seed)` under two fixed
seeds. It never emits a business row or raw field value and does not aggregate
whole-table text with `STRING_AGG`.

## Generated Coverage

| Data family | Deterministic coverage |
|---|---|
| Assets | BTCUSDT, ETHUSDT, SOLUSDT, BNBUSDT, XRPUSDT, DOGEUSDT |
| Analysis runs | `138`: 120 success, 12 failed, 6 started |
| Decisions | `120` |
| Execution plans | `121`, including an intentional sibling-plan case |
| Asset states | `9` rows covering all eight required database state values; one compatible state is duplicated for app smoke |
| Manual positions | `7`, including two open BTC positions and typed/untyped sources |
| Position monitor logs | `8`, including exact A, sibling-B isolation, unverified source, revalidation, and incomplete-boundary reason evidence |
| Monitor alerts | `6` |
| Push snapshots / rechecks | `5 / 5` |
| Hot Reset events | `6` |
| AI call logs | `5`, sanitized non-call/abstain evidence only |
| OHLCV | `1200`: 6 assets x 4 timeframes x 50 synthetic bars |

All plan, position, and AI safety flags remain manual-review-only,
non-executable, not auto-trading, and not order execution.

## Rehearsal Evidence

| Gate | Result |
|---|---|
| Executed at UTC | `2026-07-15T12:04:27Z` |
| Source restore | `PASS` |
| Source read-only inventory | `PASS_READ_ONLY_GENERATED_RELEASE_LIKE` |
| Controlled backup | `PASS` |
| Backup tool | PostgreSQL `16.14_CONTAINER_NATIVE` |
| Backup SHA-256 | `9074dc0523e1f3c5026b406d86383cfa71053d226899db087b115843ebb02ff3` |
| Recovery restore | `PASS` |
| Source/recovery structure fingerprint | `MATCH` |
| Source/recovery full content fingerprint | `MATCH` |
| Migration path | `V6_TO_V7` |
| Migration result | `PASS` |
| Historical V7 validity rewrite | `0` rows; legacy rows remain null/fail-closed |
| Pre/post migration stable content | `MATCH`; only V7 `valid_from` / `expires_at` are excluded from this comparison |
| Post-migration structure fingerprint | `aac46a72990a9c5dccc90b8afaffbd9b2a4080788320b00b939533397a347323` |
| Post-migration full content fingerprint | `e037df07fb60366a724d5f04072fc8508e4fb6dd23dc1f016e55006d3bce94cf` |
| Migration-stable content fingerprint | `7dd43e56b56785aa69036ea064453ef7d438c168680119b98b89ee6cb506404d` |
| Historical-time inventory | `PASS_READ_ONLY_AGGREGATE` |
| Application smoke | `PASS` |
| Application database role | randomized, connect-only-to-rehearsal, read-only, non-superuser/non-createdb/non-createrole |
| Read-only write probe | `DENIED` with accepted read-only/permission SQLSTATE classification |
| Flyway during app smoke | `DISABLED`; SQL init `never`; Hikari read-only `true` |
| Application content fingerprint | `MATCH` before/after smoke |
| Unexpected business writes | `0` |
| Container cleanup | `PASS` |

The recovery database retained the exact pre-migration V6 aggregate state.
The rehearsal database alone migrated to V7.

## Application Scenarios

The app ran only against the disposable rehearsal database with a dedicated
random read-only role. The role cannot connect to the other disposable
databases, create schema objects, create databases/roles, use superuser
capabilities, or write business rows. A no-op `UPDATE` probe was required to
fail before startup. Flyway was disabled, SQL initialization was `never`, and
Hikari was explicitly read-only. Schedulers, AI, market/provider calls, Push,
and all external calls were disabled.

- Health: `HTTP_200_UP`
- Dashboard Home: `HTTP_200_FAIL_CLOSED`
- Run Baseline: `HTTP_200`
- no-open-position scenario: `PASS`
- unique-position exact selection: `PASS`
- same-symbol multiple positions without position ID: selection required
- BTC position A resolves only source plan/analysis A
- BTC position B resolves only source plan/analysis B
- incomplete historical plan remains fail-closed
- expired historical plan remains position-monitoring history, not executable
- revalidation-required historical plan remains fail-closed
- application full-content fingerprint before/after smoke: `MATCH`
- unexpected business writes: `0`

The A/B check reads the exact structured
`executionSuggestion.sourceExecutionPlanId` and `sourceAnalysisId` fields. It
does not grep the whole response, where other read-only decision lists may
legitimately mention sibling plans.

## Harness Findings Closed

The real run exposed and closed these evidence-harness defects:

1. Bounded background psql calls now bind SQL input explicitly; an empty psql
   invocation can no longer be mistaken for a successful query.
2. The fingerprint query explicitly casts PostgreSQL internal constraint type
   codes to text.
3. Backup and restore use PostgreSQL 16 container-native tools, preventing a
   PostgreSQL 18 client from sending unsupported `transaction_timeout` setup
   to the PostgreSQL 16 server.
4. Flyway source validation targets the observed source version, so expected
   pending V7 is distinct from checksum failure; migration still targets V7.
5. Historical-time inventory reads V7 validity fields structurally and can
   audit V6 without pretending those columns already exist.
6. Dashboard smoke uses the real `selectedSymbol` request contract and exact
   structured plan-source fields.
7. Expired, incomplete, and revalidation-required plan checks use separate
   generated positions so their deterministic validity priorities cannot mask
   one another.
8. Preflight failures no longer overwrite an earlier successful ignored
   evidence bundle; failure summaries are written only after the current run
   has prepared its own evidence directory.
9. Production-indicator checks treat `live` as a delimited token, so the
   approved `release-like` dataset ID is not misclassified while explicit
   `live` IDs still fail closed.
10. Content integrity no longer relies on row counts: two-seed sum/XOR hashes
    detect same-row-count changes to status, timestamps, and plan boundaries.
11. Attestation validation now rejects duplicate/conflicting/unknown keys,
    invalid or future generation timestamps, class impersonation, and
    PostgreSQL/Flyway version mismatches without copying raw attestation data.
12. Dump and attestation paths use `realpath` and reject symlinks in the file
    or any parent component.
13. Application smoke runs under a dedicated read-only database role and
    requires both an explicit write denial and unchanged content fingerprint.

## Safety And Remaining Gates

- No production database or server was accessed.
- No real secret, `.env`, user record, account record, or provider record was
  read or committed.
- No live AI or market provider was called.
- No order, automatic position action, external Push, Telegram, webhook, or
  email was executed.
- No destructive action occurred outside the disposable local databases.
- Backup/restore evidence used PostgreSQL 16 container-native tools.
  `scripts/prod-backup.sh` and `scripts/prod-restore.sh` were not executed, so
  the separate operational-script gate remains `BLOCKED`.
- Writer cutover remains `MISSING_OPERATIONAL_EVIDENCE`.
- The sanitized current-state clone acquisition and final P3 evidence remain
  unperformed.

The next package is **Sanctioned Sanitized Release-Like Clone Acquisition and
P3 Final Evidence P3.2**. Production deployment cannot proceed, and P4 must
not start.
