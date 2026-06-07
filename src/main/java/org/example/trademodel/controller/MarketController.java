package org.example.trademodel.controller;

import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.market.util.BinanceUsdtSymbol;
import org.example.trademodel.service.RealMarketDataFetcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private static final long QUOTE_STALE_THRESHOLD_SECONDS = 60L;

    private final RealMarketDataFetcherService realMarketDataFetcherService;
    private final MarketQuoteClient marketQuoteClient;

    @Autowired
    public MarketController(RealMarketDataFetcherService realMarketDataFetcherService,
                            MarketQuoteClient marketQuoteClient) {
        this.realMarketDataFetcherService = realMarketDataFetcherService;
        this.marketQuoteClient = marketQuoteClient;
    }

    @GetMapping("/real-fetch")
    public Map<String, Object> fetchRealMarketData(@RequestParam String symbol,
                                                   @RequestParam(defaultValue = "1m") String interval) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            realMarketDataFetcherService.fetchRealMarketData(symbol, interval);
            result.put("code", 200);
            result.put("msg", "SUCCESS");
            result.put("data", "Real market data fetch started for " + symbol + " " + interval);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "ERROR: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/quote-status")
    public Map<String, Object> quoteStatus(@RequestParam(defaultValue = "BTCUSDT") String symbol) {
        String normalizedSymbol = BinanceUsdtSymbol.toUsdtPair(symbol);
        Map<String, Object> status = baseQuoteStatus(normalizedSymbol);
        try {
            Optional<MarketQuoteSnapshot> snapshotOpt = marketQuoteClient.fetch24hTicker(normalizedSymbol);
            if (snapshotOpt.isEmpty()) {
                applyUnavailable(status, "MARKETQUOTE_MISSING_FAIL_CLOSED", "QUOTE_UNAVAILABLE",
                        "行情缺失；只读状态，不是交易信号，不进入候选/推送/点位。");
                return status;
            }
            applySnapshotStatus(status, snapshotOpt.get());
        } catch (Exception e) {
            applyUnavailable(status, "MARKETQUOTE_BLOCKED_FAIL_CLOSED", "QUOTE_STATUS_EXCEPTION",
                    "行情状态读取异常；保持 fail-closed，只读展示，不进入候选/推送/点位。");
        }
        return status;
    }

    private static Map<String, Object> baseQuoteStatus(String normalizedSymbol) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "MARKETQUOTE_MISSING_FAIL_CLOSED");
        status.put("sampleSymbol", normalizedSymbol);
        status.put("symbols", List.of(normalizedSymbol));
        status.put("source", "UNKNOWN");
        status.put("sourceType", "MISSING");
        status.put("lastQuoteTime", null);
        status.put("lastUpdatedAt", null);
        status.put("freshnessSeconds", null);
        status.put("staleThresholdSeconds", QUOTE_STALE_THRESHOLD_SECONDS);
        status.put("fresh", false);
        status.put("fallbackActive", true);
        status.put("sourceHealth", "MISSING");
        status.put("reason", "QUOTE_UNAVAILABLE");
        status.put("message", "行情状态待确认；只读状态，不是交易信号。");
        status.put("reviewOnly", true);
        status.put("notTradingSignal", true);
        status.put("watchlistBounded", false);
        status.put("dashboardOnlySample", true);
        status.put("displaySlotsAreCandidatePool", false);
        return status;
    }

    private static void applySnapshotStatus(Map<String, Object> status, MarketQuoteSnapshot snapshot) {
        String provider = normalizeProvider(snapshot.getProvider());
        long fetchedAt = snapshot.getFetchedAtEpochMillis();
        status.put("source", provider);
        status.put("sourceType", provider + "_24H_TICKER");
        status.put("fallbackActive", false);

        if (fetchedAt <= 0L) {
            status.put("status", "MARKETQUOTE_SOURCE_HEALTH_PARTIAL");
            status.put("sourceHealth", "PARTIAL");
            status.put("reason", "QUOTE_TIMESTAMP_MISSING");
            status.put("message", "行情源可读但更新时间缺失；只读状态，不是交易信号。");
            return;
        }

        long freshnessSeconds = Math.max(0L, (System.currentTimeMillis() - fetchedAt) / 1000L);
        String lastUpdatedAt = Instant.ofEpochMilli(fetchedAt).toString();
        boolean fresh = freshnessSeconds <= QUOTE_STALE_THRESHOLD_SECONDS;
        status.put("lastQuoteTime", lastUpdatedAt);
        status.put("lastUpdatedAt", lastUpdatedAt);
        status.put("freshnessSeconds", freshnessSeconds);
        status.put("fresh", fresh);
        if (fresh) {
            status.put("status", "MARKETQUOTE_REVIEW_ONLY_READY");
            status.put("sourceHealth", "OK");
            status.put("reason", "QUOTE_FRESH");
            status.put("message", "行情源可读且新鲜；只读状态，不是交易信号，不进入候选/推送/点位。");
        } else {
            status.put("status", "MARKETQUOTE_STALE_FAIL_CLOSED");
            status.put("sourceHealth", "STALE");
            status.put("reason", "QUOTE_STALE");
            status.put("message", "行情已过期；保持 fail-closed，只读展示，不进入候选/推送/点位。");
        }
    }

    private static void applyUnavailable(Map<String, Object> status, String statusValue, String reason, String message) {
        status.put("status", statusValue);
        status.put("source", "UNKNOWN");
        status.put("sourceType", "MISSING");
        status.put("fresh", false);
        status.put("fallbackActive", true);
        status.put("sourceHealth", "MISSING");
        status.put("reason", reason);
        status.put("message", message);
    }

    private static String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "UNKNOWN";
        }
        return provider.trim().toUpperCase();
    }
}
