# Fundamental AI v4.1 Productized UI Remediation

## Task Boundary

- Current mainline: `edc3615c03c9b71763c32574f1d811c1d9a8954d`
- Current block: PR `#1179` Desktop productization and runtime validation
- Capability movement: presentation and frontend consumption only
- User-visible output: productized Desktop Home, semantic states, compact Three-AI explanation, and evidence package
- Overreach boundary: no Figma, Mobile, Schema, API contract, Backend decision logic, or automatic trading change

## Remediation Matrix

| Frozen requirement | Previous expression | Current expression | Evidence |
|---|---|---|---|
| Product identity | Legacy/technical product labels | `Fundamental AI` + `多源证据决策系统` | `02`, login and analysis templates |
| Home hierarchy | Technical runtime emphasis | Current opportunity, Final Plan, and UserPosition boundaries | `02`, `21` |
| System Status | Values without consistent helper/tone | Six semantic status cells with helper text | `02` |
| Alerts/events | Large or generic empty blocks | Compact rows and exact empty copy | source contract tests |
| Dynamic Top6 | Product meaning unclear | `当前重点机会`, Pool count, scan state, three distinct empties | `05`-`07` |
| Search/Add | Add appeared available without a selected result | Add/Analyze disabled until explicit search result selection | `18` |
| Position empty | Generic placeholder | Exact manual-position boundary and two actions | `16` |
| Final empty | Raw state/technical contract | One user state, one reason, optional audit details | `09` |
| GPT role | Final-decision wording | Candidate formation and evidence explanation only | `10` |
| Gemini/Grok | Raw enum-heavy summaries | Mapped review and failure-path semantics | `11`, `12` |
| AI unavailable | Large empty field wall | Compact unavailable state with recovery actions | `04`, `14` |
| Consistency | Independent consistency module language | `冲突与最终调整` compact dependent summary | `15` |
| Audit metadata | Provider/trace visible in primary hierarchy | Collapsed under `查看审计详情` | `10`-`14` |
| Themes | Incomplete semantic token use | One neutral surface system with semantic state colors | `19`, `20` |

## Implementation Notes

1. `frontend-contract.js` is the single user-facing semantic mapper for plan, role, collection, data, bias, plan mode, opportunity type, and mixed AI values.
2. Home Top6 remains the authoritative backend projection; JavaScript does not rank, backfill, or hardcode symbols.
3. Asset context switching clears decision-bound content before loading and uses the request token guard to prevent stale response overwrite.
4. Candidate content is never rendered as Final. Final rendering still requires Final/source/validation/not-trade gates.
5. Technical metadata remains queryable through progressive disclosure without becoming the primary product surface.

## Safety

- Automatic open/close/add/reduce/reverse/order capability added: `0`
- Fake chart, K-line, trend line, vote, percentage, or AI score added: `0`
- Backend/API/Schema changes: `0`
- Mobile changes: `0`

## Status

`IMPLEMENTED_PENDING_INDEPENDENT_FRONTEND_AUDIT`

The current phase is not DONE until independent audit, PR merge, and merged-main validation complete.
