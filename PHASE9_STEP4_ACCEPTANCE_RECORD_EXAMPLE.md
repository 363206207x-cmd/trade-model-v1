# Phase 9 Step 4 - Acceptance Record Example (Filled)

## 说明

本文件为 `PHASE9_STEP4_ACCEPTANCE_RECORD_TEMPLATE.md` 的已填样例，用于示范「Step 4 调度与失败运营化」的合格留档格式。归档时请替换为真实环境、真实请求、真实响应摘要与真实证据路径。

配套文档：

- `PHASE9_STEP4_SCHEDULER_FAILURE_OPERATIONS_EXECUTION.md`
- `PHASE9_STEP4_ACCEPTANCE_RECORD_TEMPLATE.md`

---

## 基本信息（样例已填）

- 阶段：Phase 9
- Step：Step 4
- 验收主题：调度与失败运营化
- 验收环境：本地联调环境（Spring Boot 默认端口）
- 接口基地址：`http://localhost:8080`
- 验收批次/轮次：Step 4 文档轮 · 样例归档 v1
- 值班负责人：赵六（示例）
- 后端分支/版本：`feature/phase9-step4-ops-runbook`（示例）

---

## 1) 验收执行记录表（主表 · 三条场景样例）

> 下列三行分别对应：运行态总览、失败分级、恢复复核。正式验收时请在模板主表中继续补齐其余通用验收项。

| 验收项 | 预期结果 | 实际结果 | 证据链接/截图/接口返回 | 是否通过 | 负责人 | 日期 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 场景 A：运行态总览可观测 | `GET /api/system/run-baseline` 返回 `200`；可读到 `schedulerStatus` 与 `positionSync.availabilityStatus` | 返回 `code=200`；`schedulerStatus=RUNNING`；`positionSync.availabilityStatus=FRESH`；具备 detail 字段 | `curl` 见下文「场景 A」；原始 JSON：`artifacts/phase9/step4/run-baseline-60m.json`（占位） | 通过 | 赵六 | 2026-04-17 | 值班可在 1 条请求中完成总览判断 |
| 场景 B：失败分级可执行（P1） | 当 `schedulerStatus=STALE` 且连续出现时，可判定 P1 并给出处置动作 | 连续两次采样 `STALE`，按手册判定为 P1；完成日志检查与恢复动作记录 | 证据：`artifacts/phase9/step4/p1-stale-samples.json`（占位） | 通过 | 钱七 | 2026-04-17 | 同步输出失败点/影响面/当前状态三段式结论 |
| 场景 C：恢复复核成立 | 恢复后至少两次连续复测为正常状态（`RUNNING` + `FRESH`） | 恢复后 10:22 与 10:23 两次复测均恢复正常 | 证据：`artifacts/phase9/step4/recovery-double-check.json`（占位） | 通过 | 孙八 | 2026-04-17 | 形成可归档闭环 |

---

## 2) 接口调用证据区（样例已填）

### 2.1 基线总览证据（场景 A）

- 请求 URL：`http://localhost:8080/api/system/run-baseline?windowMinutes=60`
- 请求参数：`windowMinutes=60`
- 响应摘要：`schedulerStatus=RUNNING`；`positionSync.availabilityStatus=FRESH`
- 原始返回保存位置：`artifacts/phase9/step4/run-baseline-60m.json`（占位）

```bash
curl -sS "http://localhost:8080/api/system/run-baseline?windowMinutes=60" | jq '{
  generatedAt: .data.generatedAt,
  schedulerStatus: .data.systemHealth.schedulerStatus,
  schedulerStatusDetail: .data.systemHealth.schedulerStatusDetail,
  positionSyncAvailability: .data.positionSync.availabilityStatus,
  positionSyncAvailabilityDetail: .data.positionSync.availabilityDetail
}'
```

### 2.2 持仓同步状态证据（场景 B 辅助）

- 请求 URL：`http://localhost:8080/api/system/position-sync-status`
- 响应摘要：`freshnessStatus=STALE`（故障窗口）-> `FRESH`（恢复后）
- 原始返回保存位置：`artifacts/phase9/step4/position-sync-status-series.json`（占位）

```bash
curl -sS "http://localhost:8080/api/system/position-sync-status" | jq '{
  freshnessStatus: .data.freshnessStatus,
  freshnessDetail: .data.freshnessDetail,
  lastSyncStartTime: .data.lastSyncStartTime,
  lastSyncEndTime: .data.lastSyncEndTime,
  lastSyncSuccess: .data.lastSyncSuccess,
  lastSyncMessage: .data.lastSyncMessage
}'
```

### 2.3 双窗口复核证据（场景 C）

- 请求 URL：
  - `http://localhost:8080/api/system/run-baseline?windowMinutes=15`
  - `http://localhost:8080/api/system/run-baseline?windowMinutes=60`
- 对比结论：恢复后两窗口均显示 `schedulerStatus=RUNNING` 且 `availabilityStatus=FRESH`
- 原始返回保存位置：`artifacts/phase9/step4/recovery-window-compare.json`（占位）

```bash
curl -sS "http://localhost:8080/api/system/run-baseline?windowMinutes=15" | jq '.data.systemHealth,.data.positionSync'
curl -sS "http://localhost:8080/api/system/run-baseline?windowMinutes=60" | jq '.data.systemHealth,.data.positionSync'
```

---

## 3) 失败演练与恢复证据区（样例）

- 失败注入/触发描述：
  - 模拟 provider 异常导致 position sync 连续失败（示例）
- 失败分级结论（P1/P2/P3）：
  - P1（`schedulerStatus=STALE` 连续出现）
- 失败解释（三段式）：
  - 失败点：position sync 连续失败，导致 scheduler 最近活动过旧
  - 影响面：持仓可用性降级为 `UNKNOWN/STALE`，决策上下文可靠性下降
  - 当前状态：恢复动作后两次复测回到 `RUNNING + FRESH`
- 恢复动作：
  - 恢复 provider 连接并重启服务（示例）
- 恢复后两次复测记录：
  - 10:22 与 10:23 两次 `run-baseline` 均正常
- 证据位置：
  - `artifacts/phase9/step4/p1-recovery-evidence.md`（占位）

---

## 4) 自动化测试证据区（样例）

- 执行命令：
  - `mvn test`
- 结果摘要：
  - `BUILD SUCCESS`（示例）
- 报告路径或截图：
  - `target/surefire-reports/`（以实际构建产物为准）
- 补充回归命令（如有）：
  - `mvn test -Dtest=PushRecheckServiceImplTest,ReviewAggregateServiceImplTest`

---

## 5) 最终验收结论区（样例）

- 本轮结论：**通过**
- 阻塞项（如有）：
  - 无
- 风险项（如有）：
  - 当前观测能力以内置接口为主，跨服务统一告警仍需后续里程碑补齐
- 后续动作：
  - 将 Step 4 值班手册纳入值班日历演练，并按周更新失败样例证据
- 结论确认人：
  - 运维验收负责人（占位）
- 结论日期：
  - 2026-04-17
