# Global UI Alignment Copy Inventory

Date: 2026-08-21
Scope: current Desktop Home and primary task pages
Excluded: login, logout, session, authentication, and account-security surfaces

## Method

The inventory counts unique, visible leaf-node strings after each route finishes its
UI-review runtime render at 1440 x 900. Counts are route-local and are not a claim
that shared shell labels are globally unique. The obsolete fixed inventory target
of 108 is not reused.

| Route | Visible copy count | Visible Telegram copy | Horizontal overflow | Text overflow |
|---|---:|---:|---:|---:|
| `/dashboard` | 176 | 0 | 0 | 0 |
| `/positions` | 28 | 0 | 0 | 0 |
| `/analysis` | 41 | 0 | 0 | 0 |
| `/messages` | 25 | 0 | 0 | 0 |
| `/me` | 49 | 0 | 0 | 0 |
| `/plans/ui-review-final-btc-001` | 26 | 0 | 0 | 0 |

Route-local visible occurrences total: **345**.

## Result

- Current UI Telegram copy: **0**.
- Auth/login copy included: **0**.
- Legacy Home copy branch included: **0**.
- The inventory reflects the current runtime instead of preserving an obsolete
  historical count.
