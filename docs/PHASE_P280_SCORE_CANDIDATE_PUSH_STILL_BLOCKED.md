# P280 Score Candidate Push Still Blocked

P280 keeps score, Candidate, and Push execution blocked.

## Still Blocked

The following remain blocked:

- Production ScanScore computation.
- Candidate production workflow.
- Promote To Home runtime logic.
- Opportunity Push execution.
- Scheduler/API/dashboard wiring.
- External channel behavior.
- Provider credentials / live provider call / message rendering / sending.

P280 does not implement scan output, score output, Candidate output, Push preview, Push delivery, or any runtime flow.

## Guard Validator Boundary

A future `RealScanInputContractGuardValidator` may only validate input-contract safety. It must not compute score, create Candidate, trigger Push, or make a scan input eligible for delivery.

Risk Action Guard must remain before delivery / Push / Readiness.

踩踏状态禁止机会推送。

插针不等于趋势反转。

强反转不等于直接反手。
