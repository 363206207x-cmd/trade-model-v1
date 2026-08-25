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
    private final UiReviewPositionMonitoringReadService positionReadService;

    public UiReviewDashboardHomeService(UiReviewPositionMonitoringReadService positionReadService) {
        this.positionReadService = positionReadService;
    }

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

        DashboardHomeVO.PositionAggregateVO positionAggregate = positionReadService.aggregate();
        home.setHeader(header());
        home.setSystemState(systemState(positionAggregate));
        home.setAlerts(List.of(alert()));
        home.setEvents(List.of(event()));
        home.setAssets(assets);
        List<DashboardHomeVO.PositionVO> positions = positionReadService.homeTopThree(selectedPositionId);
        home.setPositions(positions);
        home.setPositionAggregate(positionAggregate);
        home.setStates(moduleStates());
        home.setSelectedSymbol(selectedSymbol);
        home.setSelectedAssetContext(assets.stream()
                .filter(asset -> selectedSymbol.equals(asset.getSymbol()))
                .findFirst().orElse(assets.get(0)));
        home.setSelectedContextState(home.getSelectedAssetContext().getOpportunityState());
        home.setSelectedPositionId(selectedPositionId);
        home.setPositionSelectionStatus("READY");
        home.setMatchingPositionCount(positions.size());
        home.setPositionMonitoringState("OPEN_MONITORING");
        home.setExecutionSuggestion(SELECTED_SYMBOL.equals(selectedSymbol) ? finalPlan() : noFinalPlan());
        home.setAiDecision(SELECTED_SYMBOL.equals(selectedSymbol) ? aiDecision(selectedPositionId) : unavailableAi());
        home.setDerivatives(SELECTED_SYMBOL.equals(selectedSymbol)
                ? derivatives() : new DashboardHomeVO.DerivativesSummaryVO());
        home.setDiagnostics(diagnostics(positionAggregate));
        return home;
    }

    private DashboardHomeVO.HeaderVO header() {
        DashboardHomeVO.HeaderVO header = new DashboardHomeVO.HeaderVO();
        header.setPageTitle("首页总览");
        header.setDataStatus("READY");
        header.setDataSourceText("UI_REVIEW_FIXTURE");
        header.setAiStatus("READY");
        header.setAiStatusLabel("三角色完成");
        header.setUpdatedAt(null);
        return header;
    }

    private DashboardHomeVO.SystemStateVO systemState(DashboardHomeVO.PositionAggregateVO aggregate) {
        DashboardHomeVO.SystemStateVO state = new DashboardHomeVO.SystemStateVO();
        state.setMarketTrend(status("market", "BTC / 宏观环境", "趋势环境", "READY", null));
        state.setRiskLevel(status("risk", "系统风险", "—", "SOURCE_UNAVAILABLE", null));
        state.setDataQuality(status("quality", "全局数据", "—", "SOURCE_UNAVAILABLE", null));
        state.setServiceAvailability(status("service", "服务可用性", "—", "SOURCE_UNAVAILABLE", null));
        state.setAccountStatus(status("account", "账户·已录入",
                aggregate.getActiveCount() > 0 ? aggregate.getActiveCount() + " 笔" : "—",
                "AVAILABLE", aggregate.getActiveCount()));
        state.setAiConflict(status("ai", "AI 系统", "三角色完成", "READY", 82));
        state.setPendingReview(status("positions", "已录入持仓",
                "活动 " + aggregate.getActiveCount(), "READY", aggregate.getActiveCount()));
        state.setConfused(status("conflict", "冲突", "轻微分歧", "READY", 2));
        state.setHotReset(status("reset", "Hot Reset", "—", "SOURCE_UNAVAILABLE", null));
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
                asset(1, "BTCUSDT", "比特币", "WAITING_TRIGGER", "PREPARATION", 86, "87%", "HIGH", "15m", "WEAK_BULLISH", "ALIGNED", 2),
                asset(2, "ETHUSDT", "Ethereum", "CANDIDATE", "OBSERVATION", 81, "82%", "LOW", "1h", "BULLISH", "ALIGNED", 1),
                asset(3, "SOLUSDT", "Solana", "TRIGGERED", "PREPARATION", 78, "79%", "MEDIUM", "5m", "WEAK_BULLISH", "MIXED_NEUTRAL", 3),
                asset(4, "LINKUSDT", "Chainlink", "HIGH_RISK", "REDUCED", 74, "76%", "HIGH", "4h", "NEUTRAL", "MIXED_NEUTRAL", 2),
                asset(5, "AVAXUSDT", "Avalanche", "HIGH_RISK", "OBSERVATION", 69, "73%", "EXTREME", "1h", "WEAK_BEARISH", "ALIGNED", 1),
                asset(6, "DOTUSDT", "Polkadot", "WAITING_TRIGGER", "PREPARATION", 65, "71%", "LOW", "4h", "BEARISH", "MIXED_NEUTRAL", 2));
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
        boolean hasFinal = slot == 1 || slot == 3 || slot == 4;
        asset.setHasFinal(hasFinal);
        asset.setFinalMarketBias(hasFinal ? bias : null);
        asset.setFinalPlanMode(hasFinal ? planMode : null);
        asset.setFinalPlanLifecycle(slot == 3 ? "NEEDS_REVALIDATION" : hasFinal ? "CURRENT" : null);
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
        plan.setRecommendedAction("等待触发；触发后重新校验，通过后再进入人工确认");
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
        plan.setPlanLifecycleState("CURRENT");
        plan.setPlanVersion(3);
        plan.setNeedsRevalidation(false);
        plan.setRevalidationRule("结构或数据变化后重新校验");
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

    private DashboardHomeVO.AiDecisionVO aiDecision(Long scenarioId) {
        DashboardHomeVO.AiDecisionVO decision = new DashboardHomeVO.AiDecisionVO();
        decision.setSchemaVersion("v2");
        decision.setRunStatus("READY");
        decision.setRunStatusLabel("三角色完成");
        decision.setDecisionMode("OPPORTUNITY_DECISION");
        decision.setDecisionModeLabel("机会裁决");
        decision.setActiveTab("GPT_FINAL");
        decision.setTabs(List.of(gpt(), gemini(scenarioId), grok(scenarioId)));
        DashboardHomeVO.ConsistencyVO consistency = new DashboardHomeVO.ConsistencyVO();
        consistency.setDataState("READY");
        consistency.setConflictLevel("LEVEL_2_MINOR_DISAGREEMENT");
        consistency.setFinalMarketBias("WEAK_BULLISH");
        consistency.setFinalPlanMode("PREPARATION");
        consistency.setMainReason("Gemini 对 BTCUSDT 15m 触发证据完整性存在分歧，当前仍未满足连续站稳条件");
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
        tab.setAnalysisId(null);
        tab.setTraceId(null);
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
                "WEAK_BULLISH", "WAITING_TRIGGER",
                "结论：方向仍偏多，但衍生品拥挤限制追涨，先等待 15m 触发确认"));
        tab.setMultiTimeframeExplanation(new AiRoleResultsPayload.MultiTimeframeExplanation(
                "中期结构仍偏多", "回踩后保持支撑", "尚未放量站稳", "短线动能修复但未确认"));
        tab.setBiasAdjustment(new AiRoleResultsPayload.BiasAdjustment(
                "BULLISH", "WEAK_BULLISH", "未平仓量支持方向，但资金费率和多头拥挤限制强度"));
        tab.setCandidateSummary(candidate());
        tab.setSupportingEvidence(List.of(
                evidence("support-1", "价格结构", "4h 结构抬高，1h 回踩后仍守住支撑", "BULLISH", 86.0),
                evidence("support-2", "未平仓量", "CoinGlass 未平仓量随价格增加，说明有新增仓位参与", "BULLISH", 82.0),
                evidence("support-3", "清算", "CoinGlass 短时清算未出现异常激增", "NEUTRAL", 78.0)));
        tab.setSupportingEvidenceState("FOUND");
        tab.setOpposingEvidence(List.of(
                evidence("oppose-1", "资金费率", "CoinGlass 资金费率偏高，多头持仓成本和挤压风险上升", "BEARISH", 76.0),
                evidence("oppose-2", "多空拥挤", "CoinGlass 多空账户比显示多头偏拥挤，不能据此继续追涨", "BEARISH", 72.0)));
        tab.setOpposingEvidenceState("FOUND");
        tab.setDecisionSummary("先不追涨；15m 放量站稳后重新校验，当前只保留弱偏多候选");
        return tab;
    }

    private AiRoleResultsPayload.CandidateSummary candidate() {
        return new AiRoleResultsPayload.CandidateSummary(
                "PREPARATION", "MEDIUM", "MEDIUM", true, "TREND_CONTINUATION",
                "等待触发；触发后重新校验，通过后再进入人工确认", "回踩企稳后确认", "62,800–63,200", "结构区间", "回踩支撑",
                "15m 放量站稳 63,200", "结构止损", "61,500 下方", "4h 支撑", "结构失效",
                "分批止盈", "65,800 / 68,200", "阻力区间", "前高压力",
                "触发确认后评估", "风险升高时降低仓位", "4h 跌破 61,500",
                "不高于 2×", "账户风险预算 12%", "轻微冲突限制仓位与杠杆",
                "4h 收盘跌破 61,500", "规则边界", "方向基础失效",
                new BigDecimal("2.2"), "计划边界", "目标与止损区间计算",
                "24 小时", "15m", "1–3 天", "数据或结构变化后重新校验",
                "方向仍偏多但不适合追涨，等待 15m 放量站稳后重新校验");
    }

    private AiRoleResultsPayload.EvidencePayload evidence(String id, String type, String value,
                                                          String direction, double confidence) {
        return new AiRoleResultsPayload.EvidencePayload(id, type, "市场证据", value, "较基准改善",
                direction, confidence, confidence, "2026-08-20T14:26:00+08:00", "FRESH", ANALYSIS_ID);
    }

    private DashboardHomeVO.AiTabVO gemini(Long scenarioId) {
        DashboardHomeVO.AiTabVO tab = roleBase("GEMINI_REVIEW", "Gemini 冲突复核", "Gemini");
        String reviewResult = switch (scenarioId != null ? scenarioId.intValue() : 0) {
            case 7301 -> "APPROVE";
            case 7303 -> "REJECT_CANDIDATE";
            case 7304 -> "RISK_WARNING";
            default -> "DOWNGRADE";
        };
        tab.setReviewResult(reviewResult);
        tab.setPlanModeAdjustment("DOWNGRADE_ONE");
        tab.setFinalDirectionImpact("SAME_FAMILY_DOWNGRADE");
        tab.setConfidenceAdjustment("DOWNGRADE_ONE");
        tab.setRiskAdjustment("RAISE_ONE");
        String before = scenarioId != null && scenarioId == 7310L ? "CONFIRMATION" : null;
        String after = scenarioId != null && scenarioId == 7310L ? "PREPARATION" : null;
        tab.setDowngradeSuggestion(new AiRoleResultsPayload.DowngradeSuggestion(
                before, after, "触发未完成，且资金费率和拥挤度限制追涨",
                "15m 放量并连续两周期站稳，同时资金费率与拥挤度回落"));
        tab.setEvidenceGaps(List.of(finding("gap-1", "证据缺口",
                "15m 尚未放量站稳，止损来源虽可追踪但需在触发后重新确认", "不能按确认型处理")));
        tab.setEvidenceGapsState("FOUND");
        tab.setLogicConflicts(List.of(finding("logic-1", "逻辑冲突",
                "价格和未平仓量偏多，但资金费率偏高且多头拥挤", "方向不变，计划强度降一级")));
        tab.setLogicConflictsState("FOUND");
        tab.setUnderestimatedRisks(List.of(finding("risk-1", "风险低估",
                "拥挤多头在宏观事件窗口可能出现连锁清算", "风险上调一级并限制追涨")));
        tab.setUnderestimatedRisksState("FOUND");
        tab.setRecoveryCondition("15m 放量并连续两周期站稳，且 CoinGlass 资金费率与多头拥挤回落");
        return tab;
    }

    private DashboardHomeVO.AiTabVO grok(Long scenarioId) {
        DashboardHomeVO.AiTabVO tab = roleBase("GROK_CHALLENGE", "Grok 反方挑战", "Grok");
        boolean emptyPath = scenarioId != null && (scenarioId == 7401L || scenarioId == 7402L);
        tab.setFailurePaths(emptyPath ? List.of() : List.of(new AiRoleResultsPayload.FailurePathPayload(
                "failure-1", "最可能失败：拥挤多头在假突破后被连锁清算",
                "63,200 附近未放量且未平仓量继续增加、资金费率维持偏高",
                "假突破 → 多头拥挤加深 → 价格回落 → 多头清算放大跌幅",
                "未来 4 小时", List.of("15m 成交量", "未平仓量", "资金费率", "多头清算额"),
                List.of("support-1", "support-2", "oppose-1", "oppose-2"), "持续放量站稳 63,200 且拥挤度回落")));
        tab.setFailurePathState(scenarioId != null && scenarioId == 7401L
                ? "NO_VERIFIABLE_FAILURE_PATH" : "FOUND");
        tab.setOpposingScenarios(List.of(finding("scenario-1", "反向情景",
                "价格上涨但未平仓量下降时，更可能是空头回补而不是新多头确认", "不得把反弹当成新趋势")));
        tab.setOpposingScenariosState("FOUND");
        tab.setExternalEventRisks(List.of(finding("event-1", "外部事件", "宏观数据公布前波动扩张", "触发条件失真")));
        tab.setExternalEventRisksState("FOUND");
        tab.setMicrostructureRisks(List.of(finding("micro-1", "微观结构",
                "上方卖盘深度增加，同时多头清算开始抬升", "假突破风险上升")));
        tab.setMicrostructureRisksState("FOUND");
        tab.setWatchIndicators(List.of(
                finding("watch-1", "观察指标", "15m 成交量、未平仓量与价格是否同向", "确认是否有新增仓位支持"),
                finding("watch-2", "观察指标", "资金费率、多空账户比和多头清算额", "监控拥挤与挤压风险")));
        tab.setWatchIndicatorsState("FOUND");
        tab.setCurrentDirectionChallenge("当前偏多不是无条件成立；最需要防的是拥挤多头假突破后连锁清算");
        tab.setRiskAdjustment("RAISE_ONE");
        tab.setPlanModeImpact("DOWNGRADE_ONE");
        tab.setChallengeSummary("结论：保留机会但不追涨，先验证量价、未平仓量和拥挤风险");
        tab.setMajorCounterEvidence(false);
        return tab;
    }

    private DashboardHomeVO.DerivativesSummaryVO derivatives() {
        DashboardHomeVO.DerivativesSummaryVO summary = new DashboardHomeVO.DerivativesSummaryVO();
        summary.setStatus("正常");
        summary.setOpenInterestStructure("价格与未平仓量同向增加");
        summary.setFundingRisk("偏高");
        summary.setLiquidationRisk("短时正常");
        summary.setCrowdingDirection("多头偏拥挤");
        summary.setDataTime(NOW.minusMinutes(2).toInstant(ZoneOffset.ofHours(8)));
        summary.setSource("CoinGlass v4");
        summary.setDecisionImpact("限制追涨，等待确认");
        summary.setReasonCodes(List.of("OPEN_INTEREST_PRICE_CONFIRMATION", "LONG_CROWDING"));
        return summary;
    }

    private AiRoleResultsPayload.FindingPayload finding(String id, String category, String value, String impact) {
        return new AiRoleResultsPayload.FindingPayload(id, category, value, impact,
                List.of("support-1", "support-2", "oppose-1", "oppose-2"),
                "所述风险条件出现", "未来 4 小时", List.of("15m 成交量", "未平仓量", "资金费率", "清算额"),
                "CoinGlass v4 / 市场证据", "2026-08-20T14:26:00+08:00",
                "2026-08-20T14:00:00+08:00/2026-08-20T20:30:00+08:00",
                value, "15m", category, "待验证");
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

    private DashboardHomeVO.DiagnosticsVO diagnostics(DashboardHomeVO.PositionAggregateVO aggregate) {
        DashboardHomeVO.DiagnosticsVO diagnostics = new DashboardHomeVO.DiagnosticsVO();
        diagnostics.setDataIngestion("READY");
        diagnostics.setDataQuality("READY");
        diagnostics.setAiCall("READY");
        diagnostics.setConfused("LEVEL_2_MINOR_DISAGREEMENT");
        diagnostics.setHotReset("OK");
        diagnostics.setMarketDataProvider("READY");
        diagnostics.setAiProvider("READY");
        diagnostics.setExternalContextProvider("READY");
        diagnostics.setAccountRiskCoverageState(aggregate.getCoverageState());
        return diagnostics;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
