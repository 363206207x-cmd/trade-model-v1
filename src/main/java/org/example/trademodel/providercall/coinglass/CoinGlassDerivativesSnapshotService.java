package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderSnapshotMetadata;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CoinGlassDerivativesSnapshotService {
    private final CoinGlassProperties properties;
    private final CoinGlassOpenInterestSnapshotService openInterestService;
    private final CoinGlassFundingSnapshotService fundingService;
    private final CoinGlassLiquidationSnapshotService liquidationService;
    private final CoinGlassLongShortSnapshotService longShortService;
    private final CoinGlassDerivativesSnapshotAssembler assembler;
    private final CoinGlassSymbolMapper symbolMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public CoinGlassDerivativesSnapshotService(CoinGlassProperties properties,
                                               CoinGlassOpenInterestSnapshotService openInterestService,
                                               CoinGlassFundingSnapshotService fundingService,
                                               CoinGlassLiquidationSnapshotService liquidationService,
                                               CoinGlassLongShortSnapshotService longShortService,
                                               CoinGlassDerivativesSnapshotAssembler assembler,
                                               CoinGlassSymbolMapper symbolMapper) {
        this.properties = properties;
        this.openInterestService = openInterestService;
        this.fundingService = fundingService;
        this.liquidationService = liquidationService;
        this.longShortService = longShortService;
        this.assembler = assembler;
        this.symbolMapper = symbolMapper;
    }

    public CoinGlassDerivativesSnapshotService(CoinGlassProperties properties,
                                               CoinGlassOpenInterestSnapshotService openInterestService,
                                               CoinGlassFundingSnapshotService fundingService,
                                               CoinGlassLiquidationSnapshotService liquidationService,
                                               CoinGlassLongShortSnapshotService longShortService,
                                               CoinGlassDerivativesSnapshotAssembler assembler) {
        this(properties, openInterestService, fundingService, liquidationService, longShortService,
                assembler, new CoinGlassSymbolMapper());
    }

    public ProviderCallResult<DerivativesRiskSnapshot> get(
            String symbol, AssetPriority priority, Duration freshTtl, String traceId) {
        Duration ttl = freshTtl == null
                ? Duration.ofSeconds(Math.max(1, properties.getFreshTtlSeconds())) : freshTtl;
        ProviderCallResult<CoinGlassOpenInterestSnapshot> oi =
                openInterestService.get(symbol, priority, ttl, traceId);
        ProviderCallResult<CoinGlassFundingSnapshot> funding =
                fundingService.get(symbol, priority, ttl, traceId);
        ProviderCallResult<CoinGlassLiquidationSnapshot> liquidation =
                liquidationService.get(symbol, priority, ttl, traceId);
        ProviderCallResult<CoinGlassLongShortSnapshot> longShort =
                longShortService.get(symbol, priority, ttl, traceId);
        return assembler.assemble(symbol, traceId, oi, funding, liquidation, longShort);
    }

    public ProviderCallResult<DerivativesRiskSnapshot> get(
            CanonicalInstrumentId canonicalInstrumentId,
            AssetPriority priority,
            Duration freshTtl,
            String traceId) {
        try {
            CoinGlassSymbolMapper.CoinGlassSymbol mapping = symbolMapper.map(canonicalInstrumentId);
            return get(mapping.pairSymbol(), priority, freshTtl, traceId);
        } catch (IllegalArgumentException invalid) {
            java.time.Instant now = java.time.Instant.now();
            String symbol = canonicalInstrumentId == null ? "UNMAPPED"
                    : canonicalInstrumentId.baseAsset() + canonicalInstrumentId.quoteAsset();
            ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata("COINGLASS",
                    ProviderDatasetType.DERIVATIVES, canonicalInstrumentId, symbol, "GLOBAL", null,
                    now, now, 0L, UnifiedSourceStatus.NOT_CONFIGURED,
                    SnapshotFreshnessStatus.UNAVAILABLE, traceId, "UNMAPPED", "UNMAPPED",
                    false, false, "DERIVATIVES_REQUIRE_PERPETUAL_INSTRUMENT",
                    java.util.List.of("DERIVATIVES_REQUIRE_PERPETUAL_INSTRUMENT"));
            return new ProviderCallResult<>(null, metadata, null);
        }
    }

    /**
     * Reads only coordinator cache entries. This method never invokes a CoinGlass adapter.
     */
    public ProviderCallResult<DerivativesRiskSnapshot> peek(
            String symbol, AssetPriority priority, Duration freshTtl, String traceId) {
        Duration ttl = freshTtl == null
                ? Duration.ofSeconds(Math.max(1, properties.getFreshTtlSeconds())) : freshTtl;
        ProviderCallResult<CoinGlassOpenInterestSnapshot> oi =
                openInterestService.peek(symbol, priority, ttl, traceId);
        ProviderCallResult<CoinGlassFundingSnapshot> funding =
                fundingService.peek(symbol, priority, ttl, traceId);
        ProviderCallResult<CoinGlassLiquidationSnapshot> liquidation =
                liquidationService.peek(symbol, priority, ttl, traceId);
        ProviderCallResult<CoinGlassLongShortSnapshot> longShort =
                longShortService.peek(symbol, priority, ttl, traceId);
        return assembler.assemble(symbol, traceId, oi, funding, liquidation, longShort);
    }
}
