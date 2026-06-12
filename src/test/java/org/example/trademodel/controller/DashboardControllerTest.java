package org.example.trademodel.controller;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvStaleReasonCode;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEventSourceOwnershipResult;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.entity.HotResetEventDO;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.entity.MarketEnvironmentSnapshotDO;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.mapper.MarketEnvironmentSnapshotMapper;
import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.service.DecisionService;
import org.example.trademodel.service.EvidenceService;
import org.example.trademodel.service.MonitorService;
import org.example.trademodel.service.ReviewAggregateService;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.service.RuntimeMetricService;
import org.example.trademodel.service.ScoreService;
import org.example.trademodel.service.SourceTraceEventSourceOwnershipService;
import org.example.trademodel.service.SystemHealthService;
import org.example.trademodel.service.dashboard.DefaultDashboardRuntimeKlineContextAdapter;
import org.example.trademodel.service.dashboard.DefaultDashboardSourceTraceDetailAdapter;
import org.example.trademodel.service.dashboard.DashboardSourceTraceDetailAdapter;
import org.example.trademodel.service.dashboard.ExecutionPlanDisplayAdapter;
import org.example.trademodel.service.dashboard.PaperObservationDisplayAdapter;
import org.example.trademodel.service.dashboard.PlanBoundaryDisplayAdapter;
import org.example.trademodel.service.dashboard.RiskActionGuardDisplayAdapter;
import org.example.trademodel.service.impl.RuntimeKlineContextAssemblyServiceImpl;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.DashboardDetailResponseVO;
import org.example.trademodel.vo.EvidenceBriefVO;
import org.example.trademodel.vo.LightSystemStatusVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.vo.ReviewAggregateSummaryVO;
import org.example.trademodel.vo.ReviewAggregateVO;
import org.example.trademodel.vo.ReviewStateVO;
import org.example.trademodel.vo.ScoreBriefVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("smoke")
class DashboardControllerTest {
    private static final Path DASHBOARD_TEMPLATE =
            Path.of("src/main/resources/templates/dashboard.html");
    private static final String INTERNAL_PUSH_PREVIEW_START =
            "<section class=\"card module-status-card review-display-card\" id=\"internalPushPreviewDisplay\"";
    private static final String SECTION_END = "</section>";

    @Mock
    private DecisionService decisionService;
    @Mock
    private SystemHealthService systemHealthService;
    @Mock
    private MonitorService monitorService;
    @Mock
    private RuntimeMetricService runtimeMetricService;
    @Mock
    private RealMarketEnvironmentService realMarketEnvironmentService;
    @Mock
    private MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper;
    @Mock
    private AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    @Mock
    private HotResetEventMapper hotResetEventMapper;
    @Mock
    private SourceTraceEventSourceOwnershipService sourceTraceEventSourceOwnershipService;
    @Mock
    private EvidenceService evidenceService;
    @Mock
    private ScoreService scoreService;
    @Mock
    private ReviewService reviewService;
    @Mock
    private ReviewAggregateService reviewAggregateService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                new DefaultDashboardSourceTraceDetailAdapter(
                        new DefaultDashboardRuntimeKlineContextAdapter((symbol, timeframe, requiredWindowSize, maxReadLagMs) ->
                                readiness(
                                        PersistedOhlcvReadinessStatus.MISSING,
                                        PersistedOhlcvStaleReasonCode.NO_BARS_FOR_SYMBOL_TIMEFRAME,
                                        "No closed persisted OHLCV bars exist for symbol/timeframe.",
                                        List.of("persistedOhlcvWindow", "klineItems")
                                )
                        )
                )
        )).build();
    }

    @Test
    void dashboardTemplateRendersInternalPushPreviewDisplayGateAsReviewOnlyAndNonSendable() throws Exception {
        String section = normalizedInternalPushPreviewDisplay();

        assertThat(section).contains("internal push preview");
        assertThat(section).contains("review-only preview");
        assertThat(section).contains("not a trade instruction");
        assertThat(section).contains("manual review required");
        assertThat(section).contains("recheck required");
        assertThat(section).contains("risk action guard required");
        assertThat(section).contains("external channel disabled");
        assertThat(section).contains("no telegram / email / webhook connected");
        assertThat(section).contains("no readiness / point / entry / stop / tp / rr generated");
        assertThat(section).contains("blocked / fail-closed explanation");
        assertThat(section).contains("watchlist pool");
        assertThat(section).contains("display slots");
        assertThat(section).contains("not display slots");

        assertThat(section).doesNotContain("telegram enabled");
        assertThat(section).doesNotContain("email enabled");
        assertThat(section).doesNotContain("webhook enabled");
        assertThat(section).doesNotContain("external channel enabled");
        assertThat(section).doesNotContain("readiness generated");
        assertThat(section).doesNotContain("point generated");
        assertThat(section).doesNotContain("entry generated");
        assertThat(section).doesNotContain("stop generated");
        assertThat(section).doesNotContain("take profit generated");
        assertThat(section).doesNotContain("place order");
        assertThat(section).doesNotContain("execute order");
        assertThat(section).doesNotContain("start auto-trading");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyPositionSyncProviderStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/system/position-sync-status");
        assertThat(html).contains("providerStatusValue");
        assertThat(html).contains("providerActiveValue");
        assertThat(html).contains("providerConfiguredValue");
        assertThat(html).contains("providerFallbackValue");
        assertThat(html).contains("providerFallbackReasonValue");
        assertThat(html).contains("providerLastSyncSuccessValue");
        assertThat(html).contains("providerFreshnessValue");
        assertThat(html).contains("providerOpenCountValue");
        assertThat(html).contains("providerLastSyncTimeValue");
        assertThat(html).contains("REVIEW_ONLY_POSITION_SYNC_READY");
        assertThat(html).contains("SIMULATED_FALLBACK");
        assertThat(html).contains("INCOMPLETE");
        assertThat(html).contains("BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("只读状态，不是交易建议");
        assertThat(html).contains("模拟来源不等于真实 Binance 持仓");
        assertThat(html).contains("currentOpenPositionCount");
        assertThat(html).contains("lastSyncEndTime");
        assertThat(html).contains("lastSyncStartTime");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyWatchlistRuntimeStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/rule/push-watchlist");
        assertThat(html).contains("watchlistStatusPanel");
        assertThat(html).contains("watchlistRuntimeStatusValue");
        assertThat(html).contains("watchlistSymbolsValue");
        assertThat(html).contains("watchlistSourceValue");
        assertThat(html).contains("watchlistFailClosedValue");
        assertThat(html).contains("watchlistDisplaySlotsBoundaryValue");
        assertThat(html).contains("watchlistReviewOnlyValue");
        assertThat(html).contains("WATCHLIST_REVIEW_ONLY_READY");
        assertThat(html).contains("WATCHLIST_EMPTY_FAIL_CLOSED");
        assertThat(html).contains("WATCHLIST_CONFIG_MISSING");
        assertThat(html).contains("BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("Display Slots 只是首页展示位");
        assertThat(html).contains("Display Slots 不是候选池");
        assertThat(html).contains("默认六个币不是候选池");
        assertThat(html).contains("只读状态，不发送 Push");
        assertThat(html).contains("不在 Watchlist Pool 不进入候选/推送/扫描/点位");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyRuleConfigAuditRuntimeStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/rule/config-audit-status?ruleKey=push.watchlist.symbols");
        assertThat(html).contains("ruleConfigAuditStatusPanel");
        assertThat(html).contains("ruleConfigAuditRuntimeStatusValue");
        assertThat(html).contains("ruleConfigAuditKeyValue");
        assertThat(html).contains("ruleConfigAuditMetadataValue");
        assertThat(html).contains("ruleConfigAuditSourceValue");
        assertThat(html).contains("ruleConfigAuditEnabledOnlyValue");
        assertThat(html).contains("ruleConfigAuditWatchlistValue");
        assertThat(html).contains("ruleConfigAuditContextValue");
        assertThat(html).contains("ruleConfigAuditReviewOnlyValue");
        assertThat(html).contains("ruleConfigAuditSignalBoundaryValue");
        assertThat(html).contains("ruleConfigAuditReloadBoundaryValue");
        assertThat(html).contains("ruleConfigAuditReasonValue");
        assertThat(html).contains("RULECONFIG_AUDIT_REVIEW_ONLY_READY");
        assertThat(html).contains("RULECONFIG_WATCHLIST_KEY_READY_CONTEXT");
        assertThat(html).contains("RULECONFIG_VERSION_OR_DESCRIPTION_PARTIAL");
        assertThat(html).contains("RULECONFIG_CONFIG_MISSING_FAIL_CLOSED");
        assertThat(html).contains("RULECONFIG_VALUE_EMPTY_OR_PARSE_RISK_FAIL_CLOSED");
        assertThat(html).contains("RULECONFIG_AUDIT_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("RuleConfig 只读解释配置状态，不是交易信号");
        assertThat(html).contains("RuleVersionLog context-only");
        assertThat(html).contains("不是 current RuleConfig status owner");
        assertThat(html).contains("不发送 Push");
        assertThat(html).contains("不是 Candidate");
        assertThat(html).contains("不是新的 Decision generation");
        assertThat(html).contains("不是 Point");
        assertThat(html).contains("status path 不调用 /api/rule/reload");
        assertThat(html).contains("不触发 schema/service expansion");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyMarketQuoteFreshnessStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/market/quote-status");
        assertThat(html).contains("marketQuoteStatusPanel");
        assertThat(html).contains("marketQuoteRuntimeStatusValue");
        assertThat(html).contains("marketQuoteSampleSymbolValue");
        assertThat(html).contains("marketQuoteSourceValue");
        assertThat(html).contains("marketQuoteSourceTypeValue");
        assertThat(html).contains("marketQuoteFreshnessValue");
        assertThat(html).contains("marketQuoteFallbackValue");
        assertThat(html).contains("marketQuoteSourceHealthValue");
        assertThat(html).contains("marketQuoteLastUpdatedValue");
        assertThat(html).contains("MARKETQUOTE_REVIEW_ONLY_READY");
        assertThat(html).contains("MARKETQUOTE_MISSING_FAIL_CLOSED");
        assertThat(html).contains("只读行情状态，不是交易信号");
        assertThat(html).contains("dashboard-only sample");
        assertThat(html).contains("Watchlist Pool 才是候选边界");
        assertThat(html).contains("Display Slots 不是行情候选池");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyEvidenceScoreRuntimeStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/dashboard/evidence-score-status");
        assertThat(html).contains("evidenceScoreStatusPanel");
        assertThat(html).contains("evidenceScoreRuntimeStatusValue");
        assertThat(html).contains("evidenceScoreSymbolValue");
        assertThat(html).contains("evidenceCountValue");
        assertThat(html).contains("scoreCountValue");
        assertThat(html).contains("evidenceScoreTopSummaryValue");
        assertThat(html).contains("evidenceScoreSourceTraceValue");
        assertThat(html).contains("evidenceScoreSourceHealthValue");
        assertThat(html).contains("EVIDENCE_SCORE_REVIEW_ONLY_READY");
        assertThat(html).contains("EVIDENCE_MISSING_FAIL_CLOSED");
        assertThat(html).contains("SCORE_MISSING_FAIL_CLOSED");
        assertThat(html).contains("EVIDENCE_SCORE_INCOMPLETE_FAIL_CLOSED");
        assertThat(html).contains("EVIDENCE_SCORE_SOURCE_TRACE_PARTIAL");
        assertThat(html).contains("EVIDENCE_SCORE_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("Evidence / Score 是只读状态，不是交易信号");
        assertThat(html).contains("不是 Candidate");
        assertThat(html).contains("不是 Decision");
        assertThat(html).contains("不是 Point");
        assertThat(html).contains("Watchlist Pool 和 MarketQuote freshness / fallback 边界仍适用");
        assertThat(html).contains("Display Slots 不是候选池");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyDecisionResultRuntimeStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/dashboard/decision-result-status");
        assertThat(html).contains("decisionResultStatusPanel");
        assertThat(html).contains("decisionResultRuntimeStatusValue");
        assertThat(html).contains("decisionResultSymbolValue");
        assertThat(html).contains("decisionResultAnalysisIdValue");
        assertThat(html).contains("decisionAvailableValue");
        assertThat(html).contains("decisionConfidenceValue");
        assertThat(html).contains("decisionAiRoleValue");
        assertThat(html).contains("decisionSourceTraceValue");
        assertThat(html).contains("decisionSourceHealthValue");
        assertThat(html).contains("DECISIONRESULT_REVIEW_ONLY_READY");
        assertThat(html).contains("DECISIONRESULT_MISSING_FAIL_CLOSED");
        assertThat(html).contains("DECISIONRESULT_READ_MODEL_PARTIAL");
        assertThat(html).contains("DECISIONRESULT_SOURCE_TRACE_PARTIAL");
        assertThat(html).contains("DECISIONRESULT_AI_ROLE_PARTIAL");
        assertThat(html).contains("DECISIONRESULT_STALE_OR_UNKNOWN_FAIL_CLOSED");
        assertThat(html).contains("DECISIONRESULT_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("DecisionResult 是只读状态，不是交易信号");
        assertThat(html).contains("不是 Candidate");
        assertThat(html).contains("不是新的 Decision generation");
        assertThat(html).contains("不是 Point");
        assertThat(html).contains("Watchlist Pool、MarketQuote freshness / fallback、Evidence / Score 边界仍适用");
        assertThat(html).contains("Display Slots 不是候选池");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyExecutionPlanBoundaryRuntimeStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/dashboard/execution-plan-boundary-status");
        assertThat(html).contains("executionPlanBoundaryStatusPanel");
        assertThat(html).contains("executionPlanBoundaryRuntimeStatusValue");
        assertThat(html).contains("executionPlanBoundarySymbolValue");
        assertThat(html).contains("executionPlanBoundaryAnalysisIdValue");
        assertThat(html).contains("planBoundaryStatusValue");
        assertThat(html).contains("executionPlanStatusValue");
        assertThat(html).contains("executionPlanSourceTraceValue");
        assertThat(html).contains("executionPlanSourceHealthValue");
        assertThat(html).contains("executionPlanRiskGuardValue");
        assertThat(html).contains("executionPlanNotExecutableReasonValue");
        assertThat(html).contains("executionPlanBoundaryReviewOnlyValue");
        assertThat(html).contains("executionPlanBoundarySignalBoundaryValue");
        assertThat(html).contains("executionPlanBoundaryUpstreamValue");
        assertThat(html).contains("EXECUTIONPLAN_BOUNDARY_REVIEW_ONLY_READY");
        assertThat(html).contains("PLAN_BOUNDARY_BACKEND_PENDING_FAIL_CLOSED");
        assertThat(html).contains("PLAN_BOUNDARY_INCOMPLETE_FAIL_CLOSED");
        assertThat(html).contains("PLAN_BOUNDARY_WATCH_ONLY");
        assertThat(html).contains("EXECUTIONPLAN_BOUNDARY_PENDING_FAIL_CLOSED");
        assertThat(html).contains("EXECUTIONPLAN_SOURCE_TRACE_PARTIAL");
        assertThat(html).contains("EXECUTIONPLAN_RISK_GUARD_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("EXECUTIONPLAN_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("ExecutionPlan / BoundaryCandidate 是只读状态，不是交易信号，不可执行");
        assertThat(html).contains("不是 Candidate");
        assertThat(html).contains("不是新的 Decision generation");
        assertThat(html).contains("不是 Point");
        assertThat(html).contains("Watchlist Pool、MarketQuote freshness / fallback、Evidence / Score、DecisionResult 边界仍适用");
        assertThat(html).contains("Display Slots 不是候选池");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyRiskActionGuardRuntimeStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/dashboard/risk-action-guard-status");
        assertThat(html).contains("riskActionGuardStatusPanel");
        assertThat(html).contains("riskActionGuardRuntimeStatusValue");
        assertThat(html).contains("riskActionGuardSymbolValue");
        assertThat(html).contains("riskActionGuardAnalysisIdValue");
        assertThat(html).contains("riskActionGuardStatusValue");
        assertThat(html).contains("riskActionGuardLiquidityValue");
        assertThat(html).contains("riskActionGuardManualReviewValue");
        assertThat(html).contains("riskActionGuardActionFlagsValue");
        assertThat(html).contains("riskActionGuardAdviceValue");
        assertThat(html).contains("riskActionGuardSourceHealthValue");
        assertThat(html).contains("riskActionGuardFailClosedValue");
        assertThat(html).contains("riskActionGuardReviewOnlyValue");
        assertThat(html).contains("riskActionGuardSignalBoundaryValue");
        assertThat(html).contains("riskActionGuardActionBoundaryValue");
        assertThat(html).contains("riskActionGuardUpstreamValue");
        assertThat(html).contains("riskActionGuardReasonValue");
        assertThat(html).contains("riskActionGuardDetailStatusPanel");
        assertThat(html).contains("RISK_ACTION_GUARD_REVIEW_ONLY_READY");
        assertThat(html).contains("BACKEND_PENDING_FAIL_CLOSED");
        assertThat(html).contains("DECISION_MISSING_FAIL_CLOSED");
        assertThat(html).contains("PLAN_BOUNDARY_FAIL_CLOSED");
        assertThat(html).contains("EXECUTION_PLAN_NOT_READY_FAIL_CLOSED");
        assertThat(html).contains("LIQUIDITY_CONTEXT_MISSING_FAIL_CLOSED");
        assertThat(html).contains("LIQUIDITY_DETERIORATION_REVIEW_ONLY");
        assertThat(html).contains("STAMPEDE_REVIEW_ONLY_FAIL_CLOSED");
        assertThat(html).contains("WICK_ONLY_REVIEW_ONLY_FAIL_CLOSED");
        assertThat(html).contains("HIGH_RISK_REVIEW_ONLY");
        assertThat(html).contains("ACTION_FLAGS_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("ACTION_WORDING_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("RiskActionGuard 是只读状态，仅人工复核，不是交易信号");
        assertThat(html).contains("不是 Candidate");
        assertThat(html).contains("不是新的 Decision generation");
        assertThat(html).contains("不是 Point");
        assertThat(html).contains("reduce / close / reverse / move stop / open / execute 只能作为 guardrail / manual-review copy");
        assertThat(html).contains("Display Slots 不是候选池");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyPaperObservationRuntimeStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/dashboard/paper-observation-status");
        assertThat(html).contains("paperObservationStatusPanel");
        assertThat(html).contains("paperObservationRuntimeStatusValue");
        assertThat(html).contains("paperObservationSymbolValue");
        assertThat(html).contains("paperObservationAnalysisIdValue");
        assertThat(html).contains("paperObservationStatusValue");
        assertThat(html).contains("paperObservationBackendValue");
        assertThat(html).contains("paperObservationCountsValue");
        assertThat(html).contains("paperObservationManualReviewValue");
        assertThat(html).contains("paperObservationFailClosedValue");
        assertThat(html).contains("paperObservationReviewSummaryValue");
        assertThat(html).contains("paperObservationReviewOnlyValue");
        assertThat(html).contains("paperObservationExecutionBoundaryValue");
        assertThat(html).contains("paperObservationSignalBoundaryValue");
        assertThat(html).contains("paperObservationReasonValue");
        assertThat(html).contains("PAPER_OBSERVATION_REVIEW_ONLY_READY");
        assertThat(html).contains("PAPER_OBSERVATION_BACKEND_PENDING_FAIL_CLOSED");
        assertThat(html).contains("PAPER_OBSERVATION_MISSING_FAIL_CLOSED");
        assertThat(html).contains("PAPER_OBSERVATION_PARTIAL_REVIEW_ONLY");
        assertThat(html).contains("PAPER_ORDER_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("SIMULATED_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("PAPER_PNL_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("POSITION_MONITOR_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("POINT_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("Paper Observation / Paper Trading 是 review-only，只读展示，仅人工复核");
        assertThat(html).contains("not real position");
        assertThat(html).contains("not trade instruction");
        assertThat(html).contains("not paper order");
        assertThat(html).contains("not simulated execution");
        assertThat(html).contains("not paper PnL generation");
        assertThat(html).contains("not Position Monitor execution");
        assertThat(html).contains("not entry / stop / TP / RR");
        assertThat(html).contains("Display Slots 不是候选池");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyAlertFatiguePolicyRuntimeStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/dashboard/alert-fatigue-policy-status");
        assertThat(html).contains("alertFatiguePolicyStatusPanel");
        assertThat(html).contains("alertFatiguePolicyRuntimeStatusValue");
        assertThat(html).contains("alertFatiguePolicySymbolValue");
        assertThat(html).contains("alertFatiguePolicySourceHealthValue");
        assertThat(html).contains("alertFatiguePolicyCountsValue");
        assertThat(html).contains("alertFatiguePolicyCooldownValue");
        assertThat(html).contains("alertFatiguePolicySuppressionValue");
        assertThat(html).contains("alertFatiguePolicyDuplicateValue");
        assertThat(html).contains("alertFatiguePolicyFatigueValue");
        assertThat(html).contains("alertFatiguePolicySourceValue");
        assertThat(html).contains("alertFatiguePolicyLatestValue");
        assertThat(html).contains("alertFatiguePolicyReviewOnlyValue");
        assertThat(html).contains("alertFatiguePolicyPushBoundaryValue");
        assertThat(html).contains("alertFatiguePolicySignalBoundaryValue");
        assertThat(html).contains("ALERT_POLICY_REVIEW_ONLY_READY");
        assertThat(html).contains("ALERT_POLICY_BACKEND_PENDING_FAIL_CLOSED");
        assertThat(html).contains("ALERT_READ_MODEL_MISSING_FAIL_CLOSED");
        assertThat(html).contains("ALERT_RECENT_EMPTY_REVIEW_ONLY");
        assertThat(html).contains("ALERT_SUPPRESSION_ACTIVE_REVIEW_ONLY");
        assertThat(html).contains("ALERT_COOLDOWN_ACTIVE_REVIEW_ONLY");
        assertThat(html).contains("ALERT_DUPLICATE_RISK_REVIEW_ONLY");
        assertThat(html).contains("ALERT_FATIGUE_HIGH_REVIEW_ONLY");
        assertThat(html).contains("NOTIFICATION_POLICY_MISSING_FAIL_CLOSED");
        assertThat(html).contains("PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("review-only，只读展示，不发送 Push");
        assertThat(html).contains("not push send");
        assertThat(html).contains("not external channel");
        assertThat(html).contains("not recheck execution");
        assertThat(html).contains("not scheduler trigger");
        assertThat(html).contains("not collector trigger");
        assertThat(html).contains("not API client refresh");
        assertThat(html).contains("not alert write");
        assertThat(html).contains("not trading");
        assertThat(html).contains("not candidate");
        assertThat(html).contains("not decision generation");
        assertThat(html).contains("not point");
        assertThat(html).contains("not executable");
        assertThat(html).contains("Display Slots 不是候选池");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyReviewReplayRuntimeStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/dashboard/review-replay-result-status");
        assertThat(html).contains("reviewReplayStatusPanel");
        assertThat(html).contains("reviewReplayRuntimeStatusValue");
        assertThat(html).contains("reviewReplayAnalysisIdValue");
        assertThat(html).contains("reviewReplayResultValue");
        assertThat(html).contains("reviewReplayAggregateValue");
        assertThat(html).contains("reviewReplaySummaryValue");
        assertThat(html).contains("reviewReplaySourceTraceValue");
        assertThat(html).contains("reviewReplaySourceHealthValue");
        assertThat(html).contains("reviewReplaySafetyBoundaryValue");
        assertThat(html).contains("REVIEW_REPLAY_REVIEW_ONLY_READY");
        assertThat(html).contains("REVIEW_RESULT_MISSING_FAIL_CLOSED");
        assertThat(html).contains("REVIEW_AGGREGATE_MISSING_FAIL_CLOSED");
        assertThat(html).contains("REPLAY_SUMMARY_MISSING_FAIL_CLOSED");
        assertThat(html).contains("REVIEW_REPLAY_SOURCE_TRACE_PARTIAL");
        assertThat(html).contains("REPLAY_EXECUTION_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("REVIEW_REPLAY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("Review / Replay 是只读状态，不是交易信号");
        assertThat(html).contains("不触发 replay execution");
        assertThat(html).contains("不生成复盘结果");
        assertThat(html).contains("不是 Candidate");
        assertThat(html).contains("不是新的 Decision generation");
        assertThat(html).contains("不是 Point");
        assertThat(html).contains("Watchlist Pool、MarketQuote freshness / fallback、Evidence / Score、DecisionResult、ExecutionPlan / BoundaryCandidate 边界仍适用");
        assertThat(html).contains("Display Slots 不是候选池");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyDataSourceHealthRuntimeStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/dashboard/data-source-health-status");
        assertThat(html).contains("dataSourceHealthStatusPanel");
        assertThat(html).contains("dataSourceHealthRuntimeStatusValue");
        assertThat(html).contains("dataSourceHealthSymbolValue");
        assertThat(html).contains("dataSourceHealthSourceHealthValue");
        assertThat(html).contains("dataSourceHealthScopedSourcesValue");
        assertThat(html).contains("dataSourceHealthOkSourcesValue");
        assertThat(html).contains("dataSourceHealthPartialSourcesValue");
        assertThat(html).contains("dataSourceHealthMissingStaleSourcesValue");
        assertThat(html).contains("dataSourceHealthWatchBlockedSourcesValue");
        assertThat(html).contains("dataSourceHealthReviewOnlyValue");
        assertThat(html).contains("dataSourceHealthSignalBoundaryValue");
        assertThat(html).contains("dataSourceHealthRefreshBoundaryValue");
        assertThat(html).contains("DATA_SOURCE_HEALTH_REVIEW_ONLY_READY");
        assertThat(html).contains("DATA_SOURCE_HEALTH_PARTIAL_REVIEW_ONLY");
        assertThat(html).contains("DATA_SOURCE_HEALTH_STALE_FAIL_CLOSED");
        assertThat(html).contains("DATA_SOURCE_HEALTH_MISSING_FAIL_CLOSED");
        assertThat(html).contains("DATA_SOURCE_HEALTH_WATCH_ONLY_REVIEW");
        assertThat(html).contains("DATA_SOURCE_HEALTH_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("Data Source Health 是只读状态，不是交易信号");
        assertThat(html).contains("不是 Candidate");
        assertThat(html).contains("不是新的 Decision generation");
        assertThat(html).contains("不是 Point");
        assertThat(html).contains("不触发 external API refresh / scheduler / collector / API client");
        assertThat(html).contains("Watchlist Pool、MarketQuote、Evidence / Score、DecisionResult、ExecutionPlan / BoundaryCandidate、Review / Replay 边界仍适用");
        assertThat(html).contains("Display Slots 不是候选池");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyMissedArchiveRuntimeStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/missed-opportunity/review-archive-status");
        assertThat(html).contains("missedArchiveStatusPanel");
        assertThat(html).contains("missedArchiveRuntimeStatusValue");
        assertThat(html).contains("missedArchiveScopeValue");
        assertThat(html).contains("missedArchiveCountValue");
        assertThat(html).contains("missedArchiveLatestValue");
        assertThat(html).contains("missedArchiveReasonParseValue");
        assertThat(html).contains("missedArchiveSourceHealthValue");
        assertThat(html).contains("missedArchiveReviewOnlyValue");
        assertThat(html).contains("missedArchiveManualReviewValue");
        assertThat(html).contains("missedArchiveSignalBoundaryValue");
        assertThat(html).contains("missedArchiveGenerationBoundaryValue");
        assertThat(html).contains("missedArchiveWritePushBoundaryValue");
        assertThat(html).contains("missedArchiveExecutionBoundaryValue");
        assertThat(html).contains("missedArchiveUpstreamValue");
        assertThat(html).contains("missedArchiveReasonValue");
        assertThat(html).contains("REVIEW_ARCHIVE_AGGREGATE_REVIEW_ONLY_READY");
        assertThat(html).contains("REVIEW_ARCHIVE_AGGREGATE_BACKEND_PENDING_FAIL_CLOSED");
        assertThat(html).contains("REVIEW_ARCHIVE_AGGREGATE_MISSING_FAIL_CLOSED");
        assertThat(html).contains("REVIEW_ARCHIVE_AGGREGATE_PARTIAL_REVIEW_ONLY");
        assertThat(html).contains("MISSED_OPPORTUNITY_COUNT_REVIEW_ONLY");
        assertThat(html).contains("REVIEW_ARCHIVE_COUNT_REVIEW_ONLY");
        assertThat(html).contains("MISSED_OPPORTUNITY_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("MISSED_OPPORTUNITY_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("REVIEW_RESULT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("POINT_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("Review Archive Aggregate 是 review-only / manual review only 状态，不是交易信号");
        assertThat(html).contains("manualReviewOnly=true");
        assertThat(html).contains("不是 Candidate");
        assertThat(html).contains("不是新的 Decision generation");
        assertThat(html).contains("不是 Point");
        assertThat(html).contains("不是 final direction");
        assertThat(html).contains("不是 entry / stop / TP / RR");
        assertThat(html).contains("不触发 missed-opportunity generation");
        assertThat(html).contains("不写入 missed opportunity");
        assertThat(html).contains("不生成复盘结果");
        assertThat(html).contains("不触发 replay / recheck execution");
        assertThat(html).contains("not Push send");
        assertThat(html).contains("not external channel");
        assertThat(html).contains("not order / execution / auto-trading");
        assertThat(html).contains("Display Slots 不是候选池");
    }

    @Test
    void dashboardTemplateShowsReviewOnlySourceRuntimeDataQualityStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/dashboard/source-runtime-data-quality-status");
        assertThat(html).contains("sourceRuntimeDataQualityStatusPanel");
        assertThat(html).contains("sourceRuntimeStatusValue");
        assertThat(html).contains("sourceTraceReadinessValue");
        assertThat(html).contains("runtimeKlineReadinessValue");
        assertThat(html).contains("persistedOhlcvReadinessValue");
        assertThat(html).contains("dataQualityStatusValue");
        assertThat(html).contains("multiTimeframeStatusValue");
        assertThat(html).contains("sourceRuntimeRefreshBoundaryValue");
        assertThat(html).contains("sourceRuntimeSignalBoundaryValue");
        assertThat(html).contains("sourceRuntimeReasonValue");
        assertThat(html).contains("SOURCE_RUNTIME_STATUS_REVIEW_ONLY_READY");
        assertThat(html).contains("SOURCE_TRACE_MISSING_FAIL_CLOSED");
        assertThat(html).contains("RUNTIME_KLINE_CONTEXT_MISSING_FAIL_CLOSED");
        assertThat(html).contains("PERSISTED_OHLCV_STALE_REVIEW_ONLY");
        assertThat(html).contains("DATA_QUALITY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("MULTITIMEFRAME_CONFLICT_REVIEW_ONLY");
        assertThat(html).contains("REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("not scheduler trigger");
        assertThat(html).contains("not collector trigger");
        assertThat(html).contains("not API client refresh");
        assertThat(html).contains("not external refresh");
        assertThat(html).contains("not source-binding generation");
        assertThat(html).contains("not final direction");
        assertThat(html).contains("not entry / stop / TP / RR");
        assertThat(html).contains("Display Slots 不是候选池");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyRuntimeReadinessGuardrailStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/system/runtime-readiness-guardrail-status");
        assertThat(html).contains("/api/system/run-baseline");
        assertThat(html).contains("/api/system/health is static liveness only");
        assertThat(html).contains("runtimeReadinessGuardrailStatusPanel");
        assertThat(html).contains("runtimeReadinessStatusValue");
        assertThat(html).contains("systemGuardrailStatusValue");
        assertThat(html).contains("runBaselineStatusValue");
        assertThat(html).contains("runtimeMetricStatusValue");
        assertThat(html).contains("runtimeReadinessSourceHealthValue");
        assertThat(html).contains("runtimeReadinessFailClosedValue");
        assertThat(html).contains("runtimeReadinessReviewOnlyValue");
        assertThat(html).contains("runtimeReadinessBoundaryValue");
        assertThat(html).contains("runtimeReadinessSignalBoundaryValue");
        assertThat(html).contains("runtimeReadinessReasonValue");
        assertThat(html).contains("RUNTIME_READINESS_REVIEW_ONLY_READY");
        assertThat(html).contains("RUNTIME_READINESS_BACKEND_PENDING_FAIL_CLOSED");
        assertThat(html).contains("RUNTIME_READINESS_MISSING_FAIL_CLOSED");
        assertThat(html).contains("RUNTIME_READINESS_PARTIAL_REVIEW_ONLY");
        assertThat(html).contains("SYSTEM_GUARDRAIL_REVIEW_ONLY_READY");
        assertThat(html).contains("SYSTEM_GUARDRAIL_DEGRADED_REVIEW_ONLY");
        assertThat(html).contains("SYSTEM_GUARDRAIL_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("RUN_BASELINE_REVIEW_ONLY_READY");
        assertThat(html).contains("RUN_BASELINE_MISSING_FAIL_CLOSED");
        assertThat(html).contains("RUNTIME_METRIC_REVIEW_ONLY_READY");
        assertThat(html).contains("RUNTIME_METRIC_MISSING_FAIL_CLOSED");
        assertThat(html).contains("EXECUTABLE_READINESS_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("RECOVERY_REPAIR_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("API_CLIENT_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("EXTERNAL_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("POINT_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("review-only");
        assertThat(html).contains("manual review only");
        assertThat(html).contains("fail-closed");
        assertThat(html).contains("readiness 只是 operational guardrail status，不是 executable readiness");
        assertThat(html).contains("not executable readiness");
        assertThat(html).contains("not trading authorization");
        assertThat(html).contains("not recovery / repair / restart / auto-fix");
        assertThat(html).contains("not scheduler trigger");
        assertThat(html).contains("not collector trigger");
        assertThat(html).contains("not API client refresh");
        assertThat(html).contains("not external refresh");
        assertThat(html).contains("not candidate");
        assertThat(html).contains("not decision generation");
        assertThat(html).contains("not point");
        assertThat(html).contains("not final direction");
        assertThat(html).contains("not entry / stop / TP / RR");
        assertThat(html).contains("not trading");
        assertThat(html).contains("not executable");
        assertThat(html).contains("Display Slots 不是候选池");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyAccountRiskExposureStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/dashboard/account-risk-exposure-status");
        assertThat(html).contains("accountRiskExposureStatusPanel");
        assertThat(html).contains("accountRiskRuntimeStatusValue");
        assertThat(html).contains("accountRiskAllowedEvidenceValue");
        assertThat(html).contains("accountRiskExposureValue");
        assertThat(html).contains("accountRiskSnapshotSourceValue");
        assertThat(html).contains("accountRiskReviewOnlyValue");
        assertThat(html).contains("accountRiskWriteBoundaryValue");
        assertThat(html).contains("accountRiskPushRecheckBoundaryValue");
        assertThat(html).contains("accountRiskSignalBoundaryValue");
        assertThat(html).contains("accountRiskReasonValue");
        assertThat(html).contains("AccountRiskSnapshotMapper.selectLatestByAnalysisId");
        assertThat(html).contains("AccountRiskSnapshotMapper.selectById is historical snapshot read only");
        assertThat(html).contains("tm_account_risk_snapshot");
        assertThat(html).contains("ACCOUNT_RISK_STATUS_REVIEW_ONLY_READY");
        assertThat(html).contains("ACCOUNT_RISK_STATUS_BACKEND_PENDING_FAIL_CLOSED");
        assertThat(html).contains("ACCOUNT_RISK_STATUS_MISSING_FAIL_CLOSED");
        assertThat(html).contains("ACCOUNT_RISK_STATUS_PARTIAL_REVIEW_ONLY");
        assertThat(html).contains("ACCOUNT_EXPOSURE_REVIEW_ONLY_READY");
        assertThat(html).contains("ACCOUNT_EXPOSURE_MISSING_FAIL_CLOSED");
        assertThat(html).contains("RISK_ALLOWED_READ_ONLY_EVIDENCE");
        assertThat(html).contains("ACCOUNT_RISK_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("POSITION_SIZING_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("REDUCE_CLOSE_STOP_REVERSE_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("POINT_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("review-only");
        assertThat(html).contains("manual review only");
        assertThat(html).contains("fail-closed");
        assertThat(html).contains("not account-risk write");
        assertThat(html).contains("not PushSnapshot write");
        assertThat(html).contains("not Push send");
        assertThat(html).contains("not Recheck execution");
        assertThat(html).contains("not trading authorization");
        assertThat(html).contains("not position sizing");
        assertThat(html).contains("not reduce / close / stop / reverse guidance");
        assertThat(html).contains("not candidate");
        assertThat(html).contains("not decision generation");
        assertThat(html).contains("not point");
        assertThat(html).contains("not final direction");
        assertThat(html).contains("not entry / stop / TP / RR");
        assertThat(html).contains("not trading");
        assertThat(html).contains("not executable");
        assertThat(html).contains("Display Slots 不是候选池");
    }

    @Test
    void dashboardTemplateShowsReviewOnlyHotResetEventImpactSourceStatusMapping() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);

        assertThat(html).contains("/api/dashboard/hot-reset-event-impact-source-status");
        assertThat(html).contains("hotResetEventImpactSourceStatusPanel");
        assertThat(html).contains("hotResetEventSourceStatusValue");
        assertThat(html).contains("eventImpactSourceStatusValue");
        assertThat(html).contains("sourceTraceEventSourceOwnershipValue");
        assertThat(html).contains("hotResetEventCountsValue");
        assertThat(html).contains("hotResetEventLatestValue");
        assertThat(html).contains("hotResetEventBoundaryValue");
        assertThat(html).contains("hotResetExternalBoundaryValue");
        assertThat(html).contains("hotResetSignalBoundaryValue");
        assertThat(html).contains("hotResetEventReasonValue");
        assertThat(html).contains("HotResetEventMapper.selectLatestByAnalysisId/countByAnalysisId");
        assertThat(html).contains("SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED");
        assertThat(html).contains("HOT_RESET_EVENT_SOURCE_REVIEW_ONLY_READY");
        assertThat(html).contains("HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED");
        assertThat(html).contains("HOT_RESET_EVENT_SOURCE_PARTIAL_REVIEW_ONLY");
        assertThat(html).contains("EVENT_IMPACT_SOURCE_REVIEW_ONLY_READY");
        assertThat(html).contains("EVENT_IMPACT_SOURCE_MISSING_FAIL_CLOSED");
        assertThat(html).contains("HOT_RESET_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("HOT_RESET_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("EVENT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("EXTERNAL_API_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("NEWS_FETCH_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("RECHECK_REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("POINT_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED");
        assertThat(html).contains("review-only");
        assertThat(html).contains("manual review only");
        assertThat(html).contains("fail-closed");
        assertThat(html).contains("not Hot Reset execution");
        assertThat(html).contains("not Hot Reset write");
        assertThat(html).contains("not event generation");
        assertThat(html).contains("not external API refresh");
        assertThat(html).contains("not news fetch");
        assertThat(html).contains("not scheduler trigger");
        assertThat(html).contains("not collector trigger");
        assertThat(html).contains("not Push send");
        assertThat(html).contains("not external channel");
        assertThat(html).contains("not Recheck execution");
        assertThat(html).contains("not Replay execution");
        assertThat(html).contains("not candidate");
        assertThat(html).contains("not decision generation");
        assertThat(html).contains("not point");
        assertThat(html).contains("not final direction");
        assertThat(html).contains("not entry / stop / TP / RR");
        assertThat(html).contains("not order / execution / auto-trading");
        assertThat(html).contains("not trading");
        assertThat(html).contains("not executable");
        assertThat(html).contains("Display Slots 不是候选池");
    }

    private DashboardController controllerWith(DashboardSourceTraceDetailAdapter sourceTraceDetailAdapter) {
        PlanBoundaryDisplayAdapter planBoundaryDisplayAdapter = (symbol, decision, fallbackDisplay) -> fallbackDisplay;
        ExecutionPlanDisplayAdapter executionPlanDisplayAdapter = (decision, planBoundaryDisplay, fallbackDisplay) -> fallbackDisplay;
        RiskActionGuardDisplayAdapter riskActionGuardDisplayAdapter = (decision, planBoundaryDisplay, executionPlanDisplay, fallbackDisplay) -> fallbackDisplay;
        PaperObservationDisplayAdapter paperObservationDisplayAdapter = (decision, planBoundaryDisplay, executionPlanDisplay, riskActionGuardDisplay, fallbackDisplay) -> fallbackDisplay;
        return controllerWith(
                sourceTraceDetailAdapter,
                planBoundaryDisplayAdapter,
                executionPlanDisplayAdapter,
                riskActionGuardDisplayAdapter,
                paperObservationDisplayAdapter
        );
    }

    private DashboardController controllerWith(
            DashboardSourceTraceDetailAdapter sourceTraceDetailAdapter,
            PlanBoundaryDisplayAdapter planBoundaryDisplayAdapter,
            ExecutionPlanDisplayAdapter executionPlanDisplayAdapter,
            RiskActionGuardDisplayAdapter riskActionGuardDisplayAdapter,
            PaperObservationDisplayAdapter paperObservationDisplayAdapter
    ) {
        return new DashboardController(
                decisionService,
                systemHealthService,
                monitorService,
                runtimeMetricService,
                realMarketEnvironmentService,
                marketEnvironmentSnapshotMapper,
                accountRiskSnapshotMapper,
                evidenceService,
                scoreService,
                reviewService,
                reviewAggregateService,
                sourceTraceDetailAdapter,
                planBoundaryDisplayAdapter,
                executionPlanDisplayAdapter,
                riskActionGuardDisplayAdapter,
                paperObservationDisplayAdapter,
                hotResetEventMapper,
                sourceTraceEventSourceOwnershipService
        );
    }

    @Test
    void summary_json_exposesPendingCountOnSystemStatus() throws Exception {
        LightSystemStatusVO system = new LightSystemStatusVO();
        system.setPendingCount(7);
        when(decisionService.getLightSystemStatus()).thenReturn(system);
        when(decisionService.countOpenPositions()).thenReturn(0);
        when(systemHealthService.getSystemHealth()).thenReturn(Collections.emptyMap());
        when(monitorService.getRecentAlerts(3)).thenReturn(Collections.emptyList());
        when(decisionService.getLatestDecisionResults(12)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemStatus.pendingCount").value(7));
    }

    @Test
    void summary_json_exposesConfusedCountOnSystemStatus() throws Exception {
        LightSystemStatusVO system = new LightSystemStatusVO();
        system.setConfusedCount(4);
        when(decisionService.getLightSystemStatus()).thenReturn(system);
        when(decisionService.countOpenPositions()).thenReturn(0);
        when(systemHealthService.getSystemHealth()).thenReturn(Collections.emptyMap());
        when(monitorService.getRecentAlerts(3)).thenReturn(Collections.emptyList());
        when(decisionService.getLatestDecisionResults(12)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemStatus.confusedCount").value(4));
    }

    @Test
    void summary_json_exposesReverseSignalCountOnSystemStatus() throws Exception {
        LightSystemStatusVO system = new LightSystemStatusVO();
        system.setReverseSignalCount(3);
        when(decisionService.getLightSystemStatus()).thenReturn(system);
        when(decisionService.countOpenPositions()).thenReturn(0);
        when(systemHealthService.getSystemHealth()).thenReturn(Collections.emptyMap());
        when(monitorService.getRecentAlerts(3)).thenReturn(Collections.emptyList());
        when(decisionService.getLatestDecisionResults(12)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemStatus.reverseSignalCount").value(3));
    }

    @Test
    void summary_json_exposesCoreFieldsOnFirstDecision() throws Exception {
        stubSummaryData();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        when(decisionService.getLatestDecisionResults(12)).thenReturn(List.of(decision));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisions[0].marketBiasHierarchy").value("H1>H4>D1"))
                .andExpect(jsonPath("$.decisions[0].isWorthOpening").value(true))
                .andExpect(jsonPath("$.decisions[0].recommendedAction").value("OPEN_LONG"))
                .andExpect(jsonPath("$.decisions[0].aiConflictLevel").value("L2"))
                .andExpect(jsonPath("$.decisions[0].aiConflictScore").value(42))
                .andExpect(jsonPath("$.decisions[0].aiPlanMode").value("AGGRESSIVE"))
                .andExpect(jsonPath("$.decisions[0].confusedScore").value(3));
    }

    @Test
    void detail_json_exposesCoreFieldsOnDecision() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-btc");
        decision.setAnalysisId("ana-btc");
        decision.setCreateTime(LocalDateTime.of(2026, 5, 17, 12, 0));
        decision.setTimeframe("1h");
        decision.setMultiTfConvergence("STRONG");
        decision.setDataQualityScore(91);
        decision.setLatestPrice(BigDecimal.valueOf(68100));
        decision.setPriceUpdateTimeMs(1710000000000L);
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-btc")).thenReturn(Collections.emptyList());
        ScoreBriefVO score = new ScoreBriefVO();
        score.setScoreType("综合评分");
        score.setScoreValue(81.5);
        when(scoreService.listTopScoreBriefByAnalysisId("ana-btc")).thenReturn(List.of(score));

        mockMvc.perform(get("/api/dashboard/detail").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision.marketBiasHierarchy").value("H1>H4>D1"))
                .andExpect(jsonPath("$.decision.isWorthOpening").value(true))
                .andExpect(jsonPath("$.decision.recommendedAction").value("OPEN_LONG"))
                .andExpect(jsonPath("$.decision.aiConflictLevel").value("L2"))
                .andExpect(jsonPath("$.decision.aiConflictScore").value(42))
                .andExpect(jsonPath("$.decision.aiPlanMode").value("AGGRESSIVE"))
                .andExpect(jsonPath("$.decision.confusedScore").value(3))
                .andExpect(jsonPath("$.evidenceTopItems").isArray())
                .andExpect(jsonPath("$.scoreTopItems").isArray())
                .andExpect(jsonPath("$.scoreTopItems[0].scoreType").value("综合评分"))
                .andExpect(jsonPath("$.scoreTopItems[0].scoreValue").value(81.5))
                .andExpect(jsonPath("$.sourceTrace.fallbackStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.sourceTrace.decisionId").value("dec-btc"))
                .andExpect(jsonPath("$.sourceTrace.decisionIdSource").value("DecisionResultVO.decisionId"))
                .andExpect(jsonPath("$.sourceTrace.analysisId").value("ana-btc"))
                .andExpect(jsonPath("$.sourceTrace.analysisIdSource").value("DecisionResultVO.analysisId"))
                .andExpect(jsonPath("$.sourceTrace.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.sourceTrace.symbolSource").value("DecisionResultVO.symbol"))
                .andExpect(jsonPath("$.sourceTrace.decisionCreateTime").exists())
                .andExpect(jsonPath("$.sourceTrace.decisionCreateTimeSource").value("DecisionResultVO.createTime"))
                .andExpect(jsonPath("$.sourceTrace.timeframe").value("1h"))
                .andExpect(jsonPath("$.sourceTrace.timeframeSource").value("DecisionResultVO.timeframe"))
                .andExpect(jsonPath("$.sourceTrace.runtimeKlineContextStatus").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.sourceTrace.runtimeKlineContextSource").value("dashboardDetail.noRuntimeKlineContext"))
                .andExpect(jsonPath("$.sourceTrace.runtimeKlineReadinessStatus").value("MISSING"))
                .andExpect(jsonPath("$.sourceTrace.runtimeKlineStaleReasonCode").value("NO_BARS_FOR_SYMBOL_TIMEFRAME"))
                .andExpect(jsonPath("$.sourceTrace.runtimeKlineStaleReasonText").value("No closed persisted OHLCV bars exist for symbol/timeframe."))
                .andExpect(jsonPath("$.sourceTrace.runtimeKlineReadinessMissingFields[?(@ == 'persistedOhlcvWindow')]").exists())
                .andExpect(jsonPath("$.sourceTrace.runtimeKlineReadinessMissingFields[?(@ == 'klineItems')]").exists())
                .andExpect(jsonPath("$.runtimeKlineContext.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.runtimeKlineContext.timeframe").value("1h"))
                .andExpect(jsonPath("$.runtimeKlineContext.fallbackStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.runtimeKlineContext.latestPrice").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.klineItems").isEmpty())
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvReadinessStatus").value("MISSING"))
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvStaleReasonCode").value("NO_BARS_FOR_SYMBOL_TIMEFRAME"))
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvStaleReasonText").value("No closed persisted OHLCV bars exist for symbol/timeframe."))
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvMissingFields[?(@ == 'persistedOhlcvWindow')]").exists())
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvMissingFields[?(@ == 'klineItems')]").exists())
                .andExpect(jsonPath("$.runtimeKlineContext.entryPriceSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.stopPriceSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.tpPriceSources").isEmpty())
                .andExpect(jsonPath("$.runtimeKlineContext.rrSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.liquiditySource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.eventSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.wickSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.runtimeKlineContext.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.sourceTrace.quoteLatestPrice").value(68100))
                .andExpect(jsonPath("$.sourceTrace.quoteLatestPriceSource").value("DecisionResultVO.latestPrice"))
                .andExpect(jsonPath("$.sourceTrace.quotePriceUpdateTimeMs").value(1710000000000L))
                .andExpect(jsonPath("$.sourceTrace.quotePriceUpdateTimeSource").value("DecisionResultVO.priceUpdateTimeMs"))
                .andExpect(jsonPath("$.sourceTrace.quoteFreshnessStatus").value("QUOTE_UPDATE_TIME_ONLY"))
                .andExpect(jsonPath("$.sourceTrace.dataQualityScore").value(91))
                .andExpect(jsonPath("$.sourceTrace.dataQualityScoreSource").value("DecisionResultVO.dataQualityScore"))
                .andExpect(jsonPath("$.sourceTrace.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.sourceTrace.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.sourceTrace.missingFields").isArray())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'runtimeKlineContext')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'timeframe')]").doesNotExist())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'latestPrice')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'entryPriceSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'stopPriceSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'tpPriceSources')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'rrSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'liquiditySource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.multiTimeframeSource").value("DecisionResultVO.multiTfConvergence"))
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'multiTimeframeSource')]").doesNotExist())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'eventSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'wickSource')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.fallbackStatus").value("SAFE_FAIL_CLOSED_ONLY"))
                .andExpect(jsonPath("$.derivativesRiskContext.timeframe").value("1h"))
                .andExpect(jsonPath("$.derivativesRiskContext.timeframeSource").value("DecisionResultVO.timeframe"))
                .andExpect(jsonPath("$.derivativesRiskContext.dataQualityScore").value(91))
                .andExpect(jsonPath("$.derivativesRiskContext.dataQualityScoreSource").value("DecisionResultVO.dataQualityScore"))
                .andExpect(jsonPath("$.derivativesRiskContext.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.derivativesRiskContext.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'openInterestHistory')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'fundingHistory')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'liquidationCluster')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'leverageDistribution')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'longShortRatio')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'liquidityStress')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'eventWindowBlockers')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'wickConfirmationSources')]").exists())
                .andExpect(jsonPath("$.derivativesRiskContext.missingFields[?(@ == 'dataQualityScore')]").doesNotExist());
    }

    @Test
    void evidenceScoreStatusEndpointReturnsReviewOnlyReadyStatus() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-evidence-score-ready");
        EvidenceBriefVO evidence = new EvidenceBriefVO();
        evidence.setEvidenceType("价格结构");
        evidence.setDescription("突破后回踩确认");
        evidence.setDirection("BULLISH");
        evidence.setSource("SYSTEM_GENERATED");
        ScoreBriefVO score = new ScoreBriefVO();
        score.setScoreType("综合评分");
        score.setScoreValue(82.0);
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-evidence-score-ready")).thenReturn(List.of(evidence));
        when(scoreService.listTopScoreBriefByAnalysisId("ana-evidence-score-ready")).thenReturn(List.of(score));

        mockMvc.perform(get("/api/dashboard/evidence-score-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EVIDENCE_SCORE_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.evidenceCount").value(1))
                .andExpect(jsonPath("$.scoreCount").value(1))
                .andExpect(jsonPath("$.evidenceAvailable").value(true))
                .andExpect(jsonPath("$.scoreAvailable").value(true))
                .andExpect(jsonPath("$.evidenceTopItems[0].evidenceType").value("价格结构"))
                .andExpect(jsonPath("$.scoreTopItems[0].scoreType").value("综合评分"))
                .andExpect(jsonPath("$.sourceTraceComplete").value(true))
                .andExpect(jsonPath("$.sourceHealth").value("OK"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionSignal").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.watchlistBounded").value(true))
                .andExpect(jsonPath("$.marketQuoteChecked").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(false));
    }

    @Test
    void evidenceScoreStatusEndpointFailsClosedWhenAnalysisContextMissing() throws Exception {
        when(decisionService.getLatestDecisionResultBySymbol("ETHUSDT")).thenReturn(null);

        mockMvc.perform(get("/api/dashboard/evidence-score-status").param("symbol", "ETHUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EVIDENCE_SCORE_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.symbol").value("ETHUSDT"))
                .andExpect(jsonPath("$.evidenceCount").value(0))
                .andExpect(jsonPath("$.scoreCount").value(0))
                .andExpect(jsonPath("$.evidenceAvailable").value(false))
                .andExpect(jsonPath("$.scoreAvailable").value(false))
                .andExpect(jsonPath("$.sourceTraceComplete").value(false))
                .andExpect(jsonPath("$.sourceHealth").value("BLOCKED"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionSignal").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.watchlistBounded").value(true))
                .andExpect(jsonPath("$.marketQuoteChecked").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("ANALYSIS_CONTEXT_MISSING"));
    }

    @Test
    void evidenceScoreStatusEndpointDoesNotExposeExecutableCandidateDecisionPointOrTradingFields() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-evidence-score-safe");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-evidence-score-safe")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-evidence-score-safe")).thenReturn(Collections.emptyList());

        MvcResult result = mockMvc.perform(get("/api/dashboard/evidence-score-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(
                "candidateRanking",
                "finalDirection",
                "entry",
                "stop",
                "takeProfit",
                "riskReward",
                "positionSize",
                "leverage",
                "placeOrder",
                "createOrder",
                "submitOrder",
                "auto-trading",
                "order action"
        );
    }

    @Test
    void decisionResultStatusEndpointReturnsReviewOnlyReadyStatus() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-ready");
        decision.setAnalysisId("ana-ready");
        decision.setCreateTime(LocalDateTime.of(2026, 5, 17, 12, 0));
        decision.setConfidenceLevel("HIGH");
        decision.setAiRoleResults("{\"role\":\"present\"}");
        decision.setReadModelTruthStatus("FULL");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);

        mockMvc.perform(get("/api/dashboard/decision-result-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECISIONRESULT_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.analysisId").value("ana-ready"))
                .andExpect(jsonPath("$.decisionAvailable").value(true))
                .andExpect(jsonPath("$.decisionStatus").value("FULL"))
                .andExpect(jsonPath("$.confidence").value("HIGH"))
                .andExpect(jsonPath("$.aiRoleResultsAvailable").value(true))
                .andExpect(jsonPath("$.aiRoleResultsSummary").value("available; raw read-model context hidden from review-only status"))
                .andExpect(jsonPath("$.sourceTraceComplete").value(true))
                .andExpect(jsonPath("$.sourceHealth").value("OK"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.watchlistBounded").value(true))
                .andExpect(jsonPath("$.marketQuoteChecked").value(true))
                .andExpect(jsonPath("$.evidenceScoreChecked").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(false));
    }

    @Test
    void decisionResultStatusEndpointFailsClosedWhenDecisionResultMissing() throws Exception {
        when(decisionService.getLatestDecisionResultBySymbol("ETHUSDT")).thenReturn(null);

        mockMvc.perform(get("/api/dashboard/decision-result-status").param("symbol", "ETHUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECISIONRESULT_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.symbol").value("ETHUSDT"))
                .andExpect(jsonPath("$.analysisId").value(nullValue()))
                .andExpect(jsonPath("$.decisionAvailable").value(false))
                .andExpect(jsonPath("$.decisionStatus").value("UNKNOWN"))
                .andExpect(jsonPath("$.aiRoleResultsAvailable").value(false))
                .andExpect(jsonPath("$.sourceTraceComplete").value(false))
                .andExpect(jsonPath("$.sourceHealth").value("MISSING"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.watchlistBounded").value(true))
                .andExpect(jsonPath("$.marketQuoteChecked").value(true))
                .andExpect(jsonPath("$.evidenceScoreChecked").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("DECISIONRESULT_MISSING"));
    }

    @Test
    void decisionResultStatusEndpointMarksReadModelPartialFailClosed() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-partial");
        decision.setAnalysisId("ana-partial");
        decision.setCreateTime(LocalDateTime.of(2026, 5, 17, 12, 0));
        decision.setConfidenceLevel("MEDIUM");
        decision.setAiRoleResults("{\"role\":\"present\"}");
        decision.setReadModelTruthStatus("PARTIAL");
        decision.setReadModelFallbackReason("LEGACY_MISSING:review_reasons");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);

        mockMvc.perform(get("/api/dashboard/decision-result-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECISIONRESULT_READ_MODEL_PARTIAL"))
                .andExpect(jsonPath("$.decisionAvailable").value(true))
                .andExpect(jsonPath("$.decisionStatus").value("PARTIAL"))
                .andExpect(jsonPath("$.sourceTraceComplete").value(true))
                .andExpect(jsonPath("$.sourceHealth").value("PARTIAL"))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("LEGACY_MISSING:review_reasons"));
    }

    @Test
    void decisionResultStatusEndpointMarksSourceTracePartialFailClosed() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-source-partial");
        decision.setCreateTime(LocalDateTime.of(2026, 5, 17, 12, 0));
        decision.setConfidenceLevel("HIGH");
        decision.setAiRoleResults("{\"role\":\"present\"}");
        decision.setReadModelTruthStatus("FULL");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);

        mockMvc.perform(get("/api/dashboard/decision-result-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECISIONRESULT_SOURCE_TRACE_PARTIAL"))
                .andExpect(jsonPath("$.sourceTraceComplete").value(false))
                .andExpect(jsonPath("$.sourceHealth").value("PARTIAL"))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("SOURCE_TRACE_PARTIAL"));
    }

    @Test
    void decisionResultStatusEndpointMarksAiRolePartialFailClosed() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-ai-partial");
        decision.setAnalysisId("ana-ai-partial");
        decision.setCreateTime(LocalDateTime.of(2026, 5, 17, 12, 0));
        decision.setConfidenceLevel("HIGH");
        decision.setReadModelTruthStatus("FULL");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);

        mockMvc.perform(get("/api/dashboard/decision-result-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECISIONRESULT_AI_ROLE_PARTIAL"))
                .andExpect(jsonPath("$.aiRoleResultsAvailable").value(false))
                .andExpect(jsonPath("$.aiRoleResultsSummary").value("missing"))
                .andExpect(jsonPath("$.sourceTraceComplete").value(true))
                .andExpect(jsonPath("$.sourceHealth").value("PARTIAL"))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("AI_ROLE_RESULTS_MISSING"));
    }

    @Test
    void decisionResultStatusEndpointDoesNotExposeExecutableCandidateDecisionPointOrTradingFields() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-safe");
        decision.setAnalysisId("ana-safe");
        decision.setCreateTime(LocalDateTime.of(2026, 5, 17, 12, 0));
        decision.setConfidenceLevel("HIGH");
        decision.setAiRoleResults("{\"role\":\"present\"}");
        decision.setReadModelTruthStatus("FULL");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);

        mockMvc.perform(get("/api/dashboard/decision-result-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionId").doesNotExist())
                .andExpect(jsonPath("$.candidateRanking").doesNotExist())
                .andExpect(jsonPath("$.candidateScore").doesNotExist())
                .andExpect(jsonPath("$.finalDirection").doesNotExist())
                .andExpect(jsonPath("$.entry").doesNotExist())
                .andExpect(jsonPath("$.stop").doesNotExist())
                .andExpect(jsonPath("$.takeProfit").doesNotExist())
                .andExpect(jsonPath("$.riskReward").doesNotExist())
                .andExpect(jsonPath("$.positionSize").doesNotExist())
                .andExpect(jsonPath("$.leverage").doesNotExist())
                .andExpect(jsonPath("$.orderAction").doesNotExist())
                .andExpect(jsonPath("$.pushSendState").doesNotExist())
                .andExpect(jsonPath("$.autoTradingAction").doesNotExist());
    }

    @Test
    void executionPlanBoundaryStatusEndpointReturnsReviewOnlyReadyStatus() throws Exception {
        MockMvc executionPlanBoundaryMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, decision) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(
                        completeExecutionPlanBoundarySourceTrace(),
                        null
                ),
                (symbol, decision, fallbackDisplay) -> readyPlanBoundaryDisplay(),
                (decision, planBoundaryDisplay, fallbackDisplay) -> readyExecutionPlanDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, fallbackDisplay) -> manualReviewRiskActionGuardDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, riskActionGuardDisplay, fallbackDisplay) -> fallbackDisplay
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-execution-plan-ready");
        decision.setAnalysisId("ana-execution-plan-ready");
        decision.setCreateTime(LocalDateTime.of(2026, 5, 17, 12, 0));
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-execution-plan-ready")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-execution-plan-ready")).thenReturn(Collections.emptyList());

        executionPlanBoundaryMockMvc.perform(get("/api/dashboard/execution-plan-boundary-status")
                        .param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTIONPLAN_BOUNDARY_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.analysisId").value("ana-execution-plan-ready"))
                .andExpect(jsonPath("$.planBoundaryStatus").value("VALID"))
                .andExpect(jsonPath("$.executionPlanStatus").value("READY_REVIEW_ONLY"))
                .andExpect(jsonPath("$.sourceTraceStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.sourceTraceComplete").value(true))
                .andExpect(jsonPath("$.sourceHealth").value("OK"))
                .andExpect(jsonPath("$.riskActionGuardStatus").value("MANUAL_REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.notExecutableReason").value("REVIEW_ONLY_NOT_EXECUTABLE"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.watchlistBounded").value(true))
                .andExpect(jsonPath("$.marketQuoteChecked").value(true))
                .andExpect(jsonPath("$.evidenceScoreChecked").value(true))
                .andExpect(jsonPath("$.decisionResultChecked").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(false));
    }

    @Test
    void executionPlanBoundaryStatusEndpointFailsClosedWhenDecisionResultMissing() throws Exception {
        when(decisionService.getLatestDecisionResultBySymbol("ETHUSDT")).thenReturn(null);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("ETHUSDT", null)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/dashboard/execution-plan-boundary-status").param("symbol", "ETHUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTIONPLAN_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.symbol").value("ETHUSDT"))
                .andExpect(jsonPath("$.analysisId").value(nullValue()))
                .andExpect(jsonPath("$.planBoundaryStatus").value("BACKEND_PENDING"))
                .andExpect(jsonPath("$.executionPlanStatus").value("BOUNDARY_PENDING"))
                .andExpect(jsonPath("$.sourceTraceComplete").value(false))
                .andExpect(jsonPath("$.sourceHealth").value("BLOCKED"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.watchlistBounded").value(true))
                .andExpect(jsonPath("$.marketQuoteChecked").value(true))
                .andExpect(jsonPath("$.evidenceScoreChecked").value(true))
                .andExpect(jsonPath("$.decisionResultChecked").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("DECISIONRESULT_MISSING"));
    }

    @Test
    void executionPlanBoundaryStatusEndpointFailsClosedWhenPlanBoundaryOwnerDataMissing() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-plan-boundary-missing");
        decision.setAnalysisId("ana-plan-boundary-missing");
        decision.setCreateTime(LocalDateTime.of(2026, 5, 17, 12, 0));
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-plan-boundary-missing")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-plan-boundary-missing")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/execution-plan-boundary-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLAN_BOUNDARY_BACKEND_PENDING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.analysisId").value("ana-plan-boundary-missing"))
                .andExpect(jsonPath("$.planBoundaryStatus").value("BACKEND_PENDING"))
                .andExpect(jsonPath("$.executionPlanStatus").value("BOUNDARY_PENDING"))
                .andExpect(jsonPath("$.sourceTraceComplete").value(false))
                .andExpect(jsonPath("$.sourceHealth").value("MISSING"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("PLAN_BOUNDARY_BACKEND_PENDING"));
    }

    @Test
    void executionPlanBoundaryStatusEndpointDoesNotExposeExecutableCandidatePointOrTradingFields() throws Exception {
        MockMvc executionPlanBoundaryMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, decision) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(
                        completeExecutionPlanBoundarySourceTrace(),
                        null
                ),
                (symbol, decision, fallbackDisplay) -> readyPlanBoundaryDisplay(),
                (decision, planBoundaryDisplay, fallbackDisplay) -> readyExecutionPlanDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, fallbackDisplay) -> manualReviewRiskActionGuardDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, riskActionGuardDisplay, fallbackDisplay) -> fallbackDisplay
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-execution-plan-safe");
        decision.setAnalysisId("ana-execution-plan-safe");
        decision.setCreateTime(LocalDateTime.of(2026, 5, 17, 12, 0));
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-execution-plan-safe")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-execution-plan-safe")).thenReturn(Collections.emptyList());

        executionPlanBoundaryMockMvc.perform(get("/api/dashboard/execution-plan-boundary-status")
                        .param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateRanking").doesNotExist())
                .andExpect(jsonPath("$.candidateScore").doesNotExist())
                .andExpect(jsonPath("$.finalDirection").doesNotExist())
                .andExpect(jsonPath("$.entry").doesNotExist())
                .andExpect(jsonPath("$.stop").doesNotExist())
                .andExpect(jsonPath("$.takeProfit").doesNotExist())
                .andExpect(jsonPath("$.tp").doesNotExist())
                .andExpect(jsonPath("$.riskReward").doesNotExist())
                .andExpect(jsonPath("$.rr").doesNotExist())
                .andExpect(jsonPath("$.positionSize").doesNotExist())
                .andExpect(jsonPath("$.leverage").doesNotExist())
                .andExpect(jsonPath("$.orderAction").doesNotExist())
                .andExpect(jsonPath("$.executionAction").doesNotExist())
                .andExpect(jsonPath("$.pushSendState").doesNotExist())
                .andExpect(jsonPath("$.autoTradingAction").doesNotExist());
    }

    @Test
    void riskActionGuardStatusEndpointReturnsReviewOnlyReadyStatus() throws Exception {
        MockMvc riskGuardMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, decision) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(null, null),
                (symbol, decision, fallbackDisplay) -> readyPlanBoundaryDisplay(),
                (decision, planBoundaryDisplay, fallbackDisplay) -> readyExecutionPlanDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, fallbackDisplay) -> manualReviewRiskActionGuardDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, riskActionGuardDisplay, fallbackDisplay) -> fallbackDisplay
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-risk-guard-ready");
        decision.setAnalysisId("ana-risk-guard-ready");
        decision.setCreateTime(LocalDateTime.of(2026, 5, 17, 12, 0));
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);

        riskGuardMockMvc.perform(get("/api/dashboard/risk-action-guard-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RISK_ACTION_GUARD_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.analysisId").value("ana-risk-guard-ready"))
                .andExpect(jsonPath("$.riskActionGuardStatus").value("MANUAL_REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.planBoundaryStatus").value("VALID"))
                .andExpect(jsonPath("$.executionPlanStatus").value("READY_REVIEW_ONLY"))
                .andExpect(jsonPath("$.manualRiskReviewRequired").value(true))
                .andExpect(jsonPath("$.actionFlagsAllFalse").value(true))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.notPositionMonitorExecution").value(true))
                .andExpect(jsonPath("$.notExecutionPlanGeneration").value(true))
                .andExpect(jsonPath("$.notBoundaryCandidateGeneration").value(true))
                .andExpect(jsonPath("$.externalRefreshTriggered").value(false))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(false));
    }

    @Test
    void riskActionGuardStatusEndpointFailsClosedWhenDecisionMissing() throws Exception {
        when(decisionService.getLatestDecisionResultBySymbol("ETHUSDT")).thenReturn(null);

        mockMvc.perform(get("/api/dashboard/risk-action-guard-status").param("symbol", "ETHUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECISION_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.symbol").value("ETHUSDT"))
                .andExpect(jsonPath("$.analysisId").value(nullValue()))
                .andExpect(jsonPath("$.sourceHealth").value("BLOCKED"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("DECISION_MISSING"));
    }

    @Test
    void riskActionGuardStatusEndpointFailsClosedWhenActionFlagsAreTrue() throws Exception {
        MockMvc riskGuardMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, decision) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(null, null),
                (symbol, decision, fallbackDisplay) -> readyPlanBoundaryDisplay(),
                (decision, planBoundaryDisplay, fallbackDisplay) -> readyExecutionPlanDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, fallbackDisplay) -> actionFlagRiskActionGuardDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, riskActionGuardDisplay, fallbackDisplay) -> fallbackDisplay
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-risk-guard-action-flag");
        decision.setAnalysisId("ana-risk-guard-action-flag");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);

        riskGuardMockMvc.perform(get("/api/dashboard/risk-action-guard-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTION_FLAGS_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.actionFlagsAllFalse").value(false))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("RISK_ACTION_GUARD_ACTION_FLAGS_TRUE"));
    }

    @Test
    void riskActionGuardStatusEndpointFailsClosedWhenActionWordingIsUnsafe() throws Exception {
        MockMvc riskGuardMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, decision) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(null, null),
                (symbol, decision, fallbackDisplay) -> readyPlanBoundaryDisplay(),
                (decision, planBoundaryDisplay, fallbackDisplay) -> readyExecutionPlanDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, fallbackDisplay) -> unsafeActionWordingRiskActionGuardDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, riskActionGuardDisplay, fallbackDisplay) -> fallbackDisplay
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-risk-guard-action-wording");
        decision.setAnalysisId("ana-risk-guard-action-wording");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);

        riskGuardMockMvc.perform(get("/api/dashboard/risk-action-guard-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTION_WORDING_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.riskActionAdviceSummary").value("RiskActionGuard advice 包含可执行动作措辞，已按只读 fail-closed 处理。"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("RISK_ACTION_GUARD_ACTION_WORDING_UNSAFE"));
    }

    @Test
    void riskActionGuardStatusEndpointDoesNotExposeExecutableCandidatePointOrTradingFields() throws Exception {
        MockMvc riskGuardMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, decision) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(null, null),
                (symbol, decision, fallbackDisplay) -> readyPlanBoundaryDisplay(),
                (decision, planBoundaryDisplay, fallbackDisplay) -> readyExecutionPlanDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, fallbackDisplay) -> manualReviewRiskActionGuardDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, riskActionGuardDisplay, fallbackDisplay) -> fallbackDisplay
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-risk-guard-safe");
        decision.setAnalysisId("ana-risk-guard-safe");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);

        riskGuardMockMvc.perform(get("/api/dashboard/risk-action-guard-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateRanking").doesNotExist())
                .andExpect(jsonPath("$.candidateScore").doesNotExist())
                .andExpect(jsonPath("$.finalDirection").doesNotExist())
                .andExpect(jsonPath("$.entry").doesNotExist())
                .andExpect(jsonPath("$.stop").doesNotExist())
                .andExpect(jsonPath("$.takeProfit").doesNotExist())
                .andExpect(jsonPath("$.tp").doesNotExist())
                .andExpect(jsonPath("$.riskReward").doesNotExist())
                .andExpect(jsonPath("$.rr").doesNotExist())
                .andExpect(jsonPath("$.positionSize").doesNotExist())
                .andExpect(jsonPath("$.leverage").doesNotExist())
                .andExpect(jsonPath("$.orderAction").doesNotExist())
                .andExpect(jsonPath("$.executionAction").doesNotExist())
                .andExpect(jsonPath("$.pushSendState").doesNotExist())
                .andExpect(jsonPath("$.autoTradingAction").doesNotExist());
    }

    @Test
    void paperObservationStatusEndpointReturnsReviewOnlyReadyStatus() throws Exception {
        MockMvc paperObservationMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, decision) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(null, null),
                (symbol, decision, fallbackDisplay) -> readyPlanBoundaryDisplay(),
                (decision, planBoundaryDisplay, fallbackDisplay) -> readyExecutionPlanDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, fallbackDisplay) -> manualReviewRiskActionGuardDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, riskActionGuardDisplay, fallbackDisplay) ->
                        readyPaperObservationDisplay()
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-paper-observation-ready");
        decision.setAnalysisId("ana-paper-observation-ready");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());

        paperObservationMockMvc.perform(get("/api/dashboard/paper-observation-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAPER_OBSERVATION_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.analysisId").value("ana-paper-observation-ready"))
                .andExpect(jsonPath("$.paperObservationStatus").value("MANUAL_REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.reviewSummary").value("AVAILABLE_REVIEW_ONLY"))
                .andExpect(jsonPath("$.paperObservationAvailable").value(false))
                .andExpect(jsonPath("$.manualReviewSurfaceAvailable").value(false))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.notRealPosition").value(true))
                .andExpect(jsonPath("$.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.notPaperOrder").value(true))
                .andExpect(jsonPath("$.notSimulatedExecution").value(true))
                .andExpect(jsonPath("$.notPaperPnlGeneration").value(true))
                .andExpect(jsonPath("$.notPositionMonitorExecution").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notFinalDirection").value(true))
                .andExpect(jsonPath("$.notEntryStopTpRr").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.paperOwnerSafetyFlagsAllTrue").value(true))
                .andExpect(jsonPath("$.failClosed").value(false));
    }

    @Test
    void paperObservationStatusEndpointFailsClosedWhenDecisionMissing() throws Exception {
        when(decisionService.getLatestDecisionResultBySymbol("ETHUSDT")).thenReturn(null);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("ETHUSDT", null)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/dashboard/paper-observation-status").param("symbol", "ETHUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAPER_OBSERVATION_BACKEND_PENDING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.symbol").value("ETHUSDT"))
                .andExpect(jsonPath("$.analysisId").value(nullValue()))
                .andExpect(jsonPath("$.sourceHealth").value("BLOCKED"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.notRealPosition").value(true))
                .andExpect(jsonPath("$.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.notPaperOrder").value(true))
                .andExpect(jsonPath("$.notSimulatedExecution").value(true))
                .andExpect(jsonPath("$.notPaperPnlGeneration").value(true))
                .andExpect(jsonPath("$.notPositionMonitorExecution").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("DECISION_MISSING"));
    }

    @Test
    void paperObservationStatusEndpointFailsClosedWhenPaperExecutionBoundaryIsUnsafe() throws Exception {
        MockMvc paperObservationMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, decision) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(null, null),
                (symbol, decision, fallbackDisplay) -> readyPlanBoundaryDisplay(),
                (decision, planBoundaryDisplay, fallbackDisplay) -> readyExecutionPlanDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, fallbackDisplay) -> manualReviewRiskActionGuardDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, riskActionGuardDisplay, fallbackDisplay) ->
                        unsafePaperObservationDisplay()
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-paper-observation-unsafe");
        decision.setAnalysisId("ana-paper-observation-unsafe");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());

        paperObservationMockMvc.perform(get("/api/dashboard/paper-observation-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAPER_ORDER_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.paperObservationAvailable").value(true))
                .andExpect(jsonPath("$.manualReviewSurfaceAvailable").value(true))
                .andExpect(jsonPath("$.paperOwnerSafetyFlagsAllTrue").value(false))
                .andExpect(jsonPath("$.notPaperOrder").value(true))
                .andExpect(jsonPath("$.notSimulatedExecution").value(true))
                .andExpect(jsonPath("$.notPaperPnlGeneration").value(true))
                .andExpect(jsonPath("$.notPositionMonitorExecution").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("PAPER_EXECUTION_BOUNDARY_UNSAFE"));
    }

    @Test
    void paperObservationStatusEndpointDoesNotExposeExecutablePaperTradingCandidatePointOrTradingFields() throws Exception {
        MockMvc paperObservationMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, decision) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(null, null),
                (symbol, decision, fallbackDisplay) -> readyPlanBoundaryDisplay(),
                (decision, planBoundaryDisplay, fallbackDisplay) -> readyExecutionPlanDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, fallbackDisplay) -> manualReviewRiskActionGuardDisplay(),
                (decision, planBoundaryDisplay, executionPlanDisplay, riskActionGuardDisplay, fallbackDisplay) ->
                        readyPaperObservationDisplay()
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-paper-observation-safe");
        decision.setAnalysisId("ana-paper-observation-safe");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());

        paperObservationMockMvc.perform(get("/api/dashboard/paper-observation-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateRanking").doesNotExist())
                .andExpect(jsonPath("$.candidateScore").doesNotExist())
                .andExpect(jsonPath("$.finalDirection").doesNotExist())
                .andExpect(jsonPath("$.entry").doesNotExist())
                .andExpect(jsonPath("$.stop").doesNotExist())
                .andExpect(jsonPath("$.takeProfit").doesNotExist())
                .andExpect(jsonPath("$.tp").doesNotExist())
                .andExpect(jsonPath("$.riskReward").doesNotExist())
                .andExpect(jsonPath("$.rr").doesNotExist())
                .andExpect(jsonPath("$.paperOrder").doesNotExist())
                .andExpect(jsonPath("$.paperOrderAction").doesNotExist())
                .andExpect(jsonPath("$.simulatedExecution").doesNotExist())
                .andExpect(jsonPath("$.simulatedExecutionAction").doesNotExist())
                .andExpect(jsonPath("$.paperPnl").doesNotExist())
                .andExpect(jsonPath("$.manualReviewEntryAvailable").doesNotExist())
                .andExpect(jsonPath("$.realPosition").doesNotExist())
                .andExpect(jsonPath("$.positionMonitorAction").doesNotExist())
                .andExpect(jsonPath("$.orderAction").doesNotExist())
                .andExpect(jsonPath("$.executionAction").doesNotExist())
                .andExpect(jsonPath("$.pushSendState").doesNotExist())
                .andExpect(jsonPath("$.autoTradingAction").doesNotExist());
    }

    @Test
    void alertFatiguePolicyStatusEndpointReturnsReviewOnlyReadyStatus() throws Exception {
        when(monitorService.getRecentAlerts(20)).thenReturn(List.of(
                monitorAlert("BTCUSDT", "RISK_ALERT", "WARN", "OPEN", null, null)
        ));

        mockMvc.perform(get("/api/dashboard/alert-fatigue-policy-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALERT_POLICY_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.recentAlertCount").value(1))
                .andExpect(jsonPath("$.openAlertCount").value(1))
                .andExpect(jsonPath("$.suppressedAlertCount").value(0))
                .andExpect(jsonPath("$.cooldownActive").value(false))
                .andExpect(jsonPath("$.suppressionActive").value(false))
                .andExpect(jsonPath("$.duplicateRiskVisible").value(false))
                .andExpect(jsonPath("$.fatigueHigh").value(false))
                .andExpect(jsonPath("$.policySource").value("MonitorAlert read model"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notPushSend").value(true))
                .andExpect(jsonPath("$.notExternalChannel").value(true))
                .andExpect(jsonPath("$.notRecheckExecution").value(true))
                .andExpect(jsonPath("$.notSchedulerTrigger").value(true))
                .andExpect(jsonPath("$.notCollectorTrigger").value(true))
                .andExpect(jsonPath("$.notApiClientRefresh").value(true))
                .andExpect(jsonPath("$.notAlertWrite").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(false))
                .andExpect(jsonPath("$.statusMapping[?(@ == 'PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED')]").exists());
    }

    @Test
    void alertFatiguePolicyStatusEndpointFailsClosedWhenReadModelMissing() throws Exception {
        when(monitorService.getRecentAlerts(20)).thenReturn(null);

        mockMvc.perform(get("/api/dashboard/alert-fatigue-policy-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALERT_READ_MODEL_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.sourceHealth").value("MISSING"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notPushSend").value(true))
                .andExpect(jsonPath("$.notRecheckExecution").value(true))
                .andExpect(jsonPath("$.notAlertWrite").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void alertFatiguePolicyStatusEndpointShowsSuppressionAndCooldownAsReviewOnly() throws Exception {
        when(monitorService.getRecentAlerts(20)).thenReturn(List.of(
                monitorAlert("BTCUSDT", "RISK_ALERT", "WARN", "SUPPRESSED", "2026-06-12T12:15:00", "dedupe window")
        ));

        mockMvc.perform(get("/api/dashboard/alert-fatigue-policy-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALERT_SUPPRESSION_ACTIVE_REVIEW_ONLY"))
                .andExpect(jsonPath("$.suppressionActive").value(true))
                .andExpect(jsonPath("$.cooldownActive").value(true))
                .andExpect(jsonPath("$.suppressedAlertCount").value(1))
                .andExpect(jsonPath("$.notPushSend").value(true))
                .andExpect(jsonPath("$.notExternalChannel").value(true))
                .andExpect(jsonPath("$.notRecheckExecution").value(true))
                .andExpect(jsonPath("$.notSchedulerTrigger").value(true))
                .andExpect(jsonPath("$.notCollectorTrigger").value(true))
                .andExpect(jsonPath("$.notApiClientRefresh").value(true))
                .andExpect(jsonPath("$.notAlertWrite").value(true))
                .andExpect(jsonPath("$.failClosed").value(false));
    }

    @Test
    void alertFatiguePolicyStatusEndpointDoesNotExposeExecutableSendRefreshCandidatePointOrTradingFields() throws Exception {
        when(monitorService.getRecentAlerts(20)).thenReturn(List.of(
                monitorAlert("BTCUSDT", "RISK_ALERT", "WARN", "OPEN", null, null)
        ));

        mockMvc.perform(get("/api/dashboard/alert-fatigue-policy-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateRanking").doesNotExist())
                .andExpect(jsonPath("$.candidateScore").doesNotExist())
                .andExpect(jsonPath("$.finalDirection").doesNotExist())
                .andExpect(jsonPath("$.entry").doesNotExist())
                .andExpect(jsonPath("$.stop").doesNotExist())
                .andExpect(jsonPath("$.takeProfit").doesNotExist())
                .andExpect(jsonPath("$.tp").doesNotExist())
                .andExpect(jsonPath("$.riskReward").doesNotExist())
                .andExpect(jsonPath("$.rr").doesNotExist())
                .andExpect(jsonPath("$.positionSize").doesNotExist())
                .andExpect(jsonPath("$.leverage").doesNotExist())
                .andExpect(jsonPath("$.orderAction").doesNotExist())
                .andExpect(jsonPath("$.executionAction").doesNotExist())
                .andExpect(jsonPath("$.pushSend").doesNotExist())
                .andExpect(jsonPath("$.pushSendState").doesNotExist())
                .andExpect(jsonPath("$.externalChannelAction").doesNotExist())
                .andExpect(jsonPath("$.recheckExecutionAction").doesNotExist())
                .andExpect(jsonPath("$.schedulerTrigger").doesNotExist())
                .andExpect(jsonPath("$.collectorTrigger").doesNotExist())
                .andExpect(jsonPath("$.apiClientRefreshAction").doesNotExist())
                .andExpect(jsonPath("$.alertWriteAction").doesNotExist())
                .andExpect(jsonPath("$.autoTradingAction").doesNotExist());
    }

    @Test
    void accountRiskExposureStatusEndpointReturnsReviewOnlyReadyStatusFromLatestSnapshotOwnerPath() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-account-ready");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(accountRiskSnapshotMapper.selectLatestByAnalysisId("ana-account-ready"))
                .thenReturn(accountRiskSnapshot("ana-account-ready", "BTCUSDT", true,
                        "NORMAL", new BigDecimal("0.12"), new BigDecimal("0.30")));

        mockMvc.perform(get("/api/dashboard/account-risk-exposure-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCOUNT_RISK_STATUS_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.analysisId").value("ana-account-ready"))
                .andExpect(jsonPath("$.snapshotId").value(9001))
                .andExpect(jsonPath("$.riskLevelSnapshot").value("NORMAL"))
                .andExpect(jsonPath("$.riskAllowedEvidence").value(true))
                .andExpect(jsonPath("$.riskAllowedStatus").value("RISK_ALLOWED_READ_ONLY_EVIDENCE"))
                .andExpect(jsonPath("$.positionExposure").value(0.12))
                .andExpect(jsonPath("$.maxAllowedExposure").value(0.30))
                .andExpect(jsonPath("$.accountExposureStatus").value("ACCOUNT_EXPOSURE_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.ownerPath").value("AccountRiskSnapshotMapper.selectLatestByAnalysisId"))
                .andExpect(jsonPath("$.historicalSnapshotReadOnly").value("AccountRiskSnapshotMapper.selectById is historical snapshot read only and not the runtime owner path"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.notAccountRiskWrite").value(true))
                .andExpect(jsonPath("$.notPushSnapshotWrite").value(true))
                .andExpect(jsonPath("$.notPushSend").value(true))
                .andExpect(jsonPath("$.notRecheckExecution").value(true))
                .andExpect(jsonPath("$.notTradingAuthorization").value(true))
                .andExpect(jsonPath("$.notPositionSizing").value(true))
                .andExpect(jsonPath("$.notReduceCloseStopReverseGuidance").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notFinalDirection").value(true))
                .andExpect(jsonPath("$.notEntryStopTpRr").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.accountRiskWriteBoundaryStatus").value("ACCOUNT_RISK_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.pushSnapshotWriteBoundaryStatus").value("PUSH_SNAPSHOT_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.pushBoundaryStatus").value("PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.recheckBoundaryStatus").value("RECHECK_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.tradingAuthorizationBoundaryStatus").value("TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.positionSizingBoundaryStatus").value("POSITION_SIZING_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.reduceCloseStopReverseBoundaryStatus").value("REDUCE_CLOSE_STOP_REVERSE_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.candidateBoundaryStatus").value("CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.pointBoundaryStatus").value("POINT_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.tradingBoundaryStatus").value("TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.failClosed").value(false))
                .andExpect(jsonPath("$.statusMapping[?(@ == 'ACCOUNT_RISK_STATUS_REVIEW_ONLY_READY')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'ACCOUNT_EXPOSURE_REVIEW_ONLY_READY')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'TRADING_AUTHORIZATION_BOUNDARY_BLOCKED_FAIL_CLOSED')]").exists());

        verify(accountRiskSnapshotMapper).selectLatestByAnalysisId("ana-account-ready");
        verify(accountRiskSnapshotMapper, never()).selectById(anyLong());
    }

    @Test
    void accountRiskExposureStatusEndpointFailsClosedWhenSnapshotMissing() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-account-missing");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(accountRiskSnapshotMapper.selectLatestByAnalysisId("ana-account-missing")).thenReturn(null);

        mockMvc.perform(get("/api/dashboard/account-risk-exposure-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCOUNT_RISK_STATUS_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.sourceHealth").value("MISSING"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notAccountRiskWrite").value(true))
                .andExpect(jsonPath("$.notPushSnapshotWrite").value(true))
                .andExpect(jsonPath("$.notTradingAuthorization").value(true))
                .andExpect(jsonPath("$.notPositionSizing").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void accountRiskExposureStatusEndpointFailsClosedWhenReadPathThrows() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-account-error");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(accountRiskSnapshotMapper.selectLatestByAnalysisId("ana-account-error"))
                .thenThrow(new RuntimeException("read unavailable"));

        mockMvc.perform(get("/api/dashboard/account-risk-exposure-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCOUNT_RISK_STATUS_BACKEND_PENDING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.sourceHealth").value("BLOCKED"))
                .andExpect(jsonPath("$.notAccountRiskWrite").value(true))
                .andExpect(jsonPath("$.notPushSnapshotWrite").value(true))
                .andExpect(jsonPath("$.notPushSend").value(true))
                .andExpect(jsonPath("$.notRecheckExecution").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void accountRiskExposureStatusEndpointFailsClosedWhenExposureMissing() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-account-exposure-missing");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(accountRiskSnapshotMapper.selectLatestByAnalysisId("ana-account-exposure-missing"))
                .thenReturn(accountRiskSnapshot("ana-account-exposure-missing", "BTCUSDT", true,
                        "NORMAL", null, null));

        mockMvc.perform(get("/api/dashboard/account-risk-exposure-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCOUNT_EXPOSURE_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.accountExposureStatus").value("ACCOUNT_EXPOSURE_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.riskAllowedStatus").value("RISK_ALLOWED_READ_ONLY_EVIDENCE"))
                .andExpect(jsonPath("$.notTradingAuthorization").value(true))
                .andExpect(jsonPath("$.notPositionSizing").value(true))
                .andExpect(jsonPath("$.notReduceCloseStopReverseGuidance").value(true))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void accountRiskExposureStatusEndpointDoesNotExposeAccountActionCandidatePointOrTradingFields() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-account-safe");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(accountRiskSnapshotMapper.selectLatestByAnalysisId("ana-account-safe"))
                .thenReturn(accountRiskSnapshot("ana-account-safe", "BTCUSDT", true,
                        "NORMAL", new BigDecimal("0.12"), new BigDecimal("0.30")));

        mockMvc.perform(get("/api/dashboard/account-risk-exposure-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountRiskWriteAction").doesNotExist())
                .andExpect(jsonPath("$.pushSnapshotWriteAction").doesNotExist())
                .andExpect(jsonPath("$.pushSend").doesNotExist())
                .andExpect(jsonPath("$.pushSendState").doesNotExist())
                .andExpect(jsonPath("$.recheckExecutionAction").doesNotExist())
                .andExpect(jsonPath("$.tradingAuthorization").doesNotExist())
                .andExpect(jsonPath("$.positionSize").doesNotExist())
                .andExpect(jsonPath("$.leverage").doesNotExist())
                .andExpect(jsonPath("$.positionSizing").doesNotExist())
                .andExpect(jsonPath("$.reduceGuidance").doesNotExist())
                .andExpect(jsonPath("$.closeGuidance").doesNotExist())
                .andExpect(jsonPath("$.stopGuidance").doesNotExist())
                .andExpect(jsonPath("$.reverseGuidance").doesNotExist())
                .andExpect(jsonPath("$.candidateRanking").doesNotExist())
                .andExpect(jsonPath("$.candidateScore").doesNotExist())
                .andExpect(jsonPath("$.finalDirection").doesNotExist())
                .andExpect(jsonPath("$.entry").doesNotExist())
                .andExpect(jsonPath("$.stop").doesNotExist())
                .andExpect(jsonPath("$.takeProfit").doesNotExist())
                .andExpect(jsonPath("$.tp").doesNotExist())
                .andExpect(jsonPath("$.riskReward").doesNotExist())
                .andExpect(jsonPath("$.rr").doesNotExist())
                .andExpect(jsonPath("$.orderAction").doesNotExist())
                .andExpect(jsonPath("$.executionAction").doesNotExist())
                .andExpect(jsonPath("$.autoTradingAction").doesNotExist());
    }

    @Test
    void hotResetEventImpactSourceStatusEndpointReturnsReviewOnlyEvidenceAndFailClosedOwnership() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-hot-ready");
        decision.setTimeframe("1m");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(sourceTraceEventSourceOwnershipService.resolveEventSourceOwnership(any(RuntimeKlineContextDTO.class)))
                .thenReturn(SourceTraceEventSourceOwnershipResult.missingSource("BTCUSDT", "1m"));
        when(hotResetEventMapper.selectLatestByAnalysisId("ana-hot-ready"))
                .thenReturn(hotResetEvent("ana-hot-ready", true));
        when(hotResetEventMapper.countByAnalysisId("ana-hot-ready")).thenReturn(2);

        mockMvc.perform(get("/api/dashboard/hot-reset-event-impact-source-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.analysisId").value("ana-hot-ready"))
                .andExpect(jsonPath("$.timeframe").value("1m"))
                .andExpect(jsonPath("$.hotResetEventAvailable").value(true))
                .andExpect(jsonPath("$.hotResetEventSourceStatus").value("HOT_RESET_EVENT_SOURCE_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.eventImpactSourceStatus").value("EVENT_IMPACT_SOURCE_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.sourceTraceEventSourceOwnershipStatus").value("SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED"))
                .andExpect(jsonPath("$.sourceTraceEventSourceOwnershipMissingReason").value("MISSING_SOURCE"))
                .andExpect(jsonPath("$.hotResetEventCount").value(2))
                .andExpect(jsonPath("$.hotResetTriggerType").value("MACRO_EVENT_IMPACT"))
                .andExpect(jsonPath("$.hotResetTriggerReasonCode").value("EVENT_IMPACT_SOURCE_READ"))
                .andExpect(jsonPath("$.ownerPath").value("DecisionResult.latest.analysisId -> HotResetEventMapper.selectLatestByAnalysisId/countByAnalysisId -> SourceTraceEventSourceOwnershipService.resolveEventSourceOwnership"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.notHotResetExecution").value(true))
                .andExpect(jsonPath("$.notHotResetWrite").value(true))
                .andExpect(jsonPath("$.notEventGeneration").value(true))
                .andExpect(jsonPath("$.notExternalApiRefresh").value(true))
                .andExpect(jsonPath("$.notNewsFetch").value(true))
                .andExpect(jsonPath("$.notSchedulerTrigger").value(true))
                .andExpect(jsonPath("$.notCollectorTrigger").value(true))
                .andExpect(jsonPath("$.notPushSend").value(true))
                .andExpect(jsonPath("$.notExternalChannel").value(true))
                .andExpect(jsonPath("$.notRecheckExecution").value(true))
                .andExpect(jsonPath("$.notReplayExecution").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notFinalDirection").value(true))
                .andExpect(jsonPath("$.notEntryStopTpRr").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.hotResetExecutionBoundaryStatus").value("HOT_RESET_EXECUTION_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.hotResetWriteBoundaryStatus").value("HOT_RESET_WRITE_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.eventGenerationBoundaryStatus").value("EVENT_GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.externalApiRefreshBoundaryStatus").value("EXTERNAL_API_REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.newsFetchBoundaryStatus").value("NEWS_FETCH_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.schedulerTriggerBoundaryStatus").value("SCHEDULER_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.collectorTriggerBoundaryStatus").value("COLLECTOR_TRIGGER_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.pushBoundaryStatus").value("PUSH_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.recheckReplayBoundaryStatus").value("RECHECK_REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.candidateBoundaryStatus").value("CANDIDATE_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.pointBoundaryStatus").value("POINT_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.tradingBoundaryStatus").value("TRADING_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.statusMapping[?(@ == 'HOT_RESET_EVENT_SOURCE_REVIEW_ONLY_READY')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'EVENT_IMPACT_SOURCE_REVIEW_ONLY_READY')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED')]").exists())
                .andExpect(jsonPath("$.statusMapping[?(@ == 'RECHECK_REPLAY_BOUNDARY_BLOCKED_FAIL_CLOSED')]").exists());

        verify(hotResetEventMapper).selectLatestByAnalysisId("ana-hot-ready");
        verify(hotResetEventMapper).countByAnalysisId("ana-hot-ready");
        verify(hotResetEventMapper, never()).insert(any(HotResetEventDO.class));
    }

    @Test
    void hotResetEventImpactSourceStatusEndpointFailsClosedWhenEventMissing() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-hot-missing");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(hotResetEventMapper.selectLatestByAnalysisId("ana-hot-missing")).thenReturn(null);

        mockMvc.perform(get("/api/dashboard/hot-reset-event-impact-source-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.hotResetEventSourceStatus").value("HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.eventImpactSourceStatus").value("EVENT_IMPACT_SOURCE_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.sourceHealth").value("MISSING"))
                .andExpect(jsonPath("$.notHotResetExecution").value(true))
                .andExpect(jsonPath("$.notHotResetWrite").value(true))
                .andExpect(jsonPath("$.notEventGeneration").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void hotResetEventImpactSourceStatusEndpointFailsClosedWhenReadPathThrows() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-hot-error");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(hotResetEventMapper.selectLatestByAnalysisId("ana-hot-error"))
                .thenThrow(new RuntimeException("read unavailable"));

        mockMvc.perform(get("/api/dashboard/hot-reset-event-impact-source-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HOT_RESET_EVENT_SOURCE_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.reason").value("HOT_RESET_EVENT_READ_PATH_UNAVAILABLE"))
                .andExpect(jsonPath("$.sourceHealth").value("BLOCKED"))
                .andExpect(jsonPath("$.notExternalApiRefresh").value(true))
                .andExpect(jsonPath("$.notNewsFetch").value(true))
                .andExpect(jsonPath("$.notSchedulerTrigger").value(true))
                .andExpect(jsonPath("$.notCollectorTrigger").value(true))
                .andExpect(jsonPath("$.notPushSend").value(true))
                .andExpect(jsonPath("$.notRecheckExecution").value(true))
                .andExpect(jsonPath("$.notReplayExecution").value(true))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void hotResetEventImpactSourceStatusEndpointMarksPartialEventAsReviewOnlySubstatus() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-hot-partial");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(hotResetEventMapper.selectLatestByAnalysisId("ana-hot-partial"))
                .thenReturn(hotResetEvent("ana-hot-partial", false));
        when(hotResetEventMapper.countByAnalysisId("ana-hot-partial")).thenReturn(1);

        mockMvc.perform(get("/api/dashboard/hot-reset-event-impact-source-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hotResetEventSourceStatus").value("HOT_RESET_EVENT_SOURCE_PARTIAL_REVIEW_ONLY"))
                .andExpect(jsonPath("$.sourceTraceEventSourceOwnershipStatus").value("SOURCE_TRACE_EVENT_SOURCE_OWNERSHIP_INCOMPLETE_FAIL_CLOSED"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.manualReviewOnly").value(true))
                .andExpect(jsonPath("$.notHotResetExecution").value(true))
                .andExpect(jsonPath("$.notEventGeneration").value(true))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void hotResetEventImpactSourceStatusEndpointDoesNotExposeExecutionRefreshSignalOrTradingFields() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-hot-safe");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(hotResetEventMapper.selectLatestByAnalysisId("ana-hot-safe"))
                .thenReturn(hotResetEvent("ana-hot-safe", true));
        when(hotResetEventMapper.countByAnalysisId("ana-hot-safe")).thenReturn(1);

        mockMvc.perform(get("/api/dashboard/hot-reset-event-impact-source-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hotResetExecutionAction").doesNotExist())
                .andExpect(jsonPath("$.hotResetWriteAction").doesNotExist())
                .andExpect(jsonPath("$.eventGenerationAction").doesNotExist())
                .andExpect(jsonPath("$.externalApiRefreshAction").doesNotExist())
                .andExpect(jsonPath("$.newsFetchAction").doesNotExist())
                .andExpect(jsonPath("$.schedulerAction").doesNotExist())
                .andExpect(jsonPath("$.collectorAction").doesNotExist())
                .andExpect(jsonPath("$.pushSend").doesNotExist())
                .andExpect(jsonPath("$.externalChannelAction").doesNotExist())
                .andExpect(jsonPath("$.recheckExecutionAction").doesNotExist())
                .andExpect(jsonPath("$.replayExecutionAction").doesNotExist())
                .andExpect(jsonPath("$.candidateRanking").doesNotExist())
                .andExpect(jsonPath("$.candidateScore").doesNotExist())
                .andExpect(jsonPath("$.finalDirection").doesNotExist())
                .andExpect(jsonPath("$.entry").doesNotExist())
                .andExpect(jsonPath("$.stop").doesNotExist())
                .andExpect(jsonPath("$.takeProfit").doesNotExist())
                .andExpect(jsonPath("$.tp").doesNotExist())
                .andExpect(jsonPath("$.riskReward").doesNotExist())
                .andExpect(jsonPath("$.rr").doesNotExist())
                .andExpect(jsonPath("$.orderAction").doesNotExist())
                .andExpect(jsonPath("$.executionAction").doesNotExist())
                .andExpect(jsonPath("$.autoTradingAction").doesNotExist())
                .andExpect(jsonPath("$.positionMonitorAction").doesNotExist());
    }

    @Test
    void reviewReplayStatusEndpointReturnsReviewOnlyReadyStatus() throws Exception {
        when(reviewService.getStateByAnalysisId("ana-review-ready"))
                .thenReturn(reviewState("ana-review-ready"));
        when(reviewAggregateService.getAggregateSummaryByAnalysisId("ana-review-ready"))
                .thenReturn(Optional.of(reviewAggregateSummary("ana-review-ready", "BTCUSDT", 2, true, true)));

        mockMvc.perform(get("/api/dashboard/review-replay-result-status")
                        .param("analysisId", "ana-review-ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_REPLAY_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.analysisId").value("ana-review-ready"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.reviewResultAvailable").value(true))
                .andExpect(jsonPath("$.reviewAggregateAvailable").value(true))
                .andExpect(jsonPath("$.reviewClosureAvailable").value(true))
                .andExpect(jsonPath("$.replaySummaryAvailable").value(true))
                .andExpect(jsonPath("$.replaySummaryCount").value(2))
                .andExpect(jsonPath("$.sourceTraceComplete").value(true))
                .andExpect(jsonPath("$.sourceHealth").value("OK"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notReplayExecution").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.watchlistBounded").value(true))
                .andExpect(jsonPath("$.marketQuoteChecked").value(true))
                .andExpect(jsonPath("$.evidenceScoreChecked").value(true))
                .andExpect(jsonPath("$.decisionResultChecked").value(true))
                .andExpect(jsonPath("$.executionPlanBoundaryChecked").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(false));
    }

    @Test
    void reviewReplayStatusEndpointFailsClosedWhenReviewResultMissing() throws Exception {
        when(reviewService.getStateByAnalysisId("ana-review-missing")).thenReturn(null);

        mockMvc.perform(get("/api/dashboard/review-replay-result-status")
                        .param("analysisId", "ana-review-missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_RESULT_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.analysisId").value("ana-review-missing"))
                .andExpect(jsonPath("$.reviewResultAvailable").value(false))
                .andExpect(jsonPath("$.reviewAggregateAvailable").value(false))
                .andExpect(jsonPath("$.replaySummaryAvailable").value(false))
                .andExpect(jsonPath("$.sourceTraceComplete").value(false))
                .andExpect(jsonPath("$.sourceHealth").value("MISSING"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notReplayExecution").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("REVIEW_RESULT_MISSING"));
    }

    @Test
    void reviewReplayStatusEndpointFailsClosedWhenAggregateMissing() throws Exception {
        when(reviewService.getStateByAnalysisId("ana-aggregate-missing"))
                .thenReturn(reviewState("ana-aggregate-missing"));
        when(reviewAggregateService.getAggregateSummaryByAnalysisId("ana-aggregate-missing"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/dashboard/review-replay-result-status")
                        .param("analysisId", "ana-aggregate-missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_AGGREGATE_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.reviewResultAvailable").value(true))
                .andExpect(jsonPath("$.reviewAggregateAvailable").value(false))
                .andExpect(jsonPath("$.replaySummaryAvailable").value(false))
                .andExpect(jsonPath("$.sourceHealth").value("MISSING"))
                .andExpect(jsonPath("$.notReplayExecution").value(true))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("REVIEW_AGGREGATE_MISSING"));
    }

    @Test
    void reviewReplayStatusEndpointFailsClosedWhenReplaySummaryMissing() throws Exception {
        when(reviewService.getStateByAnalysisId("ana-replay-missing"))
                .thenReturn(reviewState("ana-replay-missing"));
        when(reviewAggregateService.getAggregateSummaryByAnalysisId("ana-replay-missing"))
                .thenReturn(Optional.of(reviewAggregateSummary("ana-replay-missing", "BTCUSDT", 0, true, true)));

        mockMvc.perform(get("/api/dashboard/review-replay-result-status")
                        .param("analysisId", "ana-replay-missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPLAY_SUMMARY_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.reviewResultAvailable").value(true))
                .andExpect(jsonPath("$.reviewAggregateAvailable").value(true))
                .andExpect(jsonPath("$.reviewClosureAvailable").value(true))
                .andExpect(jsonPath("$.replaySummaryAvailable").value(false))
                .andExpect(jsonPath("$.replaySummaryCount").value(0))
                .andExpect(jsonPath("$.sourceHealth").value("MISSING"))
                .andExpect(jsonPath("$.notReplayExecution").value(true))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("REPLAY_SUMMARY_MISSING"));
    }

    @Test
    void reviewReplayStatusEndpointBlocksWhenReplaySummaryOwnerPathMissing() throws Exception {
        when(reviewService.getStateByAnalysisId("ana-replay-owner-missing"))
                .thenReturn(reviewState("ana-replay-owner-missing"));
        when(reviewAggregateService.getAggregateSummaryByAnalysisId("ana-replay-owner-missing"))
                .thenReturn(Optional.of(reviewAggregateSummary("ana-replay-owner-missing", "BTCUSDT", 0, true, false)));

        mockMvc.perform(get("/api/dashboard/review-replay-result-status")
                        .param("analysisId", "ana-replay-owner-missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPLAY_EXECUTION_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.reviewAggregateAvailable").value(true))
                .andExpect(jsonPath("$.replaySummaryAvailable").value(false))
                .andExpect(jsonPath("$.sourceHealth").value("BLOCKED"))
                .andExpect(jsonPath("$.notReplayExecution").value(true))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("REPLAY_SUMMARY_OWNER_PATH_MISSING"));
    }

    @Test
    void reviewReplayStatusEndpointDoesNotExposeExecutableReplayCandidatePointOrTradingFields() throws Exception {
        when(reviewService.getStateByAnalysisId("ana-review-safe"))
                .thenReturn(reviewState("ana-review-safe"));
        when(reviewAggregateService.getAggregateSummaryByAnalysisId("ana-review-safe"))
                .thenReturn(Optional.of(reviewAggregateSummary("ana-review-safe", "BTCUSDT", 1, true, true)));

        mockMvc.perform(get("/api/dashboard/review-replay-result-status")
                        .param("analysisId", "ana-review-safe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateRanking").doesNotExist())
                .andExpect(jsonPath("$.candidateScore").doesNotExist())
                .andExpect(jsonPath("$.finalDirection").doesNotExist())
                .andExpect(jsonPath("$.entry").doesNotExist())
                .andExpect(jsonPath("$.stop").doesNotExist())
                .andExpect(jsonPath("$.takeProfit").doesNotExist())
                .andExpect(jsonPath("$.tp").doesNotExist())
                .andExpect(jsonPath("$.riskReward").doesNotExist())
                .andExpect(jsonPath("$.rr").doesNotExist())
                .andExpect(jsonPath("$.positionSize").doesNotExist())
                .andExpect(jsonPath("$.leverage").doesNotExist())
                .andExpect(jsonPath("$.orderAction").doesNotExist())
                .andExpect(jsonPath("$.executionAction").doesNotExist())
                .andExpect(jsonPath("$.replayExecutionAction").doesNotExist())
                .andExpect(jsonPath("$.pushSendState").doesNotExist())
                .andExpect(jsonPath("$.autoTradingAction").doesNotExist());
    }

    @Test
    void dataSourceHealthStatusEndpointReturnsReviewOnlyFailClosedStatus() throws Exception {
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(null);

        mockMvc.perform(get("/api/dashboard/data-source-health-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DATA_SOURCE_HEALTH_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.sourceHealth").value("BLOCKED"))
                .andExpect(jsonPath("$.scopedSources[?(@ == 'MarketQuote')]").exists())
                .andExpect(jsonPath("$.scopedSources[?(@ == 'Evidence / Score')]").exists())
                .andExpect(jsonPath("$.scopedSources[?(@ == 'DecisionResult')]").exists())
                .andExpect(jsonPath("$.scopedSources[?(@ == 'ExecutionPlan / BoundaryCandidate')]").exists())
                .andExpect(jsonPath("$.scopedSources[?(@ == 'Review / Replay')]").exists())
                .andExpect(jsonPath("$.sourceStatuses[?(@.name == 'MarketQuote')]").exists())
                .andExpect(jsonPath("$.sourceStatuses[?(@.name == 'Evidence / Score')]").exists())
                .andExpect(jsonPath("$.sourceStatuses[?(@.name == 'DecisionResult')]").exists())
                .andExpect(jsonPath("$.sourceStatuses[?(@.name == 'ExecutionPlan / BoundaryCandidate')]").exists())
                .andExpect(jsonPath("$.sourceStatuses[?(@.name == 'Review / Replay')]").exists())
                .andExpect(jsonPath("$.missingSources[?(@ == 'MarketQuote')]").exists())
                .andExpect(jsonPath("$.watchOnlySources[?(@ == 'ExecutionPlan / BoundaryCandidate')]").exists())
                .andExpect(jsonPath("$.blockedSources[?(@ == 'Evidence / Score')]").exists())
                .andExpect(jsonPath("$.blockedSources[?(@ == 'Review / Replay')]").exists())
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notReplayExecution").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.watchlistBounded").value(true))
                .andExpect(jsonPath("$.marketQuoteChecked").value(true))
                .andExpect(jsonPath("$.evidenceScoreChecked").value(true))
                .andExpect(jsonPath("$.decisionResultChecked").value(true))
                .andExpect(jsonPath("$.executionPlanBoundaryChecked").value(true))
                .andExpect(jsonPath("$.reviewReplayChecked").value(true))
                .andExpect(jsonPath("$.externalRefreshTriggered").value(false))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void dataSourceHealthStatusEndpointDoesNotExposeExecutableRefreshCandidatePointOrTradingFields() throws Exception {
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(null);

        mockMvc.perform(get("/api/dashboard/data-source-health-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateRanking").doesNotExist())
                .andExpect(jsonPath("$.candidateScore").doesNotExist())
                .andExpect(jsonPath("$.finalDirection").doesNotExist())
                .andExpect(jsonPath("$.entry").doesNotExist())
                .andExpect(jsonPath("$.stop").doesNotExist())
                .andExpect(jsonPath("$.takeProfit").doesNotExist())
                .andExpect(jsonPath("$.tp").doesNotExist())
                .andExpect(jsonPath("$.riskReward").doesNotExist())
                .andExpect(jsonPath("$.rr").doesNotExist())
                .andExpect(jsonPath("$.positionSize").doesNotExist())
                .andExpect(jsonPath("$.leverage").doesNotExist())
                .andExpect(jsonPath("$.orderAction").doesNotExist())
                .andExpect(jsonPath("$.executionAction").doesNotExist())
                .andExpect(jsonPath("$.externalRefreshAction").doesNotExist())
                .andExpect(jsonPath("$.collectorTrigger").doesNotExist())
                .andExpect(jsonPath("$.schedulerTrigger").doesNotExist())
                .andExpect(jsonPath("$.apiClientTrigger").doesNotExist())
                .andExpect(jsonPath("$.pushSendState").doesNotExist())
                .andExpect(jsonPath("$.autoTradingAction").doesNotExist());
    }

    @Test
    void sourceRuntimeDataQualityStatusEndpointReturnsReviewOnlyReadyStatus() throws Exception {
        MockMvc sourceRuntimeMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, decision) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(
                        sourceRuntimeTrace("5m/15m/1h alignment review-only", new BigDecimal("0.91"), false),
                        sourceRuntimeKline("FRESH", "NONE", "Persisted OHLCV window is fresh.", List.of(),
                                new BigDecimal("0.91"), "5m/15m/1h alignment review-only"),
                        null
                )
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-source-runtime-ready");
        decision.setAnalysisId("ana-source-runtime-ready");
        decision.setCreateTime(LocalDateTime.of(2026, 5, 17, 12, 0));
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-source-runtime-ready")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-source-runtime-ready")).thenReturn(Collections.emptyList());

        sourceRuntimeMockMvc.perform(get("/api/dashboard/source-runtime-data-quality-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOURCE_RUNTIME_STATUS_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.analysisId").value("ana-source-runtime-ready"))
                .andExpect(jsonPath("$.sourceTraceAvailable").value(true))
                .andExpect(jsonPath("$.runtimeKlineContextAvailable").value(true))
                .andExpect(jsonPath("$.sourceTraceStatus").value("SOURCE_RUNTIME_STATUS_REVIEW_ONLY_READY"))
                .andExpect(jsonPath("$.runtimeKlineStatus").value("RUNTIME_KLINE_CONTEXT_READY_REVIEW_ONLY"))
                .andExpect(jsonPath("$.persistedOhlcvStatus").value("PERSISTED_OHLCV_READY_REVIEW_ONLY"))
                .andExpect(jsonPath("$.dataQualityStatus").value("DATA_QUALITY_PARTIAL_REVIEW_ONLY"))
                .andExpect(jsonPath("$.multiTimeframeStatus").value("MULTITIMEFRAME_ALIGNMENT_REVIEW_ONLY"))
                .andExpect(jsonPath("$.refreshBoundaryStatus").value("REFRESH_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.generationBoundaryStatus").value("GENERATION_BOUNDARY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.reviewOnly").value(true))
                .andExpect(jsonPath("$.notCandidateSignal").value(true))
                .andExpect(jsonPath("$.notDecisionGeneration").value(true))
                .andExpect(jsonPath("$.notPointSignal").value(true))
                .andExpect(jsonPath("$.notFinalDirection").value(true))
                .andExpect(jsonPath("$.notEntryStopTpRr").value(true))
                .andExpect(jsonPath("$.notTradingSignal").value(true))
                .andExpect(jsonPath("$.notExecutable").value(true))
                .andExpect(jsonPath("$.notSchedulerTrigger").value(true))
                .andExpect(jsonPath("$.notCollectorTrigger").value(true))
                .andExpect(jsonPath("$.notApiClientRefresh").value(true))
                .andExpect(jsonPath("$.notExternalRefresh").value(true))
                .andExpect(jsonPath("$.notSourceBindingGeneration").value(true))
                .andExpect(jsonPath("$.displaySlotsAreCandidatePool").value(false))
                .andExpect(jsonPath("$.failClosed").value(false));
    }

    @Test
    void sourceRuntimeDataQualityStatusEndpointFailsClosedForMissingSourceTraceAndRuntimeKline() throws Exception {
        MockMvc sourceRuntimeMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, decision) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(null, null)
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-source-runtime-missing");
        decision.setAnalysisId("ana-source-runtime-missing");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-source-runtime-missing")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-source-runtime-missing")).thenReturn(Collections.emptyList());

        sourceRuntimeMockMvc.perform(get("/api/dashboard/source-runtime-data-quality-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOURCE_TRACE_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.sourceTraceAvailable").value(false))
                .andExpect(jsonPath("$.runtimeKlineContextAvailable").value(false))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("SOURCE_TRACE_MISSING"));
    }

    @Test
    void sourceRuntimeDataQualityStatusEndpointFailsClosedWhenRuntimeKlineMissing() throws Exception {
        MockMvc sourceRuntimeMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, decision) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(
                        sourceRuntimeTrace("alignment review-only", new BigDecimal("0.88"), false),
                        null,
                        null
                )
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-runtime-kline-missing");
        decision.setAnalysisId("ana-runtime-kline-missing");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-runtime-kline-missing")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-runtime-kline-missing")).thenReturn(Collections.emptyList());

        sourceRuntimeMockMvc.perform(get("/api/dashboard/source-runtime-data-quality-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNTIME_KLINE_CONTEXT_MISSING_FAIL_CLOSED"))
                .andExpect(jsonPath("$.sourceTraceAvailable").value(true))
                .andExpect(jsonPath("$.runtimeKlineContextAvailable").value(false))
                .andExpect(jsonPath("$.failClosed").value(true))
                .andExpect(jsonPath("$.reason").value("RUNTIME_KLINE_CONTEXT_MISSING"));
    }

    @Test
    void sourceRuntimeDataQualityStatusEndpointCoversPersistedOhlcvDataQualityAndMultiTimeframeBoundaryStates() throws Exception {
        MockMvc staleRuntimeMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, decision) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(
                        sourceRuntimeTrace("alignment review-only", new BigDecimal("0.77"), false),
                        sourceRuntimeKline("STALE", "KLINE_STALE", "Latest persisted OHLCV bar is stale.",
                                List.of("klineFreshness"), new BigDecimal("0.77"), "alignment review-only"),
                        null
                )
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-source-runtime-boundary");
        decision.setAnalysisId("ana-source-runtime-boundary");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-source-runtime-boundary")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-source-runtime-boundary")).thenReturn(Collections.emptyList());

        staleRuntimeMockMvc.perform(get("/api/dashboard/source-runtime-data-quality-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PERSISTED_OHLCV_STALE_REVIEW_ONLY"))
                .andExpect(jsonPath("$.persistedOhlcvStatus").value("PERSISTED_OHLCV_STALE_REVIEW_ONLY"))
                .andExpect(jsonPath("$.sourceHealth").value("STALE"))
                .andExpect(jsonPath("$.failClosed").value(true));

        MockMvc dataQualityBlockedMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, row) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(
                        sourceRuntimeTrace("alignment review-only", null, false),
                        sourceRuntimeKline("FRESH", "NONE", "Persisted OHLCV window is fresh.", List.of(),
                                null, "alignment review-only"),
                        null
                )
        )).build();
        dataQualityBlockedMockMvc.perform(get("/api/dashboard/source-runtime-data-quality-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DATA_QUALITY_BLOCKED_FAIL_CLOSED"))
                .andExpect(jsonPath("$.dataQualityAvailable").value(false))
                .andExpect(jsonPath("$.failClosed").value(true));

        MockMvc multiConflictMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, row) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(
                        sourceRuntimeTrace("5m vs 1h CONFLICT review-only", new BigDecimal("0.82"), false),
                        sourceRuntimeKline("FRESH", "NONE", "Persisted OHLCV window is fresh.", List.of(),
                                new BigDecimal("0.82"), "5m vs 1h CONFLICT review-only"),
                        null
                )
        )).build();
        multiConflictMockMvc.perform(get("/api/dashboard/source-runtime-data-quality-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MULTITIMEFRAME_CONFLICT_REVIEW_ONLY"))
                .andExpect(jsonPath("$.multiTimeframeStatus").value("MULTITIMEFRAME_CONFLICT_REVIEW_ONLY"))
                .andExpect(jsonPath("$.failClosed").value(true));
    }

    @Test
    void sourceRuntimeDataQualityStatusEndpointDoesNotExposeExecutableRefreshGenerationCandidatePointOrTradingFields() throws Exception {
        MockMvc sourceRuntimeMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                (symbol, decision) -> new DashboardSourceTraceDetailAdapter.DashboardSourceTraceDetailContext(
                        sourceRuntimeTrace("alignment review-only", new BigDecimal("0.91"), false),
                        sourceRuntimeKline("FRESH", "NONE", "Persisted OHLCV window is fresh.", List.of(),
                                new BigDecimal("0.91"), "alignment review-only"),
                        null
                )
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setDecisionId("dec-source-runtime-safe");
        decision.setAnalysisId("ana-source-runtime-safe");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-source-runtime-safe")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-source-runtime-safe")).thenReturn(Collections.emptyList());

        sourceRuntimeMockMvc.perform(get("/api/dashboard/source-runtime-data-quality-status").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateRanking").doesNotExist())
                .andExpect(jsonPath("$.candidateScore").doesNotExist())
                .andExpect(jsonPath("$.finalDirection").doesNotExist())
                .andExpect(jsonPath("$.entry").doesNotExist())
                .andExpect(jsonPath("$.stop").doesNotExist())
                .andExpect(jsonPath("$.takeProfit").doesNotExist())
                .andExpect(jsonPath("$.tp").doesNotExist())
                .andExpect(jsonPath("$.riskReward").doesNotExist())
                .andExpect(jsonPath("$.rr").doesNotExist())
                .andExpect(jsonPath("$.positionSize").doesNotExist())
                .andExpect(jsonPath("$.leverage").doesNotExist())
                .andExpect(jsonPath("$.orderAction").doesNotExist())
                .andExpect(jsonPath("$.executionAction").doesNotExist())
                .andExpect(jsonPath("$.externalRefreshAction").doesNotExist())
                .andExpect(jsonPath("$.collectorTrigger").doesNotExist())
                .andExpect(jsonPath("$.schedulerTrigger").doesNotExist())
                .andExpect(jsonPath("$.apiClientTrigger").doesNotExist())
                .andExpect(jsonPath("$.sourceBindingGenerationAction").doesNotExist())
                .andExpect(jsonPath("$.pushSendState").doesNotExist())
                .andExpect(jsonPath("$.autoTradingAction").doesNotExist());
    }

    @Test
    void detail_json_exposesRuntimeKlineContextAsSeparateReadOnlyBoundaryWhenAssemblyIsSafe() throws Exception {
        MockMvc runtimeKlineMockMvc = MockMvcBuilders.standaloneSetup(controllerWith(
                new DefaultDashboardSourceTraceDetailAdapter(
                        new DefaultDashboardRuntimeKlineContextAdapter(
                                (symbol, timeframe, requiredWindowSize, maxReadLagMs) -> freshReadiness(List.of(
                                        bar(60_000L, 119_999L, "101.10", "130.00", "100.50", "102.30"),
                                        bar(0L, 59_999L, "100.00", "105.00", "98.00", "101.10")
                                )),
                                new RuntimeKlineContextAssemblyServiceImpl()
                        )
                )
        )).build();
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setSymbol("BTCUSDT");
        decision.setAnalysisId("ana-runtime");
        decision.setTimeframe("1m");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-runtime")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-runtime")).thenReturn(Collections.emptyList());

        runtimeKlineMockMvc.perform(get("/api/dashboard/detail").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runtimeKlineContext.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.runtimeKlineContext.timeframe").value("1m"))
                .andExpect(jsonPath("$.runtimeKlineContext.fallbackStatus").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.latestPrice").value(102.3))
                .andExpect(jsonPath("$.runtimeKlineContext.klineItems.length()").value(2))
                .andExpect(jsonPath("$.runtimeKlineContext.klineItems[0].closePrice").value(102.3))
                .andExpect(jsonPath("$.runtimeKlineContext.klineItems[0].provider").value("LOCAL_FIXTURE"))
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvReadinessStatus").value("FRESH"))
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvStaleReasonCode").value("NONE"))
                .andExpect(jsonPath("$.runtimeKlineContext.persistedOhlcvMissingFields").isEmpty())
                .andExpect(jsonPath("$.runtimeKlineContext.missingFields").isEmpty())
                .andExpect(jsonPath("$.runtimeKlineContext.entryPriceSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.stopPriceSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.tpPriceSources").isEmpty())
                .andExpect(jsonPath("$.runtimeKlineContext.rrSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.liquiditySource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.eventSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.wickSource").value(nullValue()))
                .andExpect(jsonPath("$.runtimeKlineContext.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.runtimeKlineContext.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.sourceTrace.fallbackStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'runtimeKlineContext')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'latestPrice')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'entryPriceSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'entrySourceType')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'entrySourceTimeframe')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'entrySourceReason')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'entrySourceRef')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'stopPriceSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'stopSourceType')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'stopSourceTimeframe')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'stopSourceReason')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'stopSourceRef')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'tpPriceSources')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'tpSourceType')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'tpSourceTimeframe')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'tpSourceReason')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'tpSourceRef')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'rrSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'rrRuleRef')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'liquiditySource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'eventSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.missingFields[?(@ == 'wickSource')]").exists())
                .andExpect(jsonPath("$.sourceTrace.quoteLatestPrice").value(nullValue()))
                .andExpect(jsonPath("$.sourceTrace.entryPriceSource").value(nullValue()))
                .andExpect(jsonPath("$.sourceTrace.stopPriceSource").value(nullValue()))
                .andExpect(jsonPath("$.sourceTrace.tpPriceSources").isEmpty())
                .andExpect(jsonPath("$.sourceTrace.rrSource").value(nullValue()))
                .andExpect(jsonPath("$.sourceTrace.liquiditySource").value(nullValue()))
                .andExpect(jsonPath("$.sourceTrace.eventSource").value(nullValue()))
                .andExpect(jsonPath("$.sourceTrace.wickSource").value(nullValue()))
                .andExpect(jsonPath("$.sourceTrace.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.sourceTrace.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.planBoundaryDisplay.planBoundaryStatus").value("BACKEND_PENDING"))
                .andExpect(jsonPath("$.planBoundaryDisplay.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.planBoundaryDisplay.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.executionPlanDisplay.executionPlanStatus").value("BOUNDARY_PENDING"))
                .andExpect(jsonPath("$.executionPlanDisplay.executionPlanBoundaryAligned").value(false))
                .andExpect(jsonPath("$.executionPlanDisplay.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.executionPlanDisplay.notTradeInstruction").value(true));
    }

    @Test
    void detail_json_exposesMarketEnvironmentMini_fromSnapshot_whenAvailable() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        MarketEnvironmentSnapshotDO snapshot = new MarketEnvironmentSnapshotDO();
        snapshot.setSummary("snapshot summary: BTCUSDT env");
        snapshot.setEnvironmentType("trend_market");
        snapshot.setRiskMode("normal");
        snapshot.setSourceType("BINANCE_24H_HEURISTIC");
        EvidenceBriefVO evidence = new EvidenceBriefVO();
        evidence.setEvidenceType("价格结构");
        evidence.setDescription("突破后回踩确认");
        evidence.setDirection("BULLISH");
        evidence.setSource("SYSTEM_GENERATED");
        decision.setAnalysisId("ana-btc-env");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(marketEnvironmentSnapshotMapper.selectByAnalysisId("ana-btc-env")).thenReturn(snapshot);
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-btc-env")).thenReturn(List.of(evidence));
        when(scoreService.listTopScoreBriefByAnalysisId("ana-btc-env")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/detail").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketEnvironmentMini.summary").value("snapshot summary: BTCUSDT env"))
                .andExpect(jsonPath("$.marketEnvironmentMini.environmentType").value("trend_market"))
                .andExpect(jsonPath("$.marketEnvironmentMini.riskMode").value("normal"))
                .andExpect(jsonPath("$.marketEnvironmentMini.sourceType").value("BINANCE_24H_HEURISTIC"))
                .andExpect(jsonPath("$.evidenceTopItems[0].evidenceType").value("价格结构"))
                .andExpect(jsonPath("$.evidenceTopItems[0].description").value("突破后回踩确认"))
                .andExpect(jsonPath("$.evidenceTopItems[0].direction").value("BULLISH"))
                .andExpect(jsonPath("$.evidenceTopItems[0].source").value("SYSTEM_GENERATED"))
                .andExpect(jsonPath("$.scoreTopItems").isArray())
                .andExpect(jsonPath("$.scoreTopItems").isEmpty());
    }

    @Test
    void detail_json_exposesMarketEnvironmentMini_fromHeuristic_whenSnapshotMissing() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-btc-heuristic");
        MarketEnvironmentVO marketEnvironment = new MarketEnvironmentVO();
        marketEnvironment.setSummary("fallback summary from realtime heuristic");
        marketEnvironment.setEnvironmentType("range_market");
        marketEnvironment.setRiskMode("elevated");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(marketEnvironmentSnapshotMapper.selectByAnalysisId("ana-btc-heuristic")).thenReturn(null);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.of(marketEnvironment));
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-btc-heuristic")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-btc-heuristic")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/detail").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketEnvironmentMini.summary").value("fallback summary from realtime heuristic"))
                .andExpect(jsonPath("$.marketEnvironmentMini.environmentType").value("range_market"))
                .andExpect(jsonPath("$.marketEnvironmentMini.riskMode").value("elevated"))
                .andExpect(jsonPath("$.marketEnvironmentMini.sourceType").value("BINANCE_24H_HEURISTIC"))
                .andExpect(jsonPath("$.evidenceTopItems").isArray())
                .andExpect(jsonPath("$.evidenceTopItems").isEmpty())
                .andExpect(jsonPath("$.scoreTopItems").isArray())
                .andExpect(jsonPath("$.scoreTopItems").isEmpty());
    }

    @Test
    void detail_json_exposesMarketEnvironmentMini_fallback_whenSnapshotAndHeuristicMissing() throws Exception {
        DecisionResultVO decision = newDecisionWithCoreDashboardTruthFields();
        decision.setAnalysisId("ana-btc-missing");
        when(decisionService.getLatestDecisionResultBySymbol("BTCUSDT")).thenReturn(decision);
        when(marketEnvironmentSnapshotMapper.selectByAnalysisId("ana-btc-missing")).thenReturn(null);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("BTCUSDT", null)).thenReturn(Optional.empty());
        when(evidenceService.listTopEvidenceBriefByAnalysisId("ana-btc-missing")).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId("ana-btc-missing")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/detail").param("symbol", "BTCUSDT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketEnvironmentMini").exists())
                .andExpect(jsonPath("$.marketEnvironmentMini.summary").value(nullValue()))
                .andExpect(jsonPath("$.marketEnvironmentMini.environmentType").value(nullValue()))
                .andExpect(jsonPath("$.marketEnvironmentMini.riskMode").value(nullValue()))
                .andExpect(jsonPath("$.marketEnvironmentMini.sourceType").value("PLACEHOLDER_FALLBACK"));
    }

    @Test
    void summary_usesDefaultLimitWhenAbsent() throws Exception {
        stubSummaryData();
        when(decisionService.getLatestDecisionResults(12)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(jsonPath("$.decisions").isArray())
                .andExpect(jsonPath("$.openPositionCount").value(0));

        verify(decisionService).getLatestDecisionResults(12);
        verify(runtimeMetricService).recordDuration(eq("dashboard.summary"), anyLong());
    }

    @Test
    void summary_clampsLimitToGuardrailRange() throws Exception {
        stubSummaryData();
        when(decisionService.getLatestDecisionResults(24)).thenReturn(Collections.emptyList());
        when(decisionService.getLatestDecisionResults(1)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/summary").param("limit", "200"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/summary").param("limit", "0"))
                .andExpect(status().isOk());

        verify(decisionService).getLatestDecisionResults(24);
        verify(decisionService).getLatestDecisionResults(1);
    }

    @Test
    void detail_rejectsBlankSymbolAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/dashboard/detail").param("symbol", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detail_rejectsMissingSymbolParameter() throws Exception {
        mockMvc.perform(get("/api/dashboard/detail"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detail_doesNotExposeDeprecationHeader() throws Exception {
        when(decisionService.getLatestDecisionResultBySymbol("AAPL")).thenReturn(null);
        when(realMarketEnvironmentService.tryBuildFromRealQuote("AAPL", null)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/dashboard/detail").param("symbol", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(jsonPath("$.evidenceTopItems").isArray())
                .andExpect(jsonPath("$.evidenceTopItems").isEmpty())
                .andExpect(jsonPath("$.scoreTopItems").isArray())
                .andExpect(jsonPath("$.scoreTopItems").isEmpty());
    }

    @Test
    void refresh_keepsLegacyContractAndMetrics() throws Exception {
        stubSummaryData();
        when(decisionService.getLatestDecisionResults(12)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/refresh"))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Link", "</api/dashboard/summary>; rel=\"alternate\"; title=\"replacement\""))
                .andExpect(jsonPath("$.decisions").isArray())
                .andExpect(jsonPath("$.alerts").isArray())
                .andExpect(jsonPath("$.systemHealth").isMap());

        verify(decisionService).getLatestDecisionResults(12);
        verify(runtimeMetricService).recordDuration(eq("dashboard.refresh"), anyLong());
    }

    private void stubSummaryData() {
        when(decisionService.getLightSystemStatus()).thenReturn(null);
        when(decisionService.countOpenPositions()).thenReturn(0);
        when(systemHealthService.getSystemHealth()).thenReturn(Collections.emptyMap());
        when(monitorService.getRecentAlerts(3)).thenReturn(Collections.emptyList());
    }

    private String normalizedInternalPushPreviewDisplay() throws Exception {
        return internalPushPreviewDisplay()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private String internalPushPreviewDisplay() throws Exception {
        String html = Files.readString(DASHBOARD_TEMPLATE);
        int sectionStart = html.indexOf(INTERNAL_PUSH_PREVIEW_START);
        assertThat(sectionStart).isNotNegative();

        int sectionEnd = html.indexOf(SECTION_END, sectionStart);
        assertThat(sectionEnd).isNotNegative();

        return html.substring(sectionStart, sectionEnd + SECTION_END.length());
    }

    private static DecisionResultVO newDecisionWithCoreDashboardTruthFields() {
        DecisionResultVO row = new DecisionResultVO();
        row.setSymbol("BTCUSDT");
        row.setMarketBiasHierarchy("H1>H4>D1");
        row.setIsWorthOpening(Boolean.TRUE);
        row.setRecommendedAction("OPEN_LONG");
        row.setAiConflictLevel("L2");
        row.setAiConflictScore(42);
        row.setAiPlanMode("AGGRESSIVE");
        row.setConfusedScore(3);
        return row;
    }

    private static TmAccountRiskSnapshotDO accountRiskSnapshot(
            String analysisId,
            String symbol,
            Boolean riskAllowed,
            String riskLevel,
            BigDecimal positionExposure,
            BigDecimal maxAllowedExposure) {
        TmAccountRiskSnapshotDO snapshot = new TmAccountRiskSnapshotDO();
        snapshot.setId(9001L);
        snapshot.setAnalysisId(analysisId);
        snapshot.setSymbol(symbol);
        snapshot.setRiskAllowed(riskAllowed);
        snapshot.setRiskLevelSnapshot(riskLevel);
        snapshot.setRiskReasonCode("ACCOUNT_RISK_REVIEW_ONLY");
        snapshot.setRiskReasonText("Account risk snapshot is review-only evidence.");
        snapshot.setPositionExposure(positionExposure);
        snapshot.setMaxAllowedExposure(maxAllowedExposure);
        snapshot.setSnapshotSource("tm_account_risk_snapshot");
        snapshot.setSnapshotVersion(1);
        snapshot.setSourceNote("read-only account exposure snapshot");
        snapshot.setTraceId("trace-" + analysisId);
        snapshot.setCreateTime(LocalDateTime.of(2026, 6, 12, 12, 0));
        return snapshot;
    }

    private static HotResetEventDO hotResetEvent(String analysisId, boolean complete) {
        HotResetEventDO event = new HotResetEventDO();
        event.setEventId("hot-reset-" + analysisId);
        event.setAnalysisId(analysisId);
        event.setTraceId("trace-" + analysisId);
        event.setSymbol("BTCUSDT");
        event.setTriggerType(complete ? "MACRO_EVENT_IMPACT" : null);
        event.setTriggerValue("event-impact-read-only");
        event.setDecisionId("dec-" + analysisId);
        event.setDecisionState("REVIEW_ONLY");
        event.setConfusedScoreSnapshot(3);
        event.setMultiTimeframeAlignedSnapshot(false);
        event.setTriggerReasonCode(complete ? "EVENT_IMPACT_SOURCE_READ" : null);
        event.setTriggerReasonText("Persisted Hot Reset event is read-only evidence.");
        event.setEventVersion(1);
        event.setEventTime(complete ? LocalDateTime.of(2026, 6, 12, 12, 30) : null);
        event.setPreState("BEFORE_REVIEW_ONLY");
        event.setPostState("AFTER_REVIEW_ONLY");
        event.setCreateTime(LocalDateTime.of(2026, 6, 12, 12, 31));
        return event;
    }

    private static ReviewStateVO reviewState(String analysisId) {
        ReviewStateVO state = new ReviewStateVO();
        state.setReviewId("review-" + analysisId);
        state.setAnalysisId(analysisId);
        state.setErrorType("NO_ERROR");
        state.setActualOutcome("manual review recorded");
        state.setAdjustmentSuggestion("keep review-only observation");
        state.setCreateTime(LocalDateTime.of(2026, 5, 17, 12, 0));
        state.setUpdateTime(LocalDateTime.of(2026, 5, 17, 12, 30));
        return state;
    }

    private static ReviewAggregateSummaryVO reviewAggregateSummary(
            String analysisId,
            String symbol,
            int replaySummaryCount,
            boolean includeClosure,
            boolean includeReplayMeta) {
        ReviewAggregateSummaryVO summary = new ReviewAggregateSummaryVO();
        ReviewAggregateVO.ReviewRunSummary run = new ReviewAggregateVO.ReviewRunSummary();
        run.setAnalysisId(analysisId);
        run.setSymbol(symbol);
        run.setStatus("DONE");
        run.setTraceId("trace-" + analysisId);
        summary.setRun(run);
        if (includeClosure) {
            ReviewAggregateVO.ReviewClosureSummary closure = new ReviewAggregateVO.ReviewClosureSummary();
            closure.setStageLabel("review-only closure");
            closure.setDecisionConclusion("review-only conclusion available");
            summary.setReviewClosure(closure);
        }
        if (includeReplayMeta) {
            ReviewAggregateSummaryVO.DetailSectionMeta meta = new ReviewAggregateSummaryVO.DetailSectionMeta();
            meta.setSection("pushRecheck");
            meta.setTotal(replaySummaryCount);
            meta.setRecommendedLimit(20);
            summary.setDetailSections(List.of(meta));
        } else {
            summary.setDetailSections(Collections.emptyList());
        }
        return summary;
    }

    private static SourceTraceDTO completeExecutionPlanBoundarySourceTrace() {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setDecisionId("dec-execution-plan-ready");
        sourceTrace.setAnalysisId("ana-execution-plan-ready");
        sourceTrace.setTimeframe("1h");
        sourceTrace.setMissingFields(Collections.emptyList());
        sourceTrace.setEntryPriceSource(new BigDecimal("68000.00"));
        sourceTrace.setEntrySourceType("REVIEW_ONLY_SOURCE");
        sourceTrace.setEntrySourceTimeframe("1h");
        sourceTrace.setEntrySourceReason("REVIEW_ONLY_BOUNDARY_SOURCE");
        sourceTrace.setEntrySourceRef("source-trace-fixture");
        sourceTrace.setStopPriceSource(new BigDecimal("66000.00"));
        sourceTrace.setStopSourceType("REVIEW_ONLY_SOURCE");
        sourceTrace.setStopSourceTimeframe("1h");
        sourceTrace.setStopSourceReason("REVIEW_ONLY_BOUNDARY_SOURCE");
        sourceTrace.setStopSourceRef("source-trace-fixture");
        sourceTrace.setTpPriceSources(List.of(new BigDecimal("70000.00")));
        sourceTrace.setTpSourceType("REVIEW_ONLY_SOURCE");
        sourceTrace.setTpSourceTimeframe("1h");
        sourceTrace.setTpSourceReason("REVIEW_ONLY_BOUNDARY_SOURCE");
        sourceTrace.setTpSourceRef("source-trace-fixture");
        sourceTrace.setRrSource(new BigDecimal("2.00"));
        sourceTrace.setRrRuleRef("review-only-rr-rule");
        sourceTrace.setLiquiditySource("review-only-liquidity-source");
        sourceTrace.setMultiTimeframeSource("review-only-mtf-source");
        sourceTrace.setEventSource("review-only-event-source");
        sourceTrace.setWickSource("review-only-wick-source");
        sourceTrace.setManualReviewRequired(true);
        sourceTrace.setNotTradeInstruction(true);
        return sourceTrace;
    }

    private static SourceTraceDTO sourceRuntimeTrace(String multiTimeframeSource,
                                                     BigDecimal dataQualityScore,
                                                     boolean partial) {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setSymbol("BTCUSDT");
        sourceTrace.setDecisionId("dec-source-runtime");
        sourceTrace.setAnalysisId("ana-source-runtime");
        sourceTrace.setTimeframe("1m");
        sourceTrace.setSourceOwner("DashboardSourceTraceDetailAdapter");
        sourceTrace.setSourceRef("dashboard-detail-owner-path");
        sourceTrace.setRuntimeKlineContextStatus("READY_REVIEW_ONLY");
        sourceTrace.setRuntimeKlineContextSource("DefaultDashboardRuntimeKlineContextAdapter");
        sourceTrace.setRuntimeKlineReadinessStatus("FRESH");
        sourceTrace.setRuntimeKlineStaleReasonCode("NONE");
        sourceTrace.setRuntimeKlineStaleReasonText("Persisted OHLCV window is fresh.");
        sourceTrace.setDataQualityScore(dataQualityScore);
        sourceTrace.setDataQualityScoreSource(dataQualityScore != null ? "runtime-kline-readiness-metadata" : null);
        sourceTrace.setMultiTimeframeSource(multiTimeframeSource);
        sourceTrace.setMissingFields(partial ? List.of("runtimeKlineReadinessStatus") : Collections.emptyList());
        sourceTrace.setManualReviewRequired(true);
        sourceTrace.setNotTradeInstruction(true);
        return sourceTrace;
    }

    private static RuntimeKlineContextDTO sourceRuntimeKline(String readiness,
                                                            String staleReasonCode,
                                                            String staleReasonText,
                                                            List<String> missingFields,
                                                            BigDecimal dataQualityScore,
                                                            String multiTimeframeSource) {
        RuntimeKlineContextDTO runtimeKline = new RuntimeKlineContextDTO();
        runtimeKline.setSymbol("BTCUSDT");
        runtimeKline.setTimeframe("1m");
        runtimeKline.setLatestPrice(new BigDecimal("102.30"));
        runtimeKline.setDataQualityScore(dataQualityScore);
        runtimeKline.setPersistedOhlcvReadinessStatus(readiness);
        runtimeKline.setPersistedOhlcvStaleReasonCode(staleReasonCode);
        runtimeKline.setPersistedOhlcvStaleReasonText(staleReasonText);
        runtimeKline.setPersistedOhlcvMissingFields(missingFields);
        runtimeKline.setMultiTimeframeSource(multiTimeframeSource);
        runtimeKline.setMissingFields(Collections.emptyList());
        runtimeKline.setManualReviewRequired(true);
        runtimeKline.setNotTradeInstruction(true);
        return runtimeKline;
    }

    private static DashboardDetailResponseVO.PlanBoundaryDisplayVO readyPlanBoundaryDisplay() {
        DashboardDetailResponseVO.PlanBoundaryDisplayVO display = new DashboardDetailResponseVO.PlanBoundaryDisplayVO();
        display.setPlanBoundaryStatus("VALID");
        display.setPlanBoundaryStatusLabel("只读边界可复核");
        display.setSourceTraceStatus("COMPLETE");
        display.setBackendConnectionStatus("READY");
        display.setIncompleteReasons(Collections.emptyList());
        display.setBlockingReasons(Collections.emptyList());
        display.setManualReviewRequired(true);
        display.setNotTradeInstruction(true);
        return display;
    }

    private static DashboardDetailResponseVO.ExecutionPlanDisplayVO readyExecutionPlanDisplay() {
        DashboardDetailResponseVO.ExecutionPlanDisplayVO display = new DashboardDetailResponseVO.ExecutionPlanDisplayVO();
        display.setExecutionPlanStatus("READY_REVIEW_ONLY");
        display.setExecutionPlanStatusLabel("只读执行计划状态可复核");
        display.setExecutionPlanBoundaryAligned(true);
        display.setPlanBoundaryStatus("VALID");
        display.setNotExecutableReason("REVIEW_ONLY_NOT_EXECUTABLE");
        display.setIncompleteReasons(Collections.emptyList());
        display.setManualReviewRequired(true);
        display.setNotTradeInstruction(true);
        return display;
    }

    private static DashboardDetailResponseVO.RiskActionGuardDisplayVO manualReviewRiskActionGuardDisplay() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = new DashboardDetailResponseVO.RiskActionGuardDisplayVO();
        display.setRiskActionGuardStatus("MANUAL_REVIEW_REQUIRED");
        display.setRiskActionGuardStatusLabel("只读人工复核");
        display.setRiskActionBlockingReason("MANUAL_REVIEW_REQUIRED");
        display.setOpportunityPushAllowed(false);
        display.setReverseTradeAllowed(false);
        display.setNewPositionAllowed(false);
        display.setMarketOrderExitAllowed(false);
        display.setManualRiskReviewRequired(true);
        display.setNotTradeInstruction(true);
        return display;
    }

    private static DashboardDetailResponseVO.RiskActionGuardDisplayVO actionFlagRiskActionGuardDisplay() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = manualReviewRiskActionGuardDisplay();
        display.setOpportunityPushAllowed(true);
        return display;
    }

    private static DashboardDetailResponseVO.RiskActionGuardDisplayVO unsafeActionWordingRiskActionGuardDisplay() {
        DashboardDetailResponseVO.RiskActionGuardDisplayVO display = manualReviewRiskActionGuardDisplay();
        display.setRiskActionAdvice("execute close reverse open");
        return display;
    }

    private static DashboardDetailResponseVO.PaperObservationDisplayVO readyPaperObservationDisplay() {
        DashboardDetailResponseVO.PaperObservationDisplayVO display = new DashboardDetailResponseVO.PaperObservationDisplayVO();
        display.setPaperObservationStatus("MANUAL_REVIEW_REQUIRED");
        display.setPaperObservationStatusLabel("需要人工复核");
        display.setPaperObservationAvailable(false);
        display.setManualReviewEntryAvailable(false);
        display.setLinkedPaperObservationCount(2);
        display.setLinkedReviewCount(3);
        display.setMissedOpportunityFlag(false);
        display.setReviewSummary("AVAILABLE_REVIEW_ONLY");
        display.setNotRealPosition(true);
        display.setNotTradeInstruction(true);
        display.setManualReviewRequired(true);
        display.setBackendConnectionStatus("READY_REVIEW_ONLY");
        return display;
    }

    private static DashboardDetailResponseVO.PaperObservationDisplayVO unsafePaperObservationDisplay() {
        DashboardDetailResponseVO.PaperObservationDisplayVO display = readyPaperObservationDisplay();
        display.setPaperObservationAvailable(true);
        display.setManualReviewEntryAvailable(true);
        return display;
    }

    private static MonitorAlertDO monitorAlert(String symbol,
                                               String alertType,
                                               String alertLevel,
                                               String status,
                                               String cooldownUntil,
                                               String suppressReason) {
        MonitorAlertDO alert = new MonitorAlertDO();
        alert.setId("alert-" + alertType + "-" + status);
        alert.setAssetSymbol(symbol);
        alert.setAlertType(alertType);
        alert.setAlertLevel(alertLevel);
        alert.setAlertMessage("review-only alert fixture");
        alert.setStatus(status);
        alert.setCooldownUntil(cooldownUntil);
        alert.setSuppressReason(suppressReason);
        alert.setTraceId("trace-alert");
        alert.setRuleVersion("v1");
        return alert;
    }

    private static PersistedOhlcvReadinessResult readiness(
            PersistedOhlcvReadinessStatus status,
            PersistedOhlcvStaleReasonCode reasonCode,
            String reasonText,
            List<String> missingFields
    ) {
        PersistedOhlcvReadinessResult result = new PersistedOhlcvReadinessResult();
        result.setStatus(status);
        result.setStaleReasonCode(reasonCode);
        result.setStaleReasonText(reasonText);
        result.setMissingFields(missingFields);
        result.setManualReviewRequired(true);
        result.setNotTradeInstruction(true);
        return result;
    }

    private static PersistedOhlcvReadinessResult freshReadiness(List<PersistedOhlcvBarDO> bars) {
        PersistedOhlcvReadinessResult result = readiness(
                PersistedOhlcvReadinessStatus.FRESH,
                PersistedOhlcvStaleReasonCode.NONE,
                "Persisted OHLCV window is fresh.",
                List.of()
        );
        result.setSymbol("BTCUSDT");
        result.setTimeframe("1m");
        result.setRequiredWindowSize(2);
        result.setBars(bars);
        result.setLatestCloseTimeMs(bars.stream()
                .filter(bar -> bar.getCloseTimeMs() != null)
                .map(PersistedOhlcvBarDO::getCloseTimeMs)
                .max(Long::compareTo)
                .orElse(null));
        result.setLatestIngestedAt(LocalDateTime.of(2026, 5, 17, 10, 0));
        return result;
    }

    private static PersistedOhlcvBarDO bar(
            Long openTimeMs,
            Long closeTimeMs,
            String openPrice,
            String highPrice,
            String lowPrice,
            String closePrice
    ) {
        PersistedOhlcvBarDO bar = new PersistedOhlcvBarDO();
        bar.setSymbol("BTCUSDT");
        bar.setTimeframe("1m");
        bar.setOpenTimeMs(openTimeMs);
        bar.setCloseTimeMs(closeTimeMs);
        bar.setOpenPrice(new BigDecimal(openPrice));
        bar.setHighPrice(new BigDecimal(highPrice));
        bar.setLowPrice(new BigDecimal(lowPrice));
        bar.setClosePrice(new BigDecimal(closePrice));
        bar.setVolume(new BigDecimal("123.45"));
        bar.setClosed(true);
        bar.setProvider("LOCAL_FIXTURE");
        bar.setProviderMarketType("USDT_PERP");
        bar.setSourceEndpoint("persisted-ohlcv-fixture");
        bar.setSourceBatchId("batch-1");
        bar.setSourceTraceId("trace-1");
        bar.setSourceVersion(1);
        bar.setIngestedAt(LocalDateTime.of(2026, 5, 17, 10, 0));
        bar.setQualityStatus("OK");
        bar.setIsDeleted(0);
        return bar;
    }
}
