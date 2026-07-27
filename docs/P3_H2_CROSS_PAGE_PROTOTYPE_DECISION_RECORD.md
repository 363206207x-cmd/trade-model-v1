# P3-H2 Cross-Page Prototype Decision Record

## Decision Metadata

- Decision ID: `TMV1-P3-H2-20260727-001`
- Date: `2026-07-27`
- Status: `OPTION_A_AUTHORIZED_BASELINE_ACCEPTED`
- Scope: `FIGMA_PROTOTYPE_ONLY`
- Mainline reviewed: `e8bf2b66377cc2ef99c4aac2133d237e8d79bef0`
- Figma file: `Trade Model Design System`
- File key: `rdMYmsAvZYkXHJX8hdl7UN`

This record documents and accepts a Figma prototype limitation under Option A.
It does not change product semantics, frontend or backend behavior, APIs, data
contracts, or delivery phase completion.

## Option A Authorization

```text
OPTION_A_AUTHORIZED: YES

FIGMA_BASELINE_ACCEPTANCE:
PASS_WITH_ACCEPTED_PROTOTYPE_LIMITATION

FE03_IMPLEMENTATION_ALLOWED:
YES
```

Option A makes the canonical Figma Page, Frame, node identities, Interaction
Contract, and API Contract the FE-03 development baseline. A clickable
cross-page Prototype reaction is not an FE-03 implementation gate.

This authorization:

1. accepts the current Codex-to-Figma MCP cross-page navigation limitation;
2. preserves Page `250:2`, Frame `250:4`, Frame `154:825`, and node `262:931`;
3. preserves the current Figma Page structure;
4. does not authorize a same-page duplicate or Frame relocation; and
5. allows FE-03 implementation to begin under the existing interaction, API,
   fail-closed, AI-role, and trading-safety contracts.

`FE03_IMPLEMENTATION_ALLOWED: YES` is permission to start the bounded
implementation package. It is not evidence that FE-03 is implemented, tested,
merged, effective on main, or complete.

## Canonical Nodes

| Role | Page | Node | ID |
| --- | --- | --- | --- |
| Source page | `01 V1 Product UI` | Page | `108:2` |
| Source frame | `01 V1 Product UI` | `Mobile Asset Detail / iPhone 17 Pro Max` | `154:825` |
| Forward entry | `01 V1 Product UI` | `Analysis Detail Entry / 查看分析详情` | `262:931` |
| Destination page | `FE-03 Analysis Detail` | Page | `250:2` |
| Destination frame | `FE-03 Analysis Detail` | `Analysis Detail / iPhone 17 Pro Max` | `250:4` |
| Back entry | `FE-03 Analysis Detail` | `Back to Asset Detail` | `252:3` |

The source and destination frames are intentionally preserved as the current
canonical visual baseline. This decision does not authorize moving, copying,
renaming, replacing, or deleting any of these nodes.

## Observed Evidence

The read-only Prototype gate established:

1. All canonical nodes existed with the expected names and parent
   relationships.
2. Entry `262:931` was visible, contained the formal `查看分析详情` label, and
   had zero Prototype reactions.
3. Back node `252:3` had one `ON_CLICK -> BACK` reaction.
4. The AI component set retained exactly three roles:
   `GPT_FINAL`, `GEMINI_REVIEW`, and `GROK_CHALLENGE`.
5. AI role switching used the existing `CHANGE_TO` component variants.
6. The required states were readable:
   `NORMAL`, `PARTIAL`, `ANALYSIS_NOT_FOUND`, `LOAD_FAILED`,
   `AI_TRACE_UNAVAILABLE`, and `MULTI_TIMEFRAME_UNAVAILABLE`.
7. No unsupported trading, complete-evidence, complete-score, hidden-reasoning,
   or reanalysis interaction was found.

One minimal write was attempted after the identity gate passed:

```text
trigger: ON_CLICK
action: NAVIGATE
destination: 250:4
transition: INSTANT
```

The Figma MCP operation returned `INVALID_ARGUMENT`. The write operation was
atomic, so no reaction or other Figma mutation was committed.

This is evidence that the exact cross-page operation is unsupported through
the current Codex-to-Figma MCP path and current file context. It is not a
universal claim about every Figma client, plugin, or future API version.

## Product Limitation

The formal Asset Detail entry and Analysis Detail destination reside on
different Figma Pages. In the currently verified tooling path, the canonical
entry cannot be connected to the canonical destination with the required
internal `NAVIGATE` reaction.

The limitation affects clickable prototype continuity only. It does not mean:

- the Analysis Detail product concept is invalid;
- the backend `analysisId` contract is unavailable;
- a frontend route already exists;
- a runtime route may be inferred from Figma node IDs;
- missing evidence, scores, timeframe data, or AI provenance may be simulated;
- FE-03 has already been implemented or completed.

## Decision

1. Keep the existing Pages, Frames, node identities, visual content, back
   action, AI variants, and fail-closed states unchanged.
2. Do not create a same-page duplicate, proxy frame, external URL workaround,
   overlay, or replacement entry.
3. Keep the forward Prototype status as
   `UNAVAILABLE_ACCEPTED_CROSS_PAGE_LIMITATION`.
4. Record
   `FIGMA_BASELINE_ACCEPTANCE: PASS_WITH_ACCEPTED_PROTOTYPE_LIMITATION`.
5. Record `FIGMA_ANALYSIS_DETAIL_BASELINE: READY`.
6. Record `FE03_IMPLEMENTATION_ALLOWED: YES`.
7. Require separate explicit authorization before changing Figma structure.

The current task provides Option A acceptance authorization only. It does not
authorize Option B, Option C, or any Figma mutation.

## Prototype And Development Contract

Figma is a visual and interaction specification. It is not proof of runtime
capability. The following boundaries remain frozen:

| Concern | Contract |
| --- | --- |
| Figma entry | Describes the intended transition from Asset Detail to Analysis Detail |
| Runtime navigation | Must be implemented separately in the bounded FE-03 package; this record does not implement it |
| Navigation identity | Runtime navigation uses the authoritative nullable `analysisId`, never a Figma node ID |
| Missing `analysisId` | Fail closed; do not fabricate a destination or select a sibling analysis |
| Missing evidence/scores/timeframes | Show the frozen unavailable states; do not generate complete data |
| AI display | Exactly three advisory roles; no voting, hidden reasoning, or rule-layer override |
| Execution behavior | No buy, sell, order, execution, auto-open, auto-close, or auto-reverse action |
| UserPosition | Must remain separate from Execution Plan |

The missing Figma reaction neither weakens nor expands these rules.

## Authorization Decision

```text
OPTION_A_AUTHORIZED: YES
STRUCTURAL_ADJUSTMENT_AUTHORIZED: NO
SEPARATE_STRUCTURAL_AUTHORIZATION_REQUIRED: YES
```

Option A is selected. The other paths remain unauthorized:

### Option A - Accepted Baseline Contract

Keep the current Figma structure and accept a documentation-backed intended
navigation without a clickable cross-page Prototype reaction.

This authorized decision removes the Prototype reaction from the FE-03
implementation gate. The intended navigation remains defined by the canonical
frames, Interaction Contract, and API Contract.

### Option B - Canonical Structure Realignment - Not Authorized

Relocate the canonical destination into a Prototype-compatible Page and update
the formal baseline identity.

This is a Figma structure change. Authorization must define:

- the canonical Page and Frame after the move;
- whether existing node IDs may change;
- the exact nodes allowed to move;
- how existing references are migrated;
- preservation of the back action, AI variants, and fail-closed states;
- confirmation that no duplicate canonical frame remains.

### Option C - Canonical Same-Page Replacement - Not Authorized

Create a new same-page canonical destination only as part of an explicitly
approved replacement and deprecation plan.

This option is not recommended because it creates the highest risk of duplicate
visual baselines and semantic drift. It must not be used as an informal
workaround.

## Authorized Outcome

Option A is now authoritative for P3-H2. It preserves one canonical Analysis
Detail frame and avoids node duplication.

Use Option B only after new authorization if stakeholders later require a fully
clickable Figma flow. Do not use Option C without a formal replacement and
deprecation plan.

## Baseline Acceptance

The baseline evidence is accepted because:

1. all canonical node identities were verified;
2. the intended forward destination is explicitly frozen as Frame `250:4`;
3. the `BACK` return action passed;
4. all three AI role variants passed;
5. all six fail-closed states passed;
6. no unsupported interaction was present;
7. the cross-page Prototype limitation is explicitly accepted; and
8. no Figma structure or business contract changed.

The accepted result is:

```text
FIGMA_BASELINE_ACCEPTANCE:
PASS_WITH_ACCEPTED_PROTOTYPE_LIMITATION

FIGMA_ANALYSIS_DETAIL_BASELINE:
READY

PROTOTYPE_REQUIRED_FOR_FE03:
NO

FE03_IMPLEMENTATION_ALLOWED:
YES
```

## Explicit Non-Goals

- No Figma mutation.
- No page, frame, component, variable, or Prototype change.
- No frontend or backend implementation.
- No API or schema change.
- No FE-03 or FE-04 work.
- No PR creation or merge.
- No product capability completion claim.
- No automatic trading capability.

## Final Record

```text
CROSS_PAGE_PROTOTYPE_STATUS:
UNSUPPORTED_IN_CURRENT_MCP_FILE_CONTEXT_ACCEPTED

STRUCTURAL_ADJUSTMENT_AUTHORIZED:
NO

OPTION_A_AUTHORIZED:
YES

SEPARATE_STRUCTURAL_AUTHORIZATION_REQUIRED:
YES

FIGMA_BASELINE_ACCEPTANCE:
PASS_WITH_ACCEPTED_PROTOTYPE_LIMITATION

FIGMA_ANALYSIS_DETAIL_BASELINE:
READY

PROTOTYPE_REQUIRED_FOR_FE03:
NO

FE03_IMPLEMENTATION_ALLOWED:
YES
```
