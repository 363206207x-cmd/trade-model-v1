package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.opportunitylog.OpportunityLogStatus;
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
class OpportunityLogMapperIntegrationTest {
    @Autowired
    private OpportunityLogMapper opportunityLogMapper;

    @Test
    void insertSelectAndQueryPersistOpportunitySafetyFields() {
        OpportunityLogDO row = row();

        opportunityLogMapper.insert(row);

        OpportunityLogDO persisted = opportunityLogMapper.selectByOpportunityId("opp-it-1");
        assertThat(persisted.getOpportunityKey()).isEqualTo("ana-it:dec-it");
        assertThat(persisted.getLifecycleStatus()).isEqualTo(OpportunityLogStatus.PENDING_EVALUATION);
        assertThat(persisted.getEntryReference()).isEqualByComparingTo("100.00000000");
        assertThat(persisted.getReviewOnly()).isTrue();
        assertThat(persisted.getManualReviewOnly()).isTrue();
        assertThat(persisted.getNotTradeInstruction()).isTrue();
        assertThat(persisted.getNotExecutable()).isTrue();
        assertThat(persisted.getNotAutoTrading()).isTrue();
        assertThat(persisted.getNotOrderExecution()).isTrue();
        assertThat(persisted.getNotUserPositionCreation()).isTrue();
        assertThat(persisted.getNotUserPositionMutation()).isTrue();
        assertThat(persisted.getNotPushSend()).isTrue();
        assertThat(persisted.getNotExternalChannel()).isTrue();

        List<OpportunityLogDO> rows = opportunityLogMapper.query("ana-it", null, "plan-it",
                "BTCUSDT", null, OpportunityLogStatus.PENDING_EVALUATION,
                LocalDateTime.of(2026, 6, 23, 9, 0),
                LocalDateTime.of(2026, 6, 23, 11, 0),
                10);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getOpportunityId()).isEqualTo("opp-it-1");
    }

    private static OpportunityLogDO row() {
        OpportunityLogDO row = new OpportunityLogDO();
        row.setOpportunityId("opp-it-1");
        row.setOpportunityKey("ana-it:dec-it");
        row.setAnalysisId("ana-it");
        row.setDecisionId("dec-it");
        row.setExecutionPlanId("plan-it");
        row.setSymbol("BTCUSDT");
        row.setTimeframe("1h");
        row.setDirection("LONG");
        row.setLifecycleStatus(OpportunityLogStatus.PENDING_EVALUATION);
        row.setAnchorTime(LocalDateTime.of(2026, 6, 23, 10, 0));
        row.setEntryReference(new BigDecimal("100"));
        row.setTargetPrice(new BigDecimal("120"));
        row.setInvalidationPrice(new BigDecimal("90"));
        row.setTargetHit(false);
        row.setInvalidationHit(false);
        row.setPushPresent(false);
        row.setRiskBlockedEvidence(false);
        row.setUserPositionPresent(false);
        row.setSourceType("AUTHORITATIVE_ANALYSIS");
        row.setTraceId("trace-it");
        row.setCreatedAt(LocalDateTime.of(2026, 6, 23, 10, 0));
        row.setUpdatedAt(LocalDateTime.of(2026, 6, 23, 10, 0));
        return row;
    }
}
