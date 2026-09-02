# Trade Model V1 Product Source of Truth

Status: `P0_PRODUCT_BASELINE_FREEZE_CANDIDATE`

This file is the single highest product-direction authority for Trade Model V1. It indexes the formal product plans, freezes cross-module semantics, and defines the Product Source Gate used before product work begins. It does not describe current implementation completion, workflow state, CI results, or production readiness.

## 1. Purpose

Every audit, design, implementation, test, repair, PR, merge gate, or deployment task must map its scope to the registered product sources before editing. Current code and current screens are implementation evidence, not the final product standard.

When implementation and a formal product source disagree, record a product gap. Do not weaken the product source to match current code. When two formal product sources genuinely conflict and precedence cannot resolve the conflict, stop for a human product decision; do not silently combine them.

## 2. Product Source Priority

The fixed authority order is:

1. `PRODUCT_SOURCE_OF_TRUTH`
2. formal V1 Product Architecture
3. formal Position Monitoring plan
4. formal AI Conflict / Confused / Push Recheck / Review plan
5. final Home design and interaction plan
6. frozen Figma design
7. formal business contracts
8. current runtime code
9. current phase and progress records
10. Workflow
11. Governance
12. Tests

Consequences:

- current code is not the final product standard;
- current UI is not the final product standard;
- a passing test does not prove product alignment;
- Governance and Workflow cannot override a formal product plan;
- `docs/ACTIVE_MAINLINE_STATUS.yml` reports current implementation state only;
- old PRs, old audits, task prompts, and chat history cannot override a formal source;
- current implementation may be more restrictive for safety, privacy, or fail-closed behavior, but it cannot claim an unapproved product capability;
- product-source changes require explicit human approval and a recorded reason.

For Fundamental AI v4.1 specifically,
`docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md` is the sole
ACTIVE/AUTHORITATIVE source. Earlier V1, Home, AI, Figma, authorization,
implementation and audit documents are historical/supporting evidence for
v4.1 and cannot override the unified source. Owner-final executable decisions
from the preserved `a60eff8d...` candidate are reconciled directly into that
canonical source rather than registered through a second product document. Its
page/route/component, ownership, PR-reuse and visual-density/proportion annexes
are part of the same authority, not additional Product Sources.

## 3. Registered Product Sources

The HTML comments below are the machine-readable registry consumed by `scripts/product-source-gate.sh`. They intentionally contain only source ID, repository path, SHA-256, and applicable-module label.

<!-- PRODUCT_SOURCE|PS-V1-ARCHITECTURE|docs/product-sources/V1_PRODUCT_ARCHITECTURE.md|8d2929207af67b592f7f4efd3dd1404018549f99f113fe1613a3d7a1ccf27842|ALL_PRODUCT_MODULES -->
<!-- PRODUCT_SOURCE|PS-POSITION-MONITORING|docs/product-sources/POSITION_MONITORING_COMPLETE_PLAN.md|c1a42a7d6dc7c0275ebccf2a61ceece157602b3e5327b062369d5e5c641369c3|POSITION_AND_REVIEW -->
<!-- PRODUCT_SOURCE|PS-AI-CONFLICT-RECHECK-REVIEW|docs/product-sources/AI_CONFLICT_RECHECK_REVIEW_PLAN.md|10c2f96c145371baa75bb46a7e6e6aa2c5f27ae4a43da4d96f008b42800feb3c|AI_STATE_PUSH_REVIEW -->
<!-- PRODUCT_SOURCE|PS-HOME-INTERACTION|docs/design/P3_U2_IPHONE_HOME_SEMANTIC_CONTRACT.md|1a51a9fc30d696a852d9193007f9d8aa00c6d5656ef2152eccec59c597e23834|HOME_AND_MOBILE_NAVIGATION -->
<!-- PRODUCT_SOURCE|PS-HOME-CORE-DATA-AUTHORIZATION|docs/P1B_HOME_CORE_DATA_AUTHORIZATION.md|3b149afd60063fd8a640258018f4aa7225a01f94f12cfb7eb9524975a4358628|HOME_CORE_DATA -->
<!-- PRODUCT_SOURCE|PS-P2-POSITION-MONITORING-AUTHORIZATION|docs/P2_POSITION_MONITORING_BACKEND_AUTHORIZATION.md|32f5e9351b8552a1dc82761b0e0b5e2bc54e69cb4a3647a73a343b294dd90919|POSITION_MONITORING_BACKEND -->
<!-- PRODUCT_SOURCE|PS-FUNDAMENTAL-AI-V4-1-DECISION-CHAIN|docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md|103b77453d0efaebe8d913b59281591b31e01f3894fb74453f3e74b5230f0c4d|V4_1_UNIFIED_PRODUCT_SOURCE -->
<!-- PRODUCT_SOURCE|PS-FUNDAMENTAL-AI-V4-1-FINAL-INTERACTION-AUTHORIZATION|docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_AUTHORIZATION.md|a3a108609ad080e42211e5fcfa9ede5ac7c66fbc3fab410f0abfde83ca8a6c1a|V4_1_FINAL_INTERACTION_AUTHORIZATION -->
<!-- PRODUCT_SOURCE|PS-FUNDAMENTAL-AI-V4-1-TARGET-RUNTIME-REMEDIATION-AUTHORIZATION|docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION.md|fcd2b953182d6bdedf2a332e0028dc824e95bbf781f405467c11b44bd764b24f|V4_1_TARGET_RUNTIME_REMEDIATION_AUTHORIZATION -->
<!-- PRODUCT_SOURCE|PS-FUNDAMENTAL-AI-V4-1-TELEGRAM-AUTHORIZATION|docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_AUTHORIZATION.md|261742b4bb2da3fe9234250003bb47e691e32123ee79856e2502a6c5b10e94c4|V4_1_TELEGRAM_CHANNEL_AUTHORIZATION -->
<!-- PRODUCT_SOURCE|PS-TRINE-LOGIC-TELEGRAM-TWO-CATEGORY-REMEDIATION-AUTHORIZATION|docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_AUTHORIZATION.md|5e254094531ea5dfcc33d8455ecb6f75551f5c22cf5e891b48787eca3b519323|V4_1_TELEGRAM_TWO_CATEGORY_REMEDIATION_AUTHORIZATION -->
<!-- PRODUCT_SOURCE|PS-TRINE-LOGIC-CORE-PRODUCTION-LOOP-AUTOMATION-AUTHORIZATION|docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION.md|25968904bddc80b5e5774cc13fb3de01193c5c33d1587e80473178078d01dce0|V4_1_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION -->
<!-- PRODUCT_SOURCE|PS-FUNDAMENTAL-AI-LOCAL-REAL-AUTHORIZATION|docs/FUNDAMENTAL_AI_LOCAL_REAL_READINESS_AUTHORIZATION.md|c994167bb2824ecbc3b2778ecf977ba5ef9adbcdb71c4dfc9da5926987add64e|LOCAL_REAL_READINESS_AND_CURRENT_HOME_AUTHORIZATION -->
<!-- PRODUCT_SOURCE|PS-FUNDAMENTAL-AI-FRONTEND-INTERACTION-RUNTIME-CLOSURE-AUTHORIZATION|docs/FUNDAMENTAL_AI_FRONTEND_INTERACTION_RUNTIME_CLOSURE_AUTHORIZATION.md|116a559ab8ee5a60d4233237a26bc88203b52380ad67a78d4019c025ebd54ea5|DESKTOP_RUNTIME_INTERACTION_CLOSURE_AUTHORIZATION -->
<!-- PRODUCT_SOURCE|PS-TRINE-LOGIC-MULTI-USER-ACCOUNT-REGISTRATION-AUTHORIZATION|docs/TRINE_LOGIC_MULTI_USER_ACCOUNT_REGISTRATION_AUTHORIZATION.md|417a93357663953de338fdb64ec9961c85589589c4feb4c0d0328dcaa198a7d7|PRIVATE_MULTI_USER_REGISTRATION_AND_DATA_ISOLATION_AUTHORIZATION -->
<!-- PRODUCT_SOURCE|PS-FIGMA-BASELINE|docs/FE04_POSITION_MONITORING_IMPLEMENTATION_FREEZE.md|fbb0fcd2987a9f98e85646bb73efa4925aaf79295ca4e72394ed5c6d3650d851|FE04_SCREENS_AND_COMPONENTS -->
<!-- PRODUCT_SOURCE|PS-FORMAL-BUSINESS-CONTRACT|docs/PROJECT_DELIVERY_CONTRACT.md|2330e29151336d95f881929cd908b050961557dd57aa2e0cb22c2b8e44e29a64|DELIVERY_AND_SAFETY -->

### PS-V1-ARCHITECTURE

| Attribute | Registration |
|---|---|
| Document name | Multi-source Evidence-driven Trading Decision Closed-loop System V1 |
| Repository path | `docs/product-sources/V1_PRODUCT_ARCHITECTURE.md` |
| Original source | `/Users/xuchao/Documents/多源证据驱动的交易决策闭环系统.docx` |
| Original SHA-256 | `822865bc41e34d660d96bd36ba1d78d18f902a5626efe27fba48c4b60fde9f0a` |
| Repository SHA-256 | `8d2929207af67b592f7f4efd3dd1404018549f99f113fe1613a3d7a1ccf27842` |
| Version/date | V1 source; original modified `2026-03-28` |
| Authority | Product architecture, priority 2 |
| Applicable modules | Data, Evidence, Scores, Decision, ExecutionPlan, AssetState, AI, Monitor, Review, frontend structure |
| Required concepts | real multi-source data; raw and standardized evidence; eight scores; 5m/15m/1h/4h; data-quality gate; rule-led base direction; market bias/confidence/risk; ExecutionPlan; AssetState; three-role AI subordination; replay and rule-version loop |
| Forbidden reinterpretations | no automatic trading; no AI-first decision; no fabricated source; no `triggered` as opened; no ExecutionPlan as UserPosition |

### PS-POSITION-MONITORING

| Attribute | Registration |
|---|---|
| Document name | Position Monitoring Complete Plan |
| Repository path | `docs/product-sources/POSITION_MONITORING_COMPLETE_PLAN.md` |
| Original source | `/Users/xuchao/Documents/持仓监控完整方案.docx` |
| Original SHA-256 | `19ed4323bc7ace42bd31f82be23ed0e984644861027ae12d01e222f11459e078` |
| Repository SHA-256 | `c1a42a7d6dc7c0275ebccf2a61ceece157602b3e5327b062369d5e5c641369c3` |
| Version/date | Complete-plan source; original modified `2026-04-29` |
| Authority | Position Monitoring product plan, priority 3 |
| Applicable modules | UserPosition, Position Detail, PositionMonitor, alerts, manual close, Review |
| Required concepts | manual user position; plan versus actual execution; original-logic validation; no/weak/strong reversal; stop/target distance; size/leverage/liquidity/wick risk; alerts and manual suggestions; PositionMonitorLog; manual close; post-close review |
| Forbidden reinterpretations | no automatic position creation, close, reduce, add, or reverse; no mixing system stops with user stops; no wick-only strong reversal |

### PS-AI-CONFLICT-RECHECK-REVIEW

| Attribute | Registration |
|---|---|
| Document name | Environment Reset, Push Recheck, Missed Opportunity Review, and AI Conflict Unified Plan |
| Repository path | `docs/product-sources/AI_CONFLICT_RECHECK_REVIEW_PLAN.md` |
| Original source | `/Users/xuchao/Documents/复盘与 AI 冲突处理统一落地方案.docx` |
| Original SHA-256 | `d2cc0762bff67de46a31d99b5e9817b26de425382db51118be2ad1d8bd921352` |
| Repository SHA-256 | `10c2f96c145371baa75bb46a7e6e6aa2c5f27ae4a43da4d96f008b42800feb3c` |
| Version/date | Unified implementation plan; original modified `2026-04-15` |
| Authority | AI conflict/state/recheck/review product plan, priority 4 |
| Applicable modules | Decision, GPT/Gemini/Grok, Confused, Hot Reset, Push Detail, Opportunity Review, Recovery |
| Required concepts | four conflict levels; fixed role boundaries; rules first; Confused enter/exit; Hot Reset; Push Recheck; validity recheck; Recovery; MISSED_VALID; unexecuted opportunity review; AI fallback |
| Forbidden reinterpretations | no three-way vote; no AI state-machine override; no Recheck trade authorization; no direct Confused-to-triggered exit; no AI disagreement causing permanent paralysis |

### PS-HOME-INTERACTION

| Attribute | Registration |
|---|---|
| Document name | P3-U2 iPhone Home Semantic Contract and companion final Home bundle |
| Primary repository path | `docs/design/P3_U2_IPHONE_HOME_SEMANTIC_CONTRACT.md` |
| Primary SHA-256 | `1a51a9fc30d696a852d9193007f9d8aa00c6d5656ef2152eccec59c597e23834` |
| Version/date | Current merged contract at baseline `2552dd24b1b756d5eb517e640baa772e1c5bcab6`; source date `2026-07-28` |
| Authority | Final Home design and interaction, priority 5 |
| Applicable modules | Home, five-tab mobile shell, focus assets, ExecutionPlan, AI summary, Top3 positions, contextual details, fail-closed states |
| Required concepts | exactly five tabs; Watchlist not a tab; asset card changes selected context and does not default-navigate; plan and AI follow selected asset; direct plan summary; exactly three roles; evidence/scores in Analysis Detail; Top3 positions; OPPORTUNITY/POSITION_RISK messages; Telegram as outlet only |
| Forbidden reinterpretations | no card-body default detail jump; no position rebinding on asset selection; no fabricated counts, percentages, fields, or successful unsupported route |

Companion files are part of this registered source bundle:

| Repository path | SHA-256 | Role |
|---|---|---|
| `docs/design/P3_U2_IPHONE_HOME_IA_V2.md` | `e356f76964992c331118fa43bc42a46cdfad5eccf26162b7f14471c3e07e5f39` | mobile information order, geometry, focus, and responsive behavior |
| `docs/design/P3_U2_IPHONE_HOME_FIELD_MAPPING.md` | `f19809ccec520eb4c2b5048b0857e03a520b3baf7bdbdab9820eb04df5e3a429` | exact field ownership and empty behavior |
| `docs/design/FE04_SEMANTIC_CONTRACT_V2.md` | `09abda774c51f6a1c84cdffe89ad4c6fb21f8c04cc5d1e4af16e45ffc6fa0834` | FE-04 navigation and semantic overlay |
| `docs/INTERACTION_CONTRACT_V3.md` | `a68e1efbc98e0d2e2952ca8081cf63b189b6bb0942928e7ec050e5735d494133` | desktop/mobile page, identity, state, and fail-closed interaction contract |

### PS-HOME-CORE-DATA-AUTHORIZATION

| Attribute | Registration |
|---|---|
| Document name | P1B Home Core Data Completion Authorization |
| Repository path | `docs/P1B_HOME_CORE_DATA_AUTHORIZATION.md` |
| Repository SHA-256 | `3b149afd60063fd8a640258018f4aa7225a01f94f12cfb7eb9524975a4358628` |
| Version/date | Product decision candidate; authorization effective only after merged-main validation |
| Authority | Explicit Product Source reconciliation for the P1B Home core-data package |
| Applicable modules | Asset Card, exact ExecutionPlan read projection, Top3 UserPosition, Home state handling |
| Required concepts | seven-field primary card body; subordinate four-field status strip; truthful source classification; exact persisted plan identity; owner-scoped independent UserPosition; LOADING/READY/PARTIAL/EMPTY/ERROR/MISSING; retry and stale-context clearing |
| Forbidden reinterpretations | no replacement of the seven primary fields; no latest/symbol/timeframe plan inference; no `tm_real_position`; no stale-success recovery; no AI/Score/notification/trading expansion |

For Home core-data work, the phrase "card body displays exactly" freezes the
seven primary business fields. It does not prohibit a visually subordinate,
non-navigating status strip containing `dataQuality`,
`multiTimeframeState`, `Confused`, and `updatedAt`. The strip is metadata, not
an eighth through eleventh primary field, and it must not displace or visually
outrank direction, confidence, risk, or AssetState. This explicit decision
resolves the prior ambiguity without changing the existing interaction rule:
card selection changes asset context and never selects a position or performs
a mutation.

### PS-P2-POSITION-MONITORING-AUTHORIZATION

| Attribute | Registration |
|---|---|
| Document name | P2 Position Monitoring Backend Implementation Authorization |
| Repository path | `docs/P2_POSITION_MONITORING_BACKEND_AUTHORIZATION.md` |
| Repository SHA-256 | `32f5e9351b8552a1dc82761b0e0b5e2bc54e69cb4a3647a73a343b294dd90919` |
| Version/date | Product authorization candidate; effective only after merged-main validation |
| Authority | Explicit authorization for the first bounded Product P2 backend package |
| Applicable modules | UserPosition-backed Position Monitor persistence, risk, trust, Home Position projection, and tests |
| Required concepts | independent entry-logic/conclusion/reversal/risk-reason semantics; per-position risk; verified fresh source gate; mark-price/PnL provenance; fail-closed state handling |
| Forbidden reinterpretations | no aggregate risk copied to each position; no semantic fallback; no fake zero/missing data; no automatic open/close/reduce/add/reverse/order; no Mobile or Figma change |

This record authorizes only `P2_POSITION_MONITORING_BACKEND_IMPLEMENTATION`.
It remains non-effective until reviewed and merged, and it does not make the
separate local candidate diff authoritative by existence.

### PS-FUNDAMENTAL-AI-V4-1-DECISION-CHAIN

| Attribute | Registration |
|---|---|
| Document name | Fundamental AI v4.1 Unified Product Source |
| Repository path | `docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md` |
| Original sources | `/Users/xuchao/Documents/唯一产品开发方案_最终冻结版.docx`; `/Users/xuchao/Documents/Fundamental_AI_v4.1_最终交互逻辑与页面设计开发规格_冻结版.docx` |
| Original SHA-256 | `91bcfbd154bc43b2176107bfc65a948271e10e3e9862027f3647dc13bf5e0900`; `43ec787f3228ec05e4e81a3c07fce4c3969c38850d709efa7097a2a406c463d3` |
| Repository SHA-256 | `103b77453d0efaebe8d913b59281591b31e01f3894fb74453f3e74b5230f0c4d` |
| Version/date | v4.1 unified final freeze; registered `2026-08-14` |
| Authority | Sole ACTIVE/AUTHORITATIVE v4.1 Product Source; business chapters, final interaction and normative annexes |
| Applicable modules | full decision chain plus 14 Desktop routes, 11 overlays, 54 component families, 81 acceptance states and runtime contracts |
| Required concepts | all original decision-chain concepts plus 18 final disambiguation contracts, dual analysis modes, plan lifecycle/revalidation, Message/Telegram ownership, selected context, complete routed interaction and the frozen Canonical Figma visual-density/proportion contract |
| Forbidden reinterpretations | no competing v4.1 source; no fake data/progress; no automatic trading; no AI rule bypass; no Preview persistence; no Candidate as Final; no plan as position; no duplicate owner; no second/non-canonical Figma or Design System; no Mobile implementation in current package |

Normative annex registration:

| Repository path | SHA-256 | Role |
|---|---|---|
| `docs/FUNDAMENTAL_AI_V4_1_VISUAL_DENSITY_AND_PROPORTION_CONTRACT.md` | `4d3e937be4534d69e07d34fcf3fe08c4cd5a63ed0bda58b4961ffe6249d26d61` | Canonical Desktop visual measurements and ratio supersession |

### PS-FUNDAMENTAL-AI-V4-1-FINAL-INTERACTION-AUTHORIZATION

| Attribute | Registration |
|---|---|
| Document name | Fundamental AI v4.1 Final Interaction Page and Runtime Authorization |
| Repository path | `docs/FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_AUTHORIZATION.md` |
| Repository SHA-256 | `a3a108609ad080e42211e5fcfa9ede5ac7c66fbc3fab410f0abfde83ca8a6c1a` |
| Version/date | Authorization candidate; effective only after merged-main validation |
| Authority | Exact implementation permission for one bounded final-interaction Desktop/runtime package |
| Applicable modules | The exact `FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_IMPLEMENTATION` package only |
| Required concepts | PR #1179 reuse, canonical ownership, exact package match, pre-merge block, post-merge implementation/PR/Canonical-Figma permission, single Canonical Figma key and independent final audit |
| Forbidden reinterpretations | no differently named/broader package, Mobile, second/non-canonical Figma or Design System, automatic trading, duplicate object stack, fake data, or implementation inside this authorization change |

The earlier Decision Chain authorization is
`HISTORICAL_REFERENCE_ONLY / SUPERSEDED` and is intentionally absent from the
active registry. The Final Interaction authorization and implementation are
effective historical delivery evidence through merged main
`3a6f56afaf6fbba3d094d532f7f9555a23ac30a1`; they do not authorize another
implementation package. Historical FE-04 and Canonical Figma records remain
supporting evidence and are not editable by the current runtime-remediation
authorization.

### PS-FUNDAMENTAL-AI-V4-1-TARGET-RUNTIME-REMEDIATION-AUTHORIZATION

| Attribute | Registration |
|---|---|
| Document name | Fundamental AI v4.1 Target Runtime Blocker Remediation Authorization |
| Repository path | `docs/FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION_AUTHORIZATION.md` |
| Repository SHA-256 | `fcd2b953182d6bdedf2a332e0028dc824e95bbf781f405467c11b44bd764b24f` |
| Version/date | Authorization candidate `2026-08-15`; effective only after merged-main validation |
| Authority | Exact implementation permission for one bounded B01-B04 target-runtime remediation package |
| Applicable modules | The exact `FUNDAMENTAL_AI_V4_1_TARGET_RUNTIME_BLOCKER_REMEDIATION` package only |
| Required concepts | accepted blocker evidence, existing-owner reuse, exact package match, pre-merge block, post-merge repository/implementation/PR permission, secret-free provider protocol validation, and independent remediation audit |
| Forbidden reinterpretations | no differently named or broader package, Product Source change, Figma/Desktop/Mobile change, live-secret repository write, duplicate owner, fake readiness/data, automatic trading, position mutation, or B01-B04 implementation inside this authorization change |

This subordinate authorization does not create a second v4.1 Product Source.
It maps reproduced target-runtime failures to the sole canonical source and
existing owners.

### PS-FUNDAMENTAL-AI-V4-1-TELEGRAM-AUTHORIZATION

| Attribute | Registration |
|---|---|
| Document name | Fundamental AI v4.1 Telegram High-Value Alert Authorization |
| Repository path | `docs/FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_AUTHORIZATION.md` |
| Repository SHA-256 | `261742b4bb2da3fe9234250003bb47e691e32123ee79856e2502a6c5b10e94c4` |
| Version/date | Authorization candidate `2026-08-16`; effective only after merged-main validation |
| Authority | Delivery authorization subordinate to the canonical v4.1 Product Source |
| Applicable modules | The exact `FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION` package only |
| Required concepts | Message-first persistence, ChannelDelivery-only Telegram, three high-value categories, trusted source gates, dedupe/cooldown/retry, secret-safe readiness and zero automatic trading |
| Forbidden reinterpretations | no second Message/Push/Position owner; no Preview/Candidate/unverified-position delivery; no secret persistence or live use before merged-main audit |

This subordinate authorization does not create a second v4.1 Product Source.
Its exact Telegram successor remains blocked until this record is effective on
clean, synchronized merged main.

### PS-TRINE-LOGIC-TELEGRAM-TWO-CATEGORY-REMEDIATION-AUTHORIZATION

| Attribute | Registration |
|---|---|
| Document name | TRINE LOGIC Telegram Two-Category Remediation Authorization |
| Repository path | `docs/TRINE_LOGIC_TELEGRAM_TWO_CATEGORY_REMEDIATION_AUTHORIZATION.md` |
| Repository SHA-256 | `5e254094531ea5dfcc33d8455ecb6f75551f5c22cf5e891b48787eca3b519323` |
| Version/date | Owner first-release narrowing authorization candidate `2026-08-26`; effective only after merged-main validation |
| Authority | Historical first-release narrowing, superseded where the later Owner-final canonical v4.1 contract explicitly differs |
| Applicable modules | Exact `FUNDAMENTAL_AI_V4_1_TELEGRAM_HIGH_VALUE_ALERT_CHANNEL_INTEGRATION` package only |
| Required concepts | all three in-app Message categories retained; Telegram Delivery narrowed to CONFIRMATION Final and trusted material active-position change; safety changes in-app only; missing source facts fail closed; existing owners reused |
| Forbidden reinterpretations | no Section 15.2 rewrite, third Telegram first-release category, `REDUCED` delivery, untrusted position delivery, second owner, automatic position scheduling, real send, secret access, deployment, Figma/Mobile or automatic trading |

This record remains delivery-history evidence. The later Owner-final canonical
contract restores all three fact-owned Telegram eligibility categories while
retaining the same Message ownership, trust, idempotency and zero-trading
boundaries; the historical narrowing cannot override that later decision.

### PS-TRINE-LOGIC-CORE-PRODUCTION-LOOP-AUTOMATION-AUTHORIZATION

| Attribute | Registration |
|---|---|
| Document name | TRINE LOGIC Core Production Loop Automation Authorization |
| Repository path | `docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION.md` |
| Repository SHA-256 | `25968904bddc80b5e5774cc13fb3de01193c5c33d1587e80473178078d01dce0` |
| Version/date | Owner authorization candidate `2026-08-27`; effective only after merged-main validation |
| Authority | Runtime-loop authorization subordinate to the sole canonical v4.1 Product Source |
| Applicable modules | Exact `FUNDAMENTAL_AI_V4_1_CORE_PRODUCTION_LOOP_AUTOMATION` package only |
| Required concepts | Asset Pool sole source; state-sensitive 15m/5m/2m/lightweight-1m opportunity loop; promotion-gated full analysis; Binance public SPOT closed 5m/15m/1h/4h OHLCV; active-position 30s monitoring; existing-owner reuse; production opt-in defaults off; canonical three-category Message/Telegram eligibility and stable-subject idempotency |
| Forbidden reinterpretations | no second scheduler/business/Telegram owner, cadence-only Schema field, cross-provider fallback, open/fixture candle as real, unconditional high-cost AI loop, automatic position mutation/trading, switch activation, secret access, Figma/Mobile, Staging or Production deployment in this authorization package |

Closed PR #1201 and its preserved Head are non-effective audit/recovery
evidence for the successor. This authorization does not make that branch
current, copy its rules, or create a parallel Telegram implementation.

### Owner-approved one-pass baseline reconciliation and exact B01-B04 successor

This is a subordinate machine-authorization record, not a second Product
Source. Owner explicitly authorized
`TRINE_LOGIC_V4_1_BASELINE_RECONCILIATION_GATE` to start from clean
`origin/main` at `08abe1f1040df0d4242a01cc306867ad5d3b4782` on branch
`codex/v4-1-baseline-reconciliation-gate`. It may reconcile only the four
named product/gate files, canonical gate-owner/status mirrors and their
existing contract tests. It adds no runtime capability.

The exact four normalization files are:

- `docs/PRODUCT_SOURCE_OF_TRUTH.md`;
- `docs/product-sources/FUNDAMENTAL_AI_V4_1_DECISION_CHAIN.md`;
- `docs/TRINE_LOGIC_CORE_PRODUCTION_LOOP_AUTOMATION_AUTHORIZATION.md`;
- `scripts/product-source-gate.sh`.

The successor contract is one-time and fail closed:

| Attribute | Exact value |
|---|---|
| Package | `REAL_DATA_HOME_BLOCKER_CLOSURE` |
| Branch | `codex/v4-1-real-data-home-blocker-closure` |
| Normalization source parent | `a60eff8d83c0e1d04371bd425267f1e8d0e4f95c` |
| Normalization expected source | merged `origin/main` after this reconciliation |
| Normalization changed-file count | `4` |
| Unauthorized normalization files | `0` |

The machine gate derives exactly one direct child of the source parent as the
normalized base, requires that commit to change only those four paths, and
requires every resulting blob to equal merged main. Package, branch, complete
source SHA, normalized commit topology and file contents all must match.
Missing, shortened, inferred, duplicated or reusable normalization evidence
fails closed. The permission cannot be used by another package, branch, source
SHA or second normalization commit.

After normalization and merged-main gate synchronization, the successor may
address B01 direction/state schema alignment and persistence-error
classification, B02 Home provenance projection, B03 asset-card vertical
overflow, B04 favicon, corresponding tests and real read-only acceptance. It
may proceed through its separately checked PR, merged-main and private Staging
acceptance under the Owner's one-pass authorization. Production deployment,
automatic trading, exchange private order APIs, fake data, weakened Final/DQ/
source/risk gates and secret disclosure remain forbidden.

### Owner-approved final runtime, Home, access and idempotency closure

This is a subordinate machine-authorization record, not a second Product
Source. Owner revoked the prior Home visual acceptance and authorized exactly
one final private-Staging closure package.

| Attribute | Exact value |
|---|---|
| Package | `V41_FINAL_RUNTIME_HOME_ACCESS_IDEMPOTENCY_CLOSURE` |
| Branch | `codex/v4-1-final-runtime-home-access-idempotency-closure` |
| Starting full SHA | `0e9bd779b10e9d3140b8ceaea0a5193a28d6264f` |
| Gate baseline rule | exact starting SHA must be an ancestor; the authorization branch must start directly from current clean merged main |
| Ranking contract | existing `V41-HOME-RANK-1`; qualified opportunities first, then real user-pool observations, stable selected-asset retention |
| Card contract | symbol/full name plus real price; direction, confidence/risk and 1h/4h context; missing values map to `观望`, `—`, `未知` and explicit data-insufficient text |
| Wide layout | true 1440 x 900 uses 6x1 cards, 70:30 Position/Plan with 320px Plan minimum, and visible AI Workspace entry |
| Acceptance | Codex runtime/visual audit first; explicit new Owner acceptance is required and prior acceptance is superseded |

The successor may change only the ten exact Home runtime, focused-test and
visual-evidence paths listed by `docs/CODEX_NEXT_TASK.yml`. Status-bar and alert
content must remain source-owned and fail closed; primary navigation remains
Home, Positions, Analysis, Messages and Me. The existing AnalysisRun
idempotency boundary is regression-tested, not replaced. No DTO, API, Schema,
migration, provider, scoring, direction, Plan Mode, Telegram-send, Figma,
Mobile, Production or automatic-trading change is authorized.

The exact product baseline remains immutable even though the authorization
branch starts from a newer clean merged main containing the completed
idempotency fix. This one-package ancestor rule does not authorize a wildcard,
directory grant, gate-owner mutation by the implementation branch, or any
other successor.

### Owner-approved real Provider and Three-AI runtime closure

This is a subordinate machine-authorization record, not a second Product
Source and not a change to the frozen scoring, direction, Plan Mode, AI-role,
Home visual or no-trade contracts. Owner authorized exactly one bounded
private-Staging runtime closure package from current merged main.

| Attribute | Exact value |
|---|---|
| Package | `V41_REAL_PROVIDER_AND_THREE_AI_RUNTIME_CLOSURE` |
| Branch | `codex/v4-1-real-provider-three-ai-runtime-closure` |
| Starting full SHA | `52201ba2d3d39d03aee8a005064e1ccf628f2491` |
| Gate baseline rule | authorization and implementation start from the exact merged-main SHA; package, branch and exact path allowlist fail closed |
| Existing owners | Binance persistence/source gate, CoinGlass provider/cache/derivatives Evidence adapter, AI Provider readiness/orchestrator, AnalysisRun and Home projection |
| Runtime objective | truthful six-asset Binance data, contract-complete CoinGlass Evidence, time-bounded Provider health, exact-model GPT/Gemini/Grok readiness and one guarded same-run chain |
| Acceptance boundary | exact-head CI and merged-main private-Staging evidence; explicit Owner visual review remains separate |

The successor may change only the twelve exact existing owner and focused-test
paths listed by `docs/CODEX_NEXT_TASK.yml`. It may normalize provider freshness
into the existing Evidence vocabulary, attach truthful value/comparison facts
to stale or partial status Evidence, make configured AI readiness recover via
the existing bounded scheduler, and expire CoinGlass health according to the
existing configured freshness TTL. Missing values remain missing and all
Provider and AI calls remain bounded, read-only and fail closed.

Runtime evidence may also close two proven wiring defects through those exact
existing owners: production Provider readiness may consume only cached runtime
health plus source-owned persisted closed candles, and the decision-chain AI
adapter may honor the already frozen bounded provider-specific timeout values
instead of the obsolete eight-second compatibility cap. This does not authorize
a new Provider, scoring/direction/Plan rule, persistence object, endpoint, or
visual behavior.

No Controller, DTO, Schema, migration, frontend, CSS, static asset, Product
Source semantics, Provider Matrix, quality threshold, scoring/direction/Plan
algorithm, AI role permission, Telegram send, Production deployment or
automatic-trading change is authorized. `ANALYSIS_PREVIEW` may not persist or
impersonate Opportunity, Candidate, Final Plan or position objects; persisted
Resolver and Rule Validation evidence must come only from an already legal
decision-chain mode.

### Owner-approved GPT background and Three-AI timeout closure

This is a subordinate machine-authorization record, not a second Product
Source and not a change to the frozen scoring, direction, Plan Mode, AI-role,
Home visual or no-trade contracts. The preceding real Provider runtime package
is effective on merged main. Owner authorized exactly one bounded successor to
close the remaining synchronous GPT timeout on private Staging.

| Attribute | Exact value |
|---|---|
| Package | `V41_GPT_BACKGROUND_THREE_AI_TIMEOUT_CLOSURE` |
| Branch | `codex/v4-1-gpt-background-three-ai-timeout-closure` |
| Starting full SHA | `1c13286eb64bb5b074e960352f8e290a317eb704` |
| Gate baseline rule | authorization and implementation start from the exact merged-main SHA; package, branch and exact path allowlist fail closed |
| Existing owners | `AnalysisRun`, `AITrace` (`tm_ai_call_log`), OpenAI Responses adapter, Three-AI orchestrator, Candidate, Conflict Resolver and Rule Validation |
| Runtime objective | provider-native OpenAI background submit/poll/recovery, one GPT Candidate, parallel Gemini/Grok and a bounded fail-closed 300-second chain |
| Acceptance boundary | exact-head CI and merged-main private-Staging real-provider evidence; explicit Owner visual review remains separate |

The 30-second OpenAI limit is the submit-or-ack deadline, not the full GPT
reasoning deadline. GPT may run for at most 180 seconds. Gemini and Grok each
have a 120-second deadline and start in parallel only after one valid GPT
Candidate. The whole chain has a 300-second deadline and at most one retry for
an explicitly transient transport failure. The configured GPT model remains
`gpt-5.6-sol`, reasoning effort is `medium`, text verbosity is `low`, and no
model fallback is authorized for the acceptance call.

The successor may extend the existing AITrace persistence with one forward-only
V17 migration and the H2 schema mirror only where required to retain
`providerResponseId`, attempt, task state, role/data state, submit/start/finish
times, token breakdown, failure classification and prompt/schema versions.
Submitted or running work must recover by polling the same provider response;
restart, refresh and duplicate triggers must not resubmit or duplicate a
Candidate, Final, alert, Message or Telegram event.

The versioned compact input keeps the frozen four-timeframe summaries,
source-owned Evidence identifiers and freshness, eight scores, CoinGlass
readings, rule direction/confidence/risk/Plan Mode, confused score, execution
feasibility, account risk and source/config versions. It excludes raw candle
walls, repeated prose, frontend copy, secrets and unrelated persistence fields.
Structured output remains mandatory and truncation fails closed.

Only the thirty-six exact backend, migration, configuration and focused-test
paths in `docs/CODEX_NEXT_TASK.yml` are authorized. V1-V16, frontend, CSS,
static assets, Home structure/copy, Provider algorithms, scoring/direction/Plan
eligibility, Telegram real send, Production deployment and automatic trading
remain blocked. No fake AI output may substitute for a failed role, and a real
Final remains optional when Rule Validation legitimately blocks it.

### Owner-approved AnalysisRun idempotency transaction-boundary fix

This is a completed subordinate machine-authorization record, not a second
Product Source. Owner authorized exactly one confirmed defect closure from merged main
`0e9bd779b10e9d3140b8ceaea0a5193a28d6264f`.

| Attribute | Exact value |
|---|---|
| Package | `ANALYSIS_RUN_IDEMPOTENCY_TRANSACTION_BOUNDARY_FIX` |
| Branch | `codex/v4-1-analysis-run-idempotency-tx-fix` |
| Starting full SHA | `0e9bd779b10e9d3140b8ceaea0a5193a28d6264f` |
| Authorized defect count | `1` |
| Canonical owner | existing `AnalysisRun` / `AnalysisIdempotencyGuard` |
| Required invariant | preserve `uk_tm_analysis_run_idempotency_key` and return one canonical `analysisRunId` |
| Mismatch behavior | fail closed as `IDEMPOTENCY_KEY_PAYLOAD_MISMATCH` or an existing equivalent normalized error |

The implementation changed only the historical exact six Guard, Mapper and
test paths registered by its authorization. PostgreSQL uses an atomic
conflict-safe claim or a fully rolled-back isolated transaction before reading
the canonical row. H2 must expose the same business semantics. Sequential and
2/10/50-way concurrent retries must produce exactly one row and one canonical
analysis identifier, while a reused key with different normalized payload must
fail closed.

The unique constraint may not be removed or weakened. No migration, scoring,
direction, plan, Home, provider, Telegram, position-monitoring, order,
execution or automatic-trading behavior was authorized. PRs #1209 and #1210
made the authorization and implementation effective on merged main and private
Staging. Production remains forbidden.

### PS-FUNDAMENTAL-AI-LOCAL-REAL-AUTHORIZATION

| Attribute | Registration |
|---|---|
| Document name | Fundamental AI Local-Real Readiness Authorization |
| Repository path | `docs/FUNDAMENTAL_AI_LOCAL_REAL_READINESS_AUTHORIZATION.md` |
| Repository SHA-256 | `c994167bb2824ecbc3b2778ecf977ba5ef9adbcdb71c4dfc9da5926987add64e` |
| Version/date | Authorization candidate `2026-08-20`; effective only after merged-main validation |
| Authority | Delivery authorization subordinate to the canonical v4.1 Product Source and approved Home interaction |
| Applicable modules | The exact `LOCAL_REAL_READINESS_SYNC_AND_REAL_ANALYSIS_ENABLEMENT` package only |
| Required concepts | authoritative normal-scan readiness synchronization, current Home live read binding, fail-closed missing data, existing-owner reuse, unchanged quality threshold and zero automatic trading |
| Forbidden reinterpretations | no Schema/API redesign, second readiness or Dashboard owner, new provider architecture, fake data, forced AI, Figma/Mobile redesign, deployment or automatic trading |

This subordinate authorization does not create a second v4.1 Product Source.
Its exact successor remains blocked until this record is effective on clean,
synchronized merged main.

### PS-FIGMA-BASELINE

Repository record: `docs/FE04_POSITION_MONITORING_IMPLEMENTATION_FREEZE.md`, SHA-256 `fbb0fcd2987a9f98e85646bb73efa4925aaf79295ca4e72394ed5c6d3650d851`.

| Surface | Node IDs |
|---|---|
| Mobile | Home `296:2`; Position `296:3`; AI `296:4`; Message `296:5`; Push Detail `296:6`; Profile `296:7` |
| Desktop | Dashboard `296:8`; Position `296:9`; AI `296:10`; Message `296:11`; Profile `296:12` |
| Components | Asset Card `28:154`; Execution Plan `31:23`; AI Role `35:97`; Position Monitor `32:26`; Message Card `299:54`; Push Detail `300:234` |

Figma shows design intent; it cannot invent data, APIs, state transitions, or completion.

## 4. Frozen Product Semantics

1. The rule layer produces base direction, confidence/risk context, and plan mode before AI review.
2. For v4.1, GPT Final generates only an ExecutionPlanCandidate; Gemini Review reviews that Candidate; Grok Challenge supplies counter-evidence and risk challenge; Conflict Resolver records adjustments; Rule Validation alone confirms the FinalExecutionPlan. They are not parallel voters.
3. AI is checkpoint-triggered, not required every cycle. AI failure falls back to the rule chain and cannot stop manual-position price safety monitoring.
4. `AssetState` and `UserPositionState` are separate domains.
5. `triggered` means conditions matched, not that the user opened a position.
6. `ExecutionPlan` is a system suggestion, never a user action or UserPosition.
7. UserPosition is created only from an authenticated explicit manual user action.
8. PositionMonitor validates the original plan and entry logic against current evidence. A short wick alone is not a trend reversal.
9. `Confused` is a conflict-breaker state, not ordinary observing, empty data, or low data quality.
10. Confused recovery enters observing/candidate or cooling as defined by the source; it never jumps directly to triggered.
11. Push Recheck re-evaluates a notification context. It is separate from PositionMonitor and never authorizes a trade.
12. The Home asset-card body switches selected asset context. It does not default-navigate to detail and does not alter the selected UserPosition.
13. Home displays the verified ExecutionPlan summary and three AI summaries; detailed evidence, eight scores, timeframes, source trace, and conflict reasons belong in Analysis Detail.
14. Message Center product sources are only `OPPORTUNITY` and `POSITION_RISK`. Telegram is a future delivery outlet, not a message type or Message Center.
15. A module is complete only when product, design, semantics, real data, interaction, failure handling, and a real scenario all pass.
16. Asset Pool is the only v4.1 opportunity source; Home focus assets are projections from it, not a fixed opportunity universe.
17. Opportunity state and execution permission are separate; `triggered` never creates UserPosition.
18. ExecutionPlanCandidate and FinalExecutionPlan are separate identities and storage contracts; an unvalidated Candidate is never a Final plan.
19. AITrace and ConflictResolverResult extend the existing AI call-log and conflict-resolver owners rather than creating parallel object families.

## 5. Permanent Safety and Privacy Boundaries

- No automatic open, close, reduce, add, reverse, order, or trade execution.
- Push Recheck, AI, ExecutionPlan, AssetState, and monitor suggestions cannot create or mutate UserPosition.
- `OPPORTUNITY` is authenticated shared public opportunity data and must contain no UserPosition, account-risk, position-risk, private reason, or private Recheck reference.
- `POSITION_RISK` is exact owner-scoped private data.
- API/data failure, stale data, missing identity, and partial data must remain distinguishable and fail closed.
- Fake records, fallback values, examples, and local-only placeholders cannot be presented as real product data.

## 6. Governance Limitation

The Product Source Gate may check only:

- registered source files exist and are non-empty;
- registered content hashes match;
- the current task declares a product module and required sources;
- the current task contains product, design, data, gap, allowed-scope, blocked-scope, real-scenario, and stop-condition mappings;
- permanent hard boundaries remain explicitly blocked.

The gate must not become a natural-language synonym engine, semantic parser, inventory/digest project, self-governing program, or substitute for product development. It does not prove that an agent understood a plan. Read-only product gap audits may proceed when sources and mapping exist even if implementation is blocked.

## 7. PRODUCT_FIRST_STOP_RULE

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

## 8. Task Conflict Rule

Stop before editing when any of these is true:

- a required registered source is missing, empty, or hash-mismatched;
- the task omits its product module or contract mapping;
- the task treats Governance, Workflow, tests, current code, or a PR as higher product authority;
- a formal source conflict cannot be resolved by the fixed priority;
- the task would weaken owner scope, public/private separation, fail-closed behavior, identity exactness, or the no-trading boundary;
- the task requires inventing a field, state, route, interaction, or completion claim.

## 9. Baseline Documents

- `docs/PRODUCT_MODULE_TREE.md`
- `docs/PRODUCT_RELATION_GRAPH.md`
- `docs/PRODUCT_STATE_MACHINE.md`
- `docs/PRODUCT_PAGE_INTERACTION.md`
- `docs/PRODUCT_FIELD_SOURCE.md`
- `docs/PRODUCT_COMPLETION_MATRIX.md`
- `docs/PRODUCT_GAP_ANALYSIS.md`
- `docs/PRODUCT_ROADMAP_V2.md`
- `docs/PRODUCT_ACCEPTANCE_STANDARD.md`
- `docs/PRODUCT_BASELINE_FREEZE_REPORT.md`

These are derived product baselines under this source index. They may explain or assess the product, but they cannot silently change a registered formal source.
