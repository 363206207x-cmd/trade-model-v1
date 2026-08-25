# TRINE LOGIC Multi-User Account Registration Authorization

Authorization package: `MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE`

Status: `AUTHORIZED_PENDING_MERGED_MAIN`

## Purpose

Authorize one bounded implementation package that extends the existing private
Session/BCrypt user owner from a single Owner account to private multi-user
registration and strict authenticated-user data isolation. This document grants
permission only; it contains no application, API, Schema, runtime or deployment
implementation.

## Frozen Account Contract

- The service remains private and Tailscale-only; public exposure stays zero.
- At most 10 active accounts may exist, including `xuchao`.
- `xuchao` remains the single `OWNER`; newly registered accounts are `USER`.
- Username is unique. Password length is 8 through 128 characters, with no
  additional composition rule.
- Authentication continues to use BCrypt, server-side Session and CSRF.
- Concurrent sessions are allowed.
- The Owner may force logout, disable and re-enable a `USER`.
- Accounts are never hard-deleted.
- Registration is available in the frontend only while the account limit and
  safety checks permit it.

## Frozen Ownership Contract

Data must be classified as `GLOBAL_SHARED`, `USER_OWNED` or `OWNER_ONLY`.

- Public market facts are `GLOBAL_SHARED`.
- Each user's watch pool, configuration, positions, analyses, plans, messages
  and audit records are `USER_OWNED` and must be resolved from the authenticated
  user, never a client-supplied substitute owner.
- Owner administration is `OWNER_ONLY`.
- Existing `tm_user.id=1` / `xuchao` is preserved. Existing personal records are
  assigned to user 1 by a forward migration only after a verified backup and
  rollback point exist.
- No second user, position, analysis, plan, message, audit or market-data owner
  may be introduced.

## Authorized Successor Scope

- Minimum compatible Schema and persistence ownership changes.
- Registration, account-limit and unique-username handling.
- OWNER/USER authorization and Owner account administration.
- Authenticated-user scoping across the frozen `USER_OWNED` object set.
- Session invalidation for force logout and disabled users.
- Frontend registration and account-administration bindings without changing
  the approved Home architecture.
- Migration, isolation, authorization, browser and rollback tests.
- Exact-head CI and private Staging validation after implementation review.

## Forbidden Scope

- Figma, Mobile, approved Home layout or product-semantic redesign.
- Telegram delivery-rule changes; new users start with Telegram disabled.
- CoinGlass work, new providers or production deployment.
- Public registration or public Staging exposure.
- Fake data, owner-scope fallback or cross-user aggregation of private facts.
- Automatic open, close, add, reduce, reverse or exchange-order behavior.
- Hard account deletion, a second Owner, or duplicate business skeletons.

## Migration and Rollback Contract

Before any ownership migration, the implementation must prove a readable
database backup and a rollback point. Migration must preserve the existing
Owner ID and all linked data, backfill user 1 only where the frozen ownership
map requires it, and fail closed for ambiguous ownership. No migration may
silently reassign one user's private facts to another.

## Effectivity

Before this authorization is merged to clean synchronized `main`, successor
implementation and implementation PR creation remain blocked. After merged-main
validation, only `MULTI_USER_ACCOUNT_REGISTRATION_CLOSURE` may resolve:

- `REPOSITORY_EDITS_ALLOWED=true`
- `IMPLEMENTATION_ALLOWED=true`
- `PR_CREATION_ALLOWED=true`
- `CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED=false`
- `MOBILE_IMPLEMENTATION_ALLOWED=false`

Wrong, expanded or misspelled package names fail closed. Capability movement in
this authorization package is none.
