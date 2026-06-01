# V1 MVP Reality Roadmap

This roadmap follows the user-facing business chain rather than P-number order.

## Roadmap

| Step | Current status | Next MAX_SAFE_PACK | Risk level | Merge grouping recommendation |
|---|---|---|---|---|
| Watchlist candidate source | Boundary exists; production proof source for new scan-chain still needs alignment. | Watchlist candidate source and proof source alignment. | B | Can group with MarketReadRequest assembler planning if docs/test-only only. |
| Market read request wiring | Completed as test-only in P292: DTO flows into GuardValidator and returns review-only validation result. | P293 review-only output assembler. | B | Keep provider wiring separate; no production market client in the next slice. |
| Market read request review-only output | Completed in P293: validation result becomes readable review-only output with status, reasons, blockers, safety flags, and next-step message; P294 consumed it into review-only scan output. | Review-only Scan Output to Evidence / Score Entry. | B | Do not combine with provider wiring, scan score, Candidate, Push, Readiness, or point generation. |
| Review-only MarketRead scan output | Completed in P294: MarketRead request review output becomes a review-only scan output skeleton. | P295 review-only Evidence / Score entry. | B | Keep production scan output, Evidence generation, Score, Candidate, Push, Readiness, and point generation separate. |
| Market read adapter | Safe/no-op skeleton exists; new scan-chain is not wired to live provider. | Review-only adapter path with fixture/fake data before live provider. | B | Keep separate from provider wiring. |
| scan output | Review-only MarketRead scan output skeleton exists after P294; P295 turns it into a review-only Evidence / Score entry envelope. | P296 evidence normalization review-only slice. | B | Can group with Score input contract only if no runtime provider and no real score calculation. |
| review-only Evidence / Score entry | Completed in P295: review-only scan output becomes an Evidence / Score entry envelope, not real evidence or score. | P296 Evidence normalization review-only slice. | B | Keep real EvidenceItem generation, score calculation, Candidate, Push, Readiness, and point generation separate. |
| review-only evidence normalization | Completed in P296: Evidence / Score entry becomes a normalized evidence skeleton, not real EvidenceItem and not persisted evidence. | P297 score input / precheck review-only slice. | B | Keep real EvidenceItem generation, score calculation, Candidate, Push, Readiness, and point generation separate. |
| review-only score input / precheck | Completed in P297: normalized evidence becomes a score input precheck skeleton, not real ScoreItem and not score calculation. | P298 review-only score assembly. | B | Keep real ScoreItem generation, score calculation, Candidate, Push, Readiness, and point generation separate. |
| review-only score assembly | Completed in P298: score input precheck becomes a review-only score assembly skeleton, not real ScoreItem, not score calculation, not final score, and not direction. | P299 score-to-candidate handoff review-only slice. | B | Keep real ScoreItem generation, score calculation, final score, direction, Candidate, Push, Readiness, and point generation separate. |
| score-to-candidate handoff | Completed in P299: score assembly becomes a review-only candidate handoff skeleton, not real Candidate, Candidate Attention, Promote To Home, Push, Readiness, or point generation. | P300 Candidate Attention review-only slice. | B | Keep real Candidate, Candidate Attention production workflow, Promote To Home runtime logic, Push, Readiness, and point generation separate. |
| candidate attention | Completed in P300: candidate handoff becomes a review-only candidate attention skeleton, not real Candidate, not candidate rank, not candidate score, not Promote To Home, not Push, not Readiness, and not point generation. | P301 Candidate Preview / Ranking Guard review-only slice. | B | Keep real Candidate ranking, Push, Readiness, and point generation separate. |
| candidate preview / ranking guard | Active P301 PR: candidate attention becomes a review-only candidate preview guard skeleton, not real Candidate, not candidate rank, not candidate score, not real ranking result, not Promote To Home, not Push, not Readiness, and not point generation. | Internal Push Preview / Recheck Handoff review-only slice. | B | Keep real Candidate ranking, external push, Readiness, and point generation separate. |
| review-only ScanScore | Review-only calculator skeleton exists. | Review-only score over normalized evidence / precheck output with targeted tests after score assembly skeleton lands. | B | Can group with score explanation output only after score assembly remains review-only. |
| review-only Candidate | Candidate Attention / Promote skeleton exists, P299 prepares a handoff envelope, P300 prepares only a review-only candidate attention envelope, and P301 prepares only a review-only preview guard envelope. | Internal Push Preview / Recheck Handoff review-only slice. | B | Can group with internal push preview only if no external send and no production Candidate workflow. |
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
