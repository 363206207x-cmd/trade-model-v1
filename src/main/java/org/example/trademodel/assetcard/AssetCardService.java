package org.example.trademodel.assetcard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.example.trademodel.mapper.AssetCardMapper;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.v41.DashboardLiveEvent;
import org.example.trademodel.v41.DashboardLiveEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/** Background card owner. All request-facing methods below are strictly read-only. */
@Service
public class AssetCardService {
    private static final Logger log = LoggerFactory.getLogger(AssetCardService.class);
    private final AssetCardProperties properties;
    private final AssetCardMarketDataService market;
    private final AssetCardMapper mapper;
    private final AssetPoolService pool;
    private final DashboardLiveEventService events;
    private final ObjectMapper json;
    private AssetCardEvidenceService evidence;
    private final AssetCardFeatureService featureBuilder = new AssetCardFeatureService();
    private final AssetCardSignalService signals = new AssetCardSignalService();
    private final AssetCardRiskService risks = new AssetCardRiskService();
    private volatile AssetCardModelBundle model = AssetCardModelBundle.unavailable("MODEL_NOT_CONFIGURED");
    private final Map<String, AssetCardFeatureService.Frame> featureFrames = new ConcurrentHashMap<>();
    private final Map<String, AssetCardFeatureService.Frame> signalFrames = new ConcurrentHashMap<>();
    private final Map<String, AssetCardSignalService.State> signalStates = new ConcurrentHashMap<>();
    private final Map<String, AssetCardEvidenceService.EvidenceFrame> providerFacts = new ConcurrentHashMap<>();
    private final Map<String, AssetCardFeatureService.Observation> minuteVolatility = new ConcurrentHashMap<>();
    private final Map<String, AssetCardFeatureService.Observation> minuteReturns = new ConcurrentHashMap<>();
    private record BarIdentity(String symbol, Instant closedAt) {}
    private final Set<BarIdentity> queuedBars = ConcurrentHashMap.newKeySet();
    private final Map<String, Object> symbolLocks = new ConcurrentHashMap<>();
    private final Map<String, Object> inferenceLocks = new ConcurrentHashMap<>();
    private final Set<String> pendingRiskRefreshes = ConcurrentHashMap.newKeySet();
    private final Set<String> recovered = ConcurrentHashMap.newKeySet();
    private final Set<String> previouslyHealthy = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService inference = Executors.newScheduledThreadPool(
            Math.max(4, Math.min(16, Runtime.getRuntime().availableProcessors())), task -> {
        Thread thread = new Thread(task, "asset-card-inference"); thread.setDaemon(true); return thread;
    });
    private final Map<String, AssetCardSnapshot> snapshots = new ConcurrentHashMap<>();
    private volatile boolean writerReady;
    private final Map<String, AssetCardMarketDataService.SpotQuote> pendingTrades = new ConcurrentHashMap<>();
    private enum Field { PRICE, SIGNAL, RISK, PERSISTENCE, RECOVERY }
    private final Map<Field, Map<String, String>> fieldFailures = new EnumMap<>(Field.class);
    private final ScheduledExecutorService background = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "asset-card-projection"); thread.setDaemon(true); return thread;
    });
    private final ScheduledExecutorService riskWorkers = Executors.newScheduledThreadPool(4, task -> {
        Thread thread = new Thread(task, "asset-card-risk"); thread.setDaemon(true); return thread;
    });
    private boolean started;

    public AssetCardService(AssetCardProperties properties, AssetCardMarketDataService market, AssetCardMapper mapper,
                            AssetPoolService pool, DashboardLiveEventService events, ObjectMapper json) {
        this.properties = properties; this.market = market; this.mapper = mapper;
        this.pool = pool; this.events = events;
        // Card JSON trees must preserve fractional epoch seconds without changing the shared application mapper.
        this.json = json.copy().enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        for (Field field : Field.values()) fieldFailures.put(field, new ConcurrentHashMap<>());
    }

    private Object symbolLock(String symbol) { return symbolLocks.computeIfAbsent(symbol, key -> new Object()); }
    private void failed(Field field, String symbol, String reason) { fieldFailures.get(field).put(symbol, reason); }
    private void healthy(Field field, String symbol) { fieldFailures.get(field).remove(symbol); }
    private String failure(Field field, String symbol) { return fieldFailures.get(field).get(symbol); }
    private String riskVersion() { return model.riskVersion() == null ? AssetCardRiskService.RULE_VERSION : model.riskVersion(); }

    @org.springframework.beans.factory.annotation.Autowired
    public void setEvidenceService(AssetCardEvidenceService evidence) { this.evidence = evidence; }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if (started || !properties.isEnabled()) return;
        started = true;
        if (properties.getModelBundlePath() != null && !properties.getModelBundlePath().isBlank())
            model = AssetCardModelBundle.load(Path.of(properties.getModelBundlePath()), properties.getModelBundleSha256());
        refreshWriterReadiness();
        market.setWriterReadiness(() -> writerReady);
        market.addListener(this::onMarketUpdate);
        market.addTradeListener(this::onTradeObserved);
        background.scheduleWithFixedDelay(this::reconcileSubscriptionsSafely, 0, 60, TimeUnit.SECONDS);
        background.scheduleWithFixedDelay(this::flushPrices, 500, 500, TimeUnit.MILLISECONDS);
        riskWorkers.scheduleWithFixedDelay(this::enqueueRiskRefreshes, 1, 1, TimeUnit.SECONDS);
        inference.scheduleWithFixedDelay(this::reconcileEvidenceSafely, 0, 60, TimeUnit.SECONDS);
        riskWorkers.scheduleWithFixedDelay(this::flushTradeObservations, 1, 1, TimeUnit.SECONDS);
        riskWorkers.scheduleWithFixedDelay(this::refreshWriterReadiness, 60, 60, TimeUnit.SECONDS);
    }

    private void refreshWriterReadiness() {
        boolean configured = properties.isWriterEnabled() && !properties.getBarRetention().isZero()
                && !properties.getFeatureRetention().isZero() && !properties.getTradeRetention().isZero()
                && !properties.getLabelRetention().isZero();
        var permission = configured ? mapper.inspectWriterPermissions() : null;
        writerReady = configured && permission != null && permission.writable();
        if (!writerReady) for (String symbol : market.subscribedSymbols())
            failed(Field.PERSISTENCE, symbol, "卡片专用写入权限或保留策略未就绪");
    }

    /** One-second sampled actual aggregate trade, never an interpolated or substituted price. */
    private void onTradeObserved(AssetCardMarketDataService.SpotQuote quote) {
        if (!started || !properties.isEnabled() || !writerReady) return;
        pendingTrades.compute(quote.symbol(), (symbol, previous) -> previous == null || quote.tradeId() > previous.tradeId() ? quote : previous);
    }

    private void flushTradeObservations() {
        if (!writerReady) return;
        pendingTrades.forEach((symbol, quote) -> {
            if (!pendingTrades.remove(symbol, quote)) return;
            try {
                var observation = tradeObservation(quote);
                mapper.saveTradeObservation(quote, observation.instrument(), observation.sourceVersion(), json.writeValueAsString(
                        payload("observation", observation, "quantity", quote.quantity(), "sampling", "LATEST_ACTUAL_AGG_TRADE_PER_ONE_SECOND_FLUSH",
                                "dataKind", "LIVE_OBSERVED_CARD_TRADE")));
            } catch (RuntimeException | JsonProcessingException failure) {
                failed(Field.PERSISTENCE, symbol, "真实成交观察保存失败，标签不得补造");
            }
        });
    }

    private AssetCardFeatureService.Observation tradeObservation(AssetCardMarketDataService.SpotQuote quote) {
        return new AssetCardFeatureService.Observation(quote.price().doubleValue(), quote.source(), quote.observedAt(), quote.availableAt(),
                AssetCardFeatureService.spotInstrument(quote.symbol()), AssetCardFeatureService.SPOT_SOURCE_VERSION, "QUOTE_CURRENCY",
                quote.observedAt().plus(properties.getPriceTtl()), String.valueOf(quote.tradeId()));
    }

    /** Source is the existing pool union, never a page visit, click, or a new subscription-writing GET. */
    public void reconcileSubscriptions() {
        if (!properties.isEnabled()) return;
        market.reconcileSubscriptions(pool.listScanSymbols());
        for (String symbol : market.subscribedSymbols()) recoverRuntimeState(symbol, Instant.now());
    }

    /** Only a closed market bar can enqueue inference. No page request reaches this method. */
    void onMarketUpdate(AssetCardMarketDataService.MarketUpdate update) {
        if (!started || !properties.isEnabled()) return;
        switch (update.type()) {
            case "PRICE_FAILURE" -> failed(Field.PRICE, update.symbol(), "真实现货成交连接中断");
            case "RISK_FAILURE" -> failed(Field.RISK, update.symbol(), "盘口证据重建中");
            case "PERSISTENCE_FAILURE" -> failed(Field.PERSISTENCE, update.symbol(), "卡片存储写入未就绪");
            case "RECOVERY" -> failed(Field.RECOVERY, update.symbol(), "行情恢复中");
            case "OBSERVATION_GAP" -> failed(Field.RECOVERY, update.symbol(), "成交观察存在队列缺口");
            case "BAR_DROPPED" -> {
                failed(Field.SIGNAL, update.symbol(), "闭线队列容量不足，本轮未完成");
                if (writerReady && "5m".equals(update.interval()) && update.observedAt() != null)
                    inference.execute(() -> recordClosedFailure(update.symbol(), update.observedAt(), Instant.now(), "DROPPED"));
            }
            default -> { }
        }
        if (!"BAR".equals(update.type())) return;
        if ("1m".equals(update.interval())) {
            inference.execute(() -> refreshMinuteVolatility(update.symbol(), Instant.now()));
            return;
        }
        if (!"5m".equals(update.interval()) || update.observedAt() == null) return;
        var identity = new BarIdentity(update.symbol(), update.observedAt());
        if (queuedBars.add(identity)) inference.execute(() -> inferClosedBar(identity.symbol(), identity.closedAt(), Instant.now()));
    }

    private void reconcileEvidenceSafely() {
        if (!properties.isEnabled() || evidence == null) return;
        for (String symbol : market.subscribedSymbols()) {
            try { providerFacts.put(symbol, evidence.read(symbol, Instant.now())); }
            catch (RuntimeException failure) { providerFacts.remove(symbol); }
        }
    }

    void inferClosedBar(String symbol, Instant closedAt, Instant at) {
        long enqueued = System.nanoTime();
        synchronized (inferenceLocks.computeIfAbsent(symbol, key -> new Object())) {
            inferClosedBarLocked(symbol, closedAt, at.plusNanos(Math.max(0, System.nanoTime() - enqueued)));
        }
    }

    private void inferClosedBarLocked(String symbol, Instant closedAt, Instant at) {
        if (!properties.isEnabled() || closedAt == null || closedAt.isAfter(at)) return;
        if (started && !writerReady) { failed(Field.PERSISTENCE, symbol, "闭线持久化写入权限未就绪"); return; }
        long startedNanos = System.nanoTime();
        var identity = new BarIdentity(symbol, closedAt);
        boolean retry = false;
        try {
            recoverRuntimeState(symbol, at);
            var completed = signalStates.get(symbol);
            if (completed != null && completed.lastClosed5mAt() != null && !closedAt.isAfter(completed.lastClosed5mAt())) {
                var current = loadSnapshot(symbol, symbol);
                publishSignalAndRisk(current, completed.signal(), current.risk(), AssetCardFeatureService.FEATURE_VERSION,
                        model.modelVersion(), model.calibrationVersion(), at);
                return;
            }
            // Exact persisted closed-bar identity, independent from insertion and processing timestamps.
            var prior = mapper.selectInference(symbol, closedAt, at);
            if (prior != null && prior.isPresent()) {
                restoreAudit(symbol, prior.get().payloadJson(), at);
                return;
            }
            Instant deadline = closedAt.plusSeconds(15);
            if (at.isAfter(deadline)) { recordClosedFailure(symbol, closedAt, at, "TIMED_OUT"); return; }
            Map<String, List<AssetCardFeatureService.Bar>> bars = new LinkedHashMap<>();
            for (String interval : AssetCardFeatureService.INTERVALS) bars.put(interval,
                    market.bars(symbol, interval, at, 24).stream().map(AssetCardService::featureBar).toList());
            boolean aligned = true;
            for (var entry : bars.entrySet()) if (!latestIntervalClosed(entry.getValue(), entry.getKey(), closedAt)) {
                aligned = false;
                // A previous 1h/4h window is not this boundary's complete input.
                entry.setValue(List.of());
            }
            if (!aligned && at.isBefore(deadline)) {
                if (started) {
                    retry = true;
                    inference.schedule(() -> inferClosedBar(symbol, closedAt, Instant.now()),
                            Math.max(1, Math.min(250, Duration.between(at, deadline).toMillis())), TimeUnit.MILLISECONDS);
                }
                return;
            }
            Map<String, AssetCardFeatureService.Observation> inputs = rawInputs(symbol, at);
            var raw = new AssetCardFeatureService.RawFrame(symbol, at, bars, inputs);
            var frame = featureBuilder.build(raw);
            // Missing/old 5m bars do not produce an inference for an unrelated close.
            if (frame.closed5mAt() == null || !frame.closed5mAt().equals(closedAt)) {
                recordClosedFailure(symbol, closedAt, at, "CLOSED_BAR_INPUT_MISSING"); return;
            }
            var prediction = model.predict(frame);
            Instant completedAt = at.plusNanos(Math.max(0, System.nanoTime() - startedNanos));
            if (completedAt.isAfter(deadline)) { recordClosedFailure(symbol, closedAt, completedAt, "TIMED_OUT"); return; }
            synchronized (symbolLock(symbol)) {
                var previous = signalStates.getOrDefault(symbol, AssetCardSignalService.State.initial(symbol));
                var state = signals.evaluate(previous, frame, model, prediction, properties.getModelMode(), properties.getCanarySymbols());
                var anchor = Objects.equals(state.signal().signalAsOf(), frame.signalAsOf()) ? frame : signalFrames.get(symbol);
                Map<String, Object> audit = payload("rawFrame", raw, "frame", frame, "signalFrame", anchor, "state", state,
                        "closed5mAt", closedAt, "completedAt", completedAt, "outcome", "COMPLETED",
                        "thresholdVersion", model.thresholdVersion(), "riskVersion", riskVersion(),
                        "modelMode", properties.getModelMode(), "dataKind", "LIVE_OBSERVED_CARD_INPUTS");
                if (mapper.saveInference(symbol, closedAt, completedAt, json.writeValueAsString(audit)) != 1) {
                    mapper.selectInference(symbol, closedAt, completedAt).ifPresent(existing -> {
                        try { restoreAudit(symbol, existing.payloadJson(), completedAt); }
                        catch (JsonProcessingException invalid) { throw new IllegalStateException("INVALID_CARD_AUDIT", invalid); }
                    });
                    return;
                }
                signalStates.put(symbol, state); featureFrames.put(symbol, frame);
                if (Objects.equals(state.signal().signalAsOf(), frame.signalAsOf())) signalFrames.put(symbol, frame);
                var current = loadSnapshot(symbol, symbol);
                publishSignalAndRisk(current, state.signal(), current.risk(), frame.featureVersion(),
                        model.modelVersion(), model.calibrationVersion(), completedAt);
                healthy(Field.SIGNAL, symbol);
            }
        } catch (RuntimeException | JsonProcessingException failure) {
            failed(Field.SIGNAL, symbol, "卡片分析暂时不可用");
            log.warn("[asset-card] Closed-bar analysis failed ({})", failure.getClass().getSimpleName());
            try { recordClosedFailure(symbol, closedAt, at, "FAILED"); }
            catch (RuntimeException writeFailure) { failed(Field.PERSISTENCE, symbol, "闭线失败状态无法持久化"); }
        } finally {
            if (!retry) queuedBars.remove(identity);
        }
    }

    private void recordClosedFailure(String symbol, Instant closedAt, Instant at, String outcome) {
        failed(Field.SIGNAL, symbol, "TIMED_OUT".equals(outcome) ? "本轮闭线计算超过15秒，等待下一闭线" : "本轮闭线数据或计算未完成");
        try {
            mapper.saveInference(symbol, closedAt, at, json.writeValueAsString(payload("closed5mAt", closedAt,
                    "completedAt", at, "outcome", outcome, "modelMode", properties.getModelMode(),
                    "dataKind", "LIVE_OBSERVED_CARD_OUTCOME")));
        } catch (JsonProcessingException invalid) { throw new IllegalStateException("CARD_OUTCOME_SERIALIZATION_FAILED", invalid); }
    }

    static boolean latestIntervalClosed(List<AssetCardFeatureService.Bar> bars, String interval, Instant closedAt) {
        if (bars == null || bars.size() < 24) return false;
        long period = switch (interval) { case "5m" -> 300_000; case "15m" -> 900_000;
            case "1h" -> 3_600_000; case "4h" -> 14_400_000; default -> throw new IllegalArgumentException("Unknown interval"); };
        long boundary = Math.floorDiv(closedAt.toEpochMilli() + 1, period) * period;
        var last = bars.get(bars.size()-1);
        return last.closeTime() != null && (last.closeTime().toEpochMilli() == boundary || last.closeTime().toEpochMilli() == boundary-1);
    }

    /** Background-only recovery. The persisted snapshot includes private, non-API state so a source-loss
     * invalidation cannot be undone by replaying an earlier inference audit after restart. */
    void recoverRuntimeState(String symbol, Instant at) {
        synchronized (symbolLock(symbol)) { recoverRuntimeStateLocked(symbol, at); }
    }

    private void recoverRuntimeStateLocked(String symbol, Instant at) {
        if (recovered.contains(symbol)) return;
        try {
            String stored = mapper.selectSnapshotJson(symbol);
            if (stored != null) {
                var runtime = json.readTree(stored).get("_runtime");
                if (runtime != null && !runtime.isNull()) restoreAudit(symbol, runtime.toString(), at);
            }
            var history = mapper.selectHistory(symbol, AssetCardMapper.HistoryKind.INFERENCE, at.minusSeconds(900), at, at, 32);
            if (history != null) for (var audit : history) restoreAudit(symbol, audit.payloadJson(), at);
            recovered.add(symbol);
            healthy(Field.RECOVERY, symbol);
        } catch (RuntimeException | JsonProcessingException failure) {
            failed(Field.RECOVERY, symbol, "卡片运行状态恢复暂时不可用");
            throw new IllegalStateException("ASSET_CARD_RUNTIME_RECOVERY_FAILED", failure);
        }
    }

    private void restoreAudit(String symbol, String stored, Instant at) throws JsonProcessingException {
        var node = json.readTree(stored);
        if (!"LIVE_OBSERVED_CARD_INPUTS".equals(node.path("dataKind").asText())) return;
        var frame = json.treeToValue(node.path("frame"), AssetCardFeatureService.Frame.class);
        var state = json.treeToValue(node.path("state"), AssetCardSignalService.State.class);
        if (frame == null || state == null || !symbol.equals(frame.symbol()) || !symbol.equals(state.symbol())
                || frame.signalAsOf() == null || frame.availableAt() == null || frame.closed5mAt() == null
                || frame.signalAsOf().isAfter(at) || frame.availableAt().isAfter(frame.signalAsOf())
                || !Objects.equals(frame.closed5mAt(), state.lastClosed5mAt())
                || !freshFrame(frame, at) || !AssetCardFeatureService.FEATURE_VERSION.equals(frame.featureVersion())) return;
        var identity = new AssetCardSignalService.ModelIdentity(frame.featureVersion(), model.modelVersion(),
                model.calibrationVersion(), model.thresholdVersion(), properties.getModelMode(),
                properties.getModelMode() == AssetCardProperties.ModelMode.ACTIVE
                        || properties.getModelMode() == AssetCardProperties.ModelMode.CANARY && properties.getCanarySymbols().contains(symbol), model.validated());
        if (!identity.equals(state.identity())) return;
        var previous = signalStates.get(symbol);
        if (previous != null && (previous.lastClosed5mAt() != null && !frame.closed5mAt().isAfter(previous.lastClosed5mAt())
                || previous.invalidatedAt() != null && !frame.signalAsOf().isAfter(previous.invalidatedAt()))) {
            // A retry after an audit commit but failed snapshot publication repairs the projection,
            // using the current state (including any newer invalidation), not the replayed audit.
            var current = loadSnapshot(symbol, symbol);
            publishSignalAndRisk(current, previous.signal(), current.risk(), frame.featureVersion(),
                    model.modelVersion(), model.calibrationVersion(), at);
            return;
        }
        AssetCardFeatureService.Frame anchor = node.path("signalFrame").isNull() || node.path("signalFrame").isMissingNode()
                ? null : json.treeToValue(node.path("signalFrame"), AssetCardFeatureService.Frame.class);
        if (state.signal().direction() != null && (anchor == null || !symbol.equals(anchor.symbol())
                || !Objects.equals(anchor.signalAsOf(), state.signal().signalAsOf())
                || !frame.featureVersion().equals(anchor.featureVersion()) || anchor.signalAsOf().isAfter(frame.signalAsOf()))) return;
        signalStates.put(symbol, state); featureFrames.put(symbol, frame);
        if (anchor != null) signalFrames.put(symbol, anchor);
        var current = loadSnapshot(symbol, symbol);
        publishSignalAndRisk(current, state.signal(), current.risk(), frame.featureVersion(), model.modelVersion(), model.calibrationVersion(), at);
    }

    private static boolean freshFrame(AssetCardFeatureService.Frame frame, Instant at) {
        return frame != null && frame.closed5mAt() != null && !frame.closed5mAt().isAfter(at)
                && Duration.between(frame.closed5mAt(), at).compareTo(Duration.ofSeconds(315)) <= 0;
    }

    private Map<String, AssetCardFeatureService.Observation> rawInputs(String symbol, Instant at) {
        Map<String, AssetCardFeatureService.Observation> inputs = new LinkedHashMap<>();
        var facts = providerFacts.get(symbol);
        if (facts != null) inputs.putAll(facts.observations(at));
        market.quote(symbol, at).ifPresent(q -> inputs.put("spotPrice", new AssetCardFeatureService.Observation(
                q.price().doubleValue(), q.source(), q.observedAt(), q.availableAt(),
                AssetCardFeatureService.spotInstrument(symbol), AssetCardFeatureService.SPOT_SOURCE_VERSION,
                "QUOTE_CURRENCY", q.observedAt().plus(properties.getPriceTtl()), String.valueOf(q.tradeId()))));
        market.book(symbol, at).ifPresent(book -> {
            double bid = book.bids().get(0).price().doubleValue(), ask = book.asks().get(0).price().doubleValue(), mid = (bid + ask) / 2;
            inputs.put("spreadBps", observation(symbol, "spreadBps", (ask - bid) / mid * 10_000, book));
            for (int bps : List.of(10, 25)) if (book.coversBasisPoints(bps)) {
                double low = mid * (1 - bps / 10_000.0), high = mid * (1 + bps / 10_000.0);
                double bids = book.bids().stream().filter(l -> l.price().doubleValue() >= low)
                        .mapToDouble(l -> l.price().doubleValue() * l.quantity().doubleValue()).sum();
                double asks = book.asks().stream().filter(l -> l.price().doubleValue() <= high)
                        .mapToDouble(l -> l.price().doubleValue() * l.quantity().doubleValue()).sum();
                inputs.put("depth" + bps + "Bps", observation(symbol, "depth" + bps + "Bps", bids + asks, book));
                if (bps == 25 && bids + asks > 0) inputs.put("bookImbalance", observation(symbol, "bookImbalance", (bids - asks) / (bids + asks), book));
            }
        });
        var minute = minuteVolatility.get(symbol);
        if (minute != null && !minute.availableAt().isAfter(at) && Duration.between(minute.observedAt(), at).getSeconds() <= 75)
            inputs.put("volatility1m", minute);
        var minuteReturn = minuteReturns.get(symbol);
        if (AssetCardFeatureService.usableObservation(symbol, "return1m", minuteReturn, at)) inputs.put("return1m", minuteReturn);
        return inputs;
    }

    private AssetCardFeatureService.Observation observation(String symbol, String key, double value, AssetCardMarketDataService.SpotBook book) {
        return new AssetCardFeatureService.Observation(value, book.source(), book.observedAt(), book.availableAt(),
                AssetCardFeatureService.spotInstrument(symbol), AssetCardFeatureService.SPOT_SOURCE_VERSION,
                AssetCardFeatureService.observationUnit(key), book.observedAt().plus(properties.getPriceTtl()));
    }
    private static AssetCardFeatureService.Bar featureBar(AssetCardMarketDataService.SpotBar bar) {
        return new AssetCardFeatureService.Bar(bar.openTime(), bar.closeTime(), bar.availableAt(), bar.open().doubleValue(),
                bar.high().doubleValue(), bar.low().doubleValue(), bar.close().doubleValue(), bar.volume().doubleValue(),
                bar.takerBuyBaseVolume() == null ? null : bar.takerBuyBaseVolume().doubleValue(), bar.tradeCount());
    }

    private void refreshMinuteVolatility(String symbol, Instant at) {
        if (!properties.isEnabled()) return;
        try {
            var bars = market.bars(symbol, "1m", at, 13);
            if (bars.size() != 13 || Duration.between(bars.get(12).closeTime(), at).getSeconds() > 75) return;
            double[] returns = new double[12]; double mean = 0;
            for (int i = 1; i < bars.size(); i++) {
                if (!bars.get(i).openTime().equals(bars.get(i - 1).openTime().plusSeconds(60))) return;
                returns[i-1] = Math.log(bars.get(i).close().doubleValue() / bars.get(i-1).close().doubleValue()); mean += returns[i-1] / 12;
            }
            double variance = 0; for (double value : returns) variance += (value - mean) * (value - mean) / 12;
            Instant observed = bars.get(12).closeTime();
            Instant available = bars.stream().map(AssetCardMarketDataService.SpotBar::availableAt).max(Comparator.naturalOrder()).orElseThrow();
            minuteVolatility.put(symbol, new AssetCardFeatureService.Observation(Math.sqrt(variance), "BINANCE_SPOT_CLOSED_1M",
                    observed, available, AssetCardFeatureService.spotInstrument(symbol), AssetCardFeatureService.SPOT_SOURCE_VERSION,
                    "LOG_RETURN_STD", observed.plusSeconds(75)));
            minuteReturns.put(symbol, new AssetCardFeatureService.Observation(returns[11], "BINANCE_SPOT_CLOSED_1M",
                    observed, available, AssetCardFeatureService.spotInstrument(symbol), AssetCardFeatureService.SPOT_SOURCE_VERSION,
                    "LOG_RETURN", observed.plusSeconds(75)));
        } catch (RuntimeException failure) { minuteVolatility.remove(symbol); minuteReturns.remove(symbol); }
    }

    private void refreshRisksSafely() {
        if (!properties.isEnabled()) return;
        for (String symbol : market.subscribedSymbols()) {
            try { refreshRisk(symbol, Instant.now()); }
            catch (RuntimeException failure) {
                failed(Field.RISK, symbol, "卡片风险证据暂时不可用");
                log.warn("[asset-card] Risk refresh failed ({})", failure.getClass().getSimpleName());
            }
        }
    }

    private void enqueueRiskRefreshes() {
        if (!properties.isEnabled()) return;
        for (String symbol : market.subscribedSymbols()) if (pendingRiskRefreshes.add(symbol)) {
            riskWorkers.execute(() -> {
                try { refreshRisk(symbol, Instant.now()); }
                catch (RuntimeException failure) { failed(Field.RISK, symbol, "卡片风险证据暂时不可用"); }
                finally { pendingRiskRefreshes.remove(symbol); }
            });
        }
    }

    void refreshRisk(String symbol, Instant at) {
        synchronized (symbolLock(symbol)) { refreshRiskLocked(symbol, at); }
    }

    private void refreshRiskLocked(String symbol, Instant at) {
        if (!properties.isEnabled() || started && !writerReady) return;
        recoverRuntimeState(symbol, at);
        var current = loadSnapshot(symbol, symbol);
        var frame = featureFrames.get(symbol);
        var anchor = signalFrames.get(symbol);
        var quote = market.quote(symbol, at).orElse(null);
        var book = market.book(symbol, at).orElse(null);
        boolean frameFresh = freshFrame(frame, at);
        boolean coreComplete = quote != null && book != null && book.coversBasisPoints(25) && frameFresh && frame.ready();
        boolean sourceLost = (previouslyHealthy.contains(symbol) || current.signal().direction() != null)
                && !coreComplete;
        if (coreComplete) previouslyHealthy.add(symbol);
        Map<String, AssetCardRiskService.Metric> metrics = riskMetrics(symbol, frameFresh ? frame : null, at);
        String breach = null;
        if (quote != null && frameFresh && anchor != null && current.signal().direction() != null) {
            if (current.signal().direction().longSide() && anchor.structuralSupport() != null
                    && quote.price().doubleValue() < anchor.structuralSupport())
                breach = "现货价 " + quote.price() + " 低于信号时结构支撑 " + anchor.structuralSupport();
            if (current.signal().direction().shortSide() && anchor.structuralResistance() != null
                    && quote.price().doubleValue() > anchor.structuralResistance())
                breach = "现货价 " + quote.price() + " 高于信号时结构阻力 " + anchor.structuralResistance();
        }
        var event = evidence == null ? null : evidence.readEventRisk(symbol, at);
        var signal = current.signal();
        var side = signal.direction() == null ? AssetCardSnapshot.SignalSide.NON_DIRECTIONAL : signal.direction().signalSide();
        var result = risks.evaluate(new AssetCardRiskService.Input(symbol, at, side, signal.direction(), signal.signalAsOf(), riskVersion(), metrics,
                model.validatedAt(at) ? model.riskDistributions(symbol, side.name()) : Map.of(), event, coreComplete, sourceLost, breach));
        if (result.invalidate() || !frameFresh && signal.direction() != null) {
            var state = signalStates.get(symbol);
            if (state != null) {
                state = signals.invalidate(state, at); signalStates.put(symbol, state); signal = state.signal();
            } else signal = signal.invalidated();
        }
        publishSignalAndRisk(current, signal, result.risk(), current.featureVersion(), current.modelVersion(), current.calibrationVersion(), at);
        healthy(Field.RISK, symbol);
    }

    private Map<String, AssetCardRiskService.Metric> riskMetrics(String symbol, AssetCardFeatureService.Frame frame, Instant at) {
        Map<String, AssetCardFeatureService.Observation> facts = rawInputs(symbol, at);
        if (frame != null) for (String key : List.of("structuralCenterDistanceAtr", "extensionAtr", "volatility5m", "return5m",
                "priceReturn1h", "slope5m", "slope1h", "slope4h", "logLongShortRatio", "liquidationImbalance")) {
            var value = frame.realInputs().get(key);
            if (value != null) facts.put(key, value);
        }
        var quote = market.quote(symbol, at).orElse(null);
        if (frame != null && quote != null && frame.atr() != null && frame.atr() > 0
                && frame.structuralSupport() != null && frame.structuralResistance() != null) {
            double price = quote.price().doubleValue(), center = (frame.structuralSupport() + frame.structuralResistance()) / 2;
            double extension = price > frame.structuralResistance() ? price - frame.structuralResistance()
                    : price < frame.structuralSupport() ? price - frame.structuralSupport() : 0;
            for (var item : Map.of("structuralCenterDistanceAtr", (price - center) / frame.atr(), "extensionAtr", extension / frame.atr()).entrySet()) {
                facts.put(item.getKey(), new AssetCardFeatureService.Observation(item.getValue(), "BINANCE_SPOT_STRUCTURE_AND_TRADE",
                        quote.observedAt(), quote.availableAt(), AssetCardFeatureService.spotInstrument(symbol),
                        AssetCardFeatureService.SPOT_SOURCE_VERSION, "ATR_MULTIPLE", quote.observedAt().plus(properties.getPriceTtl()),
                        String.valueOf(quote.tradeId())));
            }
        }
        var ratio = facts.get("longShortRatio");
        if (AssetCardFeatureService.usableObservation(symbol, "longShortRatio", ratio, at) && ratio.value() > 0)
            facts.put("logLongShortRatio", new AssetCardFeatureService.Observation(Math.log(ratio.value()), ratio.source(), ratio.observedAt(),
                    ratio.availableAt(), ratio.instrument(), ratio.sourceVersion(), "LOG_RATIO", ratio.expiresAt(), ratio.observationId()));
        var longs = facts.get("longLiquidation"); var shorts = facts.get("shortLiquidation");
        if (AssetCardFeatureService.usableObservation(symbol, "longLiquidation", longs, at)
                && AssetCardFeatureService.usableObservation(symbol, "shortLiquidation", shorts, at)
                && Objects.equals(longs.instrument(), shorts.instrument()) && Objects.equals(longs.sourceVersion(), shorts.sourceVersion())
                && longs.value() >= 0 && shorts.value() >= 0) {
            double sum = longs.value()+shorts.value();
            facts.put("liquidationImbalance",new AssetCardFeatureService.Observation(sum == 0 ? 0 : (longs.value()-shorts.value())/sum,
                    longs.source()+"+"+shorts.source(),later(longs.observedAt(),shorts.observedAt()),later(longs.availableAt(),shorts.availableAt()),
                    longs.instrument(), longs.sourceVersion(), "RATIO", longs.expiresAt().isBefore(shorts.expiresAt()) ? longs.expiresAt() : shorts.expiresAt()));
        }
        Map<String, AssetCardRiskService.Metric> result = new LinkedHashMap<>();
        facts.forEach((key, fact) -> {
            if (AssetCardFeatureService.usableObservation(symbol, key, fact, at))
                result.put(key,new AssetCardRiskService.Metric(fact.value(),fact.unit(),fact.source(),fact.observedAt(),fact.availableAt(),fact.expiresAt()));
        });
        return result;
    }

    private static Instant later(Instant a, Instant b) { return a.isAfter(b) ? a : b; }

    private void publishSignalAndRisk(AssetCardSnapshot current, AssetCardSnapshot.Signal signal, AssetCardSnapshot.Risk risk,
                                      String featureVersion, String modelVersion, String calibrationVersion, Instant at) {
        if (risk == null || !risk.matchesBasis(signal) || !Objects.equals(risk.riskVersion(), riskVersion()))
            risk = AssetCardSnapshot.Risk.unknownFor(signal, riskVersion(), "方向或风险版本已变化，等待本方向证据重新评估");
        boolean signalChanged = !Objects.equals(signal,current.signal()) || !Objects.equals(featureVersion,current.featureVersion())
                || !Objects.equals(modelVersion,current.modelVersion()) || !Objects.equals(calibrationVersion,current.calibrationVersion())
                || !Objects.equals(model.thresholdVersion(), current.thresholdVersion());
        boolean riskChanged = !sameEffectiveRisk(risk,current.risk());
        if (!signalChanged && !riskChanged) return;
        if (!riskChanged) risk = current.risk();
        Instant clock = sameEffectiveSignal(signal,current.signal()) && sameEffectiveRisk(risk,current.risk()) ? current.cardAsOf() : at;
        long version = mapper.nextSnapshotVersion(current.symbol());
        var next = new AssetCardSnapshot(current.symbol(),current.assetName(),current.spotPrice(),current.latestPriceAt(),signal,risk,
                current.health(),clock,version,featureVersion,modelVersion,calibrationVersion,model.thresholdVersion(),current.priceTradeId());
        persist(next, current.snapshotVersion());
        if (signalChanged) publish(next,"ASSET_CARD_SIGNAL",payload("signal",signal,"risk",risk,"cardAsOf",clock,
                "featureVersion",featureVersion,"modelVersion",modelVersion,"calibrationVersion",calibrationVersion),at);
        if (riskChanged) publish(next,"ASSET_CARD_RISK",payload("risk",risk,"cardAsOf",clock),at);
    }

    static boolean sameEffectiveSignal(AssetCardSnapshot.Signal a, AssetCardSnapshot.Signal b) {
        return a == b || a != null && b != null && a.direction() == b.direction() && Objects.equals(a.status(),b.status())
                && Objects.equals(a.calibratedConfidence(),b.calibratedConfidence());
    }
    static boolean sameEffectiveRisk(AssetCardSnapshot.Risk a, AssetCardSnapshot.Risk b) {
        if (a == b) return true;
        if (a == null || b == null || !Objects.equals(a.overallLevel(),b.overallLevel()) || a.items().size() != b.items().size()
                || a.riskBasisSide() != b.riskBasisSide() || a.riskBasisDirection() != b.riskBasisDirection()
                || !Objects.equals(a.riskBasisSignalAsOf(), b.riskBasisSignalAsOf()) || !Objects.equals(a.riskVersion(), b.riskVersion())) return false;
        for (int i=0;i<a.items().size();i++) {
            var x=a.items().get(i); var y=b.items().get(i);
            if (!Objects.equals(x.type(),y.type()) || !Objects.equals(x.assessmentStatus(),y.assessmentStatus())
                    || !Objects.equals(x.level(),y.level()) || !Objects.equals(x.evidenceValue(),y.evidenceValue())
                    || !Objects.equals(x.unit(),y.unit()) || !Objects.equals(x.source(),y.source()) || !Objects.equals(x.reason(),y.reason())
                    || x.hardInvalidation() != y.hardInvalidation()) return false;
        }
        return true;
    }

    private void reconcileSubscriptionsSafely() {
        try { reconcileSubscriptions(); }
        catch (RuntimeException failure) { log.warn("[asset-card] Subscription read failed ({})", failure.getClass().getSimpleName()); }
    }

    public void flushPrices() { flushPrices(Instant.now()); }

    void flushPrices(Instant at) {
        if (!properties.isEnabled()) return;
        for (String symbol : market.subscribedSymbols()) {
            try { refreshPrice(symbol, at); healthy(Field.PERSISTENCE, symbol); }
            catch (RuntimeException failure) {
                failed(Field.PERSISTENCE, symbol, "卡片快照存储暂时不可用");
                log.warn("[asset-card] Snapshot publication failed ({})", failure.getClass().getSimpleName());
            }
        }
    }

    private void refreshPrice(String symbol, Instant at) {
        synchronized (symbolLock(symbol)) { refreshPriceLocked(symbol, at); }
    }

    private void refreshPriceLocked(String symbol, Instant at) {
        AssetCardSnapshot current;
        try {
            if (!started || writerReady) recoverRuntimeState(symbol, at);
            current = loadSnapshot(symbol, symbol);
        } catch (RuntimeException unavailableStore) {
            failed(Field.RECOVERY, symbol, "卡片快照恢复暂不可用；现货成交仍独立更新");
            current = snapshots.getOrDefault(symbol, AssetCardSnapshot.unavailable(symbol, symbol, "卡片快照存储暂不可用"));
        }
        var quote = market.quote(symbol, at).orElse(null);
        var price = quote == null ? null : quote.price();
        var priceAt = quote == null ? null : quote.observedAt();
        var health = new AssetCardSnapshot.Health(quote == null ? "SOURCE_UNAVAILABLE" : "HEALTHY",
                quote == null ? "Binance现货成交数据尚未就绪或已过期" : null, quote == null ? at : priceAt);
        var signal = current.signal();
        var risk = current.risk();
        if (quote == null && signal != null && signal.direction() != null) {
            var state = signalStates.get(symbol);
            if (state != null) {
                state = signals.invalidate(state, at); signalStates.put(symbol, state); signal = state.signal();
            } else signal = signal.invalidated();
            risk = sourceLostRisk(signal, risk, at);
        }
        boolean priceChanged = !Objects.equals(price, current.spotPrice()) || !Objects.equals(priceAt, current.latestPriceAt())
                || quote != null && !Objects.equals(quote.tradeId(), current.priceTradeId());
        boolean signalChanged = !Objects.equals(signal, current.signal());
        boolean riskChanged = !sameEffectiveRisk(risk, current.risk());
        if (!riskChanged) risk = current.risk();
        boolean healthChanged = current.health() == null || !Objects.equals(health.status(), current.health().status())
                || !Objects.equals(health.reason(), current.health().reason());
        if (!priceChanged && !signalChanged && !riskChanged && !healthChanged) return;
        boolean durableChange = signalChanged || riskChanged || healthChanged;
        long version = current.snapshotVersion();
        boolean canPersist = !started || writerReady;
        if (durableChange && canPersist) {
            try { version = mapper.nextSnapshotVersion(symbol); }
            catch (RuntimeException denied) { canPersist = false; failed(Field.PERSISTENCE, symbol, "卡片状态保存失败；价格独立更新"); }
        }
        var next = new AssetCardSnapshot(symbol, current.assetName(), price, priceAt, signal, risk, health,
                sameEffectiveSignal(signal, current.signal()) && sameEffectiveRisk(risk, current.risk()) ? current.cardAsOf() : at, version,
                current.featureVersion(), current.modelVersion(), current.calibrationVersion(),current.thresholdVersion(), quote == null ? null : quote.tradeId());
        if (durableChange && canPersist) {
            try { persist(next, current.snapshotVersion()); }
            catch (RuntimeException denied) {
                failed(Field.PERSISTENCE, symbol, "卡片状态保存失败；价格独立更新");
                // A failed CAS must not publish uncommitted signal/risk as a new durable version.
                next = new AssetCardSnapshot(symbol, current.assetName(), price, priceAt, current.signal(), current.risk(), health,
                        current.cardAsOf(), current.snapshotVersion(), current.featureVersion(), current.modelVersion(),
                        current.calibrationVersion(), current.thresholdVersion(), quote == null ? null : quote.tradeId());
                canPersist = false;
            }
        }
        if (!durableChange || canPersist) snapshots.put(symbol, next);
        if (priceChanged && quote != null) publish(next, "ASSET_CARD_PRICE", payload("spotPrice", price, "latestPriceAt", priceAt,
                "priceTradeId", quote.tradeId()), at);
        if (!canPersist) {
            if (quote == null && usesCardSignalDisplay(symbol))
                dispatch(next, "ASSET_CARD_HEALTH", payload("health", health, "signal", signal, "risk", risk), at);
            return;
        }
        if (signalChanged) publish(next, "ASSET_CARD_SIGNAL", payload("signal", signal, "risk", risk, "cardAsOf", next.cardAsOf(),
                "featureVersion",next.featureVersion(),"modelVersion",next.modelVersion(),"calibrationVersion",next.calibrationVersion()), at);
        if (riskChanged) publish(next, "ASSET_CARD_RISK", payload("risk", risk, "cardAsOf", next.cardAsOf()), at);
        if (healthChanged) publish(next, "ASSET_CARD_HEALTH", payload("health", health, "signal", signal, "risk", risk), at);
    }

    private AssetCardSnapshot.Risk sourceLostRisk(AssetCardSnapshot.Signal signal, AssetCardSnapshot.Risk previous, Instant at) {
        List<AssetCardSnapshot.RiskItem> items = new ArrayList<>((previous != null && previous.matchesBasis(signal)
                && Objects.equals(previous.riskVersion(), riskVersion()) ? previous
                : AssetCardSnapshot.Risk.unknownFor(signal, riskVersion(), "当前信号风险证据未就绪")).items());
        items.removeIf(item -> "DATA".equals(item.type()));
        items.add(new AssetCardSnapshot.RiskItem("DATA", "ASSESSED", "HIGH", "SPOT_SOURCE_UNAVAILABLE",
                "BINANCE_SPOT_AGG_TRADE", at, "真实现货成交来源缺失或已过期", "SOURCE_STATE", true));
        return new AssetCardSnapshot.Risk("HIGH", items, at,
                signal.direction() == null ? AssetCardSnapshot.SignalSide.NON_DIRECTIONAL : signal.direction().signalSide(),
                signal.direction(), signal.signalAsOf(), previous == null ? null : previous.riskMarketAsOf(), riskVersion());
    }

    private void persist(AssetCardSnapshot snapshot, long expectedSnapshotVersion) {
        try {
            com.fasterxml.jackson.databind.node.ObjectNode stored = json.valueToTree(snapshot);
            var state = signalStates.get(snapshot.symbol());
            var frame = featureFrames.get(snapshot.symbol());
            if (state != null && frame != null) stored.set("_runtime", json.valueToTree(payload(
                    "state", state, "frame", frame, "signalFrame", signalFrames.get(snapshot.symbol()),
                    "modelMode", properties.getModelMode(), "dataKind", "LIVE_OBSERVED_CARD_INPUTS")));
            if (mapper.saveSnapshot(snapshot.symbol(), expectedSnapshotVersion, snapshot.snapshotVersion(), json.writeValueAsString(stored), snapshot.cardAsOf()) != 1) {
                snapshots.remove(snapshot.symbol());
                throw new IllegalStateException("ASSET_CARD_CONCURRENT_SNAPSHOT_REJECTED");
            }
            snapshots.put(snapshot.symbol(), snapshot);
        } catch (JsonProcessingException failure) { throw new IllegalStateException("ASSET_CARD_SERIALIZATION_FAILED", failure); }
    }

    private void publish(AssetCardSnapshot snapshot, String type, Map<String, Object> values, Instant at) {
        // Private computation/persistence above is never gated by the browser rollout cohort.
        if (!usesCardSignalDisplay(snapshot.symbol())) return;
        var visible = publicModelProjection(snapshot, at);
        if ("ASSET_CARD_SIGNAL".equals(type)) { values.put("signal", visible.signal()); values.put("risk", visible.risk()); }
        if ("ASSET_CARD_RISK".equals(type)) values.put("risk", visible.risk());
        if ("ASSET_CARD_HEALTH".equals(type)) {
            values.put("health", visible.health()); values.put("signal", visible.signal()); values.put("risk", visible.risk());
        }
        dispatch(snapshot, type, values, at);
        // A price/risk-only update must also revoke an already-rendered old percentage, at the same version.
        if (visible != snapshot && !"ASSET_CARD_HEALTH".equals(type))
            dispatch(snapshot, "ASSET_CARD_HEALTH", payload("health", visible.health(), "signal", visible.signal(), "risk", visible.risk()), at);
    }

    private void dispatch(AssetCardSnapshot snapshot, String type, Map<String, Object> values, Instant at) {
        values.put("symbol", snapshot.symbol());
        values.put("snapshotVersion", snapshot.snapshotVersion());
        values.put("featureVersion", snapshot.featureVersion());
        values.put("modelVersion", snapshot.modelVersion());
        values.put("calibrationVersion", snapshot.calibrationVersion());
        values.put("thresholdVersion", snapshot.thresholdVersion());
        var boundRisk = values.get("risk") instanceof AssetCardSnapshot.Risk r ? r : snapshot.risk();
        values.put("riskVersion", boundRisk.riskVersion());
        values.put("riskBasisSide", boundRisk.riskBasisSide());
        values.put("riskBasisDirection", boundRisk.riskBasisDirection());
        values.put("riskBasisSignalAsOf", boundRisk.riskBasisSignalAsOf());
        events.publish(new DashboardLiveEvent(type + ":" + snapshot.symbol() + ":" + snapshot.snapshotVersion(), type,
                snapshot.symbol(), snapshot.snapshotVersion(), at, at, values));
    }

    private static Map<String, Object> payload(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) values.put((String) pairs[i], pairs[i + 1]);
        return values;
    }

    /** No cache insertion, version allocation, event publication, subscription or other write on this path. */
    public AssetCardSnapshot snapshot(String symbol, String name) {
        String normalized = normalize(symbol);
        Instant now = Instant.now();
        AssetCardSnapshot stored;
        try { stored = loadSnapshot(normalized, name); }
        catch (RuntimeException unavailableStore) { stored = AssetCardSnapshot.unavailable(normalized, name, "卡片快照存储暂不可用"); }
        var value = publicModelProjection(stored, now);
        var quote = market.quote(normalized, now).orElse(null);
        var currentPrice = quote == null ? value.spotPrice() : quote.price();
        var currentPriceAt = quote == null ? value.latestPriceAt() : quote.observedAt();
        Long priceTradeId = quote == null ? value.priceTradeId() : Long.valueOf(quote.tradeId());
        var signal = value.signal();
        var risk = value.risk();
        var health = value.health();
        boolean priceMissing = currentPrice == null || currentPriceAt == null || currentPriceAt.isAfter(now)
                || Duration.between(currentPriceAt, now).compareTo(properties.getPriceTtl()) > 0;
        if (priceMissing) {
            signal = signal.direction() != null ? signal.invalidated() : signal;
            risk = sourceLostRisk(signal, risk, now);
            health = new AssetCardSnapshot.Health("SOURCE_UNAVAILABLE", "真实现货成交价格尚未就绪或已过期", now);
        } else {
            // Risk, model, storage and recovery faults never become a PRICE source failure.
            String riskFailure = failure(Field.RISK, normalized);
            if (riskFailure != null) risk = AssetCardSnapshot.Risk.unknownFor(signal, riskVersion(), riskFailure);
            String signalFailure = failure(Field.SIGNAL, normalized);
            if (signalFailure != null) {
                signal = new AssetCardSnapshot.Signal(signal.direction(), "FAILED", null, null, null,
                        signal.oneHourState(), signal.fourHourTrend(), signal.signalAsOf());
                health = new AssetCardSnapshot.Health("SIGNAL_FAILED", signalFailure, now);
            }
            if (failure(Field.PERSISTENCE, normalized) != null)
                health = new AssetCardSnapshot.Health("PERSISTENCE_UNAVAILABLE", failure(Field.PERSISTENCE, normalized), now);
        }
        return new AssetCardSnapshot(normalized, name, priceMissing ? null : currentPrice, priceMissing ? null : currentPriceAt, signal, risk,
                health, value.cardAsOf(), value.snapshotVersion(), value.featureVersion(), value.modelVersion(), value.calibrationVersion(),value.thresholdVersion(),
                priceMissing ? null : priceTradeId);
    }

    /** Current model identity is checked at every public read/event; stored private facts are never rewritten. */
    private AssetCardSnapshot publicModelProjection(AssetCardSnapshot snapshot, Instant checkedAt) {
        var signal = snapshot.signal();
        var risk = snapshot.risk();
        if (!risk.matchesBasis(signal) || !Objects.equals(risk.riskVersion(), riskVersion()))
            risk = AssetCardSnapshot.Risk.unknownFor(signal, riskVersion(), "风险身份或版本未匹配当前信号");
        boolean hasModelResult = signal != null && (signal.direction() != null || signal.calibratedConfidence() != null
                || signal.pLong() != null || signal.pShort() != null);
        var current = model;
        boolean trusted = !hasModelResult || current != null && current.validatedAt(checkedAt) && current.validatedAssets().contains(snapshot.symbol())
                && AssetCardFeatureService.FEATURE_VERSION.equals(snapshot.featureVersion())
                && current.modelVersion() != null && !current.modelVersion().isBlank()
                && current.calibrationVersion() != null && !current.calibrationVersion().isBlank()
                && Objects.equals(current.modelVersion(), snapshot.modelVersion())
                && Objects.equals(current.calibrationVersion(), snapshot.calibrationVersion())
                && Objects.equals(current.thresholdVersion(), snapshot.thresholdVersion());
        boolean sourceLost = snapshot.health() != null && "SOURCE_UNAVAILABLE".equals(snapshot.health().status());
        if (trusted && risk == snapshot.risk() && !sourceLost) return snapshot;
        if (!trusted) {
            signal = AssetCardSnapshot.Signal.unavailable("UNVALIDATED", signal.signalAsOf());
            risk = AssetCardSnapshot.Risk.unknownFor(signal, riskVersion(), "模型身份失效，不能沿用此前方向风险");
        }
        if (sourceLost) {
            signal = signal.direction() == null ? signal : signal.invalidated();
            risk = sourceLostRisk(signal, risk, checkedAt);
        }
        return new AssetCardSnapshot(snapshot.symbol(), snapshot.assetName(), snapshot.spotPrice(), snapshot.latestPriceAt(),
                signal, risk, trusted || sourceLost ? snapshot.health() : new AssetCardSnapshot.Health("MODEL_UNAVAILABLE", "卡片模型、校准或阈值版本不可用", checkedAt),
                snapshot.cardAsOf(), snapshot.snapshotVersion(), snapshot.featureVersion(), snapshot.modelVersion(), snapshot.calibrationVersion(),snapshot.thresholdVersion(),snapshot.priceTradeId());
    }

    private AssetCardSnapshot loadSnapshot(String symbol, String name) {
        var cached = snapshots.get(symbol);
        if (cached != null) return cached;
        String stored = mapper.selectSnapshotJson(symbol);
        if (stored == null) return AssetCardSnapshot.unavailable(symbol, name, "尚未产生独立卡片快照");
        try {
            com.fasterxml.jackson.databind.node.ObjectNode projection = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(stored);
            projection.remove("_runtime"); // private restart state is never returned by Home/card APIs
            var loaded = json.treeToValue(projection, AssetCardSnapshot.class);
            if (!symbol.equals(loaded.symbol()) || loaded.signal() == null || loaded.risk() == null || loaded.snapshotVersion() <= 0)
                throw new IllegalStateException("ASSET_CARD_STORED_IDENTITY_INVALID");
            return loaded;
        } catch (JsonProcessingException invalid) { throw new IllegalStateException("ASSET_CARD_STORED_SNAPSHOT_INVALID", invalid); }
    }

    public List<AssetCardSnapshot> snapshotsForUser(Long userId, Collection<String> requested) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("Authenticated user required");
        if (requested == null || requested.size() > 6) throw new IllegalArgumentException("At most six displayed card symbols required");
        var members = pool.listForUser(userId);
        Map<String, String> names = new HashMap<>();
        members.forEach(member -> names.put(normalize(member.symbol()), member.displayName()));
        LinkedHashSet<String> symbols = new LinkedHashSet<>();
        for (String symbol : requested) {
            String normalized = normalize(symbol);
            if (!names.containsKey(normalized) || !symbols.add(normalized)) throw new IllegalArgumentException("Invalid displayed card selection");
        }
        return symbols.stream().filter(this::usesCardSignalDisplay)
                .map(symbol -> snapshot(symbol, names.get(symbol))).toList();
    }

    /** Rollout cohort only: absent/invalid models stay on the new fail-closed projection, never legacy. */
    public boolean usesCardSignalDisplay(String symbol) {
        if (!properties.isEnabled() || symbol == null) return false;
        final String canonical;
        try { canonical = normalize(symbol); }
        catch (IllegalArgumentException invalid) { return false; }
        return properties.getModelMode() == AssetCardProperties.ModelMode.ACTIVE
                || properties.getModelMode() == AssetCardProperties.ModelMode.CANARY
                && properties.getCanarySymbols().contains(canonical);
    }

    private static String normalize(String symbol) {
        if (symbol == null) throw new IllegalArgumentException("Card symbol required");
        String value = symbol.trim().toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z0-9]{2,32}")) throw new IllegalArgumentException("Invalid card symbol");
        return value;
    }
    @PreDestroy public void close() { started = false; background.shutdownNow(); inference.shutdownNow(); riskWorkers.shutdownNow(); model.close(); }
}
