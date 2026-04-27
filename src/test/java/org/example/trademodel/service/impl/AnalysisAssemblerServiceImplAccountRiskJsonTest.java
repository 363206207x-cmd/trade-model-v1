package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.mapper.AccountRiskSnapshotMapper;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.MarketEnvironmentSnapshotMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.service.AssetStateService;
import org.example.trademodel.service.DecisionEngineService;
import org.example.trademodel.service.EvidenceService;
import org.example.trademodel.service.HotResetService;
import org.example.trademodel.service.MissedOpportunityService;
import org.example.trademodel.service.MonitorAlertWriteService;
import org.example.trademodel.service.PlanService;
import org.example.trademodel.service.PushSnapshotService;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.service.ScoreService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisAssemblerServiceImplAccountRiskJsonTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private EvidenceService evidenceService;
    @Mock
    private ScoreService scoreService;
    @Mock
    private PlanService planService;
    @Mock
    private DecisionEngineService decisionEngineService;
    @Mock
    private AssetStateService assetStateService;
    @Mock
    private RuleConfigService ruleConfigService;
    @Mock
    private AnalysisRunMapper analysisRunMapper;
    @Mock
    private EvidenceItemMapper evidenceItemMapper;
    @Mock
    private ScoreItemMapper scoreItemMapper;
    @Mock
    private DecisionResultMapper decisionResultMapper;
    @Mock
    private ExecutionPlanMapper executionPlanMapper;
    @Mock
    private AccountRiskSnapshotMapper accountRiskSnapshotMapper;
    @Mock
    private MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper;
    @Mock
    private PushSnapshotService pushSnapshotService;
    @Mock
    private MonitorAlertWriteService monitorAlertWriteService;
    @Mock
    private HotResetService hotResetService;
    @Mock
    private MissedOpportunityService missedOpportunityService;

    private AnalysisAssemblerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnalysisAssemblerServiceImpl(
                evidenceService,
                scoreService,
                planService,
                decisionEngineService,
                null,
                assetStateService,
                ruleConfigService,
                analysisRunMapper,
                evidenceItemMapper,
                scoreItemMapper,
                decisionResultMapper,
                executionPlanMapper,
                accountRiskSnapshotMapper,
                marketEnvironmentSnapshotMapper,
                pushSnapshotService,
                monitorAlertWriteService,
                hotResetService,
                missedOpportunityService);
    }

    @Test
    void saveToDatabase_writesNonNullAccountRiskJson_withSevenKeys_whenSnapshotWrittenBeforeExecution() throws Exception {
        String analysisId = "ana-risk-json-1";
        Long snapshotId = 101L;
        when(pushSnapshotService.ensureAccountRiskSnapshot(any(), any(), any(), any())).thenReturn(snapshotId);
        when(accountRiskSnapshotMapper.selectLatestByAnalysisId(analysisId))
                .thenReturn(mockSnapshot());

        ReflectionTestUtils.invokeMethod(
                service,
                "saveToDatabase",
                mockAnalysis(analysisId),
                new ArrayList<>(),
                new ArrayList<>(),
                mockDecision(true, "price breaks decision support"),
                mockPlan(),
                "BINANCE_24H_HEURISTIC");

        ArgumentCaptor<ExecutionPlanDO> planCaptor = ArgumentCaptor.forClass(ExecutionPlanDO.class);
        verify(executionPlanMapper).insert(planCaptor.capture());
        String accountRiskJson = planCaptor.getValue().getAccountRiskJson();
        assertThat(accountRiskJson).isNotBlank();
        assertThat(planCaptor.getValue().getInvalidCondition()).isEqualTo("price breaks decision support");

        JsonNode node = JSON.readTree(accountRiskJson);
        Set<String> names = new HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        assertThat(names).containsExactlyInAnyOrder(
                "riskAllowed",
                "riskReasonCode",
                "riskReasonText",
                "positionExposure",
                "maxAllowedExposure",
                "snapshotSource",
                "snapshotVersion");
        assertThat(names).hasSize(7);

        InOrder inOrder = inOrder(pushSnapshotService, accountRiskSnapshotMapper, executionPlanMapper, pushSnapshotService);
        inOrder.verify(pushSnapshotService).ensureAccountRiskSnapshot(any(), any(), any(), any());
        inOrder.verify(pushSnapshotService).insertAuthoritativeSnapshot(any(), any(), any(), any(), eq(snapshotId));
        inOrder.verify(accountRiskSnapshotMapper).selectLatestByAnalysisId(analysisId);
        inOrder.verify(executionPlanMapper).insert(any(ExecutionPlanDO.class));
    }

    @Test
    void saveToDatabase_keepsExecutionInsert_whenNoSnapshot() {
        String analysisId = "ana-risk-json-2";
        when(pushSnapshotService.ensureAccountRiskSnapshot(any(), any(), any(), any())).thenReturn(null);
        when(accountRiskSnapshotMapper.selectLatestByAnalysisId(analysisId)).thenReturn(null);

        ReflectionTestUtils.invokeMethod(
                service,
                "saveToDatabase",
                mockAnalysis(analysisId),
                new ArrayList<>(),
                new ArrayList<>(),
                mockDecision(false, null),
                mockPlan(),
                "BINANCE_24H_HEURISTIC");

        ArgumentCaptor<ExecutionPlanDO> planCaptor = ArgumentCaptor.forClass(ExecutionPlanDO.class);
        verify(executionPlanMapper).insert(planCaptor.capture());
        assertThat(planCaptor.getValue().getAccountRiskJson()).isNull();
        assertThat(planCaptor.getValue().getInvalidCondition()).isNull();
        verify(pushSnapshotService).ensureAccountRiskSnapshot(any(), any(), any(), any());
        verify(pushSnapshotService).insertAuthoritativeSnapshot(any(), any(), any(), any(), eq(null));
        verify(monitorAlertWriteService).emitAfterAnalysisPersist(any(), any(), any());
    }

    @Test
    void saveToDatabase_whenNotWorthOpeningAndPlanPresent_stillWritesExecutionRiskJson() throws Exception {
        String analysisId = "ana-risk-json-3";
        when(pushSnapshotService.ensureAccountRiskSnapshot(any(), any(), any(), any())).thenReturn(202L);
        when(accountRiskSnapshotMapper.selectLatestByAnalysisId(analysisId)).thenReturn(mockDecisionNotWorthOpeningSnapshot());

        ReflectionTestUtils.invokeMethod(
                service,
                "saveToDatabase",
                mockAnalysis(analysisId),
                new ArrayList<>(),
                new ArrayList<>(),
                mockDecision(false, "decision says invalid after trend reversal"),
                mockPlan(),
                "BINANCE_24H_HEURISTIC");

        ArgumentCaptor<ExecutionPlanDO> planCaptor = ArgumentCaptor.forClass(ExecutionPlanDO.class);
        verify(executionPlanMapper).insert(planCaptor.capture());
        String accountRiskJson = planCaptor.getValue().getAccountRiskJson();
        assertThat(accountRiskJson).isNotBlank();
        assertThat(planCaptor.getValue().getInvalidCondition()).isEqualTo("decision says invalid after trend reversal");
        JsonNode jsonNode = JSON.readTree(accountRiskJson);
        assertThat(jsonNode.path("riskReasonCode").asText()).isEqualTo("DECISION_NOT_WORTH_OPENING");

        verify(pushSnapshotService).insertAuthoritativeSnapshot(any(), any(), any(), any(), eq(202L));
    }

    private static AssetAnalysisVO mockAnalysis(String analysisId) {
        AssetAnalysisVO vo = new AssetAnalysisVO();
        vo.setAnalysisId(analysisId);
        vo.setSymbol("BTCUSDT");
        vo.setTimeframe("1h");
        vo.setDataQualityScore(85);
        return vo;
    }

    private static ExecutionPlanVO mockPlan() {
        ExecutionPlanVO vo = new ExecutionPlanVO();
        vo.setPlanId("plan-risk-json");
        vo.setPlanMode("ADVISORY");
        vo.setRecommendedAction("WAIT");
        vo.setEntryZone("100-101");
        vo.setStopLoss("99");
        vo.setTakeProfitRules("tp");
        vo.setLeverageSuggestion("2x");
        vo.setPositionSuggestion("10%");
        return vo;
    }

    private static DecisionBundleVO mockDecision(boolean worthOpening, String invalidCondition) {
        DecisionBundleVO decision = new DecisionBundleVO();
        decision.setDecisionId("dec-risk-json");
        decision.setIsWorthOpening(worthOpening);
        decision.setRiskLevel("MEDIUM");
        decision.setAiPlanMode("ADVISORY");
        decision.setPushInvalidationSummary(invalidCondition);
        return decision;
    }

    private static TmAccountRiskSnapshotDO mockSnapshot() {
        TmAccountRiskSnapshotDO row = new TmAccountRiskSnapshotDO();
        row.setRiskAllowed(Boolean.TRUE);
        row.setRiskReasonCode("ACCOUNT_RISK_ALLOWED");
        row.setRiskReasonText("position exposure within threshold");
        row.setPositionExposure(new BigDecimal("0.1000"));
        row.setMaxAllowedExposure(new BigDecimal("0.3000"));
        row.setSnapshotSource("ROUND2_MINIMAL_DECISION_PLUS_PLAN_EXPOSURE");
        row.setSnapshotVersion(2);
        return row;
    }

    private static TmAccountRiskSnapshotDO mockDecisionNotWorthOpeningSnapshot() {
        TmAccountRiskSnapshotDO row = mockSnapshot();
        row.setRiskAllowed(Boolean.FALSE);
        row.setRiskReasonCode("DECISION_NOT_WORTH_OPENING");
        row.setRiskReasonText("decision.isWorthOpening=false");
        return row;
    }
}
