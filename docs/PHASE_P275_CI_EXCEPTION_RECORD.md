# P275 CI Exception Record

P275 records the P274 local-validation exception merge.

## Exception Summary

P274 was merged as a user-approved exception because GitHub CI did not trigger for PR #667 / HEAD `929841d`.

This record is narrow and applies only to P274.

## What Happened

- PR: `#667 BACKEND-P274 External Channel Disabled No-Op Java Skeleton`
- Branch: `p274`
- Issue: `#666`
- Head that needed CI: `929841d chore(push): retrigger P274 ci`
- Merge commit: `905fb41 BACKEND-P274 External Channel Skeleton (#667)`

GitHub CI did not trigger for PR #667 / HEAD `929841d`.

An empty retrigger commit was attempted:

```text
929841d chore(push): retrigger P274 ci
```

Ready for review transition was attempted.

The user explicitly approved the local-validation exception merge.

## Local Validation That Passed

The following local validation passed before the exception merge:

```bash
./mvnw -q -Dtest=NoOpOpportunityPushExternalChannelPolicyTest test
./mvnw -q -DskipTests compile
./mvnw -q -DskipTests test-compile
```

These checks covered the P274 targeted test and compilation surface for the newly added external channel disabled no-op skeleton.

## Exception Boundary

This exception does not convert P274 into a normal CI-passed merge.

This exception does not authorize:

- relaxing future CI expectations
- skipping tests for future Java changes
- implementing external channel behavior
- connecting Telegram/email/webhook/app notification/local notification
- handling provider credentials
- making live provider calls
- rendering messages
- sending messages
- adding scheduler/API/dashboard wiring
- upgrading Readiness
- generating point values
- creating order/execution/auto-trading paths

Future exceptions must be explicitly approved and recorded separately.
