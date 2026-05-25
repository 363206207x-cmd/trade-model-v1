# P258 Opportunity Push Review-Only Skeleton Verification

## 1. 阶段目标

P258 只实现 Opportunity Push DTO / enum / rule skeleton。该 skeleton 只能表达 review-only opportunity attention（值得人工关注的机会提醒候选），不代表外部推送执行、Readiness、point generation 或交易建议。

## 2. 本轮新增文件

- `src/main/java/org/example/trademodel/dto/watchlistscan/OpportunityPushDTO.java`
- `src/main/java/org/example/trademodel/dto/watchlistscan/OpportunityPushStatusEnum.java`
- `src/main/java/org/example/trademodel/service/watchlistscan/OpportunityPushRule.java`
- `src/main/java/org/example/trademodel/service/watchlistscan/DefaultOpportunityPushRule.java`
- `src/test/java/org/example/trademodel/service/watchlistscan/DefaultOpportunityPushRuleTest.java`

## 3. 安全语义

- review-only only
- no external Opportunity Push execution
- no Telegram / email / webhook / app notification
- no Readiness
- no point generation
- no entry / stop / TP / RR
- no order / execution / auto-trading
- no API / dashboard
- no MarketQuoteClient
- no scheduler
- no runtime / live / external data reads
- Risk Action Guard blocker / stampede / liquidity deterioration blocks push eligibility
- wick-only / pin-bar direct reversal reason blocks trend-reversal push semantics

## 4. 测试覆盖

`DefaultOpportunityPushRuleTest` 覆盖：

- null input fails closed
- blank symbol fails closed
- missing Candidate Attention fails closed
- unsafe / blocked Candidate Attention fails closed
- stampede / extreme stress blocks push eligibility
- liquidity deterioration blocks execution-like push semantics
- wick-only / pin-bar direct reversal reason blocks trend-reversal push semantics
- safe review-only Candidate Attention can produce review-only Opportunity Push candidate
- every output keeps `manualReviewRequired=true` and `notTradeInstruction=true`
- every output keeps external push / readiness / trading / entry-stop-TP-RR flags false
- DTO defensive copy
- enum names expose no BUY / SELL / LONG / SHORT / READY / EXECUTABLE / SENT / TRADE / ORDER / ENTRY / STOP / TAKE_PROFIT surface
- implementation has no controller / scheduler / MarketQuoteClient / BinanceMarketQuoteClient / webhook / Telegram / email / order / execution / auto-trading dependency

## 5. 验证命令和结果

CI failure diagnosis:

- `mvn -B compile` passed in the local default shell.
- `mvn -B verify -Pci` failed in the local default shell before tests because Maven used JDK 25.0.2 and the project enforcer requires `[17,18)`.
- GitHub CI uses `.github/workflows/ci.yml` with `actions/setup-java@v4` and Temurin JDK 17.
- GitHub Actions run `26403067791` / job `77719827580` reported `quality-gate` failure for PR #635 head `2d91748`.
- The job had no runner, no steps, and no Maven log. The check-run annotation reported: "The job was not started because recent account payments have failed or your spending limit needs to be increased. Please check the 'Billing & plans' section in your settings".
- The failure is a GitHub billing / spending-limit gate before runner startup, not a P258 Java compile/test failure.
- Local verification was still re-run with `JAVA_HOME` and `PATH` pinned to Temurin JDK 17 before advancing the PR head.

Fix:

- Marked `DefaultOpportunityPushRuleTest` with `@Tag("core-regression")` so the `ci` profile runs the P258 review-only safety tests.
- Re-ran CI-equivalent Maven commands under JDK 17, matching GitHub Actions.

```bash
./mvnw -q -Dtest=DefaultOpportunityPushRuleTest test
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
mvn -B compile
```

结果：通过。

```bash
mvn -B verify -Pci
```

结果：本地默认 shell 使用 JDK 25 时失败，原因是 enforcer 要求 JDK 17；切换到 JDK 17 后通过，174 tests，JaCoCo check 通过。

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home mvn -B clean verify -Pci
```

结果：通过；干净环境重新编译 385 个 main source files 和 124 个 test source files，174 tests 全部通过，JaCoCo check 通过。

```bash
git diff --check
```

结果：通过。

## 6. 当前结论

P258 是 review-only Opportunity Push skeleton，不是外部推送，不是 Readiness，不是 point generation，不是交易建议。

后续 external push channel、Readiness、point generation、entry / stop / TP / RR、order / execution / auto-trading 仍必须另开授权门。
