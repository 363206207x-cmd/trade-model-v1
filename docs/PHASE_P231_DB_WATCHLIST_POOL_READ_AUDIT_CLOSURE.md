# PHASE P231 DB Watchlist Pool Read Audit Closure

## 1. Phase Position

P231 is the closure for the P230 DB Watchlist Pool read plan / mapper / schema audit.

P231 only records what P230 completed and what remains out of scope.

P231 does not implement new functionality.

## 2. P230 Merge Baseline

- PR: #579
- Issue: #578
- Merge commit: `27cc8cc`
- Title: BACKEND-P230 DB Watchlist Pool Read Plan and Mapper Schema Audit

## 3. P230 Completed Scope

P230 completed:

- DB Watchlist Pool read plan.
- Watchlist mapper / schema audit.
- DB read runtime still blocked document.
- Update to `docs/V1_CURRENT_STATE.md`.
- Update to `docs/PROJECT_PROGRESS_INDEX.md`.

## 4. P230 Audit Conclusions

P230 confirmed these reusable pieces:

- `RuleConfigService` exists at `src/main/java/org/example/trademodel/service/RuleConfigService.java`.
- `RuleConfigServiceImpl` exists.
- `RuleConfigMapper` exists.
- `RuleConfigDO` exists.
- `schema.sql` contains `tm_rule_config`.
- `RuleConfigMapper` currently exposes `findByRuleKey` and `findAllEnabled`.

P230 also confirmed these gaps:

- `push.watchlist.symbols` was not confirmed as implemented in Java or schema seed data.
- No dedicated watchlist audit table was found.
- No dedicated GET / POST watchlist API was found.
- `dashboard.html` has Watchlist Pool / Display Slots explanatory text, but it is not DB-backed read implementation.
- The current conclusion does not recommend immediately creating a new DB table.

## 5. P230 Did Not Complete

P230 did not complete:

- DB read implementation.
- Java changes.
- schema changes.
- DB queries.
- RuntimeSource service.
- `MarketQuoteClient`.
- scheduler.
- scan loop.
- ScanScore.
- Candidate Attention.
- Promote To Home.
- Opportunity Push.
- entry / stop / TP / RR.
- readiness.
- order / execution / auto-trading.

## 6. Current Conclusion

P230 is a DB read plan / mapper schema audit.

P230 is not DB read implementation.

P230 does not authorize P232 to directly connect `MarketQuoteClient`, scheduler, or real scan.
