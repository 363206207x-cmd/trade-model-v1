# Fundamental AI v4.1 Result And Explanation Separation

## Ownership

| Surface | Owns | Must not own |
|---|---|---|
| Execution Plan | Validated `FinalExecutionPlan`, `finalMarketBias`, `finalPlanMode`, Rule Validation/source identity | Candidate inference, AI-generated fallback, UserPosition creation |
| GPT_FINAL tab | `ExecutionPlanCandidate` explanation, candidate bias/mode, supporting and opposing evidence, multi-timeframe reasoning | Final mode, Final entry/stop/target, final execution status |
| GEMINI_REVIEW tab | Candidate evidence and risk review | Plan generation, Final creation |
| GROK_CHALLENGE tab | Failure paths, opposing scenarios, external and microstructure risks | Plan generation, Final creation |
| AI Consistency | Compact dependent conflict/final-adjustment summary | Fourth AI role, vote, percentage, chart |

## Candidate / Final UI Isolation

GPT uses `data-result-layer="candidate"` and labels the mode `候选计划模式`. The Execution Plan uses `data-plan-source="final"` and renders only after the full Final visibility gate passes. There is no fallback from candidate mode, summary, entry, stop, or target fields into Final.

## Availability Rules

- Candidate exists, Final absent: GPT may explain Candidate; Execution Plan displays `等待规则校验` or the real missing-Final state.
- Final exists, AI unavailable: Execution Plan remains visible; Three AI displays `AI解释暂不可用`.
- Neither layer constructs the other layer's content.
- Role state and collection state remain independent.

## Safety And Product Copy

Default-visible disclaimer copy is removed because it obscured the product result. The underlying `notTradeInstruction` gate, Candidate/Final separation, Final/UserPosition separation, and zero automatic-trading capability remain enforced in contracts and tests and may be inspected through collapsed audit details.
