# Dead Code Candidates

This file records deletion candidates only.

No deletion is allowed unless all conditions are true:

1. Risk is LOW.
2. Recommendation is DELETE.
3. There is no production reference.
4. There is no test reference.
5. The file is not required by future phases in docs/PROJECT_DELIVERY_CONTRACT.md.
6. Maven tests pass after deletion.

No deletion is allowed in the contract-lock task.

| File | Candidate Type | Production Reference | Test Reference | Schema Support | Needed By Contract Future Phase | Risk | Recommendation | Reason |
|---|---|---|---|---|---|---|---|---|
| src/main/java/org/example/trademodel/dto/point/ReviewOnlyPointProposalDisplayDTO.java | review-only / placeholder / point-domain candidate | Unknown without dependency trace | Unknown without dependency trace | None found in scan | Possible P0-2 source-gate reference or frozen point-domain reference | UNKNOWN | DEFER | Scan found review-only placeholder semantics; deletion risk cannot be proven LOW. |
| src/main/java/org/example/trademodel/assembler/point/ReviewOnlyNumericPointProposalAssembler.java | review-only / point-domain candidate | Unknown without dependency trace | Test file exists | None found in scan | Possible P0-2 source-gate reference or frozen point-domain reference | UNKNOWN | DEFER | Candidate/Point files are frozen, not automatically dead. |
| src/main/java/org/example/trademodel/validator/point/NumericPointSafetyValidator.java | point-domain validator candidate | Unknown without dependency trace | Test file exists | None found in scan | Possible P0-2 source-gate reference | UNKNOWN | DEFER | Could be reused by ExecutionPlan Source Gate; cannot delete in P0-0. |
| src/main/java/org/example/trademodel/service/watchlistscan/NoOpOpportunityPushExternalChannelPolicy.java | no-op external channel policy candidate | Production safety reference possible | Test file exists | None found in scan | P1-1 PushRecheck semantic hardening may need no-external-channel evidence | UNKNOWN | DEFER | No-op policy is safety evidence, not proven dead. |
| src/test/java/org/example/trademodel/service/watchlistscan/MarketReadRequestTestOnlyWiring.java | test-only wiring candidate | Test only | Test reference exists | None | Could support existing tests | UNKNOWN | DEFER | Test-only helper cannot be deleted without targeted dependency and test proof. |


---

## P0-0 Reconciliation Scan Note

This task does not delete files and does not mark any candidate DELETE.

The scan found many `review-only`, `placeholder`, `duplicate`, `no-op`, `Preview`, `Mock`, `Candidate`, `Point`, and `Signal` references, but none are classified as LOW-risk DELETE in this governance draft because future-phase ownership and test references still need explicit migration evidence.

Recommendation for all ambiguous candidates in this draft: DEFER.
