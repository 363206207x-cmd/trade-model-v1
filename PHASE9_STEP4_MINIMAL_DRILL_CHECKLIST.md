# Phase 9 Step 4 - Minimal Drill Checklist

## 文档目的

本清单用于补齐「第九阶段 Step 4：调度与失败运营化」的日常演练层，目标是把“可值班”推进到“可演练”。

适用场景：

- 值班交接前快速演练
- 每周例行故障演练
- 联调前运行态检查

配套文档：

- `PHASE9_STEP4_SCHEDULER_FAILURE_OPERATIONS_EXECUTION.md`
- `PHASE9_STEP4_ACCEPTANCE_RECORD_TEMPLATE.md`
- `PHASE9_STEP4_ACCEPTANCE_RECORD_EXAMPLE.md`
- `PHASE9_STEP4_QUICK_DRILL_CARD.md`

---

## 演练通用约定

建议先设置统一变量：

```bash
BASE_URL="http://localhost:8080"
TS="$(date +%Y%m%d-%H%M%S)"
EVIDENCE_DIR="artifacts/phase9/step4/drill-$TS"
mkdir -p "$EVIDENCE_DIR"
```

每个场景固定输出：

- 演练目标
- 触发/采样方式
- 判定标准（通过/不通过）
- 恢复后复核
- 证据落盘路径

---

## 场景 1：PositionSync 最近活动陈旧（STALE）

### 目标

验证值班人员能识别 `schedulerStatus=STALE` 或 `positionSync.availabilityStatus=STALE`，并完成分级判断与记录。

### 执行

```bash
curl -sS "$BASE_URL/api/system/run-baseline?windowMinutes=60" \
  | tee "$EVIDENCE_DIR/s1-run-baseline.json" \
  | jq '{schedulerStatus:.data.systemHealth.schedulerStatus, schedulerDetail:.data.systemHealth.schedulerStatusDetail, availabilityStatus:.data.positionSync.availabilityStatus, availabilityDetail:.data.positionSync.availabilityDetail}'

curl -sS "$BASE_URL/api/system/position-sync-status" \
  | tee "$EVIDENCE_DIR/s1-position-sync-status.json" \
  | jq '{freshnessStatus:.data.freshnessStatus, freshnessDetail:.data.freshnessDetail, lastSyncEndTime:.data.lastSyncEndTime, lastSyncSuccess:.data.lastSyncSuccess, lastSyncMessage:.data.lastSyncMessage}'
```

### 判定标准

- 若出现 `STALE`：应能按 Step 4 规则给出 P1/P2 分级和处置动作
- 若未出现 `STALE`：记录“本轮未触发异常，场景按观测演练通过”

### 证据

- `s1-run-baseline.json`
- `s1-position-sync-status.json`
- 值班结论文本（可附在验收记录备注）

---

## 场景 2：PushRecheckScheduler 待复扫积压观测

### 目标

验证值班人员能识别 push recheck 相关风险信号（待处理积压、状态偏斜），并形成“影响面解释”。

### 执行

```bash
curl -sS "$BASE_URL/api/system/run-baseline?windowMinutes=60" \
  | tee "$EVIDENCE_DIR/s2-run-baseline.json" \
  | jq '{recheckSummary:.data.recheckSummary, generatedAt:.data.generatedAt}'
```

建议补充观察：

- `recheckSummary.totalCountWindow` 是否异常抬升
- 状态分布是否集中在少数非终态（结合业务阈值解释）

### 判定标准

- 能输出“是否存在积压风险”的结论
- 能解释“当前影响的是吞吐、时效，还是准确性”

### 证据

- `s2-run-baseline.json`
- 状态分布解读文本（1-3 句）

---

## 场景 3：Baseline 指标/状态异常识别

### 目标

验证值班人员对 baseline 中异常项（ratio 或状态）的快速识别能力。

### 执行

```bash
curl -sS "$BASE_URL/api/system/run-baseline?windowMinutes=15" \
  | tee "$EVIDENCE_DIR/s3-baseline-15m.json" \
  | jq '{alertSummary:.data.alertSummary, dataQualitySummary:.data.dataQualitySummary, systemHealth:.data.systemHealth}'

curl -sS "$BASE_URL/api/system/run-baseline?windowMinutes=60" \
  | tee "$EVIDENCE_DIR/s3-baseline-60m.json" \
  | jq '{alertSummary:.data.alertSummary, dataQualitySummary:.data.dataQualitySummary, systemHealth:.data.systemHealth}'
```

### 判定标准

- 至少指出 1 个“可疑项”（例如 suppression ratio、low quality ratio、scheduler 状态）
- 能完成 15m vs 60m 对比，区分短时抖动或持续异常

### 证据

- `s3-baseline-15m.json`
- `s3-baseline-60m.json`
- 对比结论文本

---

## 场景 4：某个 scheduler 恢复后复核

### 目标

验证“恢复后必须双次复测”的动作闭环，不以单次正常作为恢复完成。

### 执行

```bash
curl -sS "$BASE_URL/api/system/run-baseline?windowMinutes=60" \
  | tee "$EVIDENCE_DIR/s4-recheck-1.json" \
  | jq '{t:.data.generatedAt, schedulerStatus:.data.systemHealth.schedulerStatus, availabilityStatus:.data.positionSync.availabilityStatus}'

sleep 60

curl -sS "$BASE_URL/api/system/run-baseline?windowMinutes=60" \
  | tee "$EVIDENCE_DIR/s4-recheck-2.json" \
  | jq '{t:.data.generatedAt, schedulerStatus:.data.systemHealth.schedulerStatus, availabilityStatus:.data.positionSync.availabilityStatus}'
```

### 判定标准

- 两次连续复测均恢复到目标状态（通常为 `RUNNING + FRESH`）才算恢复完成
- 若第二次回落，故障保持“未恢复/观察中”状态

### 证据

- `s4-recheck-1.json`
- `s4-recheck-2.json`
- 恢复状态结论文本

---

## 场景 5：值班记录补证据（归档动作）

### 目标

验证值班人员能将演练结果补入 Step 4 验收记录，形成可追溯闭环。

### 执行

1. 打开 `PHASE9_STEP4_ACCEPTANCE_RECORD_TEMPLATE.md`
2. 至少填写以下字段：
   - 验收项、预期结果、实际结果
   - 证据链接/接口返回路径
   - 是否通过、负责人、日期、备注
3. 将本次演练证据目录写入记录：
   - `artifacts/phase9/step4/drill-<timestamp>/...`

### 判定标准

- 至少完成 1 条场景的“完整可追溯记录”
- 第三方可仅凭记录复现你的结论路径

---

## 最小通过线（Step 4 Drill）

一次“最小演练通过”需要满足：

- [ ] 场景 1~4 至少完成 3 个
- [ ] 至少 1 个场景包含“异常识别 + 解释 + 处置建议”
- [ ] 至少 1 个场景包含“恢复后双次复测”
- [ ] 场景 5 完成并写入验收模板

建议频率：

- 日常值班：每周 1 次
- 版本发布前：发布窗口前 1 次
- 重大故障后：48 小时内复盘演练 1 次
