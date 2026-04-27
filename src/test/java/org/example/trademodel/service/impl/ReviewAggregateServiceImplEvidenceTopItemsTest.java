package org.example.trademodel.service.impl;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.mapper.MarketEnvironmentSnapshotMapper;
import org.example.trademodel.mapper.MissedOpportunityMapper;
import org.example.trademodel.mapper.MonitorAlertMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.ReviewResultMapper;
import org.example.trademodel.service.EvidenceService;
import org.example.trademodel.service.RuleVersionLogQueryService;
import org.example.trademodel.service.ScoreService;
import org.example.trademodel.vo.EvidenceBriefVO;
import org.example.trademodel.vo.ReviewAggregateVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewAggregateServiceImplEvidenceTopItemsTest {

    @Mock
    private AnalysisRunMapper analysisRunMapper;
    @Mock
    private DecisionResultMapper decisionResultMapper;
    @Mock
    private ExecutionPlanMapper executionPlanMapper;
    @Mock
    private PushSnapshotMapper pushSnapshotMapper;
    @Mock
    private MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper;
    @Mock
    private AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    @Mock
    private PushRecheckLogMapper pushRecheckLogMapper;
    @Mock
    private MissedOpportunityMapper missedOpportunityMapper;
    @Mock
    private MonitorAlertMapper monitorAlertMapper;
    @Mock
    private AssetStateMapper assetStateMapper;
    @Mock
    private HotResetEventMapper hotResetEventMapper;
    @Mock
    private ReviewResultMapper reviewResultMapper;
    @Mock
    private RuleVersionLogQueryService ruleVersionLogQueryService;
    @Mock
    private EvidenceService evidenceService;
    @Mock
    private ScoreService scoreService;

    private ReviewAggregateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReviewAggregateServiceImpl(
                analysisRunMapper,
                decisionResultMapper,
                executionPlanMapper,
                pushSnapshotMapper,
                marketEnvironmentSnapshotMapper,
                accountRiskSnapshotMapper,
                pushRecheckLogMapper,
                missedOpportunityMapper,
                monitorAlertMapper,
                assetStateMapper,
                hotResetEventMapper,
                reviewResultMapper,
                ruleVersionLogQueryService,
                evidenceService,
                scoreService);
    }

    @Test
    void getAggregateByAnalysisId_returnsEvidenceTopItems_whenServiceReturnsRows() {
        String analysisId = "ana-ev-top3";
        stubMinimalAggregateDependencies(analysisId);

        EvidenceBriefVO e1 = new EvidenceBriefVO();
        e1.setEvidenceType("价格结构");
        e1.setDescription("回踩确认");
        e1.setDirection("BULLISH");
        e1.setSource("SYSTEM_GENERATED");

        EvidenceBriefVO e2 = new EvidenceBriefVO();
        e2.setEvidenceType("杠杆");
        e2.setDescription("费率偏高");
        e2.setDirection("BEARISH");
        e2.setSource("MARKET_HEURISTIC");

        when(evidenceService.listTopEvidenceBriefByAnalysisId(analysisId)).thenReturn(List.of(e1, e2));

        Optional<ReviewAggregateVO> result = service.getAggregateByAnalysisId(analysisId);

        assertThat(result).isPresent();
        verify(evidenceService).listTopEvidenceBriefByAnalysisId(eq(analysisId));
        assertThat(result.get().getEvidenceTopItems()).hasSize(2);
        assertThat(result.get().getEvidenceTopItems().get(0).getEvidenceType()).isEqualTo("价格结构");
        assertThat(result.get().getEvidenceTopItems().get(0).getDescription()).isEqualTo("回踩确认");
        assertThat(result.get().getEvidenceTopItems().get(0).getDirection()).isEqualTo("BULLISH");
        assertThat(result.get().getEvidenceTopItems().get(0).getSource()).isEqualTo("SYSTEM_GENERATED");
        assertThat(result.get().getEvidenceTopItems().get(1).getEvidenceType()).isEqualTo("杠杆");
        assertThat(result.get().getEvidenceTopItems().get(1).getDirection()).isEqualTo("BEARISH");
        assertThat(result.get().getEvidenceTopItems().get(1).getSource()).isEqualTo("MARKET_HEURISTIC");
    }

    @Test
    void getAggregateByAnalysisId_returnsEmptyEvidenceTopItems_whenNone() {
        String analysisId = "ana-ev-empty";
        stubMinimalAggregateDependencies(analysisId);

        when(evidenceService.listTopEvidenceBriefByAnalysisId(analysisId)).thenReturn(Collections.emptyList());

        Optional<ReviewAggregateVO> result = service.getAggregateByAnalysisId(analysisId);

        assertThat(result).isPresent();
        assertThat(result.get().getEvidenceTopItems()).isEmpty();
    }

    private void stubMinimalAggregateDependencies(String analysisId) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setSymbol("BTCUSDT");
        run.setTimeframe("1h");
        run.setAnalysisTime(LocalDateTime.of(2026, 4, 21, 10, 0));
        when(analysisRunMapper.selectById(analysisId)).thenReturn(run);

        when(pushSnapshotMapper.listByAnalysisId(analysisId)).thenReturn(Collections.emptyList());
        when(missedOpportunityMapper.listByAnalysisId(analysisId)).thenReturn(Collections.emptyList());
        when(monitorAlertMapper.listByAnalysisId(analysisId)).thenReturn(Collections.emptyList());
        when(ruleVersionLogQueryService.listByAnalysisId(analysisId, 20)).thenReturn(Collections.emptyList());
        when(marketEnvironmentSnapshotMapper.selectByAnalysisId(analysisId)).thenReturn(null);
        when(scoreService.listTopScoreBriefByAnalysisId(anyString())).thenReturn(Collections.emptyList());
    }
}
