# Fundamental AI v4.1 Canonical Figma Authorization Scope Reconciliation

Status: `AUTHORIZATION_AMENDMENT_PENDING_MERGED_MAIN`

Date: `2026-08-14`

Base main: `707bb8d8527eba64e6b1a975a7a5bcc0e725173c`

Unchanged implementation candidate:

- PR: `#1179 / OPEN / DRAFT / UNMERGED`
- Head: `62ba9702e54b268ef27158bcff7e33422e23015e`
- Disposition: `REUSABLE_PENDING_AUTHORIZATION_RECONCILIATION`

## 1. Contract Gap and Resolution

| Contract Item | Before | Amendment | Validation |
|---|---|---|---|
| Exact package | Final interaction implementation package existed | package name remains unchanged | exact-package resolver |
| Canonical Figma | successor scope explicitly forbade Figma modification | only file `rdMYmsAvZYkXHJX8hdl7UN` is permitted for frozen Desktop implementation | exact Figma permission test |
| Visual contract | not present on merged main | frozen visual annex registered and hash-bound to the sole Product Source | Product Source and authorization gates |
| Position / Plan | historical `70:30` | `SUPERSEDED`; replacement `60:40`, allowed `58:42-62:38` | visual contract assertions |
| Mobile | forbidden | remains forbidden | machine permission test |
| PR #1179 history | earlier registered Head no longer described current candidate | current Head is recorded as pre-amendment reusable candidate | immutable Head check |

## 2. Exact Permission Boundary

After this amendment is merged and validated on clean, synchronized `main`,
only package
`FUNDAMENTAL_AI_V4_1_FINAL_INTERACTION_PAGE_AND_RUNTIME_IMPLEMENTATION`
may resolve:

```text
IMPLEMENTATION_ALLOWED: true
PR_CREATION_ALLOWED: true
CANONICAL_FIGMA_DESKTOP_IMPLEMENTATION_ALLOWED: true
MOBILE_IMPLEMENTATION_ALLOWED: false
CANONICAL_FIGMA_FILE_KEY: rdMYmsAvZYkXHJX8hdl7UN
```

Old, misspelled, broader and differently named packages resolve all positive
permissions to `false`. Mobile remains `false` for every request.

## 3. Canonical Figma Scope

Allowed in the successor package only:

- one Canonical Figma file;
- 14 Desktop routed pages;
- 11 overlays;
- 54 component families;
- 81 Desktop acceptance states;
- approved component/variant extension;
- Auto Layout and Variables;
- node-ID evidence;
- state/route/data-matched Figma/runtime comparison.

Still forbidden:

- another Figma file or Design System;
- Mobile frames, screenshots, code or navigation;
- static screenshots replacing editable layers;
- static Figma replacing runtime;
- fake market, AI, progress or acceptance data;
- duplicate business ownership;
- automatic trading or automatic position mutation.

## 4. Historical Candidate Rule

This amendment does not claim that PR #1179 Figma work was authorized when it
was created. It authorizes future continuation only after merged-main
effectivity. Head `62ba9702e54b268ef27158bcff7e33422e23015e` cannot receive final approval
or merge authorization. The branch must first synchronize with amended
`main`, preserve the protected assets, produce a new exact Head, and rerun all
contract, PostgreSQL V13, Figma mapping, browser and runtime validation.

## 5. Scope Evidence

This amendment changes Product Source references, visual/authorization
contracts, page-matrix status, delivery/machine state, gate tests and this
report only.

```text
APPLICATION_CODE_CHANGED: NO
API_CHANGED: NO
SCHEMA_CHANGED: NO
FIGMA_CHANGED: NO
MOBILE_CHANGED: NO
CAPABILITY_LEVEL_CHANGED: NO
```

No new business skeleton is created. Existing Cursor-era and PR #1179 assets
remain reusable evidence under the sole canonical owner map. This follows the
#830 duplicate-skeleton recommendation and connects no new service, runtime,
Dashboard or API capability.
