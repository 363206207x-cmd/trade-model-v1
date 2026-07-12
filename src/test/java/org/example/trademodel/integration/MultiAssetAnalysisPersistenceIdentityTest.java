package org.example.trademodel.integration;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.analysisrun.AnalysisPersistenceIds;
import org.example.trademodel.analysisrun.AnalysisRunIds;
import org.example.trademodel.derivatives.DerivativesBusinessAssessment;
import org.example.trademodel.derivatives.DerivativesBusinessInput;
import org.example.trademodel.derivatives.DerivativesBusinessIntegrationService;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.EvidenceItemDO;
import org.example.trademodel.entity.ScoreItemDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.vo.EvidenceItemVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class MultiAssetAnalysisPersistenceIdentityTest {
    private static final List<String> SYMBOLS = List.of(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT");

    @Autowired private AnalysisRunMapper analysisRunMapper;
    @Autowired private EvidenceItemMapper evidenceItemMapper;
    @Autowired private ScoreItemMapper scoreItemMapper;
    @Autowired private DecisionResultMapper decisionResultMapper;
    @Autowired private DerivativesBusinessIntegrationService derivativesBusinessIntegrationService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void multiAssetCyclePersistsIndependentRunsSnapshotsEvidenceScoresAndDecisions() {
        String cycleId = "cycle-" + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime sameAnalysisTime = LocalDateTime.of(2026, 7, 13, 2, 20);
        List<String> analysisIds = new ArrayList<>();
        Set<String> evidenceIds = new HashSet<>();
        Set<String> scoreIds = new HashSet<>();
        Set<String> decisionIds = new HashSet<>();

        for (String symbol : SYMBOLS) {
            String analysisId = AnalysisRunIds.analysisId();
            String traceId = AnalysisRunIds.traceId();
            analysisIds.add(analysisId);
            analysisRunMapper.insert(successfulRun(
                    analysisId, traceId, symbol, cycleId, sameAnalysisTime));

            DerivativesBusinessAssessment assessment = derivativesBusinessIntegrationService.evaluate(
                    derivativesInput(symbol, analysisId, traceId));
            EvidenceItemVO evidence = derivativesBusinessIntegrationService.toEvidenceVos(assessment).get(0);
            EvidenceItemDO evidenceRow = evidenceRow(evidence, analysisId);
            evidenceItemMapper.insert(evidenceRow);
            evidenceIds.add(evidenceRow.getEvidenceId());

            ScoreItemDO score = scoreRow(analysisId, symbol);
            scoreItemMapper.insert(score);
            scoreIds.add(score.getScoreId());

            DecisionResult decision = decisionRow(analysisId, symbol);
            decisionResultMapper.insert(decision);
            decisionIds.add(decision.getDecisionId());
        }

        assertThat(analysisIds).hasSize(6).doesNotHaveDuplicates();
        assertThat(evidenceIds).hasSize(6);
        assertThat(scoreIds).hasSize(6);
        assertThat(decisionIds).hasSize(6);
        for (int i = 0; i < SYMBOLS.size(); i++) {
            String analysisId = analysisIds.get(i);
            String symbol = SYMBOLS.get(i);
            AnalysisRunDO run = analysisRunMapper.selectById(analysisId);
            assertThat(run).isNotNull();
            assertThat(run.getInputSnapshotJson()).contains("\"symbol\":\"" + symbol + "\"");
            assertThat(analysisRunMapper.selectEvidenceIdsByAnalysisId(analysisId)).hasSize(1);
            assertThat(analysisRunMapper.selectScoreIdsByAnalysisId(analysisId)).hasSize(1);
            assertThat(analysisRunMapper.selectDecisionIdsByAnalysisId(analysisId)).hasSize(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT analysis_id FROM tm_evidence_item WHERE evidence_id = ?",
                    String.class, analysisRunMapper.selectEvidenceIdsByAnalysisId(analysisId).get(0)))
                    .isEqualTo(analysisId);
        }
    }

    private static AnalysisRunDO successfulRun(String analysisId, String traceId, String symbol,
                                                String cycleId, LocalDateTime analysisTime) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setSymbol(symbol);
        run.setTimeframe("5m");
        run.setAnalysisTime(analysisTime);
        run.setRuleVersion("v1.0");
        run.setDataQualityScore(80);
        run.setTraceId(traceId);
        run.setStatus("SUCCESS");
        run.setIdempotencyKey(cycleId + "-" + symbol);
        run.setRequestId("req-" + symbol);
        run.setTriggerType("SCHEDULED");
        run.setTriggerReference(cycleId);
        run.setParentTraceId(cycleId);
        run.setInputSnapshotJson("{\"symbol\":\"" + symbol + "\",\"cycleId\":\"" + cycleId + "\"}");
        run.setInputSnapshotHash("hash-" + symbol);
        run.setAttemptCount(1);
        run.setStartedAt(analysisTime);
        run.setCompletedAt(analysisTime.plusSeconds(1));
        run.setCreatedAt(analysisTime);
        run.setUpdatedAt(analysisTime.plusSeconds(1));
        run.setVersionNo(1);
        return run;
    }

    private static DerivativesBusinessInput derivativesInput(String symbol, String analysisId, String traceId) {
        return new DerivativesBusinessInput(symbol, "NEUTRAL", new BigDecimal("100"), new BigDecimal("100"),
                true, Map.of("5m", "NEUTRAL", "15m", "NEUTRAL", "1h", "NEUTRAL", "4h", "NEUTRAL"),
                true, 80, true, false, false, null, null, traceId, analysisId, "v1.0");
    }

    private static EvidenceItemDO evidenceRow(EvidenceItemVO evidence, String analysisId) {
        EvidenceItemDO row = new EvidenceItemDO();
        row.setEvidenceId(evidence.getEvidenceId());
        row.setAnalysisId(analysisId);
        row.setEvidenceType(evidence.getEvidenceType());
        row.setDescription(evidence.getDescription());
        row.setDirection(evidence.getDirection());
        row.setStrength(evidence.getStrength());
        row.setConfidence(evidence.getConfidence());
        row.setSource(evidence.getSource());
        row.setSourceProvider(evidence.getSourceProvider());
        row.setSourceReference(evidence.getSourceReference());
        row.setSourceTraceId(evidence.getSourceTraceId());
        row.setCreateTime(LocalDateTime.now());
        return row;
    }

    private static ScoreItemDO scoreRow(String analysisId, String symbol) {
        ScoreItemDO row = new ScoreItemDO();
        row.setScoreId(AnalysisPersistenceIds.scoreId());
        row.setAnalysisId(analysisId);
        row.setScoreType("MULTI_ASSET_TEST_SCORE");
        row.setScoreValue(50.0);
        row.setWeight(1.0);
        row.setDirection("NEUTRAL");
        row.setDescription(symbol);
        return row;
    }

    private static DecisionResult decisionRow(String analysisId, String symbol) {
        DecisionResult row = new DecisionResult();
        row.setDecisionId(AnalysisPersistenceIds.decisionId());
        row.setAnalysisId(analysisId);
        row.setSymbol(symbol);
        row.setMarketBiasHierarchy("NEUTRAL");
        row.setIsWorthOpening(false);
        row.setCreateTime(LocalDateTime.now());
        return row;
    }
}
