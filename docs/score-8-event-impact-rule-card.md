# Score-8 Rule Card: Event Impact

## Purpose

This card explains how the `事件冲击分` (Score-8) is calculated, which inputs are used, what penalties are applied, and how the output description is rendered.

The goal is to provide one readable reference for product, troubleshooting, and parameter tuning.

## Scope and Boundaries

- This is a lightweight scoring rule under the frozen constraints.
- It does **not** change UI behavior.
- It does **not** change decision main-path logic.
- It only affects the Score-8 item in score list output.

## Inputs (Priority Order)

### 1) Contract Input (`eventImpactInput`)

When `eventImpactInput` exists, Score-8 uses it as the primary source:

- `eventFactHit`
- `eventFactCount`
- `eventLatestTime`
- `eventReasonCode`
- `eventTriggerType`
- `eventVersion`
- `eventTraceId`

### 2) Evidence Fallback (`evidenceList`)

When `eventImpactInput` is absent, the rule falls back to evidence scanning:

- If any evidence has type `事件` (trimmed match), it is treated as event hit.
- Otherwise, event is treated as miss.

## Scoring Formula

Start score:

- Base score = `50`

Penalties:

- If event hit, apply base penalty: `-10`
- If `eventFactCount >= EVENT_IMPACT_MULTI_HIT_THRESHOLD`, apply extra penalty:
  - `-EVENT_IMPACT_MULTI_HIT_EXTRA_PENALTY`
- If `eventTriggerType` is severe, apply extra penalty:
  - `-EVENT_IMPACT_SEVERE_TRIGGER_EXTRA_PENALTY`

Severe trigger type set:

- Defined by `EvidenceTypeConstants.EVENT_IMPACT_SEVERE_TRIGGER_TYPES`
- Current values:
  - `CIRCUIT_BREAKER`
  - `EXCHANGE_OUTAGE`
  - `LIQUIDATION_CASCADE`

Score clamp:

- Final score is clamped to `[0, 100]`

## Description Output Contract

Description is normalized with stable templates:

- With `eventImpactInput`:
  - Prefix: rule description text
  - Hit section: ordered hit markers (`eventFactHit`, `eventFactCount>=3`, `eventTriggerType=SEVERE`)
  - Ordered fields:
    - `eventFactCount`
    - `eventLatestTime`
    - `eventReasonCode`
    - `eventTriggerType`
    - `eventVersion`
    - `eventTraceId`
- Without `eventImpactInput`:
  - `eventEvidence=hit:-10` or `eventEvidence=miss:+0`

Missing/invalid value handling:

- `eventFactCount <= 0` or null is rendered as `0`
- null text-like fields are rendered as `NA`
- severe trigger match is case-insensitive and trim-aware

## Quick Examples

### Example A: Contract Hit + Multi-hit + Severe Trigger

- Input:
  - `eventFactHit=true`
  - `eventFactCount=4`
  - `eventTriggerType=LIQUIDATION_CASCADE`
- Score:
  - `50 - 10 - 5 - 5 = 30`

### Example B: Evidence Fallback Hit

- Input:
  - No `eventImpactInput`
  - evidence contains type `事件`
- Score:
  - `50 - 10 = 40`

## Regression Protection

Current regression coverage includes:

- Score boundary and branch behavior
- Description snapshot stability
- Realistic analysis input contract snapshots
- Evidence fallback snapshot stability

These tests guard against accidental score drift and output format drift.
