# Non-CoinGlass Blocker Closure Evidence

Start Head: `c80af6bf20c1135e174ef636f28abd5f8e7f97af`
Implementation Head: `8c5f6f11`
Final evidence Head: the commit containing this index, reported externally
PR #1195: open, Draft, unmerged

> Superseded configuration detail: the current production-safe defaults are
> both Kraken enablement flags `false`. Kraken is still the required release
> provider and must be enabled by explicit deployment injection. See
> `docs/evidence/non_coinglass_production_final_gate/README.md`.

## Evidence classification

| Evidence | Label | Result |
|---|---|---|
| Gemini controlled reproduction and differential probes | `LIVE_PROVIDER` | account/location/region restriction; no code change |
| OpenAI/xAI controlled calls | `LIVE_PROVIDER` | PASS |
| Three-AI H2 startup/orchestration repair | `AUTOMATED_TEST` / `LOCAL` | PASS |
| Kraken/Binance release policy tests | `AUTOMATED_TEST` | PASS |
| Full Maven | `LOCAL` | 4,791 / 0 failures / 0 errors / 14 controlled skips |
| Product Source / Workflow | `AUTOMATED_TEST` | PASS on clean implementation Head |
| Existing Home visual evidence | `UI_REVIEW_FIXTURE` | locked; no UI changed |
| Remote P3H gates | `NOT_VERIFIED` | 13 required inputs missing |
| CoinGlass live path | `DEFERRED_BY_OWNER_POLICY` | missing private key; not called |

## Changed implementation files

- `scripts/ai-parallel-orchestrator-controlled-smoke.sh`
- `src/main/java/org/example/trademodel/ai/AiParallelOrchestratorControlledSmoke.java`
- `src/main/java/org/example/trademodel/config/ProductionProfileSafetyGuard.java`
- `src/main/java/org/example/trademodel/config/TargetRuntimePreflight.java`
- `src/main/resources/application-prod.yml`
- `src/test/java/org/example/trademodel/ai/AiParallelOrchestratorControlledSmokeTest.java`
- `src/test/java/org/example/trademodel/config/ProductionProfileSafetyGuardTest.java`
- `src/test/java/org/example/trademodel/config/TargetRuntimePreflightTest.java`
- `src/test/java/org/example/trademodel/market/client/impl/RoutedPublicOhlcvProviderTest.java`
- `src/test/java/org/example/trademodel/service/impl/DashboardHomeServiceImplTest.java`

No schema, Flyway migration, Telegram implementation, Figma, Mobile, AI role
contract, Position state machine, or automatic-trading file changed.

## Sanitized provider evidence

Gemini: `/v1/interactions`, `models/gemini-3.5-flash`, request
`application/json`, response `application/json`, HTTP 400, authentication
accepted, request ID absent, location/region provider restriction. No secret,
prompt, request body, raw response, authorization header, or signed URL is
stored here.

Post-fix bounded orchestrator: OpenAI 2 calls and xAI 2 calls succeeded and
parsed; Gemini made one readiness call and failed closed before role execution.
Total external calls: 5. Final role order remained deterministic. This is
partial real-provider evidence, not complete three-role lineage.

## Test matrix

| Matrix | Result |
|---|---:|
| Gemini model/request/parse/fail-closed contracts | PASS |
| AI readiness and bounded smoke | PASS |
| AI review and v4.1 decision-chain orchestrators | PASS |
| Kraken prod defaults disabled / explicit two-flag opt-in required / Binance disabled | PASS |
| Kraken failure / Binance call count | PASS / 0 |
| Explicit Binance 451 | PASS fail closed |
| Persisted Kraken close time | PASS |
| Selected asset / global readiness separation | PASS |
| Frontend role/data-state matrix | PASS |
| Header/status timestamp matrix | PASS |

## Staging authorization

The preflight reported these names as not set, without reading or printing
values: `P3H_CONFIRM`, both attestation files, SSH host/port/user/identity and
fingerprint, staging hostname, TLS mode/CA bundle, secret backend, and secret
mount. `STAGING_AUTHORIZATION=NOT_VERIFIED_MISSING_CONFIGURATION`.

Consequently no remote deployment, user-path mutation, PostgreSQL upgrade,
least-privilege check, backup/restore, HTTPS smoke, rotation drill, scheduler
cycle, or server observability check was attempted.

## Existing locked visual evidence

No frontend file changed. The existing 1080/1440 and position/AI screenshots
remain mapping evidence only under:

- `docs/evidence/global_ui_alignment/`
- `docs/evidence/home_ui_review_acceptance/`
- `docs/evidence/b1_2_3_2/`

They are not restated as live staging evidence.

## Remaining blockers

1. Gemini provider account/location/region restriction.
2. Authorized remote P3H identity and all target operational evidence absent.
3. CoinGlass private key absent; live path not called.

Final result: `PRODUCTION_READINESS=BLOCKED_MULTIPLE`.
