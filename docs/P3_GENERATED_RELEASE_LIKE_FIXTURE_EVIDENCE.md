# P3 Generated Release-Like Fixture Evidence

## Decision

- Package: `Generated Release-Like Dataset and P3 End-to-End Rehearsal P3.1`
- Base merged-main commit: `c94c99dfa72843e558ac4ce87037bfe71bd5dfaf`
- Evidence branch starting head: `58fb6eb70e8ce3dab65e8e4ccbf852f72c9d02dc`
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
| Source fingerprint | `515192907bc261379d2b20e8c2389fc9d17f155f670965a8a0f4fa2dfea7a051` |
| Deterministic rerun | `PASS`, same source fingerprint on repeated generation |
| Generated dump SHA-256 | `164ce23db6f7deb2ef400d8bea05f4e93ce5b85501a2e2e9e729ec7a9a1ce5a1` |
| Generated attestation SHA-256 | `225b315047d8830ba2ddc241080f71df49da8cbdeee9d1655bf98283c445dfac` |
| Fixture generator SHA-256 | `821f1968f37dbce0f8427c1651ffc4c75d9d55ca13b5f925a79e32b42191652b` |
| Secret candidates | `0` |
| PII candidates | `0` |
| Production-reference candidates | `0` |
| Container cleanup | `PASS` |

The dump and attestation are ignored runtime artifacts. Their hashes can vary
between exports because the archive and attestation carry generation-time
metadata; deterministic database content is proven by the repeated aggregate
source fingerprint.

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
| Executed at UTC | `2026-07-15T11:22:24Z` |
| Source restore | `PASS` |
| Source read-only inventory | `PASS_READ_ONLY_GENERATED_RELEASE_LIKE` |
| Controlled backup | `PASS` |
| Backup tool | PostgreSQL `16.14_CONTAINER_NATIVE` |
| Backup SHA-256 | `eee90d3f00d50b949709d9d26695b0780bd7a4878d78d0fc8b4e680b70475958` |
| Recovery restore | `PASS` |
| Source/recovery fingerprint | `MATCH` |
| Migration path | `V6_TO_V7` |
| Migration result | `PASS` |
| Historical V7 validity rewrite | `0` rows; legacy rows remain null/fail-closed |
| Post-migration fingerprint | `9dc3ccd45cdd947351bdd0d7f6c3a1ffe1e3091a60367a50ad3eb715b60964d9` |
| Historical-time inventory | `PASS_READ_ONLY_AGGREGATE` |
| Application smoke | `PASS` |
| Unexpected business writes | `0` |
| Container cleanup | `PASS` |

The recovery database retained the exact pre-migration V6 aggregate state.
The rehearsal database alone migrated to V7.

## Application Scenarios

The app ran only against the disposable rehearsal database with schedulers,
AI, market/provider calls, Push, and all external calls disabled.

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

## Safety And Remaining Gates

- No production database or server was accessed.
- No real secret, `.env`, user record, account record, or provider record was
  read or committed.
- No live AI or market provider was called.
- No order, automatic position action, external Push, Telegram, webhook, or
  email was executed.
- No destructive action occurred outside the disposable local databases.
- Writer cutover remains `MISSING_OPERATIONAL_EVIDENCE`.
- The sanitized current-state clone acquisition and final P3 evidence remain
  unperformed.

The next package is **Sanctioned Sanitized Release-Like Clone Acquisition and
P3 Final Evidence P3.2**. Production deployment cannot proceed, and P4 must
not start.
