# P273 External Channel Java Authorization Gate

## 1. Future P274 Authorization

P274 may enter an external channel disabled no-op Java skeleton only if it is separately authorized by its own issue and PR.

P273 does not implement Java and does not authorize production external channel behavior.

## 2. P274 Allowed Starting Shape

If P274 is authorized, it may only start with:

- a minimal Java DTO / enum / interface / no-op policy or assembler shape
- disabled-by-default behavior
- fail-closed output
- audit-only semantics
- review-only semantics
- no-message-sending semantics
- no-credential semantics
- no-live-provider-call semantics
- targeted tests proving the disabled no-op boundary

## 3. P274 Must Not Do

P274 must not:

- connect Telegram
- connect email
- connect webhook
- connect app notification
- connect local notification
- implement real external channel behavior
- implement provider selection
- handle provider credentials
- make live provider calls
- implement message rendering
- generate final send text
- send any message
- activate scheduler behavior
- connect API / dashboard
- read runtime / live / external data
- implement external Opportunity Push execution
- upgrade Readiness
- generate point generation
- generate entry / stop / TP / RR
- create order / execution / auto-trading paths

## 4. Guard Rails

Risk Action Guard must be before delivery.

Watchlist Pool is the candidate boundary.

Display Slots / 默认六币 cannot be used as a batch universe.

P274 cannot interpret stampede, wick-only reversal, or strong reversal as permission to push, reverse, or trade.

## 5. Current Result

P273 only records this future Java authorization gate.

No Java is changed in P273.
