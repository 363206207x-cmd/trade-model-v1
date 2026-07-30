package org.example.trademodel.service.impl;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.MarketEnvironmentSnapshotDO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewAggregateServiceImplMarketEnvironmentTest {

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
                scoreService
        );
    }

    @Test
    void getAggregateByAnalysisId_returnsMarketEnvironment_whenSnapshotExists() {
        String analysisId = "ana-env-1";
        stubMinimalAggregateDependencies(analysisId);
        MarketEnvironmentSnapshotDO snapshot = new MarketEnvironmentSnapshotDO();
        snapshot.setSummary("snapshot summary");
        snapshot.setSourceType("BINANCE_24H_HEURISTIC");
        snapshot.setEnvironmentType("TRENDING");
        snapshot.setRiskMode("MEDIUM");
        when(marketEnvironmentSnapshotMapper.selectByAnalysisId(analysisId)).thenReturn(snapshot);

        Optional<ReviewAggregateVO> result = service.getAggregateByAnalysisId(analysisId);

        assertThat(result).isPresent();
        assertThat(result.get().getMarketEnvironment()).isNotNull();
        assertThat(result.get().getMarketEnvironment().getSummary()).isEqualTo("snapshot summary");
        assertThat(result.get().getMarketEnvironment().getSourceType()).isEqualTo("BINANCE_24H_HEURISTIC");
        assertThat(result.get().getMarketEnvironment().getEnvironmentType()).isEqualTo("TRENDING");
        assertThat(result.get().getMarketEnvironment().getRiskMode()).isEqualTo("MEDIUM");
    }

    @Test
    void getAggregateByAnalysisId_returnsNullMarketEnvironment_whenSnapshotMissing() {
        String analysisId = "ana-env-2";
        stubMinimalAggregateDependencies(analysisId);
        when(marketEnvironmentSnapshotMapper.selectByAnalysisId(analysisId)).thenReturn(null);

        Optional<ReviewAggregateVO> result = service.getAggregateByAnalysisId(analysisId);

        assertThat(result).isPresent();
        assertThat(result.get().getMarketEnvironment()).isNull();
    }

    private void stubMinimalAggregateDependencies(String analysisId) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setSymbol("BTCUSDT");
        run.setTimeframe("1h");
        run.setAnalysisTime(LocalDateTime.of(2026, 4, 20, 12, 0));
        when(analysisRunMapper.selectById(analysisId)).thenReturn(run);

        when(missedOpportunityMapper.listByAnalysisId(analysisId)).thenReturn(Collections.emptyList());
        when(monitorAlertMapper.listByAnalysisId(analysisId)).thenReturn(Collections.emptyList());
        when(ruleVersionLogQueryService.listByAnalysisId(analysisId, 20)).thenReturn(Collections.emptyList());
        when(evidenceService.listTopEvidenceBriefByAnalysisId(anyString())).thenReturn(Collections.emptyList());
        when(scoreService.listTopScoreBriefByAnalysisId(anyString())).thenReturn(Collections.emptyList());
    }
}
