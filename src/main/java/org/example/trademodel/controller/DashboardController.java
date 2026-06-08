package org.example.trademodel.controller;

import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.entity.MarketEnvironmentSnapshotDO;
import org.example.trademodel.mapper.MarketEnvironmentSnapshotMapper;
import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.service.EvidenceService;
import org.example.trademodel.service.ScoreService;
import org.example.trademodel.service.dashboard.DashboardSourceTraceDetailAdapter;
import org.example.trademodel.service.dashboard.ExecutionPlanDisplayAdapter;
import org.example.trademodel.service.dashboard.PaperObservationDisplayAdapter;
import org.example.trademodel.service.dashboard.PlanBoundaryDisplayAdapter;
import org.example.trademodel.service.dashboard.RiskActionGuardDisplayAdapter;
import org.example.trademodel.vo.EvidenceBriefVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
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

    private final DecisionService decisionService;
    private final SystemHealthService systemHealthService;
    private final MonitorService monitorService;
    private final RuntimeMetricService runtimeMetricService;
    private final RealMarketEnvironmentService realMarketEnvironmentService;
    private final MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper;
    private final EvidenceService evidenceService;
    private final ScoreService scoreService;
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
}
