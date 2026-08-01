# Trade Model V1 Product Relation Graph

Status: `P0_PRODUCT_BASELINE_FREEZE_CANDIDATE`

This document freezes business relationships. It is not a UI diagram, code architecture, execution engine, or completion claim. Authority: `docs/PRODUCT_SOURCE_OF_TRUTH.md`.

## 1. End-to-End Product Chain

```mermaid
flowchart TD
    MD["Real Market Data"] --> RE["Raw Evidence"]
    RE --> ES["Evidence Standardization"]
    ES --> SC["Eight Scores"]
    SC --> DQ["Data Quality Gate"]
    DQ --> RULE["Rule Engine"]
    RULE --> MTF["Multi-Timeframe Convergence"]
    MTF --> BASE["Base Decision: direction, confidence, risk"]
    BASE --> AIG["AI Trigger Gate"]
    AIG -->|"triggered checkpoint"| GPT["GPT Final"]
    AIG -->|"triggered checkpoint"| GEM["Gemini Review"]
    AIG -->|"triggered checkpoint"| GROK["Grok Challenge"]
    AIG -->|"not triggered or AI unavailable"| FALLBACK["Rule-chain Fallback"]
    GPT --> CONFLICT["Conflict Adjustment"]
    GEM --> CONFLICT
    GROK --> CONFLICT
    BASE --> CONFLICT
    FALLBACK --> FINAL["Final Decision"]
    CONFLICT --> FINAL
    FINAL --> PLAN["ExecutionPlan"]
    FINAL --> AS["AssetState"]
    PLAN --> HOME["Home / Analysis Read-only Presentation"]
    AS --> HOME
    HOME --> USER["Explicit Authenticated User Action"]
    USER --> POS["UserPosition"]
    PLAN -. "reference only" .-> POS
    POS --> MON["PositionMonitor"]
    MD --> MON
    RE --> MON
    MON --> RISKMSG["POSITION_RISK Message"]
    FINAL --> OPPMSG["OPPORTUNITY Message"]
    OPPMSG --> RECHECK["Push Recheck"]
    RISKMSG --> RECHECK
    PLAN --> REPLAY["Replay / Review"]
    POS --> REPLAY
    MON --> REPLAY
    RECHECK --> REPLAY
    REPLAY --> RULEVER["Rule Version Iteration Evidence"]
    RULEVER -. "human-reviewed iteration" .-> RULE
```

The chain stops at advice and state. No node in this graph automatically opens, closes, reduces, adds, reverses, or executes a trade.

## 2. Rule Layer and AI Authority

```mermaid
flowchart LR
    INPUT["Evidence + Scores + Data Quality"] --> RULE["Rule-layer Base Decision"]
    RULE --> GATE{"AI checkpoint required?"}
    GATE -->|"No"| FINAL["Rule-chain Final Decision"]
    GATE -->|"Yes"| ROLES["GPT Final + Gemini Review + Grok Challenge"]
    ROLES --> ADJUST["Explain / challenge / downgrade / conflict classify"]
    RULE --> ADJUST
    ADJUST --> FINAL2["Final Decision constrained by rule base"]
```

- The rule layer owns the base direction and state-machine boundary.
- GPT synthesizes the rule-led result; Gemini reviews conflicts and downgrade needs; Grok supplies counter-evidence and event or microstructure challenge.
- The roles are not three equal voters. No majority vote replaces the rule base.
- AI may reduce confidence, explain conflict, or enter the defined conflict path. It cannot create a direction forbidden by the rule/state contract.
- AI failure uses the documented fallback. It cannot fabricate a result or stop price-safety monitoring of a real manual position.

## 3. Plan, Opportunity, and Real Position Separation

```mermaid
flowchart TD
    DEC["Final Decision"] --> PLAN["ExecutionPlan: system advice"]
    DEC --> ASTATE["AssetState: opportunity lifecycle"]
    ASTATE --> TRIG["TRIGGERED: condition matched"]
    PLAN --> PRESENT["Read-only presentation"]
    TRIG --> PRESENT
    PRESENT --> CHOICE{"User explicitly acts?"}
    CHOICE -->|"No"| NONE["No UserPosition"]
    CHOICE -->|"Yes, authenticated manual input"| POSITION["UserPosition: user fact"]
    PLAN -. "source/reference link" .-> POSITION
```

Frozen separation:

- `triggered` is an opportunity condition, not an order and not an opened position.
- `ExecutionPlan` is advice, not a position record.
- `tm_real_position` or exchange-like data cannot silently substitute for `UserPosition`.
- Only an explicit authenticated user action may create or update a user-held position fact.
- A plan may be linked as historical context, but user-entered price, quantity, leverage, stop, target, and time remain distinct facts.

## 4. Asset State and User Position State

```mermaid
flowchart LR
    subgraph AssetDomain["Public / market opportunity domain"]
        OBS["OBSERVING"] --> CAND["CANDIDATE"]
        CAND --> TRG["TRIGGERED"]
        CAND --> COOL["COOLING"]
        TRG --> EXP["EXPIRED / INVALIDATED"]
    end
    subgraph PositionDomain["Private user position domain"]
        OPEN["OPEN"] --> PART["PARTIALLY_CLOSED"]
        OPEN --> CLOSED["CLOSED"]
        PART --> CLOSED
    end
    TRG -. "never automatic" .-> OPEN
```

The dotted relationship is a prohibited automatic transition. A real user action is required between the domains. Asset selection on Home cannot rebind, create, or alter a selected UserPosition.

## 5. Position Monitor and Push Recheck Separation

```mermaid
flowchart TD
    POS["Owner-scoped UserPosition"] --> PM["PositionMonitor"]
    PLAN["Original ExecutionPlan"] --> PM
    LIVE["Current Market Evidence"] --> PM
    PM --> LOGIC["Logic valid / weakened / invalidated"]
    PM --> REV["No / weak / strong reversal"]
    PM --> RISK["Risk + manual adjustment suggestion"]
    PM --> LOG["PositionMonitorLog"]
    MSG["Immutable message context"] --> PR["Push Recheck"]
    CURRENT["Current public or owner-scoped evidence"] --> PR
    PR --> CHANGE["Validity / status / change reason"]
```

| Boundary | PositionMonitor | Push Recheck |
|---|---|---|
| Primary identity | exact `positionId` | exact public message identity or authorized private message identity |
| Purpose | continuously validate a real user's original position logic | re-evaluate the context of a previously surfaced message |
| Private data | owner-scoped position facts and risk | only when source is `POSITION_RISK` and ownership passes |
| Mutation authority | none | none |
| Trading authority | none | none |
| Output | monitor state, reason, risk, suggestion, log | current recheck state, snapshot comparison, change reason |

## 6. Message Domain Separation

```mermaid
flowchart LR
    OPPSRC["Public Opportunity Decision"] --> OPP["OPPORTUNITY public projection"]
    POS["Owner-scoped UserPosition"] --> MON["PositionMonitor"]
    MON --> PRISK["POSITION_RISK private projection"]
    OPP --> CENTER["Message Center"]
    PRISK --> CENTER
    CENTER --> DETAIL["Source-specific Push Detail"]
    DETAIL -. "optional future outlet only" .-> TG["Telegram"]
```

- `OPPORTUNITY` is authenticated shared public opportunity data. It contains no UserPosition, account risk, private position risk, private reason, private push identity, or private Recheck reference.
- `POSITION_RISK` is exact owner-scoped private data.
- The two projections must not be carried by one mixed public/private DTO.
- Telegram is an optional future delivery outlet, not a Message Center source, state, or product completion requirement in the current baseline.

## 7. Confused, Hot Reset, and Recovery

```mermaid
flowchart TD
    NORMAL["Normal rule-led lifecycle"] --> CHECK{"Material defined conflict?"}
    CHECK -->|"No"| NORMAL2["Continue normal state"]
    CHECK -->|"Yes"| CONF["CONFUSED"]
    CONF --> RESET["Hot Reset: clear volatile context, preserve audit"]
    RESET --> REC["Recovery and fresh evidence"]
    REC --> OBS["OBSERVING / CANDIDATE / COOLING"]
    CONF -. "forbidden" .-> TRG["TRIGGERED"]
```

Confused is not a synonym for low data quality, empty data, or ordinary observation. Recovery must use fresh evidence and the defined state path; it cannot jump directly to triggered.

## 8. Replay and Rule Iteration

Replay joins the original evidence snapshot, rule version, decision, plan, user action, position facts, monitor history, messages, Recheck results, and actual outcome. It produces review evidence such as executed-valid, executed-invalid, missed-valid, missed-invalid, pushed-not-filled-valid, or blocked-by-risk-valid. Rule changes remain human-reviewed iteration; Replay never mutates live trading state by itself.

## 9. Non-Driving Relationships

The following relationships are explicitly forbidden:

- Tests, Workflow, Governance, or PR metadata driving product semantics.
- AI output bypassing data-quality or rule-layer direction.
- an AssetState transition creating a UserPosition.
- an ExecutionPlan creating or mutating a UserPosition.
- Push Recheck opening, closing, reducing, adding, or reversing a position.
- PositionMonitor automatically closing or reversing a position.
- public OPPORTUNITY identity granting access to private PushRecheck or POSITION_RISK data.
- Home asset selection changing a position identity.
- examples, fallback values, or local fixtures appearing as real production data.
