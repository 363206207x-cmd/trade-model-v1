package org.example.trademodel.providercall;

import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.MarketType;
import org.example.trademodel.providercall.instrument.ProviderSymbolMapping;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProviderCallTestFixtures {
    private ProviderCallTestFixtures() {
    }

    public static CanonicalInstrumentId perpetual(String symbol) {
        return instrument(symbol, MarketType.PERPETUAL, ContractType.LINEAR);
    }

    public static CanonicalInstrumentId spot(String symbol) {
        return instrument(symbol, MarketType.SPOT, ContractType.NONE);
    }

    public static ProviderSymbolMappingRegistry binanceRegistry(String... symbols) {
        List<ProviderSymbolMapping> mappings = new ArrayList<>();
        for (String symbol : symbols) {
            String compact = compact(symbol);
            mappings.add(new ProviderSymbolMapping("BINANCE", perpetual(compact), compact,
                    true, "BINANCE_USDM_TEST_V1"));
            mappings.add(new ProviderSymbolMapping("BINANCE", spot(compact), compact,
                    true, "BINANCE_SPOT_TEST_V1"));
        }
        return new ProviderSymbolMappingRegistry(mappings);
    }

    public static ProviderRequestKey key(String provider,
                                         ProviderDatasetType type,
                                         String symbol,
                                         String timeframe,
                                         String bucket) {
        CanonicalInstrumentId instrument = "COINGLASS".equalsIgnoreCase(provider)
                ? perpetual(symbol) : spot(symbol);
        return new ProviderRequestKey(provider, type, instrument, compact(symbol), timeframe, bucket,
                provider.toUpperCase(Locale.ROOT) + "_TEST_V1");
    }

    private static CanonicalInstrumentId instrument(String symbol,
                                                    MarketType marketType,
                                                    ContractType contractType) {
        String compact = compact(symbol);
        if (!compact.endsWith("USDT") || compact.length() <= 4) {
            throw new IllegalArgumentException("test symbol must use a USDT quote");
        }
        return new CanonicalInstrumentId(compact.substring(0, compact.length() - 4), "USDT",
                marketType, "BINANCE", contractType);
    }

    private static String compact(String symbol) {
        if (symbol == null) throw new IllegalArgumentException("symbol is required");
        return symbol.trim().toUpperCase(Locale.ROOT).replace("/", "").replace("-", "");
    }
}
