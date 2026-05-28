# V1 Capability Matrix

This matrix is the operational capability source of truth.

Current merged main at P291B creation:

- `26e8943 BACKEND-P291A Workflow Reset and Progress Source of Truth Pack (#703)`

Open PR #705 P292 is not counted as completed until merged.

## Capability Levels

| Level | Name |
|---:|---|
| 0 | NOT_STARTED |
| 1 | DOCS_ONLY_GATE |
| 2 | SKELETON |
| 3 | TARGETED_TEST |
| 4 | TEST_ONLY_WIRING |
| 5 | REVIEW_ONLY_RUNTIME |
| 6 | PRODUCTION_WIRING |
| 7 | PRODUCTION_READY |

## Matrix

| Module | Current level | Completed evidence | Not completed | Cannot be mistaken as | Next MAX_SAFE_PACK |
|---|---:|---|---|---|---|
| Watchlist Pool | 4 TEST_ONLY_WIRING | Boundary/proof rules, runtime source DTO/guard/no-op adapter/test-only wiring lineage are merged. | Canonical new scan-chain proof source is not fully closed. | Not Display Slots, not batch universe, not push universe. | Watchlist candidate source and proof-source alignment. |
| Display Slots | 5 REVIEW_ONLY_RUNTIME | Dashboard/default slot review surface exists. | Does not prove scan eligibility. | Not Watchlist Pool and not scan universe. | Keep as UI review surface while Watchlist Pool owns scan eligibility. |
| Risk Action Guard | 5 REVIEW_ONLY_RUNTIME | Review-only guard semantics and dashboard risk surfaces exist. | Full downgrade-action suggestion chain is incomplete. | Not order execution, not auto-trading, not permanent no-output block. | Review-only risk downgrade output pack. |
| RealScanInputContractDTO | 3 TARGETED_TEST | P279 merged DTO/enum/targeted tests. | No production market-read assembly. | Not runtime data read. | Reuse in MarketReadRequest wiring after source-of-truth fill. |
| RealScanInputContractGuardValidator | 3 TARGETED_TEST | P281 merged guard validator skeleton and targeted tests. | No production service wiring. | Not production scan loop. | Fold into market-read test-only wiring or assembler work. |
| RealScanInputContract test-only wiring | 4 TEST_ONLY_WIRING | P283 merged test-only wiring test proving DTO -> guard flow. | No production wiring. | Not runtime market read. | Reuse pattern for MarketReadRequest test-only wiring. |
| MarketReadRequestDTO | 3 TARGETED_TEST | P288 merged pure-data DTO skeleton and targeted tests. | No production market read; no runtime assembler. | Not production market read. | MarketReadRequest DTO -> GuardValidator test-only wiring. |
| MarketReadRequestGuardValidator | 3 TARGETED_TEST | P290 merged fail-closed validator skeleton and targeted tests. | No test-only wiring on merged main. | Not production wiring. | MarketReadRequest test-only wiring. |
| MarketReadRequest test-only wiring | 0 NOT_STARTED | Not merged on main. PR #705 is open and must not be counted. | No merged helper/fixture connects DTO into validator chain. | Not completed until PR #705 or equivalent merges. | P292 test-only wiring review/merge if approved. |
| MarketReadRequest assembler | 0 NOT_STARTED | No merged assembler for MarketReadRequest. | No conversion from RealScanInputContractDTO to MarketReadRequestDTO. | Not production assembler. | Test-only assembler, then review-only output assembler. |
| MarketReadAdapter | 3 TARGETED_TEST | Safe/no-op adapter skeleton and tests exist in watchlist scan lineage. | Not connected to live provider for new scan-chain. | Not real market read completion. | Review-only market-read output adapter with fake/test source first. |
| MarketQuoteClient legacy runtime | 6 PRODUCTION_WIRING | Legacy market quote client interface exists and is used by older runtime paths. | Not authorized for new scan-chain. | Not new scan-chain completion. | Separate migration/adapter authorization before scan-chain use. |
| BinanceMarketQuoteClient legacy runtime | 6 PRODUCTION_WIRING | Legacy Binance implementation exists for older runtime paths. | Not wired into P287-P290 MarketReadRequest chain. | Not authorized provider wiring for new scan-chain. | Separate provider boundary pack before scan-chain use. |
| runtime market read for new scan-chain | 0 NOT_STARTED | No merged runtime market-read chain for P287-P290 path. | No provider read, no runtime/live/external data read. | Not legacy market client. | Review-only MarketRead output / scan output with fake/test input first. |
| scan output | 3 TARGETED_TEST | Watchlist scan result DTO/assembler skeleton and targeted tests exist. | No production scan output from live market read. | Not real scan loop. | Review-only scan output from authorized MarketRead output. |
| ScanScore | 3 TARGETED_TEST | ScanScore DTO/rule/calculator skeleton and tests exist. | No production ScanScore over live scan output. | Not production scoring. | Review-only ScanScore over review-only scan output. |
| Candidate Attention | 3 TARGETED_TEST | Review-only Candidate Attention rule skeleton/tests exist. | No production Candidate workflow. | Not production candidate generation. | Review-only Candidate assembly from score. |
| Promote To Home | 3 TARGETED_TEST | Promote-to-home boundary/review-only semantics exist. | No production promote runtime. | Not dashboard promotion execution. | Review-only promote preview only. |
| Opportunity Push | 3 TARGETED_TEST | Review-only/no-op push, audit, queue, envelope, provider/channel skeletons exist. | No external send and no production push execution. | Not Telegram/email/webhook/app send. | Internal Opportunity Push preview. |
| Push Recheck | 6 PRODUCTION_WIRING | Legacy scheduled recheck service exists. | Not aligned with new review-only scan-chain preview. | Not permission to send messages. | Push Recheck alignment for internal preview. |
| Execution Advice | 5 REVIEW_ONLY_RUNTIME | ExecutionPlan review-only display exists. | Not backed by completed source-owned runtime candidate chain. | Not order instruction. | Review-only execution advice proposal. |
| entry / stop / TP / RR | 3 TARGETED_TEST | Source-owned candidate designs and fixture-level tests exist. | Review-only proposal runtime is not complete; production points are blocked. | Not production point generation. | Review-only entry/stop/TP/RR proposal pack. |
| manual position entry | 5 REVIEW_ONLY_RUNTIME | Manual/monitor review surfaces and position foundations exist. | Full manual-entry-to-monitor MVP loop is incomplete. | Not exchange write and not auto-position management. | Manual position entry alignment pack. |
| Position Monitor | 6 PRODUCTION_WIRING | Legacy monitor/sync foundations exist with guarded provider path. | Review-only action suggestion loop is incomplete. | Not auto-close, auto-reverse, or auto-order. | Position monitoring action suggestion pack. |
| AI Conflict / GPT / Gemini / Grok | 5 REVIEW_ONLY_RUNTIME | Heuristic conflict handling and role names exist. | Real GPT/Gemini/Grok provider orchestration is not complete. | Not actual three-AI arbitration. | AI conflict downgrade output and recovery-condition pack. |
| Dashboard MVP smoke | 5 REVIEW_ONLY_RUNTIME | Dashboard review surfaces exist. | Full end-to-end MVP smoke is incomplete. | Not proof of production data integrity. | Dashboard MVP smoke after upstream review-only outputs. |
| Review / Missed-valid logging | 5 REVIEW_ONLY_RUNTIME | Review/archive concepts and displays exist. | Missed-valid opportunity logging loop is incomplete. | Not automatic rule correction. | Review / missed-valid logging pack. |
| order API | 0 NOT_STARTED | Explicitly blocked. | No order API should be built in V1. | Not a V1 progress target. | None unless user opens future non-V1 scope. |
| execution API | 0 NOT_STARTED | Explicitly blocked. | No execution API should be built in V1. | Not a V1 progress target. | None unless user opens future non-V1 scope. |
| auto-trading | 0 NOT_STARTED | Explicitly blocked. | No auto-trading should be built in V1. | Not a V1 progress target. | None unless user opens future non-V1 scope. |
