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
| production readiness | Blocked | Readiness must not become executable trade readiness. |
| production point generation | Blocked | Real entry / stop / TP / RR production point generation is not complete. |
| production MarketQuoteClient scan-chain wiring | Blocked | Legacy market clients may not be treated as authorized scan-chain provider wiring. |
| production Push execution | Blocked | Internal previews and no-op/audit paths do not equal external push execution. |
| production Candidate workflow | Blocked | Review-only Candidate skeletons do not equal production candidate generation. |

## Interpretation Rule

Blocked does not mean the system can never produce a useful review-only proposal.

Blocked means the capability cannot automatically execute, externally send, mutate production state, or become production wiring without a separate authorization package.

For example:

- entry / stop / TP / RR proposals can be review-only outputs;
- risk downgrade suggestions can be review-only outputs;
- internal push previews can be review-only outputs;
- position-monitor suggestions can be review-only outputs.

Those outputs must remain manual-review required and not trade instructions.
