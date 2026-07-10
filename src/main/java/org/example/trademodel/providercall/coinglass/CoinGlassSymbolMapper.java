package org.example.trademodel.providercall.coinglass;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class CoinGlassSymbolMapper {
    private static final Set<String> SUPPORTED = Set.of(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT");

    public CoinGlassSymbol map(String internalSymbol) {
        String normalized = internalSymbol == null ? "" : internalSymbol.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED.contains(normalized)) {
            throw new IllegalArgumentException("unsupported CoinGlass symbol");
        }
        return new CoinGlassSymbol(normalized, normalized.substring(0, normalized.length() - 4));
    }

    public record CoinGlassSymbol(String pairSymbol, String coinSymbol) {
    }
}
