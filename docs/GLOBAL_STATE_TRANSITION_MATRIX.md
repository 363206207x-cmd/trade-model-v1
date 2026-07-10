# Global State Transition Matrix

## Asset State

| State | Runtime entry/exit reality | Classification |
|---|---|---|
| `OBSERVING` | Decision default; exits on candidate, risk, confused, or invalidation | `IMPLEMENTED_AND_TRACED` |
| `CANDIDATE` | Rule result worth opening; exits on later risk/confused/invalidation | `IMPLEMENTED_AND_TRACED` |
| `WAITING_TRIGGER` | No normal production writer found; enum/schema/UI label only | `NOT_IMPLEMENTED` |
| `TRIGGERED` | No normal production writer found; enum/schema/UI label only | `NOT_IMPLEMENTED` |
| `HIGH_RISK` | Decision/Hot Reset risk gate; recovers through later analysis | `IMPLEMENTED_AND_TRACED` |
| `INVALIDATED` | Mainly Hot Reset invalidation; requires reanalysis | `IMPLEMENTED_AND_TRACED` |
| `COOLING` | Confused recovery/post-event cooling; later returns to observing | `IMPLEMENTED_AND_TRACED` |
| `CONFUSED` | Confused score threshold or fail-closed read error; two recovery cycles exit | `IMPLEMENTED_AND_TRACED` |

`AssetStateService.persistAuthoritativeState` does not validate a central legal transition graph. Enum completeness is not lifecycle proof; transition enforcement is `NOT_IMPLEMENTED`.

## Confused State

| Transition/contract | Reality | Classification |
|---|---|---|
| normal -> `CONFUSED` | weighted conflict/divergence score reaches fixed threshold | `IMPLEMENTED_AND_TRACED` |
| read error -> `CONFUSED` | prior-state read fails closed | `IMPLEMENTED_AND_TRACED` |
| `CONFUSED` -> `COOLING` | below exit threshold for two consecutive cycles | `IMPLEMENTED_AND_TRACED` |
| config -> thresholds | `confused_state_config` exists but is not consumed by policy | `BACKEND_FIELD_UNUSED` |
| real inputs -> score | several inputs are fixed heuristics | `SEMANTIC_DRIFT` |

## Plan, Position, and Monitor

| State/condition | Runtime behavior | Classification |
|---|---|---|
| complete execution boundary | requires primary timeframe plus real entry, stop, and TP trace | `BLOCKED_NO_REAL_DATA` |
| boundary incomplete | persists fail-closed flags; Home shows compact empty state | `IMPLEMENTED_AND_TRACED` |
| invalidated/expired plan | requires reanalysis; never creates a position/order | `IMPLEMENTED_AND_TRACED` |
| fallback leverage/position | generic strings persist without source trace | `PLACEHOLDER_ONLY` |
| UserPosition `OPEN` | explicit manual-open; eligible for monitor/manual close | `IMPLEMENTED_AND_TRACED` |
| `PARTIALLY_CLOSED` | active model/import state; eligible for monitor/manual close | `IMPLEMENTED_AND_TRACED` |
| `CLOSED` | explicit manual-close; excluded from Home and enters review source | `IMPLEMENTED_AND_TRACED` |
| position source provenance | persisted source is lost because VO hardcodes `MANUAL` | `WRONG_SOURCE_MAPPING` |
| monitor `LOGIC_VALID` | plan/direction support remains valid | `IMPLEMENTED_AND_TRACED` |
| `LOGIC_WEAKENED` | support weakens; manual review wording | `IMPLEMENTED_AND_TRACED` |
| `PLAN_INVALIDATED` | authoritative invalidation; recheck/manual review wording | `IMPLEMENTED_AND_TRACED` |
| `HIGH_RISK` | account/market/confused/external risk; manual risk review | `IMPLEMENTED_AND_TRACED` |

Position Monitor writes logs only and never changes the UserPosition state.

## Push Recheck

| Status | Meaning | Classification |
|---|---|---|
| `REVIEW_WAITING` | insufficient conditions; review-only | `IMPLEMENTED_AND_TRACED` |
| `REVIEW_PASSED` | review passed but never executable | `IMPLEMENTED_AND_TRACED` |
| `DRIFTED_FROM_ENTRY_ZONE` | quote drifted outside entry zone | `IMPLEMENTED_AND_TRACED` |
| deprecated `DRIFTED` | legacy alias canonicalized by helper | `DEAD_OR_LEGACY_CODE` |
| `INVALIDATED` | plan/price/quote condition invalid | `IMPLEMENTED_AND_TRACED` |
| `RISK_BLOCKED` / `CONFUSED_BLOCKED` | risk or confused gate blocks | `IMPLEMENTED_AND_TRACED` |
| `EXPIRED` | validity ended | `IMPLEMENTED_AND_TRACED` |

Missing caller price uses `MarketQuoteClient`; empty, null, non-positive, or failed quotes produce `QUOTE_UNAVAILABLE`/`PRICE_REQUIRED` and fail closed.

## Hot Reset

| Phase | Reality | Classification |
|---|---|---|
| detect/claim | RuleConfig thresholds and event-key idempotency | `IMPLEMENTED_AND_TRACED` |
| invalidate | state, decision, plan, and push review data updated transactionally | `IMPLEMENTED_AND_TRACED` |
| rebuild | analysis requested after commit | `IMPLEMENTED_AND_TRACED` |
| legacy overload | fixed-threshold helper has no production caller found | `DEAD_OR_LEGACY_CODE` |

## Opportunity and Review

| State/source | Meaning | Classification |
|---|---|---|
| candidate/pending | worth-opening decision awaits explicit evaluation | `IMPLEMENTED_AND_TRACED` |
| `EXECUTED_VALID` / `EXECUTED_INVALID` | linked manual execution, final outcome | `IMPLEMENTED_AND_TRACED` |
| `MISSED_VALID` / `MISSED_INVALID` | no execution, final outcome | `IMPLEMENTED_AND_TRACED` |
| `PUSHED_NOT_FILLED_VALID` | pushed, not filled, later valid | `IMPLEMENTED_AND_TRACED` |
| `BLOCKED_BY_RISK_VALID` | risk-blocked, later valid | `IMPLEMENTED_AND_TRACED` |
| outcome evaluation source | persisted OHLCV target/invalidation ordering | `BLOCKED_NO_REAL_DATA` |
| legacy missed-opportunity path | old producer is frozen/no-op | `DEAD_OR_LEGACY_CODE` |
| Review Center projection | positions, OpportunityLog, push/recheck, review/rule logs | `IMPLEMENTED_AND_TRACED` |
| review save | explicit request writes result and rule-version log | `IMPLEMENTED_AND_TRACED` |
| automatic rule mutation | deliberately absent | `NOT_IMPLEMENTED` |
| feedback processing state | no authoritative workflow field | `NOT_IMPLEMENTED` |

## Illegal/Unproven Transition Register

| Gap | Classification |
|---|---|
| No central legal transition table for all eight asset states | `NOT_IMPLEMENTED` |
| `WAITING_TRIGGER` and `TRIGGERED` lack normal writers | `NOT_IMPLEMENTED` |
| Non-manual position provenance can become `MANUAL` | `WRONG_SOURCE_MAPPING` |
| Opportunity terminal evaluation has no scheduled real-data path | `BLOCKED_NO_REAL_DATA` |
| External-context absence can enter healthy `READY` | `SEMANTIC_DRIFT` |
| Write schedulers outside analysis lack proven distributed claims | `MISSING_TEST_COVERAGE` |
