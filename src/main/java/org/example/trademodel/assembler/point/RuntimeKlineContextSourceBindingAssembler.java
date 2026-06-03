package org.example.trademodel.assembler.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.trademodel.dto.point.RuntimeKlineContextSourceBindingDTO;
import org.example.trademodel.validator.point.RuntimeKlineContextSourceBindingValidator;

public class RuntimeKlineContextSourceBindingAssembler {

    private static final String REASON_INPUT_MISSING = "RUNTIME_KLINE_BINDING_INPUT_MISSING";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_UNSUPPORTED_STATUS = "UNSUPPORTED_STATUS";

    private final RuntimeKlineContextSourceBindingValidator validator;

    public RuntimeKlineContextSourceBindingAssembler() {
        this(new RuntimeKlineContextSourceBindingValidator());
    }

    public RuntimeKlineContextSourceBindingAssembler(RuntimeKlineContextSourceBindingValidator validator) {
        this.validator = validator == null ? new RuntimeKlineContextSourceBindingValidator() : validator;
    }

    public AssembledRuntimeKlineContextSourceBinding assemble(AssemblyInput input) {
        RuntimeKlineContextSourceBindingDTO context = contextFrom(input);
        RuntimeKlineContextSourceBindingValidator.ValidationResult validationResult = validator.validate(context);
        return new AssembledRuntimeKlineContextSourceBinding(context, validationResult);
    }

    private RuntimeKlineContextSourceBindingDTO contextFrom(AssemblyInput input) {
        if (input == null) {
            return RuntimeKlineContextSourceBindingDTO.incomplete(
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    REASON_INPUT_MISSING
            );
        }

        RuntimeKlineContextSourceBindingDTO.BindingStatus requestedStatus = input.getRequestedStatus();
        if (RuntimeKlineContextSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED.equals(requestedStatus)) {
            return RuntimeKlineContextSourceBindingDTO.blockedFailClosed(
                    input.getRuntimeKlineContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getTimeframe(),
                    input.getKlineWindow(),
                    input.getSourceTraceRefs(),
                    requiredOrFallback(input.getBlockedReason(), REASON_BLOCKED_REASON_REQUIRED)
            );
        }

        if (RuntimeKlineContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING_DEGRADED
                .equals(requestedStatus)) {
            return RuntimeKlineContextSourceBindingDTO.degraded(
                    input.getRuntimeKlineContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getTimeframe(),
                    input.getKlineWindow(),
                    input.getLatestPrice(),
                    input.getLatestClose(),
                    input.getOpen(),
                    input.getHigh(),
                    input.getLow(),
                    input.getClose(),
                    input.getVolume(),
                    input.getQuoteVolume(),
                    input.getCandleClosed(),
                    input.getOhlcvCompleteness(),
                    input.getFreshnessStatus(),
                    input.getWickStatus(),
                    input.getGapStatus(),
                    input.getLiquidityState(),
                    input.getLiquiditySeverity(),
                    input.getStampedeState(),
                    input.getSourceTraceRefs(),
                    input.getMarketDataSourceRef(),
                    input.getObservedAt(),
                    input.getCreatedAt(),
                    requiredOrFallback(input.getMissingReason(), REASON_MISSING_REASON_REQUIRED)
            );
        }

        if (RuntimeKlineContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING
                .equals(requestedStatus)) {
            return RuntimeKlineContextSourceBindingDTO.reviewOnly(
                    input.getRuntimeKlineContextId(),
                    input.getSymbol(),
                    input.getMarket(),
                    input.getTimeframe(),
                    input.getKlineWindow(),
                    input.getLatestPrice(),
                    input.getLatestClose(),
                    input.getOpen(),
                    input.getHigh(),
                    input.getLow(),
                    input.getClose(),
                    input.getVolume(),
                    input.getQuoteVolume(),
                    input.getCandleClosed(),
                    input.getOhlcvCompleteness(),
                    input.getFreshnessStatus(),
                    input.getWickStatus(),
                    input.getGapStatus(),
                    input.getLiquidityState(),
                    input.getLiquiditySeverity(),
                    input.getStampedeState(),
                    input.getSourceTraceRefs(),
                    input.getMarketDataSourceRef(),
                    input.getObservedAt(),
                    input.getCreatedAt()
            );
        }

        String fallback = RuntimeKlineContextSourceBindingDTO.BindingStatus.INCOMPLETE.equals(requestedStatus)
                ? REASON_MISSING_REASON_REQUIRED
                : REASON_UNSUPPORTED_STATUS;
        return RuntimeKlineContextSourceBindingDTO.incomplete(
                input.getRuntimeKlineContextId(),
                input.getSymbol(),
                input.getMarket(),
                input.getTimeframe(),
                input.getKlineWindow(),
                input.getSourceTraceRefs(),
                requiredOrFallback(input.getMissingReason(), fallback)
        );
    }

    private static String requiredOrFallback(String reason, String fallbackReason) {
        return isBlank(reason) ? fallbackReason : reason;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class AssemblyInput {
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
        private final RuntimeKlineContextSourceBindingDTO.FreshnessStatus freshnessStatus;
        private final RuntimeKlineContextSourceBindingDTO.WickStatus wickStatus;
        private final RuntimeKlineContextSourceBindingDTO.GapStatus gapStatus;
        private final RuntimeKlineContextSourceBindingDTO.LiquidityState liquidityState;
        private final String liquiditySeverity;
        private final RuntimeKlineContextSourceBindingDTO.StampedeState stampedeState;
        private final List<String> sourceTraceRefs;
        private final String marketDataSourceRef;
        private final String observedAt;
        private final String createdAt;
        private final String missingReason;
        private final String blockedReason;
        private final Boolean trustedSource;
        private final RuntimeKlineContextSourceBindingDTO.BindingStatus requestedStatus;

        private AssemblyInput(
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
                RuntimeKlineContextSourceBindingDTO.FreshnessStatus freshnessStatus,
                RuntimeKlineContextSourceBindingDTO.WickStatus wickStatus,
                RuntimeKlineContextSourceBindingDTO.GapStatus gapStatus,
                RuntimeKlineContextSourceBindingDTO.LiquidityState liquidityState,
                String liquiditySeverity,
                RuntimeKlineContextSourceBindingDTO.StampedeState stampedeState,
                List<String> sourceTraceRefs,
                String marketDataSourceRef,
                String observedAt,
                String createdAt,
                String missingReason,
                String blockedReason,
                Boolean trustedSource,
                RuntimeKlineContextSourceBindingDTO.BindingStatus requestedStatus
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
            this.sourceTraceRefs = copy(sourceTraceRefs);
            this.marketDataSourceRef = marketDataSourceRef;
            this.observedAt = observedAt;
            this.createdAt = createdAt;
            this.missingReason = missingReason;
            this.blockedReason = blockedReason;
            this.trustedSource = trustedSource;
            this.requestedStatus = requestedStatus;
        }

        public static AssemblyInput of(
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
                RuntimeKlineContextSourceBindingDTO.FreshnessStatus freshnessStatus,
                RuntimeKlineContextSourceBindingDTO.WickStatus wickStatus,
                RuntimeKlineContextSourceBindingDTO.GapStatus gapStatus,
                RuntimeKlineContextSourceBindingDTO.LiquidityState liquidityState,
                String liquiditySeverity,
                RuntimeKlineContextSourceBindingDTO.StampedeState stampedeState,
                List<String> sourceTraceRefs,
                String marketDataSourceRef,
                String observedAt,
                String createdAt,
                String missingReason,
                String blockedReason,
                Boolean trustedSource,
                RuntimeKlineContextSourceBindingDTO.BindingStatus requestedStatus
        ) {
            return new AssemblyInput(
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
                    missingReason,
                    blockedReason,
                    trustedSource,
                    requestedStatus
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

        public RuntimeKlineContextSourceBindingDTO.FreshnessStatus getFreshnessStatus() {
            return freshnessStatus;
        }

        public RuntimeKlineContextSourceBindingDTO.WickStatus getWickStatus() {
            return wickStatus;
        }

        public RuntimeKlineContextSourceBindingDTO.GapStatus getGapStatus() {
            return gapStatus;
        }

        public RuntimeKlineContextSourceBindingDTO.LiquidityState getLiquidityState() {
            return liquidityState;
        }

        public String getLiquiditySeverity() {
            return liquiditySeverity;
        }

        public RuntimeKlineContextSourceBindingDTO.StampedeState getStampedeState() {
            return stampedeState;
        }

        public List<String> getSourceTraceRefs() {
            return copy(sourceTraceRefs);
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

        public Boolean getTrustedSource() {
            return trustedSource;
        }

        public RuntimeKlineContextSourceBindingDTO.BindingStatus getRequestedStatus() {
            return requestedStatus;
        }

        private static List<String> copy(List<String> values) {
            return values == null ? Collections.emptyList() : new ArrayList<>(values);
        }
    }

    public static class AssembledRuntimeKlineContextSourceBinding {
        private final RuntimeKlineContextSourceBindingDTO context;
        private final RuntimeKlineContextSourceBindingValidator.ValidationResult validationResult;

        private AssembledRuntimeKlineContextSourceBinding(
                RuntimeKlineContextSourceBindingDTO context,
                RuntimeKlineContextSourceBindingValidator.ValidationResult validationResult
        ) {
            this.context = context;
            this.validationResult = validationResult;
        }

        public RuntimeKlineContextSourceBindingDTO getContext() {
            return context;
        }

        public RuntimeKlineContextSourceBindingValidator.ValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
