# TRINE LOGIC v4.1 Final Contract Implementation Report

Status: `IMPLEMENTED_AND_RUNTIME_VERIFIED`

Starting Head: `52bca71b3a4abdcebce9bac51759eb010b59dfd4`

Branch: `codex/v4-1-final-contract-machine-closure`

## Contract And Ownership

- D0 owns business objects and safety; R0 overrides only the named Owner
  reconciliation points; D1 owns interaction clarifications; D2 owns visual
  presentation.
- Existing AnalysisRun, EvidenceItem, ScoreItem, DecisionResult,
  ExecutionPlanCandidate, ExecutionPlan, AITrace, ConflictResolverResult,
  UserPosition, PositionMonitorLog, Review and Message owners are reused.
- Candidate, Final and UserPosition remain separate. Automatic trading
  capability count is zero.

## Machine-Executable Changes

- The calculation DAG is acyclic and uses EvidenceReliability before Final
  confidence exists.
- One versioned policy owns normalization, eight scores, DQ zones and the Plan
  source gate.
- `ruleMarketBias`, `validatedMarketBias` and `finalMarketBias` are separate.
- Six validated directions may start Candidate review. RANGE/WAIT do not start
  a directional plan.
- Rule Validation enforces state/mode legality and mode-specific Final field
  completeness.
- Home uses deterministic Tier 1 ranking and only recent formal Asset Pool
  analysis for Tier 2. System default templates are effective pool members,
  but unscanned templates cannot fill Home.
- Telegram keeps Message as the sole fact owner and retains the three bounded
  categories behind default-off switches.
- Thesis-free `MANUAL_INDEPENDENT` monitoring reports entry logic as N/A.

## Same-Run Runtime Evidence

An isolated local H2 application used the standard candidate JAR and the
official Binance public read-only endpoint. CoinGlass, all AI provider calls,
Telegram, trading and unrelated schedulers remained disabled. One authorized
ADAUSDT/5m Asset Pool scan persisted 100 closed bars for each of 5m, 15m, 1h
and 4h and produced:

| Object | Runtime value |
|---|---|
| requestId | `req-042e2123e4bf4aed967be69fadaf0dba` |
| analysisId | `ana-bc12ab0bb68d4713823ac28a1b85ef59` |
| traceId | `trace-9c3086bea1f549b194ab1f3b6938e165` |
| opportunityId | `opp-user-1-6-adausdt-5m` |
| candidateId | `candidate-28a493a8-76b9-4e46-92ff-18d834b9b1cd` |
| resolverResultId | `resolver-57594c73-295e-4daa-8e16-80224fd060b9` |
| validationResultId | `validation-45e841c8-62e7-4b9a-a908-a64d202b7988` |
| Final plan | absent, correctly fail closed |

All three role outputs were recorded as FALLBACK because the AI input contract
reported `SIGNIFICANT_EVIDENCE_CHANGE_MISSING`. Rule Validation returned
BLOCKED, no Final or position was created, and `notAutoTrading=true`.

The first two authorized replays exposed two concrete Home projection defects. The effective pool first
excluded system default templates even though the Asset Pool service had
already materialized them as effective user choices. After that was corrected,
the runtime showed five unscanned templates filling empty Home slots. The final
implementation accepts default templates as pool members but requires a formal
AnalysisRun before any Tier 2 projection.

The final authorized replay started from an empty H2 database, ingested one
real ADAUSDT closed-candle set from Binance, and executed exactly one Asset Pool
scan. The Home API returned one `OBSERVATION` card for ADAUSDT, zero unscanned
template cards and no Final-only fields. The authenticated browser rendered the
same single card at a fixed 1280 x 720 in-app viewport, with zero horizontal
overflow, zero console errors and no UI Review/fixture marker. The pre-fix
six-card response is retained only as defect evidence and is not acceptance
evidence.

Standard-JAR startup also exposed an H2 portability defect: H2 requires a
declared unique table constraint for the composite UserPosition ownership
foreign key, while the separate unique index was sufficient for PostgreSQL.
`schema.sql` now declares `UNIQUE (id, user_id)` inside `tm_user_position`;
the PostgreSQL V9/V15 history is unchanged. The V15 migration contract test
asserts both the unique ownership key and the review ownership foreign key.

## Validation

| Gate | Result |
|---|---|
| focused decision/Home/Telegram/position contracts | PASS |
| frontend contracts | PASS |
| full Java 17 Maven | 4919 run, 4905 passed, 0 failed, 0 errors, 14 skipped |
| Product Source Gate | PASS |
| Workflow Contract | PASS |
| core production-loop authorization | PASS |
| git diff check | PASS |

The 14 skipped tests require Docker/Testcontainers PostgreSQL. Earlier package
evidence covered PostgreSQL V1 to V15, restart and checksum validation; this
local rerun does not relabel a Docker skip as a new PostgreSQL pass.

## Conflict Scan

- Current production: no waiting_trigger+REDUCED, Candidate-as-Final, DQ<70
  directional Final, weak/normal new-plan Telegram, fixed-six fill, default
  plan values, source-less boundaries, automatic UserPosition creation or
  Telegram second fact owner.
- Current active Home: 70:30 via `home.html`/`home.css`.
- `dashboard.html` 60:40 evidence is an inactive sealed legacy route; the
  current `/dashboard` controller resolves to `home.html`.
- Negative tests and historical/sealed reports remain classified as such and
  are not consumed by production.

## Runtime Gate

`BROWSER_RUNTIME_EVIDENCE=PASS`. The final authorized post-fix replay confirms
that only the analyzed ADA asset is projected, the audit-chain IDs match, no
Final fields leak, no fake data is used and the active Home has no console or
horizontal-layout error. The wider 1440/1600/1728 responsive geometry remains
owned by the existing exact-head multi-viewport browser contract evidence; the
new real-provider screenshot is reported honestly at the in-app browser's
fixed 1280 x 720 viewport.
