# V1 Minimal Review-Only MarketQuote Freshness Runtime Wiring Design

## 1. Executive Summary

本任务只做 design，不实现。

最小 runtime 目标：未来用既有 `MarketQuoteClient` / `BinanceMarketQuoteClient` / `MarketQuoteSnapshot`、`RealMarketEnvironmentService`、dashboard detail / SourceTrace quote metadata，形成一个只读的 MarketQuote freshness / fallback / source-health 状态，让用户能看见行情来源是否可用、是否新鲜、是否 fallback、是否 fail-closed。

Owner path：

```text
MarketQuoteClient / BinanceMarketQuoteClient
  -> MarketQuoteSnapshot / quote metadata
  -> RealMarketEnvironmentService / existing market read owner path
  -> source trace / dashboard detail metadata
  -> future minimal review-only quote freshness API/dashboard status
```

不需要新增 DTO / Validator / Assembler。是否需要新增最小 endpoint 不能在本包直接决定实现，必须由下一步 readiness gate 判断；如果现有 dashboard detail/source trace 足够，应优先复用现有 surface。无需改 schema。不得接 Push / Candidate / Point / Trading。不得生成候选、点位、方向、entry / stop / TP / RR、order 或 execution。

下一步：`Minimal Review-Only MarketQuote Freshness Runtime Wiring Implementation Readiness Gate`。

## 2. Owner Path To Preserve

Fixed owner path:

```text
MarketQuoteClient / BinanceMarketQuoteClient
  -> MarketQuoteSnapshot / quote metadata
  -> RealMarketEnvironmentService / existing market read owner path
  -> source trace / dashboard detail metadata
  -> future minimal review-only quote freshness API/dashboard status
```

Rules:

- Future implementation must not bypass existing MarketQuote / MarketEnvironment owner path.
- Do not create a new MarketQuote wrapper owner.
- Do not route quote status directly into Push / Candidate / Point.
- Do not treat Display Slots as 行情候选池.
- Future asset boundaries must follow Watchlist Pool, or be explicitly labeled as dashboard-only sample.
- Existing SourceTrace quote metadata may be reused only as review-only display/source-health evidence.
- Legacy `DecisionServiceImpl` and `PushRecheckScheduler` MarketQuote usage must not become the owner path for this slice.

## 3. Minimal Future Status Mapping

| Status | Trigger condition | Dashboard/API copy | Candidate / Push allowed? | Review-only? | Fail-closed? |
|---|---|---|---|---|---|
| `MARKETQUOTE_REVIEW_ONLY_READY` | Quote source exists, symbol is bounded, last quote time is present, freshness is inside the future threshold, no fallback is active, and source health is not blocked. | 行情源可读；仅显示来源与新鲜度，不是交易信号。 | No | Yes | No for display; still No for candidate/push. |
| `MARKETQUOTE_STALE_FAIL_CLOSED` | Quote exists but last quote time is older than future threshold, or freshness cannot prove current data. | 行情已过期；候选/推送/点位链路 fail-closed。 | No | Yes | Yes |
| `MARKETQUOTE_MISSING_FAIL_CLOSED` | Quote is missing, provider returns empty, last quote time is missing, or selected symbol has no quote metadata. | 行情缺失；只读显示缺口，不能推进候选/推送/点位。 | No | Yes | Yes |
| `MARKETQUOTE_FALLBACK_ACTIVE` | Runtime uses placeholder fallback, provider failure fallback, or source-read fallback instead of confirmed quote data. | 行情 fallback 生效；不是 Binance 实时行情确认。 | No | Yes | Yes |
| `MARKETQUOTE_SOURCE_HEALTH_PARTIAL` | Provider/source is known but failure reason, freshness threshold, last update, or source-health fields are incomplete. | 行情源状态部分可见；需人工复核，不能作为候选输入。 | No | Yes | Yes for candidate/push/point |
| `MARKETQUOTE_BLOCKED_FAIL_CLOSED` | Provider state is contradictory, source is ambiguous, symbol boundary is not Watchlist-bounded/dashboard-only, or quote status would mislead the user. | 行情状态被阻断；保持 fail-closed。 | No | Yes | Yes |

## 4. Freshness / Fallback / Source Health Fields

Allowed minimal fields:

- `status`
- `symbol` or `symbols`
- `source`
- `sourceType`
- `lastQuoteTime` / `lastUpdatedAt` if available
- `freshnessSeconds` / `staleThresholdSeconds` if available
- `fresh`
- `fallbackActive`
- `sourceHealth`
- `reason`
- `message`
- `reviewOnly = true`
- `notTradingSignal = true`
- `watchlistBounded = true` / `dashboardOnlySample = true`

Forbidden fields:

- candidate ranking
- score
- final direction
- entry
- stop
- TP
- RR
- position size
- leverage
- order action
- Push send state

Field design principle: if an existing field is insufficient, the readiness gate must decide whether a minimal Map/existing-VO response can safely express status. It must not introduce a DTO / Validator / Assembler family by default.

## 5. Dashboard/API Minimal Surface

Future minimal dashboard/API display must show:

- quote source;
- freshness / stale / missing;
- fallback active;
- source health;
- last update if available;
- review-only label;
- not trading signal label;
- Watchlist boundary label.

Surface rules:

- If an existing endpoint/surface can safely expose these fields, reuse it.
- If no existing endpoint is sufficient, the next readiness gate must decide whether a minimal read-only endpoint is allowed.
- Do not add complex market cards.
- Do not run all-market scan.
- Do not connect Push / Candidate / Point.
- Do not present quote freshness as entry / stop / TP / RR evidence.
- Do not present quote freshness as final direction or trading authorization.

Candidate future surfaces to evaluate in readiness gate:

- existing `/api/dashboard/detail` and SourceTrace quote metadata;
- existing market environment mini data;
- a minimal read-only quote status endpoint only if existing surfaces cannot safely display freshness/fallback/source health.

## 6. Watchlist Boundary

- MarketQuote slice must not bypass Watchlist Pool.
- It must not default to all-market scanning.
- It must not treat Display Slots as 行情候选池.
- Assets outside Watchlist Pool must not enter candidate / push / point chains.
- A dashboard-only sample must be explicitly labeled as not a candidate pool.
- Ambiguity must fail closed.
- If future implementation reads a symbol from dashboard detail, it must be labeled as selected-symbol display metadata unless the source is proven Watchlist-bounded.
- Display Slots may choose what the homepage shows; they must not authorize quote scans or candidate generation.

## 7. Minimal Future Implementation Boundary

If the next package enters readiness gate, future minimal implementation must be limited to:

- Prefer existing `MarketQuoteClient` / `BinanceMarketQuoteClient`.
- Prefer existing `RealMarketEnvironmentService` / existing market read owner path.
- Prefer existing dashboard detail / source trace metadata.
- Optional minimal API/status mapping only after readiness gate.
- Optional minimal dashboard status/copy only after readiness gate.
- No new DTO / Validator / Assembler.
- No schema change.
- No Push.
- No Candidate.
- No Decision wiring expansion.
- No Point.
- No generated trading action.

Future implementation must stay review-only and manual-review safe. It must never make Binance provider presence look like trading authorization.

## 8. Readiness Checklist

The next readiness gate must check:

- Is there an existing endpoint that can be reused?
- Is a minimal endpoint required?
- Are `MarketQuoteSnapshot` fields sufficient?
- Are SourceTrace quote fields sufficient?
- Are fallback fields sufficient?
- Are freshness fields sufficient?
- Are source-health fields sufficient?
- Does dashboard have a safe DOM slot?
- Do existing tests cover enough of client parsing, service mapping, dashboard detail, and source trace?
- Can implementation avoid new DTO?
- Does the slice still avoid Push / Candidate / Point / Trading?
- Can Watchlist Pool / dashboard-only sample boundary be displayed without misleading users?
- Can stale/missing/fallback states remain fail-closed?

## 9. Capability-Level Movement

- Current level: `REVIEW_ONLY_RUNTIME partial`.
- 本包是否提升 level: No, design only.
- Future minimal MarketQuote implementation target: `REVIEW_ONLY_RUNTIME partial` for MarketQuote slice.
- It is not Production Wiring.
- It is not Push.
- It is not Candidate generation.
- It is not Point generation.
- It is not Trading.

## 10. Freeze Rule Compliance

- 是否创建新骨架: No
- 是否复用 Cursor-era 资产: Yes
- 是否减少重复: Yes
- 是否提升 capability level: No, design only
- 是否接 service/runtime/dashboard/API: No, design only
- 是否符合 #830 审计建议: Yes

## 11. Final Recommendation

可以进入 `Minimal Review-Only MarketQuote Freshness Runtime Wiring Implementation Readiness Gate`。最小实现大概只允许复用现有 MarketQuote / MarketEnvironment / dashboard detail / SourceTrace 资产，必要时增加最小只读 status surface 和 dashboard copy；禁止 Push、Candidate、Point、P359/P360、新 DTO / Validator / Assembler、全市场扫描、交易方向、entry / stop / TP / RR、order / execution / auto-trading。
