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

        PositionMonitorLogDO older = log(position.getId(), "ana-p0-4", "LOGIC_VALID", "LOW", "HOLD",
                LocalDateTime.of(2026, 6, 22, 8, 10));
        PositionMonitorLogDO newer = log(position.getId(), "ana-p0-4", "HIGH_RISK", "HIGH", "RISK_REVIEW",
                LocalDateTime.of(2026, 6, 22, 8, 20));

        positionMonitorLogMapper.insert(older);
        positionMonitorLogMapper.insert(newer);

        assertThat(older.getLogId()).isNotNull();
        PositionMonitorLogDO persisted = positionMonitorLogMapper.selectById(older.getLogId());
        assertThat(persisted.getPositionId()).isEqualTo(position.getId());
        assertThat(persisted.getAnalysisId()).isEqualTo("ana-p0-4");
        assertThat(persisted.getExecutionPlanId()).isEqualTo("plan-p0-4");
        assertThat(persisted.getCurrentPrice()).isEqualByComparingTo("111.25000000");
        assertThat(persisted.getLogicStatus()).isEqualTo("LOGIC_VALID");
        assertThat(persisted.getRiskLevel()).isEqualTo("LOW");
        assertThat(persisted.getSuggestedAction()).isEqualTo("HOLD");
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

    private static PositionMonitorLogDO log(Long positionId, String analysisId, String logicStatus,
                                            String riskLevel, String suggestedAction, LocalDateTime createdAt) {
        PositionMonitorLogDO row = new PositionMonitorLogDO();
        row.setPositionId(positionId);
        row.setAnalysisId(analysisId);
        row.setExecutionPlanId("plan-p0-4");
        row.setCurrentPrice(new BigDecimal("111.25"));
        row.setLogicStatus(logicStatus);
        row.setRiskLevel(riskLevel);
        row.setSuggestedAction(suggestedAction);
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
        row.setSourceType("MANUAL");
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
