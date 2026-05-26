# P271 Provider Channel Java Authorization Gate

## 1. Future P272 Authorization

P272 may enter a provider channel disabled no-op Java skeleton only if it is separately authorized by its own issue and PR.

P271 does not implement Java and does not authorize production provider behavior.

## 2. P272 Allowed Starting Shape

If P272 is authorized, it may only start with:

- a minimal Java DTO / enum / interface / no-op policy or assembler shape
- disabled-by-default behavior
- fail-closed output
- audit-only semantics
- review-only semantics
- no-message-sending semantics
- no-credential semantics
- no-live-provider-call semantics
- targeted tests proving the disabled no-op boundary

## 3. P272 Must Not Do

P272 must not:

- implement provider selection
- connect a real provider
- handle provider credentials
- call Telegram
- call email
- call webhook
- call app notification
- call local notification
- implement message rendering
- generate final send text
- send any message
- activate scheduler behavior
- connect API / dashboard
- read runtime / live / external data
- implement real delivery behavior
- implement external Opportunity Push execution
- upgrade Readiness
- generate point generation
- generate entry / stop / TP / RR
- create order / execution / auto-trading paths

## 4. Guard Rails

Risk Action Guard must be before delivery.

Watchlist Pool is the candidate boundary.

Display Slots / 默认六币 cannot be used as a batch universe.

P272 cannot interpret stampede, wick-only reversal, or strong reversal as permission to push, reverse, or trade.

## 5. Current Result

P271 only records this future Java authorization gate.

No Java is changed in P271.
