# V1 MVP Reality Roadmap

This roadmap follows the user-facing business chain rather than P-number order.

## Roadmap

| Step | Current status | Next MAX_SAFE_PACK | Risk level | Merge grouping recommendation |
|---|---|---|---|---|
| Watchlist candidate source | Boundary exists; production proof source for new scan-chain still needs alignment. | Watchlist candidate source and proof source alignment. | B | Can group with MarketReadRequest assembler planning if docs/test-only only. |
| Market read request wiring | Completed as test-only in P292: DTO flows into GuardValidator and returns review-only validation result. | P293 review-only output assembler. | B | Keep provider wiring separate; no production market client in the next slice. |
| Market read request review-only output | Active P293: validation result becomes readable review-only output with status, reasons, blockers, safety flags, and next-step message. | Review-only MarketRead output / scan output slice after P293 merges. | B | Do not combine with provider wiring, scan score, Candidate, Push, Readiness, or point generation. |
| Market read adapter | Safe/no-op skeleton exists; new scan-chain is not wired to live provider. | Review-only adapter path with fixture/fake data before live provider. | B | Keep separate from provider wiring. |
| scan output | Skeleton/test layer exists; no production output from live read. | Review-only scan output assembly from test-only market read result. | B | Can group with ScanScore input contract if no runtime provider. |
| review-only ScanScore | Review-only calculator skeleton exists. | Review-only score over scan output with targeted tests. | B | Can group with score explanation output. |
| review-only Candidate | Candidate Attention / Promote skeleton exists. | Review-only candidate assembly from score. | B | Can group with internal push preview only if no external send. |
| internal Opportunity Push preview | No-op/audit/channel skeletons exist. | Internal preview only, no external channel. | B | Can group with Push Recheck preview. |
| Push Recheck | Legacy recheck exists; not integrated with new preview chain. | Internal preview recheck with expiry/drift handling. | B | Keep external send blocked. |
| review-only Execution Advice | Display exists, but full chain input not complete. | Execution advice proposal from review-only Candidate. | B | Can group with entry/stop/TP/RR proposal tests. |
| entry / stop / TP / RR proposal | Fixture/design/test pieces exist; runtime proposal chain incomplete. | Review-only proposal pack with source ownership and guard status. | B | Keep separate from order/execution. |
| manual position entry | Legacy position foundations exist. | Manual position entry review-only state alignment. | B | Can group with position-monitor display if no provider write. |
| position monitoring | Legacy monitor/sync foundation exists. | Review-only monitor action suggestions. | B | Keep automatic close/reverse blocked. |
| AI conflict downgrade | Heuristic role/conflict logic exists; real GPT/Gemini/Grok orchestration not complete. | AI conflict downgrade and recovery-condition pack. | B/C | Provider integration requires separate C-level authorization. |
| dashboard MVP smoke | Dashboard has review surfaces; full chain smoke incomplete. | MVP smoke after upstream review-only outputs exist. | B | Keep separate from major dashboard redesign. |
| review / missed-valid logging | Review concepts exist; feedback loop incomplete. | Missed-valid logging and review archive pack. | B | Can group with dashboard archive display if safe. |

## MVP Definition

MVP means the user can review a safe, non-executable chain:

Watchlist candidate -> market read request -> market read result -> scan output -> review-only score -> review-only Candidate -> internal push preview -> recheck -> review-only execution advice -> manual position monitor -> review / missed-valid log.

MVP does not require auto-trading, external send, or executable readiness.
