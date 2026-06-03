package org.example.trademodel.dto.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RuntimeKlineContextSourceBindingDTO {

    public enum BindingStatus {
        INCOMPLETE,
        BLOCKED_FAIL_CLOSED,
        REVIEW_ONLY_RUNTIME_KLINE_BINDING,
        REVIEW_ONLY_RUNTIME_KLINE_BINDING_DEGRADED
    }

    public enum FreshnessStatus {
        FRESH,
        STALE,
        UNKNOWN
    }

    public enum WickStatus {
        NONE,
        WICK_ONLY,
        WICK_CONFIRMED,
        UNKNOWN
    }

    public enum GapStatus {
        NONE,
        MINOR_GAP,
        SEVERE_GAP,
        UNKNOWN
    }

    public enum LiquidityState {
        NORMAL,
        DEGRADED,
        SEVERELY_DEGRADED,
        UNKNOWN
    }

    public enum StampedeState {
        NONE,
        SUSPECTED,
        CONFIRMED,
        UNKNOWN
    }

    private final String runtimeKlineContextId;
    private final String symbol;
    private final String market;
    private final String timeframe;
    private final String klineWindow;
    private final BigDecimal latestPrice;
    private final BigDecimal latestClose;
    private final BigDecimal open;
    private final BigDecimal high;
    private final BigDecimal low;
    private final BigDecimal close;
    private final BigDecimal volume;
    private final BigDecimal quoteVolume;
    private final Boolean candleClosed;
    private final BigDecimal ohlcvCompleteness;
    private final FreshnessStatus freshnessStatus;
    private final WickStatus wickStatus;
    private final GapStatus gapStatus;
    private final LiquidityState liquidityState;
    private final String liquiditySeverity;
    private final StampedeState stampedeState;
    private final List<String> sourceTraceRefs;
    private final String marketDataSourceRef;
    private final String observedAt;
    private final String createdAt;
    private final String missingReason;
    private final String blockedReason;
    private final boolean trustedSource;
    private final boolean reviewOnly;
    private final boolean notTradeInstruction;
    private final boolean manualReviewRequired;
    private final boolean incompleteSafe;
    private final boolean failClosed;
    private final BindingStatus bindingStatus;

    private RuntimeKlineContextSourceBindingDTO(
            String runtimeKlineContextId,
            String symbol,
            String market,
            String timeframe,
            String klineWindow,
            BigDecimal latestPrice,
            BigDecimal latestClose,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume,
            BigDecimal quoteVolume,
            Boolean candleClosed,
            BigDecimal ohlcvCompleteness,
            FreshnessStatus freshnessStatus,
            WickStatus wickStatus,
            GapStatus gapStatus,
            LiquidityState liquidityState,
            String liquiditySeverity,
            StampedeState stampedeState,
            List<String> sourceTraceRefs,
            String marketDataSourceRef,
            String observedAt,
            String createdAt,
            String missingReason,
            String blockedReason,
            BindingStatus bindingStatus
    ) {
        this.runtimeKlineContextId = runtimeKlineContextId;
        this.symbol = symbol;
        this.market = market;
        this.timeframe = timeframe;
        this.klineWindow = klineWindow;
        this.latestPrice = latestPrice;
        this.latestClose = latestClose;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.quoteVolume = quoteVolume;
        this.candleClosed = candleClosed;
        this.ohlcvCompleteness = ohlcvCompleteness;
        this.freshnessStatus = freshnessStatus;
        this.wickStatus = wickStatus;
        this.gapStatus = gapStatus;
        this.liquidityState = liquidityState;
        this.liquiditySeverity = liquiditySeverity;
        this.stampedeState = stampedeState;
        this.sourceTraceRefs = immutableCopy(sourceTraceRefs);
        this.marketDataSourceRef = marketDataSourceRef;
        this.observedAt = observedAt;
        this.createdAt = createdAt;
        this.missingReason = missingReason;
        this.blockedReason = blockedReason;
        this.bindingStatus = bindingStatus;
        this.trustedSource = bindingStatus != BindingStatus.BLOCKED_FAIL_CLOSED;
        this.reviewOnly = true;
        this.notTradeInstruction = true;
        this.manualReviewRequired = true;
        this.incompleteSafe = true;
        this.failClosed = bindingStatus == BindingStatus.BLOCKED_FAIL_CLOSED;
    }

    public static RuntimeKlineContextSourceBindingDTO incomplete(
            String runtimeKlineContextId,
            String symbol,
            String market,
            String timeframe,
            String klineWindow,
            List<String> sourceTraceRefs,
            String missingReason
    ) {
        return new RuntimeKlineContextSourceBindingDTO(
                runtimeKlineContextId,
                symbol,
                market,
                timeframe,
                klineWindow,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                FreshnessStatus.UNKNOWN,
                WickStatus.UNKNOWN,
                GapStatus.UNKNOWN,
                LiquidityState.UNKNOWN,
                null,
                StampedeState.UNKNOWN,
                sourceTraceRefs,
                null,
                null,
                null,
                requiredReason(missingReason, "missingReason"),
                null,
                BindingStatus.INCOMPLETE
        );
    }

    public static RuntimeKlineContextSourceBindingDTO blockedFailClosed(
            String runtimeKlineContextId,
            String symbol,
            String market,
            String timeframe,
            String klineWindow,
            List<String> sourceTraceRefs,
            String blockedReason
    ) {
        return new RuntimeKlineContextSourceBindingDTO(
                runtimeKlineContextId,
                symbol,
                market,
                timeframe,
                klineWindow,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                FreshnessStatus.UNKNOWN,
                WickStatus.UNKNOWN,
                GapStatus.UNKNOWN,
                LiquidityState.UNKNOWN,
                null,
                StampedeState.UNKNOWN,
                sourceTraceRefs,
                null,
                null,
                null,
                null,
                requiredReason(blockedReason, "blockedReason"),
                BindingStatus.BLOCKED_FAIL_CLOSED
        );
    }

    public static RuntimeKlineContextSourceBindingDTO degraded(
            String runtimeKlineContextId,
            String symbol,
            String market,
            String timeframe,
            String klineWindow,
            BigDecimal latestPrice,
            BigDecimal latestClose,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume,
            BigDecimal quoteVolume,
            Boolean candleClosed,
            BigDecimal ohlcvCompleteness,
            FreshnessStatus freshnessStatus,
            WickStatus wickStatus,
            GapStatus gapStatus,
            LiquidityState liquidityState,
            String liquiditySeverity,
            StampedeState stampedeState,
            List<String> sourceTraceRefs,
            String marketDataSourceRef,
            String observedAt,
            String createdAt,
            String missingReason
    ) {
        return new RuntimeKlineContextSourceBindingDTO(
                runtimeKlineContextId,
                symbol,
                market,
                timeframe,
                klineWindow,
                latestPrice,
                latestClose,
                open,
                high,
                low,
                close,
                volume,
                quoteVolume,
                candleClosed,
                ohlcvCompleteness,
                freshnessStatus,
                wickStatus,
                gapStatus,
                liquidityState,
                liquiditySeverity,
                stampedeState,
                sourceTraceRefs,
                marketDataSourceRef,
                observedAt,
                createdAt,
                requiredReason(missingReason, "missingReason"),
                null,
                BindingStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING_DEGRADED
        );
    }

    public static RuntimeKlineContextSourceBindingDTO reviewOnly(
            String runtimeKlineContextId,
            String symbol,
            String market,
            String timeframe,
            String klineWindow,
            BigDecimal latestPrice,
            BigDecimal latestClose,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume,
            BigDecimal quoteVolume,
            Boolean candleClosed,
            BigDecimal ohlcvCompleteness,
            FreshnessStatus freshnessStatus,
            WickStatus wickStatus,
            GapStatus gapStatus,
            LiquidityState liquidityState,
            String liquiditySeverity,
            StampedeState stampedeState,
            List<String> sourceTraceRefs,
            String marketDataSourceRef,
            String observedAt,
            String createdAt
    ) {
        return new RuntimeKlineContextSourceBindingDTO(
                runtimeKlineContextId,
                symbol,
                market,
                timeframe,
                klineWindow,
                latestPrice,
                latestClose,
                open,
                high,
                low,
                close,
                volume,
                quoteVolume,
                candleClosed,
                ohlcvCompleteness,
                freshnessStatus,
                wickStatus,
                gapStatus,
                liquidityState,
                liquiditySeverity,
                stampedeState,
                sourceTraceRefs,
                marketDataSourceRef,
                observedAt,
                createdAt,
                null,
                null,
                BindingStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING
        );
    }

    public String getRuntimeKlineContextId() {
        return runtimeKlineContextId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getMarket() {
        return market;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public String getKlineWindow() {
        return klineWindow;
    }

    public BigDecimal getLatestPrice() {
        return latestPrice;
    }

    public BigDecimal getLatestClose() {
        return latestClose;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public BigDecimal getQuoteVolume() {
        return quoteVolume;
    }

    public Boolean getCandleClosed() {
        return candleClosed;
    }

    public BigDecimal getOhlcvCompleteness() {
        return ohlcvCompleteness;
    }

    public FreshnessStatus getFreshnessStatus() {
        return freshnessStatus;
    }

    public WickStatus getWickStatus() {
        return wickStatus;
    }

    public GapStatus getGapStatus() {
        return gapStatus;
    }

    public LiquidityState getLiquidityState() {
        return liquidityState;
    }

    public String getLiquiditySeverity() {
        return liquiditySeverity;
    }

    public StampedeState getStampedeState() {
        return stampedeState;
    }

    public List<String> getSourceTraceRefs() {
        return sourceTraceRefs;
    }

    public String getMarketDataSourceRef() {
        return marketDataSourceRef;
    }

    public String getObservedAt() {
        return observedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getMissingReason() {
        return missingReason;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public boolean isTrustedSource() {
        return trustedSource;
    }

    public boolean isReviewOnly() {
        return reviewOnly;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isIncompleteSafe() {
        return incompleteSafe;
    }

    public boolean isFailClosed() {
        return failClosed;
    }

    public BindingStatus getBindingStatus() {
        return bindingStatus;
    }

    private static List<String> immutableCopy(List<String> sourceTraceRefs) {
        if (sourceTraceRefs == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(sourceTraceRefs));
    }

    private static String requiredReason(String reason, String fieldName) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return reason;
    }
}
