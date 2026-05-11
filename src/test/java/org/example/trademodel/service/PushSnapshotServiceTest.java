package org.example.trademodel.service;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushSnapshotServiceTest {

    @Mock
    private PushSnapshotMapper pushSnapshotMapper;
    @Mock
    private AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    @Mock
    private WatchlistPushEligibilityService watchlistPushEligibilityService;

    private PushSnapshotService service;

    @BeforeEach
    void setUp() {
        service = new PushSnapshotService(
                pushSnapshotMapper,
                accountRiskSnapshotMapper,
                watchlistPushEligibilityService);
    }

    @Test
    void insertAuthoritativeSnapshot_capturesWhenWatchlistEligible() {
        when(watchlistPushEligibilityService.isEligibleForDirectionalPush("BTCUSDT")).thenReturn(true);

        service.insertAuthoritativeSnapshot(baseRun(), baseAnalysis(), baseDecision(), basePlan(), 77L);

        ArgumentCaptor<TmPushSnapshotDO> cap = ArgumentCaptor.forClass(TmPushSnapshotDO.class);
        verify(pushSnapshotMapper).insert(cap.capture());
        assertThat(cap.getValue().getSymbol()).isEqualTo("BTCUSDT");
        assertThat(cap.getValue().getPushStatus()).isEqualTo("CAPTURED");
        assertThat(cap.getValue().getAccountRiskSnapshotId()).isEqualTo(77L);
        verifyNoInteractions(accountRiskSnapshotMapper);
    }

    @Test
    void insertAuthoritativeSnapshot_skipsWhenNotWatchlistEligible() {
        when(watchlistPushEligibilityService.isEligibleForDirectionalPush("BTCUSDT")).thenReturn(false);

        service.insertAuthoritativeSnapshot(baseRun(), baseAnalysis(), baseDecision(), basePlan(), null);

        verify(pushSnapshotMapper, never()).insert(any(TmPushSnapshotDO.class));
        verifyNoInteractions(accountRiskSnapshotMapper);
    }

    @Test
    void insertAuthoritativeSnapshot_skipsWhenEligibilityThrows() {
        when(watchlistPushEligibilityService.isEligibleForDirectionalPush("BTCUSDT"))
                .thenThrow(new IllegalStateException("rule config unavailable"));

        service.insertAuthoritativeSnapshot(baseRun(), baseAnalysis(), baseDecision(), basePlan(), null);

        verify(pushSnapshotMapper, never()).insert(any(TmPushSnapshotDO.class));
        verifyNoInteractions(accountRiskSnapshotMapper);
    }

    private static AnalysisRunDO baseRun() {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId("analysis-1");
        run.setSymbol("BTCUSDT");
        run.setTimeframe("1h");
        run.setRuleVersion("v1");
        run.setTraceId("trace-1");
        return run;
    }

    private static AssetAnalysisVO baseAnalysis() {
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        analysis.setAnalysisId("analysis-1");
        analysis.setSymbol("BTCUSDT");
        analysis.setTimeframe("1h");
        analysis.setDataQualityScore(85);
        return analysis;
    }

    private static DecisionBundleVO baseDecision() {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setIsWorthOpening(true);
        decision.setRiskLevel("LOW");
        decision.setAiPlanMode("CONFIRM");
        decision.setConfusedScore(20);
        decision.setPushTriggerPrice(new BigDecimal("100"));
        decision.setPushExpiresAt(LocalDateTime.now().plusMinutes(30));
        decision.setPushInvalidPriceBelow(new BigDecimal("90"));
        decision.setPushInvalidationSummary("below 90 invalidates");
        return decision;
    }

    private static ExecutionPlanVO basePlan() {
        ExecutionPlanVO plan = new ExecutionPlanVO();
        plan.setEntryZone("100-102");
        plan.setStopLoss("90");
        plan.setInvalidCondition("below 90 invalidates");
        plan.setPositionSuggestion("10%");
        return plan;
    }
}
