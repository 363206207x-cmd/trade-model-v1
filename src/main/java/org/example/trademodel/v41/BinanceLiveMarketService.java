package org.example.trademodel.v41;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.example.trademodel.service.PositionMonitorScheduler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Public six-asset Binance futures stream. It has no account or trading capability. */
@Service
public class BinanceLiveMarketService implements WebSocket.Listener {
    private static final Logger log = LoggerFactory.getLogger(BinanceLiveMarketService.class);
    private static final List<String> SYMBOLS = List.of("btcusdt", "ethusdt", "solusdt", "bnbusdt", "xrpusdt", "adausdt");
    private final ObjectMapper objectMapper;
    private final DashboardLiveEventService eventService;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final AtomicBoolean connecting = new AtomicBoolean();
    private final Map<String, AtomicLong> versions = new ConcurrentHashMap<>();
    private final Map<String, Long> lastEventTimeByStream = new ConcurrentHashMap<>();
    private final Map<String, ArrayDeque<TimedPrice>> recentPrices = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> latestKlineReturns = new ConcurrentHashMap<>();
    private final Map<String, Integer> invalidationCrossCounts = new ConcurrentHashMap<>();
    private final Map<String, ShockEmission> shockEmissions = new ConcurrentHashMap<>();
    private final Map<String, BinanceOrderBook> depthBooks = new ConcurrentHashMap<>();
    private final Map<String, TopQuote> topQuotes = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> depthSnapshotRequests = new ConcurrentHashMap<>();
    private final StringBuilder messageBuffer = new StringBuilder();
    private final boolean enabled;
    private PositionMonitorScheduler positionMonitorScheduler;
    private volatile WebSocket webSocket;

    public BinanceLiveMarketService(ObjectMapper objectMapper, DashboardLiveEventService eventService,
                                    @Value("${trade-model.home-live.enabled:false}") boolean enabled) {
        this.objectMapper = objectMapper;
        this.eventService = eventService;
        this.enabled = enabled;
    }

    @Autowired(required = false)
    void setPositionMonitorScheduler(PositionMonitorScheduler value) {
        this.positionMonitorScheduler = value;
    }

    @Scheduled(fixedDelay = 5_000L, initialDelay = 1_000L)
    public void ensureConnected() {
        if (!enabled || webSocket != null || !connecting.compareAndSet(false, true)) return;
        httpClient.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create(streamUri()), this)
                .whenComplete((socket, error) -> {
                    connecting.set(false);
                    if (error != null) log.warn("[home-live] Binance stream connect failed: {}", error.getMessage());
                    else webSocket = socket;
                });
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        this.webSocket = webSocket;
        log.info("[home-live] Binance six-asset public stream connected");
        SYMBOLS.forEach(symbol -> requestDepthSnapshot(symbol.toUpperCase(Locale.ROOT)));
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
        synchronized (messageBuffer) {
            messageBuffer.append(data);
            if (last) {
                String message = messageBuffer.toString();
                messageBuffer.setLength(0);
                process(message);
            }
        }
        socket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
        webSocket = null;
        depthBooks.values().forEach(BinanceOrderBook::invalidate);
        log.warn("[home-live] Binance stream closed status={}", statusCode);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket socket, Throwable error) {
        webSocket = null;
        depthBooks.values().forEach(BinanceOrderBook::invalidate);
        log.warn("[home-live] Binance stream error: {}", error.getMessage());
    }

    void process(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String stream = root.path("stream").asText("");
            JsonNode data = root.path("data");
            String symbol = data.path("s").asText("").toUpperCase(Locale.ROOT);
            long eventTime = data.path("E").asLong(System.currentTimeMillis());
            if (symbol.isBlank()) return;
            if (stream.contains("@depth")) {
                processDepth(symbol, data, eventTime);
                return;
            }
            long sequence = stream.contains("@bookTicker")
                    ? data.path("u").asLong(eventTime)
                    : eventTime;
            if (!acceptSequence(stream, sequence)) return;
            if (stream.contains("@markPrice")) publishMark(symbol, data, eventTime);
            else if (stream.contains("@bookTicker")) updateBook(symbol, data, eventTime);
            else if (stream.contains("@kline_1m") || stream.contains("@kline_5m")) publishKline(symbol, data, eventTime);
        } catch (Exception failure) {
            log.warn("[home-live] malformed Binance stream payload: {}", failure.getMessage());
        }
    }

    private void publishMark(String symbol, JsonNode data, long eventTime) {
        BigDecimal mark = decimal(data, "p");
        BigDecimal index = decimal(data, "i");
        if (!positive(mark)) return;
        remember(symbol, mark, eventTime);
        long version = version(symbol, eventTime);
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("latestPrice", mark);
        payload.put("indexPrice", index);
        payload.put("latestPriceAt", Instant.ofEpochMilli(eventTime));
        payload.put("source", "BINANCE_MARK_PRICE_WEBSOCKET");
        payload.put("freshness", "FRESH");
        BinanceOrderBook.Snapshot depth = depthSnapshot(symbol, eventTime);
        if (depth != null) {
            payload.put("bidPrice", depth.bid());
            payload.put("askPrice", depth.ask());
            payload.put("spreadBps", spreadBps(depth.bid(), depth.ask()));
            payload.put("bidDepth10Bps", depth.bidDepth10Bps());
            payload.put("askDepth10Bps", depth.askDepth10Bps());
            payload.put("depthSequence", depth.sequence());
            payload.put("depthSource", "BINANCE_DEPTH_SNAPSHOT_INCREMENTAL");
        } else {
            TopQuote quote = topQuotes.get(symbol);
            if (quote != null) {
                payload.put("bidPrice", quote.bid());
                payload.put("askPrice", quote.ask());
                payload.put("spreadBps", spreadBps(quote.bid(), quote.ask()));
                payload.put("depthSource", "BINANCE_BOOK_TICKER_TOP_ONLY");
            }
        }
        eventService.publish(new DashboardLiveEvent(symbol + "-price-" + version,
                "ASSET_PRICE_UPDATED", symbol, version, Instant.ofEpochMilli(eventTime), Instant.now(), payload));
    }

    private void publishKline(String symbol, JsonNode data, long eventTime) {
        JsonNode kline = data.path("k");
        BigDecimal open = decimal(kline, "o");
        BigDecimal close = decimal(kline, "c");
        if (!positive(open) || !positive(close)) return;
        BigDecimal change = close.subtract(open).divide(open, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        String timeframe = kline.path("i").asText("1m");
        latestKlineReturns.put(symbol + "|" + timeframe.toLowerCase(Locale.ROOT), change);
        for (DashboardLiveEventService.ScopedStructuralContext scoped : eventService.structuralContexts(symbol)) {
            publishRiskAssessment(scoped, symbol, close, eventTime, timeframe);
        }
    }

    private void publishRiskAssessment(DashboardLiveEventService.ScopedStructuralContext scoped,
                                       String symbol, BigDecimal close, long eventTime, String timeframe) {
        DashboardLiveEventService.StructuralContext context = scoped.context();
        Instant observedAt = Instant.ofEpochMilli(eventTime);
        Instant directionExpiry = context.expiresAt() == null
                ? context.calculatedAt().plus(Duration.ofMinutes(90)) : context.expiresAt();
        boolean directionFresh = !observedAt.isAfter(directionExpiry);
        String scopeKey = scoped.userId() + "|" + symbol;
        int invalidationCrossCount = invalidationCrossCounts.compute(scopeKey, (ignored, previous) ->
                crossed(context.structuralDirection(), close, context.invalidationLevel())
                        ? Math.min(2, (previous == null ? 0 : previous) + 1) : 0);
        V41RealtimeShockPolicy.Assessment assessment = V41RealtimeShockPolicy.assess(
                new V41RealtimeShockPolicy.Input(context.structuralDirection(), context.invalidationLevel(),
                        close, context.atr1h(), latestReturn(symbol, "1m"), latestReturn(symbol, "5m"),
                        null, null, null, context.derivativesFresh(),
                        Math.abs(System.currentTimeMillis() - eventTime) <= 10_000L,
                        directionFresh, false, invalidationCrossCount));
        ShockEmission previous = shockEmissions.get(scopeKey);
        if ("NORMAL".equals(assessment.realtimeState()) && previous != null
                && !java.util.Objects.equals(previous.decisionId(), context.decisionId())) {
            shockEmissions.remove(scopeKey, previous);
            publishRiskEvent(scoped, context, assessment, symbol, eventTime, timeframe);
            return;
        }
        if ("NORMAL".equals(assessment.realtimeState())) {
            return;
        }
        String signature = assessment.realtimeState() + "|" + assessment.effectiveExecutionState()
                + "|" + String.join(",", assessment.reasonCodes());
        if (previous != null && previous.signature().equals(signature)
                && eventTime - previous.emittedAtMillis() < 30_000L) return;
        shockEmissions.put(scopeKey, new ShockEmission(signature, eventTime, context.decisionId()));
        publishRiskEvent(scoped, context, assessment, symbol, eventTime, timeframe);
    }

    private void publishRiskEvent(DashboardLiveEventService.ScopedStructuralContext scoped,
                                  DashboardLiveEventService.StructuralContext context,
                                  V41RealtimeShockPolicy.Assessment assessment,
                                  String symbol, long eventTime, String timeframe) {
        long version = version(symbol, eventTime);
        Map<String, Object> risk = new java.util.LinkedHashMap<>();
        risk.put("riskType", riskType(assessment.realtimeState()));
        risk.put("riskTypeLabel", riskTypeLabel(assessment.realtimeState()));
        risk.put("score", assessment.severityScore());
        risk.put("severity", assessment.severityScore() >= 70 ? "HIGH" : "MEDIUM");
        risk.put("primaryEvidence", timeframe + "实时行情触发 "
                + String.join("、", assessment.reasonCodes()));
        risk.put("source", "BINANCE_KLINE_WEBSOCKET");
        risk.put("observedAt", Instant.ofEpochMilli(eventTime));
        risk.put("recoveryCondition", "需要新AnalysisRun重验方向、风险和计划");
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("realtimeState", assessment.realtimeState());
        payload.put("effectiveExecutionState", assessment.effectiveExecutionState());
        payload.put("shockReasonCodes", assessment.reasonCodes());
        payload.put("confidenceCap", assessment.confidenceCap());
        payload.put("baseConfidence", context.baseConfidence());
        payload.put("analysisId", context.analysisId());
        payload.put("decisionId", context.decisionId());
        payload.put("traceId", context.traceId());
        payload.put("riskItems", List.of(Map.copyOf(risk)));
        payload.values().removeIf(java.util.Objects::isNull);
        eventService.publishToUser(scoped.userId(), new DashboardLiveEvent(symbol + "-risk-" + version,
                "ASSET_RISK_UPDATED", symbol, version, Instant.ofEpochMilli(eventTime), Instant.now(),
                Map.copyOf(payload)));

        if (!"ACTIVE".equals(assessment.effectiveExecutionState())) {
            Map<String, Object> planPayload = new java.util.LinkedHashMap<>();
            planPayload.put("sourceAnalysisId", context.analysisId());
            planPayload.put("sourceDecisionId", context.decisionId());
            planPayload.put("sourceTraceId", context.traceId());
            planPayload.put("planState", assessment.effectiveExecutionState());
            planPayload.put("blockedReason", String.join("、", assessment.reasonCodes()));
            planPayload.put("recoveryCondition", "新AnalysisRun完成重验且实时冲击退出");
            planPayload.values().removeIf(java.util.Objects::isNull);
            eventService.publishToUser(scoped.userId(), new DashboardLiveEvent(symbol + "-plan-" + (version + 1),
                    "PLAN_STATE_CHANGED", symbol, version + 1, Instant.ofEpochMilli(eventTime), Instant.now(),
                    Map.copyOf(planPayload)));
            if (positionMonitorScheduler != null) {
                positionMonitorScheduler.requestImmediateRiskMonitor(
                        assessment.reasonCodes().isEmpty() ? "REALTIME_SHOCK"
                                : assessment.reasonCodes().get(0));
            }
        }
    }

    private BigDecimal latestReturn(String symbol, String timeframe) {
        return latestKlineReturns.getOrDefault(symbol + "|" + timeframe, BigDecimal.ZERO);
    }

    private static boolean crossed(String direction, BigDecimal price, BigDecimal level) {
        if (!positive(price) || !positive(level) || direction == null) return false;
        return direction.contains("BULLISH") ? price.compareTo(level) <= 0
                : direction.contains("BEARISH") && price.compareTo(level) >= 0;
    }

    private static String riskType(String state) {
        return switch (state) {
            case "STRUCTURE_INVALIDATED" -> "DIRECTION_CONFLICT_RISK";
            case "DIRECTION_STALE", "DATA_SOURCE_DEGRADED" -> "DATA_FRESHNESS_RISK";
            default -> "RAPID_MOVE_RISK";
        };
    }

    private static String riskTypeLabel(String state) {
        return switch (state) {
            case "STRUCTURE_INVALIDATED" -> "方向失效风险";
            case "DIRECTION_STALE", "DATA_SOURCE_DEGRADED" -> "数据时效风险";
            default -> "急涨急跌风险";
        };
    }

    private void updateBook(String symbol, JsonNode data, long eventTime) {
        BigDecimal bid = decimal(data, "b");
        BigDecimal ask = decimal(data, "a");
        if (positive(bid) && positive(ask) && ask.compareTo(bid) >= 0) {
            topQuotes.put(symbol, new TopQuote(bid, ask, eventTime, data.path("u").asLong(0)));
        }
    }

    private void processDepth(String symbol, JsonNode data, long eventTime) {
        long first = data.path("U").asLong(0);
        long last = data.path("u").asLong(0);
        Long previous = data.hasNonNull("pu") ? data.path("pu").asLong() : null;
        BinanceOrderBook book = depthBooks.computeIfAbsent(symbol, ignored -> new BinanceOrderBook());
        BinanceOrderBook.ApplyResult result = book.apply(new BinanceOrderBook.DepthDelta(
                first, last, previous, levels(data.path("b")), levels(data.path("a"))));
        if (result == BinanceOrderBook.ApplyResult.GAP) {
            publishSourceState(symbol, eventTime, "DEGRADED", "BINANCE_DEPTH_SEQUENCE_GAP");
            requestDepthSnapshot(symbol);
        }
    }

    private void requestDepthSnapshot(String symbol) {
        AtomicBoolean requesting = depthSnapshotRequests.computeIfAbsent(symbol, ignored -> new AtomicBoolean());
        if (!requesting.compareAndSet(false, true)) return;
        URI uri = URI.create("https://fapi.binance.com/fapi/v1/depth?symbol=" + symbol + "&limit=100");
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(8)).GET().build();
        httpClient.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, failure) -> {
                    requesting.set(false);
                    if (failure != null || response == null || response.statusCode() != 200) {
                        publishSourceState(symbol, System.currentTimeMillis(), "DEGRADED",
                                "BINANCE_DEPTH_SNAPSHOT_UNAVAILABLE");
                        return;
                    }
                    try {
                        JsonNode snapshot = objectMapper.readTree(response.body());
                        BinanceOrderBook book = depthBooks.computeIfAbsent(symbol, ignored -> new BinanceOrderBook());
                        BinanceOrderBook.ApplyResult result = book.applySnapshot(snapshot.path("lastUpdateId").asLong(0),
                                levels(snapshot.path("bids")), levels(snapshot.path("asks")));
                        if (result == BinanceOrderBook.ApplyResult.GAP) {
                            book.invalidate();
                            publishSourceState(symbol, System.currentTimeMillis(), "DEGRADED",
                                    "BINANCE_DEPTH_REPLAY_GAP");
                            requestDepthSnapshot(symbol);
                        }
                    } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException malformed) {
                        depthBooks.computeIfAbsent(symbol, ignored -> new BinanceOrderBook()).invalidate();
                        publishSourceState(symbol, System.currentTimeMillis(), "DEGRADED",
                                "BINANCE_DEPTH_SNAPSHOT_INVALID");
                    }
                });
    }

    private void publishSourceState(String symbol, long eventTime, String state, String reason) {
        long version = version(symbol, eventTime);
        eventService.publish(new DashboardLiveEvent(symbol + "-source-" + version,
                "DATA_SOURCE_STATUS_CHANGED", symbol, version, Instant.ofEpochMilli(eventTime), Instant.now(),
                Map.of("source", "BINANCE_DEPTH", "state", state, "reasonCode", reason,
                        "version", V41RealtimeShockPolicy.VERSION)));
    }

    private BinanceOrderBook.Snapshot depthSnapshot(String symbol, long eventTime) {
        BinanceOrderBook book = depthBooks.get(symbol);
        return book == null ? null : book.snapshot(eventTime);
    }

    private static List<BinanceOrderBook.Level> levels(JsonNode rows) {
        if (rows == null || !rows.isArray()) return List.of();
        List<BinanceOrderBook.Level> result = new ArrayList<>();
        for (JsonNode row : rows) {
            if (!row.isArray() || row.size() < 2) continue;
            try {
                result.add(new BinanceOrderBook.Level(new BigDecimal(row.get(0).asText()),
                        new BigDecimal(row.get(1).asText())));
            } catch (RuntimeException ignored) {
                // Invalid levels are excluded; continuity still advances only on the containing event.
            }
        }
        return List.copyOf(result);
    }

    private boolean acceptSequence(String stream, long eventTime) {
        Long previous = lastEventTimeByStream.putIfAbsent(stream, eventTime);
        if (previous == null) return true;
        if (eventTime <= previous) return false;
        return lastEventTimeByStream.replace(stream, previous, eventTime) || acceptSequence(stream, eventTime);
    }

    private long version(String symbol, long eventTime) {
        return versions.computeIfAbsent(symbol, ignored -> new AtomicLong())
                .updateAndGet(previous -> Math.max(previous + 1, eventTime));
    }

    private void remember(String symbol, BigDecimal price, long eventTime) {
        ArrayDeque<TimedPrice> values = recentPrices.computeIfAbsent(symbol, ignored -> new ArrayDeque<>());
        synchronized (values) {
            values.addLast(new TimedPrice(price, eventTime));
            long cutoff = eventTime - 5 * 60_000L;
            while (!values.isEmpty() && values.getFirst().eventTime() < cutoff) values.removeFirst();
        }
    }

    private static BigDecimal spreadBps(BigDecimal bid, BigDecimal ask) {
        BigDecimal mid = ask.add(bid).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
        return ask.subtract(bid).divide(mid, 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(10_000)).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        try { return new BigDecimal(node.path(field).asText()); }
        catch (RuntimeException ignored) { return null; }
    }

    private static boolean positive(BigDecimal value) { return value != null && value.signum() > 0; }

    private static String streamUri() {
        List<String> streams = new ArrayList<>();
        for (String symbol : SYMBOLS) {
            streams.add(symbol + "@markPrice@1s");
            streams.add(symbol + "@bookTicker");
            streams.add(symbol + "@depth@500ms");
            streams.add(symbol + "@kline_1m");
            streams.add(symbol + "@kline_5m");
        }
        return "wss://fstream.binance.com/stream?streams=" + String.join("/", streams);
    }

    @PreDestroy
    public void close() {
        WebSocket socket = webSocket;
        webSocket = null;
        if (socket != null) socket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
    }

    private record TimedPrice(BigDecimal price, long eventTime) { }
    private record TopQuote(BigDecimal bid, BigDecimal ask, long eventTime, long sequence) { }
    private record ShockEmission(String signature, long emittedAtMillis, String decisionId) { }
}
