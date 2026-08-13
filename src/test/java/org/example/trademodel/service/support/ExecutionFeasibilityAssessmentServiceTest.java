package org.example.trademodel.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.analysisrun.AnalysisExecutionContext;
import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.dto.planboundary.BoundaryEntryDTO;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderSnapshotMetadata;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionFeasibilityAssessmentServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-12T10:00:00Z");
    private static final LocalDateTime LOCAL_NOW = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

    private final FundamentalAiV41Properties properties = FundamentalAiV41Properties.contractFixture();
    private final ExecutionFeasibilityAssessmentService service =
            new ExecutionFeasibilityAssessmentService(null, null, properties,
                    new ObjectMapper().findAndRegisterModules(), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void freshRealTopOfBookInsideEntryZoneBecomesVerified() {
        ExecutionPlanVO plan = new ExecutionPlanVO();

        ExecutionFeasibilityContract.Assessment assessment = service.applyProviderResult(
                plan, context(), entry("99", "101"), "LONG",
                result("99.95", "200", "100.05", "200",
                        UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH, false),
                properties.getExecutionFeasibility(), LOCAL_NOW);

        assertThat(assessment.allowed()).isTrue();
        assertThat(assessment.status()).isEqualTo(ExecutionFeasibilityContract.VERIFIED);
        assertThat(plan.getExecutionFeasibilityStatus()).isEqualTo(ExecutionFeasibilityContract.VERIFIED);
        assertThat(plan.getRiskActionGuardReady()).isTrue();
        assertThat(plan.getExecutionFeasibilitySourceRefsJson())
                .contains("analysis-feasibility", "BINANCE", "sideNotional", "TOP_OF_BOOK_SIDE_NOTIONAL");
    }

    @Test
    void staleSourceFailsClosedEvenWhenPricesExist() {
        ExecutionPlanVO plan = new ExecutionPlanVO();

        ExecutionFeasibilityContract.Assessment assessment = service.applyProviderResult(
                plan, context(), entry("99", "101"), "LONG",
                result("99.95", "200", "100.05", "200",
                        UnifiedSourceStatus.STALE, SnapshotFreshnessStatus.STALE_READABLE, false),
                properties.getExecutionFeasibility(), LOCAL_NOW);

        assertThat(assessment.allowed()).isFalse();
        assertThat(assessment.status()).isEqualTo(ExecutionFeasibilityContract.STALE);
        assertThat(plan.getRiskActionGuardReady()).isFalse();
        assertThat(plan.getExecutionFeasibilityReason()).isEqualTo("EXECUTION_MARKET_SNAPSHOT_STALE");
    }

    @Test
    void fallbackOrMissingDepthCannotBecomeVerified() {
        ExecutionPlanVO fallbackPlan = new ExecutionPlanVO();
        ExecutionPlanVO missingDepthPlan = new ExecutionPlanVO();

        ExecutionFeasibilityContract.Assessment fallback = service.applyProviderResult(
                fallbackPlan, context(), entry("99", "101"), "LONG",
                result("99.95", "200", "100.05", "200",
                        UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH, true),
                properties.getExecutionFeasibility(), LOCAL_NOW);
        ExecutionFeasibilityContract.Assessment missingDepth = service.applyProviderResult(
                missingDepthPlan, context(), entry("99", "101"), "LONG",
                result("99.95", null, "100.05", null,
                        UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH, false),
                properties.getExecutionFeasibility(), LOCAL_NOW);

        assertThat(fallback.allowed()).isFalse();
        assertThat(fallback.status()).isEqualTo(ExecutionFeasibilityContract.UNAVAILABLE);
        assertThat(missingDepth.allowed()).isFalse();
        assertThat(missingDepth.status()).isEqualTo(ExecutionFeasibilityContract.UNAVAILABLE);
        assertThat(missingDepthPlan.getExecutionFeasibilityReason())
                .isEqualTo("EXECUTION_TOP_OF_BOOK_UNAVAILABLE");
    }

    @Test
    void triggerOutsideZoneRemainsPendingWithoutFabricatingExecutionReadiness() {
        ExecutionPlanVO plan = new ExecutionPlanVO();

        ExecutionFeasibilityContract.Assessment assessment = service.applyProviderResult(
                plan, context(), entry("99", "101"), "LONG",
                result("100.95", "200", "101.05", "200",
                        UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH, false),
                properties.getExecutionFeasibility(), LOCAL_NOW);

        assertThat(assessment.allowed()).isFalse();
        assertThat(assessment.status()).isEqualTo(ExecutionFeasibilityContract.PENDING);
        assertThat(plan.getTriggerStatus()).isEqualTo(ExecutionFeasibilityContract.PENDING);
        assertThat(plan.getRiskActionGuardReady()).isFalse();
        assertThat(plan.getExecutionFeasibilityReason()).isEqualTo("EXECUTION_TRIGGER_NOT_MET");
    }

    private static AnalysisExecutionContext context() {
        return new AnalysisExecutionContext(
                "analysis-feasibility", "trace-feasibility", "request-feasibility",
                "idem-feasibility", "BTCUSDT", "5m", LOCAL_NOW, LOCAL_NOW,
                "rules-v4.1", AnalysisRunTriggerType.ASSET_POOL_SCAN, "pool-scan",
                null, null, "{}", "hash", "lease", 1, 1, true,
                "USER", 7L, 11L, false);
    }

    private static BoundaryEntryDTO entry(String low, String high) {
        BoundaryEntryDTO entry = new BoundaryEntryDTO();
        entry.setEntryZoneLow(new BigDecimal(low));
        entry.setEntryZoneHigh(new BigDecimal(high));
        return entry;
    }

    private static ProviderCallResult<MarketPriceSnapshot> result(
            String bid, String bidQuantity, String ask, String askQuantity,
            UnifiedSourceStatus sourceStatus, SnapshotFreshnessStatus freshness, boolean fallback) {
        CanonicalInstrumentId instrument = new CanonicalInstrumentId(
                "BTC", "USDT", MarketType.PERPETUAL, "BINANCE", ContractType.LINEAR);
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata(
                "BINANCE", ProviderDatasetType.PRICE, instrument, "BTCUSDT", "GLOBAL",
                NOW.minusSeconds(5), NOW.minusSeconds(4), NOW.plusSeconds(25), 5L,
                sourceStatus, freshness, "trace-feasibility", "quote-key", "BINANCE_USDM_V1",
                false, fallback, null, List.of());
        BigDecimal bidValue = bid == null ? null : new BigDecimal(bid);
        BigDecimal askValue = ask == null ? null : new BigDecimal(ask);
        MarketPriceSnapshot quote = new MarketPriceSnapshot(
                "BTCUSDT", new BigDecimal("100"), bidValue,
                bidQuantity == null ? null : new BigDecimal(bidQuantity), askValue,
                askQuantity == null ? null : new BigDecimal(askQuantity),
                bidValue == null || askValue == null ? null : askValue.subtract(bidValue),
                new BigDecimal("110"), new BigDecimal("90"), BigDecimal.ZERO,
                "BINANCE", NOW.minusSeconds(4), metadata);
        return new ProviderCallResult<>(quote, metadata, null);
    }
}
