package org.example.trademodel.providercall.coinglass;

import org.springframework.beans.factory.annotation.Autowired;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class CoinGlassSymbolMapper {
    private static final Set<String> SUPPORTED = Set.of(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT");

    private final ProviderSymbolMappingRegistry registry;

    @Autowired
    public CoinGlassSymbolMapper(ProviderSymbolMappingRegistry registry) {
        this.registry = registry;
    }

    public CoinGlassSymbolMapper() {
        this.registry = null;
    }

    public CoinGlassSymbol map(String internalSymbol) {
        String normalized = internalSymbol == null ? "" : internalSymbol.trim().toUpperCase(Locale.ROOT);
        if (registry != null) {
            ProviderSymbolMapping mapping = registry.resolve("COINGLASS", normalized, MarketType.PERPETUAL);
            String compact = mapping.normalizedProviderSymbol();
            return new CoinGlassSymbol(mapping.providerSymbol(), mapping.canonicalInstrumentId().baseAsset(),
                    mapping.canonicalInstrumentId(), mapping.sourceVersion());
        }
        if (!SUPPORTED.contains(normalized)) {
            throw new IllegalArgumentException("unsupported CoinGlass symbol");
        }
        String base = normalized.substring(0, normalized.length() - 4);
        CanonicalInstrumentId canonical = new CanonicalInstrumentId(base, "USDT", MarketType.PERPETUAL,
                "BINANCE", ContractType.LINEAR);
        return new CoinGlassSymbol(normalized, base, canonical, "COINGLASS_MAPPING_V1");
    }

    public CoinGlassSymbol map(CanonicalInstrumentId canonicalInstrumentId) {
        if (canonicalInstrumentId == null
                || canonicalInstrumentId.marketType() != MarketType.PERPETUAL
                || canonicalInstrumentId.contractType() != ContractType.LINEAR) {
            throw new IllegalArgumentException("CoinGlass derivatives require a linear perpetual instrument");
        }
        if (registry != null) {
            ProviderSymbolMapping mapping = registry.resolve("COINGLASS", canonicalInstrumentId);
            return new CoinGlassSymbol(mapping.providerSymbol(), canonicalInstrumentId.baseAsset(),
                    mapping.canonicalInstrumentId(), mapping.sourceVersion());
        }
        String symbol = canonicalInstrumentId.baseAsset() + canonicalInstrumentId.quoteAsset();
        if (!SUPPORTED.contains(symbol)) throw new IllegalArgumentException("unsupported CoinGlass symbol");
        return new CoinGlassSymbol(symbol, canonicalInstrumentId.baseAsset(), canonicalInstrumentId,
                "COINGLASS_MAPPING_V1");
    }

    public record CoinGlassSymbol(String pairSymbol,
                                  String coinSymbol,
                                  CanonicalInstrumentId canonicalInstrumentId,
                                  String sourceVersion) {
    }
}
