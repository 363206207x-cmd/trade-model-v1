# AI External Provider Release Policy Evidence

Package: PDR-LIVE17 AI External Provider Release Policy Evidence
Branch: `codex/pdr-live17-ai-external-provider-release-policy`
Current main commit: `32ce23b9`
Status date: 2026-07-09

Production deployment readiness: BLOCKED
Deployment decision: DO NOT DEPLOY

## Scope

This package records AI and external provider release-policy evidence only. It is not production deployment. It does not access a production server, production database, real secret store, or provider secret. It does not print or commit secrets, does not call real AI providers, and does not add trading runtime behavior.

## Sources Reviewed

- `src/main/resources/application.yml`
- `src/main/resources/application-prod.yml`
- `src/main/java/org/example/trademodel/config/ProductionProfileSafetyGuard.java`
- `src/test/java/org/example/trademodel/provider/ProviderReadinessServiceImplTest.java`
- `scripts/prod-provider-smoke.sh`
- `docs/CONTROLLED_PROVIDER_LIVE_SMOKE_EVIDENCE_RUN.md`
- `docs/CONTROLLED_AI_PROVIDER_SMOKE_EVIDENCE_RUN.md`
- `docs/RELEASE_EVIDENCE_BUNDLE_CURRENT_STATUS.md`
- `docs/FINAL_CONDITIONAL_READINESS_REVIEW.md`

## Redacted Environment Presence

Only boolean presence was checked. No environment values were printed.

| Variable | Presence |
|---|---:|
| `OPENAI_API_KEY` | MISSING |
| `GEMINI_API_KEY` | MISSING |
| `XAI_API_KEY` | MISSING |
| `NEWS_API_KEY` | MISSING |
| `MACRO_CALENDAR_API_KEY` | MISSING |
| `ETF_FLOW_API_KEY` | MISSING |
| `PROVIDER_SMOKE_ENABLE_EXTERNAL_CALLS` | MISSING |
| `PROVIDER_SMOKE_OPENAI_ENABLED` | MISSING |
| `PROVIDER_SMOKE_GEMINI_ENABLED` | MISSING |
| `PROVIDER_SMOKE_XAI_ENABLED` | MISSING |

## Current Provider Smoke Status

| Provider / dependency | Current smoke status | Evidence | Release-policy status |
|---|---:|---|---:|
| Binance public market data | PASS | `docs/CONTROLLED_PROVIDER_LIVE_SMOKE_EVIDENCE_RUN.md` | REQUIRED_PASS satisfied for public market-data reachability only |
| OpenAI | SKIPPED_MISSING_SECRET | `docs/CONTROLLED_AI_PROVIDER_SMOKE_EVIDENCE_RUN.md` | RELEASE_OWNER_DECISION_REQUIRED |
| Gemini | SKIPPED_MISSING_SECRET | `docs/CONTROLLED_AI_PROVIDER_SMOKE_EVIDENCE_RUN.md` | RELEASE_OWNER_DECISION_REQUIRED |
| xAI / Grok | SKIPPED_MISSING_SECRET | `docs/CONTROLLED_AI_PROVIDER_SMOKE_EVIDENCE_RUN.md` | RELEASE_OWNER_DECISION_REQUIRED |
| External context / news | SKIPPED_MISSING_SECRET | `docs/CONTROLLED_PROVIDER_LIVE_SMOKE_EVIDENCE_RUN.md` | RELEASE_OWNER_DECISION_REQUIRED |
| Macro calendar | SKIPPED_MISSING_SECRET | `docs/CONTROLLED_PROVIDER_LIVE_SMOKE_EVIDENCE_RUN.md` | RELEASE_OWNER_DECISION_REQUIRED |
| ETF flow | SKIPPED_MISSING_SECRET | `docs/CONTROLLED_PROVIDER_LIVE_SMOKE_EVIDENCE_RUN.md` | RELEASE_OWNER_DECISION_REQUIRED |

`SKIPPED_MISSING_SECRET` is not PASS. It means no provider endpoint was called and no release readiness evidence exists for that provider.

## Configuration And Guard Behavior

1. AI providers are disabled by default in `application.yml`.
2. OpenAI, Gemini, and xAI each require an explicit `trade-model.ai.<provider>.enabled=true` configuration before use.
3. `ProductionProfileSafetyGuard` rejects an explicitly enabled production AI provider when key, model, or base URL is missing.
4. Provider readiness tests show config-only providers are not marked `CONNECTED`, and explicitly enabled AI providers without keys fail closed.
5. `scripts/prod-provider-smoke.sh` defaults to no live external calls and only calls provider endpoints when the global live-call flag plus provider-specific flag are enabled.
6. The current provider smoke harness does not implement a live external-context/news/macro/ETF call; those providers remain missing evidence unless a later explicit harness or release-owner waiver is recorded.

## Fallback / Degradation Behavior

The current repository supports a safe no-provider posture: missing or disabled AI providers do not create order execution, auto-open, auto-close, auto-reverse, external Push send, fake positions, or fake review records. AI-provider absence is treated as unavailable/skipped/fail-closed evidence rather than as connected proof.

Pure-rule fallback may be used only if the release owner explicitly decides the target controlled release candidate can run without the missing AI/external provider. Until that decision is recorded, missing AI/external provider proof remains a release blocker.

## Release Policy Decision Required

The release owner must classify each missing provider before a controlled release candidate can move beyond BLOCKED:

| Provider / dependency | Allowed decisions | Current required action |
|---|---|---|
| OpenAI | REQUIRED_PASS / OPTIONAL_WITH_WAIVER / DISABLED_FOR_RELEASE | Decide policy; if REQUIRED_PASS, provide redacted PASS smoke. |
| Gemini | REQUIRED_PASS / OPTIONAL_WITH_WAIVER / DISABLED_FOR_RELEASE | Decide policy; if REQUIRED_PASS, provide redacted PASS smoke. |
| xAI / Grok | REQUIRED_PASS / OPTIONAL_WITH_WAIVER / DISABLED_FOR_RELEASE | Decide policy; if REQUIRED_PASS, provide redacted PASS smoke. |
| External context / news | REQUIRED_PASS / OPTIONAL_WITH_WAIVER / DISABLED_FOR_RELEASE / NOT_APPLICABLE | Decide policy and evidence/waiver. |
| Macro calendar | REQUIRED_PASS / OPTIONAL_WITH_WAIVER / DISABLED_FOR_RELEASE / NOT_APPLICABLE | Decide policy and evidence/waiver. |
| ETF flow | REQUIRED_PASS / OPTIONAL_WITH_WAIVER / DISABLED_FOR_RELEASE / NOT_APPLICABLE | Decide policy and evidence/waiver. |

`OPTIONAL_WITH_WAIVER` is not PASS by itself. It becomes acceptable release evidence only when an explicit release-owner waiver names the provider, the target release, the reason it is optional, and the fallback behavior.

## Does Missing AI Provider Evidence Block The Controlled Release Candidate?

Yes by default.

Missing OpenAI, Gemini, xAI/Grok, and external context provider proof blocks a controlled release candidate until one of these happens per provider:

1. Controlled, redacted provider smoke records PASS.
2. Release owner classifies the provider as `OPTIONAL_WITH_WAIVER` for the target release and records the waiver.
3. Release owner classifies the provider as `DISABLED_FOR_RELEASE` or `NOT_APPLICABLE`, and the release bundle proves no feature in the target release depends on that provider.


## Post-LIVE18 Decision Register Link

PDR-LIVE18 adds `docs/RELEASE_OWNER_DECISION_REGISTER.md` as the central register for provider waivers and other release-owner decisions. LIVE18 approves no waiver. OpenAI, Gemini, xAI/Grok, and external context/news/macro/ETF remain `RELEASE_OWNER_DECISION_REQUIRED` unless a later package records controlled PASS evidence or explicit release-owner waiver/disablement.

## Safety Confirmation

- No production server was accessed.
- No production DB was accessed.
- No real secret store was accessed.
- No secret values were printed or committed.
- No `.env` file was committed.
- No AI provider endpoint was called in this package.
- No external context/news/macro provider endpoint was called in this package.
- No orders were placed.
- No auto-open, auto-close, or auto-reverse behavior was introduced.
- No order execution or auto-trading behavior was introduced.
- No external Push was sent.
- No fake positions or fake review records were created.

## Production Readiness Decision

Production readiness remains BLOCKED.

Production deployment cannot proceed. Binance public market-data smoke is PASS, but AI/external provider status remains missing-secret or release-owner-decision-required evidence. A missing provider key remains `SKIPPED_MISSING_SECRET` unless the release owner explicitly waives or disables that provider for the target release.

## Remaining Blockers

1. Release owner has not classified OpenAI, Gemini, xAI/Grok, external context/news, macro calendar, or ETF flow for the target release.
2. OpenAI, Gemini, and xAI/Grok live provider smoke remains `SKIPPED_MISSING_SECRET`.
3. External context/news/macro/ETF live provider proof remains missing or `SKIPPED_MISSING_SECRET`.
4. Real server smoke is still `SKIPPED_MISSING_CONTROLLED_SERVER`.
5. Auth smoke through the intended HTTPS proxy remains missing.
6. Real secret-store injection evidence remains missing.
7. Real credential rotation drill evidence remains missing.
8. Final release owner approval remains missing.

## Next Recommendation

Proceed to a release-owner provider policy decision package, a controlled real-server PASS smoke package if infrastructure becomes available, or a controlled secrets/rotation/proxy evidence package. The next package must not be production deployment and must preserve all no-trading/no-order/no-external-push guardrails.
