# Phase 9 Step 4 - Scheduler And Failure Operations Execution Guide

## 文档目的

本文件用于承接「第九阶段 Step 4：调度与失败运营化执行指令」，将 scheduler 运行态从“可运行”推进到“可观测、可运营、可解释失败”。

交付目标：

- 明确调度链路与责任边界（market / position / push recheck）
- 明确运行态观测入口与状态判定标准
- 明确失败分级、排障顺序、恢复动作与留档要求
- 提供可直接执行的值班操作指令（curl + jq）

范围控制：仅覆盖当前仓库已有能力，不新增数据库表、不改接口协议、不引入外部告警平台依赖。

---

## 1. 运行链路与责任边界

### 1.1 调度任务清单（当前实现）

- `MarketDataScheduler#fetchRealMarketDataScheduled`
  - 触发：`@Scheduled(initialDelay=60000, fixedRate=30000)`
  - 作用：按配置 symbol 列表触发主链（fetch + assemble + decision + 落库）
- `PositionSyncScheduler#syncPositionsScheduled`
  - 触发：`@Scheduled(initialDelay=15000, fixedRate=30000)`
  - 作用：拉取实时持仓并更新 `tm_real_position`
- `PushRecheckScheduler#recheckPendingPushesScheduled`
  - 触发：`@Scheduled(initialDelay=15000, fixedRate=30000)`
  - 作用：处理待复检 push，调用 `pushRecheckService.recheck`

### 1.2 Step 4 关注的运营语义

- **可观测**：能知道 scheduler 是否“最近有活动”、position sync 是否“新鲜”
- **可运营**：能按固定手册执行检查、确认、恢复、复核
- **可解释失败**：失败时能回答“哪里失败、失败影响面、当前是否已恢复”

---

## 2. 观测入口与状态判定标准

### 2.1 核心观测接口

- 运行基线快照：`GET /api/system/run-baseline`
- 持仓同步状态：`GET /api/system/position-sync-status`

建议基地址：

```bash
BASE_URL="http://localhost:8080"
```

### 2.2 scheduler 运行态判定（来自 run-baseline.systemHealth）

关键字段：

- `systemHealth.schedulerStatus`
- `systemHealth.schedulerStatusDetail`

当前可见状态语义：

- `RUNNING`：最近 10 分钟存在 position sync 活动（start 或 completion）
- `STALE`：最近活动早于 10 分钟阈值
- `NO_RECENT_ACTIVITY`：尚未观测到有效活动

### 2.3 position sync 可用性判定（来自 run-baseline.positionSync）

关键字段：

- `positionSync.availabilityStatus`：`FRESH` / `STALE` / `UNKNOWN`
- `positionSync.availabilityDetail`
- `positionSync.lastSyncSuccess`
- `positionSync.lastSyncMessage`
- `positionSync.fallbackOccurred` / `fallbackReason`

运营解释口径：

- `FRESH`：可用性正常，持仓可用于决策上下文
- `STALE`：数据过旧，功能可用但可靠性下降
- `UNKNOWN`：状态不可判定（未完成、失败或缺乏样本）

---

## 3. 值班执行指令（按顺序）

### 3.1 第一步：基线总览

```bash
curl -sS "$BASE_URL/api/system/run-baseline?windowMinutes=60" | jq '{
  generatedAt: .data.generatedAt,
  schedulerStatus: .data.systemHealth.schedulerStatus,
  schedulerStatusDetail: .data.systemHealth.schedulerStatusDetail,
  positionSyncAvailability: .data.positionSync.availabilityStatus,
  positionSyncAvailabilityDetail: .data.positionSync.availabilityDetail,
  lastSyncSuccess: .data.positionSync.lastSyncSuccess,
  lastSyncMessage: .data.positionSync.lastSyncMessage
}'
```

### 3.2 第二步：持仓同步细节确认

```bash
curl -sS "$BASE_URL/api/system/position-sync-status" | jq '{
  freshnessStatus: .data.freshnessStatus,
  freshnessDetail: .data.freshnessDetail,
  staleThresholdMinutes: .data.staleThresholdMinutes,
  configuredProviderType: .data.configuredProviderType,
  activeProviderType: .data.activeProviderType,
  activeProviderName: .data.activeProviderName,
  fallbackOccurred: .data.fallbackOccurred,
  fallbackReason: .data.fallbackReason,
  lastSyncStartTime: .data.lastSyncStartTime,
  lastSyncEndTime: .data.lastSyncEndTime,
  lastSyncSuccess: .data.lastSyncSuccess,
  lastSyncMessage: .data.lastSyncMessage
}'
```

### 3.3 第三步：运行窗口对比（排除瞬时抖动）

```bash
curl -sS "$BASE_URL/api/system/run-baseline?windowMinutes=15" | jq '.data.systemHealth,.data.positionSync'
curl -sS "$BASE_URL/api/system/run-baseline?windowMinutes=60" | jq '.data.systemHealth,.data.positionSync'
```

判定建议：

- 15m 与 60m 均异常：高优先级故障
- 15m 异常、60m 正常：优先按短时抖动跟踪

---

## 4. 失败分级与处置剧本

### 4.1 P1（高优先级）

触发条件（任一满足）：

- `schedulerStatus=STALE` 且持续超过 2 个轮询周期
- `positionSync.lastSyncSuccess=false` 且连续出现
- `positionSync.availabilityStatus=UNKNOWN` 且 `lastSyncMessage` 明确失败

处置动作：

1. 立即采样并保存 run-baseline + position-sync-status 原始响应
2. 检查服务日志中 `position-sync`、`push-recheck-scheduler`、`sched-single-chain` 关键报错
3. 记录影响窗口（开始时间 / 当前时间 / 已影响轮次）
4. 触发恢复动作（重启应用或恢复依赖）后，连续两次复测接口状态

### 4.2 P2（中优先级）

触发条件（任一满足）：

- `positionSync.availabilityStatus=STALE`
- `fallbackOccurred=true` 且持续出现
- `schedulerStatus=NO_RECENT_ACTIVITY` 但在启动窗口内刚上线

处置动作：

1. 先确认是否处于冷启动窗口（initialDelay + 首次执行）
2. 检查 provider 配置与 fallback 原因
3. 增加观测频次（建议每 1 分钟一次，持续 10 分钟）

### 4.3 P3（低优先级/观察项）

触发条件：

- 单次 `UNKNOWN` 且下一轮自动恢复
- 无用户影响，仅有瞬时波动

处置动作：

- 留档并进入趋势观察，不触发升级

---

## 5. 失败解释模板（对内统一口径）

建议按以下三句输出：

1. **失败点**：`position sync / scheduler freshness / provider fallback` 中哪一层异常
2. **影响面**：仅影响可观测性、还是影响决策上下文新鲜度
3. **当前状态**：已恢复 / 观察中 / 未恢复（附最近两次接口证据时间）

示例：

- 失败点：position sync 在 10:12-10:20 连续失败（`lastSyncSuccess=false`）
- 影响面：持仓新鲜度降为 `UNKNOWN`，决策上下文可靠性下降
- 当前状态：10:22 与 10:23 两次复测恢复 `FRESH`，故障已收敛

---

## 6. Step 4 验收清单（可打勾执行）

### 6.1 可观测性

- [ ] 值班人员可通过 `run-baseline` 一次性获取 scheduler 与 position sync 摘要
- [ ] 值班人员可通过 `position-sync-status` 解释 freshness / fallback / lastSync 细节
- [ ] 可明确区分 `RUNNING` / `STALE` / `NO_RECENT_ACTIVITY`

### 6.2 可运营性

- [ ] 已形成固定执行顺序（总览 -> 细节 -> 双窗口复核）
- [ ] 已定义 P1/P2/P3 分级与对应动作
- [ ] 证据留档路径与负责人可追溯

### 6.3 可解释失败

- [ ] 失败结论能说明失败点、影响面、当前恢复状态
- [ ] 至少 1 条失败样例具备完整证据（请求 + 响应 + 结论）
- [ ] 恢复后至少两次连续复测正常

### 6.4 自动化回归建议

- [ ] `mvn test` 回归通过（至少覆盖 scheduler/position sync 相关现有测试）
- [ ] `run-baseline` 与 `position-sync-status` 返回结构未破坏

---

## 7. 非本轮目标（保持边界）

以下内容不作为 Step 4 通过条件：

- 引入外部 APM/告警平台（Prometheus/Grafana/告警网关）
- 新增调度中心、任务编排系统或分布式锁
- 对 scheduler 执行策略做架构级重构
- 历史失败数据清洗或批量修复脚本体系化
