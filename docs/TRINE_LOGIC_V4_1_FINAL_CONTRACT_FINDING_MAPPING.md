# TRINE LOGIC v4.1 Final Contract Finding Mapping

Status: `IMPLEMENTED_AND_RUNTIME_VERIFIED`

Starting Head: `52bca71b3a4abdcebce9bac51759eb010b59dfd4`

This matrix began as the mandatory pre-code mapping for the 18 reconciled
findings. The closure table below records the current production owner and
test evidence. The final isolated runtime replay used real Binance public
closed OHLCV, an empty H2 database and the standard release JAR.

| ID | Final contract | Existing owner/code | Baseline | This change | Required proof |
|---|---|---|---|---|---|
| F01 | D0/D1/D2 scoped authority | Product Source registry and core-loop authorization | CONFLICT | Register R0 and exact precedence | Product Source Gate and hash verification |
| F02 | Home no-opportunity truth | `OpportunityPriorityRankingServiceImpl` | PARTIAL | Explicit Tier 2/empty reason, no fixed fill | no-opportunity and real-pool tests |
| F03 | Owner-proposed observation fill | ranking service and `HomeTopAssetProjection` | PARTIAL | Tier 1 then real user-pool Tier 2 only | pool <6 and no fixture tests |
| F04 | Candidate/Final Home eligibility | Decision chain, ranking mapper | GAP | Candidate plan-in-progress; Final-only Tier 1 | Candidate cannot expose Final test |
| F05 | Deterministic Top6 ranking | ranking service/config | GAP | Exact formula, penalties, ties, +5 hysteresis | dynamic ranking matrix |
| F06 | Provider requirement and TTL | runtime market status/config | PARTIAL | Versioned dataset matrix and TTL permissions | mandatory/optional/stale/disabled tests |
| F07 | Eight scores and OpportunityScore | `ScoreServiceImpl`, `DecisionEngineService` | GAP | One no-default score contract and acyclic score formula | formula/missing-input tests |
| F08 | DQ, AI and fallback permission | assembler, decision engine, AI orchestrator | GAP | Exact DQ zones; zero AI below gate; no false success | DQ 69/70/85 and AI trace tests |
| F09 | State x Plan Mode | opportunity state service and rule validator | CONFLICT | Enforce the eight-state matrix | all legal/illegal combinations |
| F10 | high_risk responsibilities | semantic mapper, Message policy | PARTIAL | Separate current fact/change/material message | projection/dedupe tests |
| F11 | AI cost and scheduler fan-out | AI orchestrator/config/trace | PARTIAL | hard run/asset/hour/day/concurrency limits | quota, retry, cache tests |
| F12 | user/default asset ownership | Asset Pool, plans, positions, messages | PARTIAL | principal scope and fail-closed identity | cross-user/default materialization tests |
| F13 | Telegram three-category contract | Message, ChannelDelivery, HighValueAlert policy | CONFLICT | strong-plan + safety-change + active-position gates | three-category eligibility tests |
| F14 | Final/executable semantics | existing `ExecutionPlanDO` Final owner | PARTIAL | mode-specific fields; no second Final object | mode field/nullability tests |
| F15 | MANUAL_INDEPENDENT monitoring | UserPosition/PositionMonitor | GAP | N/A logic and explicit PnL coverage | no-thesis and coverage tests |
| F16 | Mobile/route delivery scope | registered D1/D2 controls | AMBIGUOUS | Preserve 14 routes; no Mobile/Figma work here | conflict scan and changed-file audit |
| F17 | same-run IDs and time chain | analysis, trace, plan and Home projections | PARTIAL | analysis/candidate/trace/plan provenance aggregation | same-run runtime/API/browser test |
| F18 | structural provenance and direction maturity | MarketBiasPolicy, boundary extractor, source gate | GAP | rolling normalizer, 4h/1h engine, seven-part numeric provenance | source-gate and direction tests |

## Closure Evidence

| ID | Status | Production evidence | Test/runtime evidence |
|---|---|---|---|
| F01 | PASS | Product Source registry contains D0, R0 and scoped precedence | Product Source Gate PASS |
| F02 | PASS | Home returns the actual eligible count and no fixed fill | ranking empty/pool-size tests |
| F03 | PASS | Tier 1 followed only by effective-pool Tier 2 rows with a formal AnalysisRun | default-template, foreign-owner and unscanned-template tests; final runtime Home projected only the analyzed ADA asset and zero unscanned templates |
| F04 | PASS | Candidate projection clears every Final-only field | ranking, Home and frontend field-isolation tests |
| F05 | PASS | versioned formula, stable tie order and five-point replacement threshold | ranking matrix and hysteresis tests |
| F06 | PASS | provider-owned closed OHLCV and versioned TTL matrix | mandatory/optional/stale/source-isolation tests |
| F07 | PASS | one shared score/DQ policy, no neutral defaults and acyclic OpportunityScore | score, assembler and decision-engine tests |
| F08 | PASS | DQ zones and AI/fallback permissions fail closed | DQ 69/70/85 and AI trace tests |
| F09 | PASS | Rule Validation enforces the eight-state Plan Mode matrix | legal/illegal state-mode tests |
| F10 | PASS | current high-risk fact, material change and Message ownership stay separate | Home, policy and dedupe tests |
| F11 | PASS | bounded calls, retries, cache and quota configuration | AI orchestrator quota/fallback tests |
| F12 | PASS | principal reads plus immutable default templates in the effective user pool | user/default/foreign-owner tests |
| F13 | PASS | Message remains fact owner; three Telegram categories use scoped eligibility | policy/message tests; real sends 0 |
| F14 | PASS | existing Final owner has mode-specific required/null fields | Final semantics and mapper tests |
| F15 | PASS | thesis-free MANUAL_INDEPENDENT uses N/A and explicit PnL coverage | monitor contract tests |
| F16 | PASS | 14 routes retained; no Mobile/Figma implementation in this package | changed-file and frontend contract audit |
| F17 | PASS | analysis/candidate/role trace/resolver/validation IDs aggregate by one trace | final real Binance/H2 run and authenticated browser proved the same-run fail-closed chain; Final, position and trading action counts remained zero |
| F18 | PASS | three direction layers, 4h/1h engine and numeric source provenance | direction, source-gate and integration tests |

## Object Ownership

| Fact | Reused owner | Duplicate owner allowed |
|---|---|---|
| Pool membership | AssetPool / AssetPoolItem | NO |
| Opportunity and transition log | existing Opportunity/AssetState owners | NO |
| Analysis | AnalysisRun | NO |
| Evidence and eight scores | EvidenceItem / ScoreItem | NO |
| Decision | DecisionBundle / DecisionResult | NO |
| Candidate | ExecutionPlanCandidate | NO |
| AI calls | AITrace/AiCallLog | NO |
| Conflict resolution | ConflictResolverResult | NO |
| Rule validation and Final | existing validation result / ExecutionPlan Final | NO |
| User position | UserPosition | NO |
| Monitoring | PositionMonitorLog | NO |
| Review | existing Review owners | NO |
| Notification fact | Message | NO |
| Telegram lifecycle | ChannelDelivery | NO |

## Test Ownership

Focused contracts belong beside their production owner. Cross-owner same-run
proof belongs in an integration test. Browser proof must read persisted IDs and
assert `fixture=false`; a mock-only test cannot close F17.

## Boundaries

- no Figma or Mobile implementation;
- no CoinGlass call or enablement;
- no real Telegram send;
- no automatic trade or position mutation;
- no second product object family;
- no push, merge or production deployment in this package.
