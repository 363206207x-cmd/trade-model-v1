# P267 Delivery Pipeline Java Authorization Gate

## 1. Future P268 Authorization

P268 may enter a delivery pipeline disabled no-op Java skeleton only if it is separately authorized by its own issue and PR.

P267 does not implement Java and does not authorize production delivery behavior.

## 2. P268 Allowed Starting Shape

If P268 is authorized, it may only start with:

- a minimal Java DTO / enum / interface / no-op policy or assembler shape
- disabled-by-default behavior
- fail-closed output
- audit-only semantics
- review-only semantics
- no-message semantics
- no-provider semantics
- targeted tests proving the disabled no-op boundary

## 3. P268 Must Not Do

P268 must not:

- connect Telegram
- connect email
- connect webhook
- connect app notification
- connect local notification
- send any message
- implement provider delivery
- activate scheduler behavior
- connect API / dashboard
- read runtime / live / external data
- implement queue runtime behavior
- enqueue or dequeue
- start a worker
- write schema / mapper / repository / DB
- upgrade Readiness
- generate point generation
- generate entry / stop / TP / RR
- create order / execution / auto-trading paths

## 4. Guard Rails

Risk Action Guard must be before delivery.

Watchlist Pool is the candidate boundary.

Display Slots / 默认六币 cannot be used as a batch universe.

P268 cannot interpret stampede, wick-only reversal, or strong reversal as permission to push, reverse, or trade.

## 5. Current Result

P267 only records this future Java authorization gate.

No Java is changed in P267.
