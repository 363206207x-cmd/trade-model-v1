package org.example.trademodel.integration;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.dto.ohlcv.OhlcvBarInput;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionBatch;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.entity.OpportunityLogDO;
import org.example.trademodel.mapper.OpportunityLogMapper;
import org.example.trademodel.opportunitylog.OpportunityLogDTO;
import org.example.trademodel.opportunitylog.OpportunityLogStatus;
import org.example.trademodel.service.OpportunityLogService;
import org.example.trademodel.service.PersistedOhlcvIngestionService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class OpportunityOhlcvRuntimeIntegrationTest {
    @Autowired
    private PersistedOhlcvIngestionService ingestionService;
    @Autowired
    private OpportunityLogMapper opportunityLogMapper;
    @Autowired
    private OpportunityLogService opportunityLogService;

    @Test
    void opportunityEvaluatorReadsRuntimeIngestedBars() {
        long closeTime = System.currentTimeMillis() - 1_000L;
        long openTime = closeTime - (5L * 60_000L) + 1L;
        OhlcvBarInput bar = new OhlcvBarInput("BTCUSDT", "5m", openTime, closeTime,
                new BigDecimal("100"), new BigDecimal("121"), new BigDecimal("99"), new BigDecimal("118"),
                new BigDecimal("1000"), new BigDecimal("100000"), 100L,
                new BigDecimal("500"), new BigDecimal("50000"), true);
        OhlcvIngestionResult ingestion = ingestionService.ingest(new OhlcvIngestionBatch(
                "BINANCE_PUBLIC", "SPOT", "/api/v3/klines", OhlcvSourceState.READY, Instant.now(),
                "runtime-integration-v1", 1, "trace-runtime-opportunity", "run-runtime-opportunity", List.of(bar)));
        assertThat(ingestion.ready()).isTrue();

        OpportunityLogDO opportunity = opportunity(openTime);
        opportunityLogMapper.insert(opportunity);

        OpportunityLogDTO evaluated = opportunityLogService.evaluateOpportunity(
                opportunity.getOpportunityId(), LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(closeTime + 1), ZoneId.systemDefault()));

        assertThat(evaluated.getOpportunityStatus()).isEqualTo(OpportunityLogStatus.MISSED_VALID);
        assertThat(evaluated.getMarketDataSource()).isEqualTo("BINANCE_PUBLIC");
        assertThat(evaluated.getMarketDataTraceId()).isEqualTo("trace-runtime-opportunity");
    }

    private static OpportunityLogDO opportunity(long anchorMs) {
        LocalDateTime anchor = LocalDateTime.ofInstant(Instant.ofEpochMilli(anchorMs), ZoneId.systemDefault());
        OpportunityLogDO row = new OpportunityLogDO();
        row.setOpportunityId("opp-runtime-ohlcv");
        row.setOpportunityKey("runtime-ohlcv-key");
        row.setAnalysisId("analysis-runtime-ohlcv");
        row.setSymbol("BTCUSDT");
        row.setTimeframe("5m");
        row.setDirection("LONG");
        row.setLifecycleStatus(OpportunityLogStatus.PENDING_EVALUATION);
        row.setAnchorTime(anchor);
        row.setEntryReference(new BigDecimal("100"));
        row.setTargetPrice(new BigDecimal("120"));
        row.setInvalidationPrice(new BigDecimal("90"));
        row.setTargetHit(false);
        row.setInvalidationHit(false);
        row.setPushPresent(false);
        row.setRiskBlockedEvidence(false);
        row.setUserPositionPresent(false);
        row.setSourceType("AUTHORITATIVE_ANALYSIS");
        row.setSourceReference("analysis-runtime-ohlcv");
        row.setTraceId("trace-opportunity");
        row.setCreatedAt(anchor);
        row.setUpdatedAt(anchor);
        return row;
    }
}
