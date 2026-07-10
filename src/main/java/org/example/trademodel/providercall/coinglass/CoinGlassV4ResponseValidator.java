package org.example.trademodel.providercall.coinglass;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CoinGlassV4ResponseValidator {
    static final String OI_CAPABILITY = "CG_V4_OPEN_INTEREST_EXCHANGE_LIST";
    static final String FUNDING_CAPABILITY = "CG_V4_OI_WEIGHTED_FUNDING_HISTORY";
    static final String LIQUIDATION_CAPABILITY = "CG_V4_AGGREGATED_LIQUIDATION_HISTORY";
    static final String LONG_SHORT_CAPABILITY = "CG_V4_GLOBAL_ACCOUNT_LONG_SHORT_RATIO";

    public CoinGlassMappingResult<CoinGlassOpenInterestSnapshot> openInterest(
            JsonNode data, CoinGlassSymbolMapper.CoinGlassSymbol symbol, Instant fetchTime) {
        if (!isArray(data)) return malformedOrEmpty(data);
        JsonNode aggregate = null;
        List<String> exchanges = new ArrayList<>();
        BigDecimal largestExchange = null;
        for (JsonNode row : data) {
            if (!row.isObject() || !symbol.coinSymbol().equalsIgnoreCase(text(row, "symbol"))) continue;
            String exchange = text(row, "exchange");
            if (exchange == null) continue;
            if ("All".equalsIgnoreCase(exchange)) {
                aggregate = row;
            } else {
                exchanges.add(exchange);
                BigDecimal exchangeOi = nonNegative(row, "open_interest_usd");
                if (exchangeOi != null && (largestExchange == null || exchangeOi.compareTo(largestExchange) > 0)) {
                    largestExchange = exchangeOi;
                }
            }
        }
        if (aggregate == null) {
            return CoinGlassMappingResult.failed(UnifiedSourceStatus.ERROR, "AGGREGATE_OPEN_INTEREST_MISSING");
        }
        BigDecimal total = nonNegative(aggregate, "open_interest_usd");
        if (total == null) {
            return CoinGlassMappingResult.failed(UnifiedSourceStatus.ERROR, "OPEN_INTEREST_VALUE_INVALID");
        }
        BigDecimal concentration = largestExchange == null || total.signum() == 0 ? null
                : largestExchange.divide(total, MathContext.DECIMAL64);
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("openInterestUsd", OI_CAPABILITY + ":data[exchange=All].open_interest_usd");
        sources.put("openInterestChange5m", OI_CAPABILITY + ":data[exchange=All].open_interest_change_percent_5m");
        sources.put("openInterestChange15m", OI_CAPABILITY + ":data[exchange=All].open_interest_change_percent_15m");
        sources.put("openInterestChange1h", OI_CAPABILITY + ":data[exchange=All].open_interest_change_percent_1h");
        sources.put("exchangeConcentrationScore", OI_CAPABILITY + ":max(exchange.open_interest_usd)/All.open_interest_usd");
        return CoinGlassMappingResult.ready(new CoinGlassOpenInterestSnapshot(symbol.pairSymbol(), total, null,
                decimal(aggregate, "open_interest_change_percent_5m"),
                decimal(aggregate, "open_interest_change_percent_15m"),
                decimal(aggregate, "open_interest_change_percent_1h"),
                concentration, exchanges, fetchTime, sources), fetchTime);
    }

    public CoinGlassMappingResult<CoinGlassFundingSnapshot> funding(
            JsonNode data, CoinGlassSymbolMapper.CoinGlassSymbol symbol, Instant fetchTime) {
        if (!isArray(data)) return malformedOrEmpty(data);
        TimedRow latest = latestTimedRow(data, fetchTime);
        if (latest == null) return CoinGlassMappingResult.failed(UnifiedSourceStatus.ERROR, "FUNDING_TIMESTAMP_INVALID");
        BigDecimal close = decimal(latest.row(), "close");
        if (close == null) return CoinGlassMappingResult.failed(UnifiedSourceStatus.ERROR, "FUNDING_VALUE_INVALID");
        Map<String, String> sources = Map.of(
                "weightedFundingRate", FUNDING_CAPABILITY + ":data.latest.close");
        return CoinGlassMappingResult.ready(new CoinGlassFundingSnapshot(symbol.pairSymbol(), close,
                latest.time(), sources), latest.time());
    }

    public CoinGlassMappingResult<CoinGlassLiquidationSnapshot> liquidation(
            JsonNode data, CoinGlassSymbolMapper.CoinGlassSymbol symbol, Instant fetchTime) {
        if (!isArray(data)) return malformedOrEmpty(data);
        List<TimedLiquidation> rows = new ArrayList<>();
        for (JsonNode row : data) {
            Instant time = millis(row, "time", fetchTime);
            if (time == null) continue;
            BigDecimal longUsd = nonNegative(row, "aggregated_long_liquidation_usd");
            BigDecimal shortUsd = nonNegative(row, "aggregated_short_liquidation_usd");
            if (longUsd == null && shortUsd == null) continue;
            rows.add(new TimedLiquidation(time, longUsd, shortUsd));
        }
        rows.sort(Comparator.comparing(TimedLiquidation::time).reversed());
        if (rows.isEmpty()) {
            return CoinGlassMappingResult.failed(UnifiedSourceStatus.ERROR, "LIQUIDATION_SERIES_INVALID");
        }
        Map<String, String> sources = new LinkedHashMap<>();
        for (String window : List.of("1m", "5m", "15m", "1h")) {
            sources.put("longLiquidationUsd" + window, LIQUIDATION_CAPABILITY
                    + ":sum(latest " + window + " aggregated_long_liquidation_usd)");
            sources.put("shortLiquidationUsd" + window, LIQUIDATION_CAPABILITY
                    + ":sum(latest " + window + " aggregated_short_liquidation_usd)");
        }
        CoinGlassLiquidationSnapshot snapshot = new CoinGlassLiquidationSnapshot(symbol.pairSymbol(),
                sum(rows, 1, true), sum(rows, 5, true), sum(rows, 15, true), sum(rows, 60, true),
                sum(rows, 1, false), sum(rows, 5, false), sum(rows, 15, false), sum(rows, 60, false),
                rows.get(0).time(), sources);
        return CoinGlassMappingResult.ready(snapshot, rows.get(0).time());
    }

    public CoinGlassMappingResult<CoinGlassLongShortSnapshot> longShort(
            JsonNode data, CoinGlassSymbolMapper.CoinGlassSymbol symbol, Instant fetchTime, String exchange) {
        if (!isArray(data)) return malformedOrEmpty(data);
        TimedRow latest = latestTimedRow(data, fetchTime);
        if (latest == null) return CoinGlassMappingResult.failed(UnifiedSourceStatus.ERROR, "LONG_SHORT_TIMESTAMP_INVALID");
        BigDecimal ratio = nonNegative(latest.row(), "global_account_long_short_ratio");
        if (ratio == null || ratio.signum() == 0) {
            return CoinGlassMappingResult.failed(UnifiedSourceStatus.ERROR, "LONG_SHORT_RATIO_INVALID");
        }
        String source = exchange.toUpperCase() + "_GLOBAL_ACCOUNT_RATIO";
        return CoinGlassMappingResult.ready(new CoinGlassLongShortSnapshot(symbol.pairSymbol(), ratio, source,
                latest.time(), Map.of("longShortRatio", LONG_SHORT_CAPABILITY
                + ":data.latest.global_account_long_short_ratio")), latest.time());
    }

    @SuppressWarnings("unchecked")
    private static <T> CoinGlassMappingResult<T> malformedOrEmpty(JsonNode data) {
        if (data != null && data.isArray() && data.isEmpty()) {
            return (CoinGlassMappingResult<T>) CoinGlassMappingResult.failed(
                    UnifiedSourceStatus.EMPTY_CONFIRMED, "PROVIDER_DATA_EMPTY");
        }
        return (CoinGlassMappingResult<T>) CoinGlassMappingResult.failed(
                UnifiedSourceStatus.ERROR, "PROVIDER_DATA_MALFORMED");
    }

    private static boolean isArray(JsonNode data) {
        return data != null && data.isArray() && !data.isEmpty();
    }

    private static TimedRow latestTimedRow(JsonNode data, Instant fetchTime) {
        TimedRow latest = null;
        for (JsonNode row : data) {
            Instant time = millis(row, "time", fetchTime);
            if (time != null && (latest == null || time.isAfter(latest.time()))) latest = new TimedRow(time, row);
        }
        return latest;
    }

    private static Instant millis(JsonNode row, String field, Instant fetchTime) {
        JsonNode value = row == null ? null : row.get(field);
        if (value == null || !value.canConvertToLong()) return null;
        long millis = value.asLong();
        if (millis <= 0) return null;
        Instant time = Instant.ofEpochMilli(millis);
        return time.isAfter(fetchTime.plus(Duration.ofMinutes(5))) ? null : time;
    }

    private static BigDecimal sum(List<TimedLiquidation> rows, int requiredCount, boolean longSide) {
        if (rows.size() < requiredCount) return null;
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < requiredCount; i++) {
            if (i > 0) {
                long gapSeconds = Duration.between(rows.get(i).time(), rows.get(i - 1).time()).getSeconds();
                if (gapSeconds < 30 || gapSeconds > 90) return null;
            }
            BigDecimal value = longSide ? rows.get(i).longUsd() : rows.get(i).shortUsd();
            if (value == null) return null;
            total = total.add(value);
        }
        return total;
    }

    private static String text(JsonNode row, String field) {
        JsonNode value = row == null ? null : row.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static BigDecimal nonNegative(JsonNode row, String field) {
        BigDecimal value = decimal(row, field);
        return value == null || value.signum() < 0 ? null : value;
    }

    private static BigDecimal decimal(JsonNode row, String field) {
        JsonNode value = row == null ? null : row.get(field);
        if (value == null || value.isNull() || (!value.isNumber() && !value.isTextual())) return null;
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record TimedRow(Instant time, JsonNode row) {
    }

    private record TimedLiquidation(Instant time, BigDecimal longUsd, BigDecimal shortUsd) {
    }
}
