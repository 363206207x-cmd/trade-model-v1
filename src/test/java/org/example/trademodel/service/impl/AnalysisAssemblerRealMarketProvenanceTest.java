package org.example.trademodel.service.impl;

import org.example.trademodel.analysisrun.AnalysisExecutionContext;
import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.entity.*;
import org.example.trademodel.mapper.*;
import org.example.trademodel.market.PersistedRealMarketEnvironmentAssessment;
import org.example.trademodel.market.PersistedRealMarketEnvironmentService;
import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.service.*;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisAssemblerRealMarketProvenanceTest {

    @Test
    void analysisAssemblerAcceptsValidKrakenRuntimeContext() {
        EvidenceService evidenceService = mock(EvidenceService.class);
        RealMarketEnvironmentService quoteService = mock(RealMarketEnvironmentService.class);
        PersistedRealMarketEnvironmentService persistedService = mock(PersistedRealMarketEnvironmentService.class);
        when(quoteService.tryBuildFromRealQuote("BTCUSDT", "5m")).thenReturn(Optional.empty());
        when(persistedService.assess("BTCUSDT", "5m")).thenReturn(readyAssessment());
        when(evidenceService.buildEvidence(any(), any())).thenThrow(new IllegalStateException("EVIDENCE_REACHED"));
        AnalysisAssemblerServiceImpl assembler = assembler(evidenceService, quoteService, persistedService);

        assertThatThrownBy(() -> assembler.assemble(context()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("EVIDENCE_REACHED");
    }

    @Test
    void analysisAssemblerStillRejectsIncompleteContext() {
        EvidenceService evidenceService = mock(EvidenceService.class);
        RealMarketEnvironmentService quoteService = mock(RealMarketEnvironmentService.class);
        PersistedRealMarketEnvironmentService persistedService = mock(PersistedRealMarketEnvironmentService.class);
        when(quoteService.tryBuildFromRealQuote("BTCUSDT", "5m")).thenReturn(Optional.empty());
        when(persistedService.assess("BTCUSDT", "5m")).thenReturn(
                PersistedRealMarketEnvironmentAssessment.failed(
                        "REAL_MARKET_PROVENANCE_INCOMPLETE", Map.of()));
        AnalysisAssemblerServiceImpl assembler = assembler(evidenceService, quoteService, persistedService);

        assertThatThrownBy(() -> assembler.assemble(context()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("REAL_MARKET_ENVIRONMENT_REQUIRED");
    }

    @Test
    void analysisAssemblerFailsClosedWhenV41DecisionChainIsUnavailable() {
        AnalysisAssemblerServiceImpl assembler = new AnalysisAssemblerServiceImpl(
                mock(EvidenceService.class),
                mock(ScoreService.class),
                mock(PlanService.class),
                mock(DecisionEngineService.class),
                mock(RealMarketEnvironmentService.class),
                mock(AssetStateService.class),
                mock(RuleConfigService.class),
                mock(AnalysisRunMapper.class),
                mock(EvidenceItemMapper.class),
                mock(ScoreItemMapper.class),
                mock(DecisionResultMapper.class),
                mock(ExecutionPlanMapper.class),
                mock(AccountRiskSnapshotMapper.class),
                mock(MarketEnvironmentSnapshotMapper.class),
                mock(PushSnapshotService.class),
                mock(MonitorAlertWriteService.class),
                mock(HotResetService.class),
                mock(MissedOpportunityService.class),
                mock(OpportunityLogService.class));

        assertThatThrownBy(() -> assembler.assemble(context()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("V4_1_DECISION_CHAIN_REQUIRED");
    }

    private static AnalysisAssemblerServiceImpl assembler(
            EvidenceService evidenceService,
            RealMarketEnvironmentService quoteService,
            PersistedRealMarketEnvironmentService persistedService) {
        AnalysisAssemblerServiceImpl assembler = new AnalysisAssemblerServiceImpl(
                evidenceService,
                mock(ScoreService.class),
                mock(PlanService.class),
                mock(DecisionEngineService.class),
                quoteService,
                mock(AssetStateService.class),
                mock(RuleConfigService.class),
                mock(AnalysisRunMapper.class),
                mock(EvidenceItemMapper.class),
                mock(ScoreItemMapper.class),
                mock(DecisionResultMapper.class),
                mock(ExecutionPlanMapper.class),
                mock(AccountRiskSnapshotMapper.class),
                mock(MarketEnvironmentSnapshotMapper.class),
                mock(PushSnapshotService.class),
                mock(MonitorAlertWriteService.class),
                mock(HotResetService.class),
                mock(MissedOpportunityService.class),
                mock(OpportunityLogService.class));
        assembler.setRequireRealMarketEnvironment(true);
        assembler.setPersistedRealMarketEnvironmentService(persistedService);
        ReflectionTestUtils.setField(assembler, "decisionChainService", mock(DecisionChainService.class));
        return assembler;
    }

    private static PersistedRealMarketEnvironmentAssessment readyAssessment() {
        MarketEnvironmentVO environment = new MarketEnvironmentVO();
        environment.setSummary("Real persisted OHLCV (KRAKEN SPOT)");
        return new PersistedRealMarketEnvironmentAssessment(true, null, "KRAKEN",
                "KRAKEN_PERSISTED_OHLCV", environment, Map.of(), 400, 123L, List.of("trace-1"));
    }

    private static AnalysisExecutionContext context() {
        LocalDateTime time = LocalDateTime.of(2026, 7, 13, 2, 0);
        return new AnalysisExecutionContext(
                "analysis-real-1", "trace-real-1", "request-real-1", "key-real-1",
                "BTCUSDT", "5m", time, time, "v1.0", AnalysisRunTriggerType.SCHEDULED,
                "SCHEDULED:" + time, null, null, "{}", "hash-real-1", "lease-real-1",
                1, 1, true);
    }
}
