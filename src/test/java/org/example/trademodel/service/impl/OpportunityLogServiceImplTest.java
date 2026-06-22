package org.example.trademodel.service.impl;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.OpportunityLogMapper;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatsDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class OpportunityLogServiceImplTest {
    @Mock
    private OpportunityLogMapper opportunityLogMapper;
    @Mock
    private UserPositionMapper userPositionMapper;
    @Mock
    private DecisionResultMapper decisionResultMapper;
    @Mock
    private ExecutionPlanMapper executionPlanMapper;
    @Mock
    private PushSnapshotMapper pushSnapshotMapper;
    @Mock
    private PushRecheckLogMapper pushRecheckLogMapper;
    @Mock
    private AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    @Mock
    private PersistedOhlcvBarMapper persistedOhlcvBarMapper;

    private OpportunityLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OpportunityLogServiceImpl(opportunityLogMapper, userPositionMapper, decisionResultMapper,
                executionPlanMapper, pushSnapshotMapper, pushRecheckLogMapper, accountRiskSnapshotMapper,
                persistedOhlcvBarMapper);
    }

    @Test
    void recordFromAuthoritativeAnalysis_createsPendingCandidateAndIsIdempotent() {
        when(pushSnapshotMapper.listByAnalysisId("ana-1")).thenReturn(List.of(push(9L, "ana-1")));
        ArgumentCaptor<OpportunityLogDO> captor = ArgumentCaptor.forClass(OpportunityLogDO.class);

        OpportunityLogDTO dto = service.recordFromAuthoritativeAnalysis(run("ana-1"), decision("BULLISH", true),
                validPlan("plan-1", "ana-1", "100", "120", "90"), 3L, "trace-1");

        verify(opportunityLogMapper).insert(captor.capture());
        OpportunityLogDO row = captor.getValue();
        assertThat(row.getOpportunityKey()).isEqualTo("ana-1:dec-1");
        assertThat(row.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.PENDING_EVALUATION);
        assertThat(row.getDirection()).isEqualTo("LONG");
        assertThat(row.getEntryReference()).isEqualByComparingTo("100");
        assertThat(row.getTargetPrice()).isEqualByComparingTo("120");
        assertThat(row.getInvalidationPrice()).isEqualByComparingTo("90");
        assertThat(row.getPushPresent()).isTrue();
        assertThat(dto.getReviewOnly()).isTrue();
        assertThat(dto.getNotTradeInstruction()).isTrue();
    }

    @Test
    void recordFromAuthoritativeAnalysis_sourceIncompleteDoesNotCreateFinalStatus() {
        ArgumentCaptor<OpportunityLogDO> captor = ArgumentCaptor.forClass(OpportunityLogDO.class);

        service.recordFromAuthoritativeAnalysis(run("ana-2"), decision("BULLISH", true),
                validPlan("plan-2", "ana-2", "100", "120", null), null, "trace-2");

        verify(opportunityLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getLifecycleStatus()).isEqualTo(OpportunityLogStatus.SOURCE_INCOMPLETE);
        assertThat(captor.getValue().getOpportunityStatus()).isNull();
        assertThat(captor.getValue().getReasonCodes()).contains("INVALIDATION_PRICE_NOT_COMPUTABLE");
    }

    @Test
    void evaluate_longTargetFirstWithExactPlanUserPosition_returnsExecutedValidAndMfeMae() {
        OpportunityLogDO row = pendingLong();
        UserPositionDO position = userPosition(77L, "plan-1");
        when(opportunityLogMapper.selectByOpportunityId("opp-1")).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefId("plan-1")).thenReturn(List.of(position));
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "121", "98", "118")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunity("opp-1", row.getAnchorTime().plusHours(2));

        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.EXECUTED_VALID);
        assertThat(dto.getUserPositionId()).isEqualTo(77L);
        assertThat(dto.getHitOrder()).isEqualTo(OpportunityLogStatus.TARGET_FIRST);
        assertThat(dto.getMfePrice()).isEqualByComparingTo("121");
        assertThat(dto.getMfeRatio()).isEqualByComparingTo("0.2100000000");
        assertThat(dto.getMaePrice()).isEqualByComparingTo("98");
        assertThat(dto.getMaeRatio()).isEqualByComparingTo("0.0200000000");
    }

    @Test
    void evaluate_shortInvalidationFirstWithoutUserPosition_returnsMissedInvalid() {
        OpportunityLogDO row = pendingShort();
        when(opportunityLogMapper.selectByOpportunityId("opp-short")).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefId("plan-short")).thenReturn(List.of());
        when(userPositionMapper.listByExactSourceRefId("ana-short")).thenReturn(List.of());
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("ETHUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "112", "99", "108")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunity("opp-short", row.getAnchorTime().plusHours(2));

        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.MISSED_INVALID);
        assertThat(dto.getHitOrder()).isEqualTo(OpportunityLogStatus.INVALIDATION_FIRST);
    }

    @Test
    void evaluate_sameBarTargetAndInvalidation_isAmbiguousWithoutFinalStatus() {
        OpportunityLogDO row = pendingLong();
        when(opportunityLogMapper.selectByOpportunityId("opp-1")).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefId("plan-1")).thenReturn(List.of());
        when(userPositionMapper.listByExactSourceRefId("ana-1")).thenReturn(List.of());
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "122", "89", "101")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunity("opp-1", row.getAnchorTime().plusHours(2));

        assertThat(dto.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.AMBIGUOUS_MARKET_PATH);
        assertThat(dto.getOpportunityStatus()).isNull();
        assertThat(dto.getHitOrder()).isEqualTo(OpportunityLogStatus.AMBIGUOUS_SAME_BAR);
    }

    @Test
    void evaluate_noBars_returnsMarketPathUnavailableWithoutFinalStatus() {
        OpportunityLogDO row = pendingLong();
        when(opportunityLogMapper.selectByOpportunityId("opp-1")).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefId("plan-1")).thenReturn(List.of());
        when(userPositionMapper.listByExactSourceRefId("ana-1")).thenReturn(List.of());
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of());
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunity("opp-1", row.getAnchorTime().plusHours(2));

        assertThat(dto.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.MARKET_PATH_UNAVAILABLE);
        assertThat(dto.getOpportunityStatus()).isNull();
    }

    @Test
    void evaluate_targetFirstWithRiskBlockedBeforeTarget_returnsBlockedByRiskValid() {
        OpportunityLogDO row = pendingLong();
        when(opportunityLogMapper.selectByOpportunityId("opp-1")).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefId("plan-1")).thenReturn(List.of());
        when(userPositionMapper.listByExactSourceRefId("ana-1")).thenReturn(List.of());
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "121", "99", "120")));
        when(accountRiskSnapshotMapper.selectLatestByAnalysisId("ana-1")).thenReturn(riskBlocked(row.getAnchorTime().plusMinutes(5)));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunity("opp-1", row.getAnchorTime().plusHours(2));

        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.BLOCKED_BY_RISK_VALID);
        assertThat(dto.getRiskBlockedEvidence()).isTrue();
    }

    @Test
    void evaluate_targetFirstWithPushAndNoExecution_returnsPushedNotFilledValid() {
        OpportunityLogDO row = pendingLong();
        row.setPushPresent(true);
        row.setPushId(9L);
        when(opportunityLogMapper.selectByOpportunityId("opp-1")).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefId("plan-1")).thenReturn(List.of());
        when(userPositionMapper.listByExactSourceRefId("ana-1")).thenReturn(List.of());
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "121", "99", "120")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunity("opp-1", row.getAnchorTime().plusHours(2));

        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.PUSHED_NOT_FILLED_VALID);
    }

    @Test
    void evaluate_multipleExactUserPositions_requiresReview() {
        OpportunityLogDO row = pendingLong();
        when(opportunityLogMapper.selectByOpportunityId("opp-1")).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefId("plan-1"))
                .thenReturn(List.of(userPosition(1L, "plan-1"), userPosition(2L, "plan-1")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunity("opp-1", row.getAnchorTime().plusHours(2));

        assertThat(dto.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.REVIEW_REQUIRED);
        assertThat(dto.getOpportunityStatus()).isNull();
        assertThat(dto.getReasonCodes()).contains("MULTIPLE_LINKED_USER_POSITIONS");
        verify(persistedOhlcvBarMapper, never()).selectClosedBarsBetween(any(), any(), anyLong(), anyLong(), anyInt());
    }

    @Test
    void evaluate_resolvedRecordIsImmutableAndDeduplicated() {
        OpportunityLogDO row = pendingLong();
        row.setLifecycleStatus(OpportunityLogStatus.RESOLVED);
        row.setOpportunityStatus(OpportunityLogStatus.MISSED_VALID);
        row.setMfeRatio(new BigDecimal("0.10"));
        when(opportunityLogMapper.selectByOpportunityId("opp-1")).thenReturn(row);

        OpportunityLogDTO dto = service.evaluateOpportunity("opp-1", row.getAnchorTime().plusHours(2));

        assertThat(dto.getDeduplicated()).isTrue();
        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
        assertThat(dto.getMfeRatio()).isEqualByComparingTo("0.10");
        verify(opportunityLogMapper, never()).updateEvaluation(any());
    }

    @Test
    void getStats_countsReviewOnlyOpportunityOutcomes() {
        OpportunityLogDO valid = pendingLong();
        valid.setLifecycleStatus(OpportunityLogStatus.RESOLVED);
        valid.setOpportunityStatus(OpportunityLogStatus.MISSED_VALID);
        valid.setHitOrder(OpportunityLogStatus.TARGET_FIRST);
        valid.setMfeRatio(new BigDecimal("0.20"));
        valid.setMaeRatio(new BigDecimal("0.04"));
        OpportunityLogDO invalid = pendingLong();
        invalid.setOpportunityId("opp-2");
        invalid.setLifecycleStatus(OpportunityLogStatus.RESOLVED);
        invalid.setOpportunityStatus(OpportunityLogStatus.MISSED_INVALID);
        invalid.setHitOrder(OpportunityLogStatus.INVALIDATION_FIRST);
        invalid.setMfeRatio(new BigDecimal("0.10"));
        invalid.setMaeRatio(new BigDecimal("0.06"));
        when(opportunityLogMapper.query(eq(null), eq(null), eq(null), eq("BTCUSDT"), eq(null), eq(null), any(), any(), eq(200)))
                .thenReturn(List.of(valid, invalid));

        OpportunityLogStatsDTO stats = service.getStats("BTCUSDT", LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertThat(stats.getTotalCount()).isEqualTo(2);
        assertThat(stats.getResolvedCount()).isEqualTo(2);
        assertThat(stats.getMissedValidCount()).isEqualTo(1);
        assertThat(stats.getMissedInvalidCount()).isEqualTo(1);
        assertThat(stats.getValidRate()).isEqualByComparingTo("0.50000000");
        assertThat(stats.getReviewOnly()).isTrue();
        assertThat(stats.getNotExecutable()).isTrue();
    }

    @Test
    void dtoDoesNotExposeExecutableActionFields() {
        List<String> names = java.util.Arrays.stream(OpportunityLogDTO.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .toList();
        assertThat(names).doesNotContain("openAction", "closeAction", "reduceAction", "reverseAction",
                "orderAction", "executionAction", "autoTradingAction", "pushSendAction",
                "externalChannelAction", "executablePayload", "providerPayload");
    }

    private static OpportunityLogDO pendingLong() {
        OpportunityLogDO row = new OpportunityLogDO();
        row.setOpportunityId("opp-1");
        row.setOpportunityKey("ana-1:dec-1");
        row.setAnalysisId("ana-1");
        row.setDecisionId("dec-1");
        row.setExecutionPlanId("plan-1");
        row.setSymbol("BTCUSDT");
        row.setTimeframe("1h");
        row.setDirection("LONG");
        row.setLifecycleStatus(OpportunityLogStatus.PENDING_EVALUATION);
        row.setAnchorTime(LocalDateTime.of(2026, 6, 23, 10, 0));
        row.setEntryReference(new BigDecimal("100"));
        row.setTargetPrice(new BigDecimal("120"));
        row.setInvalidationPrice(new BigDecimal("90"));
        row.setPushPresent(false);
        row.setUserPositionPresent(false);
        return row;
    }

    private static OpportunityLogDO pendingShort() {
        OpportunityLogDO row = new OpportunityLogDO();
        row.setOpportunityId("opp-short");
        row.setOpportunityKey("ana-short:dec-short");
        row.setAnalysisId("ana-short");
        row.setDecisionId("dec-short");
        row.setExecutionPlanId("plan-short");
        row.setSymbol("ETHUSDT");
        row.setTimeframe("1h");
        row.setDirection("SHORT");
        row.setLifecycleStatus(OpportunityLogStatus.PENDING_EVALUATION);
        row.setAnchorTime(LocalDateTime.of(2026, 6, 23, 10, 0));
        row.setEntryReference(new BigDecimal("100"));
        row.setTargetPrice(new BigDecimal("80"));
        row.setInvalidationPrice(new BigDecimal("110"));
        return row;
    }

    private static AnalysisRunDO run(String analysisId) {
        AnalysisRunDO row = new AnalysisRunDO();
        row.setAnalysisId(analysisId);
        row.setSymbol("BTCUSDT");
        row.setTimeframe("1h");
        row.setAnalysisTime(LocalDateTime.of(2026, 6, 23, 10, 0));
        row.setTraceId("trace-" + analysisId);
        return row;
    }

    private static DecisionResult decision(String bias, boolean worthOpening) {
        DecisionResult row = new DecisionResult();
        row.setDecisionId("dec-1");
        row.setAnalysisId("ana-1");
        row.setSymbol("BTCUSDT");
        row.setMarketBiasHierarchy(bias);
        row.setIsWorthOpening(worthOpening);
        row.setCreateTime(LocalDateTime.of(2026, 6, 23, 10, 1));
        return row;
    }

    private static ExecutionPlanDO validPlan(String planId, String analysisId, String entry, String target, String invalidation) {
        ExecutionPlanDO row = new ExecutionPlanDO();
        row.setPlanId(planId);
        row.setAnalysisId(analysisId);
        row.setExecutionPlanStatus("VALID");
        row.setSourceGateStatus("VALID");
        row.setSourceGateComplete(true);
        row.setNeedsRevalidation(false);
        row.setEntryZone(entry);
        row.setTakeProfitRules(target);
        row.setStopLoss(invalidation);
        row.setCreateTime(LocalDateTime.of(2026, 6, 23, 10, 2));
        return row;
    }

    private static TmPushSnapshotDO push(Long pushId, String analysisId) {
        TmPushSnapshotDO row = new TmPushSnapshotDO();
        row.setPushId(pushId);
        row.setAnalysisId(analysisId);
        row.setTriggerPrice(new BigDecimal("100"));
        return row;
    }

    private static UserPositionDO userPosition(Long id, String sourceRefId) {
        UserPositionDO row = new UserPositionDO();
        row.setId(id);
        row.setSourceType("MANUAL");
        row.setSourceRefId(sourceRefId);
        row.setStatus("CLOSED");
        row.setClosedAt(LocalDateTime.of(2026, 6, 23, 11, 30));
        return row;
    }

    private static TmAccountRiskSnapshotDO riskBlocked(LocalDateTime createTime) {
        TmAccountRiskSnapshotDO row = new TmAccountRiskSnapshotDO();
        row.setRiskAllowed(false);
        row.setRiskReasonCode("ACCOUNT_RISK_BLOCKED");
        row.setCreateTime(createTime);
        return row;
    }

    private static PersistedOhlcvBarDO bar(int hourOffset, String open, String high, String low, String close) {
        LocalDateTime openTime = LocalDateTime.of(2026, 6, 23, 10, 0).plusHours(hourOffset);
        PersistedOhlcvBarDO row = new PersistedOhlcvBarDO();
        row.setOpenTimeMs(openTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        row.setCloseTimeMs(openTime.plusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        row.setOpenPrice(new BigDecimal(open));
        row.setHighPrice(new BigDecimal(high));
        row.setLowPrice(new BigDecimal(low));
        row.setClosePrice(new BigDecimal(close));
        row.setClosed(true);
        row.setProvider("LOCAL_PERSISTED_OHLCV");
        row.setSourceTraceId("trace-bars");
        return row;
    }
}
