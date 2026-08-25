# TRINE LOGIC Multi-User Account Registration Implementation Report

Status: `IMPLEMENTATION_CANDIDATE_COMPLETE_PENDING_EXACT_HEAD_CI_AND_STAGING`

Date: 2026-08-25

Authorized package: `MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE`

Implementation baseline after authorization merge:
`0145038bca0713f0ad4dda490541e2533e7486d0`

Historical product-task baseline:
`4270dc5cdfdfc97e97fbb97ddfdd90c32314d1ea`

## Account Contract

- The existing `tm_user.id=1` / `xuchao` row remains the unique `OWNER`.
- Registration creates only enabled `USER` rows and never creates a second
  Owner.
- Usernames are trimmed, normalized to lower case, validated against the
  frozen 3-32 character policy and protected by a case-insensitive database
  unique index.
- Passwords are 8-128 characters and persisted only as BCrypt hashes.
- A singleton registration-guard row is locked inside the registration and
  re-enable transactions. The database limit and configured limit are both
  applied, with an effective maximum of 10 enabled accounts.
- Concurrent sessions are allowed. Disable, force logout and password changes
  increment the account session generation so earlier sessions fail closed.
- Owner administration can only target `USER` accounts. The canonical Owner
  cannot be disabled, re-enabled, force-logged-out by the account-management
  endpoints or created through public registration.

## Data Ownership

| Data family | Ownership | Implementation result |
|---|---|---|
| Provider instruments, OHLCV and market health | `GLOBAL_SHARED` | Existing global persistence and read paths remain shared |
| Account, credential, role and session generation | `USER_OWNED` with `OWNER_ONLY` administration | Existing `tm_user` owner extended; no replacement identity subsystem |
| User configuration and watch pool | `USER_OWNED` | New accounts receive isolated defaults; authenticated owner ID scopes reads and writes |
| UserPosition and Position Monitoring | `USER_OWNED` | Existing position owner retained; deterministic legacy null owners migrate to user 1 and cross-user reads fail closed |
| Analysis, opportunity, Candidate, Final and review history | `USER_OWNED` | Existing object families retained and queried through authenticated-user ownership |
| Messages and user-facing alert history | `USER_OWNED` | Existing message owner retained; new-user Telegram state remains unbound and disabled |
| Provider, AI and Telegram runtime configuration | `OWNER_ONLY` | Existing global configuration endpoints require the Owner role |

The implementation does not accept a client-supplied substitute owner for
private resources. Detail routes return the existing not-found/denied outcome
when an object does not belong to the authenticated user.

## Schema V15

`V15__private_multi_user_account_registration.sql`:

- extends `tm_user` with role, enabled state, session generation and audit
  timestamps;
- enforces exactly one canonical `xuchao` Owner through role/identity checks
  and a unique Owner slot;
- adds a case-insensitive username unique index;
- adds the locked ten-account registration guard;
- stores only SHA-256 hashes for single-use Owner password-setup tokens;
- assigns legacy `tm_user_position.user_id IS NULL` rows to the preserved
  Owner only after ambiguity checks, then makes the owner column non-null;
- materializes the existing system-default watch pool for each existing user;
- fails closed for ambiguous Owner identity, duplicate case-insensitive
  usernames, missing Owner ownership and active-account overflow.

No V1-V14 migration was modified. The standard release smoke now validates
V1-V15 and uses the canonical Owner identity expected by V15.

## Frontend And Security

- `/register` remains available while registration is enabled and capacity is
  available.
- `/me/security` changes only the authenticated user's password.
- `/me/accounts` and `/api/owner/**` are Owner-only.
- `/owner/password-setup` consumes a 15-minute single-use token whose plaintext
  value is never persisted.
- Form login, server-side Session, CSRF, session fixation protection and the
  existing rate-limit controls remain active.
- The approved Home module structure and Position/Plan/Three-AI semantics were
  not redesigned.

## Frozen Boundaries

- Figma and Mobile changes: `0`
- Automatic open/close/add/reduce/reverse/order capabilities added: `0`
- Telegram delivery-rule changes: `0`
- New provider or CoinGlass capability: `0`
- Production deployment: `NOT_EXECUTED`
- Staging deployment: `PENDING_EXACT_HEAD_CI`
