# V1 Allowed Review-Only Outputs

Review-only does not mean no output.

The system may produce useful, structured, non-executable proposals when they are clearly marked as:

- manual-review required;
- not a trade instruction;
- not an order;
- not executable by the system;
- blocked from external send unless a later package explicitly authorizes that channel.

## Allowed Review-Only Outputs

The following outputs are allowed in review-only scope when their source ownership, freshness, risk state, and guard result are visible:

- entry zone proposal;
- stop zone proposal;
- TP proposal;
- RR estimate;
- position size suggestion;
- leverage cap suggestion;
- invalidation condition;
- reduce position;
- tighten stop;
- move stop;
- partial take-profit;
- wait for trigger;
- plan invalidated;
- manual review required;
- internal push preview;
- risk downgraded candidate;
- confused with recovery condition;
- readiness review-only status;
- point boundary review-only status;
- source-owned review-only point proposal;
- point proposal review-only display status;
- executable point generation pre-approval status;
- source-owned numeric point proposal plan status;
- SourceTrace numeric point contract status;
- SourceTrace missing / stale / conflicted reason;
- RuntimeKlineContext numeric point contract status;
- RuntimeKlineContext missing / stale / gap / abnormal reason;
- DataQuality numeric point contract status;
- DataQuality missing / degraded / threshold failure reason;
- MultiTimeframe numeric point contract status;
- MultiTimeframe missing / conflicted / wick-only / noise-only reason;
- Risk Action Guard numeric point contract status;
- Risk Action Guard missing / blocked / recheck reason;
- Numeric Point Safety Validator plan status;
- Numeric Point Safety Validator incomplete / blocked / degraded reason;
- Numeric Point Fixture Matrix plan status;
- Numeric Point fixture expected outcome summary;
- ReviewOnlyNumericPointProposalDTO skeleton status;
- ReviewOnlyNumericPointProposalDTO missing / blocked / degraded reason;
- Numeric Point Safety Validator skeleton status;
- Numeric Point Safety Validator incomplete / blocked / degraded reason;
- ReviewOnly Numeric Point Assembler skeleton status;
- ReviewOnly Numeric Point Assembler validation result status;
- Source-owned Numeric Point Candidate Assembler plan status;
- Source-owned numeric candidate missing / blocked / degraded reason plan;
- Source-owned Numeric Point Candidate Assembler skeleton status;
- Source-owned Numeric Point Candidate Assembler validation result status;
- Source-owned Numeric Point Candidate Assembler verification status;
- Source Context Integration Plan status;
- source context missing / blocked integration reason;
- SourceTrace Numeric Source Read Model Plan status;
- SourceTraceNumericSourceContextDTO skeleton status;
- SourceTrace numeric source missing / blocked reason;
- SourceTraceNumericSourceReadModelValidator skeleton status;
- SourceTrace numeric source validation result status;
- SourceTrace numeric source validator incomplete / blocked / degraded reason;
- SourceTrace Numeric Source Validator Verification status;
- SourceTraceNumericSourceReadModelAssembler skeleton status;
- SourceTrace numeric source assembler validation result status;
- SourceTrace Numeric Source Assembler Verification status;
- SourceTrace Runtime / Source Binding Plan status;
- SourceTrace Runtime / Source Binding Verification status;
- RuntimeKlineContext Source Binding Plan status;
- RuntimeKlineContextSourceBindingDTO status;
- RuntimeKlineContextSourceBindingValidator skeleton status;
- RuntimeKlineContext source binding validation result status;
- RuntimeKlineContext source binding incomplete / blocked / degraded reason;
- RuntimeKlineContextSourceBindingAssembler skeleton status;
- RuntimeKlineContext source binding assembler validation result status;
- RuntimeKlineContext Source Binding Verification status;
- DataQualityContext source binding DTO status;
- DataQualityContext source binding validator result status;
- DataQualityContext source binding assembler validation result status;
- DataQualityContext Source Binding Verification status;
- MultiTimeframeContext source binding DTO status;
- MultiTimeframeContext source binding validator result status;
- MultiTimeframeContext source binding assembler validation result status;
- MultiTimeframeContext Source Binding Verification status;
- RiskActionGuard source binding DTO status;
- RiskActionGuard source binding validator result status;
- RiskActionGuard source binding assembler validation result status;
- RiskActionGuard Source Binding Verification status;
- WatchlistPoolProof source binding DTO status;
- WatchlistPoolProof source binding validator result status;
- WatchlistPoolProof source binding assembler validation result status;
- WatchlistPoolProof Source Binding Verification status;
- Source-Owned Candidate Integration Boundary Plan status;
- source-owned candidate integration readiness status;
- source-owned candidate unavailable reason;
- source binding completeness summary;
- review-only candidate boundary label;
- review-only candidate blocked reason;
- review-only candidate degraded reason;
- source-owned candidate integration trace refs;
- Source-Owned Candidate Integration Source Binding Plan status;
- source-owned candidate integration binding status;
- source-owned candidate source binding unavailable reason;
- source-owned candidate source binding completeness summary;
- source-owned candidate source binding trace refs;
- SourceOwnedCandidateIntegrationSourceBindingDTO skeleton status;
- SourceOwnedCandidateIntegrationSourceBindingValidator result status;
- SourceOwnedCandidateIntegrationSourceBindingValidator Verification status;
- SourceOwnedCandidateIntegrationSourceBindingAssembler Plan status;
- SourceOwnedCandidateIntegrationSourceBindingAssembler validation result status;
- SourceOwnedCandidateIntegrationSourceBindingAssembler Verification status;
- Source-Owned Candidate Integration Runtime Boundary Plan status;
- source-owned candidate runtime integration status;
- source-owned numeric point proposal unavailable reason;
- point proposal unavailable reason;
- incomplete reason;
- point boundary unavailable reason;
- manual review required;
- risk guard required reason.

## Forbidden Executable Outputs

The following remain forbidden unless a future explicitly authorized scope changes the project boundary:

- automatic order;
- automatic close;
- automatic reverse;
- automatic leverage change;
- automatic execution;
- executable readiness;
- executable point generation;
- executable entry;
- executable stop;
- executable TP;
- real order instruction;
- external push send.

## Boundary Notes

Allowed review-only output must not be hidden behind broad blocked language. If a request is unsafe for execution, it should still state whether a safer review-only downgrade exists.

Examples:

- `entry zone proposal` is allowed as review-only context; `place buy order` is not allowed.
- `tighten stop suggestion` is allowed as review-only risk context; `modify stop order` is not allowed.
- `internal push preview` is allowed; Telegram/email/webhook/app notification send is not allowed until separately authorized.
- `readiness review-only status` is allowed; executable readiness is not allowed.
- `point boundary unavailable reason` is allowed; fabricated entry / stop / TP / RR is not allowed.
- `source-owned review-only point proposal` is allowed only when it remains manual-review required, not a trade instruction, and incomplete-safe; executable entry / stop / TP / RR remains forbidden.
- `point proposal review-only display status` is allowed only when display values remain unavailable placeholders and cannot be interpreted as instructions.
- `executable point generation pre-approval status` is allowed only as docs-only or review-only gating context; it cannot authorize numeric generation, external send, order, execution, or auto-trading.
- `source-owned numeric point proposal plan status` is allowed only as docs-only planning context; it cannot create Java DTOs, generate numeric values, or authorize executable entry / stop / TP / RR.
- `SourceTrace numeric point contract status` is allowed only as docs-only planning context; it cannot create SourceTrace Java DTOs, generate numeric values, or authorize executable entry / stop / TP / RR.
- `RuntimeKlineContext numeric point contract status` is allowed only as docs-only planning context; it cannot create RuntimeKlineContext Java DTOs, generate numeric values, or authorize executable entry / stop / TP / RR.
- `DataQuality numeric point contract status` is allowed only as docs-only planning context; it cannot create DataQuality Java DTOs, generate numeric values, or authorize executable entry / stop / TP / RR.
- `MultiTimeframe numeric point contract status` is allowed only as docs-only planning context; it cannot create MultiTimeframe Java DTOs, generate numeric values, or authorize executable entry / stop / TP / RR.
- `Risk Action Guard numeric point contract status` is allowed only as docs-only planning context; it cannot create Risk Action Guard Java DTOs, generate numeric values, authorize executable entry / stop / TP / RR, or authorize external push, order, execution, or auto-trading.
- `Numeric Point Safety Validator plan status` is allowed only as docs-only planning context; it cannot create Safety Validator Java, generate numeric values, authorize executable entry / stop / TP / RR, or authorize external push, order, execution, or auto-trading.
- `Numeric Point Fixture Matrix plan status` is allowed only as docs-only planning context; it cannot create Java tests, generate numeric values, authorize executable entry / stop / TP / RR, or authorize external push, order, execution, or auto-trading.
- `ReviewOnlyNumericPointProposalDTO skeleton status` is allowed only as DTO/test skeleton context; it cannot validate, assemble, calculate, generate numeric values, authorize executable entry / stop / TP / RR, or authorize external push, order, execution, or auto-trading.
- `Numeric Point Safety Validator skeleton status` is allowed only as validator/test skeleton context; it cannot assemble, calculate, generate numeric values, authorize executable entry / stop / TP / RR, or authorize external push, order, execution, or auto-trading.
- `ReviewOnly Numeric Point Assembler skeleton status` is allowed only as explicit-input assembler/test skeleton context; it cannot calculate, infer, generate numeric values, authorize executable entry / stop / TP / RR, or authorize external push, order, execution, or auto-trading.
- `Source-owned Numeric Point Candidate Assembler plan status` is allowed only as docs-only planning context; it cannot create Java, read runtime providers, calculate numeric point values, authorize executable entry / stop / TP / RR, or authorize external push, order, execution, or auto-trading.
- `Source-owned Numeric Point Candidate Assembler skeleton status` is allowed only as source-owned-field assembler/test skeleton context; it cannot calculate, infer, generate numeric values, authorize executable entry / stop / TP / RR, or authorize external push, order, execution, or auto-trading.
- `Source-owned Numeric Point Candidate Assembler verification status` is allowed only as docs-only verification context; it cannot create Java, tests, services, dashboard runtime, source context integration, executable entry / stop / TP / RR, external push, order, execution, or auto-trading.
- `Source Context Integration Plan status` is allowed only as docs-only planning context; it cannot connect real SourceTrace, RuntimeKlineContext, DataQuality, MultiTimeframe, RiskActionGuard, WatchlistPoolProof, source-owned point contexts, services, dashboard runtime, executable entry / stop / TP / RR, external push, order, execution, or auto-trading.
- `SourceTrace Numeric Source Read Model Plan status` is allowed only as docs-only planning context; it cannot create SourceTrace Java DTOs, validators, assemblers, runtime reads, services, dashboard runtime, executable entry / stop / TP / RR, external push, order, execution, or auto-trading.
- `SourceTraceNumericSourceContextDTO skeleton status` is allowed only as DTO/test skeleton context; it cannot validate, assemble, read runtime sources, connect services, connect dashboard runtime, generate executable entry / stop / TP / RR, authorize external push, order, execution, or auto-trading.
- `SourceTraceNumericSourceReadModelValidator skeleton status` is allowed only as validator/test skeleton context; it cannot assemble, read runtime sources, connect RuntimeKlineContext, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, services, dashboard runtime, generate executable entry / stop / TP / RR, authorize external push, order, execution, or auto-trading.
- `SourceTrace Numeric Source Validator Verification status` is allowed only as docs-only verification context; it cannot create Java, tests, assemblers, read runtime sources, connect RuntimeKlineContext, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, WatchlistPoolProof, services, dashboard runtime, generate executable entry / stop / TP / RR, authorize external push, order, execution, or auto-trading.
- `SourceTraceNumericSourceReadModelAssembler skeleton status` is allowed only as explicit-input assembler/test skeleton context; it cannot read runtime sources, connect RuntimeKlineContext, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, WatchlistPoolProof, services, dashboard runtime, generate executable entry / stop / TP / RR, authorize external push, order, execution, or auto-trading.
- `SourceTrace Numeric Source Assembler Verification status` is allowed only as docs-only verification context; it cannot create Java, tests, read runtime sources, connect RuntimeKlineContext, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, WatchlistPoolProof, services, dashboard runtime, generate executable entry / stop / TP / RR, authorize external push, order, execution, or auto-trading.
- `SourceTrace Runtime / Source Binding Plan status` is allowed only as docs-only planning context; it cannot create Java, tests, read market data, read latest price, read latest close, read external providers, connect RuntimeKlineContext, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, WatchlistPoolProof, services, dashboard runtime, generate executable entry / stop / TP / RR, authorize external push, order, execution, or auto-trading.
- `SourceTrace Runtime / Source Binding Verification status` is allowed only as docs-only verification context; it cannot create Java, tests, read market data, read latest price, read latest close, read external providers, connect RuntimeKlineContext, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, WatchlistPoolProof, services, dashboard runtime, generate executable entry / stop / TP / RR, authorize external push, Push wiring, order, execution, or auto-trading.
- `RuntimeKlineContext Source Binding Plan status` is allowed only as docs-only planning context; it cannot create Java, tests, read market data, read latest price, read latest close, read external providers, connect RuntimeKlineContext runtime, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, WatchlistPoolProof, services, dashboard runtime, generate executable entry / stop / TP / RR, authorize external push, Push wiring, order, execution, or auto-trading.
- `RuntimeKlineContextSourceBindingDTO status` is allowed only as DTO/test carrier context; it cannot validate, assemble, read market data, read latest price, read latest close, read external providers, connect RuntimeKlineContext runtime, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, WatchlistPoolProof, services, dashboard runtime, generate executable entry / stop / TP / RR, authorize external push, Push wiring, order, execution, or auto-trading.
- `RuntimeKlineContextSourceBindingValidator skeleton status` is allowed only as validator/test skeleton context; it cannot assemble, read market data, read latest price, read latest close, read external providers, connect RuntimeKlineContext runtime, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, WatchlistPoolProof, services, dashboard runtime, generate executable entry / stop / TP / RR, authorize external push, Push wiring, order, execution, or auto-trading.
- `RuntimeKlineContextSourceBindingAssembler skeleton status` is allowed only as explicit-input assembler/test skeleton context; it cannot read market data, read latest price, read latest close, read external providers, connect RuntimeKlineContext runtime, DataQualityContext, MultiTimeframeContext, RiskActionGuardContext, WatchlistPoolProof, services, dashboard runtime, generate executable entry / stop / TP / RR, authorize external push, Push wiring, order, execution, or auto-trading.
- `RuntimeKlineContext Source Binding Verification status` is allowed only as closure documentation for the P335-P338 skeleton chain; it cannot create runtime wiring, source context integration, dashboard runtime, external channel, Push wiring, order, execution, or auto-trading.
- `DataQualityContext source binding DTO status` is allowed only as DTO/test carrier context; it cannot validate, assemble, read market data, connect DataQuality runtime, connect MultiTimeframeContext, connect RiskActionGuardContext, connect services, generate executable entry / stop / TP / RR, authorize external push, Push wiring, order, execution, or auto-trading.
- `DataQualityContext source binding validator result status` is allowed only as validator/test skeleton context; it cannot assemble, read runtime data, connect DataQuality runtime, connect MultiTimeframeContext, connect RiskActionGuardContext, connect services, generate executable entry / stop / TP / RR, authorize external push, Push wiring, order, execution, or auto-trading.
- `DataQualityContext source binding assembler validation result status` is allowed only as explicit-input assembler/test skeleton context; it cannot read runtime data, connect DataQuality runtime, connect MultiTimeframeContext, connect RiskActionGuardContext, connect services, generate executable entry / stop / TP / RR, authorize external push, Push wiring, order, execution, or auto-trading.
- `DataQualityContext Source Binding Verification status` is allowed only as closure documentation for the P339 skeleton chain; it cannot create runtime wiring, source context integration, dashboard runtime, external channel, Push wiring, order, execution, or auto-trading.
- `MultiTimeframeContext source binding DTO status` is allowed only as DTO/test carrier context; it cannot validate, assemble, read market data, connect MultiTimeframe runtime, connect RiskActionGuardContext, connect WatchlistPoolProof, connect services, generate executable entry / stop / TP / RR, produce final direction, authorize external push, Push wiring, order, execution, or auto-trading.
- `MultiTimeframeContext source binding validator result status` is allowed only as validator/test skeleton context; it cannot assemble, read runtime data, connect MultiTimeframe runtime, connect RiskActionGuardContext, connect WatchlistPoolProof, connect services, generate executable entry / stop / TP / RR, produce final direction, authorize external push, Push wiring, order, execution, or auto-trading.
- `MultiTimeframeContext source binding assembler validation result status` is allowed only as explicit-input assembler/test skeleton context; it cannot read runtime data, connect MultiTimeframe runtime, connect RiskActionGuardContext, connect WatchlistPoolProof, connect services, generate executable entry / stop / TP / RR, produce final direction, authorize external push, Push wiring, order, execution, or auto-trading.
- `MultiTimeframeContext Source Binding Verification status` is allowed only as closure documentation for the P340 skeleton chain; it cannot create runtime wiring, source context integration, dashboard runtime, external channel, Push wiring, order, execution, or auto-trading.
- `RiskActionGuard source binding DTO status` is allowed only as DTO/test carrier context; it cannot validate, assemble, read market data, connect RiskActionGuard runtime, connect WatchlistPoolProof, connect services, generate executable entry / stop / TP / RR, produce final direction, produce executable action output, authorize external push, Push wiring, order, execution, or auto-trading.
- `RiskActionGuard source binding validator result status` is allowed only as validator/test skeleton context; it cannot assemble, read runtime data, connect RiskActionGuard runtime, connect WatchlistPoolProof, connect services, generate executable entry / stop / TP / RR, produce final direction, produce executable action output, authorize external push, Push wiring, order, execution, or auto-trading.
- `RiskActionGuard source binding assembler validation result status` is allowed only as explicit-input assembler/test skeleton context; it cannot read runtime data, connect RiskActionGuard runtime, connect WatchlistPoolProof, connect services, generate executable entry / stop / TP / RR, produce final direction, produce executable action output, authorize external push, Push wiring, order, execution, or auto-trading.
- `RiskActionGuard Source Binding Verification status` is allowed only as closure documentation for the P341 skeleton chain; it cannot create runtime wiring, source context integration, dashboard runtime, executable action output, external channel, Push wiring, order, execution, or auto-trading.
- `WatchlistPoolProof source binding DTO status` is allowed only as DTO/test carrier context; it cannot validate, assemble, read watchlist runtime, connect Watchlist service, read rule config, read audit tables, connect source-owned candidate runtime, generate executable entry / stop / TP / RR, produce final direction, send push, authorize external push, order, execution, or auto-trading.
- `WatchlistPoolProof source binding validator result status` is allowed only as validator/test skeleton context; it cannot assemble, read watchlist runtime, connect Watchlist service, read rule config, read audit tables, connect source-owned candidate runtime, generate executable entry / stop / TP / RR, produce final direction, send push, authorize external push, order, execution, or auto-trading.
- `WatchlistPoolProof source binding assembler validation result status` is allowed only as explicit-input assembler/test skeleton context; it cannot read watchlist runtime, connect Watchlist service, read rule config, read audit tables, connect source-owned candidate runtime, generate executable entry / stop / TP / RR, produce final direction, send push, authorize external push, order, execution, or auto-trading.
- `WatchlistPoolProof Source Binding Verification status` is allowed only as closure documentation for the P342 skeleton chain; it cannot create runtime wiring, source context integration, dashboard runtime, push sending, external channel, order, execution, or auto-trading.
- `Source-Owned Candidate Integration Boundary Plan status` is allowed only as docs-only boundary planning context; it cannot create Java, tests, source-owned candidate runtime, source-owned candidate assembler integration, service wiring, dashboard runtime, real entry, real stop, real TP, RR generation, final direction, push sending, external channel, order, execution, or auto-trading.
- `source-owned candidate integration readiness status` is allowed only as future review-only planning output and must remain `notTradeInstruction=true` and `manualReviewRequired=true`; it cannot become executable candidate output, point generation, final direction, push payload, external channel message, order, execution, or auto-trading.
- `Source-Owned Candidate Integration Source Binding Plan status` is allowed only as docs-only source binding planning context; it cannot create DTO Java, validator Java, assembler Java, tests, source-owned candidate runtime, service wiring, dashboard runtime, real entry, real stop, real TP, RR generation, final direction, push sending, external channel, order, execution, or auto-trading.
- `source-owned candidate integration binding status` is allowed only as future review-only source binding output and must remain `notTradeInstruction=true` and `manualReviewRequired=true`; it cannot become candidate generation, executable point generation, final direction, push payload, external channel message, order, execution, or auto-trading.
- `SourceOwnedCandidateIntegrationSourceBindingDTO skeleton status` is allowed only as DTO/test carrier context; it cannot validate, assemble, connect source-owned candidate runtime, connect service wiring, dashboard runtime, real entry, real stop, real TP, RR generation, final direction, push sending, external channel, order, execution, or auto-trading.
- `SourceOwnedCandidateIntegrationSourceBindingValidator result status` is allowed only as validator/test skeleton context; it cannot assemble, connect source-owned candidate runtime, connect service wiring, dashboard runtime, real entry, real stop, real TP, RR generation, final direction, push sending, external channel, order, execution, or auto-trading.
- `SourceOwnedCandidateIntegrationSourceBindingValidator Verification status` is allowed only as docs-only verification context for the P345 DTO and P346 validator stages; it cannot create Java, tests, assembler, source-owned candidate runtime, service wiring, dashboard runtime, real entry, real stop, real TP, RR generation, final direction, push sending, external channel, order, execution, or auto-trading.
- `SourceOwnedCandidateIntegrationSourceBindingAssembler Plan status` is allowed only as docs-only assembler planning context; it cannot create Java, tests, assembler implementation, source-owned candidate runtime, service wiring, dashboard runtime, real entry, real stop, real TP, RR generation, final direction, push sending, external channel, order, execution, or auto-trading.
- `SourceOwnedCandidateIntegrationSourceBindingAssembler validation result status` is allowed only as explicit-input assembler/test skeleton context; it cannot connect source-owned candidate runtime, connect service wiring, dashboard runtime, real entry, real stop, real TP, RR generation, final direction, push sending, external channel, order, execution, or auto-trading.
- `SourceOwnedCandidateIntegrationSourceBindingAssembler Verification status` is allowed only as docs-only verification context for the P345 DTO, P346 validator, and P349 assembler stages; it cannot create Java, tests, source-owned candidate runtime, service wiring, dashboard runtime, real entry, real stop, real TP, RR generation, final direction, push sending, external channel, order, execution, or auto-trading.
- `Source-Owned Candidate Integration Runtime Boundary Plan status` is allowed only as docs-only runtime boundary planning context; it cannot create Java, tests, source-owned candidate runtime, service wiring, dashboard runtime, real entry, real stop, real TP, RR generation, final direction, push sending, external channel, order, execution, or auto-trading.
- `source-owned candidate runtime integration status` is allowed only as future review-only runtime status and must remain `notTradeInstruction=true` and `manualReviewRequired=true`; it cannot become point proposal generation, executable entry / stop / TP / RR, final direction, push payload, external channel message, order, execution, or auto-trading.
- `RuntimeKlineContext source binding validation result status` is allowed only as review-only validation context; it cannot become an entry, stop, TP, RR, direction, service signal, dashboard runtime output, external channel output, order, execution, or auto-trading action.
