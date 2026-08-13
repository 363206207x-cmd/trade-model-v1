package org.example.trademodel.positionmonitorlog;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class PositionMonitorLogMapperIntegrationTest {
    @Autowired
    private UserPositionMapper userPositionMapper;
    @Autowired
    private PositionMonitorLogMapper positionMonitorLogMapper;

    @Test
    void insertSelectAndListMonitorLogsPreserveFieldsAndSortDescending() {
        UserPositionDO position = userPosition("OPEN", LocalDateTime.of(2026, 6, 22, 8, 0));
        userPositionMapper.insert(position);

        PositionMonitorLogDO older = log(position.getId(), "ana-p0-4", "LOGIC_VALID", "LOW", "CONTINUE_HOLD",
                LocalDateTime.of(2026, 6, 22, 8, 10));
        PositionMonitorLogDO newer = log(position.getId(), "ana-p0-4", "HIGH_RISK_OBSERVATION", "HIGH",
                "REDUCE_POSITION",
                LocalDateTime.of(2026, 6, 22, 8, 20));

        positionMonitorLogMapper.insert(older);
        positionMonitorLogMapper.insert(newer);

        assertThat(older.getLogId()).isNotNull();
        PositionMonitorLogDO persisted = positionMonitorLogMapper.selectById(older.getLogId());
        assertThat(persisted.getPositionId()).isEqualTo(position.getId());
        assertThat(persisted.getAnalysisId()).isEqualTo("ana-p0-4");
        assertThat(persisted.getExecutionPlanId()).isEqualTo("plan-p0-4");
        assertThat(persisted.getCurrentPrice()).isEqualByComparingTo("111.25000000");
        assertThat(persisted.getLogicStatus()).isNull();
        assertThat(persisted.getEntryLogicStatus()).isEqualTo("STILL_VALID");
        assertThat(persisted.getMonitorConclusion()).isEqualTo("LOGIC_VALID");
        assertThat(persisted.getReversalStatus()).isEqualTo("NO_REVERSAL");
        assertThat(persisted.getRiskChangeReason()).isEqualTo("NO_CLEAR_RISK_FACTOR");
        assertThat(persisted.getRiskLevel()).isEqualTo("LOW");
        assertThat(persisted.getSuggestedAction()).isEqualTo("CONTINUE_HOLD");
        assertThat(persisted.getMonitorSourceStatus()).isEqualTo("VERIFIED");
        assertThat(persisted.getMarkPriceSource()).isEqualTo("TEST_MARK_PRICE");
        assertThat(persisted.getEvidenceSnapshot()).contains("evidence");
        assertThat(persisted.getScoreSnapshot()).contains("score");
        assertThat(persisted.getDecisionSnapshot()).contains("decision");
        assertThat(persisted.getRiskSnapshot()).contains("risk");
        assertThat(persisted.getTraceId()).isEqualTo("trace-p0-4");

        List<PositionMonitorLogDO> byPosition = positionMonitorLogMapper.listByPositionId(position.getId(), 10);
        List<PositionMonitorLogDO> byAnalysis = positionMonitorLogMapper.listByAnalysisId("ana-p0-4", 10);

        assertThat(byPosition).extracting(PositionMonitorLogDO::getLogId)
                .containsExactly(newer.getLogId(), older.getLogId());
        assertThat(byAnalysis).extracting(PositionMonitorLogDO::getLogId)
                .containsExactly(newer.getLogId(), older.getLogId());
    }

    @Test
    void untrustedRowsPersistRawObservationOnlyAndDatabaseRejectsInventedSemantics() {
        UserPositionDO position = userPosition("OPEN", LocalDateTime.of(2026, 6, 22, 9, 0));
        userPositionMapper.insert(position);
        LocalDateTime observedAt = LocalDateTime.of(2026, 6, 22, 9, 10);
        PositionMonitorLogDO pending = new PositionMonitorLogDO();
        pending.setPositionId(position.getId());
        pending.setAnalysisId("UNVERIFIED_POSITION_SOURCE");
        pending.setCurrentPrice(new BigDecimal("112.25"));
        pending.setMonitorSourceStatus("PENDING_VERIFICATION");
        pending.setObservedAt(observedAt);
        pending.setFreshUntil(observedAt);
        pending.setCreatedAt(observedAt);

        assertThat(positionMonitorLogMapper.insert(pending)).isEqualTo(1);
        PositionMonitorLogDO persisted = positionMonitorLogMapper.selectById(pending.getLogId());
        assertThat(persisted.getCurrentPrice()).isEqualByComparingTo("112.25");
        assertThat(persisted.getMonitorConclusion()).isNull();
        assertThat(persisted.getReversalStatus()).isNull();
        assertThat(persisted.getRiskLevel()).isNull();
        assertThat(persisted.getSuggestedAction()).isNull();

        PositionMonitorLogDO invalid = log(position.getId(), "ana-invalid", "LOGIC_VALID", "LOW",
                "CONTINUE_HOLD", observedAt.plusMinutes(1));
        invalid.setMonitorSourceStatus("INVALID");
        assertThatThrownBy(() -> positionMonitorLogMapper.insert(invalid))
                .hasMessageContaining("CK_TM_POSITION_MONITOR_LOG_TRUSTED_PAYLOAD");
    }

    private static PositionMonitorLogDO log(Long positionId, String analysisId, String monitorConclusion,
                                            String riskLevel, String suggestedAction, LocalDateTime createdAt) {
        PositionMonitorLogDO row = new PositionMonitorLogDO();
        row.setPositionId(positionId);
        row.setAnalysisId(analysisId);
        row.setExecutionPlanId("plan-p0-4");
        row.setCurrentPrice(new BigDecimal("111.25"));
        row.setMarkPriceSource("TEST_MARK_PRICE");
        row.setEntryLogicStatus("LOGIC_VALID".equals(monitorConclusion) ? "STILL_VALID" : "WEAKENED");
        row.setMonitorConclusion(monitorConclusion);
        row.setReversalStatus("NO_REVERSAL");
        row.setRiskChangeReason("LOGIC_VALID".equals(monitorConclusion)
                ? "NO_CLEAR_RISK_FACTOR" : "OPPOSING_EVIDENCE_INCREASED");
        row.setRiskLevel(riskLevel);
        row.setRiskTrend("STABLE");
        row.setSuggestedAction(suggestedAction);
        row.setMonitorSourceStatus("VERIFIED");
        row.setObservedAt(createdAt.minusMinutes(1));
        row.setFreshUntil(createdAt.plusMinutes(5));
        row.setReason("manual review note");
        row.setEvidenceSnapshot("{\"evidence\":\"stable\"}");
        row.setScoreSnapshot("{\"score\":70}");
        row.setDecisionSnapshot("{\"decision\":\"watch\"}");
        row.setRiskSnapshot("{\"risk\":\"guarded\"}");
        row.setTraceId("trace-p0-4");
        row.setCreatedAt(createdAt);
        return row;
    }

    private static UserPositionDO userPosition(String status, LocalDateTime openedAt) {
        UserPositionDO row = new UserPositionDO();
        row.setAssetSymbol("BTCUSDT");
        row.setSide("LONG");
        row.setStatus(status);
        row.setEntryPrice(new BigDecimal("100.50"));
        row.setQuantity(new BigDecimal("0.25"));
        row.setLeverage(new BigDecimal("2"));
        row.setOpenedAt(openedAt);
        row.setSourceType("MANUAL_INDEPENDENT");
        row.setManualReviewRequired(true);
        row.setNotTradeInstruction(true);
        row.setNotAutoTrading(true);
        row.setNotOrderExecution(true);
        row.setNotPositionSync(true);
        row.setCreatedAt(openedAt);
        row.setUpdatedAt(openedAt);
        return row;
    }
}
