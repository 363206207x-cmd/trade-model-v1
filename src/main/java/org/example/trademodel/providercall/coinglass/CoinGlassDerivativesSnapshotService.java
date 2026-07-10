package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
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

    public CoinGlassDerivativesSnapshotService(CoinGlassProperties properties,
                                               CoinGlassOpenInterestSnapshotService openInterestService,
                                               CoinGlassFundingSnapshotService fundingService,
                                               CoinGlassLiquidationSnapshotService liquidationService,
                                               CoinGlassLongShortSnapshotService longShortService,
                                               CoinGlassDerivativesSnapshotAssembler assembler) {
        this.properties = properties;
        this.openInterestService = openInterestService;
        this.fundingService = fundingService;
        this.liquidationService = liquidationService;
        this.longShortService = longShortService;
        this.assembler = assembler;
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
}
