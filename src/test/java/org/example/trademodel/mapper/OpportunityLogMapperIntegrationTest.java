package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.opportunitylog.OpportunityLogCountRow;
import org.example.trademodel.opportunitylog.OpportunityLogStatsDTO;
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

    @Test
    void aggregateStatsCoversFullRangeBeyondTwoHundredRowsAndKeepsFilters() {
        LocalDateTime base = LocalDateTime.of(2026, 6, 23, 10, 0);
        for (int i = 0; i < 205; i++) {
            OpportunityLogDO row = row("opp-bulk-" + i, "ana-bulk-" + i + ":dec-bulk-" + i,
                    "ana-bulk-" + i, "dec-bulk-" + i, "BTCUSDT", base.plusMinutes(i));
            row.setLifecycleStatus(OpportunityLogStatus.RESOLVED);
            row.setOpportunityStatus(i % 2 == 0 ? OpportunityLogStatus.MISSED_VALID : OpportunityLogStatus.MISSED_INVALID);
            row.setHitOrder(i % 2 == 0 ? OpportunityLogStatus.TARGET_FIRST : OpportunityLogStatus.INVALIDATION_FIRST);
            row.setMfeRatio(new BigDecimal("0.10"));
            row.setMaeRatio(new BigDecimal("0.02"));
            opportunityLogMapper.insert(row);
        }
        OpportunityLogDO eth = row("opp-eth-1", "ana-eth:dec-eth", "ana-eth", "dec-eth",
                "ETHUSDT", base.plusMinutes(1));
        eth.setLifecycleStatus(OpportunityLogStatus.RESOLVED);
        eth.setOpportunityStatus(OpportunityLogStatus.EXECUTED_VALID);
        eth.setHitOrder(OpportunityLogStatus.TARGET_FIRST);
        eth.setMfeRatio(new BigDecimal("0.90"));
        eth.setMaeRatio(new BigDecimal("0.30"));
        opportunityLogMapper.insert(eth);

        OpportunityLogStatsDTO stats = opportunityLogMapper.aggregateStats("BTCUSDT", base, base.plusMinutes(204));
        List<OpportunityLogCountRow> statusCounts = opportunityLogMapper.countByStatus("BTCUSDT", base, base.plusMinutes(204));
        List<OpportunityLogCountRow> sourceCounts = opportunityLogMapper.countBySource("BTCUSDT", base, base.plusMinutes(204));

        assertThat(stats.getTotalCount()).isEqualTo(205);
        assertThat(stats.getResolvedCount()).isEqualTo(205);
        assertThat(stats.getMissedValidCount()).isEqualTo(103);
        assertThat(stats.getMissedInvalidCount()).isEqualTo(102);
        assertThat(stats.getTargetFirstCount()).isEqualTo(103);
        assertThat(stats.getInvalidationFirstCount()).isEqualTo(102);
        assertThat(stats.getAverageMfeRatio()).isEqualByComparingTo("0.10000000");
        assertThat(stats.getAverageMaeRatio()).isEqualByComparingTo("0.02000000");
        assertThat(stats.getMaxMfeRatio()).isEqualByComparingTo("0.1000000000");
        assertThat(statusCounts).extracting(OpportunityLogCountRow::getCount)
                .containsExactlyInAnyOrder(103, 102);
        assertThat(sourceCounts).singleElement()
                .satisfies(row -> {
                    assertThat(row.getName()).isEqualTo("AUTHORITATIVE_ANALYSIS");
                    assertThat(row.getCount()).isEqualTo(205);
                });
    }

    private static OpportunityLogDO row() {
        return row("opp-it-1", "ana-it:dec-it", "ana-it", "dec-it", "BTCUSDT",
                LocalDateTime.of(2026, 6, 23, 10, 0));
    }

    private static OpportunityLogDO row(String opportunityId,
                                        String opportunityKey,
                                        String analysisId,
                                        String decisionId,
                                        String symbol,
                                        LocalDateTime anchorTime) {
        OpportunityLogDO row = new OpportunityLogDO();
        row.setOpportunityId(opportunityId);
        row.setOpportunityKey(opportunityKey);
        row.setAnalysisId(analysisId);
        row.setDecisionId(decisionId);
        row.setExecutionPlanId("plan-it");
        row.setSymbol(symbol);
        row.setTimeframe("1h");
        row.setDirection("LONG");
        row.setLifecycleStatus(OpportunityLogStatus.PENDING_EVALUATION);
        row.setAnchorTime(anchorTime);
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
        row.setCreatedAt(anchorTime);
        row.setUpdatedAt(anchorTime);
        return row;
    }
}
