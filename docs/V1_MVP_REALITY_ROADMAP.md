# V1 MVP Reality Roadmap

This roadmap follows the business chain, not P numbers.

Current merged main at P291B creation:

- `26e8943 BACKEND-P291A Workflow Reset and Progress Source of Truth Pack (#703)`

Open PR #705 P292 is not counted as completed until merged.

## Roadmap

| Business step | Current status | Next MAX_SAFE_PACK | Risk level | Blocked? | User authorization required? |
|---|---|---|---|---|---|
| Watchlist candidate source | Boundary and proof semantics exist; canonical new scan-chain proof source still needs alignment. | Watchlist candidate source and proof-source alignment. | B | Partially blocked until proof source is explicit. | Yes, B confirmation before merge. |
| MarketReadRequest test-only wiring | Not completed on merged main; PR #705 is open. | Review and merge P292 if approved, or recreate test-only wiring pack. | B | Not blocked, but not done. | Yes, B confirmation before merge. |
| MarketReadRequest assembler | Not started on merged main. | Test-only assembler from RealScanInputContractDTO to MarketReadRequestDTO. | B | Production assembler blocked. | Yes, B confirmation before merge. |
| review-only market read output | Not started for new scan-chain. | Review-only output object from validated MarketReadRequest, no provider. | B | Runtime/provider read blocked. | Yes, B confirmation before merge. |
| review-only scan output | Skeleton/test layer exists; no output from MarketRead chain. | Assemble review-only scan output from review-only market read output. | B | Production scan output blocked. | Yes, B confirmation before merge. |
| review-only ScanScore | DTO/rule/calculator tests exist. | Score review-only scan output with explanation and fail-closed flags. | B | Production scoring blocked. | Yes, B confirmation before merge. |
| review-only Candidate | Candidate Attention/Promote skeletons exist. | Build review-only Candidate from review-only score. | B | Production Candidate workflow blocked. | Yes, B confirmation before merge. |
| internal Opportunity Push preview | No-op/audit/channel skeletons exist. | Internal preview only, no external send. | B | External send blocked. | Yes, B confirmation before merge. |
| Push Recheck alignment | Legacy recheck exists; not aligned with new preview chain. | Recheck internal preview for expiry/drift without sending. | B | External push execution blocked. | Yes, B confirmation before merge. |
| review-only Execution Advice proposal | ExecutionPlan display exists; full input chain incomplete. | Review-only advice proposal from review-only Candidate. | B | Order/execution/readiness blocked. | Yes, B confirmation before merge. |
| entry / stop / TP / RR proposal | Design/fixture tests exist; runtime review-only proposal incomplete. | Review-only proposal with source ownership and not-trade-instruction flags. | B/C | Production point generation blocked. | Yes; C if real numeric generation is production-facing. |
| manual position entry | Foundations exist; MVP loop incomplete. | Manual entry alignment for review-only monitor. | B | Exchange write blocked. | Yes, B confirmation before merge. |
| position monitoring action suggestion | Legacy monitor exists; action suggestion loop incomplete. | Review-only reduce/tighten/move/partial/invalidated suggestions. | B/C | Auto-close/reverse/order blocked. | Yes; C if it affects real risk actions. |
| AI conflict downgrade output | Heuristic conflict logic exists; real providers absent. | Downgrade/recovery-condition output without provider calls. | B/C | Real GPT/Gemini/Grok provider orchestration blocked. | Yes; C for provider integration. |
| dashboard MVP smoke | Dashboard surfaces exist; full chain smoke incomplete. | MVP smoke after upstream review-only outputs exist. | B | Major dashboard redesign blocked. | Yes, B confirmation before merge. |
| review / missed-valid logging | Review concepts exist; missed-valid loop incomplete. | Missed-valid logging and review archive pack. | B | Automatic rule correction blocked. | Yes, B confirmation before merge. |

## Operational Next Step

If PR #705 is still open, review P292 first after P291B merges.

Do not start P293 while P292 remains open unless the user explicitly cancels or supersedes P292.
