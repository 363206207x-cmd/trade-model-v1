package org.example.trademodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
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
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;
import org.example.trademodel.positionmonitor.PositionMonitorResultDTO;
import org.example.trademodel.positionmonitorlog.PositionMonitorLogDTO;
import org.example.trademodel.positionmonitorlog.RecordPositionMonitorLogCommand;
import org.example.trademodel.risk.UserPositionRiskAdapter;
import org.example.trademodel.risk.UserPositionRiskResult;
import org.example.trademodel.service.PositionMonitorService;
import org.example.trademodel.service.PositionMonitorLogService;
import org.example.trademodel.service.ReviewService;
import org.example.trademodel.service.impl.PositionMonitorServiceImpl;
import org.example.trademodel.service.impl.UserPositionServiceImpl;
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

    @Test
    void manualUserPositionFlowsThroughMonitorCloseReviewAndRuleFeedbackWithoutExecutableSurfaces()
            throws Exception {
        UserPositionMapper userPositionMapper = mock(UserPositionMapper.class);
        Map<Long, UserPositionDO> positions = new LinkedHashMap<>();
        wireUserPositionMapper(userPositionMapper, positions);

        ExecutionPlanMapper executionPlanMapper = mock(ExecutionPlanMapper.class);
        ExecutionPlanDO executionPlan = executionPlan();
        when(executionPlanMapper.selectByPlanId(PLAN_ID)).thenReturn(executionPlan);
        when(executionPlanMapper.selectLatestByAnalysisId(ANALYSIS_ID)).thenReturn(executionPlan);

        MarketQuoteClient marketQuoteClient = mock(MarketQuoteClient.class);
        when(marketQuoteClient.fetch24hTicker("BTCUSDT")).thenReturn(Optional.of(quote("100")));

        EvidenceItemMapper evidenceItemMapper = mock(EvidenceItemMapper.class);
        ScoreItemMapper scoreItemMapper = mock(ScoreItemMapper.class);
        DecisionResultMapper decisionResultMapper = mock(DecisionResultMapper.class);
        InMemoryPositionMonitorLogService monitorLogService = new InMemoryPositionMonitorLogService();
        UserPositionRiskAdapter riskAdapter = mock(UserPositionRiskAdapter.class);
        when(riskAdapter.currentRisk()).thenReturn(allowedRisk());

        UserPositionServiceImpl userPositionService = new UserPositionServiceImpl(userPositionMapper);
        PositionMonitorService positionMonitorService = new PositionMonitorServiceImpl(
                userPositionMapper,
                marketQuoteClient,
                riskAdapter,
                executionPlanMapper,
                monitorLogService,
                evidenceItemMapper,
                scoreItemMapper,
                decisionResultMapper,
                new ObjectMapper(),
                null);
        ReviewService reviewService = mock(ReviewService.class);
        ArgumentCaptor<WriteReviewResultReq> reviewCaptor = ArgumentCaptor.forClass(WriteReviewResultReq.class);
        when(reviewService.saveOrUpdate(reviewCaptor.capture())).thenAnswer(invocation -> reviewState(invocation.getArgument(0)));
        UserPositionReviewAdapter reviewAdapter =
                new DefaultUserPositionReviewAdapter(userPositionMapper, executionPlanMapper, monitorLogService, reviewService);

        UserPositionVO opened = userPositionService.manualOpen(openPositionRequest());

        assertThat(opened.getId()).isNotNull();
        assertThat(opened.getAssetSymbol()).isEqualTo("BTCUSDT");
        assertThat(opened.getStatus()).isEqualTo("OPEN");
        assertThat(opened.getSourceType()).isEqualTo("MANUAL");
        assertThat(opened.isNotTradeInstruction()).isTrue();
        assertThat(opened.isNotAutoTrading()).isTrue();
        assertThat(opened.isNotOrderExecution()).isTrue();
        assertThat(userPositionService.listOpenPositions())
                .extracting(UserPositionVO::getId)
                .containsExactly(opened.getId());

        PositionMonitorResultDTO monitorResult = positionMonitorService.monitorUserPosition(opened.getId());

        assertThat(monitorResult.getPositionId()).isEqualTo(opened.getId());
        assertThat(monitorResult.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(monitorResult.getSuggestedAction()).isEqualTo("HOLD");
        assertThat(monitorResult.getMonitorLogId()).isNotNull();
        assertThat(monitorResult.isNotTradeInstruction()).isTrue();
        assertThat(monitorResult.isNotAutoTrading()).isTrue();
        assertThat(monitorResult.isNotOrderExecution()).isTrue();
        assertThat(monitorResult.isNotPositionMutation()).isTrue();
        verify(userPositionMapper, never()).manualClose(anyLong(), any(LocalDateTime.class), any(BigDecimal.class),
                anyString(), any(LocalDateTime.class));

        List<PositionMonitorLogDTO> monitorLogs = monitorLogService.listAllByPositionIdForReview(opened.getId());
        assertThat(monitorLogs).hasSize(1);
        assertThat(monitorLogs.get(0).getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(monitorLogs.get(0).getSuggestedAction()).isEqualTo("HOLD");
        assertThat(monitorLogs.get(0).getAnalysisId()).isEqualTo(ANALYSIS_ID);

        UserPositionVO closed = userPositionService.manualClose(opened.getId(), closePositionRequest());

        assertThat(closed.getStatus()).isEqualTo("CLOSED");
        assertThat(closed.getClosePrice()).isEqualByComparingTo("112");
        assertThat(closed.isNotTradeInstruction()).isTrue();
        assertThat(closed.isNotAutoTrading()).isTrue();
        assertThat(closed.isNotOrderExecution()).isTrue();
        assertThat(userPositionService.listOpenPositions()).isEmpty();

        PositionMonitorBatchResultDTO monitorBatchAfterClose = positionMonitorService.monitorOpenUserPositions();
        assertThat(monitorBatchAfterClose.getTotalCount()).isZero();
        assertThat(monitorBatchAfterClose.getSuccessCount()).isZero();
        assertThat(monitorBatchAfterClose.getFailureCount()).isZero();

        UserPositionReviewSummaryDTO reviewSummary = reviewAdapter.buildSummary(opened.getId());

        assertThat(reviewSummary.getReviewStatus()).isEqualTo("REVIEW_SUMMARY_READY");
        assertThat(reviewSummary.getPositionStatus()).isEqualTo("CLOSED");
        assertThat(reviewSummary.getExecutionPlanId()).isEqualTo(PLAN_ID);
        assertThat(reviewSummary.getPlanContextStatus()).isEqualTo("PLAN_CONTEXT_FOUND");
        assertThat(reviewSummary.getMonitorLogCount()).isEqualTo(1);
        assertThat(reviewSummary.getExecutionDeviationStatus()).isEqualTo("DEVIATED");
        assertThat(reviewSummary.getExecutionDeviationReasons()).contains("ENTRY_DEVIATED");
        assertThat(reviewSummary.isNotTradeInstruction()).isTrue();
        assertThat(reviewSummary.isNotExecutable()).isTrue();
        assertThat(reviewSummary.isNotAutoTrading()).isTrue();
        assertThat(reviewSummary.isNotOrderExecution()).isTrue();
        assertThat(reviewSummary.isNotUserPositionMutation()).isTrue();

        UserPositionReviewFeedbackResultDTO feedbackResult = reviewAdapter.recordFeedback(opened.getId(), feedbackRequest());

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
        assertNoForbiddenExecutableFields(
                UserPositionVO.class,
                PositionMonitorResultDTO.class,
                UserPositionReviewSummaryDTO.class,
                UserPositionReviewFeedbackResultDTO.class);
    }

    private static void wireUserPositionMapper(UserPositionMapper mapper, Map<Long, UserPositionDO> positions) {
        AtomicLong ids = new AtomicLong(1000);
        doAnswer(invocation -> {
            UserPositionDO row = invocation.getArgument(0);
            row.setId(ids.incrementAndGet());
            positions.put(row.getId(), row);
            return 1;
        }).when(mapper).insert(any(UserPositionDO.class));
        when(mapper.selectById(anyLong())).thenAnswer(invocation -> positions.get(invocation.getArgument(0)));
        when(mapper.listOpenPositions()).thenAnswer(invocation -> positions.values().stream()
                .filter(row -> Set.of("OPEN", "PARTIALLY_CLOSED").contains(row.getStatus()))
                .sorted(Comparator.comparing(UserPositionDO::getId).reversed())
                .collect(Collectors.toList()));
        when(mapper.manualClose(anyLong(), any(LocalDateTime.class), any(BigDecimal.class), anyString(),
                any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    Long positionId = invocation.getArgument(0);
                    UserPositionDO row = positions.get(positionId);
                    if (row == null || !"OPEN".equals(row.getStatus())) {
                        return 0;
                    }
                    row.setClosedAt(invocation.getArgument(1));
                    row.setClosePrice(invocation.getArgument(2));
                    row.setCloseReason(invocation.getArgument(3));
                    row.setUpdatedAt(invocation.getArgument(4));
                    row.setStatus("CLOSED");
                    return 1;
                });
    }

    private static CreateUserPositionReq openPositionRequest() {
        CreateUserPositionReq req = new CreateUserPositionReq();
        req.setAssetSymbol("btcusdt");
        req.setSide("LONG");
        req.setEntryPrice(new BigDecimal("100"));
        req.setQuantity(new BigDecimal("0.25"));
        req.setLeverage(new BigDecimal("2"));
        req.setStopLoss(new BigDecimal("90"));
        req.setTakeProfit(new BigDecimal("120"));
        req.setSourceType("MANUAL");
        req.setSourceRefId(PLAN_ID);
        return req;
    }

    private static CloseUserPositionReq closePositionRequest() {
        CloseUserPositionReq req = new CloseUserPositionReq();
        req.setClosePrice(new BigDecimal("112"));
        req.setCloseReason("manual acceptance close");
        return req;
    }

    private static ExecutionPlanDO executionPlan() {
        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId(PLAN_ID);
        plan.setAnalysisId(ANALYSIS_ID);
        plan.setExecutionPlanStatus("VALID");
        plan.setSourceGateStatus("VALID");
        plan.setSourceGateComplete(Boolean.TRUE);
        plan.setRecommendedAction("OBSERVE_ONLY");
        plan.setEntryZone("110");
        plan.setStopLoss("90");
        plan.setTakeProfitRules("120");
        plan.setNotTradeInstruction(Boolean.TRUE);
        plan.setNotExecutable(Boolean.TRUE);
        plan.setNotAutoTrading(Boolean.TRUE);
        plan.setNotOrderExecution(Boolean.TRUE);
        plan.setNotUserPositionCreation(Boolean.TRUE);
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
        public PositionMonitorLogDTO recordMonitorRun(RecordPositionMonitorLogCommand command) {
            PositionMonitorLogDTO dto = new PositionMonitorLogDTO();
            dto.setLogId(ids.incrementAndGet());
            dto.setPositionId(command.getPositionId());
            dto.setAnalysisId(command.getAnalysisId());
            dto.setExecutionPlanId(command.getExecutionPlanId());
            dto.setCurrentPrice(command.getCurrentPrice());
            dto.setLogicStatus(command.getLogicStatus());
            dto.setRiskLevel(command.getRiskLevel());
            dto.setSuggestedAction(command.getSuggestedAction());
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
        public PositionMonitorLogDTO findById(Long logId) {
            return logs.stream().filter(log -> log.getLogId().equals(logId)).findFirst().orElse(null);
        }

        @Override
        public List<PositionMonitorLogDTO> listByPositionId(Long positionId, Integer limit) {
            return logs.stream()
                    .filter(log -> log.getPositionId().equals(positionId))
                    .sorted(Comparator.comparing(PositionMonitorLogDTO::getCreatedAt).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        @Override
        public List<PositionMonitorLogDTO> listAllByPositionIdForReview(Long positionId) {
            return logs.stream()
                    .filter(log -> log.getPositionId().equals(positionId))
                    .sorted(Comparator.comparing(PositionMonitorLogDTO::getCreatedAt))
                    .collect(Collectors.toList());
        }

        @Override
        public List<PositionMonitorLogDTO> listByAnalysisId(String analysisId, Integer limit) {
            return logs.stream()
                    .filter(log -> log.getAnalysisId().equals(analysisId))
                    .sorted(Comparator.comparing(PositionMonitorLogDTO::getCreatedAt).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        }
    }
}
