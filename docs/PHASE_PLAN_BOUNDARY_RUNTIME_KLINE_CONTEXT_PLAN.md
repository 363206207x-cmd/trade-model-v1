# PHASE_PLAN_BOUNDARY_RUNTIME_KLINE_CONTEXT_PLAN

## 一、方案背景

- BoundaryCandidate DTO 已完成。
- entry / stop / TP numeric source 方案要求价位边界必须来自可追溯行情结构。
- INCOMPLETE runtime gate 方案要求缺少 runtime kline window 时必须阻断。
- RuntimeKlineContext 是后续候选边界生成的行情输入对象。
- 本阶段只定义方案，不实现 Java，不改 schema，不改 dashboard，不接 service。

## 二、RuntimeKlineContext 定位

- RuntimeKlineContext 是运行时行情上下文。
- 它不是交易信号。
- 它不是执行计划。
- 它不是 AI 输出。
- 它只负责承载用于边界判断的 K 线窗口、时间周期、数据质量和来源状态。
- 后续 BoundaryCandidateService 可读取它生成 candidate。
- 没有 RuntimeKlineContext 时 candidate 必须 INCOMPLETE 或 WATCH_ONLY。

## 三、最小字段建议

建议 RuntimeKlineContext 第一版至少包含：

- `symbol`
- `timeframe`
- `klineWindowStart`
- `klineWindowEnd`
- `klineCount`
- `latestOpen`
- `latestHigh`
- `latestLow`
- `latestClose`
- `latestVolume`
- `previousClose`
- `highestHigh`
- `lowestLow`
- `averageVolume`
- `dataSourceName`
- `sourceType`
- `dataQualityScore`
- `staleStatus`
- `fetchTime`
- `generatedAt`
- `missingFields`
- `blockingReasons`
- `ruleVersion`

## 四、Kline item 结构建议

如后续需要子对象，可定义 Kline item：

- `openTime`
- `closeTime`
- `open`
- `high`
- `low`
- `close`
- `volume`
- `sourceType`

说明：

- 第一版可先不实现完整 Kline item list。
- 最小实现可以只承载聚合字段。
- 后续 swing / ATR 需要完整窗口时再扩展。

## 五、timeframe 策略

- 15m / 1h / 4h 是边界判断优先周期。
- 5m 只用于插针 / 流动性风险辅助。
- 5m 不单独决定趋势边界。
- 如果缺少 timeframe，必须 INCOMPLETE。
- 如果多周期冲突严重，candidate 不能 VALID。

## 六、数据质量策略

- `dataQualityScore` 低于阈值时不能 VALID。
- `sourceType` 如果只是 fallback，需要明确是否足够支持边界。
- stale kline 不能生成 VALID。
- 缺少 high / low / close / volume 关键字段时不能 VALID。
- 数据不完整时进入 INCOMPLETE / WATCH_ONLY。

## 七、stale / missing 判断

建议：

- `staleStatus` 可为 `FRESH` / `STALE` / `UNKNOWN`。
- `missingFields` 记录缺失字段。
- `blockingReasons` 可记录：
  - `MISSING_KLINE_WINDOW`
  - `MISSING_TIMEFRAME`
  - `KLINE_STALE`
  - `DATA_QUALITY_LOW`
  - `MISSING_HIGH_LOW`
  - `MISSING_CLOSE`
  - `MISSING_VOLUME`
  - `SOURCE_UNRELIABLE`

## 八、与 BoundaryCandidate 的关系

- RuntimeKlineContext 是 BoundaryCandidate 的输入。
- `BoundaryCandidate.sourceFields` 应引用 RuntimeKlineContext 的 `timeframe` / `klineWindowStart` / `klineWindowEnd` / `sourceType` / `dataQualityScore`。
- 没有 RuntimeKlineContext，不允许 VALID。
- RuntimeKlineContext 不负责生成 entry / stop / TP，只提供输入。
- BoundaryCandidateService 后续才负责生成边界。

## 九、与 INCOMPLETE gate 的关系

- 缺少 RuntimeKlineContext => INCOMPLETE。
- RuntimeKlineContext stale => INCOMPLETE 或 WATCH_ONLY。
- RuntimeKlineContext `dataQualityScore` 低 => INCOMPLETE。
- RuntimeKlineContext source 不可信 => INCOMPLETE。
- RuntimeKlineContext 不能证明结构边界时，不能 VALID。

## 十、与现有行情 / dashboard 的关系

- 当前 dashboard detail 有 `latestPrice` / `sourceType` / `dataQualityScore`。
- 这些字段不足以单独构成 RuntimeKlineContext。
- `latestPrice` 不能直接生成 entry / stop / TP。
- 本阶段不改 dashboard。
- 本阶段不改行情拉取逻辑。
- 本阶段不新增 API。

## 十一、与 Risk Action Guard 的关系

- 高风险不等于自动平仓。
- 高风险不等于反手。
- 插针不等于趋势反转。
- 5m 插针只用于风险辅助。
- 踩踏状态禁止机会推送。
- RuntimeKlineContext 不生成交易动作。
- 不接 order API。

## 十二、后续最小实现建议

建议后续 Java 最小实现：

- `RuntimeKlineContextDTO.java`
- `RuntimeKlineItemDTO.java`（可选，第一版可暂缓）
- `RuntimeKlineContextStatusEnum.java`
- `RuntimeKlineContextDTOTest.java`

建议 package：

- `src/main/java/org/example/trademodel/dto/planboundary/`

说明：

- 第一版只做 DTO / enum / test。
- 不接 service。
- 不接 mapper。
- 不改 schema。
- 不改 dashboard。

## 十三、后续实现顺序

建议：

1. 提交本 RuntimeKlineContext 方案文档。
2. 创建 RuntimeKlineContext implementation checklist。
3. 创建 RuntimeKlineContext DTO / enum / test。
4. 创建 BoundaryCandidateService 只读生成方案。
5. 创建 INCOMPLETE gate 单元测试方案。
6. 再评估是否接 PlanReadiness / ExecutionPlan。

## 十四、本阶段不做内容

- 不创建 Java 文件。
- 不实现 service。
- 不改 schema。
- 不改 dashboard。
- 不改行情拉取逻辑。
- 不接 PlanReadiness。
- 不接 ExecutionPlan。
- 不接 RuleEngine。
- 不接自动交易。
- 不接 order API。
- 不恢复 untracked 大轨道源码。
