# Fundamental AI v4.1 AI Analysis Preview Report

Status: `IMPLEMENTED_PENDING_INDEPENDENT_REAUDIT`

## User Flow

`/analysis` now supports:

1. fuzzy market-asset search by symbol, name or alias;
2. selection of one canonical asset identity;
3. visible Pool-membership context;
4. explicit start of `ANALYSIS_PREVIEW`;
5. task/result state;
6. Evidence, Eight Scores, Multi-Timeframe and Preview Three-AI output;
7. optional add-to-Pool action for continued tracking.

The result route remains `/analysis/{analysisId}`.

## Preview Boundary

For an asset outside the Pool, Preview does not create an Opportunity,
Candidate, Resolver result, Rule Validation result, Final Plan, Top6 entry,
opportunity message, or Home selection change. The executed integration test
verifies these database boundaries through the service/repository path.

## Role Copy

Preview mode uses evidence synthesis/direction hypothesis, evidence quality and
logic review, and adverse-scenario/risk stress wording. Opportunity Decision
uses candidate formation, evidence/risk review and failure-path stress wording.
Technical role IDs remain audit metadata and are not the primary user copy.

## Evidence

- Runtime route: `/analysis`
- Runtime capture:
  `docs/evidence/v4_1_final_p1_remediation/runtime/analysis-preview-authenticated-full.png`
- Data provenance: controlled authenticated scenario; not live AI-provider
  acceptance.
- Integration suite: `DecisionChainAuditPreviewIntegrationTest`

`AI_ASSET_SEARCH = PASS`

`ANALYSIS_PREVIEW_START = PASS`

`PREVIEW_NO_OPPORTUNITY = PASS`

`PREVIEW_NO_CANDIDATE = PASS`

`PREVIEW_NO_FINAL = PASS`
