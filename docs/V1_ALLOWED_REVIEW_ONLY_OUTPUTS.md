# V1 Allowed Review-Only Outputs

Review-only does not mean no-output.

Blocked does not mean no useful output.

Review-only means the system can show useful, structured, non-executable proposals to a human while staying:

- manual-review required;
- not a trade instruction;
- not an order;
- not executable by the system;
- blocked from external send unless a later package explicitly authorizes that channel.

## Allowed Review-Only Outputs

The following are allowed review-only outputs when source ownership, freshness, risk state, guard status, and not-trade-instruction flags are visible:

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
- confused with recovery condition.

## Forbidden Executable Outputs

The following remain forbidden:

- automatic order;
- automatic close;
- automatic reverse;
- automatic leverage change;
- automatic execution.

## Operational Rule

When a path is unsafe for execution, docs and code should still answer:

- Can a review-only proposal be shown?
- Can a downgrade suggestion be shown?
- Can a manual review state be shown?
- What is the recovery condition?

Do not replace useful review-only output with broad "blocked forever" language.

## Examples

- `entry zone proposal` is allowed; `place buy order` is forbidden.
- `tighten stop suggestion` is allowed; `modify stop order` is forbidden.
- `internal push preview` is allowed; Telegram/email/webhook/app notification send is forbidden.
- `confused with recovery condition` is allowed; infinite AI deadlock with no output is not useful.
