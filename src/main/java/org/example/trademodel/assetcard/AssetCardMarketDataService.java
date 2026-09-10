package org.example.trademodel.assetcard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.example.trademodel.mapper.AssetCardMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Independent, public Spot transport. Never calls a trading, analysis, AI or account API. */
@Service
public class AssetCardMarketDataService {
    private static final Logger log = LoggerFactory.getLogger(AssetCardMarketDataService.class);
    private static final List<String> INTERVALS = List.of("1m", "5m", "15m", "1h", "4h");
    private static final int MAX_DEPTH_LEVELS = 5000;
    private static final int MAX_BUFFERED_DEPTH_EVENTS = 256;
    private static final int MAX_BUFFERED_DEPTH_LEVELS = 20_000;
    private static final int BOOK_HISTORY_LIMIT = 16;
    private static final Duration BOOTSTRAP_GAP = Duration.ofSeconds(60);
    private final AssetCardProperties properties;
    private final ObjectMapper json;
    private final AssetCardMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final AtomicBoolean connecting = new AtomicBoolean();
    private final Map<String, SpotQuote> quotes = new ConcurrentHashMap<>();
    private final Map<String, SpotBook> books = new ConcurrentHashMap<>();
    private final Map<String, DepthState> depthStates = new ConcurrentHashMap<>();
    private final Map<String, ArrayDeque<SpotBook>> bookHistory = new ConcurrentHashMap<>();
    private final Map<String, Instant> depthRetryAfter = new ConcurrentHashMap<>();
    private Instant nextDepthBootstrapAt = Instant.MIN;
    private long connectionEpoch;
    private final Map<String, TreeMap<Instant, SpotBar>> closedBars = new ConcurrentHashMap<>();
    private final List<Consumer<MarketUpdate>> listeners = new CopyOnWriteArrayList<>();
    private final ThreadPoolExecutor frames = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(2048), runnable -> {
                Thread thread = new Thread(runnable, "asset-card-spot-events");
                thread.setDaemon(true);
                return thread;
            });
    private volatile Set<String> desiredSymbols = Set.of();
    private volatile Set<String> connectionSymbols = Set.of();
    private volatile WebSocket socket;
    private volatile Instant connectedAt;
    private volatile boolean stopped;

    public AssetCardMarketDataService(AssetCardProperties properties, ObjectMapper json, AssetCardMapper mapper) {
        this.properties = properties;
        this.json = json;
        this.mapper = mapper;
    }

    /** Background lifecycle only: callers supply the existing pool/card union; this never changes that union. */
    public synchronized void reconcileSubscriptions(Collection<String> symbols) {
        TreeSet<String> next = new TreeSet<>();
        if (symbols != null) symbols.forEach(symbol -> {
            String value = normalize(symbol);
            if (!value.isEmpty()) next.add(value);
        });
        if (next.size() > 128) throw new IllegalArgumentException("Card stream capacity exceeded; no symbols were truncated");
        desiredSymbols = Set.copyOf(next);
        quotes.keySet().removeIf(symbol -> !next.contains(symbol));
        books.keySet().removeIf(symbol -> !next.contains(symbol));
        depthStates.keySet().removeIf(symbol -> !next.contains(symbol));
        bookHistory.keySet().removeIf(symbol -> !next.contains(symbol));
        depthRetryAfter.keySet().removeIf(symbol -> !next.contains(symbol));
        closedBars.keySet().removeIf(key -> !next.contains(key.substring(0, key.indexOf('|'))));
    }

    public Set<String> subscribedSymbols() { return desiredSymbols; }
    public void addListener(Consumer<MarketUpdate> listener) { listeners.add(java.util.Objects.requireNonNull(listener)); }

    public Optional<SpotQuote> quote(String symbol, Instant asOf) {
        SpotQuote value = quotes.get(normalize(symbol));
        return value != null && fresh(value.observedAt(), value.availableAt(), asOf) ? Optional.of(value) : Optional.empty();
    }

    public synchronized Optional<SpotBook> book(String symbol, Instant asOf) {
        ArrayDeque<SpotBook> history = bookHistory.get(normalize(symbol));
        if (history == null || asOf == null) return Optional.empty();
        var iterator = history.descendingIterator();
        while (iterator.hasNext()) {
            SpotBook value = iterator.next();
            if (fresh(value.observedAt(), value.availableAt(), asOf)) return Optional.of(value);
        }
        return Optional.empty();
    }

    /** Pure read. Historical values retain their real availability; there is no registration or backfill. */
    public List<SpotBar> bars(String symbol, String interval, Instant asOf, int limit) {
        String normalized = normalize(symbol);
        if (normalized.isEmpty() || !INTERVALS.contains(interval) || asOf == null || limit < 1) return List.of();
        int maximum = Math.min(limit, properties.getRetainedBarsPerInterval());
        TreeMap<Instant, SpotBar> result = new TreeMap<>();
        List<SpotBar> own = mapper.selectClosedBars(normalized, interval, asOf, maximum);
        if (own != null) own.forEach(bar -> result.put(bar.openTime(), bar));
        // A full but stale own window must not hide newer, already-persisted Spot bars.
        List<SpotBar> existing = mapper.selectExistingSpotBars(normalized, interval, asOf, maximum);
        if (existing != null) existing.forEach(bar -> result.putIfAbsent(bar.openTime(), bar));
        return result.values().stream().filter(bar -> !bar.closeTime().isAfter(asOf) && !bar.availableAt().isAfter(asOf))
                .skip(Math.max(0, result.size() - maximum)).toList();
    }

    @Scheduled(fixedDelayString = "${trade-model.asset-card.connection-check-ms:5000}", initialDelay = 1000L)
    public synchronized void ensureConnected() {
        if (stopped) return;
        if (!properties.isEnabled() || desiredSymbols.isEmpty()) {
            disconnect();
            return;
        }
        if (socket != null && (!connectionSymbols.equals(desiredSymbols)
                || connectedAt != null && connectedAt.plus(Duration.ofHours(23)).isBefore(Instant.now()))) disconnect();
        if (socket != null) { bootstrapPendingBook(Instant.now()); return; }
        if (!connecting.compareAndSet(false, true)) return;
        Set<String> subscribed = desiredSymbols;
        http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10))
                .buildAsync(streamUri(subscribed), new SpotListener(subscribed))
                .whenComplete((opened, error) -> {
                    connecting.set(false);
                    if (error != null) {
                        log.warn("[asset-card] Spot connection failed ({})", error.getClass().getSimpleName());
                        subscribed.forEach(symbol -> notifyListeners(new MarketUpdate(symbol, "HEALTH", Instant.now())));
                    }
                });
    }

    private URI streamUri(Set<String> symbols) {
        List<String> streams = new ArrayList<>();
        symbols.stream().sorted().forEach(symbol -> {
            String prefix = symbol.toLowerCase(Locale.ROOT);
            streams.add(prefix + "@trade");
            streams.add(prefix + "@depth@100ms");
            INTERVALS.forEach(interval -> streams.add(prefix + "@kline_" + interval));
        });
        return URI.create(properties.getSpotStreamBaseUri() + "?streams=" + String.join("/", streams));
    }

    private final class SpotListener implements WebSocket.Listener {
        private final Set<String> symbols;
        private final StringBuilder buffer = new StringBuilder();
        private SpotListener(Set<String> symbols) { this.symbols = symbols; }
        @Override public void onOpen(WebSocket opened) {
            synchronized (AssetCardMarketDataService.this) {
                if (stopped || !properties.isEnabled() || !symbols.equals(desiredSymbols)) {
                    opened.abort(); return;
                }
                socket = opened;
                connectionEpoch++;
                depthStates.clear(); bookHistory.clear(); books.clear();
                connectionSymbols = symbols;
                connectedAt = Instant.now();
            }
            opened.request(1);
        }
        @Override public CompletionStage<?> onText(WebSocket opened, CharSequence data, boolean last) {
            synchronized (buffer) {
                if (buffer.length() + data.length() > 1_048_576) {
                    buffer.setLength(0); lost(opened, symbols); opened.abort();
                    return CompletableFuture.completedFuture(null);
                }
                buffer.append(data);
                if (last) {
                    String message = buffer.toString();
                    Instant receivedAt = Instant.now();
                    buffer.setLength(0);
                    try { frames.execute(() -> { if (socket == opened) acceptMessage(message, receivedAt); }); }
                    catch (RejectedExecutionException overloaded) {
                        lost(opened, symbols);
                        opened.abort();
                    }
                }
            }
            opened.request(1);
            return CompletableFuture.completedFuture(null);
        }
        @Override public CompletionStage<?> onClose(WebSocket closed, int status, String reason) {
            lost(closed, symbols); return CompletableFuture.completedFuture(null);
        }
        @Override public void onError(WebSocket failed, Throwable error) { lost(failed, symbols); }
    }

    private synchronized void lost(WebSocket previous, Set<String> symbols) {
        if (socket == previous) {
            socket = null;
            connectionEpoch++;
            connectionSymbols = Set.of();
            clearLiveData(symbols);
        }
    }
    private void clearLiveData(Set<String> symbols) {
        symbols.forEach(symbol -> {
            quotes.remove(symbol); books.remove(symbol); bookHistory.remove(symbol); depthStates.remove(symbol);
            notifyListeners(new MarketUpdate(symbol, "HEALTH", Instant.now()));
        });
    }

    /** Package-private deterministic ingestion seam; never opens a network connection. */
    synchronized void acceptMessage(String message, Instant receivedAt) {
        try {
            JsonNode root = json.readTree(message);
            String stream = root.path("stream").asText("");
            int separator = stream.indexOf('@');
            if (separator <= 0 || receivedAt == null) return;
            String symbol = normalize(stream.substring(0, separator));
            if (!desiredSymbols.contains(symbol)) return;
            JsonNode data = root.path("data");
            if (data.has("s") && !symbol.equals(data.path("s").asText())) return;
            if (stream.endsWith("@trade") && "trade".equals(data.path("e").asText())) acceptTrade(symbol, data, receivedAt);
            else if (stream.endsWith("@depth@100ms") && "depthUpdate".equals(data.path("e").asText()))
                acceptDiffDepth(symbol, data, receivedAt);
            else if (stream.contains("@kline_") && "kline".equals(data.path("e").asText())) acceptBar(symbol, stream, data, receivedAt);
        } catch (RuntimeException | java.io.IOException failure) {
            log.warn("[asset-card] Spot frame rejected ({})", failure.getClass().getSimpleName());
        }
    }

    private void acceptTrade(String symbol, JsonNode data, Instant receivedAt) {
        BigDecimal price = decimal(data, "p"), quantity = decimal(data, "q");
        long id = data.path("t").asLong(-1), time = data.path("T").asLong(-1);
        if (!positive(price) || !positive(quantity) || id < 0 || time < 0 || time > receivedAt.toEpochMilli()) return;
        Instant observedAt = Instant.ofEpochMilli(time);
        SpotQuote previous = quotes.get(symbol);
        if (previous != null && (id <= previous.tradeId() || observedAt.isBefore(previous.observedAt()))) return;
        quotes.put(symbol, new SpotQuote(symbol, price, quantity, id, observedAt, receivedAt));
        notifyListeners(new MarketUpdate(symbol, "PRICE", observedAt));
    }

    private void acceptDiffDepth(String symbol, JsonNode data, Instant receivedAt) {
        long first = data.path("U").asLong(-1), last = data.path("u").asLong(-1), time = data.path("E").asLong(-1);
        List<Level> bids = depthLevels(data.path("b"), true), asks = depthLevels(data.path("a"), true);
        if (first < 0 || last < first || time < 0 || time > receivedAt.toEpochMilli() || bids == null || asks == null) {
            invalidateDepth(symbol); return;
        }
        DepthDelta delta = new DepthDelta(first, last, bids, asks, Instant.ofEpochMilli(time), receivedAt);
        DepthState state = depthStates.computeIfAbsent(symbol, ignored -> new DepthState());
        if (state.sequence < 0) { bufferDepth(state, delta); return; }
        if (last <= state.sequence) return;
        if (first > state.sequence + 1 || state.observedAt != null && delta.observedAt().isBefore(state.observedAt)) {
            invalidateDepth(symbol);
            bufferDepth(depthStates.computeIfAbsent(symbol, ignored -> new DepthState()), delta);
            return;
        }
        if (!applyDelta(state, delta)) { invalidateDepth(symbol); return; }
        publishDepth(symbol, state, receivedAt);
    }

    private void bufferDepth(DepthState state, DepthDelta delta) {
        if (!state.pending.isEmpty() && delta.last() <= state.pending.getLast().last()) return;
        int count = delta.bids().size() + delta.asks().size();
        if (state.pending.size() >= MAX_BUFFERED_DEPTH_EVENTS || state.bufferedLevels + count > MAX_BUFFERED_DEPTH_LEVELS) {
            state.pending.clear(); state.bufferedLevels = 0;
        }
        state.pending.addLast(delta); state.bufferedLevels += count;
    }

    /** Deterministic, in-memory test seam. The network caller additionally checks socket epoch and state identity. */
    synchronized void acceptDepthSnapshot(String symbol, String message, Instant receivedAt) {
        String normalized = normalize(symbol);
        if (!desiredSymbols.contains(normalized) || receivedAt == null) return;
        DepthState state = depthStates.get(normalized);
        if (state == null || state.sequence >= 0) return;
        try {
            JsonNode snapshot = json.readTree(message);
            long sequence = snapshot.path("lastUpdateId").asLong(-1);
            List<Level> bids = depthLevels(snapshot.path("bids"), false), asks = depthLevels(snapshot.path("asks"), false);
            if (sequence < 0 || bids == null || asks == null || bids.isEmpty() || asks.isEmpty()
                    || !state.pending.isEmpty() && sequence < state.pending.getFirst().first()) {
                recordDepthBootstrapFailure(normalized, 0, null, receivedAt); return;
            }
            state.bids.clear(); state.asks.clear();
            bids.forEach(level -> state.bids.put(level.price(), level));
            asks.forEach(level -> state.asks.put(level.price(), level));
            if (state.bids.firstKey().compareTo(state.asks.firstKey()) >= 0) {
                invalidateDepth(normalized); return;
            }
            state.bidFloor = state.bids.lastKey(); state.askCeiling = state.asks.lastKey();
            state.sequence = sequence; state.snapshotAvailableAt = receivedAt;
            while (!state.pending.isEmpty()) {
                DepthDelta delta = state.pending.removeFirst();
                if (delta.last() <= state.sequence) continue;
                if (delta.first() > state.sequence + 1 || !applyDelta(state, delta)) {
                    invalidateDepth(normalized);
                    bufferDepth(depthStates.computeIfAbsent(normalized, ignored -> new DepthState()), delta);
                    return;
                }
            }
            state.bufferedLevels = 0;
            // A bare REST snapshot has no exchange time and cannot be published as a synchronized event.
            if (state.observedAt != null) publishDepth(normalized, state, receivedAt);
        } catch (RuntimeException | java.io.IOException invalid) {
            invalidateDepth(normalized);
            recordDepthBootstrapFailure(normalized, 0, null, receivedAt);
        }
    }

    private boolean applyDelta(DepthState state, DepthDelta delta) {
        if (state.observedAt != null && delta.observedAt().isBefore(state.observedAt)) return false;
        for (Level level : delta.bids()) {
            if (level.price().compareTo(state.bidFloor) < 0) continue;
            if (level.quantity().signum() == 0) state.bids.remove(level.price()); else state.bids.put(level.price(), level);
        }
        for (Level level : delta.asks()) {
            if (level.price().compareTo(state.askCeiling) > 0) continue;
            if (level.quantity().signum() == 0) state.asks.remove(level.price()); else state.asks.put(level.price(), level);
        }
        if (state.bids.isEmpty() || state.asks.isEmpty() || state.bids.size() > MAX_DEPTH_LEVELS * 2
                || state.asks.size() > MAX_DEPTH_LEVELS * 2 || state.bids.firstKey().compareTo(state.asks.firstKey()) >= 0) return false;
        state.sequence = delta.last(); state.observedAt = delta.observedAt();
        state.availableAt = delta.availableAt().isAfter(state.snapshotAvailableAt) ? delta.availableAt() : state.snapshotAvailableAt;
        return true;
    }

    private void publishDepth(String symbol, DepthState state, Instant receivedAt) {
        if (state.observedAt == null || state.lastPublishedAt != null
                && receivedAt.isBefore(state.lastPublishedAt.plusSeconds(1))) return;
        SpotBook value = new SpotBook(symbol, List.copyOf(state.bids.values()), List.copyOf(state.asks.values()),
                state.sequence, state.observedAt, state.availableAt, "EXCHANGE_EVENT", state.bidFloor, state.askCeiling);
        ArrayDeque<SpotBook> history = bookHistory.computeIfAbsent(symbol, ignored -> new ArrayDeque<>());
        history.addLast(value);
        while (history.size() > BOOK_HISTORY_LIMIT) history.removeFirst();
        books.put(symbol, value); state.lastPublishedAt = receivedAt;
        notifyListeners(new MarketUpdate(symbol, "BOOK", state.observedAt));
    }

    private void invalidateDepth(String symbol) {
        depthStates.remove(symbol); bookHistory.remove(symbol); books.remove(symbol);
        notifyListeners(new MarketUpdate(symbol, "HEALTH", Instant.now()));
    }

    /** Called only by the opt-in background connection lifecycle, never by quote/book/bars or a page request. */
    private void bootstrapPendingBook(Instant now) {
        if (!properties.isEnabled() || socket == null || stopped) return;
        for (String symbol : desiredSymbols.stream().sorted().toList()) {
            DepthState state = depthStates.get(symbol);
            if (state == null || state.sequence >= 0 || state.bootstrapInFlight || state.pending.isEmpty()
                    || !claimDepthBootstrapBudget(symbol, now)) continue;
            state.bootstrapInFlight = true;
            WebSocket expectedSocket = socket;
            long expectedEpoch = connectionEpoch;
            URI uri = URI.create(properties.getSpotDepthSnapshotUri() + "?symbol=" + symbol + "&limit=" + MAX_DEPTH_LEVELS);
            HttpRequest request = HttpRequest.newBuilder(uri).GET().timeout(Duration.ofSeconds(15)).build();
            http.sendAsync(request, ignored -> new LimitedDepthBodySubscriber())
                    .whenComplete((response, failure) -> completeDepthBootstrap(symbol, expectedSocket, expectedEpoch,
                            state, response, failure, Instant.now()));
            return; // One card bootstrap globally per 60 seconds, including errors and connection changes.
        }
    }

    private synchronized void completeDepthBootstrap(String symbol, WebSocket expectedSocket, long expectedEpoch,
                                                      DepthState expectedState, HttpResponse<String> response,
                                                      Throwable failure, Instant receivedAt) {
        // A rate limit applies to the shared public endpoint even if its connection has since been replaced.
        if (response != null && (response.statusCode() == 429 || response.statusCode() == 418))
            recordDepthBootstrapFailure(symbol, response.statusCode(), response.headers().firstValue("Retry-After").orElse(null), receivedAt);
        // Stale responses must not insert data into a replacement connection or subscription.
        if (stopped || !properties.isEnabled() || socket != expectedSocket || connectionEpoch != expectedEpoch
                || !desiredSymbols.contains(symbol) || depthStates.get(symbol) != expectedState) return;
        expectedState.bootstrapInFlight = false;
        if (failure != null || response == null) {
            recordDepthBootstrapFailure(symbol, 0, null, receivedAt);
            return;
        }
        if (response.statusCode() != 200) {
            recordDepthBootstrapFailure(symbol, response.statusCode(), response.headers().firstValue("Retry-After").orElse(null), receivedAt);
            return;
        }
        acceptDepthSnapshot(symbol, response.body(), receivedAt);
    }

    /** Pure local reservation seam for deterministic cooldown tests; does not perform any HTTP request. */
    synchronized boolean claimDepthBootstrapBudget(String symbol, Instant now) {
        String normalized = normalize(symbol);
        if (normalized.isEmpty() || now == null || now.isBefore(nextDepthBootstrapAt)
                || now.isBefore(depthRetryAfter.getOrDefault(normalized, Instant.MIN))) return false;
        nextDepthBootstrapAt = now.plus(BOOTSTRAP_GAP);
        depthRetryAfter.put(normalized, now.plus(BOOTSTRAP_GAP));
        return true;
    }

    synchronized void recordDepthBootstrapFailure(String symbol, int status, String retryAfter, Instant now) {
        if (now == null) return;
        Instant retryAt = now.plus(BOOTSTRAP_GAP);
        if ((status == 429 || status == 418) && retryAfter != null) {
            try {
                Instant instructed = retryAfter.trim().matches("[0-9]+")
                        ? now.plusSeconds(Long.parseLong(retryAfter.trim()))
                        : ZonedDateTime.parse(retryAfter.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                if (instructed.isAfter(retryAt)) retryAt = instructed;
            } catch (RuntimeException invalidHeader) { /* Keep the conservative minimum; never retry immediately. */ }
        }
        depthRetryAfter.merge(normalize(symbol), retryAt, (left, right) -> left.isAfter(right) ? left : right);
        if ((status == 429 || status == 418) && retryAt.isAfter(nextDepthBootstrapAt)) nextDepthBootstrapAt = retryAt;
    }

    synchronized int retainedBookSnapshotCount(String symbol) {
        ArrayDeque<SpotBook> history = bookHistory.get(normalize(symbol));
        return history == null ? 0 : history.size();
    }

    synchronized int bufferedDepthEventCount(String symbol) {
        DepthState state = depthStates.get(normalize(symbol));
        return state == null ? 0 : state.pending.size();
    }

    private static List<Level> depthLevels(JsonNode node, boolean allowZero) {
        if (!node.isArray() || node.size() > MAX_DEPTH_LEVELS) return null;
        List<Level> levels = new ArrayList<>();
        Set<BigDecimal> prices = new TreeSet<>();
        for (JsonNode level : node) {
            if (!level.isArray() || level.size() != 2) return null;
            BigDecimal price = new BigDecimal(level.get(0).asText()), quantity = new BigDecimal(level.get(1).asText());
            if (!positive(price) || quantity.signum() < 0 || !allowZero && quantity.signum() == 0 || !prices.add(price)) return null;
            levels.add(new Level(price, quantity));
        }
        return List.copyOf(levels);
    }

    private static final class DepthState {
        private final TreeMap<BigDecimal, Level> bids = new TreeMap<>(Comparator.reverseOrder());
        private final TreeMap<BigDecimal, Level> asks = new TreeMap<>();
        private final ArrayDeque<DepthDelta> pending = new ArrayDeque<>();
        private long sequence = -1;
        private int bufferedLevels;
        private boolean bootstrapInFlight;
        private BigDecimal bidFloor, askCeiling;
        private Instant snapshotAvailableAt, observedAt, availableAt, lastPublishedAt;
    }

    private record DepthDelta(long first, long last, List<Level> bids, List<Level> asks,
                              Instant observedAt, Instant availableAt) {}

    /** Bounds the public REST response before decoding, including error responses. */
    private static final class LimitedDepthBodySubscriber implements HttpResponse.BodySubscriber<String> {
        private static final int MAX_BYTES = 2_097_152;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final CompletableFuture<String> result = new CompletableFuture<>();
        private Flow.Subscription subscription;
        @Override public CompletionStage<String> getBody() { return result; }
        @Override public void onSubscribe(Flow.Subscription value) { subscription = value; value.request(1); }
        @Override public void onNext(List<ByteBuffer> chunks) {
            for (ByteBuffer chunk : chunks) {
                if (chunk.remaining() > MAX_BYTES - bytes.size()) {
                    subscription.cancel();
                    result.completeExceptionally(new IllegalArgumentException("ASSET_CARD_DEPTH_RESPONSE_TOO_LARGE"));
                    return;
                }
                byte[] part = new byte[chunk.remaining()]; chunk.get(part); bytes.writeBytes(part);
            }
            subscription.request(1);
        }
        @Override public void onError(Throwable failure) { result.completeExceptionally(failure); }
        @Override public void onComplete() { result.complete(bytes.toString(StandardCharsets.UTF_8)); }
    }

    private void acceptBar(String symbol, String stream, JsonNode data, Instant receivedAt) {
        JsonNode bar = data.path("k");
        String interval = bar.path("i").asText();
        if (!bar.path("x").asBoolean() || !INTERVALS.contains(interval) || !stream.endsWith("@kline_" + interval)
                || bar.has("s") && !symbol.equals(bar.path("s").asText())) return;
        long openMillis = bar.path("t").asLong(-1), closeMillis = bar.path("T").asLong(-1);
        long durationMillis = switch (interval) { case "1m" -> 60_000L; case "5m" -> 300_000L; case "15m" -> 900_000L; case "1h" -> 3_600_000L; default -> 14_400_000L; };
        if (openMillis < 0 || openMillis % durationMillis != 0 || closeMillis != openMillis + durationMillis - 1 || closeMillis > receivedAt.toEpochMilli()) return;
        BigDecimal open = decimal(bar, "o"), high = decimal(bar, "h"), low = decimal(bar, "l"), close = decimal(bar, "c"), volume = decimal(bar, "v");
        if (!positive(open) || !positive(high) || !positive(low) || !positive(close) || volume == null || volume.signum() < 0
                || high.compareTo(open.max(close)) < 0 || low.compareTo(open.min(close)) > 0 || low.compareTo(high) > 0) return;
        BigDecimal takerVolume = decimal(bar, "V");
        Long tradeCount = bar.has("n") ? bar.path("n").asLong() : null;
        if (takerVolume != null && (takerVolume.signum() < 0 || takerVolume.compareTo(volume) > 0)
                || tradeCount != null && tradeCount < 0) return;
        TreeMap<Instant, SpotBar> cache = closedBars.computeIfAbsent(symbol + "|" + interval, ignored -> new TreeMap<>());
        Instant openTime = Instant.ofEpochMilli(openMillis);
        if (cache.containsKey(openTime)) return;
        SpotBar value = new SpotBar(symbol, interval, openTime, Instant.ofEpochMilli(closeMillis), open, high, low, close,
                volume, takerVolume, tradeCount, receivedAt);
        int inserted = mapper.upsertClosedBar(value);
        cache.put(openTime, value);
        while (cache.size() > properties.getRetainedBarsPerInterval()) cache.pollFirstEntry();
        if (inserted > 0) notifyListeners(new MarketUpdate(symbol, "BAR", value.closeTime(), interval));
    }

    private void notifyListeners(MarketUpdate update) {
        for (Consumer<MarketUpdate> listener : listeners) {
            try { listener.accept(update); }
            catch (RuntimeException failure) { log.warn("[asset-card] Card consumer failed ({})", failure.getClass().getSimpleName()); }
        }
    }
    private boolean fresh(Instant observedAt, Instant availableAt, Instant asOf) {
        return asOf != null && !availableAt.isAfter(asOf) && !observedAt.isAfter(asOf)
                && Duration.between(observedAt, asOf).compareTo(properties.getPriceTtl()) <= 0;
    }
    private static String normalize(String symbol) {
        if (symbol == null) return "";
        String result = symbol.trim().toUpperCase(Locale.ROOT);
        return result.matches("[A-Z0-9]{2,32}") ? result : "";
    }
    private static BigDecimal decimal(JsonNode node, String field) {
        if (!node.hasNonNull(field)) return null;
        return new BigDecimal(node.path(field).asText());
    }
    private static boolean positive(BigDecimal value) { return value != null && value.signum() > 0; }
    private synchronized void disconnect() {
        WebSocket previous = socket;
        socket = null;
        connectionEpoch++;
        connectionSymbols = Set.of();
        if (previous != null) { previous.abort(); clearLiveData(desiredSymbols); }
    }
    @PreDestroy public void close() { stopped = true; disconnect(); frames.shutdownNow(); }

    public record SpotQuote(String symbol, BigDecimal price, BigDecimal quantity, long tradeId, Instant observedAt, Instant availableAt) {
        public String source() { return "BINANCE_SPOT_TRADE"; }
    }
    public record SpotBar(String symbol, String interval, Instant openTime, Instant closeTime, BigDecimal open,
                          BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume,
                          BigDecimal takerBuyBaseVolume, Long tradeCount, Instant availableAt) {}
    public record Level(BigDecimal price, BigDecimal quantity) {}
    public record SpotBook(String symbol, List<Level> bids, List<Level> asks, long sequence, Instant observedAt,
                           Instant availableAt, String timestampBasis, BigDecimal bidCoverageFloor, BigDecimal askCoverageCeiling) {
        public SpotBook { bids = List.copyOf(bids); asks = List.copyOf(asks); }
        public SpotBook(String symbol, List<Level> bids, List<Level> asks, long sequence, Instant observedAt,
                        Instant availableAt, String timestampBasis) {
            this(symbol, bids, asks, sequence, observedAt, availableAt, timestampBasis, null, null);
        }
        public String source() { return "BINANCE_SPOT_DIFF_DEPTH"; }
        /** True only for the known continuous snapshot interval, never inferred from isolated peripheral updates. */
        public boolean coversBasisPoints(int bps) {
            if (bps <= 0 || bps >= 10_000 || bids.isEmpty() || asks.isEmpty()
                    || !positive(bidCoverageFloor) || !positive(askCoverageCeiling)) return false;
            BigDecimal mid = bids.get(0).price().add(asks.get(0).price()).divide(BigDecimal.valueOf(2));
            BigDecimal width = BigDecimal.valueOf(bps).movePointLeft(4);
            return bidCoverageFloor.compareTo(mid.multiply(BigDecimal.ONE.subtract(width))) <= 0
                    && askCoverageCeiling.compareTo(mid.multiply(BigDecimal.ONE.add(width))) >= 0;
        }
    }
    public record MarketUpdate(String symbol, String type, Instant observedAt, String interval) {
        public MarketUpdate(String symbol, String type, Instant observedAt) { this(symbol, type, observedAt, null); }
    }
}
