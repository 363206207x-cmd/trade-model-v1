# V1 Capability Matrix

This matrix is the fixed capability-level view for Trade Model V1.

Capability levels:

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

| Module | Current capability level | Completed | Not completed | Next MAX_SAFE_PACK | Cannot be mistaken as |
|---|---:|---|---|---|---|
| Watchlist Pool | 4 TEST_ONLY_WIRING | Boundary and proof rules exist; prior DTO / guard / no-op adapter work constrains candidate source. | Canonical production proof source is not fully closed for the new scan-chain. | Business-chain pack for Watchlist candidate source and proof source alignment. | Not Display Slots, not batch universe, not push universe. |
| Display Slots | 5 REVIEW_ONLY_RUNTIME | Dashboard can display default slots and related state as a review surface. | Display Slots do not prove scan eligibility. | Keep as UI review surface while scan source comes from Watchlist Pool. | Not Watchlist Pool and not scan universe. |
| Risk Action Guard | 5 REVIEW_ONLY_RUNTIME | Guard rules and dashboard review semantics exist. | It does not yet provide a complete downgrade-action suggestion chain for every MVP path. | Review-only downgrade output pack. | Not order execution, not auto-trading, not a permanent no-output block. |
| RealScanInputContractDTO | 3 TARGETED_TEST | DTO and targeted tests preserve review-only / fail-closed semantics. | No production market read assembly. | Reuse in business-chain market-read test-only wiring after P291A. | Not runtime data read. |
| RealScanInputContractGuardValidator | 3 TARGETED_TEST | Validator skeleton and tests block missing proof / unsafe flags. | No production service wiring. | Fold into market-read test-only wiring pack. | Not production scan loop. |
| MarketReadRequestDTO | 3 TARGETED_TEST | Pure-data DTO with frozen P287 fields and targeted tests merged in P288. | No production assembler and no runtime market-read execution. | Feed only safe review-only/test-only assembly slices until provider wiring is authorized. | Not MarketQuoteClient wiring. |
| MarketReadRequestGuardValidator | 3 TARGETED_TEST | Fail-closed validator and targeted tests merged in P290. | No production service wiring to the broader scan chain yet. | Feed only safe review-only/test-only assembly slices until provider wiring is authorized. | Not production market read. |
| MarketReadRequest test-only wiring | 4 TEST_ONLY_WIRING | P292 merged: test-scope helper feeds `MarketReadRequestDTO` into `MarketReadRequestGuardValidator` and returns review-only validation result. | No production service wiring and no runtime market-read execution. | P293 review-only output assembler slice. | Not production wiring. |
| MarketReadRequest review-only output assembler | REVIEW_ONLY_OUTPUT_SKELETON | P293 merged: review-only DTO and assembler turn `MarketReadRequestDTO` + guard result into readable manual-review output. | No production assembler, no provider call, no runtime market-read execution, and no production scan output. | Feed P294 review-only scan output skeleton. | Not runtime provider call, not production assembler, not production scan output. |
| Review-only MarketRead scan output | REVIEW_ONLY_SCAN_OUTPUT_SKELETON | P294 merged: review-only scan output DTO and assembler turn `MarketReadReviewOnlyOutputDTO` into a safe manual-review scan output skeleton. | No production scan output, no runtime market read, no Score, no Evidence generation, no Candidate, no Push, no Readiness, and no point generation. | Review-only Scan Output to Evidence / Score Entry. | Not production scan output, not score, not Evidence, not Candidate, not Push, not Readiness. |
| Evidence / Score entry point | REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_SKELETON | Active P295 PR #721 adds a review-only entry DTO and assembler from `MarketReadReviewOnlyScanOutputDTO`. | No Evidence item generation, no Score item generation, no production ScanScore, no Candidate, no Push, no Readiness, and no point generation. | Evidence normalization review-only slice after P295 merges. | Not completed Evidence generation, not completed Score calculation, not Candidate, not Push, not Readiness. |
| MarketReadAdapter | 3 TARGETED_TEST | Safe adapter skeleton / no-op boundary exists for watchlist scan chain. | It is not connected to live provider data for the new scan-chain. | Review-only market-read adapter with fake/test fixture first, provider later. | Not real market read completion. |
| MarketQuoteClient / BinanceMarketQuoteClient legacy runtime | 6 PRODUCTION_WIRING | Legacy runtime client and Binance implementation exist in older services. | Not authorized as production scan-chain market-read wiring. | Separate migration/adapter authorization before use in scan-chain. | Not proof that P287-P290 market-read chain is complete. |
| scan output | 3 TARGETED_TEST | Review-only result DTO / assembly skeletons exist. | No production scan output from live market data. | Market read to scan output review-only runtime pack. | Not production scan loop. |
| ScanScore | 3 TARGETED_TEST | DTO / rule / review-only calculator skeleton and tests exist. | No production ScanScore over live scan output, and P295 only adds entry envelope, not score calculation. | Evidence normalization review-only slice before broader score runtime. | Not production scoring. |
| Candidate | 3 TARGETED_TEST | Candidate Attention / Promote review-only skeleton exists. | No production Candidate workflow from live scored scans. | Review-only Candidate assembly pack. | Not production candidate generation. |
| Opportunity Push | 3 TARGETED_TEST | Review-only / no-op push, audit, queue, envelope, channel skeletons exist. | No external send and no production push execution. | Internal push preview + Push Recheck pack. | Not Telegram/email/webhook send. |
| Push Recheck | 6 PRODUCTION_WIRING | Legacy scheduled recheck service exists and reads market client. | Not integrated with the new review-only scan-chain output. | Review-only internal preview recheck, no external send. | Not permission to send messages. |
| ExecutionPlan | 5 REVIEW_ONLY_RUNTIME | Review-only execution plan display exists. | Not backed by a completed source-owned runtime candidate chain for all MVP outputs. | Review-only execution advice proposal pack. | Not order instruction. |
| entry / stop / TP / RR | 3 TARGETED_TEST | Source-owned candidate designs and fixture-level tests exist. | No complete review-only runtime proposal chain from live scan output. | Review-only proposal pack with explicit not-trade-instruction flags. | Not executable trade signal. |
| Position Monitor | 6 PRODUCTION_WIRING | Legacy position monitor / sync foundations exist, with simulated default and provider path guarded. | MVP review-only action suggestions are not fully connected to execution advice and monitor alerts. | Manual position entry + review-only monitor action pack. | Not auto-close, auto-reverse, or auto-order. |
| AI Conflict / GPT / Gemini / Grok | 5 REVIEW_ONLY_RUNTIME | Heuristic conflict handling and role names exist. | No real three-AI provider orchestration, budget, cache, rate-limit, fallback, or final challenge chain. | AI conflict downgrade rule pack before provider integration. | Not actual GPT/Gemini/Grok arbitration. |
| Dashboard | 5 REVIEW_ONLY_RUNTIME | Dashboard review surfaces exist for many read-only areas. | MVP smoke for the full business chain is not complete. | Dashboard MVP smoke pack after review-only chain outputs exist. | Not proof of production data integrity. |
| Review | 5 REVIEW_ONLY_RUNTIME | Review/archive concepts and read-only display patterns exist. | Missed-valid logging and full feedback loop are not complete. | Review / missed-valid logging pack. | Not automatic rule correction. |
| order / execution / auto-trading | 0 NOT_STARTED | Explicitly out of V1 runtime scope and blocked. | No order API, execution API, or auto-trading should be built. | None unless user explicitly opens a future non-V1 scope. | Not a V1 progress target. |
