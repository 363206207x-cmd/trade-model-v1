# Phase 9 Step 4 - Quick Drill Card (10 Minutes)

## 文档目的

本卡片用于在 10 分钟内完成 Step 4 快速演练，适用于值班交接、联调前快检、临时排障前 sanity check。

保留 3 个最高价值场景：

1. PositionSync 最近活动与 freshness
2. PushRecheckScheduler 待复扫积压
3. baseline 核心状态异常与一次证据回填

配套文档：

- `PHASE9_STEP4_SCHEDULER_FAILURE_OPERATIONS_EXECUTION.md`
- `PHASE9_STEP4_MINIMAL_DRILL_CHECKLIST.md`
- `PHASE9_STEP4_ACCEPTANCE_RECORD_TEMPLATE.md`
- `PHASE9_STEP4_ACCEPTANCE_RECORD_EXAMPLE.md`

---

## 0) 统一变量与证据目录（开始前 30 秒）

```bash
BASE_URL="http://localhost:8080"
TS="$(date +%Y%m%d-%H%M%S)"
EVIDENCE_DIR="artifacts/phase9/step4/quick-drill-$TS"
mkdir -p "$EVIDENCE_DIR"
```

---

## 1) PositionSync 最近活动与 freshness（约 3 分钟）

### 目标

确认 scheduler freshness 与 position sync freshness 是否处于可用状态（`RUNNING` + `FRESH`）。

### 执行

```bash
curl -sS "$BASE_URL/api/system/run-baseline?windowMinutes=60" \
  | tee "$EVIDENCE_DIR/q1-run-baseline.json" \
  | jq '{schedulerStatus:.data.systemHealth.schedulerStatus, schedulerStatusDetail:.data.systemHealth.schedulerStatusDetail, availabilityStatus:.data.positionSync.availabilityStatus, availabilityDetail:.data.positionSync.availabilityDetail}'

curl -sS "$BASE_URL/api/system/position-sync-status" \
  | tee "$EVIDENCE_DIR/q1-position-sync-status.json" \
  | jq '{freshnessStatus:.data.freshnessStatus, freshnessDetail:.data.freshnessDetail, lastSyncEndTime:.data.lastSyncEndTime, lastSyncSuccess:.data.lastSyncSuccess, lastSyncMessage:.data.lastSyncMessage}'
```

### 判定标准

- 通过：`schedulerStatus=RUNNING` 且 `availabilityStatus/freshnessStatus=FRESH`
- 关注：出现 `STALE/UNKNOWN`，需在记录中给出分级（P1/P2/P3）与下一步动作

### 证据项

- `q1-run-baseline.json`
- `q1-position-sync-status.json`
- 1 句值班结论（正常/异常 + 影响面）

---

## 2) PushRecheckScheduler 待复扫积压（约 3 分钟）

### 目标

快速判断 push recheck 是否出现积压风险，并能解释影响面。

### 执行

```bash
curl -sS "$BASE_URL/api/system/run-baseline?windowMinutes=60" \
  | tee "$EVIDENCE_DIR/q2-run-baseline.json" \
  | jq '{generatedAt:.data.generatedAt, recheckSummary:.data.recheckSummary, systemHealth:.data.systemHealth}'
```

### 判定标准

- 通过：能给出“是否存在积压风险”的明确结论
- 通过：能说明主要影响吞吐、时效还是准确性（1-2 句）

### 证据项

- `q2-run-baseline.json`
- 积压风险结论文本（1-2 句）

---

## 3) baseline 核心状态异常 + 一次证据回填（约 4 分钟）

### 目标

在短窗口内识别至少 1 个可疑状态或 ratio，并将结论落入验收记录模板，完成最小闭环。

### 执行

```bash
curl -sS "$BASE_URL/api/system/run-baseline?windowMinutes=15" \
  | tee "$EVIDENCE_DIR/q3-baseline-15m.json" \
  | jq '{alertSummary:.data.alertSummary, dataQualitySummary:.data.dataQualitySummary, systemHealth:.data.systemHealth}'
```

随后执行一次记录回填：

1. 打开 `PHASE9_STEP4_ACCEPTANCE_RECORD_TEMPLATE.md`
2. 至少填写 1 条验收项（预期结果、实际结果、是否通过、负责人、日期）
3. 在证据字段写入本轮目录：`artifacts/phase9/step4/quick-drill-<timestamp>/...`

### 判定标准

- 至少指出 1 个可疑项（例如 scheduler 状态、suppression ratio、low quality ratio）
- 验收模板中至少形成 1 条可追溯记录

### 证据项

- `q3-baseline-15m.json`
- 验收模板中的回填记录（含证据路径）

---

## 最小通过线（Quick Drill）

一次 10 分钟快速演练通过需满足：

- [ ] 场景 1~3 全部完成
- [ ] 至少识别并解释 1 个异常或风险信号（可疑项也可）
- [ ] 至少完成 1 条验收模板证据回填
- [ ] 证据目录完整可复查（`quick-drill-<timestamp>`）

建议频率：

- 值班交接：每次交接执行 1 次
- 联调前：每次联调窗口前执行 1 次
- 临时排障：排障前先跑 1 次作为基线
