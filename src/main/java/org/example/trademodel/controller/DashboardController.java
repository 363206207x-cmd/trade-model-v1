package org.example.trademodel.controller;

import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.entity.MarketEnvironmentSnapshotDO;
import org.example.trademodel.mapper.MarketEnvironmentSnapshotMapper;
import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.service.EvidenceService;
import org.example.trademodel.service.ReviewAggregateService;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.service.ScoreService;
import org.example.trademodel.service.dashboard.DashboardSourceTraceDetailAdapter;
import org.example.trademodel.service.dashboard.ExecutionPlanDisplayAdapter;
import org.example.trademodel.service.dashboard.PaperObservationDisplayAdapter;
import org.example.trademodel.service.dashboard.PlanBoundaryDisplayAdapter;
import org.example.trademodel.service.dashboard.RiskActionGuardDisplayAdapter;
import org.example.trademodel.vo.EvidenceBriefVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.vo.ReviewAggregateSummaryVO;
import org.example.trademodel.vo.ReviewStateVO;
import org.example.trademodel.vo.ScoreBriefVO;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.service.SystemHealthService;
import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DashboardSummaryResponseVO;
import org.example.trademodel.vo.DecisionResultVO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class DashboardController {
    private static final int DEFAULT_DASHBOARD_SUMMARY_LIMIT = 12;
    private static final int MIN_DASHBOARD_SUMMARY_LIMIT = 1;
    private static final int MAX_DASHBOARD_SUMMARY_LIMIT = 24;

    /** RFC 9745 style; signals clients to migrate to {@code /api/dashboard/summary}. */
    private static final String DEPRECATION_HEADER = "Deprecation";
    private static final String DEPRECATION_LINK =
            "</api/dashboard/summary>; rel=\"alternate\"; title=\"replacement\"";
    private static final String MARKET_ENV_SOURCE_HEURISTIC = "BINANCE_24H_HEURISTIC";
    private static final String MARKET_ENV_SOURCE_FALLBACK = "PLACEHOLDER_FALLBACK";
    private static final String EVIDENCE_SCORE_READY = "EVIDENCE_SCORE_REVIEW_ONLY_READY";
    private static final String EVIDENCE_MISSING_FAIL_CLOSED = "EVIDENCE_MISSING_FAIL_CLOSED";
    private static final String SCORE_MISSING_FAIL_CLOSED = "SCORE_MISSING_FAIL_CLOSED";
    private static final String EVIDENCE_SCORE_INCOMPLETE_FAIL_CLOSED = "EVIDENCE_SCORE_INCOMPLETE_FAIL_CLOSED";
    private static final String EVIDENCE_SCORE_SOURCE_TRACE_PARTIAL = "EVIDENCE_SCORE_SOURCE_TRACE_PARTIAL";
    private static final String EVIDENCE_SCORE_BLOCKED_FAIL_CLOSED = "EVIDENCE_SCORE_BLOCKED_FAIL_CLOSED";
    private static final String DECISIONRESULT_READY = "DECISIONRESULT_REVIEW_ONLY_READY";
    private static final String DECISIONRESULT_MISSING_FAIL_CLOSED = "DECISIONRESULT_MISSING_FAIL_CLOSED";
    private static final String DECISIONRESULT_READ_MODEL_PARTIAL = "DECISIONRESULT_READ_MODEL_PARTIAL";
    private static final String DECISIONRESULT_SOURCE_TRACE_PARTIAL = "DECISIONRESULT_SOURCE_TRACE_PARTIAL";
    private static final String DECISIONRESULT_AI_ROLE_PARTIAL = "DECISIONRESULT_AI_ROLE_PARTIAL";
    private static final String DECISIONRESULT_STALE_OR_UNKNOWN_FAIL_CLOSED = "DECISIONRESULT_STALE_OR_UNKNOWN_FAIL_CLOSED";
    private static final String DECISIONRESULT_BLOCKED_FAIL_CLOSED = "DECISIONRESULT_BLOCKED_FAIL_CLOSED";
    private static final String EXECUTIONPLAN_BOUNDARY_READY = "EXECUTIONPLAN_BOUNDARY_REVIEW_ONLY_READY";
    private static final String PLAN_BOUNDARY_BACKEND_PENDING_FAIL_CLOSED = "PLAN_BOUNDARY_BACKEND_PENDING_FAIL_CLOSED";
    private static final String PLAN_BOUNDARY_INCOMPLETE_FAIL_CLOSED = "PLAN_BOUNDARY_INCOMPLETE_FAIL_CLOSED";
    private static final String PLAN_BOUNDARY_WATCH_ONLY = "PLAN_BOUNDARY_WATCH_ONLY";
    private static final String EXECUTIONPLAN_BOUNDARY_PENDING_FAIL_CLOSED = "EXECUTIONPLAN_BOUNDARY_PENDING_FAIL_CLOSED";
    private static final String EXECUTIONPLAN_SOURCE_TRACE_PARTIAL = "EXECUTIONPLAN_SOURCE_TRACE_PARTIAL";
    private static final String EXECUTIONPLAN_RISK_GUARD_BLOCKED_FAIL_CLOSED = "EXECUTIONPLAN_RISK_GUARD_BLOCKED_FAIL_CLOSED";
    private static final String EXECUTIONPLAN_BOUNDARY_BLOCKED_FAIL_CLOSED = "EXECUTIONPLAN_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String REVIEW_REPLAY_READY = "REVIEW_REPLAY_REVIEW_ONLY_READY";
    private static final String REVIEW_RESULT_MISSING_FAIL_CLOSED = "REVIEW_RESULT_MISSING_FAIL_CLOSED";
    private static final String REVIEW_AGGREGATE_MISSING_FAIL_CLOSED = "REVIEW_AGGREGATE_MISSING_FAIL_CLOSED";
    private static final String REPLAY_SUMMARY_MISSING_FAIL_CLOSED = "REPLAY_SUMMARY_MISSING_FAIL_CLOSED";
    private static final String REVIEW_REPLAY_SOURCE_TRACE_PARTIAL = "REVIEW_REPLAY_SOURCE_TRACE_PARTIAL";
    private static final String REPLAY_EXECUTION_BLOCKED_FAIL_CLOSED = "REPLAY_EXECUTION_BLOCKED_FAIL_CLOSED";
    private static final String REVIEW_REPLAY_BLOCKED_FAIL_CLOSED = "REVIEW_REPLAY_BLOCKED_FAIL_CLOSED";
    private static final String DATA_SOURCE_HEALTH_READY = "DATA_SOURCE_HEALTH_REVIEW_ONLY_READY";
    private static final String DATA_SOURCE_HEALTH_PARTIAL = "DATA_SOURCE_HEALTH_PARTIAL_REVIEW_ONLY";
    private static final String DATA_SOURCE_HEALTH_STALE_FAIL_CLOSED = "DATA_SOURCE_HEALTH_STALE_FAIL_CLOSED";
    private static final String DATA_SOURCE_HEALTH_MISSING_FAIL_CLOSED = "DATA_SOURCE_HEALTH_MISSING_FAIL_CLOSED";
    private static final String DATA_SOURCE_HEALTH_WATCH_ONLY = "DATA_SOURCE_HEALTH_WATCH_ONLY_REVIEW";
    private static final String DATA_SOURCE_HEALTH_BLOCKED_FAIL_CLOSED = "DATA_SOURCE_HEALTH_BLOCKED_FAIL_CLOSED";
    private static final String RISK_ACTION_GUARD_READY = "RISK_ACTION_GUARD_REVIEW_ONLY_READY";
    private static final String RISK_ACTION_GUARD_BACKEND_PENDING_FAIL_CLOSED = "BACKEND_PENDING_FAIL_CLOSED";
    private static final String RISK_ACTION_GUARD_DECISION_MISSING_FAIL_CLOSED = "DECISION_MISSING_FAIL_CLOSED";
    private static final String RISK_ACTION_GUARD_PLAN_BOUNDARY_FAIL_CLOSED = "PLAN_BOUNDARY_FAIL_CLOSED";
    private static final String RISK_ACTION_GUARD_EXECUTION_PLAN_NOT_READY_FAIL_CLOSED = "EXECUTION_PLAN_NOT_READY_FAIL_CLOSED";
    private static final String RISK_ACTION_GUARD_LIQUIDITY_CONTEXT_MISSING_FAIL_CLOSED = "LIQUIDITY_CONTEXT_MISSING_FAIL_CLOSED";
    private static final String RISK_ACTION_GUARD_LIQUIDITY_DETERIORATION_REVIEW_ONLY = "LIQUIDITY_DETERIORATION_REVIEW_ONLY";
    private static final String RISK_ACTION_GUARD_STAMPEDE_REVIEW_ONLY_FAIL_CLOSED = "STAMPEDE_REVIEW_ONLY_FAIL_CLOSED";
    private static final String RISK_ACTION_GUARD_WICK_ONLY_REVIEW_ONLY_FAIL_CLOSED = "WICK_ONLY_REVIEW_ONLY_FAIL_CLOSED";
    private static final String RISK_ACTION_GUARD_HIGH_RISK_REVIEW_ONLY = "HIGH_RISK_REVIEW_ONLY";
    private static final String RISK_ACTION_GUARD_ACTION_FLAGS_BLOCKED_FAIL_CLOSED = "ACTION_FLAGS_BLOCKED_FAIL_CLOSED";
    private static final String RISK_ACTION_GUARD_ACTION_WORDING_BLOCKED_FAIL_CLOSED = "ACTION_WORDING_BLOCKED_FAIL_CLOSED";
    private static final String READ_MODEL_FULL = "FULL";

    private final DecisionService decisionService;
    private final SystemHealthService systemHealthService;
    private final MonitorService monitorService;
    private final RuntimeMetricService runtimeMetricService;
    private final RealMarketEnvironmentService realMarketEnvironmentService;
    private final MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper;
    private final EvidenceService evidenceService;
    private final ScoreService scoreService;
    private final ReviewService reviewService;
    private final ReviewAggregateService reviewAggregateService;
    private final DashboardSourceTraceDetailAdapter dashboardSourceTraceDetailAdapter;
    private final PlanBoundaryDisplayAdapter planBoundaryDisplayAdapter;
    private final ExecutionPlanDisplayAdapter executionPlanDisplayAdapter;
    private final RiskActionGuardDisplayAdapter riskActionGuardDisplayAdapter;
    private final PaperObservationDisplayAdapter paperObservationDisplayAdapter;

    public DashboardController(DecisionService decisionService,
                               SystemHealthService systemHealthService,
                               MonitorService monitorService,
                               RuntimeMetricService runtimeMetricService,
                               RealMarketEnvironmentService realMarketEnvironmentService,
                               MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper,
                               EvidenceService evidenceService,
                               ScoreService scoreService,
                               ReviewService reviewService,
                               ReviewAggregateService reviewAggregateService,
                               DashboardSourceTraceDetailAdapter dashboardSourceTraceDetailAdapter,
                               PlanBoundaryDisplayAdapter planBoundaryDisplayAdapter,
                               ExecutionPlanDisplayAdapter executionPlanDisplayAdapter,
                               RiskActionGuardDisplayAdapter riskActionGuardDisplayAdapter,
                               PaperObservationDisplayAdapter paperObservationDisplayAdapter) {
        this.decisionService = decisionService;
        this.systemHealthService = systemHealthService;
        this.monitorService = monitorService;
        this.runtimeMetricService = runtimeMetricService;
        this.realMarketEnvironmentService = realMarketEnvironmentService;
        this.marketEnvironmentSnapshotMapper = marketEnvironmentSnapshotMapper;
        this.evidenceService = evidenceService;
        this.scoreService = scoreService;
        this.reviewService = reviewService;
        this.reviewAggregateService = reviewAggregateService;
        this.dashboardSourceTraceDetailAdapter = dashboardSourceTraceDetailAdapter;
        this.planBoundaryDisplayAdapter = planBoundaryDisplayAdapter;
        this.executionPlanDisplayAdapter = executionPlanDisplayAdapter;
        this.riskActionGuardDisplayAdapter = riskActionGuardDisplayAdapter;
        this.paperObservationDisplayAdapter = paperObservationDisplayAdapter;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long controllerStart = System.currentTimeMillis();
        long systemStatusStart = System.currentTimeMillis();
        model.addAttribute("systemStatus", decisionService.getLightSystemStatus());
        long systemStatusCostMs = System.currentTimeMillis() - systemStatusStart;
        System.out.println("[PERF] dashboard_system_status=" + systemStatusCostMs + " ms");

        long alertsStart = System.currentTimeMillis();
        model.addAttribute("alerts", monitorService.getRecentAlerts(3));
        long alertsCostMs = System.currentTimeMillis() - alertsStart;
        System.out.println("[PERF] dashboard_alerts=" + alertsCostMs + " ms");

        long decisionsStart = System.currentTimeMillis();
        model.addAttribute("decisions", decisionService.getLatestDecisionResults(DEFAULT_DASHBOARD_SUMMARY_LIMIT));
        long decisionsCostMs = System.currentTimeMillis() - decisionsStart;
        System.out.println("[PERF] dashboard_decisions=" + decisionsCostMs + " ms");

        model.addAttribute("title", "TRINE LOGIC (V1)");

        long controllerCostMs = System.currentTimeMillis() - controllerStart;
        System.out.println("[PERF] dashboard_controller=" + controllerCostMs + " ms");
        return "dashboard";
    }

    @GetMapping("/api/dashboard/refresh")
    @ResponseBody
    public ResponseEntity<DashboardSummaryResponseVO> refreshDashboard() {
        long methodStart = System.currentTimeMillis();
        DashboardSummaryResponseVO body = summaryDashboard(DEFAULT_DASHBOARD_SUMMARY_LIMIT);
        runtimeMetricService.recordDuration("dashboard.refresh", System.currentTimeMillis() - methodStart);
        return ResponseEntity.ok()
                .header(DEPRECATION_HEADER, "true")
                .header("Link", DEPRECATION_LINK)
                .body(body);
    }

    @GetMapping("/api/dashboard/summary")
    @ResponseBody
    public DashboardSummaryResponseVO summaryDashboard(@RequestParam(value = "limit", required = false) Integer limit) {
        long methodStart = System.currentTimeMillis();
        int summaryLimit = normalizeSummaryLimit(limit);
        DashboardSummaryResponseVO body = new DashboardSummaryResponseVO();
        body.setSystemStatus(decisionService.getLightSystemStatus());
        body.setOpenPositionCount(decisionService.countOpenPositions());
        body.setSystemHealth(systemHealthService.getSystemHealth());
        body.setAlerts(monitorService.getRecentAlerts(3));
        body.setDecisions(decisionService.getLatestDecisionResults(summaryLimit));
        runtimeMetricService.recordDuration("dashboard.summary", System.currentTimeMillis() - methodStart);
        return body;
    }

    @GetMapping("/api/dashboard/detail")
    @ResponseBody
    public DashboardDetailResponseVO dashboardDetail(@RequestParam("symbol") String symbol) {
        long methodStart = System.currentTimeMillis();
        String normalizedSymbol = normalizeSymbol(symbol);
        DashboardDetailResponseVO body = DashboardDetailResponseVO.withSafeDefaultDisplays();
        body.setSymbol(normalizedSymbol);
        body.setDecision(decisionService.getLatestDecisionResultBySymbol(normalizedSymbol));
        DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext sourceTraceContext =
                dashboardSourceTraceDetailAdapter != null
                        ? dashboardSourceTraceDetailAdapter.build(normalizedSymbol, body.getDecision())
                        : null;
        SourceTraceDTO sourceTrace = sourceTraceContext != null ? sourceTraceContext.getSourceTrace() : null;
        body.setSourceTrace(sourceTrace);
        body.setRuntimeKlineContext(
                sourceTraceContext != null ? sourceTraceContext.getRuntimeKlineContext() : null
        );
        body.setDerivativesRiskContext(
                sourceTraceContext != null ? sourceTraceContext.getDerivativesRiskContext() : null
        );
        body.setPlanBoundaryDisplay(planBoundaryDisplayAdapter.build(
                normalizedSymbol,
                body.getDecision(),
                body.getPlanBoundaryDisplay()
        ));
        body.setExecutionPlanDisplay(executionPlanDisplayAdapter.build(
                body.getDecision(),
                body.getPlanBoundaryDisplay(),
                body.getExecutionPlanDisplay(),
                sourceTrace
        ));
        body.setRiskActionGuardDisplay(riskActionGuardDisplayAdapter.build(
                body.getDecision(),
                body.getPlanBoundaryDisplay(),
                body.getExecutionPlanDisplay(),
                body.getRiskActionGuardDisplay()
        ));
        body.setPaperObservationDisplay(paperObservationDisplayAdapter.build(
                body.getDecision(),
                body.getPlanBoundaryDisplay(),
                body.getExecutionPlanDisplay(),
                body.getRiskActionGuardDisplay(),
                body.getPaperObservationDisplay()
        ));
        body.setMarketEnvironmentMini(resolveMarketEnvironmentMini(normalizedSymbol, body));
        body.setEvidenceTopItems(resolveEvidenceTopItems(body));
        body.setScoreTopItems(resolveScoreTopItems(body));
        runtimeMetricService.recordDuration("dashboard.detail", System.currentTimeMillis() - methodStart);
        return body;
    }

    @GetMapping("/api/dashboard/evidence-score-status")
    @ResponseBody
    public Map<String, Object> evidenceScoreStatus(@RequestParam("symbol") String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Map<String, Object> status = baseEvidenceScoreStatus(normalizedSymbol);
        DecisionResultVO decision = decisionService.getLatestDecisionResultBySymbol(normalizedSymbol);
        if (decision == null || !hasText(decision.getAnalysisId())) {
            applyEvidenceScoreStatus(
                    status,
                    EVIDENCE_SCORE_BLOCKED_FAIL_CLOSED,
                    "ANALYSIS_CONTEXT_MISSING",
                    "Evidence / Score analysis context 缺失；只读状态 fail-closed，不生成候选、决策、点位或交易信号。",
                    true,
                    "BLOCKED"
            );
            return status;
        }

        String analysisId = decision.getAnalysisId();
        List<EvidenceBriefVO> evidenceRows = resolveEvidenceTopItemsByAnalysisId(analysisId);
        List<ScoreBriefVO> scoreRows = resolveScoreTopItemsByAnalysisId(analysisId);
        boolean evidenceAvailable = !evidenceRows.isEmpty();
        boolean scoreAvailable = !scoreRows.isEmpty();
        boolean sourceTraceComplete = evidenceScoreSourceTraceComplete(evidenceRows, scoreRows);

        status.put("evidenceCount", evidenceRows.size());
        status.put("scoreCount", scoreRows.size());
        status.put("evidenceAvailable", evidenceAvailable);
        status.put("scoreAvailable", scoreAvailable);
        status.put("evidenceTopItems", evidenceRows);
        status.put("scoreTopItems", scoreRows);
        status.put("sourceTraceComplete", sourceTraceComplete);

        if (evidenceAvailable && scoreAvailable && sourceTraceComplete) {
            applyEvidenceScoreStatus(
                    status,
                    EVIDENCE_SCORE_READY,
                    "EVIDENCE_SCORE_OWNER_PATH_READ",
                    "Evidence / Score 只读状态可读；不是 Candidate、Decision、Point 或交易信号。",
                    false,
                    "OK"
            );
        } else if (evidenceAvailable && scoreAvailable) {
            applyEvidenceScoreStatus(
                    status,
                    EVIDENCE_SCORE_SOURCE_TRACE_PARTIAL,
                    "SOURCE_TRACE_PARTIAL",
                    "Evidence / Score 来源追踪不完整；仅显示摘要，不作为候选、决策、点位或交易信号。",
                    true,
                    "PARTIAL"
            );
        } else if (!evidenceAvailable && scoreAvailable) {
            applyEvidenceScoreStatus(
                    status,
                    EVIDENCE_MISSING_FAIL_CLOSED,
                    "EVIDENCE_MISSING",
                    "Evidence 缺失；Score 不能作为候选排序、决策或点位依据。",
                    true,
                    "MISSING"
            );
        } else if (evidenceAvailable) {
            applyEvidenceScoreStatus(
                    status,
                    SCORE_MISSING_FAIL_CLOSED,
                    "SCORE_MISSING",
                    "Score 缺失；Evidence / Score 链路对 Candidate、Decision、Point、Push 和交易保持 fail-closed。",
                    true,
                    "MISSING"
            );
        } else {
            applyEvidenceScoreStatus(
                    status,
                    EVIDENCE_SCORE_INCOMPLETE_FAIL_CLOSED,
                    "EVIDENCE_AND_SCORE_MISSING",
                    "Evidence / Score 缺失或不完整；只读展示，候选、决策、点位和交易全部关闭。",
                    true,
                    "MISSING"
            );
        }
        return status;
    }

    @GetMapping("/api/dashboard/decision-result-status")
    @ResponseBody
    public Map<String, Object> decisionResultStatus(@RequestParam("symbol") String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Map<String, Object> status = baseDecisionResultStatus(normalizedSymbol);
        DecisionResultVO decision = decisionService.getLatestDecisionResultBySymbol(normalizedSymbol);
        if (decision == null) {
            applyDecisionResultStatus(
                    status,
                    DECISIONRESULT_MISSING_FAIL_CLOSED,
                    "DECISIONRESULT_MISSING",
                    "DecisionResult 缺失；只读状态 fail-closed，不生成候选、决策、点位、Push 或交易信号。",
                    true,
                    "MISSING"
            );
            return status;
        }

        boolean aiRoleResultsAvailable = hasText(decision.getAiRoleResults());
        boolean sourceTraceComplete = decisionResultSourceTraceComplete(decision);
        status.put("analysisId", decision.getAnalysisId());
        status.put("decisionAvailable", true);
        status.put("decisionStatus", hasText(decision.getReadModelTruthStatus())
                ? decision.getReadModelTruthStatus()
                : "UNKNOWN");
        status.put("confidence", decision.getConfidenceLevel());
        status.put("aiRoleResultsAvailable", aiRoleResultsAvailable);
        status.put("aiRoleResultsSummary", aiRoleResultsAvailable
                ? "available; raw read-model context hidden from review-only status"
                : "missing");
        status.put("sourceTraceComplete", sourceTraceComplete);

        if (decision.getCreateTime() == null || !hasText(decision.getReadModelTruthStatus())) {
            applyDecisionResultStatus(
                    status,
                    DECISIONRESULT_STALE_OR_UNKNOWN_FAIL_CLOSED,
                    "DECISIONRESULT_STALE_OR_UNKNOWN",
                    "DecisionResult 最新性或 read-model 完整性未知；只读状态 fail-closed。",
                    true,
                    "UNKNOWN"
            );
        } else if (!READ_MODEL_FULL.equalsIgnoreCase(decision.getReadModelTruthStatus())
                || hasText(decision.getReadModelFallbackReason())) {
            applyDecisionResultStatus(
                    status,
                    DECISIONRESULT_READ_MODEL_PARTIAL,
                    firstNonBlank(decision.getReadModelFallbackReason(), "READ_MODEL_PARTIAL"),
                    "DecisionResult read model 不完整；仅显示状态，不作为候选、决策生成、点位或交易信号。",
                    true,
                    "PARTIAL"
            );
        } else if (!sourceTraceComplete) {
            applyDecisionResultStatus(
                    status,
                    DECISIONRESULT_SOURCE_TRACE_PARTIAL,
                    "SOURCE_TRACE_PARTIAL",
                    "DecisionResult source trace 不完整；只读展示，不能升级为点位或执行建议。",
                    true,
                    "PARTIAL"
            );
        } else if (!aiRoleResultsAvailable) {
            applyDecisionResultStatus(
                    status,
                    DECISIONRESULT_AI_ROLE_PARTIAL,
                    "AI_ROLE_RESULTS_MISSING",
                    "ai_role_results 缺失或不可用；不是 Three AI 裁决，也不是新的 Decision generation。",
                    true,
                    "PARTIAL"
            );
        } else {
            applyDecisionResultStatus(
                    status,
                    DECISIONRESULT_READY,
                    "DECISIONRESULT_OWNER_PATH_READ",
                    "DecisionResult 只读状态可读；这是已有 read model，不是新的决策生成或交易信号。",
                    false,
                    "OK"
            );
        }
        return status;
    }

    @GetMapping("/api/dashboard/execution-plan-boundary-status")
    @ResponseBody
    public Map<String, Object> executionPlanBoundaryStatus(@RequestParam("symbol") String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Map<String, Object> status = baseExecutionPlanBoundaryStatus(normalizedSymbol);
        DashboardDetailResponseVO detail = dashboardDetail(normalizedSymbol);
        DecisionResultVO decision = detail.getDecision();
        DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundary = detail.getPlanBoundaryDisplay();
        DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlan = detail.getExecutionPlanDisplay();
        DashboardDetailResponseVO.RiskActionGuardDisplayVO riskGuard = detail.getRiskActionGuardDisplay();
        SourceTraceDTO sourceTrace = detail.getSourceTrace();

        status.put("analysisId", decision != null ? decision.getAnalysisId() : null);
        status.put("planBoundaryStatus", firstNonBlank(
                planBoundary != null ? planBoundary.getPlanBoundaryStatus() : null,
                "BACKEND_PENDING"
        ));
        status.put("executionPlanStatus", firstNonBlank(
                executionPlan != null ? executionPlan.getExecutionPlanStatus() : null,
                "BOUNDARY_PENDING"
        ));
        status.put("sourceTraceStatus", resolveExecutionPlanSourceTraceStatus(planBoundary, sourceTrace));
        status.put("sourceTraceComplete", executionPlanSourceTraceComplete(sourceTrace));
        status.put("riskActionGuardStatus", firstNonBlank(
                riskGuard != null ? riskGuard.getRiskActionGuardStatus() : null,
                "BACKEND_PENDING"
        ));
        status.put("notExecutableReason", firstNonBlank(
                executionPlan != null ? executionPlan.getNotExecutableReason() : null,
                "PLAN_BOUNDARY_BACKEND_PENDING"
        ));
        status.put("incompleteReasons", safeReasons(executionPlan != null ? executionPlan.getIncompleteReasons() : null));
        status.put("blockingReasons", safeReasons(planBoundary != null ? planBoundary.getBlockingReasons() : null));

        applyExecutionPlanBoundaryStatus(status, detail, planBoundary, executionPlan, riskGuard, sourceTrace);
        return status;
    }

    @GetMapping("/api/dashboard/risk-action-guard-status")
    @ResponseBody
    public Map<String, Object> riskActionGuardStatus(@RequestParam("symbol") String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Map<String, Object> status = baseRiskActionGuardStatus(normalizedSymbol);

        DecisionResultVO decision = decisionService.getLatestDecisionResultBySymbol(normalizedSymbol);
        DashboardDetailResponseVO detail = DashboardDetailResponseVO.withSafeDefaultDisplays();
        detail.setSymbol(normalizedSymbol);
        detail.setDecision(decision);

        DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundary = planBoundaryDisplayAdapter.build(
                normalizedSymbol,
                decision,
                detail.getPlanBoundaryDisplay()
        );
        DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlan = executionPlanDisplayAdapter.build(
                decision,
                planBoundary,
                detail.getExecutionPlanDisplay(),
                null
        );
        DashboardDetailResponseVO.RiskActionGuardDisplayVO riskGuard = riskActionGuardDisplayAdapter.build(
                decision,
                planBoundary,
                executionPlan,
                detail.getRiskActionGuardDisplay()
        );

        status.put("analysisId", decision != null ? decision.getAnalysisId() : null);
        status.put("riskActionGuardStatus", firstNonBlank(
                riskGuard != null ? riskGuard.getRiskActionGuardStatus() : null,
                "BACKEND_PENDING"
        ));
        status.put("riskActionGuardStatusLabel", firstNonBlank(
                riskGuard != null ? riskGuard.getRiskActionGuardStatusLabel() : null,
                "后端未接入"
        ));
        status.put("riskActionAdviceSummary", safeRiskActionAdviceSummary(
                riskGuard != null ? riskGuard.getRiskActionAdvice() : null
        ));
        status.put("riskActionBlockingReason", firstNonBlank(
                riskGuard != null ? riskGuard.getRiskActionBlockingReason() : null,
                "BACKEND_PENDING"
        ));
        status.put("liquidityState", firstNonBlank(
                riskGuard != null ? riskGuard.getLiquidityState() : null,
                "BACKEND_PENDING"
        ));
        status.put("stampedeDetected", riskGuard != null && Boolean.TRUE.equals(riskGuard.getStampedeDetected()));
        status.put("wickOnlyRisk", riskGuard != null && Boolean.TRUE.equals(riskGuard.getWickOnlyRisk()));
        status.put("manualRiskReviewRequired", riskGuard == null || Boolean.TRUE.equals(riskGuard.getManualRiskReviewRequired()));
        status.put("actionFlagsAllFalse", riskActionGuardActionFlagsAllFalse(riskGuard));
        status.put("planBoundaryStatus", firstNonBlank(
                planBoundary != null ? planBoundary.getPlanBoundaryStatus() : null,
                "BACKEND_PENDING"
        ));
        status.put("executionPlanStatus", firstNonBlank(
                executionPlan != null ? executionPlan.getExecutionPlanStatus() : null,
                "BOUNDARY_PENDING"
        ));

        applyRiskActionGuardStatus(status, decision, planBoundary, executionPlan, riskGuard);
        return status;
    }

    @GetMapping("/api/dashboard/review-replay-result-status")
    @ResponseBody
    public Map<String, Object> reviewReplayResultStatus(
            @RequestParam(value = "analysisId", required = false) String analysisId,
            @RequestParam(value = "symbol", required = false) String symbol) {
        String normalizedSymbol = hasText(symbol) ? normalizeSymbol(symbol) : null;
        String normalizedAnalysisId = normalizeAnalysisIdOrNull(analysisId);
        DecisionResultVO decision = null;
        if (!hasText(normalizedAnalysisId) && hasText(normalizedSymbol)) {
            decision = decisionService.getLatestDecisionResultBySymbol(normalizedSymbol);
            normalizedAnalysisId = decision != null ? normalizeAnalysisIdOrNull(decision.getAnalysisId()) : null;
        }

        Map<String, Object> status = baseReviewReplayStatus(normalizedAnalysisId, normalizedSymbol);
        if (!hasText(normalizedAnalysisId)) {
            applyReviewReplayStatus(
                    status,
                    REVIEW_REPLAY_BLOCKED_FAIL_CLOSED,
                    "ANALYSIS_CONTEXT_MISSING",
                    "Review / Replay analysis context 缺失；只读状态 fail-closed，不触发回放执行或生成复盘结果。",
                    true,
                    "BLOCKED"
            );
            return status;
        }

        if (decision == null && hasText(normalizedSymbol)) {
            decision = decisionService.getLatestDecisionResultBySymbol(normalizedSymbol);
        }
        if (!hasText(normalizedSymbol) && decision != null) {
            normalizedSymbol = decision.getSymbol();
            status.put("symbol", normalizedSymbol);
        }

        ReviewStateVO reviewState = reviewService != null
                ? reviewService.getStateByAnalysisId(normalizedAnalysisId)
                : null;
        status.put("reviewResultAvailable", reviewState != null);
        status.put("reviewUpdatedAt", reviewState != null ? reviewState.getUpdateTime() : null);
        status.put("reviewErrorType", reviewState != null ? firstNonBlank(reviewState.getErrorType(), "REVIEW_RESULT_PRESENT") : null);
        if (reviewState == null) {
            applyReviewReplayStatus(
                    status,
                    REVIEW_RESULT_MISSING_FAIL_CLOSED,
                    "REVIEW_RESULT_MISSING",
                    "Review result 缺失；不伪造复盘结果，不触发 replay execution。",
                    true,
                    "MISSING"
            );
            return status;
        }

        Optional<ReviewAggregateSummaryVO> aggregate = reviewAggregateService != null
                ? reviewAggregateService.getAggregateSummaryByAnalysisId(normalizedAnalysisId)
                : Optional.empty();
        status.put("reviewAggregateAvailable", aggregate.isPresent());
        if (aggregate.isEmpty()) {
            applyReviewReplayStatus(
                    status,
                    REVIEW_AGGREGATE_MISSING_FAIL_CLOSED,
                    "REVIEW_AGGREGATE_MISSING",
                    "Review aggregate 缺失；不伪造聚合摘要，不触发回放执行。",
                    true,
                    "MISSING"
            );
            return status;
        }

        ReviewAggregateSummaryVO summary = aggregate.get();
        if (!hasText(normalizedSymbol) && summary.getRun() != null && hasText(summary.getRun().getSymbol())) {
            normalizedSymbol = summary.getRun().getSymbol();
            status.put("symbol", normalizedSymbol);
        }
        ReviewAggregateSummaryVO.DetailSectionMeta replayMeta = replaySummaryMeta(summary);
        boolean reviewClosureAvailable = summary.getReviewClosure() != null;
        boolean replaySummaryOwnerPresent = replayMeta != null;
        int replaySummaryCount = replayMeta != null && replayMeta.getTotal() != null ? replayMeta.getTotal() : 0;
        status.put("reviewClosureAvailable", reviewClosureAvailable);
        status.put("replaySummaryAvailable", replaySummaryOwnerPresent && replaySummaryCount > 0);
        status.put("replaySummaryCount", replaySummaryCount);
        status.put("replaySummaryRecommendedLimit", replayMeta != null ? replayMeta.getRecommendedLimit() : null);
        status.put("sourceTraceComplete", reviewReplaySourceTraceComplete(summary, reviewState, replayMeta));

        if (!replaySummaryOwnerPresent) {
            applyReviewReplayStatus(
                    status,
                    REPLAY_EXECUTION_BLOCKED_FAIL_CLOSED,
                    "REPLAY_SUMMARY_OWNER_PATH_MISSING",
                    "Replay summary owner path 缺失；状态只读，不执行 replay，不生成复盘结果。",
                    true,
                    "BLOCKED"
            );
        } else if (replaySummaryCount <= 0) {
            applyReviewReplayStatus(
                    status,
                    REPLAY_SUMMARY_MISSING_FAIL_CLOSED,
                    "REPLAY_SUMMARY_MISSING",
                    "Replay summary 缺失；不触发 replay execution，不伪造摘要。",
                    true,
                    "MISSING"
            );
        } else if (!reviewClosureAvailable || !reviewReplaySourceTraceComplete(summary, reviewState, replayMeta)) {
            applyReviewReplayStatus(
                    status,
                    REVIEW_REPLAY_SOURCE_TRACE_PARTIAL,
                    "REVIEW_REPLAY_SOURCE_TRACE_PARTIAL",
                    "Review / Replay source trace 不完整；仅展示只读状态，不生成交易结论。",
                    true,
                    "PARTIAL"
            );
        } else {
            applyReviewReplayStatus(
                    status,
                    REVIEW_REPLAY_READY,
                    "REVIEW_REPLAY_OWNER_PATH_READ",
                    "Review / Replay result status 只读可读；不触发回放执行，不生成复盘结果或交易信号。",
                    false,
                    "OK"
            );
        }
        return status;
    }

    @GetMapping("/api/dashboard/data-source-health-status")
    @ResponseBody
    public Map<String, Object> dataSourceHealthStatus(@RequestParam("symbol") String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Map<String, Object> status = baseDataSourceHealthStatus(normalizedSymbol);
        List<Map<String, Object>> sourceStatuses = new ArrayList<>();

        sourceStatuses.add(sourceStatus(
                "MarketQuote",
                "/api/market/quote-status",
                "MISSING",
                "MARKETQUOTE_STATUS_SURFACE_NOT_INVOKED",
                "MarketQuote status endpoint 作为上游边界存在；Data Source Health 聚合只读，不触发行情刷新或 API client。"
        ));
        sourceStatuses.add(sourceStatusFromPayload(
                "Evidence / Score",
                "/api/dashboard/evidence-score-status",
                evidenceScoreStatus(normalizedSymbol)
        ));
        sourceStatuses.add(sourceStatusFromPayload(
                "DecisionResult",
                "/api/dashboard/decision-result-status",
                decisionResultStatus(normalizedSymbol)
        ));
        sourceStatuses.add(sourceStatus(
                "ExecutionPlan / BoundaryCandidate",
                "/api/dashboard/execution-plan-boundary-status",
                "WATCH_ONLY",
                "EXECUTIONPLAN_BOUNDARY_STATUS_SURFACE_NOT_INVOKED",
                "ExecutionPlan / BoundaryCandidate status surface 已存在；Data Source Health 聚合不调用 dashboard detail，避免触发外部行情上下文读取。"
        ));
        sourceStatuses.add(sourceStatusFromPayload(
                "Review / Replay",
                "/api/dashboard/review-replay-result-status",
                reviewReplayResultStatus(null, normalizedSymbol)
        ));

        applyDataSourceHealthRollup(status, sourceStatuses);
        return status;
    }

    private List<EvidenceBriefVO> resolveEvidenceTopItems(DashboardDetailResponseVO body) {
        if (body == null || body.getDecision() == null || evidenceService == null) {
            return Collections.emptyList();
        }
        String analysisId = body.getDecision().getAnalysisId();
        return resolveEvidenceTopItemsByAnalysisId(analysisId);
    }

    private List<EvidenceBriefVO> resolveEvidenceTopItemsByAnalysisId(String analysisId) {
        if (!hasText(analysisId) || evidenceService == null) {
            return Collections.emptyList();
        }
        List<EvidenceBriefVO> rows = evidenceService.listTopEvidenceBriefByAnalysisId(analysisId);
        return rows != null ? rows : Collections.emptyList();
    }

    private List<ScoreBriefVO> resolveScoreTopItems(DashboardDetailResponseVO body) {
        if (body == null || body.getDecision() == null || scoreService == null) {
            return Collections.emptyList();
        }
        String analysisId = body.getDecision().getAnalysisId();
        return resolveScoreTopItemsByAnalysisId(analysisId);
    }

    private List<ScoreBriefVO> resolveScoreTopItemsByAnalysisId(String analysisId) {
        if (!hasText(analysisId) || scoreService == null) {
            return Collections.emptyList();
        }
        List<ScoreBriefVO> rows = scoreService.listTopScoreBriefByAnalysisId(analysisId);
        return rows != null ? rows : Collections.emptyList();
    }

    private Map<String, Object> baseEvidenceScoreStatus(String normalizedSymbol) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", EVIDENCE_SCORE_INCOMPLETE_FAIL_CLOSED);
        status.put("symbol", normalizedSymbol);
        status.put("evidenceCount", 0);
        status.put("scoreCount", 0);
        status.put("evidenceAvailable", false);
        status.put("scoreAvailable", false);
        status.put("evidenceTopItems", Collections.emptyList());
        status.put("scoreTopItems", Collections.emptyList());
        status.put("sourceTraceComplete", false);
        status.put("sourceHealth", "MISSING");
        status.put("reason", "EVIDENCE_SCORE_STATUS_PENDING");
        status.put("message", "Evidence / Score 只读状态待确认；不是交易信号。");
        status.put("reviewOnly", true);
        status.put("notTradingSignal", true);
        status.put("notCandidateSignal", true);
        status.put("notDecisionSignal", true);
        status.put("notPointSignal", true);
        status.put("watchlistBounded", true);
        status.put("marketQuoteChecked", true);
        status.put("displaySlotsAreCandidatePool", false);
        status.put("failClosed", true);
        return status;
    }

    private Map<String, Object> baseDecisionResultStatus(String normalizedSymbol) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", DECISIONRESULT_BLOCKED_FAIL_CLOSED);
        status.put("symbol", normalizedSymbol);
        status.put("analysisId", null);
        status.put("decisionAvailable", false);
        status.put("decisionStatus", "UNKNOWN");
        status.put("confidence", null);
        status.put("aiRoleResultsAvailable", false);
        status.put("aiRoleResultsSummary", "missing");
        status.put("sourceTraceComplete", false);
        status.put("sourceHealth", "BLOCKED");
        status.put("reason", "DECISIONRESULT_STATUS_PENDING");
        status.put("message", "DecisionResult 只读状态待确认；不是交易信号。");
        status.put("reviewOnly", true);
        status.put("notTradingSignal", true);
        status.put("notCandidateSignal", true);
        status.put("notDecisionGeneration", true);
        status.put("notPointSignal", true);
        status.put("watchlistBounded", true);
        status.put("marketQuoteChecked", true);
        status.put("evidenceScoreChecked", true);
        status.put("displaySlotsAreCandidatePool", false);
        status.put("failClosed", true);
        return status;
    }

    private Map<String, Object> baseExecutionPlanBoundaryStatus(String normalizedSymbol) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", EXECUTIONPLAN_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("symbol", normalizedSymbol);
        status.put("analysisId", null);
        status.put("planBoundaryStatus", "BACKEND_PENDING");
        status.put("executionPlanStatus", "BOUNDARY_PENDING");
        status.put("sourceTraceStatus", "UNKNOWN");
        status.put("sourceTraceComplete", false);
        status.put("sourceHealth", "BLOCKED");
        status.put("riskActionGuardStatus", "BACKEND_PENDING");
        status.put("notExecutableReason", "PLAN_BOUNDARY_BACKEND_PENDING");
        status.put("incompleteReasons", Collections.emptyList());
        status.put("blockingReasons", Collections.emptyList());
        status.put("reason", "EXECUTIONPLAN_BOUNDARY_STATUS_PENDING");
        status.put("message", "ExecutionPlan / BoundaryCandidate 只读状态待确认；不是交易信号。");
        status.put("reviewOnly", true);
        status.put("notTradingSignal", true);
        status.put("notCandidateSignal", true);
        status.put("notDecisionGeneration", true);
        status.put("notPointSignal", true);
        status.put("notExecutable", true);
        status.put("watchlistBounded", true);
        status.put("marketQuoteChecked", true);
        status.put("evidenceScoreChecked", true);
        status.put("decisionResultChecked", true);
        status.put("displaySlotsAreCandidatePool", false);
        status.put("failClosed", true);
        return status;
    }

    private Map<String, Object> baseRiskActionGuardStatus(String normalizedSymbol) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", RISK_ACTION_GUARD_BACKEND_PENDING_FAIL_CLOSED);
        status.put("symbol", normalizedSymbol);
        status.put("analysisId", null);
        status.put("riskActionGuardStatus", "BACKEND_PENDING");
        status.put("riskActionGuardStatusLabel", "后端未接入");
        status.put("riskActionAdviceSummary", "RiskActionGuard 只读状态待确认；仅人工复核，不是交易信号。");
        status.put("riskActionBlockingReason", "BACKEND_PENDING");
        status.put("liquidityState", "BACKEND_PENDING");
        status.put("stampedeDetected", false);
        status.put("wickOnlyRisk", false);
        status.put("manualRiskReviewRequired", true);
        status.put("actionFlagsAllFalse", false);
        status.put("planBoundaryStatus", "BACKEND_PENDING");
        status.put("executionPlanStatus", "BOUNDARY_PENDING");
        status.put("sourceTraceComplete", false);
        status.put("sourceHealth", "BLOCKED");
        status.put("reason", "RISK_ACTION_GUARD_STATUS_PENDING");
        status.put("message", "RiskActionGuard 只读状态待确认；仅人工复核，不是交易信号、候选、决策生成、点位或可执行动作。");
        status.put("reviewOnly", true);
        status.put("manualReviewOnly", true);
        status.put("notTradingSignal", true);
        status.put("notCandidateSignal", true);
        status.put("notDecisionGeneration", true);
        status.put("notPointSignal", true);
        status.put("notExecutable", true);
        status.put("notPositionMonitorExecution", true);
        status.put("notExecutionPlanGeneration", true);
        status.put("notBoundaryCandidateGeneration", true);
        status.put("externalRefreshTriggered", false);
        status.put("displaySlotsAreCandidatePool", false);
        status.put("failClosed", true);
        return status;
    }

    private Map<String, Object> baseReviewReplayStatus(String analysisId, String normalizedSymbol) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", REVIEW_REPLAY_BLOCKED_FAIL_CLOSED);
        status.put("symbol", normalizedSymbol);
        status.put("analysisId", analysisId);
        status.put("reviewResultAvailable", false);
        status.put("reviewAggregateAvailable", false);
        status.put("reviewClosureAvailable", false);
        status.put("replaySummaryAvailable", false);
        status.put("replaySummaryCount", 0);
        status.put("replaySummaryRecommendedLimit", null);
        status.put("reviewUpdatedAt", null);
        status.put("reviewErrorType", null);
        status.put("sourceTraceComplete", false);
        status.put("sourceHealth", "BLOCKED");
        status.put("reason", "REVIEW_REPLAY_STATUS_PENDING");
        status.put("message", "Review / Replay result status 只读状态待确认；不是交易信号。");
        status.put("reviewOnly", true);
        status.put("notTradingSignal", true);
        status.put("notCandidateSignal", true);
        status.put("notDecisionGeneration", true);
        status.put("notPointSignal", true);
        status.put("notReplayExecution", true);
        status.put("notExecutable", true);
        status.put("watchlistBounded", true);
        status.put("marketQuoteChecked", true);
        status.put("evidenceScoreChecked", true);
        status.put("decisionResultChecked", true);
        status.put("executionPlanBoundaryChecked", true);
        status.put("displaySlotsAreCandidatePool", false);
        status.put("failClosed", true);
        return status;
    }

    private Map<String, Object> baseDataSourceHealthStatus(String normalizedSymbol) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", DATA_SOURCE_HEALTH_MISSING_FAIL_CLOSED);
        status.put("symbol", normalizedSymbol);
        status.put("sourceHealth", "MISSING");
        status.put("scopedSources", List.of(
                "MarketQuote",
                "Evidence / Score",
                "DecisionResult",
                "ExecutionPlan / BoundaryCandidate",
                "Review / Replay"
        ));
        status.put("sourceStatuses", Collections.emptyList());
        status.put("okSources", Collections.emptyList());
        status.put("partialSources", Collections.emptyList());
        status.put("staleSources", Collections.emptyList());
        status.put("missingSources", Collections.emptyList());
        status.put("watchOnlySources", Collections.emptyList());
        status.put("blockedSources", Collections.emptyList());
        status.put("reason", "DATA_SOURCE_HEALTH_STATUS_PENDING");
        status.put("message", "Data Source Health 只读状态待确认；不触发外部刷新、采集、回放或交易动作。");
        status.put("reviewOnly", true);
        status.put("notTradingSignal", true);
        status.put("notCandidateSignal", true);
        status.put("notDecisionGeneration", true);
        status.put("notPointSignal", true);
        status.put("notReplayExecution", true);
        status.put("notExecutable", true);
        status.put("watchlistBounded", true);
        status.put("marketQuoteChecked", true);
        status.put("evidenceScoreChecked", true);
        status.put("decisionResultChecked", true);
        status.put("executionPlanBoundaryChecked", true);
        status.put("reviewReplayChecked", true);
        status.put("externalRefreshTriggered", false);
        status.put("displaySlotsAreCandidatePool", false);
        status.put("failClosed", true);
        return status;
    }

    private void applyEvidenceScoreStatus(Map<String, Object> status,
                                          String statusValue,
                                          String reason,
                                          String message,
                                          boolean failClosed,
                                          String sourceHealth) {
        status.put("status", statusValue);
        status.put("reason", reason);
        status.put("message", message);
        status.put("failClosed", failClosed);
        status.put("sourceHealth", sourceHealth);
    }

    private void applyDecisionResultStatus(Map<String, Object> status,
                                           String statusValue,
                                           String reason,
                                           String message,
                                           boolean failClosed,
                                           String sourceHealth) {
        status.put("status", statusValue);
        status.put("reason", reason);
        status.put("message", message);
        status.put("failClosed", failClosed);
        status.put("sourceHealth", sourceHealth);
    }

    private void applyRiskActionGuardStatus(Map<String, Object> status,
                                            DecisionResultVO decision,
                                            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundary,
                                            DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlan,
                                            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskGuard) {
        if (decision == null) {
            applyRiskActionGuardStatus(
                    status,
                    RISK_ACTION_GUARD_DECISION_MISSING_FAIL_CLOSED,
                    "DECISION_MISSING",
                    "DecisionResult 缺失；RiskActionGuard 只读状态 fail-closed。",
                    true,
                    "BLOCKED"
            );
            return;
        }
        if (riskGuard == null) {
            applyRiskActionGuardStatus(
                    status,
                    RISK_ACTION_GUARD_BACKEND_PENDING_FAIL_CLOSED,
                    "RISK_ACTION_GUARD_DISPLAY_MISSING",
                    "RiskActionGuard display owner data 缺失；只读状态 fail-closed。",
                    true,
                    "MISSING"
            );
            return;
        }

        String riskStatus = normalizedStatus(riskGuard.getRiskActionGuardStatus());
        String blockingReason = normalizedStatus(riskGuard.getRiskActionBlockingReason());
        String planStatus = normalizedStatus(planBoundary != null ? planBoundary.getPlanBoundaryStatus() : null);
        String executionStatus = normalizedStatus(executionPlan != null ? executionPlan.getExecutionPlanStatus() : null);

        if (!riskActionGuardActionFlagsAllFalse(riskGuard)) {
            applyRiskActionGuardStatus(
                    status,
                    RISK_ACTION_GUARD_ACTION_FLAGS_BLOCKED_FAIL_CLOSED,
                    "RISK_ACTION_GUARD_ACTION_FLAGS_TRUE",
                    "RiskActionGuard action flags 出现 true；已按只读 fail-closed 处理，不输出可执行动作。",
                    true,
                    "BLOCKED"
            );
        } else if (hasUnsafeActionWording(riskGuard.getRiskActionAdvice())) {
            applyRiskActionGuardStatus(
                    status,
                    RISK_ACTION_GUARD_ACTION_WORDING_BLOCKED_FAIL_CLOSED,
                    "RISK_ACTION_GUARD_ACTION_WORDING_UNSAFE",
                    "RiskActionGuard advice 包含未加防护的动作措辞；已按只读 fail-closed 处理。",
                    true,
                    "BLOCKED"
            );
        } else if ("PLAN_BOUNDARY_NOT_VALID".equals(blockingReason)
                || (!"VALID".equals(planStatus) && hasText(planStatus))) {
            applyRiskActionGuardStatus(
                    status,
                    RISK_ACTION_GUARD_PLAN_BOUNDARY_FAIL_CLOSED,
                    firstNonBlank(riskGuard.getRiskActionBlockingReason(), "PLAN_BOUNDARY_NOT_VALID"),
                    "PlanBoundary 未满足只读复核边界；RiskActionGuard fail-closed。",
                    true,
                    "BLOCKED"
            );
        } else if ("EXECUTION_PLAN_NOT_READY".equals(blockingReason)
                || (!"READY_REVIEW_ONLY".equals(executionStatus) && hasText(executionStatus))) {
            applyRiskActionGuardStatus(
                    status,
                    RISK_ACTION_GUARD_EXECUTION_PLAN_NOT_READY_FAIL_CLOSED,
                    firstNonBlank(riskGuard.getRiskActionBlockingReason(), "EXECUTION_PLAN_NOT_READY"),
                    "ExecutionPlan 未处于只读可复核状态；RiskActionGuard fail-closed。",
                    true,
                    "BLOCKED"
            );
        } else if ("LIQUIDITY_CONTEXT_MISSING".equals(blockingReason)) {
            applyRiskActionGuardStatus(
                    status,
                    RISK_ACTION_GUARD_LIQUIDITY_CONTEXT_MISSING_FAIL_CLOSED,
                    "LIQUIDITY_CONTEXT_MISSING",
                    "流动性上下文缺失；RiskActionGuard 只读状态 fail-closed。",
                    true,
                    "MISSING"
            );
        } else if ("LIQUIDITY_DETERIORATION_REVIEW_ONLY".equals(blockingReason)) {
            applyRiskActionGuardStatus(
                    status,
                    RISK_ACTION_GUARD_LIQUIDITY_DETERIORATION_REVIEW_ONLY,
                    "LIQUIDITY_DETERIORATION_REVIEW_ONLY",
                    "流动性恶化仅允许人工复核；不生成可执行动作。",
                    true,
                    "PARTIAL"
            );
        } else if ("STAMPEDE_REVIEW_ONLY".equals(blockingReason)) {
            applyRiskActionGuardStatus(
                    status,
                    RISK_ACTION_GUARD_STAMPEDE_REVIEW_ONLY_FAIL_CLOSED,
                    "STAMPEDE_REVIEW_ONLY",
                    "踩踏风险仅允许人工复核；RiskActionGuard fail-closed。",
                    true,
                    "BLOCKED"
            );
        } else if ("WICK_ONLY_REVIEW_ONLY".equals(blockingReason)) {
            applyRiskActionGuardStatus(
                    status,
                    RISK_ACTION_GUARD_WICK_ONLY_REVIEW_ONLY_FAIL_CLOSED,
                    "WICK_ONLY_REVIEW_ONLY",
                    "短线插针风险仅允许人工复核；RiskActionGuard fail-closed。",
                    true,
                    "BLOCKED"
            );
        } else if ("HIGH_RISK_REVIEW_ONLY".equals(blockingReason)) {
            applyRiskActionGuardStatus(
                    status,
                    RISK_ACTION_GUARD_HIGH_RISK_REVIEW_ONLY,
                    "HIGH_RISK_REVIEW_ONLY",
                    "高风险状态仅显示人工复核提醒；不是交易动作。",
                    true,
                    "PARTIAL"
            );
        } else if (!hasText(riskStatus) || "BACKEND_PENDING".equals(riskStatus)) {
            applyRiskActionGuardStatus(
                    status,
                    RISK_ACTION_GUARD_BACKEND_PENDING_FAIL_CLOSED,
                    "BACKEND_PENDING",
                    "RiskActionGuard owner path 仍是后端待接入；只读状态 fail-closed。",
                    true,
                    "MISSING"
            );
        } else {
            applyRiskActionGuardStatus(
                    status,
                    RISK_ACTION_GUARD_READY,
                    firstNonBlank(riskGuard.getRiskActionBlockingReason(), "MANUAL_REVIEW_REQUIRED"),
                    "RiskActionGuard 只读状态可读；仅人工复核，不是交易信号、候选、决策生成、点位或可执行动作。",
                    false,
                    "OK"
            );
        }
    }

    private void applyRiskActionGuardStatus(Map<String, Object> status,
                                            String statusValue,
                                            String reason,
                                            String message,
                                            boolean failClosed,
                                            String sourceHealth) {
        status.put("status", statusValue);
        status.put("reason", reason);
        status.put("message", message);
        status.put("failClosed", failClosed);
        status.put("sourceHealth", sourceHealth);
    }

    private void applyExecutionPlanBoundaryStatus(Map<String, Object> status,
                                                  DashboardDetailResponseVO detail,
                                                  DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundary,
                                                  DashboardDetailResponseVO.ExecutionPlanDisplayVO executionPlan,
                                                  DashboardDetailResponseVO.RiskActionGuardDisplayVO riskGuard,
                                                  SourceTraceDTO sourceTrace) {
        if (detail == null || detail.getDecision() == null) {
            applyExecutionPlanBoundaryStatus(
                    status,
                    EXECUTIONPLAN_BOUNDARY_BLOCKED_FAIL_CLOSED,
                    "DECISIONRESULT_MISSING",
                    "DecisionResult 缺失；ExecutionPlan / BoundaryCandidate 只读状态 fail-closed。",
                    true,
                    "BLOCKED"
            );
            return;
        }

        String planStatus = normalizedStatus(planBoundary != null ? planBoundary.getPlanBoundaryStatus() : null);
        String executionStatus = normalizedStatus(executionPlan != null ? executionPlan.getExecutionPlanStatus() : null);
        String riskStatus = normalizedStatus(riskGuard != null ? riskGuard.getRiskActionGuardStatus() : null);

        if (!hasText(planStatus) || "BACKEND_PENDING".equals(planStatus)) {
            applyExecutionPlanBoundaryStatus(
                    status,
                    PLAN_BOUNDARY_BACKEND_PENDING_FAIL_CLOSED,
                    "PLAN_BOUNDARY_BACKEND_PENDING",
                    "PlanBoundary owner path 仍是后端待接入；只读状态 fail-closed。",
                    true,
                    "MISSING"
            );
        } else if (!executionPlanSourceTraceComplete(sourceTrace)) {
            applyExecutionPlanBoundaryStatus(
                    status,
                    EXECUTIONPLAN_SOURCE_TRACE_PARTIAL,
                    "SOURCE_TRACE_PARTIAL",
                    "ExecutionPlan / BoundaryCandidate source trace 不完整；只读展示，不生成点位或交易信号。",
                    true,
                    "PARTIAL"
            );
        } else if ("INCOMPLETE".equals(planStatus)) {
            applyExecutionPlanBoundaryStatus(
                    status,
                    PLAN_BOUNDARY_INCOMPLETE_FAIL_CLOSED,
                    "PLAN_BOUNDARY_INCOMPLETE",
                    "PlanBoundary 不完整；ExecutionPlan / BoundaryCandidate 保持 fail-closed。",
                    true,
                    "PARTIAL"
            );
        } else if ("WATCH_ONLY".equals(planStatus)) {
            applyExecutionPlanBoundaryStatus(
                    status,
                    PLAN_BOUNDARY_WATCH_ONLY,
                    "PLAN_BOUNDARY_WATCH_ONLY",
                    "PlanBoundary 仅观察；不是候选、决策生成、点位或交易信号。",
                    true,
                    "WATCH_ONLY"
            );
        } else if (executionPlanRiskGuardBlocked(riskGuard, riskStatus)) {
            applyExecutionPlanBoundaryStatus(
                    status,
                    EXECUTIONPLAN_RISK_GUARD_BLOCKED_FAIL_CLOSED,
                    firstNonBlank(riskGuard != null ? riskGuard.getRiskActionBlockingReason() : null,
                            "RISK_ACTION_GUARD_BLOCKED"),
                    "RiskActionGuard 未允许安全复核；ExecutionPlan / BoundaryCandidate 保持 fail-closed。",
                    true,
                    "BLOCKED"
            );
        } else if (!hasText(executionStatus) || "BOUNDARY_PENDING".equals(executionStatus)) {
            applyExecutionPlanBoundaryStatus(
                    status,
                    EXECUTIONPLAN_BOUNDARY_PENDING_FAIL_CLOSED,
                    "EXECUTIONPLAN_BOUNDARY_PENDING",
                    "ExecutionPlan 等待 PlanBoundary 对齐；只读状态 fail-closed。",
                    true,
                    "MISSING"
            );
        } else if ("INCOMPLETE".equals(executionStatus)) {
            applyExecutionPlanBoundaryStatus(
                    status,
                    PLAN_BOUNDARY_INCOMPLETE_FAIL_CLOSED,
                    "EXECUTIONPLAN_INCOMPLETE",
                    "ExecutionPlan 状态不完整；只读展示，不可执行。",
                    true,
                    "PARTIAL"
            );
        } else if ("WATCH_ONLY".equals(executionStatus)) {
            applyExecutionPlanBoundaryStatus(
                    status,
                    PLAN_BOUNDARY_WATCH_ONLY,
                    "EXECUTIONPLAN_WATCH_ONLY",
                    "ExecutionPlan 仅观察；不是交易信号。",
                    true,
                    "WATCH_ONLY"
            );
        } else if ("READY_REVIEW_ONLY".equals(executionStatus)) {
            applyExecutionPlanBoundaryStatus(
                    status,
                    EXECUTIONPLAN_BOUNDARY_READY,
                    "EXECUTIONPLAN_BOUNDARY_OWNER_PATH_READ",
                    "ExecutionPlan / BoundaryCandidate 只读状态可读；不可执行，不生成候选、点位或交易动作。",
                    false,
                    "OK"
            );
        } else {
            applyExecutionPlanBoundaryStatus(
                    status,
                    EXECUTIONPLAN_BOUNDARY_BLOCKED_FAIL_CLOSED,
                    "EXECUTIONPLAN_BOUNDARY_UNRECOGNIZED",
                    "ExecutionPlan / BoundaryCandidate 状态未知；只读状态 fail-closed。",
                    true,
                    "BLOCKED"
            );
        }
    }

    private void applyExecutionPlanBoundaryStatus(Map<String, Object> status,
                                                  String statusValue,
                                                  String reason,
                                                  String message,
                                                  boolean failClosed,
                                                  String sourceHealth) {
        status.put("status", statusValue);
        status.put("reason", reason);
        status.put("message", message);
        status.put("failClosed", failClosed);
        status.put("sourceHealth", sourceHealth);
    }

    private void applyReviewReplayStatus(Map<String, Object> status,
                                         String statusValue,
                                         String reason,
                                         String message,
                                         boolean failClosed,
                                         String sourceHealth) {
        status.put("status", statusValue);
        status.put("reason", reason);
        status.put("message", message);
        status.put("failClosed", failClosed);
        status.put("sourceHealth", sourceHealth);
    }

    private Map<String, Object> sourceStatusFromPayload(String name, String endpoint, Map<String, Object> payload) {
        String health = payload != null ? String.valueOf(payload.getOrDefault("sourceHealth", "MISSING")) : "MISSING";
        String reason = payload != null ? String.valueOf(payload.getOrDefault("reason", "SOURCE_STATUS_MISSING")) : "SOURCE_STATUS_MISSING";
        String message = payload != null ? String.valueOf(payload.getOrDefault("message", "source status missing")) : "source status missing";
        return sourceStatus(name, endpoint, health, reason, message);
    }

    private Map<String, Object> sourceStatus(
            String name,
            String endpoint,
            String sourceHealth,
            String reason,
            String message) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("name", name);
        source.put("endpoint", endpoint);
        source.put("sourceHealth", normalizeSourceHealth(sourceHealth));
        source.put("reason", firstNonBlank(reason, "SOURCE_STATUS_UNKNOWN"));
        source.put("message", firstNonBlank(message, "source status unknown"));
        source.put("reviewOnly", true);
        source.put("notTradingSignal", true);
        source.put("notCandidateSignal", true);
        source.put("notDecisionGeneration", true);
        source.put("notPointSignal", true);
        source.put("notExecutable", true);
        return source;
    }

    private void applyDataSourceHealthRollup(Map<String, Object> status, List<Map<String, Object>> sourceStatuses) {
        List<String> okSources = new ArrayList<>();
        List<String> partialSources = new ArrayList<>();
        List<String> staleSources = new ArrayList<>();
        List<String> missingSources = new ArrayList<>();
        List<String> watchOnlySources = new ArrayList<>();
        List<String> blockedSources = new ArrayList<>();

        for (Map<String, Object> source : sourceStatuses) {
            String sourceName = String.valueOf(source.getOrDefault("name", "UNKNOWN"));
            String sourceHealth = normalizeSourceHealth(String.valueOf(source.getOrDefault("sourceHealth", "MISSING")));
            if ("OK".equals(sourceHealth)) {
                okSources.add(sourceName);
            } else if ("PARTIAL".equals(sourceHealth)) {
                partialSources.add(sourceName);
            } else if ("STALE".equals(sourceHealth)) {
                staleSources.add(sourceName);
            } else if ("MISSING".equals(sourceHealth)) {
                missingSources.add(sourceName);
            } else if ("WATCH_ONLY".equals(sourceHealth)) {
                watchOnlySources.add(sourceName);
            } else {
                blockedSources.add(sourceName);
            }
        }

        status.put("sourceStatuses", sourceStatuses);
        status.put("okSources", okSources);
        status.put("partialSources", partialSources);
        status.put("staleSources", staleSources);
        status.put("missingSources", missingSources);
        status.put("watchOnlySources", watchOnlySources);
        status.put("blockedSources", blockedSources);

        if (!blockedSources.isEmpty()) {
            applyDataSourceHealthStatus(
                    status,
                    DATA_SOURCE_HEALTH_BLOCKED_FAIL_CLOSED,
                    "DATA_SOURCE_HEALTH_BLOCKED",
                    "Data Source Health 存在 blocked source；只读状态 fail-closed，不触发外部刷新、候选、点位、推送或交易。",
                    true,
                    "BLOCKED"
            );
        } else if (!missingSources.isEmpty()) {
            applyDataSourceHealthStatus(
                    status,
                    DATA_SOURCE_HEALTH_MISSING_FAIL_CLOSED,
                    "DATA_SOURCE_HEALTH_MISSING",
                    "Data Source Health 存在 missing source；只读状态 fail-closed，不补数据、不刷新外部来源。",
                    true,
                    "MISSING"
            );
        } else if (!staleSources.isEmpty()) {
            applyDataSourceHealthStatus(
                    status,
                    DATA_SOURCE_HEALTH_STALE_FAIL_CLOSED,
                    "DATA_SOURCE_HEALTH_STALE",
                    "Data Source Health 存在 stale source；只读状态 fail-closed。",
                    true,
                    "STALE"
            );
        } else if (!partialSources.isEmpty()) {
            applyDataSourceHealthStatus(
                    status,
                    DATA_SOURCE_HEALTH_PARTIAL,
                    "DATA_SOURCE_HEALTH_PARTIAL",
                    "Data Source Health 部分来源可读；仅供人工复核，不作为交易信号。",
                    true,
                    "PARTIAL"
            );
        } else if (!watchOnlySources.isEmpty()) {
            applyDataSourceHealthStatus(
                    status,
                    DATA_SOURCE_HEALTH_WATCH_ONLY,
                    "DATA_SOURCE_HEALTH_WATCH_ONLY",
                    "Data Source Health 存在 watch-only source；仅观察，不生成候选、点位或交易动作。",
                    true,
                    "WATCH_ONLY"
            );
        } else {
            applyDataSourceHealthStatus(
                    status,
                    DATA_SOURCE_HEALTH_READY,
                    "DATA_SOURCE_HEALTH_OWNER_PATH_READ",
                    "Data Source Health 只读状态可读；不触发外部刷新，不生成候选、点位、推送或交易信号。",
                    false,
                    "OK"
            );
        }
    }

    private void applyDataSourceHealthStatus(Map<String, Object> status,
                                             String statusValue,
                                             String reason,
                                             String message,
                                             boolean failClosed,
                                             String sourceHealth) {
        status.put("status", statusValue);
        status.put("reason", reason);
        status.put("message", message);
        status.put("failClosed", failClosed);
        status.put("sourceHealth", sourceHealth);
    }

    private String normalizeSourceHealth(String sourceHealth) {
        String normalized = normalizedStatus(sourceHealth);
        if (normalized == null) {
            return "MISSING";
        }
        return normalized.replace('-', '_').replace(' ', '_');
    }

    private boolean evidenceScoreSourceTraceComplete(List<EvidenceBriefVO> evidenceRows, List<ScoreBriefVO> scoreRows) {
        if (evidenceRows == null || evidenceRows.isEmpty() || scoreRows == null || scoreRows.isEmpty()) {
            return false;
        }
        boolean evidenceSourcesPresent = evidenceRows.stream()
                .allMatch(row -> row != null && hasText(row.getEvidenceType()) && hasText(row.getSource()));
        boolean scoreSummariesPresent = scoreRows.stream()
                .allMatch(row -> row != null && hasText(row.getScoreType()) && row.getScoreValue() != null);
        return evidenceSourcesPresent && scoreSummariesPresent;
    }

    private boolean decisionResultSourceTraceComplete(DecisionResultVO decision) {
        if (decision == null) {
            return false;
        }
        LocalDateTime createTime = decision.getCreateTime();
        return hasText(decision.getDecisionId())
                && hasText(decision.getAnalysisId())
                && hasText(decision.getSymbol())
                && createTime != null;
    }

    private boolean executionPlanSourceTraceComplete(SourceTraceDTO sourceTrace) {
        return sourceTrace != null && sourceTrace.hasRequiredBoundarySources();
    }

    private boolean reviewReplaySourceTraceComplete(ReviewAggregateSummaryVO summary,
                                                    ReviewStateVO reviewState,
                                                    ReviewAggregateSummaryVO.DetailSectionMeta replayMeta) {
        if (summary == null || reviewState == null || replayMeta == null) {
            return false;
        }
        ReviewAggregateSummaryVO.DetailSectionMeta meta = replayMeta;
        return hasText(reviewState.getAnalysisId())
                && summary.getRun() != null
                && hasText(summary.getRun().getAnalysisId())
                && hasText(summary.getRun().getSymbol())
                && summary.getReviewClosure() != null
                && hasText(meta.getSection())
                && meta.getTotal() != null
                && meta.getTotal() > 0;
    }

    private ReviewAggregateSummaryVO.DetailSectionMeta replaySummaryMeta(ReviewAggregateSummaryVO summary) {
        if (summary == null || summary.getDetailSections() == null) {
            return null;
        }
        return summary.getDetailSections().stream()
                .filter(meta -> meta != null && "pushRecheck".equals(meta.getSection()))
                .findFirst()
                .orElse(null);
    }

    private String resolveExecutionPlanSourceTraceStatus(
            DashboardDetailResponseVO.PlanBoundaryDisplayVO planBoundary,
            SourceTraceDTO sourceTrace) {
        if (sourceTrace != null && sourceTrace.getFallbackStatus() != null) {
            return sourceTrace.getFallbackStatus().name();
        }
        return firstNonBlank(planBoundary != null ? planBoundary.getSourceTraceStatus() : null, "UNKNOWN");
    }

    private boolean executionPlanRiskGuardBlocked(
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskGuard,
            String normalizedRiskStatus) {
        if (riskGuard == null) {
            return true;
        }
        if (!hasText(normalizedRiskStatus) || "BACKEND_PENDING".equals(normalizedRiskStatus)) {
            return true;
        }
        if (Boolean.TRUE.equals(riskGuard.getOpportunityPushAllowed())
                || Boolean.TRUE.equals(riskGuard.getReverseTradeAllowed())
                || Boolean.TRUE.equals(riskGuard.getNewPositionAllowed())
                || Boolean.TRUE.equals(riskGuard.getMarketOrderExitAllowed())) {
            return true;
        }
        String blockingReason = riskGuard.getRiskActionBlockingReason();
        return hasText(blockingReason) && !"MANUAL_REVIEW_REQUIRED".equalsIgnoreCase(blockingReason);
    }

    private boolean riskActionGuardActionFlagsAllFalse(DashboardDetailResponseVO.RiskActionGuardDisplayVO riskGuard) {
        if (riskGuard == null) {
            return false;
        }
        return !Boolean.TRUE.equals(riskGuard.getOpportunityPushAllowed())
                && !Boolean.TRUE.equals(riskGuard.getReverseTradeAllowed())
                && !Boolean.TRUE.equals(riskGuard.getNewPositionAllowed())
                && !Boolean.TRUE.equals(riskGuard.getMarketOrderExitAllowed());
    }

    private String safeRiskActionAdviceSummary(String advice) {
        if (!hasText(advice)) {
            return "RiskActionGuard 只读状态待确认；仅人工复核，不是交易信号。";
        }
        if (hasUnsafeActionWording(advice)) {
            return "RiskActionGuard advice 包含可执行动作措辞，已按只读 fail-closed 处理。";
        }
        return advice.trim();
    }

    private boolean hasUnsafeActionWording(String advice) {
        if (!hasText(advice)) {
            return false;
        }
        String normalized = advice.trim();
        String lower = normalized.toLowerCase();
        boolean hasActionWord = lower.contains("reduce")
                || lower.contains("close")
                || lower.contains("reverse")
                || lower.contains("move stop")
                || lower.contains("open")
                || lower.contains("execute")
                || normalized.contains("减仓")
                || normalized.contains("平仓")
                || normalized.contains("反手")
                || normalized.contains("移动止损")
                || normalized.contains("开仓")
                || normalized.contains("执行");
        if (!hasActionWord) {
            return false;
        }
        return !(normalized.contains("不")
                || normalized.contains("禁止")
                || normalized.contains("关闭")
                || normalized.contains("人工")
                || normalized.contains("只读")
                || lower.contains("not ")
                || lower.contains("manual")
                || lower.contains("review")
                || lower.contains("blocked")
                || lower.contains("disabled"));
    }

    private List<String> safeReasons(List<String> rawReasons) {
        if (rawReasons == null || rawReasons.isEmpty()) {
            return Collections.emptyList();
        }
        return rawReasons.stream()
                .filter(this::hasText)
                .map(this::safeReason)
                .distinct()
                .toList();
    }

    private String safeReason(String reason) {
        String normalized = reason.trim();
        String upper = normalized.toUpperCase();
        if (upper.contains("ENTRY") || upper.contains("STOP") || upper.contains("TP") || upper.contains("RR")) {
            return "NUMERIC_BOUNDARY_VALUES_NOT_GENERATED";
        }
        return normalized;
    }

    private String normalizedStatus(String value) {
        return hasText(value) ? value.trim().toUpperCase() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private DashboardDetailResponseVO.MarketEnvironmentMiniVO resolveMarketEnvironmentMini(
            String symbol, DashboardDetailResponseVO body) {
        DashboardDetailResponseVO.MarketEnvironmentMiniVO mini = new DashboardDetailResponseVO.MarketEnvironmentMiniVO();
        if (body != null && body.getDecision() != null && marketEnvironmentSnapshotMapper != null) {
            String analysisId = body.getDecision().getAnalysisId();
            if (analysisId != null && !analysisId.isBlank()) {
                MarketEnvironmentSnapshotDO snapshot = marketEnvironmentSnapshotMapper.selectByAnalysisId(analysisId);
                if (snapshot != null) {
                    mini.setSummary(snapshot.getSummary());
                    mini.setEnvironmentType(snapshot.getEnvironmentType());
                    mini.setRiskMode(snapshot.getRiskMode());
                    mini.setSourceType(snapshot.getSourceType());
                    return mini;
                }
            }
        }
        if (realMarketEnvironmentService != null) {
            Optional<MarketEnvironmentVO> env = realMarketEnvironmentService.tryBuildFromRealQuote(symbol, null);
            if (env.isPresent()) {
                mini.setSummary(env.get().getSummary());
                mini.setEnvironmentType(env.get().getEnvironmentType());
                mini.setRiskMode(env.get().getRiskMode());
                mini.setSourceType(MARKET_ENV_SOURCE_HEURISTIC);
                return mini;
            }
        }
        mini.setSourceType(MARKET_ENV_SOURCE_FALLBACK);
        return mini;
    }

    private int normalizeSummaryLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_DASHBOARD_SUMMARY_LIMIT;
        }
        if (limit < MIN_DASHBOARD_SUMMARY_LIMIT) {
            return MIN_DASHBOARD_SUMMARY_LIMIT;
        }
        return Math.min(limit, MAX_DASHBOARD_SUMMARY_LIMIT);
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) {
            throw new ResponseStatusException(BAD_REQUEST, "symbol must not be blank");
        }
        String normalized = symbol.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "symbol must not be blank");
        }
        return normalized;
    }

    private String normalizeAnalysisIdOrNull(String analysisId) {
        if (!hasText(analysisId)) {
            return null;
        }
        return analysisId.trim();
    }
}
