# P256 Candidate Attention Review-Only Skeleton Verification

## 1. 阶段目标

P256 只实现 Candidate Attention DTO / enum / rule skeleton。该 skeleton 只能表达 review-only Candidate Attention（只允许复核的候选关注），不代表 Push、Promote、Readiness 或交易建议。

## 2. 本轮新增文件

- `src/main/java/org/example/trademodel/dto/watchlistscan/CandidateAttentionDTO.java`
- `src/main/java/org/example/trademodel/dto/watchlistscan/CandidateAttentionStatusEnum.java`
- `src/main/java/org/example/trademodel/service/watchlistscan/CandidateAttentionRule.java`
- `src/main/java/org/example/trademodel/service/watchlistscan/DefaultCandidateAttentionRule.java`
- `src/test/java/org/example/trademodel/service/watchlistscan/DefaultCandidateAttentionRuleTest.java`

## 3. 安全语义

- review-only only
- no Promote To Home
- no Opportunity Push
- no Readiness
- no point generation
- no entry / stop / TP / RR
- no order / execution / auto-trading
- no API / dashboard
- no MarketQuoteClient
- no scheduler
- no runtime / live / external data reads

## 4. 测试覆盖

`DefaultCandidateAttentionRuleTest` 覆盖：

- null score fails closed
- blank symbol fails closed
- unsafe score fails closed
- non-review-only score fails closed
- safe reviewOnly score returns review-only candidate attention
- score reasons and blocking reasons are preserved
- all outputs preserve no-execution defaults
- DTO defensive copy
- enum has no BUY / SELL / LONG / SHORT / READY / EXECUTABLE / PUSHED / PROMOTED
- reflection check confirms no forbidden MarketQuoteClient / BinanceMarketQuoteClient / Scheduler / Controller / Push service / DataSource / JdbcTemplate / Scheduled fields or methods
- method names do not contain push / promote / readiness / order / execute / trade

## 5. 验证命令和结果

```bash
./mvnw -q -Dtest=DefaultCandidateAttentionRuleTest test
```

结果：通过。

```bash
./mvnw -q -DskipTests compile
```

结果：通过。

```bash
./mvnw -q -DskipTests test-compile
```

结果：通过。

```bash
git diff --check
```

结果：通过。

## 6. 当前结论

P256 是 review-only Candidate Attention skeleton，不是 Push，不是 Promote，不是 Readiness，不是交易建议。后续 Promote To Home、Opportunity Push、Readiness、point generation、entry / stop / TP / RR、order / execution / auto-trading 仍必须另开授权门。
