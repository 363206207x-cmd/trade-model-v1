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
