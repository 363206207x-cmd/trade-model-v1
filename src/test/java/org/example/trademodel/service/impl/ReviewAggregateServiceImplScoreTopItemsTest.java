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
import org.example.trademodel.vo.ReviewAggregateVO;
import org.example.trademodel.vo.ScoreBriefVO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewAggregateServiceImplScoreTopItemsTest {

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
    void getAggregateByAnalysisId_returnsScoreTopItems_whenServiceReturnsRows() {
        String analysisId = "ana-sc-top3";
        stubMinimalAggregateDependencies(analysisId);

        ScoreBriefVO s1 = new ScoreBriefVO();
        s1.setScoreType("趋势结构分");
        s1.setScoreValue(72.5);
        ScoreBriefVO s2 = new ScoreBriefVO();
        s2.setScoreType("综合可信度分");
        s2.setScoreValue(81.0);

        when(evidenceService.listTopEvidenceBriefByAnalysisId(analysisId)).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId(analysisId)).thenReturn(List.of(s1, s2));

        Optional<ReviewAggregateVO> result = service.getAggregateByAnalysisId(analysisId);

        assertThat(result).isPresent();
        verify(scoreService).listTopScoreBriefByAnalysisId(eq(analysisId));
        assertThat(result.get().getScoreTopItems()).hasSize(2);
        assertThat(result.get().getScoreTopItems().get(0).getScoreType()).isEqualTo("趋势结构分");
        assertThat(result.get().getScoreTopItems().get(0).getScoreValue()).isEqualTo(72.5);
        assertThat(result.get().getScoreTopItems().get(1).getScoreType()).isEqualTo("综合可信度分");
        assertThat(result.get().getScoreTopItems().get(1).getScoreValue()).isEqualTo(81.0);
        assertThat(result.get().getEvidenceTopItems()).isEmpty();
    }

    @Test
    void getAggregateByAnalysisId_returnsEmptyScoreTopItems_whenNone() {
        String analysisId = "ana-sc-empty";
        stubMinimalAggregateDependencies(analysisId);

        when(evidenceService.listTopEvidenceBriefByAnalysisId(analysisId)).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId(analysisId)).thenReturn(Collections.emptyList());

        Optional<ReviewAggregateVO> result = service.getAggregateByAnalysisId(analysisId);

        assertThat(result).isPresent();
        assertThat(result.get().getScoreTopItems()).isEmpty();
    }

    private void stubMinimalAggregateDependencies(String analysisId) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setSymbol("BTCUSDT");
        run.setTimeframe("1h");
        run.setAnalysisTime(LocalDateTime.of(2026, 4, 21, 11, 0));
        when(analysisRunMapper.selectById(analysisId)).thenReturn(run);

        when(pushSnapshotMapper.listByAnalysisId(analysisId)).thenReturn(Collections.emptyList());
        when(missedOpportunityMapper.listByAnalysisId(analysisId)).thenReturn(Collections.emptyList());
        when(monitorAlertMapper.listByAnalysisId(analysisId)).thenReturn(Collections.emptyList());
        when(ruleVersionLogQueryService.listByAnalysisId(analysisId, 20)).thenReturn(Collections.emptyList());
        when(marketEnvironmentSnapshotMapper.selectByAnalysisId(analysisId)).thenReturn(null);
    }
}
