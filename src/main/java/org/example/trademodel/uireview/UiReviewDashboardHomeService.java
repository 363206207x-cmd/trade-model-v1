package org.example.trademodel.uireview;

import org.example.trademodel.ai.AiRoleResultsPayload;
import org.example.trademodel.service.DashboardHomeService;
import org.example.trademodel.vo.DashboardHomeVO;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/** Local-only, non-persistent read model for browser visual acceptance. */
@Primary
@Profile("ui-review")
@Service
public class UiReviewDashboardHomeService implements DashboardHomeService {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 14, 30);
    private static final String SELECTED_SYMBOL = "BTCUSDT";
    private static final String ANALYSIS_ID = "ui-review-analysis-btc";
    private static final String TRACE_ID = "ui-review-trace-btc";

    @Override
    public DashboardHomeVO getHome(String selectedSymbol, Integer limit, Long selectedPositionId) {
        return fixture(selectedSymbol, selectedPositionId);
    }

    @Override
    public DashboardHomeVO getHomeForUser(Long userId, String selectedSymbol, Integer limit,
                                          Long selectedPositionId) {
        return fixture(selectedSymbol, selectedPositionId);
    }

    private DashboardHomeVO fixture(String requestedSymbol, Long selectedPositionId) {
        DashboardHomeVO home = new DashboardHomeVO();
        List<DashboardHomeVO.AssetVO> assets = assets();
        String selectedSymbol = assets.stream()
                .map(DashboardHomeVO.AssetVO::getSymbol)
                .filter(symbol -> symbol.equalsIgnoreCase(normalize(requestedSymbol)))
                .findFirst()
                .orElse(SELECTED_SYMBOL);

        home.setHeader(header());
        home.setSystemState(systemState());
        home.setAlerts(List.of(alert()));
        home.setEvents(List.of(event()));
        home.setAssets(assets);
        home.setPositions(positions());
        home.setStates(moduleStates());
        home.setSelectedSymbol(selectedSymbol);
        home.setSelectedAssetContext(assets.stream()
                .filter(asset -> selectedSymbol.equals(asset.getSymbol()))
                .findFirst().orElse(assets.get(0)));
        home.setSelectedContextState(home.getSelectedAssetContext().getOpportunityState());
        home.setSelectedPositionId(selectedPositionId);
        home.setPositionSelectionStatus("READY");
        home.setMatchingPositionCount(3);
        home.setPositionMonitoringState("OPEN_MONITORING");
        home.setExecutionSuggestion(SELECTED_SYMBOL.equals(selectedSymbol) ? finalPlan() : noFinalPlan());
        home.setAiDecision(SELECTED_SYMBOL.equals(selectedSymbol) ? aiDecision() : unavailableAi());
        home.setDiagnostics(diagnostics());
        return home;
    }

    private DashboardHomeVO.HeaderVO header() {
        DashboardHomeVO.HeaderVO header = new DashboardHomeVO.HeaderVO();
        header.setPageTitle("首页总览");
        header.setDataStatus("READY");
        header.setDataSourceText("数据新鲜 · 服务正常");
        header.setAiStatus("READY");
        header.setAiStatusLabel("三角色完成");
        header.setUpdatedAt(NOW);
        return header;
    }

    private DashboardHomeVO.SystemStateVO systemState() {
        DashboardHomeVO.SystemStateVO state = new DashboardHomeVO.SystemStateVO();
        state.setMarketTrend(status("market", "市场趋势", "弱偏多", "READY", 72));
        state.setRiskLevel(status("risk", "风险等级", "中", "READY", 58));
        state.setDataQuality(status("quality", "数据质量", "87 · 新鲜", "READY", 87));
        state.setAiConflict(status("ai", "AI 系统", "三角色完成", "READY", 82));
        state.setPendingReview(status("positions", "已录入持仓", "活动 3", "READY", 3));
        state.setConfused(status("conflict", "冲突", "轻微分歧", "READY", 2));
        state.setHotReset(status("reset", "Hot Reset", "正常", "READY", 0));
        return state;
    }

    private DashboardHomeVO.StatusCardVO status(String key, String label, String valueLabel,
                                                 String status, Integer score) {
        DashboardHomeVO.StatusCardVO card = new DashboardHomeVO.StatusCardVO();
        card.setKey(key);
        card.setLabel(label);
        card.setValueLabel(valueLabel);
        card.setStatus(status);
        card.setScore(score);
        return card;
    }

    private DashboardHomeVO.AlertRowVO alert() {
        DashboardHomeVO.AlertRowVO row = new DashboardHomeVO.AlertRowVO();
        row.setLevel("HIGH");
        row.setMessage("SOL 持仓风险显著上升");
        row.setSymbol("SOLUSDT");
        row.setTime("2026-08-20T14:27:00+08:00");
        return row;
    }

    private DashboardHomeVO.EventRowVO event() {
        DashboardHomeVO.EventRowVO row = new DashboardHomeVO.EventRowVO();
        row.setType("宏观事件");
        row.setLabel("美国 CPI 公布");
        row.setImpactLevel("HIGH");
        row.setTimeWindow("2026-08-20T20:30:00+08:00");
        return row;
    }

    private List<DashboardHomeVO.AssetVO> assets() {
        return List.of(
                asset(1, "BTCUSDT", "比特币", "WAITING_TRIGGER", "PREPARATION", 86, "87%", "MEDIUM", "15m", "WEAK_BULLISH", "ALIGNED", 2),
                asset(2, "ETHUSDT", "Ethereum", "CANDIDATE", "OBSERVATION", 81, "82%", "LOW", "1h", "BULLISH", "ALIGNED", 1),
                asset(3, "SOLUSDT", "Solana", "TRIGGERED", "CONFIRMATION", 78, "79%", "HIGH", "5m", "WEAK_BULLISH", "MIXED_NEUTRAL", 3),
                asset(4, "LINKUSDT", "Chainlink", "HIGH_RISK", "REDUCED", 74, "76%", "HIGH", "4h", "NEUTRAL", "MIXED_NEUTRAL", 2),
                asset(5, "AVAXUSDT", "Avalanche", "OBSERVING", "OBSERVATION", 69, "73%", "MEDIUM", "1h", "WEAK_BEARISH", "ALIGNED", 1),
                asset(6, "DOTUSDT", "Polkadot", "COOLING", "BLOCKED", 65, "71%", "LOW", "4h", "BEARISH", "MIXED_NEUTRAL", 2));
    }

    private DashboardHomeVO.AssetVO asset(int slot, String symbol, String name, String opportunityState,
                                          String planMode, int score, String confidence, String risk,
                                          String timeframe, String bias, String conflict, int secondaryCount) {
        DashboardHomeVO.AssetVO asset = new DashboardHomeVO.AssetVO();
        asset.setSlot(slot);
        asset.setSlotType("OPPORTUNITY");
        asset.setSymbol(symbol);
        asset.setRawSymbol(symbol);
        asset.setName(name);
        asset.setAssetId(9000L + slot);
        asset.setAnalysisId(slot == 1 ? ANALYSIS_ID : "ui-review-analysis-" + slot);
        asset.setOpportunityId("ui-review-opportunity-" + slot);
        asset.setPrimaryOpportunityId(asset.getOpportunityId());
        asset.setOpportunityState(opportunityState);
        asset.setAssetState(opportunityState);
        asset.setPrimaryTimeframe(timeframe);
        asset.setPrimaryPlanMode(planMode);
        asset.setPlanMode(planMode);
        asset.setSecondaryOpportunityCount(secondaryCount);
        asset.setTimeframeConflictState(conflict);
        asset.setOpportunityScore(score);
        asset.setCompositeScore(score);
        asset.setConfidenceLabel(confidence);
        asset.setConfidenceLevel(confidence);
        asset.setRiskLevel(risk);
        asset.setRiskLabel(riskLabel(risk));
        asset.setMarketBias(bias);
        asset.setMarketBiasLabel(biasLabel(bias));
        asset.setDataQualityScore(Math.max(72, score));
        asset.setRankingReason("机会质量与数据完整度排序");
        asset.setDataFreshness("FRESH");
        asset.setSourceProvider("Kraken");
        asset.setEvidenceCount(4);
        asset.setModuleState("READY");
        asset.setDataQuality("READY");
        asset.setMultiTimeframeState(conflict);
        asset.setConfused(false);
        asset.setUpdatedAt(NOW.minusMinutes(slot));
        return asset;
    }

    private String riskLabel(String risk) {
        return Map.of("LOW", "低", "MEDIUM", "中", "HIGH", "高", "EXTREME", "极高").get(risk);
    }

    private String biasLabel(String bias) {
        return Map.of("BULLISH", "偏多", "WEAK_BULLISH", "弱偏多", "NEUTRAL", "中性",
                "WEAK_BEARISH", "弱偏空", "BEARISH", "偏空").get(bias);
    }

    private List<DashboardHomeVO.PositionVO> positions() {
        return List.of(
                position(7101L, "BTCUSDT", "LONG", "SYSTEM_PLAN_POSITION", "62000", "64100", "3.39",
                        "MEDIUM", "STABLE", "STILL_VALID", "NO_REVERSAL", "NO_CLEAR_RISK_FACTOR",
                        "LOGIC_VALID", "CONTINUE_HOLD", "逻辑仍成立", "继续持有", NOW.minusDays(12)),
                position(7102L, "ETHUSDT", "SHORT", "MANUAL_POSITION", "3400", "3490", "-2.65",
                        "HIGH", "INCREASED", "WEAKENED", "WEAK_REVERSAL", "OPPOSING_EVIDENCE_INCREASED",
                        "LOGIC_WEAKENED", "TIGHTEN_STOP", "逻辑弱化", "收紧止损", NOW.minusDays(7)),
                position(7103L, "SOLUSDT", "LONG", "SYSTEM_PLAN_POSITION", "145", "129.5", "-10.69",
                        "EXTREME", "SHARPLY_INCREASED", "INVALIDATED", "STRONG_REVERSAL", "STRUCTURE_CHANGED",
                        "PLAN_INVALIDATED", "WAIT_CONFIRMATION", "计划失效", "等待人工确认", NOW.minusDays(4)));
    }

    private DashboardHomeVO.PositionVO position(Long id, String symbol, String direction, String sourceType,
                                                 String entry, String mark, String pnlPercent, String risk,
                                                 String riskTrend, String logic, String reversal, String riskReason,
                                                 String conclusion, String action, String conclusionLabel,
                                                 String actionLabel, LocalDateTime openedAt) {
        DashboardHomeVO.PositionVO position = new DashboardHomeVO.PositionVO();
        position.setPositionId(id);
        position.setSymbol(symbol);
        position.setDirection(direction);
        position.setDirectionLabel("LONG".equals(direction) ? "做多" : "做空");
        position.setSourceType(sourceType);
        position.setEntryPrice(new BigDecimal(entry));
        position.setMarkPrice(new BigDecimal(mark));
        position.setCurrentPrice(new BigDecimal(mark));
        position.setMarkPriceSource("MARKET_SNAPSHOT");
        position.setMarkPriceObservedAt(NOW.minusMinutes(2));
        position.setMarkPriceFresh(true);
        position.setPnlPercent(new BigDecimal(pnlPercent));
        position.setPnlPct(new BigDecimal(pnlPercent));
        position.setRiskLevel(risk);
        position.setRiskLevelLabel(riskLabel(risk));
        position.setRiskTrend(riskTrend);
        position.setEntryLogicStatus(logic);
        position.setEntryLogicStatusLabel(labelFor(logic));
        position.setReversalStatus(reversal);
        position.setReversalStatusLabel(labelFor(reversal));
        position.setRiskReason(riskReason);
        position.setRiskReasonLabel(labelFor(riskReason));
        position.setMonitorConclusion(conclusion);
        position.setMonitorConclusionLabel(conclusionLabel);
        position.setSuggestedAction(action);
        position.setSuggestedManualAction(action);
        position.setSuggestedManualActionText(actionLabel);
        position.setOpenedAt(openedAt);
        position.setLastMonitorAt(NOW.minusMinutes(3));
        position.setLastMonitorTime(NOW.minusMinutes(3));
        position.setUpdatedAt(NOW.minusMinutes(3));
        position.setPositionStatus("OPEN");
        position.setModuleState("READY");
        position.setWarningState("EXTREME".equals(risk) ? "HIGH" : "NORMAL");
        position.setDataState("STABLE".equals(riskTrend) ? "OPEN_MONITORING" :
                ("PLAN_INVALIDATED".equals(conclusion) ? "PLAN_INVALIDATED" : "RISK_ESCALATED"));
        position.setFinalPlanId("MANUAL_POSITION".equals(sourceType) ? null : "ui-review-final-" + symbol.toLowerCase());
        return position;
    }

    private String labelFor(String value) {
        return Map.ofEntries(
                Map.entry("STILL_VALID", "仍成立"), Map.entry("WEAKENED", "弱化"),
                Map.entry("INVALIDATED", "失效"), Map.entry("NO_REVERSAL", "无明显反转"),
                Map.entry("WEAK_REVERSAL", "弱反转"), Map.entry("STRONG_REVERSAL", "强反转"),
                Map.entry("NO_CLEAR_RISK_FACTOR", "暂无明显风险因素"),
                Map.entry("OPPOSING_EVIDENCE_INCREASED", "反向证据增加"),
                Map.entry("STRUCTURE_CHANGED", "结构变化")).get(value);
    }

    private DashboardHomeVO.ExecutionSuggestionVO finalPlan() {
        DashboardHomeVO.ExecutionSuggestionVO plan = new DashboardHomeVO.ExecutionSuggestionVO();
        plan.setStatus("USABLE_REVIEW_PLAN");
        plan.setStatusLabel("当前有效");
        plan.setSourceAnalysisId(ANALYSIS_ID);
        plan.setSourceExecutionPlanId("ui-review-final-btc-001");
        plan.setSourceTraceId(TRACE_ID);
        plan.setFinalMarketBias("WEAK_BULLISH");
        plan.setDirection("LONG");
        plan.setFinalPlanMode("PREPARATION");
        plan.setWorthOpening(true);
        plan.setRecommendedAction("等待触发后人工确认");
        plan.setEntryZone("62,800–63,200");
        plan.setTriggerCondition("15m 放量站稳 63,200");
        plan.setStopZone("61,500 下方失效");
        plan.setInvalidCondition("4h 收盘跌破 61,500");
        plan.setTargetZones("65,800 / 68,200 分批");
        plan.setLeverageSuggestion("不高于 2×");
        plan.setPositionSuggestion("账户风险预算 12%");
        plan.setValidPeriod("24 小时");
        plan.setValidFrom(OffsetDateTime.of(NOW, ZoneOffset.ofHours(8)));
        plan.setExpiresAt(OffsetDateTime.of(NOW.plusHours(24), ZoneOffset.ofHours(8)));
        plan.setValidationStatus("PASS");
        plan.setSourceStatus("VALID");
        plan.setChainStatus("FINAL_VALIDATED");
        plan.setCandidateId("ui-review-candidate-btc-001");
        plan.setResolverResultId("ui-review-resolver-btc-001");
        plan.setValidationResultId("ui-review-validation-btc-001");
        plan.setFinalPlan(true);
        plan.setExecutionFeasibilityStatus("MANUAL_CONFIRMATION_REQUIRED");
        plan.setNotTradeInstruction(true);
        plan.setModuleState("READY");
        return plan;
    }

    private DashboardHomeVO.ExecutionSuggestionVO noFinalPlan() {
        DashboardHomeVO.ExecutionSuggestionVO plan = new DashboardHomeVO.ExecutionSuggestionVO();
        plan.setStatus("NOT_FORMED");
        plan.setStatusLabel("尚未形成");
        plan.setBlockedReason("尚未形成有效计划");
        plan.setFinalPlan(false);
        plan.setValidationStatus("MISSING");
        plan.setNotTradeInstruction(true);
        plan.setModuleState("MISSING");
        return plan;
    }

    private DashboardHomeVO.AiDecisionVO aiDecision() {
        DashboardHomeVO.AiDecisionVO decision = new DashboardHomeVO.AiDecisionVO();
        decision.setSchemaVersion("v2");
        decision.setRunStatus("READY");
        decision.setRunStatusLabel("三角色完成");
        decision.setDecisionMode("OPPORTUNITY_DECISION");
        decision.setDecisionModeLabel("机会裁决");
        decision.setActiveTab("GPT_FINAL");
        decision.setTabs(List.of(gpt(), gemini(), grok()));
        DashboardHomeVO.ConsistencyVO consistency = new DashboardHomeVO.ConsistencyVO();
        consistency.setDataState("READY");
        consistency.setConflictLevel("LEVEL_2_MINOR_DISAGREEMENT");
        consistency.setFinalMarketBias("WEAK_BULLISH");
        consistency.setFinalPlanMode("PREPARATION");
        consistency.setMainReason("方向一致，入场确认条件仍需满足");
        consistency.setRecoveryCondition("15m 触发成立且数据质量保持稳定");
        decision.setConsistency(consistency);
        return decision;
    }

    private DashboardHomeVO.AiDecisionVO unavailableAi() {
        DashboardHomeVO.AiDecisionVO decision = new DashboardHomeVO.AiDecisionVO();
        decision.setRunStatus("UNAVAILABLE");
        decision.setRunStatusLabel("当前不可用");
        decision.setDecisionMode("OPPORTUNITY_DECISION");
        decision.setDecisionModeLabel("机会裁决");
        decision.setTabs(List.of(unavailableRole("GPT_FINAL", "GPT 综合判断"),
                unavailableRole("GEMINI_REVIEW", "Gemini 冲突复核"),
                unavailableRole("GROK_CHALLENGE", "Grok 反方挑战")));
        return decision;
    }

    private DashboardHomeVO.AiTabVO unavailableRole(String role, String roleLabel) {
        DashboardHomeVO.AiTabVO tab = roleBase(role, roleLabel, role.toLowerCase());
        tab.setRoleState("UNAVAILABLE");
        tab.setDataState("SOURCE_UNAVAILABLE");
        tab.setRunStatus("UNAVAILABLE");
        tab.setRunStatusLabel("当前不可用");
        tab.setResultAvailable(false);
        tab.setStatusMessage("当前资产尚无完整角色结果");
        return tab;
    }

    private DashboardHomeVO.AiTabVO gpt() {
        DashboardHomeVO.AiTabVO tab = roleBase("GPT_FINAL", "GPT 综合判断", "GPT");
        tab.setCoreJudgment(new AiRoleResultsPayload.CoreJudgment(
                "WEAK_BULLISH", "WAITING_TRIGGER", "多周期方向保持偏多，触发条件尚未完成"));
        tab.setMultiTimeframeExplanation(new AiRoleResultsPayload.MultiTimeframeExplanation(
                "偏多结构保持", "回踩后企稳", "等待放量确认", "短线动能修复"));
        tab.setBiasAdjustment(new AiRoleResultsPayload.BiasAdjustment(
                "BULLISH", "WEAK_BULLISH", "反对证据限制方向强度"));
        tab.setCandidateSummary(candidate());
        tab.setSupportingEvidence(List.of(
                evidence("support-1", "趋势", "4h 结构抬高", "BULLISH", 0.86),
                evidence("support-2", "成交", "15m 成交量回升", "BULLISH", 0.78)));
        tab.setSupportingEvidenceState("FOUND");
        tab.setOpposingEvidence(List.of(
                evidence("oppose-1", "风险", "短周期动能尚未完全确认", "BEARISH", 0.64)));
        tab.setOpposingEvidenceState("FOUND");
        tab.setDecisionSummary("等待触发确认，维持预备型 Candidate");
        return tab;
    }

    private AiRoleResultsPayload.CandidateSummary candidate() {
        return new AiRoleResultsPayload.CandidateSummary(
                "PREPARATION", "84%", "MEDIUM", true, "趋势回踩",
                "等待触发后人工确认", "回踩企稳后确认", "62,800–63,200", "结构区间", "回踩支撑",
                "15m 放量站稳 63,200", "结构止损", "61,500 下方", "4h 支撑", "结构失效",
                "分批止盈", "65,800 / 68,200", "阻力区间", "前高压力",
                "触发确认后评估", "风险升高时降低仓位", "4h 跌破 61,500",
                "不高于 2×", "账户风险预算 12%", "轻微冲突限制仓位与杠杆",
                "4h 收盘跌破 61,500", "规则边界", "方向基础失效",
                new BigDecimal("2.2"), "计划边界", "目标与止损区间计算",
                "24 小时", "15m", "1–3 天", "数据或结构变化后重新校验",
                "弱偏多 Candidate，等待触发后进入人工确认");
    }

    private AiRoleResultsPayload.EvidencePayload evidence(String id, String type, String value,
                                                          String direction, double confidence) {
        return new AiRoleResultsPayload.EvidencePayload(id, type, "市场证据", value, "较基准改善",
                direction, confidence, confidence, "2026-08-20T14:26:00+08:00", "FRESH", ANALYSIS_ID);
    }

    private DashboardHomeVO.AiTabVO gemini() {
        DashboardHomeVO.AiTabVO tab = roleBase("GEMINI_REVIEW", "Gemini 冲突复核", "Gemini");
        tab.setReviewResult("DOWNGRADE");
        tab.setPlanModeAdjustment("PREPARATION");
        tab.setFinalDirectionImpact("建议同向降级");
        tab.setConfidenceAdjustment("86% → 84%");
        tab.setRiskAdjustment("LOW → MEDIUM");
        tab.setDowngradeSuggestion(new AiRoleResultsPayload.DowngradeSuggestion(
                "CONFIRMATION", "PREPARATION", "触发证据尚不完整", "15m 放量并连续两周期站稳"));
        tab.setEvidenceGaps(List.of(finding("gap-1", "证据缺口", "短周期量能确认不足", "限制立即入场")));
        tab.setEvidenceGapsState("FOUND");
        tab.setLogicConflicts(List.of(finding("logic-1", "逻辑冲突", "4h 偏多与 5m 动能分化", "降低计划强度")));
        tab.setLogicConflictsState("FOUND");
        tab.setUnderestimatedRisks(List.of(finding("risk-1", "风险低估", "事件窗口可能放大波动", "风险上调至中")));
        tab.setUnderestimatedRisksState("FOUND");
        tab.setRecoveryCondition("15m 放量并连续两周期站稳");
        return tab;
    }

    private DashboardHomeVO.AiTabVO grok() {
        DashboardHomeVO.AiTabVO tab = roleBase("GROK_CHALLENGE", "Grok 反方挑战", "Grok");
        tab.setFailurePaths(List.of(new AiRoleResultsPayload.FailurePathPayload(
                "failure-1", "突破失败后回落", "63,200 附近量价背离", "触发失败 → 动能衰减 → 跌破支撑",
                "未来 4 小时", List.of("15m 成交量", "买卖盘深度"), List.of("市场证据"), "持续放量站稳 63,200")));
        tab.setFailurePathState("FOUND");
        tab.setOpposingScenarios(List.of(finding("scenario-1", "反向情景", "突破失败并回落至区间下沿", "方向强度下降")));
        tab.setOpposingScenariosState("FOUND");
        tab.setExternalEventRisks(List.of(finding("event-1", "外部事件", "宏观数据公布前波动扩张", "触发条件失真")));
        tab.setExternalEventRisksState("FOUND");
        tab.setMicrostructureRisks(List.of(finding("micro-1", "微观结构", "上方卖盘深度增加", "突破延迟")));
        tab.setMicrostructureRisksState("FOUND");
        tab.setWatchIndicators(List.of(finding("watch-1", "观察指标", "15m 成交量与买卖盘深度", "确认触发质量")));
        tab.setWatchIndicatorsState("FOUND");
        tab.setCurrentDirectionChallenge("偏多方向成立，但突破失败路径可验证");
        tab.setPlanModeImpact("PREPARATION");
        tab.setChallengeSummary("保留机会，维持预备型并等待触发确认");
        tab.setMajorCounterEvidence(false);
        return tab;
    }

    private AiRoleResultsPayload.FindingPayload finding(String id, String category, String value, String impact) {
        return new AiRoleResultsPayload.FindingPayload(id, category, value, impact, List.of("市场证据"),
                null, "未来 4 小时", List.of(), "市场证据", "2026-08-20T14:26:00+08:00",
                null, null, "15m", null, null);
    }

    private DashboardHomeVO.AiTabVO roleBase(String role, String roleLabel, String provider) {
        DashboardHomeVO.AiTabVO tab = new DashboardHomeVO.AiTabVO();
        tab.setRole(role);
        tab.setRoleLabel(roleLabel);
        tab.setAnalysisId(ANALYSIS_ID);
        tab.setTraceId(TRACE_ID + "-" + role.toLowerCase());
        tab.setRoleState("READY");
        tab.setDataState("FOUND");
        tab.setGeneratedAt("2026-08-20T14:28:00+08:00");
        tab.setProvider(provider);
        tab.setSourceRole(role);
        tab.setFallback(false);
        tab.setRunStatus("SUCCESS");
        tab.setRunStatusLabel("已完成");
        tab.setResultAvailable(true);
        return tab;
    }

    private DashboardHomeVO.ModuleStatesVO moduleStates() {
        DashboardHomeVO.ModuleStatesVO states = new DashboardHomeVO.ModuleStatesVO();
        states.setOverall("READY");
        states.setAssets("READY");
        states.setExecutionPlan("READY");
        states.setPositions("READY");
        states.setAi("READY");
        states.setConsistency("READY");
        return states;
    }

    private DashboardHomeVO.DiagnosticsVO diagnostics() {
        DashboardHomeVO.DiagnosticsVO diagnostics = new DashboardHomeVO.DiagnosticsVO();
        diagnostics.setDataIngestion("READY");
        diagnostics.setDataQuality("READY");
        diagnostics.setAiCall("READY");
        diagnostics.setConfused("LEVEL_2_MINOR_DISAGREEMENT");
        diagnostics.setHotReset("OK");
        diagnostics.setMarketDataProvider("READY");
        diagnostics.setAiProvider("READY");
        diagnostics.setExternalContextProvider("READY");
        diagnostics.setAccountRiskCoverageState("PARTIAL_COVERAGE");
        return diagnostics;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
