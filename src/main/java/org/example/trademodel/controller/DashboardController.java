package org.example.trademodel.controller;

import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEventSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceEventSourceOwnershipStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.push.ReviewOnlyInternalPushPreviewDTO;
import org.example.trademodel.entity.HotResetEventDO;
import org.example.trademodel.entity.MarketEnvironmentSnapshotDO;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.mapper.MarketEnvironmentSnapshotMapper;
import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.service.EvidenceService;
import org.example.trademodel.service.ReviewAggregateService;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.service.ScoreService;
import org.example.trademodel.service.SourceTraceEventSourceOwnershipService;
import org.example.trademodel.service.dashboard.DashboardSourceTraceDetailAdapter;
import org.example.trademodel.service.dashboard.ExecutionPlanDisplayAdapter;
import org.example.trademodel.service.dashboard.PaperObservationDisplayAdapter;
import org.example.trademodel.service.dashboard.PlanBoundaryDisplayAdapter;
import org.example.trademodel.service.dashboard.RiskActionGuardDisplayAdapter;
import org.example.trademodel.service.push.ReviewOnlyInternalPushPreviewAssembler;
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
    private static final String ALERT_POLICY_READY = "ALERT_POLICY_REVIEW_ONLY_READY";
    private static final String ALERT_POLICY_BACKEND_PENDING_FAIL_CLOSED = "ALERT_POLICY_BACKEND_PENDING_FAIL_CLOSED";
    private static final String ALERT_READ_MODEL_MISSING_FAIL_CLOSED = "ALERT_READ_MODEL_MISSING_FAIL_CLOSED";
    private static final String ALERT_RECENT_EMPTY_REVIEW_ONLY = "ALERT_RECENT_EMPTY_REVIEW_ONLY";
    private static final String ALERT_SUPPRESSION_ACTIVE_REVIEW_ONLY = "ALERT_SUPPRESSION_ACTIVE_REVIEW_ONLY";
    private static final String ALERT_COOLDOWN_ACTIVE_REVIEW_ONLY = "ALERT_COOLDOWN_ACTIVE_REVIEW_ONLY";
    private static final String ALERT_DUPLICATE_RISK_REVIEW_ONLY = "ALERT_DUPLICATE_RISK_REVIEW_ONLY";
    private static final String ALERT_FATIGUE_HIGH_REVIEW_ONLY = "ALERT_FATIGUE_HIGH_REVIEW_ONLY";
    private static final String NOTIFICATION_POLICY_MISSING_FAIL_CLOSED = "NOTIFICATION_POLICY_MISSING_FAIL_CLOSED";
    private static final String PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED = "PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED = "RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String INTERNAL_PUSH_PREVIEW_READY = "INTERNAL_PUSH_PREVIEW_REVIEW_ONLY_READY";
    private static final String INTERNAL_PUSH_PREVIEW_MISSING_FAIL_CLOSED =
            "INTERNAL_PUSH_PREVIEW_MISSING_FAIL_CLOSED";
    private static final String INTERNAL_PUSH_PREVIEW_PARTIAL_REVIEW_ONLY =
            "INTERNAL_PUSH_PREVIEW_PARTIAL_REVIEW_ONLY";
    private static final String NOTIFICATION_PREVIEW_READY = "NOTIFICATION_PREVIEW_REVIEW_ONLY_READY";
    private static final String NOTIFICATION_PREVIEW_MISSING_FAIL_CLOSED =
            "NOTIFICATION_PREVIEW_MISSING_FAIL_CLOSED";
    private static final String INTERNAL_PUSH_PREVIEW_ASSEMBLER_READY =
            "INTERNAL_PUSH_PREVIEW_ASSEMBLER_REVIEW_ONLY_READY";
    private static final String NO_OP_EXTERNAL_CHANNEL_POLICY_READY =
            "NO_OP_EXTERNAL_CHANNEL_POLICY_REVIEW_ONLY_READY";
    private static final String DUPLICATE_ALERT_NOTIFICATION_POLICY_REVIEW_REQUIRED =
            "DUPLICATE_ALERT_NOTIFICATION_POLICY_REVIEW_REQUIRED";
    private static final String PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED =
            "PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String EXTERNAL_CHANNEL_BOUNDARY_BLOCKED_FAIL_CLOSED =
            "EXTERNAL_CHANNEL_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED = "REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String DECISION_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED =
            "DECISION_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String SOURCE_RUNTIME_READY = "SOURCE_RUNTIME_STATUS_REVIEW_ONLY_READY";
    private static final String SOURCE_TRACE_MISSING_FAIL_CLOSED = "SOURCE_TRACE_MISSING_FAIL_CLOSED";
    private static final String SOURCE_TRACE_PARTIAL_REVIEW_ONLY = "SOURCE_TRACE_PARTIAL_REVIEW_ONLY";
    private static final String RUNTIME_KLINE_CONTEXT_READY_REVIEW_ONLY = "RUNTIME_KLINE_CONTEXT_READY_REVIEW_ONLY";
    private static final String RUNTIME_KLINE_CONTEXT_MISSING_FAIL_CLOSED = "RUNTIME_KLINE_CONTEXT_MISSING_FAIL_CLOSED";
    private static final String PERSISTED_OHLCV_READY_REVIEW_ONLY = "PERSISTED_OHLCV_READY_REVIEW_ONLY";
    private static final String PERSISTED_OHLCV_STALE_REVIEW_ONLY = "PERSISTED_OHLCV_STALE_REVIEW_ONLY";
    private static final String PERSISTED_OHLCV_MISSING_FAIL_CLOSED = "PERSISTED_OHLCV_MISSING_FAIL_CLOSED";
    private static final String DATA_QUALITY_PARTIAL_REVIEW_ONLY = "DATA_QUALITY_PARTIAL_REVIEW_ONLY";
    private static final String DATA_QUALITY_BLOCKED_FAIL_CLOSED = "DATA_QUALITY_BLOCKED_FAIL_CLOSED";
    private static final String MULTITIMEFRAME_ALIGNMENT_REVIEW_ONLY = "MULTITIMEFRAME_ALIGNMENT_REVIEW_ONLY";
    private static final String MULTITIMEFRAME_CONFLICT_REVIEW_ONLY = "MULTITIMEFRAME_CONFLICT_REVIEW_ONLY";
    private static final String MULTITIMEFRAME_MISSING_FAIL_CLOSED = "MULTITIMEFRAME_MISSING_FAIL_CLOSED";
    private static final String REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED = "REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED = "GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String PAPER_OBSERVATION_READY = "PAPER_OBSERVATION_REVIEW_ONLY_READY";
    private static final String PAPER_OBSERVATION_BACKEND_PENDING_FAIL_CLOSED = "PAPER_OBSERVATION_BACKEND_PENDING_FAIL_CLOSED";
    private static final String PAPER_OBSERVATION_MISSING_FAIL_CLOSED = "PAPER_OBSERVATION_MISSING_FAIL_CLOSED";
    private static final String PAPER_OBSERVATION_PARTIAL_REVIEW_ONLY = "PAPER_OBSERVATION_PARTIAL_REVIEW_ONLY";
    private static final String NOT_REAL_POSITION_REVIEW_ONLY = "NOT_REAL_POSITION_REVIEW_ONLY";
    private static final String NOT_TRADE_INSTRUCTION_REVIEW_ONLY = "NOT_TRADE_INSTRUCTION_REVIEW_ONLY";
    private static final String PAPER_ORDER_BOUNDARY_BLOCKED_FAIL_CLOSED = "PAPER_ORDER_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String SIMULATED_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED = "SIMULATED_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String PAPER_PNL_BOUNDARY_BLOCKED_FAIL_CLOSED = "PAPER_PNL_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String POSITION_MONITOR_BOUNDARY_BLOCKED_FAIL_CLOSED = "POSITION_MONITOR_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String POINT_BOUNDARY_BLOCKED_FAIL_CLOSED = "POINT_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED = "TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String ACCOUNT_RISK_STATUS_READY = "ACCOUNT_RISK_STATUS_REVIEW_ONLY_READY";
    private static final String ACCOUNT_RISK_STATUS_BACKEND_PENDING_FAIL_CLOSED = "ACCOUNT_RISK_STATUS_BACKEND_PENDING_FAIL_CLOSED";
    private static final String ACCOUNT_RISK_STATUS_MISSING_FAIL_CLOSED = "ACCOUNT_RISK_STATUS_MISSING_FAIL_CLOSED";
    private static final String ACCOUNT_RISK_STATUS_PARTIAL_REVIEW_ONLY = "ACCOUNT_RISK_STATUS_PARTIAL_REVIEW_ONLY";
    private static final String ACCOUNT_EXPOSURE_READY = "ACCOUNT_EXPOSURE_REVIEW_ONLY_READY";
    private static final String ACCOUNT_EXPOSURE_MISSING_FAIL_CLOSED = "ACCOUNT_EXPOSURE_MISSING_FAIL_CLOSED";
    private static final String RISK_ALLOWED_READ_ONLY_EVIDENCE = "RISK_ALLOWED_READ_ONLY_EVIDENCE";
    private static final String ACCOUNT_RISK_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED = "ACCOUNT_RISK_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED = "PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED = "TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String POSITION_SIZING_BOUNDARY_BLOCKED_FAIL_CLOSED = "POSITION_SIZING_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String REDUCE_CLOSE_STOP_REVERSE_BOUNDARY_BLOCKED_FAIL_CLOSED = "REDUCE_CLOSE_STOP_REVERSE_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED = "CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String HOT_RESET_EVENT_SOURCE_READY = "HOT_RESET_EVENT_SOURCE_REVIEW_ONLY_READY";
    private static final String HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED = "HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED";
    private static final String HOT_RESET_EVENT_SOURCE_PARTIAL_REVIEW_ONLY = "HOT_RESET_EVENT_SOURCE_PARTIAL_REVIEW_ONLY";
    private static final String EVENT_IMPACT_SOURCE_READY = "EVENT_IMPACT_SOURCE_REVIEW_ONLY_READY";
    private static final String EVENT_IMPACT_SOURCE_MISSING_FAIL_CLOSED = "EVENT_IMPACT_SOURCE_MISSING_FAIL_CLOSED";
    private static final String SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_READY = "SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_REVIEW_ONLY_READY";
    private static final String SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED = "SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED";
    private static final String HOT_RESET_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED = "HOT_RESET_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String HOT_RESET_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED = "HOT_RESET_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String EVENT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED = "EVENT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String EXTERNAL_API_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED = "EXTERNAL_API_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String NEWS_FETCH_BOUNDARY_BLOCKED_FAIL_CLOSED = "NEWS_FETCH_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED = "SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED = "COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String RECHECK_REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED = "RECHECK_REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED";
    private static final String READ_MODEL_FULL = "FULL";

    private final DecisionService decisionService;
    private final SystemHealthService systemHealthService;
    private final MonitorService monitorService;
    private final RuntimeMetricService runtimeMetricService;
    private final RealMarketEnvironmentService realMarketEnvironmentService;
    private final MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper;
    private final AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    private final EvidenceService evidenceService;
    private final ScoreService scoreService;
    private final ReviewService reviewService;
    private final ReviewAggregateService reviewAggregateService;
    private final DashboardSourceTraceDetailAdapter dashboardSourceTraceDetailAdapter;
    private final PlanBoundaryDisplayAdapter planBoundaryDisplayAdapter;
    private final ExecutionPlanDisplayAdapter executionPlanDisplayAdapter;
    private final RiskActionGuardDisplayAdapter riskActionGuardDisplayAdapter;
    private final PaperObservationDisplayAdapter paperObservationDisplayAdapter;
    private final HotResetEventMapper hotResetEventMapper;
    private final SourceTraceEventSourceOwnershipService sourceTraceEventSourceOwnershipService;
    private final ReviewOnlyInternalPushPreviewAssembler internalPushPreviewAssembler =
            new ReviewOnlyInternalPushPreviewAssembler();

    public DashboardController(DecisionService decisionService,
                               SystemHealthService systemHealthService,
                               MonitorService monitorService,
                               RuntimeMetricService runtimeMetricService,
                               RealMarketEnvironmentService realMarketEnvironmentService,
                               MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper,
                               AccountRiskSnapshotMapper accountRiskSnapshotMapper,
                               EvidenceService evidenceService,
                               ScoreService scoreService,
                               ReviewService reviewService,
                               ReviewAggregateService reviewAggregateService,
                               DashboardSourceTraceDetailAdapter dashboardSourceTraceDetailAdapter,
                               PlanBoundaryDisplayAdapter planBoundaryDisplayAdapter,
                               ExecutionPlanDisplayAdapter executionPlanDisplayAdapter,
                               RiskActionGuardDisplayAdapter riskActionGuardDisplayAdapter,
                               PaperObservationDisplayAdapter paperObservationDisplayAdapter,
                               HotResetEventMapper hotResetEventMapper,
                               SourceTraceEventSourceOwnershipService sourceTraceEventSourceOwnershipService) {
        this.decisionService = decisionService;
        this.systemHealthService = systemHealthService;
        this.monitorService = monitorService;
        this.runtimeMetricService = runtimeMetricService;
        this.realMarketEnvironmentService = realMarketEnvironmentService;
        this.marketEnvironmentSnapshotMapper = marketEnvironmentSnapshotMapper;
        this.accountRiskSnapshotMapper = accountRiskSnapshotMapper;
        this.evidenceService = evidenceService;
        this.scoreService = scoreService;
        this.reviewService = reviewService;
        this.reviewAggregateService = reviewAggregateService;
        this.dashboardSourceTraceDetailAdapter = dashboardSourceTraceDetailAdapter;
        this.planBoundaryDisplayAdapter = planBoundaryDisplayAdapter;
        this.executionPlanDisplayAdapter = executionPlanDisplayAdapter;
        this.riskActionGuardDisplayAdapter = riskActionGuardDisplayAdapter;
        this.paperObservationDisplayAdapter = paperObservationDisplayAdapter;
        this.hotResetEventMapper = hotResetEventMapper;
        this.sourceTraceEventSourceOwnershipService = sourceTraceEventSourceOwnershipService;
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

    @GetMapping("/api/dashboard/alert-fatigue-policy-status")
    @ResponseBody
    public Map<String, Object> alertFatiguePolicyStatus(@RequestParam("symbol") String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Map<String, Object> status = baseAlertFatiguePolicyStatus(normalizedSymbol);

        List<MonitorAlertDO> recentAlerts;
        try {
            recentAlerts = monitorService != null ? monitorService.getRecentAlerts(20) : null;
        } catch (RuntimeException ex) {
            applyAlertFatiguePolicyStatus(
                    status,
                    ALERT_POLICY_BACKEND_PENDING_FAIL_CLOSED,
                    "MONITOR_ALERT_READ_PATH_UNAVAILABLE",
                    "MonitorAlert read path 不可用；Alert fatigue / notification policy 只读状态 fail-closed，不触发 Push、recheck、refresh 或写入。",
                    true,
                    "BLOCKED"
            );
            return status;
        }

        if (recentAlerts == null) {
            applyAlertFatiguePolicyStatus(
                    status,
                    ALERT_READ_MODEL_MISSING_FAIL_CLOSED,
                    "MONITOR_ALERT_READ_MODEL_MISSING",
                    "MonitorAlert read model 缺失；不伪造告警疲劳或通知策略状态。",
                    true,
                    "MISSING"
            );
            return status;
        }

        List<MonitorAlertDO> scopedAlerts = recentAlerts.stream()
                .filter(alert -> alertMatchesSymbol(alert, normalizedSymbol))
                .toList();
        long openCount = scopedAlerts.stream().filter(this::isOpenMonitorAlert).count();
        long suppressedCount = scopedAlerts.stream().filter(this::isSuppressedMonitorAlert).count();
        boolean cooldownActive = scopedAlerts.stream().anyMatch(alert -> hasText(alert.getCooldownUntil()));
        boolean suppressionActive = suppressedCount > 0;
        boolean duplicateRisk = duplicateAlertTypeRisk(scopedAlerts);
        boolean fatigueHigh = scopedAlerts.size() >= 6 || openCount >= 3 || suppressedCount >= 3;

        status.put("recentAlertCount", scopedAlerts.size());
        status.put("openAlertCount", openCount);
        status.put("suppressedAlertCount", suppressedCount);
        status.put("cooldownActive", cooldownActive);
        status.put("suppressionActive", suppressionActive);
        status.put("duplicateRiskVisible", duplicateRisk);
        status.put("fatigueHigh", fatigueHigh);
        status.put("latestAlertType", latestAlertType(scopedAlerts));
        status.put("latestAlertLevel", latestAlertLevel(scopedAlerts));

        if (scopedAlerts.isEmpty()) {
            applyAlertFatiguePolicyStatus(
                    status,
                    ALERT_RECENT_EMPTY_REVIEW_ONLY,
                    "ALERT_RECENT_EMPTY",
                    "最近告警为空；仅表示只读告警中心当前无可见样本，不代表推送策略或交易安全结论。",
                    false,
                    "WATCH_ONLY"
            );
        } else if (suppressionActive) {
            applyAlertFatiguePolicyStatus(
                    status,
                    ALERT_SUPPRESSION_ACTIVE_REVIEW_ONLY,
                    "ALERT_SUPPRESSION_ACTIVE",
                    "告警抑制证据可见；仅展示只读状态，不发送 Push，不触发 recheck。",
                    false,
                    "PARTIAL"
            );
        } else if (cooldownActive) {
            applyAlertFatiguePolicyStatus(
                    status,
                    ALERT_COOLDOWN_ACTIVE_REVIEW_ONLY,
                    "ALERT_COOLDOWN_ACTIVE",
                    "告警冷却证据可见；仅展示只读状态，不触发调度、采集或 API client refresh。",
                    false,
                    "PARTIAL"
            );
        } else if (duplicateRisk) {
            applyAlertFatiguePolicyStatus(
                    status,
                    ALERT_DUPLICATE_RISK_REVIEW_ONLY,
                    "ALERT_DUPLICATE_RISK",
                    "重复告警风险可见；仅供人工复核，不发送通知或重跑复查。",
                    false,
                    "PARTIAL"
            );
        } else if (fatigueHigh) {
            applyAlertFatiguePolicyStatus(
                    status,
                    ALERT_FATIGUE_HIGH_REVIEW_ONLY,
                    "ALERT_FATIGUE_HIGH",
                    "告警疲劳偏高；仅供人工复核，不生成交易信号或推送动作。",
                    false,
                    "PARTIAL"
            );
        } else {
            applyAlertFatiguePolicyStatus(
                    status,
                    ALERT_POLICY_READY,
                    "MONITOR_ALERT_READ_MODEL_READY",
                    "Alert fatigue / notification policy 只读状态可读；不发送 Push，不触发 recheck、refresh、写入或交易动作。",
                    false,
                    "OK"
            );
        }
        return status;
    }

    @GetMapping("/api/dashboard/internal-push-preview-notification-status")
    @ResponseBody
    public Map<String, Object> internalPushPreviewNotificationStatus(@RequestParam("symbol") String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Map<String, Object> status = baseInternalPushPreviewNotificationStatus(normalizedSymbol);

        ReviewOnlyInternalPushPreviewDTO preview;
        try {
            preview = internalPushPreviewAssembler.assemble(null);
        } catch (RuntimeException ex) {
            applyInternalPushPreviewNotificationStatus(
                    status,
                    INTERNAL_PUSH_PREVIEW_MISSING_FAIL_CLOSED,
                    "INTERNAL_PUSH_PREVIEW_ASSEMBLER_UNAVAILABLE",
                    "Internal Push preview assembler 只读投影不可用；状态 fail-closed，不发送 Push，不接外部通道。",
                    true,
                    "BLOCKED"
            );
            return status;
        }

        populateInternalPushPreviewNotificationStatus(status, preview);
        if (preview == null) {
            applyInternalPushPreviewNotificationStatus(
                    status,
                    INTERNAL_PUSH_PREVIEW_MISSING_FAIL_CLOSED,
                    "INTERNAL_PUSH_PREVIEW_DTO_MISSING",
                    "Internal Push preview DTO 缺失；不伪造通知预览，不生成 sendable message。",
                    true,
                    "MISSING"
            );
        } else if (preview.isFailClosed() || preview.isBlocked()) {
            applyInternalPushPreviewNotificationStatus(
                    status,
                    INTERNAL_PUSH_PREVIEW_MISSING_FAIL_CLOSED,
                    "INTERNAL_PUSH_PREVIEW_OWNER_INPUT_MISSING",
                    "Internal Push preview owner input 缺失；仅确认 existing assembler / DTO / no-op external channel policy 边界，不发送 Push。",
                    true,
                    "MISSING"
            );
        } else if (!preview.getRiskBlockers().isEmpty()) {
            applyInternalPushPreviewNotificationStatus(
                    status,
                    INTERNAL_PUSH_PREVIEW_PARTIAL_REVIEW_ONLY,
                    "INTERNAL_PUSH_PREVIEW_RISK_BLOCKERS_PRESENT",
                    "Internal Push preview 只读投影部分可读；risk blockers 仅供人工复核，不触发 Recheck 或 Replay。",
                    false,
                    "PARTIAL"
            );
        } else {
            applyInternalPushPreviewNotificationStatus(
                    status,
                    INTERNAL_PUSH_PREVIEW_READY,
                    "INTERNAL_PUSH_PREVIEW_OWNER_PATH_READ",
                    "Internal Push preview / notification preview 只读状态可读；不发送 Push，不接外部通道。",
                    false,
                    "OK"
            );
        }
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

    @GetMapping("/api/dashboard/source-runtime-data-quality-status")
    @ResponseBody
    public Map<String, Object> sourceRuntimeDataQualityStatus(@RequestParam("symbol") String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Map<String, Object> status = baseSourceRuntimeDataQualityStatus(normalizedSymbol);
        DashboardDetailResponseVO detail = dashboardDetail(normalizedSymbol);
        SourceTraceDTO sourceTrace = detail != null ? detail.getSourceTrace() : null;
        RuntimeKlineContextDTO runtimeKline = detail != null ? detail.getRuntimeKlineContext() : null;

        status.put("analysisId", detail != null && detail.getDecision() != null ? detail.getDecision().getAnalysisId() : null);
        status.put("sourceTraceAvailable", sourceTrace != null);
        status.put("runtimeKlineContextAvailable", runtimeKline != null);
        status.put("sourceTraceStatus", resolveSourceTraceReadinessStatus(sourceTrace));
        status.put("runtimeKlineStatus", resolveRuntimeKlineContextStatus(runtimeKline));

        String persistedReadiness = resolvePersistedOhlcvReadiness(sourceTrace, runtimeKline);
        String persistedStatus = resolvePersistedOhlcvStatus(persistedReadiness);
        status.put("persistedOhlcvReadiness", persistedReadiness);
        status.put("persistedOhlcvStatus", persistedStatus);
        status.put("persistedOhlcvStaleReason", resolvePersistedOhlcvStaleReason(sourceTrace, runtimeKline));
        status.put("persistedOhlcvMissingFields", resolvePersistedOhlcvMissingFields(sourceTrace, runtimeKline));

        Object dataQualityScore = resolveDataQualityScore(sourceTrace, runtimeKline);
        String dataQualityStatus = resolveDataQualityStatus(dataQualityScore, sourceTrace, runtimeKline);
        status.put("dataQualityAvailable", dataQualityScore != null);
        status.put("dataQualityScore", dataQualityScore);
        status.put("dataQualityStatus", dataQualityStatus);

        String multiTimeframeSource = resolveMultiTimeframeSource(sourceTrace, runtimeKline);
        String multiTimeframeStatus = resolveMultiTimeframeStatus(multiTimeframeSource);
        status.put("multiTimeframeAvailable", hasText(multiTimeframeSource));
        status.put("multiTimeframeSummary", firstNonBlank(multiTimeframeSource, "missing"));
        status.put("multiTimeframeStatus", multiTimeframeStatus);
        status.put("refreshBoundaryStatus", REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("generationBoundaryStatus", GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED);

        if (sourceTrace == null) {
            applySourceRuntimeDataQualityStatus(
                    status,
                    SOURCE_TRACE_MISSING_FAIL_CLOSED,
                    "SOURCE_TRACE_MISSING",
                    "SourceTrace owner path 缺失；只读状态 fail-closed，不生成来源绑定、候选、点位或交易动作。",
                    true,
                    "MISSING"
            );
        } else if (runtimeKline == null) {
            applySourceRuntimeDataQualityStatus(
                    status,
                    RUNTIME_KLINE_CONTEXT_MISSING_FAIL_CLOSED,
                    "RUNTIME_KLINE_CONTEXT_MISSING",
                    "RuntimeKline context 缺失；只读状态 fail-closed，不触发采集、调度或 API client refresh。",
                    true,
                    "MISSING"
            );
        } else if (PERSISTED_OHLCV_MISSING_FAIL_CLOSED.equals(persistedStatus)) {
            applySourceRuntimeDataQualityStatus(
                    status,
                    PERSISTED_OHLCV_MISSING_FAIL_CLOSED,
                    "PERSISTED_OHLCV_MISSING",
                    "Persisted OHLCV readiness 缺失；只读状态 fail-closed，不刷新外部 K 线。",
                    true,
                    "MISSING"
            );
        } else if (PERSISTED_OHLCV_STALE_REVIEW_ONLY.equals(persistedStatus)) {
            applySourceRuntimeDataQualityStatus(
                    status,
                    PERSISTED_OHLCV_STALE_REVIEW_ONLY,
                    "PERSISTED_OHLCV_STALE",
                    "Persisted OHLCV 非 fresh；仅展示只读 stale 状态，不触发 refresh 或生成交易结论。",
                    true,
                    "STALE"
            );
        } else if (DATA_QUALITY_BLOCKED_FAIL_CLOSED.equals(dataQualityStatus)) {
            applySourceRuntimeDataQualityStatus(
                    status,
                    DATA_QUALITY_BLOCKED_FAIL_CLOSED,
                    "DATA_QUALITY_BLOCKED",
                    "DataQuality metadata 缺失；只读状态 fail-closed，不把数据质量解释成交易折扣。",
                    true,
                    "BLOCKED"
            );
        } else if (MULTITIMEFRAME_MISSING_FAIL_CLOSED.equals(multiTimeframeStatus)) {
            applySourceRuntimeDataQualityStatus(
                    status,
                    MULTITIMEFRAME_MISSING_FAIL_CLOSED,
                    "MULTITIMEFRAME_MISSING",
                    "MultiTimeframe 状态缺失；只读状态 fail-closed，不生成方向判断。",
                    true,
                    "MISSING"
            );
        } else if (MULTITIMEFRAME_CONFLICT_REVIEW_ONLY.equals(multiTimeframeStatus)) {
            applySourceRuntimeDataQualityStatus(
                    status,
                    MULTITIMEFRAME_CONFLICT_REVIEW_ONLY,
                    "MULTITIMEFRAME_CONFLICT",
                    "MultiTimeframe 冲突仅作为只读诊断；不是方向、点位或交易信号。",
                    true,
                    "PARTIAL"
            );
        } else if (SOURCE_TRACE_PARTIAL_REVIEW_ONLY.equals(status.get("sourceTraceStatus"))) {
            applySourceRuntimeDataQualityStatus(
                    status,
                    SOURCE_TRACE_PARTIAL_REVIEW_ONLY,
                    "SOURCE_TRACE_PARTIAL",
                    "SourceTrace 部分可读；仅展示诊断状态，不生成来源绑定或候选。",
                    true,
                    "PARTIAL"
            );
        } else {
            applySourceRuntimeDataQualityStatus(
                    status,
                    SOURCE_RUNTIME_READY,
                    "SOURCE_RUNTIME_OWNER_PATH_READ",
                    "SourceTrace / RuntimeKline / DataQuality / MultiTimeframe 只读状态可读；不刷新、不生成、不执行。",
                    false,
                    "OK"
            );
        }
        return status;
    }

    @GetMapping("/api/dashboard/paper-observation-status")
    @ResponseBody
    public Map<String, Object> paperObservationStatus(@RequestParam("symbol") String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Map<String, Object> status = basePaperObservationStatus(normalizedSymbol);
        DashboardDetailResponseVO detail = dashboardDetail(normalizedSymbol);
        DecisionResultVO decision = detail != null ? detail.getDecision() : null;
        DashboardDetailResponseVO.PaperObservationDisplayVO paper =
                detail != null ? detail.getPaperObservationDisplay() : null;

        status.put("analysisId", decision != null ? decision.getAnalysisId() : null);
        status.put("paperObservationStatus", firstNonBlank(
                paper != null ? paper.getPaperObservationStatus() : null,
                "BACKEND_PENDING"
        ));
        status.put("paperObservationStatusLabel", firstNonBlank(
                paper != null ? paper.getPaperObservationStatusLabel() : null,
                "后端未接入"
        ));
        status.put("paperObservationAvailable", paper != null && Boolean.TRUE.equals(paper.getPaperObservationAvailable()));
        status.put("manualReviewSurfaceAvailable", paper != null && Boolean.TRUE.equals(paper.getManualReviewEntryAvailable()));
        status.put("linkedPaperObservationCount", safeInteger(paper != null ? paper.getLinkedPaperObservationCount() : null));
        status.put("linkedReviewCount", safeInteger(paper != null ? paper.getLinkedReviewCount() : null));
        status.put("missedOpportunityFlag", paper != null && Boolean.TRUE.equals(paper.getMissedOpportunityFlag()));
        status.put("reviewSummary", firstNonBlank(paper != null ? paper.getReviewSummary() : null, "missing"));
        status.put("backendConnectionStatus", firstNonBlank(
                paper != null ? paper.getBackendConnectionStatus() : null,
                "BACKEND_PENDING"
        ));
        status.put("ownerPath", "dashboardDetail.paperObservationDisplay");
        status.put("paperOwnerSafetyFlagsAllTrue", paperObservationSafetyFlagsAllTrue(paper));

        applyPaperObservationStatus(status, decision, paper);
        return status;
    }

    @GetMapping("/api/dashboard/account-risk-exposure-status")
    @ResponseBody
    public Map<String, Object> accountRiskExposureStatus(@RequestParam("symbol") String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Map<String, Object> status = baseAccountRiskExposureStatus(normalizedSymbol);
        DecisionResultVO decision = decisionService.getLatestDecisionResultBySymbol(normalizedSymbol);
        String analysisId = decision != null ? decision.getAnalysisId() : null;
        status.put("analysisId", analysisId);
        if (!hasText(analysisId)) {
            applyAccountRiskExposureStatus(
                    status,
                    ACCOUNT_RISK_STATUS_MISSING_FAIL_CLOSED,
                    "ANALYSIS_CONTEXT_MISSING",
                    "Account risk snapshot analysis context 缺失；账户风险 / 暴露只读状态 fail-closed，不生成交易授权、仓位大小或账户动作。",
                    true,
                    "MISSING"
            );
            return status;
        }

        TmAccountRiskSnapshotDO snapshot;
        try {
            snapshot = accountRiskSnapshotMapper != null
                    ? accountRiskSnapshotMapper.selectLatestByAnalysisId(analysisId)
                    : null;
        } catch (RuntimeException ex) {
            applyAccountRiskExposureStatus(
                    status,
                    ACCOUNT_RISK_STATUS_BACKEND_PENDING_FAIL_CLOSED,
                    "ACCOUNT_RISK_SNAPSHOT_READ_PATH_UNAVAILABLE",
                    "AccountRiskSnapshot read path 不可用；只读状态 fail-closed，不写入 account risk 或 PushSnapshot，不触发 Push / Recheck / Trading。",
                    true,
                    "BLOCKED"
            );
            return status;
        }

        if (snapshot == null) {
            applyAccountRiskExposureStatus(
                    status,
                    ACCOUNT_RISK_STATUS_MISSING_FAIL_CLOSED,
                    "ACCOUNT_RISK_SNAPSHOT_MISSING",
                    "AccountRiskSnapshot 缺失；不伪造 riskAllowed、exposure 或账户风险状态。",
                    true,
                    "MISSING"
            );
            return status;
        }

        status.put("snapshotId", snapshot.getId());
        status.put("snapshotSymbol", firstNonBlank(snapshot.getSymbol(), normalizedSymbol));
        status.put("riskLevelSnapshot", firstNonBlank(snapshot.getRiskLevelSnapshot(), "UNKNOWN"));
        status.put("riskAllowedEvidence", snapshot.getRiskAllowed());
        status.put("riskAllowedStatus", snapshot.getRiskAllowed() != null ? RISK_ALLOWED_READ_ONLY_EVIDENCE : ACCOUNT_RISK_STATUS_PARTIAL_REVIEW_ONLY);
        status.put("riskReasonCode", firstNonBlank(snapshot.getRiskReasonCode(), "UNKNOWN"));
        status.put("riskReasonText", firstNonBlank(snapshot.getRiskReasonText(), "missing"));
        status.put("positionExposure", snapshot.getPositionExposure());
        status.put("maxAllowedExposure", snapshot.getMaxAllowedExposure());
        status.put("accountExposureStatus", accountExposureStatus(snapshot));
        status.put("snapshotSource", firstNonBlank(snapshot.getSnapshotSource(), "UNKNOWN"));
        status.put("snapshotVersion", snapshot.getSnapshotVersion());
        status.put("sourceNote", firstNonBlank(snapshot.getSourceNote(), "missing"));
        status.put("traceId", firstNonBlank(snapshot.getTraceId(), "missing"));
        status.put("snapshotCreateTime", snapshot.getCreateTime());

        if (snapshot.getPositionExposure() == null || snapshot.getMaxAllowedExposure() == null) {
            applyAccountRiskExposureStatus(
                    status,
                    ACCOUNT_EXPOSURE_MISSING_FAIL_CLOSED,
                    "ACCOUNT_EXPOSURE_MISSING",
                    "Account exposure / maxAllowedExposure 缺失；只读状态 fail-closed，不推导仓位大小或交易授权。",
                    true,
                    "MISSING"
            );
        } else if (snapshot.getRiskAllowed() == null || !hasText(snapshot.getRiskLevelSnapshot())) {
            applyAccountRiskExposureStatus(
                    status,
                    ACCOUNT_RISK_STATUS_PARTIAL_REVIEW_ONLY,
                    "ACCOUNT_RISK_STATUS_PARTIAL",
                    "Account risk snapshot 部分可读；riskAllowed 仅作只读证据，不生成账户动作。",
                    true,
                    "PARTIAL"
            );
        } else {
            applyAccountRiskExposureStatus(
                    status,
                    ACCOUNT_RISK_STATUS_READY,
                    "ACCOUNT_RISK_SNAPSHOT_OWNER_PATH_READ",
                    "Account risk / exposure 只读状态可读；riskAllowed 仅作只读证据，exposure 仅作状态，不是交易授权或仓位大小建议。",
                    false,
                    "OK"
            );
        }
        return status;
    }

    @GetMapping("/api/dashboard/hot-reset-event-impact-source-status")
    @ResponseBody
    public Map<String, Object> hotResetEventImpactSourceStatus(@RequestParam("symbol") String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Map<String, Object> status = baseHotResetEventImpactSourceStatus(normalizedSymbol);
        DecisionResultVO decision = decisionService.getLatestDecisionResultBySymbol(normalizedSymbol);
        String analysisId = decision != null ? decision.getAnalysisId() : null;
        String timeframe = firstNonBlank(decision != null ? decision.getTimeframe() : null, "UNKNOWN");
        status.put("analysisId", analysisId);
        status.put("timeframe", timeframe);

        SourceTraceEventSourceOwnershipResult ownership = resolveEventSourceOwnership(normalizedSymbol, timeframe);
        applySourceTraceEventSourceOwnership(status, ownership);

        if (!hasText(analysisId)) {
            applyHotResetEventImpactSourceStatus(
                    status,
                    HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED,
                    "ANALYSIS_CONTEXT_MISSING",
                    "DecisionResult analysisId 缺失；Hot Reset / Event Impact source status fail-closed，不伪造事件来源或执行入口。",
                    true,
                    "MISSING"
            );
            return status;
        }

        HotResetEventDO latestEvent;
        Integer eventCount;
        try {
            latestEvent = hotResetEventMapper != null
                    ? hotResetEventMapper.selectLatestByAnalysisId(analysisId)
                    : null;
            eventCount = latestEvent != null && hotResetEventMapper != null
                    ? hotResetEventMapper.countByAnalysisId(analysisId)
                    : 0;
        } catch (RuntimeException ex) {
            applyHotResetEventImpactSourceStatus(
                    status,
                    HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED,
                    "HOT_RESET_EVENT_READ_PATH_UNAVAILABLE",
                    "HotResetEvent read path 不可用；只读状态 fail-closed，不执行 Hot Reset、不写入、不生成事件或刷新外部来源。",
                    true,
                    "BLOCKED"
            );
            return status;
        }

        status.put("hotResetEventCount", safeInteger(eventCount));
        if (latestEvent == null) {
            status.put("eventImpactSourceStatus", EVENT_IMPACT_SOURCE_MISSING_FAIL_CLOSED);
            applyHotResetEventImpactSourceStatus(
                    status,
                    HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED,
                    "HOT_RESET_EVENT_SOURCE_MISSING",
                    "Hot Reset persisted event source 缺失；Event Impact source status fail-closed，不伪造 event impact。",
                    true,
                    "MISSING"
            );
            return status;
        }

        populateHotResetEventImpactSource(status, latestEvent);

        boolean partialHotResetEvent = !hasText(latestEvent.getTriggerType())
                || !hasText(latestEvent.getTriggerReasonCode())
                || latestEvent.getEventTime() == null;
        if (partialHotResetEvent) {
            status.put("hotResetEventSourceStatus", HOT_RESET_EVENT_SOURCE_PARTIAL_REVIEW_ONLY);
        } else {
            status.put("hotResetEventSourceStatus", HOT_RESET_EVENT_SOURCE_READY);
        }
        status.put("eventImpactSourceStatus", EVENT_IMPACT_SOURCE_READY);

        if (SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED.equals(status.get("sourceTraceEventSourceOwnershipStatus"))) {
            applyHotResetEventImpactSourceStatus(
                    status,
                    SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED,
                    "SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE",
                    "SourceTrace event-source ownership 未完成；保持 fail-closed，不伪造来源归属。",
                    true,
                    "BLOCKED"
            );
        } else if (partialHotResetEvent) {
            applyHotResetEventImpactSourceStatus(
                    status,
                    HOT_RESET_EVENT_SOURCE_PARTIAL_REVIEW_ONLY,
                    "HOT_RESET_EVENT_SOURCE_PARTIAL",
                    "Hot Reset event source 部分可读；仅作为只读 event source evidence，不作为 Hot Reset execution。",
                    true,
                    "PARTIAL"
            );
        } else {
            applyHotResetEventImpactSourceStatus(
                    status,
                    HOT_RESET_EVENT_SOURCE_READY,
                    "HOT_RESET_EVENT_SOURCE_OWNER_PATH_READ",
                    "Hot Reset event / Event Impact source 只读状态可读；不执行、不写入、不生成事件、不刷新外部来源。",
                    false,
                    "OK"
            );
        }
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

    private Map<String, Object> baseAlertFatiguePolicyStatus(String normalizedSymbol) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", ALERT_POLICY_BACKEND_PENDING_FAIL_CLOSED);
        status.put("symbol", normalizedSymbol);
        status.put("recentAlertCount", 0);
        status.put("openAlertCount", 0);
        status.put("suppressedAlertCount", 0);
        status.put("cooldownActive", false);
        status.put("suppressionActive", false);
        status.put("duplicateRiskVisible", false);
        status.put("fatigueHigh", false);
        status.put("latestAlertType", null);
        status.put("latestAlertLevel", null);
        status.put("policySource", "MonitorAlert read model");
        status.put("sourceHealth", "BLOCKED");
        status.put("reason", "ALERT_POLICY_STATUS_PENDING");
        status.put("message", "Alert fatigue / notification policy 只读状态待确认；不发送 Push，不触发 recheck 或刷新。");
        status.put("reviewOnly", true);
        status.put("notPushSend", true);
        status.put("notExternalChannel", true);
        status.put("notRecheckExecution", true);
        status.put("notSchedulerTrigger", true);
        status.put("notCollectorTrigger", true);
        status.put("notApiClientRefresh", true);
        status.put("notAlertWrite", true);
        status.put("notTradingSignal", true);
        status.put("notCandidateSignal", true);
        status.put("notDecisionGeneration", true);
        status.put("notPointSignal", true);
        status.put("notExecutable", true);
        status.put("displaySlotsAreCandidatePool", false);
        status.put("failClosed", true);
        status.put("statusMapping", List.of(
                ALERT_POLICY_READY,
                ALERT_POLICY_BACKEND_PENDING_FAIL_CLOSED,
                ALERT_READ_MODEL_MISSING_FAIL_CLOSED,
                ALERT_RECENT_EMPTY_REVIEW_ONLY,
                ALERT_SUPPRESSION_ACTIVE_REVIEW_ONLY,
                ALERT_COOLDOWN_ACTIVE_REVIEW_ONLY,
                ALERT_DUPLICATE_RISK_REVIEW_ONLY,
                ALERT_FATIGUE_HIGH_REVIEW_ONLY,
                NOTIFICATION_POLICY_MISSING_FAIL_CLOSED,
                PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED,
                RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED
        ));
        return status;
    }

    private Map<String, Object> baseInternalPushPreviewNotificationStatus(String normalizedSymbol) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", INTERNAL_PUSH_PREVIEW_MISSING_FAIL_CLOSED);
        status.put("symbol", normalizedSymbol);
        status.put("internalPushPreviewStatus", INTERNAL_PUSH_PREVIEW_MISSING_FAIL_CLOSED);
        status.put("notificationPreviewStatus", NOTIFICATION_PREVIEW_READY);
        status.put("assemblerStatus", INTERNAL_PUSH_PREVIEW_ASSEMBLER_READY);
        status.put("externalChannelPolicyStatus", NO_OP_EXTERNAL_CHANNEL_POLICY_READY);
        status.put("duplicateAlertNotificationPolicyStatus", DUPLICATE_ALERT_NOTIFICATION_POLICY_REVIEW_REQUIRED);
        status.put("ownerPath", "ReviewOnlyCandidatePreviewGuardDTO -> ReviewOnlyInternalPushPreviewAssembler -> ReviewOnlyInternalPushPreviewDTO -> dashboard internalPushPreviewDisplay");
        status.put("displayContext", "dashboard internalPushPreviewDisplay review-only display gate");
        status.put("noOpExternalChannelPolicy", "NoOpOpportunityPushExternalChannelPolicy disabled-channel evidence only");
        status.put("previewOwnerInputAvailable", false);
        status.put("internalPushPreviewAvailable", false);
        status.put("sourceHealth", "MISSING");
        status.put("reason", "INTERNAL_PUSH_PREVIEW_STATUS_PENDING");
        status.put("message", "Internal Push preview / notification preview 只读状态待确认；不是 Push send 或外部通道。");
        status.put("reviewOnly", true);
        status.put("manualReviewOnly", true);
        status.put("notPushSend", true);
        status.put("notExternalChannel", true);
        status.put("notPushSnapshotWrite", true);
        status.put("notRecheckExecution", true);
        status.put("notReplayExecution", true);
        status.put("notCandidateSignal", true);
        status.put("notDecisionGeneration", true);
        status.put("notPointSignal", true);
        status.put("notFinalDirection", true);
        status.put("notEntryStopTpRr", true);
        status.put("notTradingSignal", true);
        status.put("notExecutable", true);
        status.put("displaySlotsAreCandidatePool", false);
        status.put("notSendableMessage", true);
        status.put("notProviderPayload", true);
        status.put("notPositionMonitorExecution", true);
        status.put("notExternalApiRefresh", true);
        status.put("notSchedulerTrigger", true);
        status.put("notCollectorTrigger", true);
        status.put("pushSendBoundaryStatus", PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("externalChannelBoundaryStatus", EXTERNAL_CHANNEL_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("pushSnapshotWriteBoundaryStatus", PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("recheckBoundaryStatus", RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("replayBoundaryStatus", REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("candidateBoundaryStatus", CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("decisionGenerationBoundaryStatus", DECISION_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("pointBoundaryStatus", POINT_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("tradingBoundaryStatus", TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("previewBlockingReasons", List.of());
        status.put("previewRiskBlockers", List.of());
        status.put("failClosed", true);
        status.put("statusMapping", List.of(
                INTERNAL_PUSH_PREVIEW_READY,
                INTERNAL_PUSH_PREVIEW_MISSING_FAIL_CLOSED,
                INTERNAL_PUSH_PREVIEW_PARTIAL_REVIEW_ONLY,
                NOTIFICATION_PREVIEW_READY,
                NOTIFICATION_PREVIEW_MISSING_FAIL_CLOSED,
                INTERNAL_PUSH_PREVIEW_ASSEMBLER_READY,
                NO_OP_EXTERNAL_CHANNEL_POLICY_READY,
                DUPLICATE_ALERT_NOTIFICATION_POLICY_REVIEW_REQUIRED,
                PUSH_SEND_BOUNDARY_BLOCKED_FAIL_CLOSED,
                EXTERNAL_CHANNEL_BOUNDARY_BLOCKED_FAIL_CLOSED,
                PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED,
                RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED,
                REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED,
                CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED,
                DECISION_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED,
                POINT_BOUNDARY_BLOCKED_FAIL_CLOSED,
                TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED
        ));
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

    private Map<String, Object> baseSourceRuntimeDataQualityStatus(String normalizedSymbol) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", SOURCE_TRACE_MISSING_FAIL_CLOSED);
        status.put("symbol", normalizedSymbol);
        status.put("analysisId", null);
        status.put("sourceTraceAvailable", false);
        status.put("runtimeKlineContextAvailable", false);
        status.put("sourceTraceStatus", SOURCE_TRACE_MISSING_FAIL_CLOSED);
        status.put("runtimeKlineStatus", RUNTIME_KLINE_CONTEXT_MISSING_FAIL_CLOSED);
        status.put("persistedOhlcvReadiness", "UNKNOWN");
        status.put("persistedOhlcvStatus", PERSISTED_OHLCV_MISSING_FAIL_CLOSED);
        status.put("persistedOhlcvStaleReason", "UNKNOWN");
        status.put("persistedOhlcvMissingFields", Collections.emptyList());
        status.put("dataQualityAvailable", false);
        status.put("dataQualityScore", null);
        status.put("dataQualityStatus", DATA_QUALITY_BLOCKED_FAIL_CLOSED);
        status.put("multiTimeframeAvailable", false);
        status.put("multiTimeframeSummary", "missing");
        status.put("multiTimeframeStatus", MULTITIMEFRAME_MISSING_FAIL_CLOSED);
        status.put("refreshBoundaryStatus", REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("generationBoundaryStatus", GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("sourceHealth", "MISSING");
        status.put("reason", "SOURCE_RUNTIME_STATUS_PENDING");
        status.put("message", "SourceTrace / RuntimeKline / DataQuality / MultiTimeframe 只读状态待确认；不刷新、不生成、不执行。");
        status.put("reviewOnly", true);
        status.put("notCandidateSignal", true);
        status.put("notDecisionGeneration", true);
        status.put("notPointSignal", true);
        status.put("notFinalDirection", true);
        status.put("notEntryStopTpRr", true);
        status.put("notTradingSignal", true);
        status.put("notExecutable", true);
        status.put("notSchedulerTrigger", true);
        status.put("notCollectorTrigger", true);
        status.put("notApiClientRefresh", true);
        status.put("notExternalRefresh", true);
        status.put("notSourceBindingGeneration", true);
        status.put("displaySlotsAreCandidatePool", false);
        status.put("failClosed", true);
        status.put("statusMapping", List.of(
                SOURCE_RUNTIME_READY,
                SOURCE_TRACE_MISSING_FAIL_CLOSED,
                SOURCE_TRACE_PARTIAL_REVIEW_ONLY,
                RUNTIME_KLINE_CONTEXT_READY_REVIEW_ONLY,
                RUNTIME_KLINE_CONTEXT_MISSING_FAIL_CLOSED,
                PERSISTED_OHLCV_READY_REVIEW_ONLY,
                PERSISTED_OHLCV_STALE_REVIEW_ONLY,
                PERSISTED_OHLCV_MISSING_FAIL_CLOSED,
                DATA_QUALITY_PARTIAL_REVIEW_ONLY,
                DATA_QUALITY_BLOCKED_FAIL_CLOSED,
                MULTITIMEFRAME_ALIGNMENT_REVIEW_ONLY,
                MULTITIMEFRAME_CONFLICT_REVIEW_ONLY,
                MULTITIMEFRAME_MISSING_FAIL_CLOSED,
                REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED,
                GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED
        ));
        return status;
    }

    private Map<String, Object> basePaperObservationStatus(String normalizedSymbol) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", PAPER_OBSERVATION_BACKEND_PENDING_FAIL_CLOSED);
        status.put("symbol", normalizedSymbol);
        status.put("analysisId", null);
        status.put("paperObservationStatus", "BACKEND_PENDING");
        status.put("paperObservationStatusLabel", "后端未接入");
        status.put("paperObservationAvailable", false);
        status.put("manualReviewSurfaceAvailable", false);
        status.put("linkedPaperObservationCount", 0);
        status.put("linkedReviewCount", 0);
        status.put("missedOpportunityFlag", false);
        status.put("reviewSummary", "missing");
        status.put("backendConnectionStatus", "BACKEND_PENDING");
        status.put("ownerPath", "dashboardDetail.paperObservationDisplay");
        status.put("sourceHealth", "BLOCKED");
        status.put("reason", "PAPER_OBSERVATION_STATUS_PENDING");
        status.put("message", "Paper Observation / Paper Trading 只读状态待确认；不是纸面订单、模拟执行、真实持仓或交易指令。");
        status.put("reviewOnly", true);
        status.put("manualReviewOnly", true);
        status.put("notRealPosition", true);
        status.put("notTradeInstruction", true);
        status.put("notPaperOrder", true);
        status.put("notSimulatedExecution", true);
        status.put("notPaperPnlGeneration", true);
        status.put("notPositionMonitorExecution", true);
        status.put("notCandidateSignal", true);
        status.put("notDecisionGeneration", true);
        status.put("notPointSignal", true);
        status.put("notFinalDirection", true);
        status.put("notEntryStopTpRr", true);
        status.put("notTradingSignal", true);
        status.put("notExecutable", true);
        status.put("displaySlotsAreCandidatePool", false);
        status.put("paperExecutionBoundaryStatus", PAPER_ORDER_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("simulatedExecutionBoundaryStatus", SIMULATED_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("paperPnlBoundaryStatus", PAPER_PNL_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("positionMonitorBoundaryStatus", POSITION_MONITOR_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("pointBoundaryStatus", POINT_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("tradingBoundaryStatus", TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("paperOwnerSafetyFlagsAllTrue", false);
        status.put("failClosed", true);
        status.put("statusMapping", List.of(
                PAPER_OBSERVATION_READY,
                PAPER_OBSERVATION_BACKEND_PENDING_FAIL_CLOSED,
                PAPER_OBSERVATION_MISSING_FAIL_CLOSED,
                PAPER_OBSERVATION_PARTIAL_REVIEW_ONLY,
                NOT_REAL_POSITION_REVIEW_ONLY,
                NOT_TRADE_INSTRUCTION_REVIEW_ONLY,
                PAPER_ORDER_BOUNDARY_BLOCKED_FAIL_CLOSED,
                SIMULATED_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED,
                PAPER_PNL_BOUNDARY_BLOCKED_FAIL_CLOSED,
                POSITION_MONITOR_BOUNDARY_BLOCKED_FAIL_CLOSED,
                POINT_BOUNDARY_BLOCKED_FAIL_CLOSED,
                TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED
        ));
        return status;
    }

    private Map<String, Object> baseAccountRiskExposureStatus(String normalizedSymbol) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", ACCOUNT_RISK_STATUS_BACKEND_PENDING_FAIL_CLOSED);
        status.put("symbol", normalizedSymbol);
        status.put("analysisId", null);
        status.put("snapshotId", null);
        status.put("snapshotSymbol", normalizedSymbol);
        status.put("riskLevelSnapshot", "UNKNOWN");
        status.put("riskAllowedEvidence", null);
        status.put("riskAllowedStatus", ACCOUNT_RISK_STATUS_MISSING_FAIL_CLOSED);
        status.put("riskReasonCode", "UNKNOWN");
        status.put("riskReasonText", "missing");
        status.put("positionExposure", null);
        status.put("maxAllowedExposure", null);
        status.put("accountExposureStatus", ACCOUNT_EXPOSURE_MISSING_FAIL_CLOSED);
        status.put("snapshotSource", "UNKNOWN");
        status.put("snapshotVersion", null);
        status.put("sourceNote", "missing");
        status.put("traceId", "missing");
        status.put("snapshotCreateTime", null);
        status.put("ownerPath", "AccountRiskSnapshotMapper.selectLatestByAnalysisId");
        status.put("historicalSnapshotReadOnly", "AccountRiskSnapshotMapper.selectById is historical snapshot read only and not the runtime owner path");
        status.put("displayContext", "ReviewAggregate / review-page account-risk fields are display context only, not execution entry");
        status.put("sourceHealth", "BLOCKED");
        status.put("reason", "ACCOUNT_RISK_EXPOSURE_STATUS_PENDING");
        status.put("message", "Account risk / exposure 只读状态待确认；不是交易授权、仓位大小或账户动作。");
        status.put("reviewOnly", true);
        status.put("manualReviewOnly", true);
        status.put("notAccountRiskWrite", true);
        status.put("notPushSnapshotWrite", true);
        status.put("notPushSend", true);
        status.put("notRecheckExecution", true);
        status.put("notTradingAuthorization", true);
        status.put("notPositionSizing", true);
        status.put("notReduceCloseStopReverseGuidance", true);
        status.put("notCandidateSignal", true);
        status.put("notDecisionGeneration", true);
        status.put("notPointSignal", true);
        status.put("notFinalDirection", true);
        status.put("notEntryStopTpRr", true);
        status.put("notTradingSignal", true);
        status.put("notExecutable", true);
        status.put("displaySlotsAreCandidatePool", false);
        status.put("accountRiskWriteBoundaryStatus", ACCOUNT_RISK_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("pushSnapshotWriteBoundaryStatus", PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("pushBoundaryStatus", PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("recheckBoundaryStatus", RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("tradingAuthorizationBoundaryStatus", TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("positionSizingBoundaryStatus", POSITION_SIZING_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("reduceCloseStopReverseBoundaryStatus", REDUCE_CLOSE_STOP_REVERSE_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("candidateBoundaryStatus", CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("pointBoundaryStatus", POINT_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("tradingBoundaryStatus", TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("failClosed", true);
        status.put("statusMapping", List.of(
                ACCOUNT_RISK_STATUS_READY,
                ACCOUNT_RISK_STATUS_BACKEND_PENDING_FAIL_CLOSED,
                ACCOUNT_RISK_STATUS_MISSING_FAIL_CLOSED,
                ACCOUNT_RISK_STATUS_PARTIAL_REVIEW_ONLY,
                ACCOUNT_EXPOSURE_READY,
                ACCOUNT_EXPOSURE_MISSING_FAIL_CLOSED,
                RISK_ALLOWED_READ_ONLY_EVIDENCE,
                ACCOUNT_RISK_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED,
                PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED,
                PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED,
                RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED,
                TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED,
                POSITION_SIZING_BOUNDARY_BLOCKED_FAIL_CLOSED,
                REDUCE_CLOSE_STOP_REVERSE_BOUNDARY_BLOCKED_FAIL_CLOSED,
                CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED,
                POINT_BOUNDARY_BLOCKED_FAIL_CLOSED,
                TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED
        ));
        return status;
    }

    private Map<String, Object> baseHotResetEventImpactSourceStatus(String normalizedSymbol) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED);
        status.put("symbol", normalizedSymbol);
        status.put("analysisId", null);
        status.put("timeframe", "UNKNOWN");
        status.put("hotResetEventAvailable", false);
        status.put("hotResetEventSourceStatus", HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED);
        status.put("eventImpactSourceStatus", EVENT_IMPACT_SOURCE_MISSING_FAIL_CLOSED);
        status.put("sourceTraceEventSourceOwnershipStatus", SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED);
        status.put("sourceTraceEventSourceOwnershipMissingReason", "MISSING_SOURCE");
        status.put("sourceTraceEventSourceOwnershipMissingFields", List.of("eventSource"));
        status.put("hotResetEventCount", 0);
        status.put("hotResetEventLatestTime", null);
        status.put("hotResetTriggerType", "missing");
        status.put("hotResetTriggerValue", "missing");
        status.put("hotResetTriggerReasonCode", "missing");
        status.put("hotResetTriggerReasonText", "missing");
        status.put("eventImpactSource", "HotResetEventDO persisted event source evidence");
        status.put("ownerPath", "DecisionResult.latest.analysisId -> HotResetEventMapper.selectLatestByAnalysisId/countByAnalysisId -> SourceTraceEventSourceOwnershipService.resolveEventSourceOwnership");
        status.put("displayContext", "dashboard.html / review-page.js display context only; no event action or trading action");
        status.put("sourceHealth", "MISSING");
        status.put("reason", "HOT_RESET_EVENT_IMPACT_SOURCE_STATUS_PENDING");
        status.put("message", "Hot Reset / Event Impact Source 只读状态待确认；不是热重置执行、事件生成或交易信号。");
        status.put("reviewOnly", true);
        status.put("manualReviewOnly", true);
        status.put("notHotResetExecution", true);
        status.put("notHotResetWrite", true);
        status.put("notEventGeneration", true);
        status.put("notExternalApiRefresh", true);
        status.put("notNewsFetch", true);
        status.put("notSchedulerTrigger", true);
        status.put("notCollectorTrigger", true);
        status.put("notPushSend", true);
        status.put("notExternalChannel", true);
        status.put("notRecheckExecution", true);
        status.put("notReplayExecution", true);
        status.put("notCandidateSignal", true);
        status.put("notDecisionGeneration", true);
        status.put("notPointSignal", true);
        status.put("notFinalDirection", true);
        status.put("notEntryStopTpRr", true);
        status.put("notTradingSignal", true);
        status.put("notExecutable", true);
        status.put("displaySlotsAreCandidatePool", false);
        status.put("hotResetExecutionBoundaryStatus", HOT_RESET_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("hotResetWriteBoundaryStatus", HOT_RESET_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("eventGenerationBoundaryStatus", EVENT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("externalApiRefreshBoundaryStatus", EXTERNAL_API_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("newsFetchBoundaryStatus", NEWS_FETCH_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("schedulerTriggerBoundaryStatus", SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("collectorTriggerBoundaryStatus", COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("pushBoundaryStatus", PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("recheckReplayBoundaryStatus", RECHECK_REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("candidateBoundaryStatus", CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("pointBoundaryStatus", POINT_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("tradingBoundaryStatus", TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED);
        status.put("failClosed", true);
        status.put("statusMapping", List.of(
                HOT_RESET_EVENT_SOURCE_READY,
                HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED,
                HOT_RESET_EVENT_SOURCE_PARTIAL_REVIEW_ONLY,
                EVENT_IMPACT_SOURCE_READY,
                EVENT_IMPACT_SOURCE_MISSING_FAIL_CLOSED,
                SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_READY,
                SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED,
                HOT_RESET_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED,
                HOT_RESET_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED,
                EVENT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED,
                EXTERNAL_API_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED,
                NEWS_FETCH_BOUNDARY_BLOCKED_FAIL_CLOSED,
                SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED,
                COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED,
                PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED,
                RECHECK_REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED,
                CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED,
                POINT_BOUNDARY_BLOCKED_FAIL_CLOSED,
                TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED
        ));
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

    private void applyAlertFatiguePolicyStatus(Map<String, Object> status,
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

    private void applyInternalPushPreviewNotificationStatus(Map<String, Object> status,
                                                            String statusValue,
                                                            String reason,
                                                            String message,
                                                            boolean failClosed,
                                                            String sourceHealth) {
        status.put("status", statusValue);
        status.put("internalPushPreviewStatus", statusValue);
        status.put("reason", reason);
        status.put("message", message);
        status.put("failClosed", failClosed);
        status.put("sourceHealth", sourceHealth);
    }

    private void populateInternalPushPreviewNotificationStatus(Map<String, Object> status,
                                                               ReviewOnlyInternalPushPreviewDTO preview) {
        if (preview == null) {
            return;
        }
        status.put("previewOwnerInputAvailable", preview.getCandidatePreviewGuardStatus() != null);
        status.put("internalPushPreviewAvailable", !preview.isBlocked() && !preview.isFailClosed());
        status.put("previewDtoStatus", firstNonBlank(preview.getInternalPushPreviewStatus(), "BLOCKED_FAIL_CLOSED"));
        status.put("candidatePreviewGuardStatus", firstNonBlank(preview.getCandidatePreviewGuardStatus(), "MISSING"));
        status.put("previewAllowedNextStep", firstNonBlank(preview.getAllowedNextStep(), "BLOCKED_BY_CANDIDATE_PREVIEW_GUARD"));
        status.put("previewReviewOnlyMessage", firstNonBlank(preview.getReviewOnlyMessage(), "Review-only internal push preview remains fail-closed."));
        status.put("previewDtoReviewOnly", preview.isReviewOnly());
        status.put("previewDtoNotTradeInstruction", preview.isNotTradeInstruction());
        status.put("previewDtoManualReviewRequired", preview.isManualReviewRequired());
        status.put("previewDtoFailClosed", preview.isFailClosed());
        status.put("previewDtoBlocked", preview.isBlocked());
        status.put("previewBlockingReasons", preview.getBlockingReasons());
        status.put("previewRiskBlockers", preview.getRiskBlockers());
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

    private void applySourceRuntimeDataQualityStatus(Map<String, Object> status,
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

    private void applyPaperObservationStatus(Map<String, Object> status,
                                             DecisionResultVO decision,
                                             DashboardDetailResponseVO.PaperObservationDisplayVO paper) {
        if (decision == null) {
            applyPaperObservationStatus(
                    status,
                    PAPER_OBSERVATION_BACKEND_PENDING_FAIL_CLOSED,
                    "DECISION_MISSING",
                    "DecisionResult 缺失；Paper Observation / Paper Trading 只读状态 fail-closed，不生成纸面订单、模拟执行或交易指令。",
                    true,
                    "BLOCKED"
            );
            return;
        }
        if (paper == null) {
            applyPaperObservationStatus(
                    status,
                    PAPER_OBSERVATION_MISSING_FAIL_CLOSED,
                    "PAPER_OBSERVATION_DISPLAY_MISSING",
                    "PaperObservation display owner data 缺失；只读状态 fail-closed。",
                    true,
                    "MISSING"
            );
            return;
        }

        boolean safetyFlagsAllTrue = paperObservationSafetyFlagsAllTrue(paper);
        String displayStatus = normalizedStatus(paper.getPaperObservationStatus());
        String reviewSummary = normalizedStatus(paper.getReviewSummary());

        if (!Boolean.TRUE.equals(paper.getNotRealPosition())) {
            applyPaperObservationStatus(
                    status,
                    POSITION_MONITOR_BOUNDARY_BLOCKED_FAIL_CLOSED,
                    "REAL_POSITION_BOUNDARY_UNSAFE",
                    "Paper Observation owner path 未声明 notRealPosition；已按只读 fail-closed 处理，不执行真实持仓监控。",
                    true,
                    "BLOCKED"
            );
        } else if (!Boolean.TRUE.equals(paper.getNotTradeInstruction())) {
            applyPaperObservationStatus(
                    status,
                    TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED,
                    "TRADE_INSTRUCTION_BOUNDARY_UNSAFE",
                    "Paper Observation owner path 未声明 notTradeInstruction；已按只读 fail-closed 处理，不输出交易指令。",
                    true,
                    "BLOCKED"
            );
        } else if (Boolean.TRUE.equals(paper.getPaperObservationAvailable())
                || Boolean.TRUE.equals(paper.getManualReviewEntryAvailable())) {
            applyPaperObservationStatus(
                    status,
                    PAPER_ORDER_BOUNDARY_BLOCKED_FAIL_CLOSED,
                    "PAPER_EXECUTION_BOUNDARY_UNSAFE",
                    "Paper Observation owner path 暴露可用入口；已阻断为只读状态，不生成纸面订单或模拟执行。",
                    true,
                    "BLOCKED"
            );
        } else if (!safetyFlagsAllTrue) {
            applyPaperObservationStatus(
                    status,
                    PAPER_OBSERVATION_PARTIAL_REVIEW_ONLY,
                    "PAPER_OBSERVATION_SAFETY_PARTIAL",
                    "Paper Observation 安全字段不完整；仅显示只读部分状态，不授予任何执行权限。",
                    true,
                    "PARTIAL"
            );
        } else if (!hasText(displayStatus) || "BACKEND_PENDING".equals(displayStatus)) {
            applyPaperObservationStatus(
                    status,
                    PAPER_OBSERVATION_BACKEND_PENDING_FAIL_CLOSED,
                    firstNonBlank(paper.getReviewSummary(), "BACKEND_PENDING"),
                    "Paper Observation owner path 仍是后端待接入或上游未满足；只读状态 fail-closed。",
                    true,
                    "MISSING"
            );
        } else if ("MANUAL_REVIEW_REQUIRED".equals(displayStatus)
                && "AVAILABLE_REVIEW_ONLY".equals(reviewSummary)) {
            applyPaperObservationStatus(
                    status,
                    PAPER_OBSERVATION_READY,
                    "PAPER_OBSERVATION_OWNER_PATH_READ",
                    "Paper Observation / Paper Trading 只读状态可读；仅人工复核，不是纸面订单、模拟执行、真实持仓或交易指令。",
                    false,
                    "OK"
            );
        } else {
            applyPaperObservationStatus(
                    status,
                    PAPER_OBSERVATION_PARTIAL_REVIEW_ONLY,
                    firstNonBlank(paper.getReviewSummary(), "PAPER_OBSERVATION_PARTIAL"),
                    "Paper Observation 只读状态部分可读；保持人工复核，不生成纸面订单、模拟执行、纸面盈亏或交易动作。",
                    true,
                    "PARTIAL"
            );
        }
    }

    private void applyPaperObservationStatus(Map<String, Object> status,
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

    private void applyAccountRiskExposureStatus(Map<String, Object> status,
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

    private void applyHotResetEventImpactSourceStatus(Map<String, Object> status,
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

    private SourceTraceEventSourceOwnershipResult resolveEventSourceOwnership(String normalizedSymbol,
                                                                             String timeframe) {
        RuntimeKlineContextDTO context = new RuntimeKlineContextDTO();
        context.setSymbol(normalizedSymbol);
        context.setTimeframe(firstNonBlank(timeframe, "UNKNOWN"));
        if (sourceTraceEventSourceOwnershipService == null) {
            return SourceTraceEventSourceOwnershipResult.missingSource(normalizedSymbol, context.getTimeframe());
        }
        SourceTraceEventSourceOwnershipResult result =
                sourceTraceEventSourceOwnershipService.resolveEventSourceOwnership(context);
        return result != null ? result : SourceTraceEventSourceOwnershipResult.missingSource(normalizedSymbol, context.getTimeframe());
    }

    private void applySourceTraceEventSourceOwnership(Map<String, Object> status,
                                                      SourceTraceEventSourceOwnershipResult ownership) {
        boolean ready = ownership != null
                && ownership.getOwnershipStatus() != SourceTraceEventSourceOwnershipStatusEnum.INCOMPLETE
                && hasText(ownership.getEventSource());
        status.put("sourceTraceEventSourceOwnershipStatus", ready
                ? SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_READY
                : SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED);
        status.put("sourceTraceEventSourceOwnershipReviewMode",
                ownership != null && ownership.getReviewMode() != null ? ownership.getReviewMode().name() : "REVIEW_ONLY");
        status.put("sourceTraceEventSourceOwnershipMissingReason",
                ownership != null && ownership.getMissingReason() != null ? ownership.getMissingReason().name() : "MISSING_SOURCE");
        status.put("sourceTraceEventSourceOwnershipMissingFields",
                ownership != null ? ownership.getMissingFields() : List.of("eventSource"));
        status.put("sourceTraceEventSourceOwnershipManualReviewRequired",
                ownership == null || ownership.isManualReviewRequired());
        status.put("sourceTraceEventSourceOwnershipNotTradeInstruction",
                ownership == null || ownership.isNotTradeInstruction());
    }

    private void populateHotResetEventImpactSource(Map<String, Object> status,
                                                   HotResetEventDO latestEvent) {
        status.put("hotResetEventAvailable", true);
        status.put("hotResetEventId", latestEvent.getEventId());
        status.put("hotResetTraceId", latestEvent.getTraceId());
        status.put("hotResetDecisionId", latestEvent.getDecisionId());
        status.put("hotResetDecisionState", firstNonBlank(latestEvent.getDecisionState(), "UNKNOWN"));
        status.put("hotResetTriggerType", firstNonBlank(latestEvent.getTriggerType(), "missing"));
        status.put("hotResetTriggerValue", firstNonBlank(latestEvent.getTriggerValue(), "missing"));
        status.put("hotResetTriggerReasonCode", firstNonBlank(latestEvent.getTriggerReasonCode(), "missing"));
        status.put("hotResetTriggerReasonText", firstNonBlank(latestEvent.getTriggerReasonText(), "missing"));
        status.put("hotResetEventVersion", latestEvent.getEventVersion());
        status.put("hotResetEventLatestTime", latestEvent.getEventTime());
        status.put("hotResetEventMeaning", "read-only event source evidence only; not Hot Reset execution");
        status.put("eventImpactMeaning", "read-only impact source status only; not event generation");
    }

    private String accountExposureStatus(TmAccountRiskSnapshotDO snapshot) {
        if (snapshot == null || snapshot.getPositionExposure() == null || snapshot.getMaxAllowedExposure() == null) {
            return ACCOUNT_EXPOSURE_MISSING_FAIL_CLOSED;
        }
        return ACCOUNT_EXPOSURE_READY;
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

    private String resolveSourceTraceReadinessStatus(SourceTraceDTO sourceTrace) {
        if (sourceTrace == null) {
            return SOURCE_TRACE_MISSING_FAIL_CLOSED;
        }
        if ((sourceTrace.getMissingFields() != null && !sourceTrace.getMissingFields().isEmpty())
                || (sourceTrace.getBlockingReasons() != null && !sourceTrace.getBlockingReasons().isEmpty())
                || sourceTrace.getFallbackStatus() != null) {
            return SOURCE_TRACE_PARTIAL_REVIEW_ONLY;
        }
        return SOURCE_RUNTIME_READY;
    }

    private String resolveRuntimeKlineContextStatus(RuntimeKlineContextDTO runtimeKline) {
        if (runtimeKline == null) {
            return RUNTIME_KLINE_CONTEXT_MISSING_FAIL_CLOSED;
        }
        return RUNTIME_KLINE_CONTEXT_READY_REVIEW_ONLY;
    }

    private String resolvePersistedOhlcvReadiness(SourceTraceDTO sourceTrace, RuntimeKlineContextDTO runtimeKline) {
        if (runtimeKline != null && hasText(runtimeKline.getPersistedOhlcvReadinessStatus())) {
            return runtimeKline.getPersistedOhlcvReadinessStatus().trim().toUpperCase();
        }
        if (sourceTrace != null && hasText(sourceTrace.getRuntimeKlineReadinessStatus())) {
            return sourceTrace.getRuntimeKlineReadinessStatus().trim().toUpperCase();
        }
        return "UNKNOWN";
    }

    private String resolvePersistedOhlcvStatus(String readiness) {
        String normalized = normalizedStatus(readiness);
        if ("FRESH".equals(normalized)) {
            return PERSISTED_OHLCV_READY_REVIEW_ONLY;
        }
        if ("STALE".equals(normalized) || "PARTIAL".equals(normalized) || "INVALID".equals(normalized)) {
            return PERSISTED_OHLCV_STALE_REVIEW_ONLY;
        }
        return PERSISTED_OHLCV_MISSING_FAIL_CLOSED;
    }

    private String resolvePersistedOhlcvStaleReason(SourceTraceDTO sourceTrace, RuntimeKlineContextDTO runtimeKline) {
        if (runtimeKline != null && hasText(runtimeKline.getPersistedOhlcvStaleReasonText())) {
            return runtimeKline.getPersistedOhlcvStaleReasonText();
        }
        if (sourceTrace != null && hasText(sourceTrace.getRuntimeKlineStaleReasonText())) {
            return sourceTrace.getRuntimeKlineStaleReasonText();
        }
        if (runtimeKline != null && hasText(runtimeKline.getPersistedOhlcvStaleReasonCode())) {
            return runtimeKline.getPersistedOhlcvStaleReasonCode();
        }
        if (sourceTrace != null && hasText(sourceTrace.getRuntimeKlineStaleReasonCode())) {
            return sourceTrace.getRuntimeKlineStaleReasonCode();
        }
        return "UNKNOWN";
    }

    private List<String> resolvePersistedOhlcvMissingFields(SourceTraceDTO sourceTrace, RuntimeKlineContextDTO runtimeKline) {
        if (runtimeKline != null && runtimeKline.getPersistedOhlcvMissingFields() != null
                && !runtimeKline.getPersistedOhlcvMissingFields().isEmpty()) {
            return runtimeKline.getPersistedOhlcvMissingFields();
        }
        if (sourceTrace != null && sourceTrace.getRuntimeKlineReadinessMissingFields() != null) {
            return sourceTrace.getRuntimeKlineReadinessMissingFields();
        }
        return Collections.emptyList();
    }

    private Object resolveDataQualityScore(SourceTraceDTO sourceTrace, RuntimeKlineContextDTO runtimeKline) {
        if (sourceTrace != null && sourceTrace.getDataQualityScore() != null) {
            return sourceTrace.getDataQualityScore();
        }
        return runtimeKline != null ? runtimeKline.getDataQualityScore() : null;
    }

    private String resolveDataQualityStatus(Object dataQualityScore, SourceTraceDTO sourceTrace, RuntimeKlineContextDTO runtimeKline) {
        if (dataQualityScore == null) {
            return DATA_QUALITY_BLOCKED_FAIL_CLOSED;
        }
        if (sourceTrace == null || runtimeKline == null) {
            return DATA_QUALITY_BLOCKED_FAIL_CLOSED;
        }
        return DATA_QUALITY_PARTIAL_REVIEW_ONLY;
    }

    private String resolveMultiTimeframeSource(SourceTraceDTO sourceTrace, RuntimeKlineContextDTO runtimeKline) {
        if (sourceTrace != null && hasText(sourceTrace.getMultiTimeframeSource())) {
            return sourceTrace.getMultiTimeframeSource();
        }
        return runtimeKline != null ? runtimeKline.getMultiTimeframeSource() : null;
    }

    private String resolveMultiTimeframeStatus(String multiTimeframeSource) {
        if (!hasText(multiTimeframeSource)) {
            return MULTITIMEFRAME_MISSING_FAIL_CLOSED;
        }
        String normalized = multiTimeframeSource.trim().toUpperCase();
        if (normalized.contains("CONFLICT")
                || normalized.contains("DIVERGENCE")
                || normalized.contains("MISMATCH")
                || normalized.contains("冲突")) {
            return MULTITIMEFRAME_CONFLICT_REVIEW_ONLY;
        }
        return MULTITIMEFRAME_ALIGNMENT_REVIEW_ONLY;
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

    private boolean paperObservationSafetyFlagsAllTrue(DashboardDetailResponseVO.PaperObservationDisplayVO paper) {
        if (paper == null) {
            return false;
        }
        return Boolean.TRUE.equals(paper.getNotRealPosition())
                && Boolean.TRUE.equals(paper.getNotTradeInstruction())
                && Boolean.TRUE.equals(paper.getManualReviewRequired())
                && !Boolean.TRUE.equals(paper.getPaperObservationAvailable())
                && !Boolean.TRUE.equals(paper.getManualReviewEntryAvailable());
    }

    private int safeInteger(Integer value) {
        return value != null ? value : 0;
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

    private boolean alertMatchesSymbol(MonitorAlertDO alert, String normalizedSymbol) {
        if (alert == null) {
            return false;
        }
        String alertSymbol = alert.getAssetSymbol();
        return !hasText(alertSymbol) || normalizedSymbol.equalsIgnoreCase(alertSymbol.trim());
    }

    private boolean isOpenMonitorAlert(MonitorAlertDO alert) {
        if (alert == null || !hasText(alert.getStatus())) {
            return true;
        }
        String status = alert.getStatus().trim().toUpperCase();
        return !("CLOSED".equals(status) || "RESOLVED".equals(status) || "SUPPRESSED".equals(status));
    }

    private boolean isSuppressedMonitorAlert(MonitorAlertDO alert) {
        if (alert == null) {
            return false;
        }
        String status = normalizedStatus(alert.getStatus());
        return "SUPPRESSED".equals(status) || hasText(alert.getSuppressReason());
    }

    private boolean duplicateAlertTypeRisk(List<MonitorAlertDO> alerts) {
        if (alerts == null || alerts.size() < 2) {
            return false;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (MonitorAlertDO alert : alerts) {
            String type = firstNonBlank(alert != null ? alert.getAlertType() : null, "UNKNOWN");
            counts.put(type, counts.getOrDefault(type, 0) + 1);
            if (counts.get(type) > 1) {
                return true;
            }
        }
        return false;
    }

    private String latestAlertType(List<MonitorAlertDO> alerts) {
        if (alerts == null || alerts.isEmpty() || alerts.get(0) == null) {
            return null;
        }
        return firstNonBlank(alerts.get(0).getAlertType(), "UNKNOWN");
    }

    private String latestAlertLevel(List<MonitorAlertDO> alerts) {
        if (alerts == null || alerts.isEmpty() || alerts.get(0) == null) {
            return null;
        }
        return firstNonBlank(alerts.get(0).getAlertLevel(), "UNKNOWN");
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
