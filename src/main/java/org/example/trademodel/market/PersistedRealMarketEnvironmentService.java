package org.example.trademodel.market;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.service.PersistedOhlcvQueryService;
import org.example.trademodel.service.RuntimeKlineContextAssemblyService;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PersistedRealMarketEnvironmentService {
    static final List<String> REQUIRED_TIMEFRAMES = List.of("5m", "15m", "1h", "4h");
    static final int REQUIRED_CLOSED_BARS = 100;

    private final PersistedOhlcvQueryService queryService;
    private final RuntimeKlineContextAssemblyService contextAssemblyService;

    public PersistedRealMarketEnvironmentService(PersistedOhlcvQueryService queryService,
                                                 RuntimeKlineContextAssemblyService contextAssemblyService) {
        this.queryService = queryService;
        this.contextAssemblyService = contextAssemblyService;
    }

    public PersistedRealMarketEnvironmentAssessment assess(String symbol, String analysisTimeframe) {
        Map<String, RuntimeKlineContextDTO> contexts = new LinkedHashMap<>();
        String provider = null;
        int closedBarCount = 0;
        Long latestClosedBarTimeMs = null;
        List<String> sourceTraceRefs = new ArrayList<>();

        for (String timeframe : REQUIRED_TIMEFRAMES) {
            PersistedOhlcvReadinessResult readiness;
            RuntimeKlineContextDTO context;
            try {
                readiness = queryService.evaluateReadiness(
                        symbol, timeframe, REQUIRED_CLOSED_BARS, maxReadLagMs(timeframe));
                context = contextAssemblyService.assemble(readiness);
            } catch (RuntimeException failure) {
                return PersistedRealMarketEnvironmentAssessment.failed("MARKET_DATA_NOT_READY", contexts);
            }
            contexts.put(timeframe, context);
            if (readiness == null || readiness.getStatus() != PersistedOhlcvReadinessStatus.FRESH) {
                return PersistedRealMarketEnvironmentAssessment.failed("MARKET_DATA_NOT_READY", contexts);
            }
            if (!realContextReady(context)) {
                return PersistedRealMarketEnvironmentAssessment.failed(
                        "REAL_MARKET_PROVENANCE_INCOMPLETE", contexts);
            }
            if (provider == null) {
                provider = context.getSourceProvider();
            } else if (!provider.equalsIgnoreCase(context.getSourceProvider())) {
                return PersistedRealMarketEnvironmentAssessment.failed(
                        "REAL_MARKET_PROVENANCE_INCOMPLETE", contexts);
            }
            closedBarCount += context.getClosedBarCount();
            if (context.getLatestClosedBarTimeMs() != null
                    && (latestClosedBarTimeMs == null
                    || context.getLatestClosedBarTimeMs() > latestClosedBarTimeMs)) {
                latestClosedBarTimeMs = context.getLatestClosedBarTimeMs();
            }
            for (String traceRef : context.getSourceTraceRefs()) {
                if (!sourceTraceRefs.contains(traceRef)) sourceTraceRefs.add(traceRef);
            }
        }

        RuntimeKlineContextDTO environmentContext = contexts.get("1h");
        MarketEnvironmentVO environment = buildEnvironment(symbol, analysisTimeframe, provider, environmentContext);
        if (environment == null) {
            return PersistedRealMarketEnvironmentAssessment.failed("REAL_MARKET_PROVENANCE_INCOMPLETE", contexts);
        }
        String sourceType = provider.toUpperCase(Locale.ROOT) + "_PERSISTED_OHLCV";
        return new PersistedRealMarketEnvironmentAssessment(true, null, provider, sourceType, environment,
                Map.copyOf(contexts), closedBarCount, latestClosedBarTimeMs, List.copyOf(sourceTraceRefs));
    }

    private static boolean realContextReady(RuntimeKlineContextDTO context) {
        return context != null
                && context.isRealMarketEnvironment()
                && "REAL".equals(context.getSourceMode())
                && hasText(context.getSourceProvider())
                && "SPOT".equalsIgnoreCase(context.getSourceMarketType())
                && context.getFallbackStatus() == null
                && context.getMissingFields().isEmpty()
                && context.getClosedBarCount() != null
                && context.getClosedBarCount() >= REQUIRED_CLOSED_BARS
                && context.getLatestClosedBarTimeMs() != null
                && context.getSourceTraceRefs() != null
                && !context.getSourceTraceRefs().isEmpty()
                && context.getKlineItems() != null
                && context.getKlineItems().size() >= REQUIRED_CLOSED_BARS;
    }

    private static MarketEnvironmentVO buildEnvironment(String symbol, String analysisTimeframe,
                                                        String provider, RuntimeKlineContextDTO context) {
        if (context == null || context.getKlineItems() == null || context.getKlineItems().size() < 24) return null;
        List<RuntimeKlineItemDTO> bars = context.getKlineItems().stream()
                .filter(item -> item != null && item.getOpenTimeMs() != null)
                .sorted(Comparator.comparing(RuntimeKlineItemDTO::getOpenTimeMs))
                .toList();
        if (bars.size() < 24) return null;
        List<RuntimeKlineItemDTO> last24 = bars.subList(bars.size() - 24, bars.size());
        RuntimeKlineItemDTO first = last24.get(0);
        RuntimeKlineItemDTO latest = last24.get(last24.size() - 1);
        if (!positive(first.getOpenPrice()) || !positive(latest.getClosePrice())) return null;

        BigDecimal changePct = latest.getClosePrice().subtract(first.getOpenPrice())
                .divide(first.getOpenPrice(), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        BigDecimal high = last24.stream().map(RuntimeKlineItemDTO::getHighPrice)
                .filter(PersistedRealMarketEnvironmentService::positive).max(BigDecimal::compareTo).orElse(null);
        BigDecimal low = last24.stream().map(RuntimeKlineItemDTO::getLowPrice)
                .filter(PersistedRealMarketEnvironmentService::positive).min(BigDecimal::compareTo).orElse(null);
        if (high == null || low == null) return null;
        double rangePct = high.subtract(low).divide(latest.getClosePrice(), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
        double absChange = changePct.abs().doubleValue();

        MarketEnvironmentVO environment = new MarketEnvironmentVO();
        environment.setPriceChangePercent24h(changePct);
        environment.setEnvironmentType(absChange >= 2.0 ? "trend_market" : "range_market");
        environment.setRiskMode(absChange >= 8.0 ? "elevated" : "normal");
        environment.setTrendFriendliness(Math.max(0, Math.min(100, 50 + changePct.doubleValue() * 2.5)));
        environment.setLeverageSuggestion(rangePct >= 6.0 ? "low_leverage" : "moderate_leverage");
        environment.setRangePct24h(rangePct);
        environment.setVolatilityRegime(RealMarketEnvironmentService.describeVolatilityRegime(rangePct));
        environment.setPerpFundingApplied(false);
        environment.setOiApplied(false);
        environment.setSummary(String.format(Locale.ROOT,
                "Real persisted OHLCV (%s SPOT): %s 24h change %s%%, range %.2f%%. Analysis timeframe: %s.",
                provider, symbol, changePct.stripTrailingZeros().toPlainString(), rangePct, analysisTimeframe));
        return environment;
    }

    static long maxReadLagMs(String timeframe) {
        return switch (timeframe) {
            case "5m" -> 11L * 60_000L;
            case "15m" -> 31L * 60_000L;
            case "1h" -> 121L * 60_000L;
            case "4h" -> 481L * 60_000L;
            default -> 0L;
        };
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
