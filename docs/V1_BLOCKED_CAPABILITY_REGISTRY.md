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
| RuntimeKlineContext numeric point contract plan | Docs-only plan completed after P314 | P314 defines future runtime kline context fields, OHLCV completeness, latest price / close boundaries, wick / pin-bar, liquidity, stampede, multi-timeframe, event, abnormal data, and Risk Action Guard references, but it does not create RuntimeKlineContext Java DTOs or generate values. |
| DataQuality numeric point contract plan | Docs-only plan completed after P315 | P315 defines future data-quality fields, score thresholds, quality subscores, degradation rules, missing / blocked reasons, fixture matrix expectations, and Risk Action Guard references, but it does not create DataQuality Java DTOs or generate values. |
| MultiTimeframe numeric point contract plan | Docs-only plan completed after P316 | P316 defines future multi-timeframe fields, 4h / 1h / 15m / 5m roles, alignment states, conflict rules, fixture matrix expectations, and Risk Action Guard references, but it does not create MultiTimeframe Java DTOs or generate values. |
| Risk Action Guard numeric point contract plan | Docs-only plan completed after P317 | P317 defines future Risk Action Guard numeric point fields, chain position, risk layering, entry / stop / TP / RR guard review, incomplete / fail-closed rules, fixture matrix expectations, and Watchlist Pool / Display Slots boundary, but it does not create Risk Action Guard Java DTOs or generate values. |
| Numeric Point Safety Validator Plan | Docs-only plan completed after P318 | P318 defines future validator inputs, output statuses, safety flags, upstream contract checks, forbidden semantics, partial candidate handling, incomplete handling, fail-closed handling, and fixture matrix expectations, but it does not create Safety Validator Java or generate values. |
| Numeric Point Fixture Matrix Plan | Docs-only plan completed after P319 | P319 defines future fixture categories for positive, incomplete, fail-closed, degraded, partial, forbidden-semantics, Watchlist / Display Slots, external-channel, order / execution / auto-trading, and cross-contract consistency cases, but it does not create Java tests. |
| ReviewOnlyNumericPointProposalDTO Java Skeleton | DTO skeleton completed after P320 | P320 adds a plain Java DTO and targeted DTO tests for future review-only numeric point proposal candidates, but it does not create assembler Java, service wiring, dashboard runtime integration, executable point generation, external channel, order, execution, or auto-trading. |
| Numeric Point Safety Validator Java Skeleton | Validator skeleton completed after P321 | P321 adds a plain Java validator and targeted tests for DTO safety boundaries, but it does not create service wiring, dashboard runtime integration, executable point generation, external channel, order, execution, or auto-trading. |
| ReviewOnly Numeric Point Assembler Java Skeleton | Assembler skeleton completed after P322 | P322 adds a plain Java assembler and targeted tests for explicit-input-only DTO assembly and validator invocation, but it does not create service wiring, dashboard runtime integration, executable point generation, external channel, order, execution, or auto-trading. |
| Source-owned Numeric Point Candidate Assembler Plan | Plan completed after P323 | P323 defines future source-owned context selection, source ref binding, explicit proposal input creation, mandatory assembler invocation, and mandatory validator gating, but it does not create service wiring, dashboard runtime integration, executable point generation, external channel, order, execution, or auto-trading. |
| Source-owned Numeric Point Candidate Assembler Java Skeleton | Skeleton completed after P324 | P324 adds a plain Java source-owned candidate assembler and targeted tests, but it does not create service wiring, dashboard runtime integration, executable point generation, external channel, order, execution, or auto-trading. |
| Source-owned Numeric Point Candidate Assembler Verification | Active P325 docs-only verification | P325 verifies P320-P324 as a review-only numeric candidate chain, but it does not create Java, tests, service wiring, dashboard runtime integration, executable point generation, external channel, order, execution, or auto-trading. |
| point generation | Not completed | Review-only point boundary work does not equal executable point generation. |
| executable point generation | Blocked | Entry / stop / TP / RR values must not be generated as executable output. |
| SourceTrace Java DTO | Not completed | P313 is only the contract plan and must not be counted as Java DTO implementation. |
| RuntimeKlineContext Java DTO | Not completed | P314 is only the contract plan and must not be counted as Java DTO implementation. |
| DataQuality Java DTO | Not completed | P315 is only the contract plan and must not be counted as Java DTO implementation. |
| MultiTimeframe Java DTO | Not completed | P316 is only the contract plan and must not be counted as Java DTO implementation. |
| Risk Action Guard Java DTO | Not completed | P317 is only the contract plan and must not be counted as Java DTO implementation. |
| Numeric Point Safety Validator Java | Skeleton active in P321 | P321 adds only the validator skeleton and must not be counted as numeric point generation, assembler, service wiring, or production runtime. |
| Numeric Point Java fixture tests | Not completed | P319 is only the fixture matrix plan and must not be counted as Java test implementation. |
| Numeric Point assembler / service | Explicit-input assembler skeleton completed after P322; source-owned candidate assembler skeleton completed after P324; P325 verification active; service not completed | P322 and P324 are assembler-only, and P325 is docs-only verification. None must be counted as service wiring, production runtime, or executable point generation. |
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
