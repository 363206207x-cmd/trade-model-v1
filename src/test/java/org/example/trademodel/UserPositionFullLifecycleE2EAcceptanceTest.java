package org.example.trademodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.example.trademodel.dto.req.CloseUserPositionReq;
import org.example.trademodel.dto.req.CreateUserPositionReq;
import org.example.trademodel.dto.req.WriteReviewResultReq;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorSourceContract;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.RecordPositionMonitorLogCommand;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.PositionMonitorService;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.PositionMonitorScheduler;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.service.impl.PositionMonitorServiceImpl;
import org.example.trademodel.service.impl.UserPositionServiceImpl;
import org.example.trademodel.testsupport.FrozenFinalExecutionPlanTestFixture;
import org.example.trademodel.userpositionreview.DefaultUserPositionReviewAdapter;
import org.example.trademodel.userpositionreview.UserPositionReviewAdapter;
import org.example.trademodel.userpositionreview.UserPositionReviewFeedbackReq;
import org.example.trademodel.userpositionreview.UserPositionReviewFeedbackResultDTO;
import org.example.trademodel.userpositionreview.UserPositionReviewSummaryDTO;
import org.example.trademodel.vo.ReviewStateVO;
import org.example.trademodel.vo.UserPositionVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserPositionFullLifecycleE2EAcceptanceTest {

    private static final String PLAN_ID = "plan-p3-e2e-user-position";
    private static final String ANALYSIS_ID = "analysis-p3-e2e-user-position";
    private static final Long USER_ID = 17L;

    @Test
    void manualOpenQueuesOneInitialBaseMonitoringRequestWithoutTradingAction() {
        UserPositionMapper mapper = mock(UserPositionMapper.class);
        ExecutionPlanMapper planMapper = mock(ExecutionPlanMapper.class);
        PositionMonitorScheduler monitorScheduler = mock(PositionMonitorScheduler.class);
        doAnswer(invocation -> {
            UserPositionDO row = invocation.getArgument(0);
            row.setId(3101L);
            return 1;
        }).when(mapper).insert(any(UserPositionDO.class));
        UserPositionServiceImpl service = new UserPositionServiceImpl(mapper, planMapper, monitorScheduler);
        CreateUserPositionReq request = openPositionRequest();
        request.setSourceType("MANUAL_INDEPENDENT");
        request.setFinalPlanId(null);
        request.setSourceRefId(null);

        UserPositionVO opened = service.manualOpenForUser(USER_ID, request);

        assertThat(opened.getId()).isEqualTo(3101L);
        assertThat(opened.isNotTradeInstruction()).isTrue();
        assertThat(opened.isNotAutoTrading()).isTrue();
        assertThat(opened.isNotOrderExecution()).isTrue();
        verify(monitorScheduler).requestInitialMonitor(3101L, USER_ID);
        verify(mapper, never()).manualCloseByIdAndUserId(
                anyLong(), anyLong(), any(LocalDateTime.class), any(BigDecimal.class),
                anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void activeMonitorAndClosedReviewUseSameSourceResolution()
            throws Exception {
        UserPositionMapper userPositionMapper = mock(UserPositionMapper.class);
        Map<Long, UserPositionDO> positions = new LinkedHashMap<>();
        wireUserPositionMapper(userPositionMapper, positions);

        ExecutionPlanMapper executionPlanMapper = mock(ExecutionPlanMapper.class);
        AnalysisRunMapper analysisRunMapper = mock(AnalysisRunMapper.class);
        ExecutionPlanDO executionPlan = executionPlan();
        when(executionPlanMapper.selectByPlanId(PLAN_ID)).thenReturn(executionPlan);
        when(executionPlanMapper.selectValidatedFinalByPlanIdAndSymbol(PLAN_ID, "BTCUSDT"))
                .thenReturn(executionPlan);
        ExecutionPlanDO latestSiblingPlanB = executionPlan();
        latestSiblingPlanB.setPlanId("plan-latest-sibling-B");
        latestSiblingPlanB.setEntryZone("B-entry");
        latestSiblingPlanB.setStopLoss("B-stop");
        latestSiblingPlanB.setTakeProfitRules("B-tp");
        lenient().when(executionPlanMapper.selectLatestByAnalysisId(ANALYSIS_ID))
                .thenReturn(latestSiblingPlanB);
        AnalysisRunDO analysisRun = new AnalysisRunDO();
        analysisRun.setAnalysisId(ANALYSIS_ID);
        analysisRun.setSymbol("BTCUSDT");
        analysisRun.setTraceId("trace-" + ANALYSIS_ID);
        analysisRun.setTimeframe("5m");
        analysisRun.setDataQualityScore(90);
        analysisRun.setStatus("SUCCESS");
        analysisRun.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(analysisRunMapper.selectById(ANALYSIS_ID)).thenReturn(analysisRun);
        when(analysisRunMapper.countEvidenceByAnalysisId(ANALYSIS_ID)).thenReturn(3);
        when(analysisRunMapper.countScoresByAnalysisId(ANALYSIS_ID)).thenReturn(8);

        MarketQuoteClient marketQuoteClient = mock(MarketQuoteClient.class);
        when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.of(quote("100")));

        EvidenceItemMapper evidenceItemMapper = mock(EvidenceItemMapper.class);
        ScoreItemMapper scoreItemMapper = mock(ScoreItemMapper.class);
        DecisionResultMapper decisionResultMapper = mock(DecisionResultMapper.class);
        org.example.trademodel.vo.DecisionResultVO currentDecision =
                new org.example.trademodel.vo.DecisionResultVO();
        currentDecision.setAnalysisId(ANALYSIS_ID);
        currentDecision.setSymbol("BTCUSDT");
        currentDecision.setTimeframe("5m");
        currentDecision.setMarketBiasHierarchy("BULLISH");
        currentDecision.setMultiTfConvergence("ALIGNED");
        currentDecision.setDataQualityScore(90);
        currentDecision.setCreateTime(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(decisionResultMapper.findLatestDecisionResultBySymbolJoined("BTCUSDT"))
                .thenReturn(currentDecision);
        InMemoryPositionMonitorLogService monitorLogService = new InMemoryPositionMonitorLogService();
        UserPositionRiskAdapter riskAdapter = mock(UserPositionRiskAdapter.class);
        when(riskAdapter.currentRiskForUser(USER_ID)).thenReturn(allowedRisk());

        UserPositionServiceImpl userPositionService =
                new UserPositionServiceImpl(userPositionMapper, executionPlanMapper);
        PositionMonitorService positionMonitorService = new PositionMonitorServiceImpl(
                userPositionMapper,
                org.example.trademodel.testsupport.MarketPriceSnapshotTestSupport.snapshotService(marketQuoteClient),
                riskAdapter,
                executionPlanMapper,
                monitorLogService,
                evidenceItemMapper,
                scoreItemMapper,
                decisionResultMapper,
                new ObjectMapper(),
                analysisRunMapper,
                null);
        ReviewService reviewService = mock(ReviewService.class);
        ArgumentCaptor<WriteReviewResultReq> reviewCaptor = ArgumentCaptor.forClass(WriteReviewResultReq.class);
        when(reviewService.saveOrUpdateForUserPosition(anyLong(), anyLong(), reviewCaptor.capture()))
                .thenAnswer(invocation -> reviewState(invocation.getArgument(2)));
        UserPositionReviewAdapter reviewAdapter =
                new DefaultUserPositionReviewAdapter(userPositionMapper, executionPlanMapper, analysisRunMapper,
                        monitorLogService, reviewService);

        UserPositionVO opened = userPositionService.manualOpenForUser(USER_ID, openPositionRequest());

        assertThat(opened.getId()).isNotNull();
        assertThat(opened.getAssetSymbol()).isEqualTo("BTCUSDT");
        assertThat(opened.getStatus()).isEqualTo("OPEN");
        assertThat(opened.getSourceType()).isEqualTo("SYSTEM_PLAN_POSITION");
        assertThat(opened.isNotTradeInstruction()).isTrue();
        assertThat(opened.isNotAutoTrading()).isTrue();
        assertThat(opened.isNotOrderExecution()).isTrue();
        assertThat(userPositionService.listOpenPositionsForUser(USER_ID))
                .extracting(UserPositionVO::getId)
                .containsExactly(opened.getId());

        PositionMonitorResultDTO monitorResult = positionMonitorService.monitorUserPositionForUser(opened.getId(), USER_ID);

        assertThat(monitorResult.getPositionId()).isEqualTo(opened.getId());
        assertThat(monitorResult.getEntryLogicStatus()).isEqualTo("STILL_VALID");
        assertThat(monitorResult.getMonitorConclusion()).isEqualTo("LOGIC_VALID");
        assertThat(monitorResult.getSuggestedAction()).isEqualTo("CONTINUE_HOLD");
        assertThat(monitorResult.getMonitorLogId()).isNotNull();
        assertThat(monitorResult.isNotTradeInstruction()).isTrue();
        assertThat(monitorResult.isNotAutoTrading()).isTrue();
        assertThat(monitorResult.isNotOrderExecution()).isTrue();
        assertThat(monitorResult.isNotPositionMutation()).isTrue();
        verify(userPositionMapper, never()).manualCloseByIdAndUserId(
                anyLong(), anyLong(), any(LocalDateTime.class), any(BigDecimal.class),
                anyString(), anyString(), any(LocalDateTime.class));

        List<PositionMonitorLogDTO> monitorLogs = monitorLogService
                .listAllByPositionIdForUserReview(USER_ID, opened.getId());
        assertThat(monitorLogs).hasSize(1);
        assertThat(monitorLogs.get(0).getEntryLogicStatus()).isEqualTo("STILL_VALID");
        assertThat(monitorLogs.get(0).getMonitorConclusion()).isEqualTo("LOGIC_VALID");
        assertThat(monitorLogs.get(0).getSuggestedAction()).isEqualTo("CONTINUE_HOLD");
        assertThat(monitorLogs.get(0).getAnalysisId()).isEqualTo(ANALYSIS_ID);

        UserPositionVO closed = userPositionService.manualCloseForUser(opened.getId(), USER_ID, closePositionRequest());

        assertThat(closed.getStatus()).isEqualTo("CLOSED");
        assertThat(closed.getClosePrice()).isEqualByComparingTo("112");
        assertThat(closed.isNotTradeInstruction()).isTrue();
        assertThat(closed.isNotAutoTrading()).isTrue();
        assertThat(closed.isNotOrderExecution()).isTrue();
        assertThat(userPositionService.listOpenPositionsForUser(USER_ID)).isEmpty();

        PositionMonitorBatchResultDTO monitorBatchAfterClose = positionMonitorService
                .monitorClaimedOpenPositionsForSystem();
        assertThat(monitorBatchAfterClose.getTotalCount()).isZero();
        assertThat(monitorBatchAfterClose.getSuccessCount()).isZero();
        assertThat(monitorBatchAfterClose.getFailureCount()).isZero();

        UserPositionReviewSummaryDTO reviewSummary = reviewAdapter.buildSummaryForUser(USER_ID, opened.getId());

        assertThat(reviewSummary.getReviewStatus()).isEqualTo("REVIEW_SUMMARY_READY");
        assertThat(reviewSummary.getPositionStatus()).isEqualTo("CLOSED");
        assertThat(reviewSummary.getExecutionPlanId()).isEqualTo(PLAN_ID);
        assertThat(reviewSummary.getExecutionPlanId()).isNotEqualTo("plan-latest-sibling-B");
        assertThat(reviewSummary.getEntryZone()).isEqualTo("110");
        assertThat(reviewSummary.getEntryZone()).isNotEqualTo("B-entry");
        assertThat(reviewSummary.getPlanContextStatus()).isEqualTo("PLAN_CONTEXT_FOUND");
        assertThat(reviewSummary.getMonitorLogCount()).isEqualTo(1);
        assertThat(reviewSummary.getExecutionDeviationStatus()).isEqualTo("DEVIATED");
        assertThat(reviewSummary.getExecutionDeviationReasons()).contains("ENTRY_DEVIATED");
        assertThat(reviewSummary.isNotTradeInstruction()).isTrue();
        assertThat(reviewSummary.isNotExecutable()).isTrue();
        assertThat(reviewSummary.isNotAutoTrading()).isTrue();
        assertThat(reviewSummary.isNotOrderExecution()).isTrue();
        assertThat(reviewSummary.isNotUserPositionMutation()).isTrue();

        UserPositionReviewFeedbackResultDTO feedbackResult = reviewAdapter
                .recordFeedbackForUser(USER_ID, opened.getId(), feedbackRequest());

        assertThat(feedbackResult.isRuleFeedbackRecorded()).isTrue();
        assertThat(feedbackResult.getPositionId()).isEqualTo(opened.getId());
        assertThat(feedbackResult.getAnalysisId()).isEqualTo(ANALYSIS_ID);
        assertThat(feedbackResult.getReviewId()).isEqualTo("review-p3-e2e-user-position");
        assertThat(feedbackResult.isNotRuleAutoApply()).isTrue();
        assertThat(feedbackResult.isNotExecutable()).isTrue();
        assertThat(feedbackResult.isNotAutoTrading()).isTrue();
        assertThat(reviewCaptor.getValue().getAnalysisId()).isEqualTo(ANALYSIS_ID);
        assertThat(reviewCaptor.getValue().getErrorType()).isEqualTo("PLAN_EXECUTION_MISMATCH");

        verify(userPositionMapper, times(1)).insert(any(UserPositionDO.class));
        verify(executionPlanMapper, never()).selectLatestByAnalysisId(anyString());
        assertNoForbiddenExecutableFields(
                UserPositionVO.class,
                PositionMonitorResultDTO.class,
                UserPositionReviewSummaryDTO.class,
                UserPositionReviewFeedbackResultDTO.class);
    }

    @Test
    void exactPlanASurvivesMonitorCloseAndReviewWithoutPlanBSubstitution() throws Exception {
        activeMonitorAndClosedReviewUseSameSourceResolution();
    }

    private static void wireUserPositionMapper(UserPositionMapper mapper, Map<Long, UserPositionDO> positions) {
        AtomicLong ids = new AtomicLong(1000);
        doAnswer(invocation -> {
            UserPositionDO row = invocation.getArgument(0);
            row.setId(ids.incrementAndGet());
            positions.put(row.getId(), row);
            return 1;
        }).when(mapper).insert(any(UserPositionDO.class));
        when(mapper.selectByIdAndUserId(anyLong(), anyLong())).thenAnswer(invocation -> {
            UserPositionDO row = positions.get(invocation.getArgument(0));
            return row != null && invocation.getArgument(1).equals(row.getUserId()) ? row : null;
        });
        when(mapper.selectClaimedByIdForSystem(anyLong()))
                .thenAnswer(invocation -> positions.get(invocation.getArgument(0)));
        when(mapper.listOpenByUserId(anyLong())).thenAnswer(invocation -> positions.values().stream()
                .filter(row -> invocation.getArgument(0).equals(row.getUserId()))
                .filter(row -> "OPEN".equals(row.getStatus()))
                .sorted(Comparator.comparing(UserPositionDO::getId).reversed())
                .collect(Collectors.toList()));
        when(mapper.listClaimedOpenForSystemMonitoring()).thenAnswer(invocation -> positions.values().stream()
                .filter(row -> row.getUserId() != null && "OPEN".equals(row.getStatus()))
                .sorted(Comparator.comparing(UserPositionDO::getId).reversed())
                .collect(Collectors.toList()));
        when(mapper.manualCloseByIdAndUserId(anyLong(), anyLong(), any(LocalDateTime.class),
                any(BigDecimal.class), anyString(), anyString(), any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    Long positionId = invocation.getArgument(0);
                    Long userId = invocation.getArgument(1);
                    UserPositionDO row = positions.get(positionId);
                    if (row == null || !userId.equals(row.getUserId()) || !"OPEN".equals(row.getStatus())) {
                        return 0;
                    }
                    row.setClosedAt(invocation.getArgument(2));
                    row.setClosePrice(invocation.getArgument(3));
                    row.setCloseReason(invocation.getArgument(4));
                    row.setCloseSubmissionId(invocation.getArgument(5));
                    row.setUpdatedAt(invocation.getArgument(6));
                    row.setStatus("CLOSED");
                    return 1;
                });
    }

    private static CreateUserPositionReq openPositionRequest() {
        CreateUserPositionReq req = new CreateUserPositionReq();
        req.setSubmissionId("acceptance-open:user-position");
        req.setAssetSymbol("btcusdt");
        req.setSide("LONG");
        req.setEntryPrice(new BigDecimal("100"));
        req.setQuantity(new BigDecimal("0.25"));
        req.setLeverage(new BigDecimal("2"));
        req.setStopLoss(new BigDecimal("90"));
        req.setTakeProfit(new BigDecimal("120"));
        req.setOpenedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        req.setSourceType("SYSTEM_PLAN_POSITION");
        req.setFinalPlanId(PLAN_ID);
        req.setSourceRefId(PositionMonitorSourceContract.executionPlanReference(PLAN_ID));
        return req;
    }

    private static CloseUserPositionReq closePositionRequest() {
        CloseUserPositionReq req = new CloseUserPositionReq();
        req.setSubmissionId("acceptance-close:user-position");
        req.setClosePrice(new BigDecimal("112"));
        req.setCloseReason("manual acceptance close");
        req.setClosedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
        return req;
    }

    private static ExecutionPlanDO executionPlan() {
        ExecutionPlanDO plan = FrozenFinalExecutionPlanTestFixture.complete(
                PLAN_ID, ANALYSIS_ID, LocalDateTime.now(ZoneOffset.UTC));
        plan.setRecommendedAction("OBSERVE_ONLY");
        plan.setEntryZone("110");
        plan.setStopLoss("90");
        plan.setTakeProfitRules("120");
        return plan;
    }

    private static MarketQuoteSnapshot quote(String price) {
        MarketQuoteSnapshot quote = new MarketQuoteSnapshot();
        quote.setProvider("acceptance-test");
        quote.setSymbolNormalized("BTCUSDT");
        quote.setLastPrice(new BigDecimal(price));
        quote.setHighPrice(new BigDecimal("101"));
        quote.setLowPrice(new BigDecimal("99"));
        quote.setPriceChangePercent24h(BigDecimal.ZERO);
        quote.setFetchedAtEpochMillis(System.currentTimeMillis());
        return quote;
    }

    private static UserPositionReviewFeedbackReq feedbackRequest() {
        UserPositionReviewFeedbackReq req = new UserPositionReviewFeedbackReq();
        req.setErrorType("PLAN_EXECUTION_MISMATCH");
        req.setActualOutcome("WIN_WITH_ENTRY_DEVIATION");
        req.setAdjustmentSuggestion("Review entry-zone tolerance after manual lifecycle acceptance.");
        return req;
    }

    private static ReviewStateVO reviewState(WriteReviewResultReq req) {
        ReviewStateVO vo = new ReviewStateVO();
        vo.setReviewId("review-p3-e2e-user-position");
        vo.setAnalysisId(req.getAnalysisId());
        vo.setErrorType(req.getErrorType());
        vo.setActualOutcome(req.getActualOutcome());
        vo.setAdjustmentSuggestion(req.getAdjustmentSuggestion());
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }

    private static UserPositionRiskResult allowedRisk() {
        UserPositionRiskResult result = new UserPositionRiskResult();
        result.setRiskStatus("RISK_ALLOWED");
        result.setRiskLevel("LOW");
        result.setRiskBlocked(false);
        result.setIncludedPositionCount(1);
        result.setOpenPositionCount(1);
        result.setReasonCodes(List.of("P3_E2E_ACCEPTANCE_RISK_ALLOWED"));
        return result;
    }

    private static void assertNoForbiddenExecutableFields(Class<?>... types) throws Exception {
        Set<String> forbiddenFields = Set.of(
                "orderAction",
                "executionAction",
                "autoTradingAction",
                "buyAction",
                "sellAction",
                "executeAction",
                "tradeAllowed",
                "orderAllowed",
                "openAllowed",
                "closeAllowed",
                "autoOpenAllowed",
                "autoCloseAllowed",
                "pushRecheckCreatedUserPosition");
        for (Class<?> type : types) {
            Set<String> fieldNames = Arrays.stream(Introspector.getBeanInfo(type).getPropertyDescriptors())
                    .map(PropertyDescriptor::getName)
                    .collect(Collectors.toSet());
            assertThat(fieldNames).doesNotContainAnyElementsOf(forbiddenFields);
        }
    }

    private static final class InMemoryPositionMonitorLogService implements PositionMonitorLogService {

        private final AtomicLong ids = new AtomicLong(5000);
        private final List<PositionMonitorLogDTO> logs = new ArrayList<>();

        @Override
        public PositionMonitorLogDTO recordMonitorRunForUser(
                Long userId, RecordPositionMonitorLogCommand command) {
            if (!USER_ID.equals(userId)) {
                throw new AssertionError("unexpected owner");
            }
            return record(command);
        }

        @Override
        public PositionMonitorLogDTO recordMonitorRunForSystem(RecordPositionMonitorLogCommand command) {
            return record(command);
        }

        private PositionMonitorLogDTO record(RecordPositionMonitorLogCommand command) {
            PositionMonitorLogDTO dto = new PositionMonitorLogDTO();
            dto.setLogId(ids.incrementAndGet());
            dto.setPositionId(command.getPositionId());
            dto.setAnalysisId(command.getAnalysisId());
            dto.setExecutionPlanId(command.getExecutionPlanId());
            dto.setCurrentPrice(command.getCurrentPrice());
            dto.setMarkPriceSource(command.getMarkPriceSource());
            dto.setLogicStatus(command.getLogicStatus());
            dto.setEntryLogicStatus(command.getEntryLogicStatus());
            dto.setMonitorConclusion(command.getMonitorConclusion());
            dto.setReversalStatus(command.getReversalStatus());
            dto.setRiskChangeReason(command.getRiskChangeReason());
            dto.setRiskLevel(command.getRiskLevel());
            dto.setSuggestedAction(command.getSuggestedAction());
            dto.setMonitorSourceStatus(command.getMonitorSourceStatus());
            dto.setObservedAt(command.getObservedAt());
            dto.setFreshUntil(command.getFreshUntil());
            dto.setReason(command.getReason());
            dto.setEvidenceSnapshot(command.getEvidenceSnapshot());
            dto.setScoreSnapshot(command.getScoreSnapshot());
            dto.setDecisionSnapshot(command.getDecisionSnapshot());
            dto.setRiskSnapshot(command.getRiskSnapshot());
            dto.setTraceId(command.getTraceId());
            dto.setNotTradeInstruction(true);
            dto.setNotExecutable(true);
            dto.setNotAutoTrading(true);
            dto.setNotOrderExecution(true);
            dto.setNotAutoClose(true);
            dto.setNotAutoReverse(true);
            dto.setNotPositionMutation(true);
            dto.setCreatedAt(LocalDateTime.now());
            logs.add(dto);
            return dto;
        }

        @Override
        public PositionMonitorLogDTO findByIdForSystem(Long logId) {
            return logs.stream().filter(log -> log.getLogId().equals(logId)).findFirst().orElse(null);
        }

        @Override
        public List<PositionMonitorLogDTO> listByPositionIdForUser(
                Long userId, Long positionId, Integer limit) {
            if (!USER_ID.equals(userId)) {
                return List.of();
            }
            return listByPositionIdForSystem(positionId, limit);
        }

        @Override
        public List<PositionMonitorLogDTO> listByPositionIdForSystem(Long positionId, Integer limit) {
            return logs.stream()
                    .filter(log -> log.getPositionId().equals(positionId))
                    .sorted(Comparator.comparing(PositionMonitorLogDTO::getCreatedAt).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        @Override
        public List<PositionMonitorLogDTO> listAllByPositionIdForUserReview(Long userId, Long positionId) {
            if (!USER_ID.equals(userId)) {
                return List.of();
            }
            return listAllByPositionIdForSystemReview(positionId);
        }

        @Override
        public List<PositionMonitorLogDTO> listAllByPositionIdForSystemReview(Long positionId) {
            return logs.stream()
                    .filter(log -> log.getPositionId().equals(positionId))
                    .sorted(Comparator.comparing(PositionMonitorLogDTO::getCreatedAt))
                    .collect(Collectors.toList());
        }

        @Override
        public List<PositionMonitorLogDTO> listByAnalysisIdForSystem(String analysisId, Integer limit) {
            return logs.stream()
                    .filter(log -> log.getAnalysisId().equals(analysisId))
                    .sorted(Comparator.comparing(PositionMonitorLogDTO::getCreatedAt).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        }
    }
}
