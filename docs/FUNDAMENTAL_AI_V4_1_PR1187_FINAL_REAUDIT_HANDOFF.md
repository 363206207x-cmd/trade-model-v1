# Fundamental AI v4.1 PR #1187 Final Re-audit Handoff

Status: `FINAL_HTTP451_CLOSURE_PENDING_EXACT_HEAD_CI_THEN_ONE_REAUDIT`

The reviewer must pin the remote PR #1187 Head produced by this closure. The
review is invalid if the Head changes after CI or during audit.

## Required Review

1. Confirm PR remains Draft, branch is
   `codex/v4-1-target-runtime-blocker-remediation`, local/remote are 0/0, and
   the worktree is clean.
2. Compare the exact Head with the final HTTP 451 remediation baseline
   `e82ba8888da596ac67c871b4cb4b03b2ec5191b3`.
3. Verify every row in the HTTP 451 closure/root-cause matrices and all
   provider production entry points; inspect actual call-order/count and exact
   registry-key assertions.
4. Reproduce unsupported symbol/timeframe, region restricted, provider
   disabled, source unavailable, not configured, stale revalidation failure,
   supported primary, independently supported fallback, and all-unsupported
   no-fabrication cases.
5. Verify exact quote/market/contract/timeframe identity and that generic quote
   enablement cannot authorize disabled OHLCV.
6. Verify one unsupported asset does not erase five successful analyses and
   aggregate state remains truthful `PARTIAL`.
7. Verify CoinGlass disabled/key-missing/RPM-missing/invalid/positive states,
   explicit 80/300 budgets, exact `CG-API-KEY` arguments, secret redaction, and
   production implicit-default count zero.
8. Run Product Source Gate, Workflow Contract, authorization validation,
   focused B01-B04 tests, full Maven, Java 17 clean package, standard packaged
   JAR PostgreSQL 16 smoke, secret/duplicate/automatic-trading scans, and
   `git diff --check`.
9. Confirm no Schema, API product contract, Figma, Desktop, Mobile, threshold,
   Three-AI authority, Position Monitoring contract, or automatic-trading
   change.

## Required HTTP 451 Reproduction

For current price, Binance funding and Binance open interest, reproduce:

- first request external call `1`;
- first request exact `REGION_RESTRICTED` write `1`;
- structured result is fail closed with null payload;
- second exact request reads the registry and external call count remains `1`
  overall, meaning the second request adds `0` calls.

Repeat the propagation/suppression check for CoinGlass open interest, funding,
liquidation and long/short. Confirm OHLCV regression remains green, 451 is not
retried as 5xx, stale payload is not returned, and a fallback is invoked only
when its own exact dataset capability is `SUPPORTED`.

Confirm dataset isolation explicitly: funding restriction does not block or
authorize open interest/price/OHLCV, and generic quote support does not grant a
derivative dataset.

## Exact Evidence Set

- `BinanceHttp451CapabilityPropagationTest`
- `CoinGlassHttp451CapabilityPropagationTest`
- `ProviderCapabilityPreCallGateTest`
- `ProviderCallCoordinatorTest`
- `ProviderCapabilityRegistryTest`
- `ProviderCapabilityGateArchitectureTest`
- `PersistentAssetPoolServiceTest`
- full Maven/H2: `4626 total / 4612 passed / 14 environment-gated skipped /
  0 failed or errors`
- isolated PostgreSQL 16 packaged-JAR V1-V13/restart/checksum/login smoke: PASS

No critical HTTP 451 test is skipped and no live provider/AI secret is used.

## Expected Recommendation Boundary

Only the exact pushed Head may receive `APPROVE` or `REQUEST_CHANGES`. This handoff
does not authorize merge, deployment, live secrets, or a new implementation
package. Live provider and exact-model acceptance remains a post-merge Product
Owner-controlled step.
