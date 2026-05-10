package org.example.trademodel.controller;

import org.example.trademodel.entity.MarketEnvironmentSnapshotDO;
import org.example.trademodel.mapper.MarketEnvironmentSnapshotMapper;
import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.service.EvidenceService;
import org.example.trademodel.service.ScoreService;
import org.example.trademodel.vo.EvidenceBriefVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.vo.ScoreBriefVO;
import org.example.trademodel.vo.ScoreEightItemVO;
import org.example.trademodel.vo.AssetEventTimelineItemVO;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.PlanReadinessService;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.service.SystemHealthService;
import org.example.trademodel.vo.AnalysisReviewSummaryVO;
import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.DashboardSummaryResponseVO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import java.util.Collections;
import java.util.List;
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
    private static final int ASSET_EVENT_TIMELINE_LIMIT = 5;

    private final DecisionService decisionService;
    private final SystemHealthService systemHealthService;
    private final MonitorService monitorService;
    private final RuntimeMetricService runtimeMetricService;
    private final RealMarketEnvironmentService realMarketEnvironmentService;
    private final MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper;
    private final EvidenceService evidenceService;
    private final ScoreService scoreService;
    private final ReviewService reviewService;
    private final PlanReadinessService planReadinessService;

    public DashboardController(DecisionService decisionService,
                               SystemHealthService systemHealthService,
                               MonitorService monitorService,
                               RuntimeMetricService runtimeMetricService,
                               RealMarketEnvironmentService realMarketEnvironmentService,
                               MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper,
                               EvidenceService evidenceService,
                               ScoreService scoreService,
                               ReviewService reviewService,
                               PlanReadinessService planReadinessService) {
        this.decisionService = decisionService;
        this.systemHealthService = systemHealthService;
        this.monitorService = monitorService;
        this.runtimeMetricService = runtimeMetricService;
        this.realMarketEnvironmentService = realMarketEnvironmentService;
        this.marketEnvironmentSnapshotMapper = marketEnvironmentSnapshotMapper;
        this.evidenceService = evidenceService;
        this.scoreService = scoreService;
        this.reviewService = reviewService;
        this.planReadinessService = planReadinessService;
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
        DashboardDetailResponseVO body = new DashboardDetailResponseVO();
        body.setSymbol(normalizedSymbol);
        body.setDecision(decisionService.getLatestDecisionResultBySymbol(normalizedSymbol));
        body.setPlanReadiness(planReadinessService.derive(body.getDecision()));
        body.setMarketEnvironmentMini(resolveMarketEnvironmentMini(normalizedSymbol, body));
        body.setEvidenceTopItems(resolveEvidenceTopItems(body));
        body.setScoreTopItems(resolveScoreTopItems(body));
        body.setScoreEightItems(resolveScoreEightItems(body));
        body.setReviewSummary(resolveReviewSummary(body));
        body.setAssetEventTimeline(resolveAssetEventTimeline(body));
        runtimeMetricService.recordDuration("dashboard.detail", System.currentTimeMillis() - methodStart);
        return body;
    }

    private AnalysisReviewSummaryVO resolveReviewSummary(DashboardDetailResponseVO body) {
        if (body == null || body.getDecision() == null || reviewService == null) {
            return null;
        }
        String analysisId = body.getDecision().getAnalysisId();
        if (analysisId == null || analysisId.isBlank()) {
            return null;
        }
        return reviewService.getAnalysisReviewSummary(analysisId.trim());
    }

    private List<AssetEventTimelineItemVO> resolveAssetEventTimeline(DashboardDetailResponseVO body) {
        if (body == null || body.getDecision() == null || monitorService == null) {
            return Collections.emptyList();
        }
        String analysisId = body.getDecision().getAnalysisId();
        if (analysisId == null || analysisId.isBlank()) {
            return Collections.emptyList();
        }
        List<AssetEventTimelineItemVO> rows =
                monitorService.listAssetEventTimelineByAnalysisId(analysisId.trim(), ASSET_EVENT_TIMELINE_LIMIT);
        return rows != null ? rows : Collections.emptyList();
    }

    private List<EvidenceBriefVO> resolveEvidenceTopItems(DashboardDetailResponseVO body) {
        if (body == null || body.getDecision() == null || evidenceService == null) {
            return Collections.emptyList();
        }
        String analysisId = body.getDecision().getAnalysisId();
        if (analysisId == null || analysisId.isBlank()) {
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
        if (analysisId == null || analysisId.isBlank()) {
            return Collections.emptyList();
        }
        List<ScoreBriefVO> rows = scoreService.listTopScoreBriefByAnalysisId(analysisId);
        return rows != null ? rows : Collections.emptyList();
    }

    private List<ScoreEightItemVO> resolveScoreEightItems(DashboardDetailResponseVO body) {
        if (scoreService == null) {
            return Collections.emptyList();
        }
        String analysisId = null;
        if (body != null && body.getDecision() != null) {
            String raw = body.getDecision().getAnalysisId();
            if (raw != null && !raw.isBlank()) {
                analysisId = raw.trim();
            }
        }
        List<ScoreEightItemVO> rows = scoreService.listScoreEightItemsByAnalysisId(analysisId);
        return rows != null ? rows : Collections.emptyList();
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
