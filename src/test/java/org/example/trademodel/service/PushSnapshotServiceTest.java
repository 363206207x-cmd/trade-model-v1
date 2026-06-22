package org.example.trademodel.service;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

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
        return plan;
    }
}
