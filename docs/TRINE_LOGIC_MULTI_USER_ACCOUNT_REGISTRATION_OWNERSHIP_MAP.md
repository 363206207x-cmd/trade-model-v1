# TRINE LOGIC Multi-User Account Registration Ownership Map

| Data family | Ownership class | Authoritative owner | Cross-user rule |
|---|---|---|---|
| Provider instruments, OHLCV and public market evidence | `GLOBAL_SHARED` | Existing provider/market persistence owners | Readable by authenticated users; never duplicated per user |
| Account, credential, role, enabled state and sessions | `USER_OWNED` with OWNER administration | Existing `tm_user` and Spring Security owners | Self-read plus explicit OWNER-only administration |
| Watch pool and user configuration | `USER_OWNED` | Existing Asset Pool/UserConfig owners | Authenticated user only |
| UserPosition and Position Monitoring | `USER_OWNED` | Existing UserPosition/PositionMonitor owners | Authenticated user only; Plan remains distinct from Position |
| Analysis, Candidate and Final plan history | `USER_OWNED` | Existing analysis/decision-chain owners | Authenticated user only; no Candidate-as-Final fallback |
| Message and channel-delivery preferences/history | `USER_OWNED` | Existing Message/ChannelDelivery owners | Authenticated user only; new-user Telegram disabled |
| Audit records containing personal actions | `USER_OWNED`; administrative audit is `OWNER_ONLY` | Existing audit owners | Subject user and authorized Owner only |
| Account administration | `OWNER_ONLY` | Unique `xuchao` OWNER | USER cannot elevate role or administer peers |

Duplicate Skeleton Gate: PASS only when implementation extends these existing
owners and creates no replacement user, position, analysis, plan, message,
market-data or audit subsystem.

Owner-scope gate: every `USER_OWNED` write derives ownership from the
authenticated principal. Missing or ambiguous ownership fails closed.
