package org.example.trademodel.assetcard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.example.trademodel.mapper.AssetCardMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.LongSupplier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Independent, public Spot transport. Never calls a trading, analysis, AI or account API. */
@Service
public class AssetCardMarketDataService implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AssetCardMarketDataService.class);
    private static final List<String> INTERVALS = List.of("1m", "5m", "15m", "1h", "4h");
    private static final int MAX_DEPTH_LEVELS = 5000;
    private static final int MAX_BUFFERED_DEPTH_EVENTS = 256;
    private static final int MAX_BUFFERED_DEPTH_LEVELS = 20_000;
    private static final int BOOK_HISTORY_LIMIT = 16;
    private static final Duration BOOTSTRAP_GAP = Duration.ofSeconds(60);
    private static final int CONSUMER_SHARDS = 4;
    private static final int DEPTH_REQUEST_WEIGHT = 250; // Binance Spot limit=5000, shared IP REQUEST_WEIGHT.
    private final AssetCardProperties properties;
    private final ObjectMapper json;
    private final AssetCardMapper mapper;
    private final Environment environment;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final AtomicBoolean connecting = new AtomicBoolean();
    private final Map<String, SpotQuote> quotes = new ConcurrentHashMap<>();
    private final Map<String, SpotQuote> tradeWatermarks = new ConcurrentHashMap<>();
    private final Map<String, SpotBook> books = new ConcurrentHashMap<>();
    private final Map<String, DepthState> depthStates = new ConcurrentHashMap<>();
    private final Map<String, ArrayDeque<SpotBook>> bookHistory = new ConcurrentHashMap<>();
    private final Map<String, Instant> depthRetryAfter = new ConcurrentHashMap<>();
    private Instant nextDepthBootstrapAt = Instant.MIN;
    private Instant weightWindow = Instant.MIN;
    private int reservedWeight;
    private long connectionEpoch;
    private final Map<String, TreeMap<Instant, SpotBar>> closedBars = new ConcurrentHashMap<>();
    private final List<Consumer<MarketUpdate>> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<SpotQuote>> tradeListeners = new CopyOnWriteArrayList<>();
    private final ThreadPoolExecutor[] tradeFrames = executors("trade", 2048);
    private final ThreadPoolExecutor[] depthFrames = executors("depth", 512);
    private final ThreadPoolExecutor[] barFrames = executors("bar", 512);
    private final Map<String, SpotQuote> pendingPrices = new ConcurrentHashMap<>();
    private final Map<String, Instant> publishedPriceAt = new ConcurrentHashMap<>();
    private final Object priceLock = new Object();
    private final AtomicLong tradeDrops = new AtomicLong(), depthDrops = new AtomicLong(), barDrops = new AtomicLong();
    private final AtomicLong coalescedPrices = new AtomicLong(), depthRecoveries = new AtomicLong(), reconnects = new AtomicLong();
    private final AtomicLong maxProcessingLagMillis = new AtomicLong(), controlId = new AtomicLong();
    private volatile Instant reconnectAfter = Instant.MIN;
    private volatile int connectionFailures;
    private volatile Set<String> prioritySymbols = Set.of();
    private volatile boolean rolloverRequested;
    private volatile boolean controlInFlight;
    private volatile Instant nextControlAt = Instant.MIN;
    private volatile long pendingControlId;
    private volatile Set<String> pendingControlSymbols = Set.of();
    private volatile Instant controlSentAt;
    private volatile Set<String> desiredSymbols = Set.of();
    private volatile Set<String> connectionSymbols = Set.of();
    private volatile WebSocket socket;
    private volatile Instant connectedAt;
    private volatile boolean stopped;
    private volatile BooleanSupplier writerReadiness = () -> false;
    private final CollectionLease collectionLease;
    private final Set<CompletableFuture<?>> depthRequests = ConcurrentHashMap.newKeySet();

    public AssetCardMarketDataService(AssetCardProperties properties, ObjectMapper json, AssetCardMapper mapper) {
        this(properties, json, mapper, null);
    }

    @Autowired
    public AssetCardMarketDataService(AssetCardProperties properties, ObjectMapper json, AssetCardMapper mapper,
                                      Environment environment) {
        this.properties = properties;
        this.json = json;
        this.mapper = mapper;
        this.environment = environment;
        this.collectionLease = new CollectionLease(properties.getCollectionWindow(), json, mapper::storageUsage);
    }

    private static ThreadPoolExecutor[] executors(String kind, int capacity) {
        ThreadPoolExecutor[] result = new ThreadPoolExecutor[CONSUMER_SHARDS];
        for (int i = 0; i < result.length; i++) {
            String name = "asset-card-spot-" + kind + "-" + i;
            result[i] = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(capacity), runnable -> {
                Thread thread = new Thread(runnable, name); thread.setDaemon(true); return thread;
            });
        }
        return result;
    }

    boolean networkAllowed() {
        if (stopped || !properties.isEnabled() || !properties.isExternalCallsEnabled() || environment == null
                || !explicitOptIn("trade-model.provider-call.external-calls-enabled")) return false;
        boolean production = java.util.Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("prod") || profile.equals("production"));
        return !production || explicitOptIn("trade-model.provider-call.enabled")
                && explicitOptIn("trade-model.schedulers.enabled")
                && "EXPLICIT_OPT_IN".equals(environment.getProperty("trade-model.production.scheduler-policy"));
    }

    private boolean explicitOptIn(String key) {
        return "true".equalsIgnoreCase(environment.getProperty(key, "").trim());
    }

    /** Background lifecycle only: callers supply the existing pool/card union; this never changes that union. */
    public synchronized void reconcileSubscriptions(Collection<String> symbols) {
        reconcileSubscriptions(symbols, List.of());
    }

    public synchronized void reconcileSubscriptions(Collection<String> symbols, Collection<String> displayedSymbols) {
        TreeSet<String> next = new TreeSet<>();
        if (symbols != null) symbols.forEach(symbol -> {
            String value = normalize(symbol);
            if (!value.isEmpty()) next.add(value);
        });
        if (next.size() > 128) throw new IllegalArgumentException("Card stream capacity exceeded; no symbols were truncated");
        if (!properties.getCollectionWindow().getSymbols().isEmpty())
            next.retainAll(properties.getCollectionWindow().getSymbols());
        desiredSymbols = Set.copyOf(next);
        synchronized (priceLock) {
            quotes.keySet().removeIf(symbol -> !next.contains(symbol));
            tradeWatermarks.keySet().removeIf(symbol -> !next.contains(symbol));
            pendingPrices.keySet().removeIf(symbol -> !next.contains(symbol));
            publishedPriceAt.keySet().removeIf(symbol -> !next.contains(symbol));
        }
        prioritySymbols = displayedSymbols == null ? Set.of() : displayedSymbols.stream().map(AssetCardMarketDataService::normalize)
                .filter(next::contains).collect(java.util.stream.Collectors.toUnmodifiableSet());
        books.keySet().removeIf(symbol -> !next.contains(symbol));
        depthStates.keySet().removeIf(symbol -> !next.contains(symbol));
        bookHistory.keySet().removeIf(symbol -> !next.contains(symbol));
        depthRetryAfter.keySet().removeIf(symbol -> !next.contains(symbol));
        closedBars.keySet().removeIf(key -> !next.contains(key.substring(0, key.indexOf('|'))));
    }

    public Set<String> subscribedSymbols() { return desiredSymbols; }
    public void addListener(Consumer<MarketUpdate> listener) { listeners.add(java.util.Objects.requireNonNull(listener)); }
    public void addTradeListener(Consumer<SpotQuote> listener) { tradeListeners.add(java.util.Objects.requireNonNull(listener)); }
    public void setWriterReadiness(BooleanSupplier readiness) { writerReadiness = java.util.Objects.requireNonNull(readiness); }
    public String collectionStatus() { return collectionLease.status(); }
    public boolean collectionAccepting(Instant now) {
        return !stopped && properties.getModelMode() == AssetCardProperties.ModelMode.SHADOW && collectionLease.accepting(now);
    }
    void stopCollection(String reason, Instant now) {
        collectionLease.stop(reason, now);
        stopCardTransport();
    }
    private void stopCardTransport() {
        disconnect();
        pendingPrices.clear();
        for (ThreadPoolExecutor[] group : List.of(tradeFrames, depthFrames, barFrames))
            for (ThreadPoolExecutor executor : group) executor.getQueue().clear();
    }

    /** This watchdog owns only card transport; other schedulers and label evidence are not stopped. */
    @Scheduled(fixedDelay = 250L, initialDelay = 1000L)
    public void enforceCollectionWindow() {
        if (!properties.isEnabled() || !properties.isExternalCallsEnabled() || stopped) return;
        Instant now = Instant.now();
        if (!writerReadiness.getAsBoolean() || properties.getModelMode() != AssetCardProperties.ModelMode.SHADOW) { stopCardTransport(); return; }
        if (!collectionLease.check(now)) stopCardTransport();
    }

    public List<String> depthBootstrapOrder() {
        return desiredSymbols.stream().sorted(Comparator.comparing((String symbol) -> !prioritySymbols.contains(symbol))
                .thenComparing(symbol -> symbol)).toList();
    }

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
        if (!networkAllowed() || desiredSymbols.isEmpty()) {
            disconnect();
            return;
        }
        Instant now = Instant.now();
        if (!writerReadiness.getAsBoolean() || properties.getModelMode() != AssetCardProperties.ModelMode.SHADOW
                || !collectionLease.check(now)) { disconnect(); return; }
        if (socket != null) {
            if (controlInFlight && controlSentAt != null && !now.isBefore(controlSentAt.plusSeconds(10))) {
                controlInFlight = false; rolloverRequested = true;
                notifyRecovery("RECOVERY", now);
            }
            if (!rolloverRequested && connectedAt != null && now.isBefore(connectedAt.plus(Duration.ofHours(23)))) {
                reconcileConnection(now);
                bootstrapPendingBook(now);
                return;
            }
        }
        if (now.isBefore(reconnectAfter)) return;
        if (!connecting.compareAndSet(false, true)) return;
        if (!collectionLease.reserve("CONNECTION", now)) { connecting.set(false); disconnect(); return; }
        Set<String> subscribed = desiredSymbols;
        http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10))
                .buildAsync(streamUri(subscribed), new SpotListener(subscribed))
                .whenComplete((opened, error) -> {
                    connecting.set(false);
                    if (error != null) {
                        connectionFailed(Instant.now());
                        log.warn("[asset-card] Spot connection failed ({})", error.getClass().getSimpleName());
                        notifyRecovery("RECOVERY", Instant.now());
                    }
                });
    }

    URI streamUri(Set<String> symbols) {
        return URI.create(properties.getSpotStreamBaseUri() + "?streams=" + String.join("/", streams(symbols)));
    }

    private List<String> streams(Set<String> symbols) {
        List<String> streams = new ArrayList<>();
        symbols.stream().sorted().forEach(symbol -> {
            String prefix = symbol.toLowerCase(Locale.ROOT);
            streams.add(prefix + "@aggTrade");
            streams.add(prefix + "@depth@100ms");
            INTERVALS.forEach(interval -> streams.add(prefix + "@kline_" + interval));
        });
        return streams;
    }

    /** One JSON control message/second leaves room for the protocol's ping/pong budget. ACK commits membership. */
    private synchronized void reconcileConnection(Instant now) {
        if (!networkAllowed() || socket == null || controlInFlight || now.isBefore(nextControlAt)) return;
        Set<String> removed = new TreeSet<>(connectionSymbols); removed.removeAll(desiredSymbols);
        Set<String> added = new TreeSet<>(desiredSymbols); added.removeAll(connectionSymbols);
        if (removed.isEmpty() && added.isEmpty()) return;
        if (!collectionLease.reserve("CONTROL", now)) { disconnect(); return; }
        boolean unsubscribe = !removed.isEmpty();
        Set<String> changed = unsubscribe ? removed : added;
        Set<String> target = new TreeSet<>(connectionSymbols);
        if (unsubscribe) target.removeAll(changed); else target.addAll(changed);
        pendingControlId = controlId.incrementAndGet(); pendingControlSymbols = Set.copyOf(target);
        controlInFlight = true; controlSentAt = now; nextControlAt = now.plusSeconds(1);
        WebSocket expected = socket;
        try {
            String message = json.writeValueAsString(Map.of("method", unsubscribe ? "UNSUBSCRIBE" : "SUBSCRIBE",
                    "params", streams(changed), "id", pendingControlId));
            expected.sendText(message, true).whenComplete((sent, failure) -> {
                if (failure != null) synchronized (AssetCardMarketDataService.this) {
                    if (socket == expected) { controlInFlight = false; rolloverRequested = true; connectionFailed(Instant.now()); }
                }
            });
        } catch (RuntimeException | java.io.IOException failure) {
            controlInFlight = false; rolloverRequested = true; connectionFailed(now);
        }
    }

    private boolean transportControl(String message, Instant at) {
        try {
            JsonNode value = json.readTree(message);
            if (value.has("id")) {
                synchronized (this) {
                    if (controlInFlight && value.path("id").asLong(-1) == pendingControlId) {
                        controlInFlight = false;
                        if (value.has("result") && value.path("result").isNull()) connectionSymbols = pendingControlSymbols;
                        else { rolloverRequested = true; connectionFailed(at); }
                    }
                }
                return true;
            }
            if ("serverShutdown".equals(value.path("data").path("e").asText())) {
                rolloverRequested = true; return true;
            }
        } catch (RuntimeException | java.io.IOException ignored) { /* Ingestion reports malformed frames. */ }
        return false;
    }

    private synchronized void connectionFailed(Instant now) {
        connectionFailures = Math.min(connectionFailures + 1, 8);
        reconnectAfter = now.plusSeconds(Math.min(120L, 1L << connectionFailures));
        reconnects.incrementAndGet();
    }

    private void notifyRecovery(String type, Instant at) {
        desiredSymbols.forEach(symbol -> notifyListeners(new MarketUpdate(symbol, type, at)));
    }

    private final class SpotListener implements WebSocket.Listener {
        private final Set<String> symbols;
        private final StringBuilder buffer = new StringBuilder();
        private SpotListener(Set<String> symbols) { this.symbols = symbols; }
        @Override public void onOpen(WebSocket opened) {
            if (installConnection(opened, symbols, Instant.now())) opened.request(1);
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
                    if (socket == opened && networkAllowed() && collectionAccepting(receivedAt)
                            && !transportControl(message, receivedAt)) enqueueMessage(message, receivedAt);
                    else if (!collectionAccepting(receivedAt)) { opened.abort(); return CompletableFuture.completedFuture(null); }
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

    synchronized boolean installConnection(WebSocket opened, Set<String> symbols, Instant at) {
        if (!networkAllowed() || !collectionAccepting(at) || !symbols.equals(desiredSymbols)) { opened.abort(); return false; }
        WebSocket previous = socket;
        socket = opened; connectionEpoch++;
        // Make before break: a real sequence gap rebuilds only that book, not every displayed card.
        depthStates.values().forEach(state -> state.bootstrapInFlight = false);
        connectionSymbols = symbols; connectedAt = at;
        connectionFailures = 0; reconnectAfter = Instant.MIN; rolloverRequested = false; controlInFlight = false;
        if (previous != null && previous != opened) previous.abort();
        return true;
    }

    private synchronized void lost(WebSocket previous, Set<String> symbols) {
        if (socket == previous) {
            Set<String> lostSymbols = connectionSymbols;
            socket = null;
            connectionEpoch++;
            connectionSymbols = Set.of();
            controlInFlight = false;
            connectionFailed(Instant.now());
            clearLiveData(lostSymbols);
        }
    }
    private void clearLiveData(Set<String> symbols) {
        symbols.forEach(symbol -> {
            synchronized (priceLock) { quotes.remove(symbol); pendingPrices.remove(symbol); }
            books.remove(symbol); bookHistory.remove(symbol); depthStates.remove(symbol);
            notifyListeners(new MarketUpdate(symbol, "PRICE_FAILURE", Instant.now()));
            notifyListeners(new MarketUpdate(symbol, "RISK_FAILURE", Instant.now()));
        });
    }

    /** Package-private deterministic ingestion seam; never opens a network connection. */
    void acceptMessage(String message, Instant receivedAt) {
        Frame frame = decode(message, receivedAt);
        if (frame != null) process(frame);
    }

    /** Live ingress: latest price is O(1); observation, depth and closed-bar consumers have independent bounded shards. */
    void enqueueMessage(String message, Instant receivedAt) {
        Frame frame = decode(message, receivedAt);
        if (frame == null) return;
        if (frame.kind().equals("TRADE")) {
            try { acceptTrade(frame.symbol(), frame.data(), frame.receivedAt(), true); }
            catch (RuntimeException malformed) {
                tradeDrops.incrementAndGet(); notifyListeners(new MarketUpdate(frame.symbol(), "OBSERVATION_GAP", receivedAt));
            }
            return;
        }
        ThreadPoolExecutor[] group = frame.kind().equals("DEPTH") ? depthFrames : barFrames;
        submit(group, frame, () -> process(frame));
    }

    private void submit(ThreadPoolExecutor[] group, Frame frame, Runnable action) {
        try {
            group[Math.floorMod(frame.symbol().hashCode(), CONSUMER_SHARDS)].execute(() -> {
                if (!desiredSymbols.contains(frame.symbol())) return;
                if (properties.isExternalCallsEnabled() && !collectionAccepting(Instant.now())) return;
                long lag = Math.max(0, Duration.between(frame.receivedAt(), Instant.now()).toMillis());
                maxProcessingLagMillis.accumulateAndGet(lag, Math::max);
                action.run();
            });
        } catch (RejectedExecutionException full) {
            if (frame.kind().equals("TRADE")) {
                tradeDrops.incrementAndGet();
                notifyListeners(new MarketUpdate(frame.symbol(), "OBSERVATION_GAP", frame.receivedAt()));
            } else if (frame.kind().equals("DEPTH")) {
                depthDrops.incrementAndGet();
                synchronized (this) { invalidateDepth(frame.symbol()); }
            } else {
                barDrops.incrementAndGet();
                notifyListeners(new MarketUpdate(frame.symbol(), "BAR_DROPPED",
                        Instant.ofEpochMilli(frame.data().path("k").path("T").asLong()), frame.data().path("k").path("i").asText()));
            }
        }
    }

    private Frame decode(String message, Instant receivedAt) {
        try {
            JsonNode root = json.readTree(message);
            String stream = root.path("stream").asText("");
            int separator = stream.indexOf('@');
            if (separator <= 0 || receivedAt == null || stopped) return null;
            String symbol = normalize(stream.substring(0, separator));
            if (!desiredSymbols.contains(symbol)) return null;
            JsonNode data = root.path("data");
            if (!symbol.equals(data.path("s").asText())) return null;
            if (stream.endsWith("@aggTrade") && "aggTrade".equals(data.path("e").asText())) return new Frame(symbol, stream, data, receivedAt, "TRADE");
            else if (stream.endsWith("@depth@100ms") && "depthUpdate".equals(data.path("e").asText()))
                return new Frame(symbol, stream, data, receivedAt, "DEPTH");
            else if (stream.contains("@kline_") && "kline".equals(data.path("e").asText()) && data.path("k").path("x").asBoolean())
                return new Frame(symbol, stream, data, receivedAt, "BAR");
        } catch (RuntimeException | java.io.IOException failure) {
            log.warn("[asset-card] Spot frame rejected ({})", failure.getClass().getSimpleName());
        }
        return null;
    }

    private void process(Frame frame) {
        try {
            if (!desiredSymbols.contains(frame.symbol())) return;
            switch (frame.kind()) {
                case "TRADE" -> acceptTrade(frame.symbol(), frame.data(), frame.receivedAt(), false);
                case "DEPTH" -> acceptDiffDepth(frame.symbol(), frame.data(), frame.receivedAt());
                case "BAR" -> acceptBar(frame.symbol(), frame.stream(), frame.data(), frame.receivedAt());
                default -> { }
            }
        } catch (RuntimeException failure) {
            if (frame.kind().equals("DEPTH")) synchronized (this) { invalidateDepth(frame.symbol()); }
            notifyListeners(new MarketUpdate(frame.symbol(), frame.kind().equals("DEPTH") ? "RISK_FAILURE"
                    : frame.kind().equals("BAR") ? "PERSISTENCE_FAILURE" : "PRICE_FAILURE", frame.receivedAt()));
            log.warn("[asset-card] {} frame failed ({})", frame.kind(), failure.getClass().getSimpleName());
        }
    }

    private record Frame(String symbol, String stream, JsonNode data, Instant receivedAt, String kind) {}

    private void acceptTrade(String symbol, JsonNode data, Instant receivedAt, boolean asyncObservation) {
        BigDecimal price = decimal(data, "p"), quantity = decimal(data, "q");
        long id = data.path("a").asLong(-1), time = data.path("T").asLong(-1);
        if (!positive(price) || !positive(quantity) || id < 0 || time < 0 || time > receivedAt.toEpochMilli()) return;
        Instant observedAt = Instant.ofEpochMilli(time);
        SpotQuote value = new SpotQuote(symbol, price, quantity, id, observedAt, receivedAt);
        boolean publish;
        synchronized (priceLock) {
            if (!desiredSymbols.contains(symbol)) return;
            SpotQuote previous = tradeWatermarks.get(symbol);
            if (previous != null && (id <= previous.tradeId() || observedAt.isBefore(previous.observedAt()))) return;
            tradeWatermarks.put(symbol, value); quotes.put(symbol, value);
            Instant lastPublished = publishedPriceAt.get(symbol);
            publish = lastPublished == null || !receivedAt.isBefore(lastPublished.plusSeconds(1));
            if (publish) { publishedPriceAt.put(symbol, receivedAt); pendingPrices.remove(symbol); }
            else { pendingPrices.put(symbol, value); coalescedPrices.incrementAndGet(); }
        }
        if (publish) notifyListeners(new MarketUpdate(symbol, "PRICE", observedAt));
        Runnable observation = () -> {
            for (Consumer<SpotQuote> listener : tradeListeners) try { listener.accept(value); }
            catch (RuntimeException failure) {
                tradeDrops.incrementAndGet(); notifyListeners(new MarketUpdate(symbol, "OBSERVATION_GAP", observedAt));
            }
        };
        if (asyncObservation) submit(tradeFrames, new Frame(symbol, "", data, receivedAt, "TRADE"), observation);
        else observation.run();
    }

    @Scheduled(fixedDelay = 250L, initialDelay = 1000L)
    public void flushPrices() {
        if (stopped) return;
        Instant now = Instant.now();
        List<SpotQuote> due = new ArrayList<>();
        synchronized (priceLock) {
            pendingPrices.forEach((symbol, value) -> {
                if (!now.isBefore(publishedPriceAt.getOrDefault(symbol, Instant.MIN).plusSeconds(1))) {
                    pendingPrices.remove(symbol, value); publishedPriceAt.put(symbol, now);
                    if (desiredSymbols.contains(symbol)) due.add(value);
                }
            });
        }
        due.forEach(value -> notifyListeners(new MarketUpdate(value.symbol(), "PRICE", value.observedAt())));
    }

    private synchronized void acceptDiffDepth(String symbol, JsonNode data, Instant receivedAt) {
        if (!desiredSymbols.contains(symbol)) return;
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
            depthDrops.incrementAndGet();
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
        if (!value.coversBasisPoints(25)) {
            invalidateDepth(symbol);
            return;
        }
        ArrayDeque<SpotBook> history = bookHistory.computeIfAbsent(symbol, ignored -> new ArrayDeque<>());
        history.addLast(value);
        while (history.size() > BOOK_HISTORY_LIMIT) history.removeFirst();
        books.put(symbol, value); state.lastPublishedAt = receivedAt;
        notifyListeners(new MarketUpdate(symbol, "BOOK", state.observedAt));
    }

    private void invalidateDepth(String symbol) {
        depthStates.remove(symbol); bookHistory.remove(symbol); books.remove(symbol);
        depthRecoveries.incrementAndGet();
        notifyListeners(new MarketUpdate(symbol, "RISK_FAILURE", Instant.now()));
        notifyListeners(new MarketUpdate(symbol, "RECOVERY", Instant.now()));
    }

    /** Called only by the opt-in background connection lifecycle, never by quote/book/bars or a page request. */
    private void bootstrapPendingBook(Instant now) {
        if (!networkAllowed() || socket == null) return;
        for (String symbol : depthBootstrapOrder()) {
            DepthState state = depthStates.get(symbol);
            if (state == null || state.sequence >= 0 || state.bootstrapInFlight || state.pending.isEmpty()
                    || !claimDepthBootstrapBudget(symbol, now)) continue;
            if (!collectionLease.reserve("REST", now)) return;
            state.bootstrapInFlight = true;
            WebSocket expectedSocket = socket;
            long expectedEpoch = connectionEpoch;
            URI uri = URI.create(properties.getSpotDepthSnapshotUri() + "?symbol=" + symbol + "&limit=" + MAX_DEPTH_LEVELS);
            HttpRequest request = HttpRequest.newBuilder(uri).GET().timeout(Duration.ofSeconds(15)).build();
            CompletableFuture<HttpResponse<String>> pending = http.sendAsync(request, ignored -> new LimitedDepthBodySubscriber());
            depthRequests.add(pending);
            pending.whenComplete((response, failure) -> {
                depthRequests.remove(pending);
                completeDepthBootstrap(symbol, expectedSocket, expectedEpoch, state, response, failure, Instant.now());
            });
            // Reservations bound all concurrent requests by actual REST weight, including replacement connections.
        }
    }

    private synchronized void completeDepthBootstrap(String symbol, WebSocket expectedSocket, long expectedEpoch,
                                                      DepthState expectedState, HttpResponse<String> response,
                                                      Throwable failure, Instant receivedAt) {
        // A rate limit applies to the shared public endpoint even if its connection has since been replaced.
        if (response != null && (response.statusCode() == 429 || response.statusCode() == 418))
            recordDepthBootstrapFailure(symbol, response.statusCode(), response.headers().firstValue("Retry-After").orElse(null), receivedAt);
        if (response != null && (response.statusCode() == 429 || response.statusCode() == 418
                || response.statusCode() == 401 || response.statusCode() == 403)) {
            stopCollection("PROVIDER_QUOTA_OR_AUTH_FAILURE", receivedAt); return;
        }
        if (response != null) {
            try {
                int used = Integer.parseInt(response.headers().firstValue("X-MBX-USED-WEIGHT-1M").orElseThrow());
                if (used < 0 || used >= properties.getCollectionWindow().getSharedIpWeightLimitPerMinute() * 0.8) {
                    stopCollection("SHARED_IP_HEADROOM_EXHAUSTED", receivedAt); return;
                }
            } catch (RuntimeException missing) { stopCollection("SHARED_IP_QUOTA_UNVERIFIABLE", receivedAt); return; }
        }
        if (response != null) response.headers().firstValue("X-MBX-USED-WEIGHT-1M").ifPresent(value -> {
            try { observeIpWeight(Integer.parseInt(value), receivedAt); }
            catch (NumberFormatException ignored) { /* A malformed header cannot enlarge the local budget. */ }
        });
        // Stale responses must not insert data into a replacement connection or subscription.
        if (!networkAllowed() || !collectionAccepting(receivedAt) || socket != expectedSocket || connectionEpoch != expectedEpoch
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
        if (now != null) resetWeightWindow(now);
        if (normalized.isEmpty() || now == null || now.isBefore(nextDepthBootstrapAt)
                || now.isBefore(depthRetryAfter.getOrDefault(normalized, Instant.MIN))
                || reservedWeight + DEPTH_REQUEST_WEIGHT > properties.getDepthWeightBudgetPerMinute()) return false;
        reservedWeight += DEPTH_REQUEST_WEIGHT;
        depthRetryAfter.put(normalized, now.plusSeconds(2));
        return true;
    }

    private void resetWeightWindow(Instant now) {
        Instant minute = Instant.ofEpochSecond(Math.floorDiv(now.getEpochSecond(), 60) * 60);
        if (minute.isAfter(weightWindow)) { weightWindow = minute; reservedWeight = 0; }
    }

    synchronized void observeIpWeight(int used, Instant at) {
        if (used < 0 || at == null) return;
        resetWeightWindow(at);
        reservedWeight = Math.max(reservedWeight, used);
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

    public RuntimeMetrics runtimeMetrics() {
        return new RuntimeMetrics(queueDepth(tradeFrames), queueDepth(depthFrames), queueDepth(barFrames),
                tradeDrops.get(), depthDrops.get(), barDrops.get(), coalescedPrices.get(), depthRecoveries.get(),
                reconnects.get(), maxProcessingLagMillis.get(), reconnectAfter);
    }

    private static int queueDepth(ThreadPoolExecutor[] group) {
        int total = 0; for (ThreadPoolExecutor executor : group) total += executor.getQueue().size(); return total;
    }

    boolean awaitQueues(Duration timeout) throws Exception {
        List<CompletableFuture<Void>> barriers = new ArrayList<>();
        for (ThreadPoolExecutor[] group : List.of(tradeFrames, depthFrames, barFrames)) for (ThreadPoolExecutor executor : group) {
            CompletableFuture<Void> barrier = new CompletableFuture<>(); barriers.add(barrier);
            Runnable task = () -> barrier.complete(null);
            try { executor.execute(task); }
            catch (RejectedExecutionException busy) {
                if (executor.isShutdown() || !executor.getQueue().offer(task, timeout.toMillis(), TimeUnit.MILLISECONDS)) return false;
            }
        }
        CompletableFuture.allOf(barriers.toArray(CompletableFuture[]::new)).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        return true;
    }

    public record RuntimeMetrics(int tradeQueueDepth, int depthQueueDepth, int barQueueDepth,
                                 long tradeDrops, long depthDrops, long barDrops, long coalescedPrices,
                                 long depthRecoveries, long reconnects, long maxProcessingLagMillis, Instant reconnectAfter) {}

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

    /** Durable card-only allowance. Absolute clocks, counters and stop state survive a process restart. */
    static final class CollectionLease {
        private static final String PROCESS_ID = java.util.UUID.randomUUID().toString();
        private final AssetCardProperties.CollectionWindow plan;
        private final ObjectMapper json;
        private final Supplier<AssetCardMapper.StorageUsage> usage;
        private final LongSupplier freeBytes;
        private final String processId;
        private volatile String pendingStop;
        private volatile String status = "NOT_STARTED";
        private volatile Instant checkedAt;
        private volatile boolean permitted;
        CollectionLease(AssetCardProperties.CollectionWindow plan, ObjectMapper json,
                        Supplier<AssetCardMapper.StorageUsage> usage) {
            this(plan, json, usage, () -> {
                try { return Files.getFileStore(plan.getStateDirectory()).getUsableSpace(); }
                catch (java.io.IOException failure) { throw new IllegalStateException("STORAGE_UNAVAILABLE"); }
            });
        }
        CollectionLease(AssetCardProperties.CollectionWindow plan, ObjectMapper json,
                        Supplier<AssetCardMapper.StorageUsage> usage, LongSupplier freeBytes) {
            this(plan, json, usage, freeBytes, PROCESS_ID);
        }
        CollectionLease(AssetCardProperties.CollectionWindow plan, ObjectMapper json,
                        Supplier<AssetCardMapper.StorageUsage> usage, LongSupplier freeBytes, String processId) {
            this.plan = plan;
            this.json = json.copy().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            this.usage = usage; this.freeBytes = freeBytes;
            this.processId = processId;
        }
        String status() { return status; }
        boolean accepting(Instant now) {
            return pendingStop == null && permitted && plan.valid() && now != null && !now.isBefore(plan.getStartsAt())
                    && now.isBefore(plan.getEndsAt()) && checkedAt != null
                    && !now.isBefore(checkedAt.minusSeconds(1)) && now.isBefore(checkedAt.plusSeconds(16));
        }
        synchronized boolean check(Instant now) {
            if (pendingStop != null) return transact("STOP", now, pendingStop);
            if (accepting(now) && now.isBefore(checkedAt.plusSeconds(15))) return true;
            return transact("CHECK", now, null);
        }
        synchronized boolean reserve(String kind, Instant now) {
            if (pendingStop != null) return transact("STOP", now, pendingStop);
            if (!Set.of("REST", "CONNECTION", "CONTROL").contains(kind)) return false;
            return transact(kind, now, null);
        }
        synchronized void stop(String reason, Instant now) {
            permitted = false;
            if (pendingStop == null) pendingStop = reason != null && reason.matches("[A-Z_]{3,80}") ? reason : "COLLECTION_STOPPED";
            transact("STOP", now, pendingStop);
        }
        synchronized void release(Instant now) {
            if (pendingStop != null) { transact("STOP", now, pendingStop); return; }
            if (plan.valid() && Files.exists(plan.getStateDirectory().resolve(plan.getId()+".json"),LinkOption.NOFOLLOW_LINKS))
                transact("RELEASE", now, null);
            permitted=false;
        }
        private boolean transact(String action, Instant now, String reason) {
            if (now == null || !plan.valid()) { permitted = false; status = "WINDOW_NOT_CONFIGURED"; return false; }
            if (now.isBefore(plan.getStartsAt()) && !"STOP".equals(action)) { permitted = false; status = "WAITING_FIXED_START"; return false; }
            try {
                Path root = plan.getStateDirectory().toAbsolutePath().normalize();
                requireDirectory(root);
                String identity = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                        json.writeValueAsBytes(new TreeMap<>(Map.ofEntries(
                                Map.entry("id", plan.getId()), Map.entry("start", plan.getStartsAt().toString()),
                                Map.entry("end", plan.getEndsAt().toString()), Map.entry("symbols", new TreeSet<>(plan.getSymbols())),
                                Map.entry("restRequests", plan.getMaximumRestRequests()), Map.entry("restWeight", plan.getMaximumRestWeight()),
                                Map.entry("connections", plan.getMaximumConnectionAttempts()), Map.entry("controls", plan.getMaximumControlMessages()),
                                Map.entry("rows", plan.getMaximumNewRows()), Map.entry("dbBytes", plan.getMaximumDatabaseGrowthBytes()),
                                Map.entry("walBytes", plan.getMaximumWalGrowthBytes()), Map.entry("freeBytes", plan.getMinimumFreeBytes()),
                                Map.entry("minuteWeight", plan.getSharedIpWeightAllowancePerMinute()),
                                Map.entry("ipLimit", plan.getSharedIpWeightLimitPerMinute()),
                                Map.entry("confirmedAt", plan.getSharedIpHeadroomConfirmedAt().toString()))))));
                Path file = root.resolve(plan.getId() + ".json"), lock = root.resolve(plan.getId() + ".lock");
                if (!Files.exists(lock, LinkOption.NOFOLLOW_LINKS)) {
                    try { Files.createFile(lock, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))); }
                    catch (java.nio.file.FileAlreadyExistsException concurrent) { /* Validate before opening. */ }
                }
                requireFile(lock);
                try (FileChannel channel = FileChannel.open(lock, StandardOpenOption.READ, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                     var held = channel.lock()) {
                  try {
                    Map<String, Object> state;
                    if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                        requireFile(file);
                        if (Files.size(file) > 4096) throw new IllegalStateException("LEDGER_INVALID");
                        state = json.readValue(Files.readAllBytes(file), new com.fasterxml.jackson.core.type.TypeReference<TreeMap<String,Object>>() {});
                        if (!identity.equals(state.get("identity")) || !Set.of("OPEN", "STOPPED").contains(state.get("state")))
                            throw new IllegalStateException("LEDGER_IDENTITY_MISMATCH");
                        for (String key : List.of("rest", "weight", "connections", "controls", "baseRows", "baseDb", "baseWal", "lastRows", "minute", "minuteWeight", "lastAt"))
                            number(state, key);
                    } else {
                        if (channel.size() != 0) throw new IllegalStateException("LEDGER_MISSING_AFTER_INITIALIZATION");
                        AssetCardMapper.StorageUsage initial = new AssetCardMapper.StorageUsage(0,0,0,0,0,true);
                        if (!"STOP".equals(action)) try {
                            initial = java.util.Objects.requireNonNull(usage.get());
                            if (!initial.postgres()) reason = "POSTGRES_STORAGE_EVIDENCE_REQUIRED";
                        } catch (RuntimeException unavailable) { reason = "STORAGE_MEASUREMENT_FAILED"; }
                        state = new TreeMap<>();
                        state.put("identity", identity); state.put("state", "OPEN"); state.put("reason", "RUNNING");
                        state.put("process",processId); state.put("cleanShutdown",false);
                        state.put("baseRows", initial.totalRows()); state.put("baseDb", initial.databaseBytes());
                        state.put("lastRows", initial.totalRows());
                        state.put("baseWal", initial.walBytes());
                        for (String key : List.of("rest", "weight", "connections", "controls", "minute", "minuteWeight", "lastAt")) state.put(key, 0L);
                        // Losing an initialized ledger must never establish a fresh baseline on restart.
                        channel.write(ByteBuffer.wrap(new byte[]{1})); channel.force(true);
                        forceDirectory(root);
                    }
                    if ("STOPPED".equals(state.get("state"))) { permitted = false; status = (String)state.get("reason"); return false; }
                    String stop = reason;
                    ByteBuffer marker = ByteBuffer.allocate(1); channel.read(marker,0);
                    if (marker.array()[0] == 2) stop = "COLLECTION_STOPPED_DURABILITY_GUARD";
                    if (!processId.equals(state.get("process")) && !Boolean.TRUE.equals(state.get("cleanShutdown")))
                        stop = "UNVERIFIED_PREVIOUS_COLLECTION_PROCESS";
                    state.put("process",processId); state.put("cleanShutdown",false);
                    if (!now.isBefore(plan.getEndsAt())) stop = "WINDOW_EXPIRED";
                    if (now.toEpochMilli() + 1000 < number(state, "lastAt")) stop = "CLOCK_MOVED_BACKWARDS";
                    if (stop == null) try {
                        var current = java.util.Objects.requireNonNull(usage.get());
                        if (!current.postgres()) stop = "POSTGRES_STORAGE_EVIDENCE_REQUIRED";
                        else if (freeBytes.getAsLong() < plan.getMinimumFreeBytes()) stop = "STORAGE_FREE_SPACE_LIMIT";
                        else if (current.totalRows() < number(state,"lastRows")) stop = "UNEXPECTED_CARD_ROWS_REMOVED";
                        else if (current.totalRows() - number(state, "baseRows") >= plan.getMaximumNewRows()) stop = "DATABASE_ROW_LIMIT";
                        else if (current.databaseBytes() - number(state, "baseDb") >= plan.getMaximumDatabaseGrowthBytes()) stop = "DATABASE_GROWTH_LIMIT";
                        else if (current.walBytes() - number(state, "baseWal") >= plan.getMaximumWalGrowthBytes()) stop = "WAL_GROWTH_LIMIT";
                        else if (current.walBytes() < number(state, "baseWal")) stop = "WAL_IDENTITY_CHANGED";
                        state.put("lastRows", Math.max(number(state,"lastRows"),current.totalRows()));
                    } catch (RuntimeException unavailable) { stop = "STORAGE_MEASUREMENT_FAILED"; }
                    if (stop == null && "REST".equals(action)) {
                        long minute = Math.floorDiv(now.getEpochSecond(), 60);
                        if (minute != number(state, "minute")) { state.put("minute", minute); state.put("minuteWeight", 0L); }
                        if (number(state,"rest") >= plan.getMaximumRestRequests()
                                || number(state,"weight") + DEPTH_REQUEST_WEIGHT > plan.getMaximumRestWeight()) stop = "REST_TOTAL_BUDGET_EXHAUSTED";
                        else if (number(state,"minuteWeight") + DEPTH_REQUEST_WEIGHT > plan.getSharedIpWeightAllowancePerMinute()) {
                            permitted = true; checkedAt = now; status = "REST_MINUTE_BUDGET_WAIT"; return false;
                        } else { increment(state,"rest",1); increment(state,"weight",DEPTH_REQUEST_WEIGHT); increment(state,"minuteWeight",DEPTH_REQUEST_WEIGHT); }
                    } else if (stop == null && "CONNECTION".equals(action)) {
                        if (number(state,"connections") >= plan.getMaximumConnectionAttempts()) stop = "WS_CONNECTION_BUDGET_EXHAUSTED";
                        else increment(state,"connections",1);
                    } else if (stop == null && "CONTROL".equals(action)) {
                        if (number(state,"controls") >= plan.getMaximumControlMessages()) stop = "WS_CONTROL_BUDGET_EXHAUSTED";
                        else increment(state,"controls",1);
                    }
                    if (stop != null) { markStopped(channel); pendingStop=stop; state.put("state", "STOPPED"); state.put("reason", stop); }
                    if (stop == null && "RELEASE".equals(action)) state.put("cleanShutdown",true);
                    state.put("lastAt", Math.max(number(state,"lastAt"),now.toEpochMilli()));
                    writeState(root, file, state);
                    permitted = stop == null && !"RELEASE".equals(action); checkedAt = now; status = stop == null ? "RUNNING" : stop;
                    return permitted;
                  } catch (Exception failure) {
                    // Overwrite the already allocated marker before any later recovery can read stale OPEN.
                    try { markStopped(channel); } catch (Exception unavailable) { /* In-memory stop plus unclean-process refusal remains. */ }
                    throw failure;
                  }
                }
            } catch (Exception failure) {
                permitted = false; status = "COLLECTION_LEDGER_OR_STORAGE_UNAVAILABLE";
                if (pendingStop == null) pendingStop=status;
                // No fallback allowance, fresh time window, raw exception or unverified data deletion.
                return false;
            }
        }
        private static void markStopped(FileChannel channel) throws java.io.IOException {
            channel.write(ByteBuffer.wrap(new byte[]{2}),0); channel.force(true);
        }
        private static long number(Map<String,Object> state, String key) {
            Object value = state.get(key);
            if (!(value instanceof Number n) || n.longValue() < 0 || n.doubleValue() != n.longValue())
                throw new IllegalArgumentException("LEDGER_INVALID");
            return n.longValue();
        }
        private static void increment(Map<String,Object> state, String key, long amount) { state.put(key,Math.addExact(number(state,key),amount)); }
        private void writeState(Path root, Path target, Map<String,Object> state) throws java.io.IOException {
            Path temporary = Files.createTempFile(root, ".window-", ".tmp", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            try {
                try (FileChannel output = FileChannel.open(temporary, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                    ByteBuffer bytes = ByteBuffer.wrap(json.writeValueAsBytes(state));
                    while (bytes.hasRemaining()) output.write(bytes);
                    output.force(true);
                }
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                forceDirectory(root);
            } finally { Files.deleteIfExists(temporary); }
        }
        private static void forceDirectory(Path root) throws java.io.IOException {
            try (FileChannel directory = FileChannel.open(root, StandardOpenOption.READ)) { directory.force(true); }
        }
        private static void requireDirectory(Path path) throws java.io.IOException {
            if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) || !path.equals(path.toRealPath())
                    || !Files.getPosixFilePermissions(path).equals(PosixFilePermissions.fromString("rwx------"))
                    || !Files.getOwner(path).getName().equals(ProcessHandle.current().info().user().orElse("")))
                throw new IllegalArgumentException("Protected service-owned collection directory required");
        }
        private static void requireFile(Path path) throws java.io.IOException {
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    || !Files.getPosixFilePermissions(path).equals(PosixFilePermissions.fromString("rw-------"))
                    || !Files.getOwner(path).equals(Files.getOwner(path.getParent())))
                throw new IllegalArgumentException("Protected collection state file required");
        }
    }

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
        SpotBar value = new SpotBar(symbol, interval, openTime, Instant.ofEpochMilli(closeMillis), open, high, low, close,
                volume, takerVolume, tradeCount, receivedAt);
        synchronized (cache) {
            if (cache.containsKey(openTime)) return;
            try {
                if (properties.isWriterEnabled() && writerReadiness.getAsBoolean()) mapper.upsertClosedBar(value);
                else notifyListeners(new MarketUpdate(symbol, "PERSISTENCE_FAILURE", value.closeTime(), interval));
            }
            catch (RuntimeException failure) {
                notifyListeners(new MarketUpdate(symbol, "PERSISTENCE_FAILURE", value.closeTime(), interval));
            }
            cache.put(openTime, value);
            while (cache.size() > properties.getRetainedBarsPerInterval()) cache.pollFirstEntry();
        }
        // DB idempotency is independent of this process's inference trigger; a pre-existing row must not suppress it.
        if (desiredSymbols.contains(symbol)) notifyListeners(new MarketUpdate(symbol, "BAR", value.closeTime(), interval));
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
        controlInFlight = false;
        for (CompletableFuture<?> pending : depthRequests) pending.cancel(true);
        depthRequests.clear();
        if (previous != null) { previous.abort(); clearLiveData(desiredSymbols); }
    }
    @PreDestroy public void close() {
        stopped = true; disconnect();
        collectionLease.release(Instant.now());
        for (ThreadPoolExecutor[] group : List.of(tradeFrames, depthFrames, barFrames))
            for (ThreadPoolExecutor executor : group) executor.shutdownNow();
    }

    public record SpotQuote(String symbol, BigDecimal price, BigDecimal quantity, long tradeId, Instant observedAt, Instant availableAt) {
        public String source() { return "BINANCE_SPOT_AGG_TRADE"; }
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
