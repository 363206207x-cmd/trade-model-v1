# PHASE P230 Watchlist Mapper Schema Audit

## 1. Phase Position

This document is only a read-only audit of watchlist mapper / schema readiness.

P230 does not implement Java.

P230 does not modify schema.

P230 does not execute DB queries.

## 2. Audit Questions And Answers

### Current RuleConfigService

`RuleConfigService` exists at:

- `src/main/java/org/example/trademodel/service/RuleConfigService.java`

The user-specified path `src/main/java/org/example/trademodel/service/rule/RuleConfigService.java` does not exist in the current branch.

`RuleConfigServiceImpl` exists at:

- `src/main/java/org/example/trademodel/service/impl/RuleConfigServiceImpl.java`

It uses `RuleConfigMapper.findAllEnabled()` to refresh an in-memory rule map.

### Current RuleConfigMapper

`RuleConfigMapper` exists at:

- `src/main/java/org/example/trademodel/mapper/RuleConfigMapper.java`

It currently exposes:

- `findByRuleKey(String ruleKey)`
- `findAllEnabled()`

### Current Schema

`src/main/resources/schema.sql` contains `tm_rule_config`.

The table includes:

- `rule_id`
- `rule_type`
- `rule_key`
- `rule_value`
- `description`
- `version`
- `enabled`

### Watchlist Key

This read-only audit did not find an implemented `push.watchlist.symbols` key in Java or schema seed data.

Earlier audit docs mention `push.watchlist.symbols` as a candidate key, but P230 does not confirm that it is currently populated.

### Watchlist Audit Table

This read-only audit did not find a dedicated watchlist audit table in `schema.sql`.

### GET / POST Watchlist API

This read-only audit did not find dedicated GET / POST watchlist APIs.

Current rule controller scope contains `GET /api/rule/reload`, which reloads rules but is not a watchlist read / write API.

### Watchlist Dashboard UI

`dashboard.html` contains Watchlist Pool / Display Slots explanatory UI text and read-only status display.

That UI is not a DB-backed Watchlist Pool read implementation.

### Should Future Watchlist Read Reuse RuleConfigService?

Yes, if Watchlist Pool is stored in `tm_rule_config`, future work should prefer `RuleConfigService` / `RuleConfigMapper` before adding a new table or mapper.

### Does Current State Need A New Table?

Default answer: no.

A new table should not be added unless a later authorization gate explicitly allows it.

## 3. Audit Conclusion Format

### Reusable Existing Files / Service / Table

- `src/main/java/org/example/trademodel/service/RuleConfigService.java`
- `src/main/java/org/example/trademodel/service/impl/RuleConfigServiceImpl.java`
- `src/main/java/org/example/trademodel/mapper/RuleConfigMapper.java`
- `src/main/java/org/example/trademodel/entity/RuleConfigDO.java`
- `src/main/resources/schema.sql`
- `tm_rule_config`

### Current Gaps

- No confirmed implemented `push.watchlist.symbols` key.
- No dedicated watchlist audit table.
- No dedicated watchlist mapper.
- No dedicated watchlist service.
- No dedicated GET / POST watchlist API.
- No DB-backed Watchlist Pool read adapter implementation.
- No production runtime source service.

### Future Minimal Java Candidate Files

Only as a future plan:

- possible `WatchlistPoolReadAdapter`
- possible `DBWatchlistPoolReadAdapter`
- possible reuse of `RuleConfigService`
- possible no new mapper

Specific file names and allowed implementation scope must be defined by a later authorization gate.

### Future Still-Prohibited Content

Future work still must not:

- read market data.
- connect `MarketQuoteClient`.
- connect scheduler.
- enter scan loop.
- generate `WatchlistScanResultDTO`.
- generate ScanScore.
- trigger Candidate Attention.
- Promote To Home.
- create Opportunity Push execution.
- generate entry / stop / TP / RR.
- upgrade readiness.
- create trading action.

## 4. Conclusion

If `RuleConfigService` / `RuleConfigMapper` / `tm_rule_config` are enough, future work should prefer reusing them.

The project should not add schema immediately just to read watchlist.

P230 makes no code changes.
