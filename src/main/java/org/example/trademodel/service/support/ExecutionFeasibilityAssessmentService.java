package org.example.trademodel.service.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.analysisrun.AnalysisExecutionContext;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.dto.planboundary.BoundaryEntryDTO;
import org.example.trademodel.dto.planboundary.SourceTraceBoundaryProducerResult;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderSnapshotMetadata;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies the frozen execution-feasibility gate from real provider data. This
 * is a read-only assessment and never authorizes or performs a trade.
 */
@Service
public class ExecutionFeasibilityAssessmentService {
    private static final BigDecimal TEN_THOUSAND = new BigDecimal("10000");

    private final MarketPriceSnapshotService marketPriceSnapshotService;
    private final ProviderSymbolMappingRegistry mappingRegistry;
    private final FundamentalAiV41Properties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ExecutionFeasibilityAssessmentService(MarketPriceSnapshotService marketPriceSnapshotService,
                                                  ProviderSymbolMappingRegistry mappingRegistry,
                                                  FundamentalAiV41Properties properties,
                                                  ObjectMapper objectMapper) {
        this(marketPriceSnapshotService, mappingRegistry, properties, objectMapper, Clock.systemUTC());
    }

    public ExecutionFeasibilityAssessmentService(MarketPriceSnapshotService marketPriceSnapshotService,
                                                  ProviderSymbolMappingRegistry mappingRegistry,
                                                  FundamentalAiV41Properties properties,
                                                  ObjectMapper objectMapper,
                                                  Clock clock) {
        this.marketPriceSnapshotService = marketPriceSnapshotService;
        this.mappingRegistry = mappingRegistry;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public ExecutionFeasibilityContract.Assessment assessAndApply(
            ExecutionPlanVO plan,
            AnalysisExecutionContext context,
            SourceTraceBoundaryProducerResult boundary,
            String direction) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (plan == null || context == null || boundary == null || !boundary.isBoundaryReady()
                || !boundary.isSourceTraceReady() || boundary.getEntry() == null) {
            return unavailable(plan, "EXECUTION_BOUNDARY_SOURCE_UNAVAILABLE", now);
        }
        String normalizedDirection = normalizeDirection(direction);
        if (normalizedDirection == null) {
            return unavailable(plan, "EXECUTION_DIRECTION_UNAVAILABLE", now);
        }
        CanonicalInstrumentId instrument;
        try {
            instrument = mappingRegistry.resolveConfiguredInstrument(context.getSymbol(),
                    MarketType.PERPETUAL, "BINANCE", ContractType.LINEAR);
        } catch (RuntimeException missingMapping) {
            return unavailable(plan, "EXECUTION_INSTRUMENT_MAPPING_UNAVAILABLE", now);
        }

        FundamentalAiV41Properties.ExecutionFeasibility config = properties.getExecutionFeasibility();
        ProviderCallResult<MarketPriceSnapshot> result;
        try {
            result = marketPriceSnapshotService.get(instrument, AssetPriority.P2_CANDIDATE,
                    Duration.ofSeconds(config.getQuoteFreshnessSeconds()), context.getTraceId());
        } catch (RuntimeException providerFailure) {
            return unavailable(plan, "EXECUTION_MARKET_PROVIDER_ERROR", now);
        }
        return applyProviderResult(plan, context, boundary.getEntry(), normalizedDirection,
                result, config, now);
    }

    ExecutionFeasibilityContract.Assessment applyProviderResult(
            ExecutionPlanVO plan,
            AnalysisExecutionContext context,
            BoundaryEntryDTO entry,
            String direction,
            ProviderCallResult<MarketPriceSnapshot> result,
            FundamentalAiV41Properties.ExecutionFeasibility config,
            LocalDateTime now) {
        if (result == null || result.metadata() == null || result.payload() == null) {
            return unavailable(plan, "EXECUTION_MARKET_SNAPSHOT_UNAVAILABLE", now);
        }
        ProviderSnapshotMetadata metadata = result.metadata();
        if (metadata.freshnessStatus() != SnapshotFreshnessStatus.FRESH) {
            return apply(plan, ExecutionFeasibilityContract.STALE,
                    ExecutionFeasibilityContract.STALE, ExecutionFeasibilityContract.STALE,
                    ExecutionFeasibilityContract.STALE, ExecutionFeasibilityContract.STALE,
                    "EXECUTION_MARKET_SNAPSHOT_STALE", metadata, null, now);
        }
        if (metadata.sourceStatus() != UnifiedSourceStatus.READY || metadata.fallbackUsed()) {
            return apply(plan, ExecutionFeasibilityContract.UNAVAILABLE,
                    ExecutionFeasibilityContract.UNAVAILABLE, ExecutionFeasibilityContract.UNAVAILABLE,
                    ExecutionFeasibilityContract.UNAVAILABLE, ExecutionFeasibilityContract.UNAVAILABLE,
                    "EXECUTION_MARKET_SOURCE_NOT_READY", metadata, null, now);
        }

        MarketPriceSnapshot quote = result.payload();
        if (!positive(quote.bidPrice()) || !positive(quote.bidQuantity())
                || !positive(quote.askPrice()) || !positive(quote.askQuantity())
                || quote.askPrice().compareTo(quote.bidPrice()) < 0) {
            return apply(plan, ExecutionFeasibilityContract.UNAVAILABLE,
                    ExecutionFeasibilityContract.UNAVAILABLE, ExecutionFeasibilityContract.UNAVAILABLE,
                    ExecutionFeasibilityContract.UNAVAILABLE, ExecutionFeasibilityContract.UNAVAILABLE,
                    "EXECUTION_TOP_OF_BOOK_UNAVAILABLE", metadata, quote, now);
        }
        if (entry == null || !positive(entry.getEntryZoneLow()) || !positive(entry.getEntryZoneHigh())
                || entry.getEntryZoneHigh().compareTo(entry.getEntryZoneLow()) < 0) {
            return apply(plan, ExecutionFeasibilityContract.UNAVAILABLE,
                    ExecutionFeasibilityContract.UNAVAILABLE, ExecutionFeasibilityContract.UNAVAILABLE,
                    ExecutionFeasibilityContract.UNAVAILABLE, ExecutionFeasibilityContract.UNAVAILABLE,
                    "EXECUTION_ENTRY_ZONE_UNAVAILABLE", metadata, quote, now);
        }

        BigDecimal mid = quote.bidPrice().add(quote.askPrice())
                .divide(new BigDecimal("2"), 12, RoundingMode.HALF_UP);
        BigDecimal spreadBps = quote.askPrice().subtract(quote.bidPrice())
                .divide(mid, 12, RoundingMode.HALF_UP).multiply(TEN_THOUSAND);
        BigDecimal sidePrice = "LONG".equals(direction) ? quote.askPrice() : quote.bidPrice();
        BigDecimal sideQuantity = "LONG".equals(direction) ? quote.askQuantity() : quote.bidQuantity();
        BigDecimal sideNotional = sidePrice.multiply(sideQuantity);
        BigDecimal driftBps = entryDriftBps(sidePrice, entry.getEntryZoneLow(), entry.getEntryZoneHigh());
        boolean triggerMet = sidePrice.compareTo(entry.getEntryZoneLow()) >= 0
                && sidePrice.compareTo(entry.getEntryZoneHigh()) <= 0;

        String slippage = spreadBps.compareTo(config.getMaxSpreadBps()) <= 0
                ? ExecutionFeasibilityContract.VERIFIED : ExecutionFeasibilityContract.INVALID;
        String depth = sideNotional.compareTo(config.getMinimumTopOfBookNotional()) >= 0
                ? ExecutionFeasibilityContract.VERIFIED : ExecutionFeasibilityContract.INVALID;
        String drift = driftBps.compareTo(config.getMaxEntryDriftBps()) <= 0
                ? ExecutionFeasibilityContract.VERIFIED : ExecutionFeasibilityContract.INVALID;
        String trigger = triggerMet ? ExecutionFeasibilityContract.VERIFIED
                : ExecutionFeasibilityContract.PENDING;
        String overall;
        String reason;
        if (ExecutionFeasibilityContract.INVALID.equals(slippage)) {
            overall = ExecutionFeasibilityContract.INVALID;
            reason = "EXECUTION_SPREAD_EXCEEDS_LIMIT";
        } else if (ExecutionFeasibilityContract.INVALID.equals(depth)) {
            overall = ExecutionFeasibilityContract.INVALID;
            reason = "EXECUTION_TOP_OF_BOOK_DEPTH_INSUFFICIENT";
        } else if (ExecutionFeasibilityContract.INVALID.equals(drift)) {
            overall = ExecutionFeasibilityContract.INVALID;
            reason = "EXECUTION_ENTRY_DRIFT_EXCEEDS_LIMIT";
        } else if (!triggerMet) {
            overall = ExecutionFeasibilityContract.PENDING;
            reason = "EXECUTION_TRIGGER_NOT_MET";
        } else {
            overall = ExecutionFeasibilityContract.VERIFIED;
            reason = null;
        }

        Map<String, Object> refs = sourceRefs(context, metadata, quote);
        refs.put("direction", direction);
        refs.put("sidePrice", sidePrice);
        refs.put("sideQuantity", sideQuantity);
        refs.put("sideNotional", sideNotional);
        refs.put("spreadBps", spreadBps);
        refs.put("entryZoneLow", entry.getEntryZoneLow());
        refs.put("entryZoneHigh", entry.getEntryZoneHigh());
        refs.put("entryDriftBps", driftBps);
        refs.put("maxSpreadBps", config.getMaxSpreadBps());
        refs.put("minimumTopOfBookNotional", config.getMinimumTopOfBookNotional());
        refs.put("maxEntryDriftBps", config.getMaxEntryDriftBps());
        refs.put("depthSemantics", "TOP_OF_BOOK_SIDE_NOTIONAL");
        return apply(plan, overall, slippage, depth, drift, trigger, reason,
                metadata, refs, now);
    }

    private ExecutionFeasibilityContract.Assessment unavailable(ExecutionPlanVO plan,
                                                                 String reason,
                                                                 LocalDateTime now) {
        return ExecutionFeasibilityContract.applyAssessment(plan,
                ExecutionFeasibilityContract.UNAVAILABLE,
                ExecutionFeasibilityContract.UNAVAILABLE,
                ExecutionFeasibilityContract.UNAVAILABLE,
                ExecutionFeasibilityContract.UNAVAILABLE,
                ExecutionFeasibilityContract.UNAVAILABLE,
                reason, null, null, null, now);
    }

    private ExecutionFeasibilityContract.Assessment apply(ExecutionPlanVO plan,
                                                           String status,
                                                           String slippage,
                                                           String depth,
                                                           String drift,
                                                           String trigger,
                                                           String reason,
                                                           ProviderSnapshotMetadata metadata,
                                                           Object sourceRefs,
                                                           LocalDateTime now) {
        Instant observed = metadata == null ? null
                : metadata.providerDataTime() != null ? metadata.providerDataTime() : metadata.fetchTime();
        Instant freshUntil = metadata == null ? null : metadata.expiresAt();
        return ExecutionFeasibilityContract.applyAssessment(plan, status, slippage, depth, drift, trigger,
                reason, utc(observed), utc(freshUntil), json(sourceRefs), now);
    }

    private Map<String, Object> sourceRefs(AnalysisExecutionContext context,
                                           ProviderSnapshotMetadata metadata,
                                           MarketPriceSnapshot quote) {
        Map<String, Object> refs = new LinkedHashMap<>();
        refs.put("analysisId", context.getAnalysisId());
        refs.put("traceId", context.getTraceId());
        refs.put("symbol", context.getSymbol());
        refs.put("timeframe", context.getTimeframe());
        refs.put("ruleVersion", context.getRuleVersion());
        refs.put("provider", metadata.provider());
        refs.put("providerSymbol", metadata.providerSymbol());
        refs.put("marketIdentity", metadata.canonicalInstrumentId() == null
                ? null : metadata.canonicalInstrumentId().canonical());
        refs.put("sourceVersion", metadata.sourceVersion());
        refs.put("providerDataTime", metadata.providerDataTime());
        refs.put("fetchTime", metadata.fetchTime());
        refs.put("expiresAt", metadata.expiresAt());
        refs.put("lastPrice", quote.lastPrice());
        refs.put("bidPrice", quote.bidPrice());
        refs.put("bidQuantity", quote.bidQuantity());
        refs.put("askPrice", quote.askPrice());
        refs.put("askQuantity", quote.askQuantity());
        return refs;
    }

    private String json(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private static BigDecimal entryDriftBps(BigDecimal price, BigDecimal low, BigDecimal high) {
        if (price.compareTo(low) < 0) {
            return low.subtract(price).divide(low, 12, RoundingMode.HALF_UP).multiply(TEN_THOUSAND);
        }
        if (price.compareTo(high) > 0) {
            return price.subtract(high).divide(high, 12, RoundingMode.HALF_UP).multiply(TEN_THOUSAND);
        }
        return BigDecimal.ZERO;
    }

    private static String normalizeDirection(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        return List.of("LONG", "SHORT").contains(normalized) ? normalized : null;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static LocalDateTime utc(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
