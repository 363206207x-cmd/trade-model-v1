# Fundamental AI v4.1 Dynamic Asset Ranking Implementation Report

Status: `IMPLEMENTATION_COMPLETE_PENDING_BACKEND_CAPABILITY_AUDIT`

Package: `FUNDAMENTAL_AI_V4_1_DECISION_CHAIN_IMPLEMENTATION`

PR: `#1177`

## Audit Result Before Implementation

| Check | Before | Result |
|---|---|---|
| Asset Pool supports more than six assets | Effective system defaults plus all active user overrides were returned without a six-item cap | `PASS` |
| Continuous analysis source | Spring runtime scheduler consumed `AssetPoolService.listScanSymbols()` | `PASS` |
| Home six-asset source | Home truncated Asset Pool focus order before reading decisions | `FAIL` |
| Opportunity priority ranking | No dedicated ranking projection existed | `FAIL` |
| Fixed Home fallback | Dashboard compatibility code could fill empty slots with a fixed six-symbol list | `FAIL` |

## Implemented Data Flow

`Asset Pool -> all effective user assets -> latest Analysis/Decision/Score/Final Plan -> exact Opportunity source gate -> Opportunity Priority Ranking -> Home Top 6 Projection -> Dashboard Home`

The ranking service reads the complete effective user Asset Pool. It does not
read `listFocusSymbols`, does not truncate pool order before ranking, and does
not introduce configured symbols that the user removed.

Every projected Home asset must have all of the following identities:

- persisted `assetId` from the effective Asset Pool;
- latest `analysisId` for the symbol;
- matching symbol and timeframe Opportunity state;
- Opportunity `lastAnalysisId` equal to the ranked Analysis;
- nonblank `opportunityId`.

Missing or mismatched provenance is excluded. Market price, a Decision row, or
a configured symbol alone cannot create a Home opportunity asset.

Plan Mode is read only from an Execution Plan where `final_plan=TRUE`, Rule
Validation is `PASS`, and chain status is `FINAL_VALIDATED` or
`RULE_FALLBACK_VALIDATED`. A newer Candidate, legacy, or Rule-blocked plan
cannot raise or replace a ranked asset's Final Plan Mode.

## Ranking Contract

The stable priority tuple is:

1. Rule-validated Final Plan Mode viability: `CONFIRM`, `PREPARE`, `REDUCE`,
   `WATCH`, `BLOCKED`;
2. Opportunity Score, descending;
3. confidence: `HIGH`, `MEDIUM`, `LOW`;
4. AI decision relationship: Conflict Level 1 through Level 4;
5. Data Quality, descending;
6. risk quality: `LOW`, `MEDIUM`, `HIGH`, `EXTREME`;
7. latest Decision time;
8. symbol as a deterministic final tie-breaker.

All frozen ranking inputs are present in the tuple. Missing values rank below
known values and are exposed as `MISSING` in `rankingReason`; they are never
replaced with fabricated values.

The service always caps the Home projection at six, even if a caller requests a
larger limit. If fewer than six sourced opportunities exist, it returns fewer
than six instead of creating placeholders.

## Home Projection Contract

`HomeTopAssetProjection` contains:

- `assetId`
- `symbol`
- `opportunityScore`
- `confidence`
- `riskLevel`
- `planMode`
- `aiDecisionResult`
- `dataQuality`
- `rankingReason`
- `analysisId`
- `opportunityId`
- `opportunityState`

`DashboardHomeVO.AssetVO` exposes the corresponding ranking and provenance
fields. `assetId` is serialized as a string to preserve JavaScript integer
identity.

The selected Home asset reuses the ranked Decision source for the existing
Three-AI and Execution Plan views. Asset Pool discovery remains the source of
the opportunity; Home does not create an opportunity.

## Fixed-Asset Removal

- the fixed six-symbol Dashboard fallback was removed;
- an empty ranked result remains an empty/fail-closed Home asset module;
- deleting default assets through a user override prevents them from returning
  through ranking;
- no Controller contains a fixed Top 6 symbol list.

Local-real readiness fixtures still contain bounded development symbols, but
they cannot create a Decision Chain Opportunity because
`DecisionChainServiceImpl` independently requires an active Asset Pool source.
They are not a Home projection source.

## Boundaries

- Figma changed: `NO`
- Mobile source/design changed: `NO`
- Position Monitoring changed: `NO`
- Three-AI authority changed: `NO`
- Execution Plan generation logic changed: `NO`
- automatic open/close/reverse/order capability added: `NO`
- schema changed by this increment: `NO`

## Delivery State

Implementation and local validation are complete. PR `#1177` remains open and
draft; this capability is not effective on merged main until independent
Backend Capability Audit, review, merge, and merged-main validation succeed.
