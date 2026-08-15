# Fundamental AI v4.1 Execution Plan Semantic Remediation

## Scope

This remediation updates PR `#1179` Desktop Home presentation only. It does not change Backend business logic, API, Schema, Figma, Mobile, or automatic-trading capability.

Starting Head: `3c3f7e96a8f384bbac1f1aa6f2534ef8d76b0efd`.

## Corrected Contract

1. The primary title is `执行计划`.
2. Plan Mode and missing-Final data state use independent semantic maps.
3. CONFIRMATION, PREPARATION, REDUCED, OBSERVATION, and BLOCKED are all formal Final results.
4. `是否值得开仓` is removed from Execution Plan and GPT. `当前计划状态` represents the five participation semantics.
5. CONFIRMATION and REDUCED render complete structured plans.
6. PREPARATION prioritizes pending trigger, upgrade, invalidation, and validity conditions.
7. OBSERVATION and BLOCKED do not render entry, stop, target, default leverage, or default position values.
8. Optional Final fields with no real value are omitted. No `--`, zero, or `暂无 AI 原始输出` is used to fill a Final field.
9. GPT remains Candidate-only. Final values are rendered only by the Execution Plan region.
10. Default-visible disclaimer and repeated contract copy is removed; safety gates remain in data contracts, tests, and audit details.

## Production Changes

- `frontend-contract.js` owns the five Plan Mode views and independent Plan Data State views.
- `renderHomeExecutionFromPayload` selects a mode-specific profile and checks the validated Final gate before rendering.
- `renderGptFinalHomeRole` carries `data-result-layer="candidate"` and exposes only candidate semantics.
- Asset switching clears the old decision context, renders a loading state, and ignores stale responses.
- AI unavailable copy is normalized to `AI解释暂不可用` without using Final fields as an explanation fallback.

## Validation Defects Closed

Browser review found and fixed two in-scope defects:

1. Missing optional Final fields were rendered as `暂无 AI 原始输出`; they are now hidden.
2. Final-with-AI-unavailable used legacy `当前分析不可用 / 尚未生成AI解释`; it now uses the frozen `AI解释暂不可用` state.

## Safety Boundary

The Final visibility gate still requires `finalPlan=true`, Rule Validation `PASS`, chain status `FINAL_VALIDATED`, source status `VALID`, and `notTradeInstruction=true`. Candidate cannot become Final through UI fallback. UserPosition still requires manual creation. Automatic open, close, add, reduce, reverse, order, and exchange execution capability remains zero.

## Candidate Status

```text
IMPLEMENTATION=COMPLETE
CONTROLLED_BROWSER=PASS
AUTHENTICATED_SPRING_BROWSER=PASS
VISIBLE_DISCLAIMER_COPY_COUNT=0
NO_FAKE_DATA=PASS
MERGED_MAIN_EFFECTIVE=NO
CURRENT_PHASE_DONE=NO
```

The candidate remains Draft and requires independent exact-Head frontend audit.
