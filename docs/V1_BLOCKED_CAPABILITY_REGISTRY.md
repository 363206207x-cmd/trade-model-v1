# V1 Blocked Capability Registry

This registry is the single place for repeated blocked capabilities.

Individual scope packs should reference this file instead of copying long blocked lists unless the package needs a narrow exception or a new boundary.

## Blocked Capabilities

| Capability | Current state | Meaning |
|---|---|---|
| auto-trading | Blocked | The system must not place, close, reverse, resize, or automate trades. |
| order API | Blocked | No order-placement API may be connected. |
| execution API | Blocked | No execution or broker/exchange write API may be connected. |
| external send | Blocked | No Telegram, email, webhook, app notification, local notification, or provider send is authorized. |
| external channel | Blocked until C authorization | Internal push preview and dashboard display do not authorize Telegram, email, webhook, app notification, local notification, or provider send. |
| readiness | Not completed | Readiness must first pass a review-only gate with recheck, Risk Action Guard, data quality, source trace, incomplete, and liquidity / stampede checks. |
| executable readiness | Blocked | Review-only readiness gate output must not unlock point generation, execution planning, external send, or trading action. |
| production readiness | Blocked | Readiness must not become executable trade readiness. |
| point boundary gate | Review-only skeleton completed after P308 | P308 may state whether a future source-owned review-only proposal can be reviewed, but it cannot generate point values. |
| source-owned review-only point proposal | Review-only skeleton completed after P309 | P309 may carry nullable, incomplete-safe entry / stop / TP / RR proposal fields, but it must not become executable point generation or a trade instruction. |
| point proposal closure / dashboard display gate | Review-only display gate completed after P310 | P310 exposes display-safe status and unavailable placeholders, but it does not modify dashboard runtime, generate executable point values, or become a trade instruction. |
| executable point generation pre-approval plan | Docs-only plan completed after P311 | P311 defines the future source trace, runtime kline context, data quality, multi-timeframe, liquidity / stampede / wick / strong reversal, and Risk Action Guard gates required before numeric point proposal work. |
| source-owned numeric point proposal plan | Docs-only plan completed after P312 | P312 defines the future review-only numeric proposal shape, source metadata, nullable fields, incomplete rules, and fail-closed rules, but it does not create Java or generate values. |
| SourceTrace numeric point contract plan | Docs-only plan completed after P313 | P313 defines future source trace fields, freshness states, entry / stop / TP / RR trace contracts, fixture matrix expectations, and Risk Action Guard references, but it does not create SourceTrace Java DTOs or generate values. |
| RuntimeKlineContext numeric point contract plan | Docs-only plan active | P314 defines future runtime kline context fields, OHLCV completeness, latest price / close boundaries, wick / pin-bar, liquidity, stampede, multi-timeframe, event, abnormal data, and Risk Action Guard references, but it does not create RuntimeKlineContext Java DTOs or generate values. |
| point generation | Not completed | Review-only point boundary work does not equal executable point generation. |
| executable point generation | Blocked | Entry / stop / TP / RR values must not be generated as executable output. |
| SourceTrace Java DTO | Not completed | P313 is only the contract plan and must not be counted as Java DTO implementation. |
| RuntimeKlineContext Java DTO | Not completed | P314 is only the contract plan and must not be counted as Java DTO implementation. |
| production point generation | Blocked | Real entry / stop / TP / RR production point generation is not complete. |
| executable entry / stop / TP / RR | Not completed | Entry, stop, TP, and RR must not be generated as executable output without source-owned trace, market context, data quality, structure confirmation, and manual review. |
| production MarketQuoteClient scan-chain wiring | Blocked | Legacy market clients may not be treated as authorized scan-chain provider wiring. |
| production Push execution | Blocked | Internal previews and no-op/audit paths do not equal external push execution. |
| production Candidate workflow | Blocked | Review-only Candidate skeletons do not equal production candidate generation. |
| order / execution / auto-trading | Blocked | Order placement, execution APIs, broker/exchange writes, and auto-trading remain outside the current V1 scope. |

## Interpretation Rule

Blocked does not mean the system can never produce a useful review-only proposal.

Blocked means the capability cannot automatically execute, externally send, mutate production state, or become production wiring without a separate authorization package.

For example:

- entry / stop / TP / RR proposals can be review-only outputs;
- risk downgrade suggestions can be review-only outputs;
- internal push previews can be review-only outputs;
- position-monitor suggestions can be review-only outputs.

Those outputs must remain manual-review required and not trade instructions.
