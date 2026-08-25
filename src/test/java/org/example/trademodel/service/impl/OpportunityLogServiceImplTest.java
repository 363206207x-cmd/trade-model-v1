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
import org.example.trademodel.opportunitylog.OpportunityLogCountRow;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.opportunitylog.OpportunityLogPublicDTO;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class OpportunityLogServiceImplTest {
    private static final Long USER_A_ID = 17L;
    private static final Long USER_B_ID = 29L;

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
                executionPlanMapper, pushSnapshotMapper, persistedOhlcvBarMapper);
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
    void evaluate_longTargetFirstPersistsOnlySharedMarketOutcomeAndMfeMae() {
        OpportunityLogDO row = pendingLong();
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-1")).thenReturn(row);
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "121", "98", "118")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunityForSystem("opp-1", row.getAnchorTime().plusHours(2));

        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
        assertThat(dto.getUserPositionId()).isNull();
        assertThat(dto.getUserPositionPresent()).isFalse();
        assertThat(dto.getHitOrder()).isEqualTo(OpportunityLogStatus.TARGET_FIRST);
        assertThat(dto.getMfePrice()).isEqualByComparingTo("121");
        assertThat(dto.getMfeRatio()).isEqualByComparingTo("0.2100000000");
        assertThat(dto.getMaePrice()).isEqualByComparingTo("98");
        assertThat(dto.getMaeRatio()).isEqualByComparingTo("0.0200000000");
        verify(userPositionMapper, never()).listClaimedByExactSourceRefIdForSystem(any());
        verify(userPositionMapper, never()).listByExactSourceRefIdAndUserId(any(), anyLong());
    }

    @Test
    void evaluateForUser_positionOpenedAfterTargetHit_isNotExecutedEvidence() {
        OpportunityLogDO row = pendingLong();
        row.setPushPresent(false);
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-1")).thenReturn(row);
        when(opportunityLogMapper.selectByOpportunityIdForUser("opp-1", USER_A_ID)).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefIdAndUserId("plan-1", USER_A_ID)).thenReturn(List.of(userPosition(77L, "plan-1",
                LocalDateTime.of(2026, 6, 23, 11, 30))));
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "121", "98", "118")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunityForUser("opp-1", USER_A_ID,
                row.getAnchorTime().plusHours(3));

        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
        assertThat(dto.getUserPositionPresent()).isFalse();
        assertThat(dto.getUserPositionId()).isNull();
        assertThat(dto.getReasonCodes()).contains("LINKED_USER_POSITION_OPENED_AFTER_OUTCOME");
        assertThat(row.getReasonCodes()).doesNotContain("LINKED_USER_POSITION_OPENED_AFTER_OUTCOME");
    }

    @Test
    void evaluateForUser_positionOpenedAtTargetHit_isOwnerScopedExecutionEvidence() {
        OpportunityLogDO row = pendingLong();
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-1")).thenReturn(row);
        when(opportunityLogMapper.selectByOpportunityIdForUser("opp-1", USER_A_ID)).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefIdAndUserId("plan-1", USER_A_ID)).thenReturn(List.of(userPosition(77L, "plan-1",
                LocalDateTime.of(2026, 6, 23, 11, 0))));
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "121", "98", "118")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunityForUser("opp-1", USER_A_ID,
                row.getAnchorTime().plusHours(3));

        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.EXECUTED_VALID);
        assertThat(dto.getUserPositionId()).isEqualTo(77L);
        assertThat(row.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
        assertThat(row.getUserPositionId()).isNull();
        assertThat(row.getUserPositionPresent()).isFalse();
    }

    @Test
    void evaluateForUser_positionOpenedAfterInvalidationHit_isNotExecutedEvidence() {
        OpportunityLogDO row = pendingLong();
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-1")).thenReturn(row);
        when(opportunityLogMapper.selectByOpportunityIdForUser("opp-1", USER_A_ID)).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefIdAndUserId("plan-1", USER_A_ID)).thenReturn(List.of(userPosition(77L, "plan-1",
                LocalDateTime.of(2026, 6, 23, 11, 30))));
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "110", "89", "91")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunityForUser("opp-1", USER_A_ID,
                row.getAnchorTime().plusHours(3));

        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.MISSED_INVALID);
        assertThat(dto.getUserPositionPresent()).isFalse();
        assertThat(dto.getReasonCodes()).contains("LINKED_USER_POSITION_OPENED_AFTER_OUTCOME");
    }

    @Test
    void evaluateForUser_linkedPositionMissingOpenedAtIsReviewRequiredOnlyInOwnerProjection() {
        OpportunityLogDO row = pendingLong();
        UserPositionDO position = userPosition(77L, "plan-1", null);
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-1")).thenReturn(row);
        when(opportunityLogMapper.selectByOpportunityIdForUser("opp-1", USER_A_ID)).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefIdAndUserId("plan-1", USER_A_ID)).thenReturn(List.of(position));
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "121", "98", "118")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunityForUser("opp-1", USER_A_ID,
                row.getAnchorTime().plusHours(3));

        assertThat(dto.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.REVIEW_REQUIRED);
        assertThat(dto.getOpportunityStatus()).isNull();
        assertThat(dto.getReasonCodes()).contains("LINKED_USER_POSITION_OPEN_TIME_MISSING");
        assertThat(row.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.RESOLVED);
        assertThat(row.getReasonCodes()).doesNotContain("LINKED_USER_POSITION_OPEN_TIME_MISSING");
        assertThat(row.getUserPositionId()).isNull();
        assertThat(row.getUserPositionPresent()).isFalse();
    }

    @Test
    void evaluate_shortInvalidationFirstWithoutUserPosition_returnsMissedInvalid() {
        OpportunityLogDO row = pendingShort();
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-short")).thenReturn(row);
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("ETHUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "112", "99", "108")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunityForSystem("opp-short", row.getAnchorTime().plusHours(2));

        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.MISSED_INVALID);
        assertThat(dto.getHitOrder()).isEqualTo(OpportunityLogStatus.INVALIDATION_FIRST);
    }

    @Test
    void evaluate_sameBarTargetAndInvalidation_isAmbiguousWithoutFinalStatus() {
        OpportunityLogDO row = pendingLong();
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-1")).thenReturn(row);
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "122", "89", "101")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunityForSystem("opp-1", row.getAnchorTime().plusHours(2));

        assertThat(dto.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.AMBIGUOUS_MARKET_PATH);
        assertThat(dto.getOpportunityStatus()).isNull();
        assertThat(dto.getHitOrder()).isEqualTo(OpportunityLogStatus.AMBIGUOUS_SAME_BAR);
    }

    @Test
    void evaluate_noBars_returnsMarketPathUnavailableWithoutFinalStatus() {
        OpportunityLogDO row = pendingLong();
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-1")).thenReturn(row);
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of());
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunityForSystem("opp-1", row.getAnchorTime().plusHours(2));

        assertThat(dto.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.MARKET_PATH_UNAVAILABLE);
        assertThat(dto.getOpportunityStatus()).isNull();
    }

    @Test
    void evaluate_targetFirstIgnoresPrivateAccountRiskAndReturnsPublicStatus() {
        OpportunityLogDO row = pendingLong();
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-1")).thenReturn(row);
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "121", "99", "120")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunityForSystem("opp-1", row.getAnchorTime().plusHours(2));

        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
        assertThat(dto.getRiskBlockedEvidence()).isNull();
        verifyNoInteractions(accountRiskSnapshotMapper, pushRecheckLogMapper);
    }

    @Test
    void evaluate_targetFirstDoesNotReadOrRewritePrivateState() {
        OpportunityLogDO row = pendingLong();
        row.setPushPresent(true);
        row.setPushId(9L);
        row.setUserPositionId(501L);
        row.setUserPositionPresent(true);
        row.setRiskBlockedEvidence(true);
        row.setRiskBlockedAt(LocalDateTime.of(2026, 6, 23, 10, 15));
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-1")).thenReturn(row);
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "121", "99", "120")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunityForSystem("opp-1", row.getAnchorTime().plusHours(2));

        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
        assertThat(row.getPushId()).isEqualTo(9L);
        assertThat(row.getPushPresent()).isTrue();
        assertThat(row.getUserPositionId()).isEqualTo(501L);
        assertThat(row.getUserPositionPresent()).isTrue();
        assertThat(row.getRiskBlockedEvidence()).isTrue();
        assertThat(row.getRiskBlockedAt()).isEqualTo(LocalDateTime.of(2026, 6, 23, 10, 15));
        verifyNoInteractions(pushRecheckLogMapper, accountRiskSnapshotMapper);
    }

    @Test
    void publicEvaluationIsScopedToTheAnalysisOwnerAndNeverReadsPrivateSources() {
        OpportunityLogDO userASource = resolvedTargetFirst();
        OpportunityLogPublicDTO publicProjection = publicProjection();
        when(opportunityLogMapper.selectPublicApiByOpportunityIdForUser("opp-1", USER_A_ID))
                .thenReturn(publicProjection);
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-1"))
                .thenReturn(userASource);

        OpportunityLogPublicDTO userA = service.evaluatePublicOpportunityForUser(
                "opp-1", USER_A_ID, userASource.getAnchorTime().plusHours(2));

        assertThat(userA).isEqualTo(publicProjection);
        verifyNoInteractions(userPositionMapper, pushSnapshotMapper, pushRecheckLogMapper, accountRiskSnapshotMapper);
    }

    @Test
    void publicEvaluationRejectsAnotherUsersOpportunityBeforeEvaluation() {
        when(opportunityLogMapper.selectPublicApiByOpportunityIdForUser("opp-1", USER_B_ID))
                .thenReturn(null);

        assertThatThrownBy(() -> service.evaluatePublicOpportunityForUser(
                "opp-1", USER_B_ID, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not owned");

        verify(opportunityLogMapper, never()).selectPublicEvaluationSourceByOpportunityId(any());
    }

    @Test
    void evaluateForUser_multipleExactPositionsRequireReviewWithoutPersistingOwnerState() {
        OpportunityLogDO row = pendingLong();
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-1")).thenReturn(row);
        when(opportunityLogMapper.selectByOpportunityIdForUser("opp-1", USER_A_ID)).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefIdAndUserId("plan-1", USER_A_ID))
                .thenReturn(List.of(userPosition(1L, "plan-1"), userPosition(2L, "plan-1")));
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "121", "98", "118")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunityForUser("opp-1", USER_A_ID,
                row.getAnchorTime().plusHours(2));

        assertThat(dto.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.REVIEW_REQUIRED);
        assertThat(dto.getOpportunityStatus()).isNull();
        assertThat(dto.getReasonCodes()).contains("MULTIPLE_LINKED_USER_POSITIONS");
        assertThat(row.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.RESOLVED);
        assertThat(row.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
        assertThat(row.getReasonCodes()).doesNotContain("MULTIPLE_LINKED_USER_POSITIONS");
        assertThat(row.getUserPositionId()).isNull();
        assertThat(row.getUserPositionPresent()).isFalse();
    }

    @Test
    void evaluate_persistedInvalidationAfterAsOfIsIgnored() {
        OpportunityLogDO row = pendingLong();
        DecisionResult decision = new DecisionResult();
        decision.setHotResetInvalidated(true);
        decision.setHotResetInvalidatedAt(LocalDateTime.of(2026, 6, 23, 10, 30));
        decision.setHotResetReasonCode("HOT_RESET_AFTER_AS_OF");
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-1")).thenReturn(row);
        when(decisionResultMapper.selectByDecisionId("dec-1")).thenReturn(decision);
        when(executionPlanMapper.selectByPlanId("plan-1")).thenReturn(null);
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of());
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunityForSystem("opp-1",
                LocalDateTime.of(2026, 6, 23, 10, 5));

        assertThat(dto.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.MARKET_PATH_UNAVAILABLE);
        assertThat(dto.getOpportunityStatus()).isNull();
        assertThat(dto.getHitOrder()).isNull();
        assertThat(dto.getReasonCodes()).contains("PERSISTED_INVALIDATION_AFTER_AS_OF_IGNORED");
    }

    @Test
    void evaluate_resolvedRecordIsImmutableAndDeduplicated() {
        OpportunityLogDO row = pendingLong();
        row.setLifecycleStatus(OpportunityLogStatus.RESOLVED);
        row.setOpportunityStatus(OpportunityLogStatus.MISSED_VALID);
        row.setMfeRatio(new BigDecimal("0.10"));
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-1")).thenReturn(row);

        OpportunityLogDTO dto = service.evaluateOpportunityForSystem("opp-1", row.getAnchorTime().plusHours(2));

        assertThat(dto.getDeduplicated()).isTrue();
        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
        assertThat(dto.getMfeRatio()).isEqualByComparingTo("0.10");
        verify(opportunityLogMapper, never()).updateEvaluation(any());
    }

    @Test
    void userScopedFindDoesNotExposeAnotherOwnersPersistedAssociation() {
        OpportunityLogDO row = resolvedTargetFirst();
        row.setUserPositionId(88L);
        row.setUserPositionPresent(true);
        row.setReasonCodes("TARGET_FIRST,MULTIPLE_LINKED_USER_POSITIONS");
        when(opportunityLogMapper.selectByOpportunityIdForUser("opp-1", USER_A_ID)).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefIdAndUserId("plan-1", USER_A_ID)).thenReturn(List.of());
        when(userPositionMapper.listByExactSourceRefIdAndUserId("ana-1", USER_A_ID)).thenReturn(List.of());

        OpportunityLogDTO dto = service.findByIdForUser("opp-1", USER_A_ID);

        assertThat(dto.getUserPositionId()).isNull();
        assertThat(dto.getUserPositionPresent()).isFalse();
        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
        assertThat(dto.getReasonCodes()).isEqualTo("TARGET_FIRST");
        verify(userPositionMapper, never()).listClaimedByExactSourceRefIdForSystem(any());
    }

    @Test
    void userScopedFindProjectsOnlyCurrentOwnersPositionAssociation() {
        OpportunityLogDO row = resolvedTargetFirst();
        row.setUserPositionId(88L);
        row.setUserPositionPresent(true);
        UserPositionDO owned = userPosition(17L, "plan-1", LocalDateTime.of(2026, 6, 23, 10, 30));
        owned.setUserId(USER_A_ID);
        when(opportunityLogMapper.selectByOpportunityIdForUser("opp-1", USER_A_ID)).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefIdAndUserId("plan-1", USER_A_ID))
                .thenReturn(List.of(owned));

        OpportunityLogDTO dto = service.findByIdForUser("opp-1", USER_A_ID);

        assertThat(dto.getUserPositionId()).isEqualTo(17L);
        assertThat(dto.getUserPositionPresent()).isTrue();
        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.EXECUTED_VALID);
        verify(userPositionMapper, never()).listClaimedByExactSourceRefIdForSystem(any());
    }

    @Test
    void userScopedEvaluateNeverUsesGlobalClaimedPositionLookup() {
        OpportunityLogDO row = pendingLong();
        UserPositionDO owned = userPosition(17L, "plan-1", LocalDateTime.of(2026, 6, 23, 10, 30));
        owned.setUserId(USER_A_ID);
        when(opportunityLogMapper.selectPublicEvaluationSourceByOpportunityId("opp-1")).thenReturn(row);
        when(opportunityLogMapper.selectByOpportunityIdForUser("opp-1", USER_A_ID)).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefIdAndUserId("plan-1", USER_A_ID))
                .thenReturn(List.of(owned));
        when(persistedOhlcvBarMapper.selectClosedBarsBetween(eq("BTCUSDT"), eq("1h"), anyLong(), anyLong(), eq(2001)))
                .thenReturn(List.of(bar(1, "100", "121", "98", "118")));
        when(opportunityLogMapper.updateEvaluation(row)).thenReturn(1);

        OpportunityLogDTO dto = service.evaluateOpportunityForUser(
                "opp-1", USER_A_ID, row.getAnchorTime().plusHours(2));

        assertThat(dto.getUserPositionId()).isEqualTo(17L);
        assertThat(dto.getUserPositionPresent()).isTrue();
        verify(userPositionMapper, never()).listClaimedByExactSourceRefIdForSystem(any());
    }

    @Test
    void userScopedFindHidesAnotherOwnersReviewLifecycleAndEvaluationTimestampForSameSymbol() {
        OpportunityLogDO row = pendingLong();
        row.setLifecycleStatus(OpportunityLogStatus.REVIEW_REQUIRED);
        row.setEvaluationAsOf(LocalDateTime.of(2026, 6, 23, 11, 30));
        row.setReasonCodes("MULTIPLE_LINKED_USER_POSITIONS");
        when(opportunityLogMapper.selectByOpportunityIdForUser("opp-1", USER_B_ID)).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefIdAndUserId("plan-1", USER_B_ID)).thenReturn(List.of());
        when(userPositionMapper.listByExactSourceRefIdAndUserId("ana-1", USER_B_ID)).thenReturn(List.of());

        OpportunityLogDTO dto = service.findByIdForUser("opp-1", USER_B_ID);

        assertThat(dto.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(dto.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.PENDING_EVALUATION);
        assertThat(dto.getEvaluationAsOf()).isNull();
        assertThat(dto.getReasonCodes()).isEmpty();
        assertThat(dto.getUserPositionId()).isNull();
        assertThat(dto.getUserPositionPresent()).isFalse();
    }

    @Test
    void userScopedFindNeverExposesSharedEvaluationTimestampWithoutOwnerProvenance() {
        OpportunityLogDO row = resolvedTargetFirst();
        row.setEvaluationAsOf(LocalDateTime.of(2026, 6, 23, 11, 30));
        when(opportunityLogMapper.selectByOpportunityIdForUser("opp-1", USER_B_ID)).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefIdAndUserId("plan-1", USER_B_ID)).thenReturn(List.of());
        when(userPositionMapper.listByExactSourceRefIdAndUserId("ana-1", USER_B_ID)).thenReturn(List.of());

        OpportunityLogDTO dto = service.findByIdForUser("opp-1", USER_B_ID);

        assertThat(dto.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.RESOLVED);
        assertThat(dto.getEvaluationAsOf()).isNull();
        assertThat(dto.getUserPositionId()).isNull();
        assertThat(dto.getUserPositionPresent()).isFalse();
    }

    @Test
    void userScopedFindHidesAnotherOwnersClosedPositionDerivedState() {
        OpportunityLogDO row = resolvedTargetFirst();
        row.setUserPositionId(88L);
        row.setUserPositionPresent(true);
        row.setEvaluationAsOf(LocalDateTime.of(2026, 6, 23, 11, 30));
        row.setReasonCodes("TARGET_FIRST,LINKED_USER_POSITION_OPENED_AFTER_OUTCOME");
        when(opportunityLogMapper.selectByOpportunityIdForUser("opp-1", USER_B_ID)).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefIdAndUserId("plan-1", USER_B_ID)).thenReturn(List.of());
        when(userPositionMapper.listByExactSourceRefIdAndUserId("ana-1", USER_B_ID)).thenReturn(List.of());

        OpportunityLogDTO dto = service.findByIdForUser("opp-1", USER_B_ID);

        assertThat(dto.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.RESOLVED);
        assertThat(dto.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
        assertThat(dto.getEvaluationAsOf()).isNull();
        assertThat(dto.getReasonCodes()).isEqualTo("TARGET_FIRST");
        assertThat(dto.getUserPositionId()).isNull();
        assertThat(dto.getUserPositionPresent()).isFalse();
    }

    @Test
    void userScopedQueryHidesUnknownOwnerDerivedNonTerminalLifecycleStates() {
        OpportunityLogDO active = pendingLong();
        active.setOpportunityId("opp-active");
        active.setLifecycleStatus("ACTIVE");
        active.setEvaluationAsOf(LocalDateTime.of(2026, 6, 23, 11, 0));
        OpportunityLogDO closedPending = pendingLong();
        closedPending.setOpportunityId("opp-closed-pending");
        closedPending.setLifecycleStatus("CLOSED_PENDING");
        closedPending.setEvaluationAsOf(LocalDateTime.of(2026, 6, 23, 12, 0));
        when(opportunityLogMapper.queryForUser(anyLong(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(active, closedPending));
        when(userPositionMapper.listByExactSourceRefIdAndUserId("plan-1", USER_B_ID)).thenReturn(List.of());
        when(userPositionMapper.listByExactSourceRefIdAndUserId("ana-1", USER_B_ID)).thenReturn(List.of());

        List<OpportunityLogDTO> rows = service.queryForUser(
                USER_B_ID, null, null, null, "BTCUSDT", null, null, null, null, 10);

        assertThat(rows).hasSize(2)
                .allSatisfy(dto -> {
                    assertThat(dto.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.PENDING_EVALUATION);
                    assertThat(dto.getEvaluationAsOf()).isNull();
                    assertThat(dto.getUserPositionId()).isNull();
                    assertThat(dto.getUserPositionPresent()).isFalse();
                });
    }

    @Test
    void userScopedQueryAppliesLifecycleFilterAfterOwnerProjection() {
        OpportunityLogDO row = resolvedTargetFirst();
        when(opportunityLogMapper.queryForUser(anyLong(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(row));
        when(userPositionMapper.listByExactSourceRefIdAndUserId("plan-1", USER_A_ID))
                .thenReturn(List.of(userPosition(1L, "plan-1"), userPosition(2L, "plan-1")));

        List<OpportunityLogDTO> rows = service.queryForUser(
                USER_A_ID, null, null, null, "BTCUSDT", null,
                OpportunityLogStatus.REVIEW_REQUIRED, null, null, 10);

        assertThat(rows).singleElement()
                .satisfies(dto -> {
                    assertThat(dto.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.REVIEW_REQUIRED);
                    assertThat(dto.getOpportunityStatus()).isNull();
                });
        verify(opportunityLogMapper).queryForUser(
                eq(USER_A_ID), isNull(), isNull(), isNull(), eq("BTCUSDT"), isNull(), isNull(), eq(200));
    }

    @Test
    void userScopedFindFailsClosedWhenOwnerProjectionLookupFails() {
        OpportunityLogDO row = resolvedTargetFirst();
        row.setUserPositionId(88L);
        row.setUserPositionPresent(true);
        row.setEvaluationAsOf(LocalDateTime.of(2026, 6, 23, 11, 30));
        when(opportunityLogMapper.selectByOpportunityIdForUser("opp-1", USER_A_ID)).thenReturn(row);
        when(userPositionMapper.listByExactSourceRefIdAndUserId("plan-1", USER_A_ID))
                .thenThrow(new IllegalStateException("lookup unavailable"));

        OpportunityLogDTO dto = service.findByIdForUser("opp-1", USER_A_ID);

        assertThat(dto.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.REVIEW_REQUIRED);
        assertThat(dto.getOpportunityStatus()).isNull();
        assertThat(dto.getEvaluationAsOf()).isNull();
        assertThat(dto.getReasonCodes()).contains("USER_POSITION_PROJECTION_UNAVAILABLE");
        assertThat(dto.getUserPositionId()).isNull();
        assertThat(dto.getUserPositionPresent()).isFalse();
    }

    @Test
    void getStats_countsReviewOnlyOpportunityOutcomes() {
        OpportunityLogStatsDTO aggregate = new OpportunityLogStatsDTO();
        aggregate.setTotalCount(2);
        aggregate.setResolvedCount(2);
        aggregate.setMissedValidCount(1);
        aggregate.setMissedInvalidCount(1);
        aggregate.setValidOpportunityCount(1);
        aggregate.setInvalidOpportunityCount(1);
        aggregate.setAverageMfeRatio(new BigDecimal("0.15000000"));
        aggregate.setAverageMaeRatio(new BigDecimal("0.05000000"));
        aggregate.setMaxMfeRatio(new BigDecimal("0.20000000"));
        aggregate.setMaxMaeRatio(new BigDecimal("0.06000000"));
        when(opportunityLogMapper.aggregateStats(eq("BTCUSDT"), any(), any())).thenReturn(aggregate);
        when(opportunityLogMapper.countByStatus(eq("BTCUSDT"), any(), any()))
                .thenReturn(List.of(countRow(OpportunityLogStatus.MISSED_VALID, 1),
                        countRow(OpportunityLogStatus.MISSED_INVALID, 1)));
        when(opportunityLogMapper.countBySource(eq("BTCUSDT"), any(), any()))
                .thenReturn(List.of(countRow("AUTHORITATIVE_ANALYSIS", 2)));

        OpportunityLogStatsDTO stats = service.getStats("BTCUSDT", LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertThat(stats.getTotalCount()).isEqualTo(2);
        assertThat(stats.getResolvedCount()).isEqualTo(2);
        assertThat(stats.getMissedValidCount()).isEqualTo(1);
        assertThat(stats.getMissedInvalidCount()).isEqualTo(1);
        assertThat(stats.getValidRate()).isEqualByComparingTo("0.50000000");
        assertThat(stats.getAverageMfeRatio()).isEqualByComparingTo("0.15000000");
        assertThat(stats.getMaxMaeRatio()).isEqualByComparingTo("0.06000000");
        assertThat(stats.getStatusCounts()).containsEntry(OpportunityLogStatus.MISSED_VALID, 1);
        assertThat(stats.getSourceCounts()).containsEntry("AUTHORITATIVE_ANALYSIS", 2);
        assertThat(stats.getReviewOnly()).isTrue();
        assertThat(stats.getNotExecutable()).isTrue();
        verify(opportunityLogMapper, never()).query(any(), any(), any(), any(), any(), any(), any(), any(), anyInt());
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

    private static OpportunityLogDO resolvedTargetFirst() {
        OpportunityLogDO row = pendingLong();
        row.setLifecycleStatus(OpportunityLogStatus.RESOLVED);
        row.setOpportunityStatus(OpportunityLogStatus.EXECUTED_VALID);
        row.setTargetHit(true);
        row.setTargetHitAt(LocalDateTime.of(2026, 6, 23, 11, 0));
        row.setHitOrder(OpportunityLogStatus.TARGET_FIRST);
        return row;
    }

    private static OpportunityLogPublicDTO publicProjection() {
        OpportunityLogDO row = resolvedTargetFirst();
        return new OpportunityLogPublicDTO(
                row.getOpportunityId(),
                row.getAnalysisId(),
                row.getSymbol(),
                row.getTimeframe(),
                row.getDirection(),
                row.getLifecycleStatus(),
                OpportunityLogStatus.MISSED_VALID,
                row.getAnchorTime(),
                row.getTargetHitAt(),
                row.getEntryReference(),
                row.getTargetPrice(),
                row.getInvalidationPrice(),
                row.getTargetHit(),
                row.getInvalidationHit(),
                row.getTargetHitAt(),
                row.getInvalidationHitAt(),
                row.getHitOrder(),
                row.getMfePrice(),
                row.getMfeRatio(),
                row.getMaePrice(),
                row.getMaeRatio(),
                row.getMarketDataSource(),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true);
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
        return userPosition(id, sourceRefId, LocalDateTime.of(2026, 6, 23, 10, 30));
    }

    private static UserPositionDO userPosition(Long id, String sourceRefId, LocalDateTime openedAt) {
        UserPositionDO row = new UserPositionDO();
        row.setId(id);
        row.setSourceType("MANUAL_INDEPENDENT");
        row.setSourceRefId(sourceRefId);
        row.setStatus("CLOSED");
        row.setOpenedAt(openedAt);
        row.setClosedAt(LocalDateTime.of(2026, 6, 23, 11, 30));
        return row;
    }

    private static OpportunityLogCountRow countRow(String name, int count) {
        OpportunityLogCountRow row = new OpportunityLogCountRow();
        row.setName(name);
        row.setCount(count);
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
        row.setVolume(new BigDecimal("1000"));
        row.setClosed(true);
        row.setProvider("LOCAL_PERSISTED_OHLCV");
        row.setProviderMarketType("SPOT");
        row.setSourceEndpoint("/controlled-test/klines");
        row.setSourceBatchId("batch-bars");
        row.setSourceTraceId("trace-bars");
        row.setSourceVersion(1);
        row.setFetchTime(openTime.plusHours(1));
        row.setSourceStatus("READY");
        row.setFreshnessStatus("FRESH");
        row.setProvenanceVersion("opportunity-test-v1");
        row.setIngestionRunId("run-bars");
        row.setIngestedAt(openTime.plusHours(1));
        row.setQualityStatus("OK");
        return row;
    }
}
