# Knife B.1 Browser Runtime Validation

Classification: `UI_REVIEW_FIXTURE`

Runtime: Java 17 standard release JAR, isolated in-memory database, `ui-review` profile, authenticated local user, schedulers/providers/AI/Telegram/auto-trading disabled.

## Responsive Results

| Viewport | Route | HTTP | Horizontal overflow | User-visible text clipping | Console errors |
|---|---|---:|---:|---:|---:|
| 1440 x 900 | `/dashboard` | 200 | 0 | 0 | 0 |
| 1080 x 900 | `/dashboard` | 200 | 0 | 0 | 0 |

The clipping scan excluded the intentional 1x1 screen-reader-only Search label. It found no visible clipped business value.

## Mode And Role Checks

- `/analysis/ui-review-preview-analysis`: missing formal audit-chain data failed closed; Candidate text count 0 and failure-path text count 0.
- The hidden auxiliary Failure Path and Before/After nodes each had zero client rectangles; no duplicate role output was visible outside the selected tab.
- Home Opportunity GPT retained Candidate-not-Final semantics.
- Home Opportunity Gemini displayed the formal review result. The isolated fixture did not provide `downgradeSuggestion.before/after`, so browser proof for those values is `NOT_VERIFIED_BROWSER_DATA_BOUNDARY`; structured automated tests cover their binding.
- Home Opportunity Grok displayed the complete trigger -> causal path -> invalidating evidence chain.

## Return Context Checks

Tested against the rendered `#analysisReturn` link:

| Input | Result |
|---|---|
| `/messages?group=position` | preserved |
| `https://evil.example/pwn` | `/dashboard` |
| `//evil.example/pwn` | `/dashboard` |
| `\\evil.example\pwn` | `/dashboard` |
| double-encoded `//evil.example/pwn` | `/dashboard` |

The Home audit action navigated to `/audit/ui-review-trace-btc-grok_challenge?returnTo=%2Fdashboard%3Fasset%3DBTCUSDT`. The isolated fixture has no persisted audit chain for that trace, and the destination correctly failed closed.

## Data Boundaries

- Four active Positions, CLOSED detail, owned Message/Recheck, ERROR Retry and F5 zero-write states are not persisted by the isolated UI-review profile. Those claims remain `AUTOMATED_TEST`, not browser or live evidence.
- No screenshot or fixture in this directory is live-provider evidence.
