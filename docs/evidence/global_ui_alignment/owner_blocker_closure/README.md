# Residual P0 browser evidence

Scope: residual P0 closure on `codex/frontend-interaction-runtime-closure`.
Profile: `ui-review`, explicitly enabled and isolated from normal runtime data.
Browser surface available to this run: `1280 x 720`.

The normal profile was started separately with the standard Java 17 release
JAR. It returned authenticated `/dashboard` HTTP 200 and loaded
`home.html` (`data-figma-node="636:708"`) without a ui-review marker.

## Current captures

| Scenario | Evidence |
|---|---|
| Home full page, six status owners, six opportunities | `residual-p0-home-1280.png` |
| GPT first visual and Candidate-not-Final boundary | `residual-p0-gpt.png` |
| Gemini APPROVE / DOWNGRADE / REJECT / RISK_WARNING | `residual-p0-gemini-approve.png`, `residual-p0-gemini-downgrade.png`, `residual-p0-gemini-reject.png`, `residual-p0-gemini-risk-warning.png` |
| Gemini illegal waiting-trigger Before/After | `residual-p0-gemini-illegal-before-after.png` |
| Grok FOUND / NONE_FOUND / inconsistent FOUND-empty | `residual-p0-grok-found.png`, `residual-p0-grok-none-found.png`, `residual-p0-grok-found-empty.png` |
| Position VERIFIED / PENDING / STALE / INVALID / SOURCE_UNAVAILABLE | `residual-p0-position-verified.png`, `residual-p0-position-pending.png`, `residual-p0-position-stale.png`, `residual-p0-position-invalid.png`, `residual-p0-position-source-unavailable.png` |
| Existing manual-close detail boundary | `residual-p0-position-detail.png` |

The fixed in-app browser surface could not produce a new exact `1440 x 900`
capture. `home-1440-top.png` predates this residual package and is retained as
historical geometry evidence only; it is not cited as current-Head runtime
proof. The current 1280 capture already exercises the six-column container
rule (`>= 1240px`), and CSS/contract tests cover the same rule, but exact 1440
recapture remains an Owner-review evidence gap.

## Runtime results

- Position untrusted rows keep identity, direction, source, entry, opened time,
  and `/positions/7101`; mark price, PnL, judgment, conclusion, and action are
  absent. Exactly one trust-state region is rendered.
- Position sources render `系统计划` and `独立录入` through the shared mapper.
- Gemini renders one primary review result. APPROVE has no fabricated
  Before/After; illegal `CONFIRMATION -> PREPARATION` for `waiting_trigger`
  renders one unavailable strip without rewriting the fixture payload.
- Grok FOUND requires a complete trigger/evolution/invalidation path.
  FOUND with an empty list renders neither found nor not-found copy.
- Opportunity lifecycle and risk are independent: WAITING_TRIGGER/HIGH,
  HIGH_RISK/HIGH, and HIGH_RISK/EXTREME all render in their own slots.
- Selected BTC and ETH have different opportunity state/risk, while all six
  system-owned status values remain identical.
- No Home derivatives strip and no visible `Decision Workspace` remain.

Machine-readable evidence is in `residual-p0-browser-qa.json`.

## Final validation

- Java 17 directed tests: PASS.
- Java 17 full Maven: `4723` tests, `0` failures, `0` errors, `14` skipped.
- Product Source Gate: PASS.
- Workflow Contract: PASS.
- JavaScript syntax and `git diff --check`: PASS.
- Normal `/dashboard`: HTTP 200, run separately from `ui-review`.
- Isolated `ui-review` `/dashboard`: HTTP 200.
- Exact current-Head console error counter: NOT_VERIFIED because the browser
  log-inspection tab could not reuse the temporary runtime login session.
- Exact current-Head 1440 capture: NOT_VERIFIED; the available controlled
  browser surface remained `1280 x 720`.
