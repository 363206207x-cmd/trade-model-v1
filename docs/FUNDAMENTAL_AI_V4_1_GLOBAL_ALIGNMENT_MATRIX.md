# Fundamental AI v4.1 Global Alignment Matrix

Pre-implementation baseline: `a2168b784a3b181ea9e0d688f064d18e5091fd7b`
Classification is against the latest branch, not the historical 2026-08-20 baseline.

## Supplied finding revalidation

Every supplied finding is classified exactly once. Detailed producer/transport/consumer evidence is in `FUNDAMENTAL_AI_V4_1_GLOBAL_RUNTIME_AUDIT.md`; the `Evidence` column identifies the current decisive location.

| ID | Classification | Source / object | Current evidence and impact | Blocks current stage | Bounded correction |
|---|---|---|---|---|---|
| H-01 | ALREADY_FIXED | UI freeze / Home contract | current tests and `home.html` use frozen Home copy | NO | none |
| H-02 | ALREADY_FIXED | route ownership / Home | `/dashboard` returns `home`; workspace has no Home branch | NO | none |
| H-03 | ALREADY_FIXED | geometry / shell | no active Home 60:40 or 3fr/2fr residue | NO | none |
| H-04 | ALREADY_FIXED | AppShell | shared 64px rail/icon/token language is present | NO | none |
| H-05 | ALREADY_FIXED | AppShell | collapsed rail and overlay expansion behavior present | NO | none |
| H-06 | ALREADY_FIXED | UI freeze | current header/padding/gap geometry matches contract | NO | none |
| H-07 | ALREADY_FIXED | design tokens | current Home/workspace use the frozen token set | NO | none |
| S-01 | PARTIALLY_CURRENT | System Status | six slots exist, but selected asset supplies macro/system/global values | YES | correct projection scope and compact binding |
| S-02 | ALREADY_FIXED | System Status | no Top6 count in strip | NO | none |
| S-03 | ALREADY_FIXED | recorded account | runtime counts only valid active positions and truthful empty state | NO | none |
| O-01 | ALREADY_FIXED | Opportunity ranking | backend ranking eligibility/dedupe is present | NO | none |
| O-02 | ALREADY_FIXED | Opportunity renderer | frontend rejects ineligible/duplicate cards and never pads | NO | none |
| O-03 | ALREADY_FIXED | per-asset Opportunity/Final | card fields and exact Final projection are per asset | NO | none |
| O-04 | ALREADY_FIXED | Final boundary | no Final renders `—`, no Candidate fallback | NO | none |
| O-05 | ALREADY_FIXED | lifecycle | triggered + revalidation displays `正在重验` | NO | none |
| O-06 | ALREADY_FIXED | responsive Top6 | 6x1 >=1240, 3x2 below, no horizontal-scroll rule | NO | none |
| D-01 | ALREADY_FIXED | Position/Plan layout | `7fr / 3fr`, 16px | NO | none |
| D-02 | ALREADY_FIXED | narrow layout | Plan ordered before Position below 1120 | NO | none |
| P-01 | ALREADY_FIXED | Position header | frozen title present | NO | none |
| P-02 | ALREADY_FIXED | Position row | 22/28/28/22 CSS and semantic groups | NO | none |
| P-03 | ALREADY_FIXED | Position identity | symbol/direction/source only | NO | none |
| P-04 | ALREADY_FIXED | Position facts | entry/opened always; mark/PnL only trusted | NO | none |
| P-05 | ALREADY_FIXED | Position judgment | logic/reversal/risk/trend are independent | NO | none |
| P-06 | ALREADY_FIXED | Position conclusion/action | distinct fields; no fallback chain | NO | none |
| P-07 | ALREADY_FIXED | Position detail boundary | risk reason/monitor time not in Home compact row | NO | none |
| P-08 | ALREADY_FIXED | Position semantics | logic and conclusion remain independently owned | NO | none |
| P-09 | ALREADY_FIXED | monitor trust gate | untrusted state hides market/judgment/action and keeps opening facts | NO | none |
| P-10 | ALREADY_FIXED | position source | system-plan and independent-manual aliases normalized at boundary | NO | none |
| P-11 | ALREADY_FIXED | semantic tone | central tone + visible labels; not color-only | NO | none |
| F-01 | ALREADY_FIXED | Final Plan | exact module title present | NO | none |
| F-02 | ALREADY_FIXED | Final Plan | status/key/metadata layers present | NO | none |
| F-03 | ALREADY_FIXED | lifecycle | lifecycle-specific semantic tone | NO | none |
| F-04 | ALREADY_FIXED | revalidation | reason/recovery/latest lifecycle state shown | NO | none |
| F-05 | ALREADY_FIXED | Final access gate | only validated exact Final is visible | NO | none |
| A-01 | ALREADY_FIXED | AI Workspace | one workspace, three tabs, one active role | NO | none |
| A-02 | PARTIALLY_CURRENT | Gemini | reviewResult gate exists; three list/state pairs are merged in renderer | YES | preserve three independent groups |
| A-03 | PARTIALLY_CURRENT | Grok | causal path survives, but primary/authority semantics are wrong | YES | use failurePathState and remove Final-plan mutation implication |
| A-04 | ALREADY_FIXED | Resolver | summary consumes separate consistency/resolver fields and fails closed | NO | none |
| A-05 | ALREADY_FIXED | state/mode legality | legal selected fixture and guards preserve waiting-trigger/PREPARATION | NO | none |
| A-06 | ALREADY_FIXED | AI tabs | roving tabindex, arrows, Home/End, focus behavior present | NO | none |
| IA-01 | OUT_OF_SCOPE | Positions P1 IA | shared workspace still needs independent product acceptance | NO | register P1 only |
| IA-02 | OUT_OF_SCOPE | Analysis P1 IA | Preview/Opportunity density needs independent acceptance | NO | register P1 only |
| IA-03 | OUT_OF_SCOPE | Messages P1 IA | message grouping/target routing needs independent acceptance | NO | register P1 only |
| IA-04 | OUT_OF_SCOPE | Me P1 IA | settings IA needs independent acceptance | NO | register P1 only |
| IA-05 | OUT_OF_SCOPE | frontend Telegram IA | Telegram is explicitly excluded from this task | NO | no change |
| IA-06 | ALREADY_FIXED | auth boundary | login/session/security outside diff | NO | none |
| FD-01 | OUT_OF_SCOPE | focused P1 detail | separate P1 acceptance scope | NO | register P1 only |
| FD-02 | OUT_OF_SCOPE | focused P1 geometry | separate P1 acceptance scope | NO | register P1 only |
| FD-03 | OUT_OF_SCOPE | focused P1 content | separate P1 acceptance scope | NO | register P1 only |
| T-01 | ALREADY_FIXED | current static contracts | current semantics replace stale screenshot-only contract | NO | rerun |
| T-02 | ALREADY_FIXED | user copy inventory | current inventory excludes auth | NO | rerun |
| V-01 | ALREADY_FIXED | fixture isolation | profile + explicit enable + prod guard present | NO | rerun normal/ui-review |
| V-02 | PARTIALLY_CURRENT | visual evidence | prior evidence exists; exact post-patch Head must be recaptured | NO | capture post-patch screenshots |

## Revalidation totals

| Classification | Count |
|---|---:|
| CONFIRMED_CURRENT | 0 |
| PARTIALLY_CURRENT | 4 |
| ALREADY_FIXED | 41 |
| OUTDATED | 0 |
| OUT_OF_SCOPE | 8 |
| BLOCKED_BY_MISSING_SOURCE | 0 |
| Total supplied findings | 53 |

## Newly discovered findings

| ID | Severity | Frozen clause | Producer -> transport -> consumer | Current state | Blocks | Correction/test |
|---|---|---|---|---|---|---|
| N-01 | P0 | System Status object scopes | decisions/provider/account -> `SystemStateVO` -> status strip | selected asset leaks into system scope | YES | aggregate projection; ownership tests |
| N-02 | P0 | GPT primary three values | structured role -> `AiTabVO` -> `renderGpt` | summary promoted as conclusion | YES | exact first visual; DOM contract |
| N-03 | P0 | Gemini independent collections | three formal pairs -> `AiTabVO` -> `renderGemini` | arrays/states concatenated | YES | three sections; independent-state test |
| N-04 | P0 | Grok failure-path primary and no plan authority | failure path + challenge -> `AiTabVO` -> `renderGrok` | challenge summary primary; planModeImpact shown | YES | state primary, preserve causal chain, remove mutation implication |
| N-05 | P0 | unknown enum truthfulness | API value -> shared mapper/`label` -> all Home copy | unknown uppercase becomes role-unavailable copy | YES | `—` plus explicit surrounding state |
| N-06 | P0 | provider truthfulness | derivatives summary -> renderer | missing source hardcodes CoinGlass | YES | no source invention; source unavailable copy |
| N-07 | P0 | frozen navigation copy | static Home/workspace shell | `AI分析` missing canonical spacing | YES | text/ARIA-only correction |

## Current implementation mapping

| Clause | Previous binding | Required binding | Fail-closed behavior | Required evidence |
|---|---|---|---|---|
| Status environment | selected decision bias | BTC/macro decision, otherwise no assessment | `—`/waiting state | service unit + DOM |
| Status system risk | selected decision risk | aggregate system risk | no aggregate -> no assessment | service unit + DOM |
| Status data | selected decision quality | global decision quality/freshness | no quality -> waiting | service unit + DOM |
| Status service | AI header only | provider + AI combined availability | partial/unavailable must remain visible | service unit + DOM |
| GPT primary | candidate summary | bias/state/mode + Candidate-not-Final | role unavailable is one role failure panel | JS contract + screenshot |
| Gemini findings | one merged list/state | 3 independent list/state groups | each empty state rendered independently | JS contract + screenshot |
| Grok primary | challenge summary | failurePathState | no path/data/source states remain distinct | JS contract + screenshot |
| Unknown enum | `当前不可查看` | `—` with owning data/role state | true role UNAVAILABLE keeps unavailable copy | JS contract |
| Derivatives source | hardcoded CoinGlass default | source value only | missing source = source unavailable | JS contract + screenshot |

## P1 register

P1-IA-01 Positions independent IA acceptance; P1-IA-02 Analysis Preview/Opportunity acceptance; P1-IA-03 Messages classification/routing acceptance; P1-IA-04 Me layout acceptance; P1-IA-05 focused detail shell acceptance. No P1 implementation is authorized in this package.

## Post-remediation closure matrix

| Finding | Old binding | New binding | Verification | Status |
|---|---|---|---|---|
| N-01 | selected asset supplied system-scope environment/risk/data | BTC macro environment, aggregate risk/global quality, provider+AI service availability, explicit Hot Reset state | service unit test + 1440/1280/1080 DOM | PASS |
| N-02 | GPT summary promoted above frozen values | Candidate-not-Final plus Market Bias / Opportunity State / Candidate Mode | focused contract + runtime screenshot | PASS |
| N-03 | Gemini arrays and states merged | evidence gaps, logic conflicts, underestimated risks remain independent | focused contract + runtime tab | PASS |
| N-04 | challenge summary primary and plan impact implied authority | failure-path state primary; challenge detail secondary; no Final mutation field | focused contract + runtime tab | PASS |
| N-05 | unknown uppercase value read as unavailable | `—`; actual unavailable state stays explicit | mapper tests | PASS |
| N-06 | absent provider source displayed as CoinGlass | snapshot provider label or `来源不可用` | service/renderer contract | PASS |
| N-07 | `AI分析` | `AI 分析` | Home/workspace contract | PASS |

All 53 supplied findings remain classified against the pre-implementation
baseline above. This closure table records the result of the seven authorized
P0 corrections; the five P1 IA items remain registered and unimplemented.
