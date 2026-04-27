package org.example.trademodel.market.util;

/**
 * Spot / USDT-M perp symbol alignment: BASE → BASEUSDT（与现货 REST 路径一致）。
 */
public final class BinanceUsdtSymbol {

    private BinanceUsdtSymbol() {}

    /**
     * @param assetSymbol e.g. BTC, ETH, BTCUSDT
     * @return uppercase USDT pair e.g. BTCUSDT
     */
    public static String toUsdtPair(String assetSymbol) {
        if (assetSymbol == null || assetSymbol.isBlank()) {
            return "BTCUSDT";
        }
        String s = assetSymbol.trim().toUpperCase();
        if (s.endsWith("USDT")) {
            return s;
        }
        return s + "USDT";
    }
}
