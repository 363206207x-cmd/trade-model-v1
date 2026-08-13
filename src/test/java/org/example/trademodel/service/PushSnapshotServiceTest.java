package org.example.trademodel.service;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PushSnapshotServiceTest {

    @Mock
    private PushSnapshotMapper pushSnapshotMapper;
    @Mock
    private AccountRiskSnapshotMapper accountRiskSnapshotMapper;

    private PushSnapshotService service;

    @BeforeEach
    void setUp() {
        service = new PushSnapshotService(pushSnapshotMapper, accountRiskSnapshotMapper);
    }

    @Test
    void directionalPushBlockedPreventsSnapshotWriteAtScore85() {
        DecisionBundleVO decision = decision(true);
        decision.setDirectionalPushBlocked(true);
        decision.setDirectionalPushBlockReason("CONFUSED_SCORE_BLOCK_THRESHOLD");
        decision.setConfusedScore(85);

        service.insertAuthoritativeSnapshot(run(), analysis(), decision, plan(), 10L);

        verify(pushSnapshotMapper, never()).insert(any());
        verify(accountRiskSnapshotMapper, never()).insert(any());
    }

    @Test
    void directionalPushNotBlockedAllowsExistingSnapshotFlowAtScore84() {
        DecisionBundleVO decision = decision(true);
        decision.setDirectionalPushBlocked(false);
        decision.setConfusedScore(84);

        service.insertAuthoritativeSnapshot(run(), analysis(), decision, plan(), 10L);

        verify(pushSnapshotMapper).insert(any());
    }

    @Test
    void decisionAndPushSnapshotShareSameExpiryInstant() {
        service.setClock(Clock.fixed(Instant.parse("2026-07-13T12:00:00Z"), ZoneOffset.UTC));
        DecisionBundleVO decision = decision(true);
        decision.setExpiresAt(OffsetDateTime.parse("2026-07-14T20:00:00+08:00"));
        decision.setPushExpiresAt(LocalDateTime.parse("2026-07-14T12:00:00"));
        ArgumentCaptor<org.example.trademodel.entity.TmPushSnapshotDO> captor =
                ArgumentCaptor.forClass(org.example.trademodel.entity.TmPushSnapshotDO.class);

        service.insertAuthoritativeSnapshot(run(), analysis(), decision, plan(), 10L);

        verify(pushSnapshotMapper).insert(captor.capture());
        assertThat(captor.getValue().getExpiresAt())
                .isEqualTo(LocalDateTime.parse("2026-07-14T12:00:00"));
        assertThat(captor.getValue().getPushCreateTime())
                .isEqualTo(LocalDateTime.parse("2026-07-13T12:00:00"));
    }

    @Test
    void inconsistentCompatibilityExpiryFailsClosed() {
        DecisionBundleVO decision = decision(true);
        decision.setExpiresAt(OffsetDateTime.parse("2026-07-14T12:00:00Z"));
        decision.setPushExpiresAt(LocalDateTime.parse("2026-07-14T13:00:00"));

        assertThatThrownBy(() -> service.insertAuthoritativeSnapshot(
                run(), analysis(), decision, plan(), 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UTC compatibility timestamp");
        verify(pushSnapshotMapper, never()).insert(any());
    }

    @Test
    void missingCandidateExposureFailsAccountRiskClosed() {
        TmAccountRiskSnapshotDO snapshot = verifiedAccountRisk();
        ExecutionPlanCandidateDO candidate = candidate(null, "1x");

        var assessment = service.assessCandidate(snapshot, candidate, "MEDIUM", false);

        assertThat(assessment.allowed()).isFalse();
        assertThat(snapshot.getRiskAllowed()).isFalse();
        assertThat(snapshot.getRiskReasonCode()).isEqualTo("POSITION_EXPOSURE_UNAVAILABLE");
        assertThat(snapshot.getPositionExposure()).isNull();
    }

    @Test
    void configuredExtremeRiskLimitIsStricterThanHighRiskLimit() {
        ExecutionPlanCandidateDO eightPercent = candidate("8%", "1x");
        TmAccountRiskSnapshotDO high = verifiedAccountRisk();
        TmAccountRiskSnapshotDO extreme = verifiedAccountRisk();

        var highAssessment = service.assessCandidate(high, eightPercent, "HIGH", false);
        var extremeAssessment = service.assessCandidate(extreme, eightPercent, "EXTREME", false);

        assertThat(highAssessment.maxAllowedExposure()).isEqualByComparingTo("0.10");
        assertThat(highAssessment.allowed()).isTrue();
        assertThat(extremeAssessment.maxAllowedExposure()).isEqualByComparingTo("0.05");
        assertThat(extremeAssessment.allowed()).isFalse();
        assertThat(extreme.getRiskReasonCode())
                .isEqualTo("EXPOSURE_LIMIT_EXCEEDED");
    }

    @Test
    void legacySnapshotEntryFailsClosedWithoutOwnerScopedRiskFacts() {
        ArgumentCaptor<TmAccountRiskSnapshotDO> captor =
                ArgumentCaptor.forClass(TmAccountRiskSnapshotDO.class);

        service.ensureAccountRiskSnapshot(run(), analysis(), decision(true), plan());

        verify(accountRiskSnapshotMapper).insert(captor.capture());
        assertThat(captor.getValue().getSourceStatus()).isEqualTo("INVALID");
        assertThat(captor.getValue().getRiskAllowed()).isFalse();
        assertThat(captor.getValue().getRiskReasonCode())
                .isEqualTo("LEGACY_ACCOUNT_RISK_CONTEXT_INSUFFICIENT");
    }

    private static AnalysisRunDO run() {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setRuleVersion("v-test");
        run.setTraceId("trace-test");
        return run;
    }

    private static AssetAnalysisVO analysis() {
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        analysis.setAnalysisId("ana-test");
        analysis.setSymbol("BTCUSDT");
        analysis.setTimeframe("1m");
        return analysis;
    }

    private static DecisionBundleVO decision(boolean worthOpening) {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(worthOpening);
        decision.setAiPlanMode("CONFIRM");
        decision.setRiskLevel("LOW");
        decision.setPushTriggerPrice(new BigDecimal("100"));
        return decision;
    }

    private static ExecutionPlanVO plan() {
        ExecutionPlanVO plan = new ExecutionPlanVO();
        plan.setEntryZone("100-102");
        plan.setStopLoss("98");
        plan.setPositionSuggestion("10%");
        plan.setFinalPlan(true);
        return plan;
    }

    private static ExecutionPlanCandidateDO candidate(String exposure, String leverage) {
        ExecutionPlanCandidateDO candidate = new ExecutionPlanCandidateDO();
        candidate.setPositionSuggestion(exposure);
        candidate.setLeverageSuggestion(leverage);
        return candidate;
    }

    private static TmAccountRiskSnapshotDO verifiedAccountRisk() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        TmAccountRiskSnapshotDO snapshot = new TmAccountRiskSnapshotDO();
        snapshot.setId(101L);
        snapshot.setSourceStatus("VERIFIED");
        snapshot.setAccountRiskStatus("RISK_ALLOWED");
        snapshot.setRiskAllowed(true);
        snapshot.setObservedAt(now.minusMinutes(1));
        snapshot.setFreshUntil(now.plusMinutes(5));
        return snapshot;
    }
}
