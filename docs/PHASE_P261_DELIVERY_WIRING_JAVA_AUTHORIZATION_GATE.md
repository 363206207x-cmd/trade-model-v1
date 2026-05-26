# P261 Delivery Wiring Java Authorization Gate

## 1. Authorization Result

P261 does not authorize Java implementation.

P261 does not authorize delivery wiring.

P261 only defines that a future P262 may enter delivery wiring Java skeleton after separate authorization.

## 2. P262 Minimum Preconditions

Future P262 must be separately authorized before any Java skeleton work starts.

Future P262 must explicitly name every allowed Java file.

Future P262 must remain disabled-by-default unless a later gate says otherwise.

Future P262 must not infer permission from P260 or P261.

## 3. Future Java Skeleton Limits

If separately authorized, future delivery wiring Java skeleton may only connect internal review-only output to:

- local queue
- audit-only envelope

This future skeleton must not:

- send messages
- connect external providers
- create scheduler behavior
- expose API endpoints
- wire dashboard behavior
- read runtime / live / external data
- create a scan loop
- implement external Opportunity Push execution
- implement Promote To Home runtime logic
- upgrade Readiness
- generate point generation
- generate entry / stop / TP / RR
- create order / execution / auto-trading paths

## 4. External Channel Block

Future P262 is not allowed to connect:

- Telegram
- email
- webhook
- app notification
- local notification

External providers require a later explicit provider/channel authorization gate.

## 5. Required Guards Before Any Delivery Surface

Risk Action Guard 必须位于 delivery 前。

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。

Display Slots / 默认六币不能作为 batch universe。

Watchlist Pool 才是推送候选边界。

## 6. Conclusion

P261 is a Java authorization gate document only.

P261 grants no implementation authority.
