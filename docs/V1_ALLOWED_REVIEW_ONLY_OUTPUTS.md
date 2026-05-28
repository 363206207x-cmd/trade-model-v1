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
- confused with recovery condition.

## Forbidden Executable Outputs

The following remain forbidden unless a future explicitly authorized scope changes the project boundary:

- automatic order;
- automatic close;
- automatic reverse;
- automatic leverage change;
- automatic execution.

## Boundary Notes

Allowed review-only output must not be hidden behind broad blocked language. If a request is unsafe for execution, it should still state whether a safer review-only downgrade exists.

Examples:

- `entry zone proposal` is allowed as review-only context; `place buy order` is not allowed.
- `tighten stop suggestion` is allowed as review-only risk context; `modify stop order` is not allowed.
- `internal push preview` is allowed; Telegram/email/webhook/app notification send is not allowed until separately authorized.
