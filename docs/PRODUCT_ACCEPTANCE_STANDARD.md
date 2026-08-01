# Trade Model V1 Product Acceptance Standard

Status: `P0_PRODUCT_BASELINE_FREEZE_CANDIDATE`

This is the mandatory completion standard for every product module. A module is complete only when all applicable gates pass together. Authority: `docs/PRODUCT_SOURCE_OF_TRUTH.md`.

## PRODUCT_FIRST_STOP_RULE

This permanent rule is a simple human review rule. It must not become a new governance product or automated semantic engine.

A review finding may block the current product stage only when it is classified as exactly one of:

- `PRODUCT_SEMANTIC_BLOCKER`: a reproducible conflict with formal product semantics or interaction, including AI authority, ExecutionPlan/UserPosition separation, state separation, Home interaction, or Position Monitoring.
- `SECURITY_OR_PRIVACY_BLOCKER`: privacy leakage, owner-scope bypass, unauthorized mutation, automatic open/close/reverse/trade, or Push Recheck used as trading authorization.
- `REAL_DATA_INTEGRITY_BLOCKER`: mock/default/fallback data presented as real, failure presented as success, or fabricated product/AI fields.
- `NEXT_PRODUCT_STAGE_BLOCKER`: reproducible evidence that the current stage cannot merge or the next formal Product Roadmap stage cannot start after merge, creating a real delivery deadlock.
- `BUILD_OR_RUNTIME_BLOCKER`: compile failure, required-test failure, application startup failure, or failure of a core runtime chain.

Every other finding is `NON_BLOCKING_TECHNICAL_DEBT` and must set `BLOCKS_CURRENT_STAGE: NO`. Examples include non-critical wording or metadata, formatting/naming preference, theoretical future cases, non-critical Workflow improvement, parser/inventory/digest/helper refinement, non-security test idealization, maintainability advice, or refactoring outside the current product package.

Every review finding must report:

```text
FINDING_ID:
BLOCKER_CLASS:
DIRECT_PRODUCT_IMPACT:
REPRODUCTION_EVIDENCE:
BLOCKS_CURRENT_STAGE: YES / NO
```

A finding with `BLOCKS_CURRENT_STAGE: YES` must also identify the affected formal product source and explain why it cannot be deferred. Without concrete product impact, a reproducible path, the affected formal product source, and a non-deferrable reason, it must set `BLOCKS_CURRENT_STAGE: NO`. P1/P2/P3 priority and blocking status are independent.

Workflow, Governance, Metadata, and Review tooling together may consume at most an estimated 10% of a product stage. At 10%, stop expanding them, register remaining items as `NON_BLOCKING_TECHNICAL_DEBT`, and resume product work. Exceptions require a demonstrated product-semantic, security/privacy, build/runtime, or actual next-stage blocker. Use a reasonable human estimate; do not build a statistics system. Task reports include:

```text
PRODUCT_WORK_RATIO:
NON_PRODUCT_WORK_RATIO:
STOP_RULE_TRIGGERED: YES / NO
```

Implementation is limited to plain documentation, fixed review fields, minimal shell assertions, and explicit human classification. Do not build a natural-language classifier, synonym list, semantic parser, inventory, digest, whole-review analyzer, independent Stop Rule phase, or large meta-test suite.

Fixed examples:

- naming preference -> `NON_BLOCKING_TECHNICAL_DEBT` -> `BLOCKS_CURRENT_STAGE: NO`
- reproducible cross-user data leak -> `SECURITY_OR_PRIVACY_BLOCKER` -> `BLOCKS_CURRENT_STAGE: YES`
- reproducible post-merge P1A deadlock -> `NEXT_PRODUCT_STAGE_BLOCKER` -> `BLOCKS_CURRENT_STAGE: YES`

## 1. Ten Mandatory Gates

### 1. Product Alignment

- Scope maps to registered formal product sources.
- Required product meanings and exclusions are preserved.
- Current code, UI, Workflow, Governance, or tests do not redefine the product.
- Any source conflict is resolved by priority or explicit human decision.

### 2. Design Alignment

- Final interaction document and applicable Figma nodes match the implemented information hierarchy, labels, states, and navigation.
- Responsive desktop/mobile behavior is verified.
- No Figma example becomes fake product data or unsupported functionality.

### 3. Semantic Alignment

- Identities, states, transitions, and domain ownership are correct.
- AssetState, ExecutionPlan, UserPosition, PositionMonitor, Push Recheck, Message, Confused, and data quality remain distinct.
- Unknown, malformed, contradictory, stale, or missing inputs fail closed.

### 4. Data Source Alignment

- Every visible field maps to a real source, service/API, timestamp, freshness rule, null behavior, and public/private classification.
- Exact identities are preserved; no symbol/latest fallback replaces an exact ID.
- No mock, fixture, placeholder, browser time, or fallback value is presented as real.

### 5. Interaction Alignment

- Every click/tap, selection, linked refresh, detail entry, back action, retry, manual action, and disabled action matches the product contract.
- Related regions update from one authoritative context.
- Read-only pages do not invoke mutations.

### 6. Error Handling

- Loading, Empty, Error, Partial, and Missing are independent.
- Error is not rendered as Empty.
- Stale cached success cannot overwrite a current error.
- Partial exposes only verified fields and a reason.
- Unsupported capability is hidden/disabled, never simulated.

### 7. Real Scenario Validation

- The module passes a representative real-data scenario or a deterministic, traceable historical replay accepted by the product plan.
- Success, boundary, failure, recovery, and privacy/ownership cases are included.
- Automated fixtures alone are insufficient.

### 8. Screenshot Evidence

- Screenshots show real rendered data and all applicable states at target desktop/mobile dimensions.
- Screenshots are paired with the payload identity/time/source used to render them.
- Real-device screenshots are required for iPhone acceptance.

### 9. Traceability

- A reviewer can follow data provider -> evidence -> score -> rule version -> decision -> plan -> user fact -> monitor/message/review where applicable.
- IDs and timestamps remain exact across network, browser, logs, and database.
- AI includes model, role, input evidence package, result availability, timestamp, and fallback/conflict trace.

### 10. No False Completion

The following cannot independently establish completion:

- docs-only;
- DTO-only;
- controller/endpoint-only;
- review-only;
- preview-only;
- dashboard-only;
- fallback-only;
- no-op or placeholder;
- mock-only;
- test-count-only;
- Governance or Workflow PASS;
- open PR or unmerged branch.

## 2. Required Acceptance Record

Each module acceptance must record:

| Record | Required content |
|---|---|
| Product sources | source IDs, paths, hashes, applicable chapters |
| Contract mapping | required semantics and forbidden reinterpretations |
| Design mapping | page/node/component, module order, clicks, linked refresh, details, states |
| Data mapping | field, domain, service/API/provider, cadence, cache, null/error, privacy |
| Exact identities | IDs used in the scenario and string/ownership proof |
| Scenario | setup, real/historical source, steps, expected and actual result |
| Failure cases | Loading/Empty/Error/Partial/Missing and stale/cache behavior |
| Security/privacy | authentication, ownership, public/private payload evidence |
| Screenshots | desktop/mobile and target device where applicable |
| Trace | source time, analysis/plan/position/message IDs, rule/model versions |
| Validation | tests/checks plus real scenario; tests are supporting evidence only |
| Deviations | unresolved differences and explicit disposition |

## 3. Home Acceptance

Home passes only when all of the following are demonstrated:

1. Primary mobile tabs are exactly 首页 / 持仓 / AI分析 / 消息 / 我的.
2. Final module order and Figma-aligned responsive layout are visible.
3. Clicking a focus asset card body changes selected asset context and does not default-navigate.
4. ExecutionPlan, GPT Final, Gemini Review, Grok Challenge, and AI consistency all update to the same exact selected analysis/plan context.
5. Existing UserPosition selection/identity does not change as a side effect.
6. The plan directly shows all available required summary fields and no unsupported field.
7. Deep scores/evidence/timeframes use an explicit exact Analysis Detail entry.
8. Top3 positions follow pinned/risk/time ordering and exact string-safe `positionId`.
9. Every visible field has real source/freshness evidence.
10. Loading, Empty, Error, Partial, Missing, stale cache, and retry behave independently.
11. Desktop/mobile screenshots and interaction evidence exist.
12. At least two real assets and one failure/partial case pass.

## 4. Position and Position Monitor Acceptance

The module passes only when:

1. The UserPosition was entered by an authenticated user with real actual facts.
2. ExecutionPlan remains a linked reference and does not overwrite actual entry facts.
3. Exact owner-scoped string `positionId` is preserved throughout.
4. OPEN, PARTIALLY_CLOSED, and CLOSED transitions occur only through explicit user records.
5. CLOSED is excluded from open monitoring.
6. List and detail use the same authoritative latest monitor and state resolver.
7. Real/historical price movement changes monitor conclusions where the plan specifies.
8. Original logic valid, weakened, and invalidated scenarios pass.
9. No, weak, and strong reversal scenarios pass; wick alone is not strong reversal.
10. Stop/target distance, size, leverage, account risk, liquidity, and correlation are correctly separated.
11. Risk alerts and adjustment suggestions are traceable, advisory, and timely.
12. The page never automatically opens, closes, reduces, adds, reverses, orders, or triggers monitor mutation on read.
13. Every monitor result is timestamped and retained in PositionMonitorLog.
14. Owner isolation and all five read states pass.
15. A complete manual close and Review entry is demonstrated.

## 5. AI Acceptance

AI Analysis passes only when:

1. Data quality and the rule layer produce a base result before AI.
2. AI trigger reason proves roles are checkpoint-triggered, not called by assumption every cycle.
3. GPT Final, Gemini Review, and Grok Challenge receive the same immutable, traceable evidence package.
4. Their fixed role boundaries are visible and tested; there is no fourth AI.
5. They are not a parallel voting system.
6. AI cannot bypass rule direction, state machine, risk block, or no-trading boundary.
7. Each `resultAvailable != true` role exposes only role name, status label, and status message.
8. Four conflict levels, Confused entry/exit, Hot Reset, and Recovery pass formal scenarios.
9. Confused is not used for empty data or low data quality and cannot exit directly to triggered.
10. AI failure falls back to the rule chain without fabricated role output.
11. Eight scores, four timeframes, evidence, sources, timestamps, model, rule version, and trace are auditable.
12. Real input/output quality is manually reviewed on representative scenarios.

## 6. Message and Push Detail Acceptance

The module passes only when:

1. Message sources are exactly OPPORTUNITY and POSITION_RISK.
2. OPPORTUNITY is an authenticated shared public projection with no UserPosition, account risk, position risk, private reason, private push identity, or private Recheck reference.
3. POSITION_RISK is exact current-user owner-scoped private data.
4. `messageId` and related IDs are string-safe with no precision loss.
5. Original snapshot, current state/Recheck, change reason, source, and timestamp are shown only where the source contract allows.
6. Public and private details are distinct server-side projections; frontend filtering is not a privacy control.
7. Raw `pushId` possession cannot bypass source/owner authorization.
8. READY/EMPTY/ERROR/MISSING/PARTIAL are distinct in list and detail.
9. No fake unread/message count is shown.
10. UI performs safe GET reads only; no Recheck POST, monitor run, notification send, mutation, or trade.
11. Telegram remains a future outlet and is not a Message Center type.
12. Public, owner, cross-user, partial, error, empty, missing, and changed-result scenarios pass with network payload evidence.

## 7. Review Acceptance

- A real or accepted historical case links source evidence, decision, plan, user action, actual result, monitor history, messages/Recheck, and rule version.
- Outcome classification is explainable and not inferred from missing data.
- User feedback is explicit and attributable.
- Rule iteration is human-reviewed; Replay never mutates live state.

## 8. My and Settings Acceptance

- Every field and action has a formal product contract and real source.
- Login/session/logout paths work on target browsers and iPhone.
- Unsupported preferences are hidden/disabled with honest state.
- Community, referral, paid plan, exchange ordering, or automatic trading are absent unless a future formal source explicitly authorizes them.

## 9. Server and Production Acceptance

- Production HTTPS, cookie/session/CSRF, secrets, database migrations, provider credentials, backups, restore, rollback, logs, metrics, alerts, SLOs, and runbooks pass.
- Sustained real multi-source data demonstrates freshness/degradation/failure handling.
- No secret/token leakage occurs; leakage is a hard stop.
- Production changes preserve all product safety and privacy boundaries.

## 10. iPhone Acceptance

- A signed/installable build or approved formal container runs on a real target iPhone.
- Five tabs, deep links, back navigation, Session/Cookie/CSRF, safe areas, 44pt targets, Dynamic Type, background/foreground, network loss, retry, and session expiry pass.
- Exact IDs and public/private projections survive bridge serialization.
- WKWebView or simulator tests support but do not replace real-device evidence.

## 11. Status Assignment

Use only states in `docs/PRODUCT_COMPLETION_MATRIX.md`. `REAL_SCENARIO_VALIDATED` requires gates 1-10 plus a real/historical scenario. `DEPLOYMENT_READY` additionally requires production and target-device readiness. `EFFECTIVE_IN_PRODUCTION` additionally requires deployed sustained evidence. A merged implementation may still be `PARTIAL` or `FUNCTIONAL_UNVALIDATED`.

## 12. Hard Stops

Acceptance is blocked by any of the following:

- formal product source missing, changed without approval, or in unresolved conflict;
- token/secret leakage;
- owner-scope or public/private boundary failure;
- exact-identity loss or symbol/latest fallback;
- fake data shown as real;
- automatic open/close/add/reduce/reverse/order/trade path;
- Push Recheck or AI treated as trading authorization;
- Error rendered as success/Empty or stale cache overriding failure;
- missing real-scenario evidence;
- unsupported field, interaction, or completion claim.
