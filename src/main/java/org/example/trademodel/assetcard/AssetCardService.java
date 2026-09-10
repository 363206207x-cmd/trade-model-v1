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
    private final Map<String, Instant> queuedBars = new ConcurrentHashMap<>();
    private final Set<String> recovered = ConcurrentHashMap.newKeySet();
    private final Set<String> previouslyHealthy = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService inference = Executors.newScheduledThreadPool(2, task -> {
        Thread thread = new Thread(task, "asset-card-inference"); thread.setDaemon(true); return thread;
    });
    private final Map<String, AssetCardSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Map<String, String> failures = new ConcurrentHashMap<>();
    private final ScheduledExecutorService background = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "asset-card-projection"); thread.setDaemon(true); return thread;
    });
    private boolean started;

    public AssetCardService(AssetCardProperties properties, AssetCardMarketDataService market, AssetCardMapper mapper,
                            AssetPoolService pool, DashboardLiveEventService events, ObjectMapper json) {
        this.properties = properties; this.market = market; this.mapper = mapper;
        this.pool = pool; this.events = events; this.json = json;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setEvidenceService(AssetCardEvidenceService evidence) { this.evidence = evidence; }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if (started || !properties.isEnabled()) return;
        started = true;
        if (properties.getModelBundlePath() != null && !properties.getModelBundlePath().isBlank())
            model = AssetCardModelBundle.load(Path.of(properties.getModelBundlePath()), properties.getModelBundleSha256());
        market.addListener(this::onMarketUpdate);
        background.scheduleWithFixedDelay(this::reconcileSubscriptionsSafely, 0, 60, TimeUnit.SECONDS);
        background.scheduleWithFixedDelay(this::flushPrices, 1, 1, TimeUnit.SECONDS);
        background.scheduleWithFixedDelay(this::refreshRisksSafely, 1, 1, TimeUnit.SECONDS);
        inference.scheduleWithFixedDelay(this::reconcileEvidenceSafely, 0, 60, TimeUnit.SECONDS);
    }

    /** Source is the existing pool union, never a page visit, click, or a new subscription-writing GET. */
    public void reconcileSubscriptions() {
        market.reconcileSubscriptions(pool.listScanSymbols());
        for (String symbol : market.subscribedSymbols()) recoverRuntimeState(symbol, Instant.now());
    }

    /** Only a closed market bar can enqueue inference. No page request reaches this method. */
    void onMarketUpdate(AssetCardMarketDataService.MarketUpdate update) {
        if (!started || !properties.isEnabled() || !"BAR".equals(update.type())) return;
        if ("1m".equals(update.interval())) {
            inference.execute(() -> refreshMinuteVolatility(update.symbol(), Instant.now()));
            return;
        }
        if (!"5m".equals(update.interval()) || update.observedAt() == null) return;
        queuedBars.compute(update.symbol(), (symbol, previous) -> {
            if (previous != null && !update.observedAt().isAfter(previous)) return previous;
            inference.schedule(() -> inferClosedBar(symbol, update.observedAt(), Instant.now()), 1, TimeUnit.SECONDS);
            return update.observedAt();
        });
    }

    private void reconcileEvidenceSafely() {
        if (!properties.isEnabled() || evidence == null) return;
        for (String symbol : market.subscribedSymbols()) {
            try { providerFacts.put(symbol, evidence.read(symbol, Instant.now())); }
            catch (RuntimeException failure) { providerFacts.remove(symbol); }
        }
    }

    synchronized void inferClosedBar(String symbol, Instant closedAt, Instant at) {
        if (!properties.isEnabled() || closedAt == null || closedAt.isAfter(at)
                || Duration.between(closedAt, at).compareTo(Duration.ofSeconds(17)) > 0) return;
        try {
            recoverRuntimeState(symbol, at);
            var completed = signalStates.get(symbol);
            if (completed != null && completed.lastClosed5mAt() != null && !closedAt.isAfter(completed.lastClosed5mAt())) {
                var current = loadSnapshot(symbol, symbol);
                publishSignalAndRisk(current, completed.signal(), current.risk(), AssetCardFeatureService.FEATURE_VERSION,
                        model.modelVersion(), model.calibrationVersion(), at);
                return;
            }
            // A completed audit in this close's allowed inference window prevents restart/replay duplication.
            var prior = mapper.selectFeatureHistory(symbol, closedAt, closedAt.plusSeconds(17), at, 1);
            if (prior != null && !prior.isEmpty()) {
                restoreAudit(symbol, prior.get(0).payloadJson(), at);
                return;
            }
            Map<String, List<AssetCardFeatureService.Bar>> bars = new LinkedHashMap<>();
            for (String interval : AssetCardFeatureService.INTERVALS) bars.put(interval,
                    market.bars(symbol, interval, at, 24).stream().map(AssetCardService::featureBar).toList());
            boolean aligned = true;
            for (var entry : bars.entrySet()) if (!latestIntervalClosed(entry.getValue(), entry.getKey(), closedAt)) {
                aligned = false;
                // A previous 1h/4h window is not this boundary's complete input.
                entry.setValue(List.of());
            }
            Instant deadline = closedAt.plusSeconds(15);
            if (!aligned && at.isBefore(deadline)) {
                if (started) inference.schedule(() -> inferClosedBar(symbol, closedAt, Instant.now()),
                        Math.max(1, Math.min(1000, Duration.between(at, deadline).toMillis())), TimeUnit.MILLISECONDS);
                return;
            }
            Map<String, AssetCardFeatureService.Observation> inputs = rawInputs(symbol, at);
            var raw = new AssetCardFeatureService.RawFrame(symbol, at, bars, inputs);
            var frame = featureBuilder.build(raw);
            // Missing/old 5m bars do not produce an inference for an unrelated close.
            if (frame.closed5mAt() == null || !frame.closed5mAt().equals(closedAt)) return;
            var prediction = at.isAfter(deadline) ? null : model.predict(frame);
            synchronized (this) {
                var previous = signalStates.getOrDefault(symbol, AssetCardSignalService.State.initial(symbol));
                var state = signals.evaluate(previous, frame, model, prediction, properties.getModelMode(), properties.getCanarySymbols());
                var anchor = Objects.equals(state.signal().signalAsOf(), frame.signalAsOf()) ? frame : signalFrames.get(symbol);
                Map<String, Object> audit = payload("rawFrame", raw, "frame", frame, "signalFrame", anchor, "state", state,
                        "modelMode", properties.getModelMode(), "dataKind", "LIVE_OBSERVED_CARD_INPUTS");
                if (mapper.saveFeatureHistory(symbol, at, at, json.writeValueAsString(audit)) != 1) return;
                signalStates.put(symbol, state); featureFrames.put(symbol, frame);
                if (Objects.equals(state.signal().signalAsOf(), frame.signalAsOf())) signalFrames.put(symbol, frame);
                var current = loadSnapshot(symbol, symbol);
                publishSignalAndRisk(current, state.signal(), current.risk(), frame.featureVersion(),
                        model.modelVersion(), model.calibrationVersion(), at);
                failures.remove(symbol, "卡片分析暂时不可用");
            }
        } catch (RuntimeException | JsonProcessingException failure) {
            failures.put(symbol, "卡片分析暂时不可用");
            log.warn("[asset-card] Closed-bar analysis failed ({})", failure.getClass().getSimpleName());
        }
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
    synchronized void recoverRuntimeState(String symbol, Instant at) {
        if (recovered.contains(symbol)) return;
        try {
            String stored = mapper.selectSnapshotJson(symbol);
            if (stored != null) {
                var runtime = json.readTree(stored).get("_runtime");
                if (runtime != null && !runtime.isNull()) restoreAudit(symbol, runtime.toString(), at);
            }
            var history = mapper.selectFeatureHistory(symbol, at.minusSeconds(900), at, at, 32);
            if (history != null) for (var audit : history) restoreAudit(symbol, audit.payloadJson(), at);
            recovered.add(symbol);
        } catch (RuntimeException | JsonProcessingException failure) {
            failures.put(symbol, "卡片运行状态恢复暂时不可用");
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
                q.price().doubleValue(), "BINANCE_SPOT", q.observedAt(), q.availableAt())));
        market.book(symbol, at).ifPresent(book -> {
            double bid = book.bids().get(0).price().doubleValue(), ask = book.asks().get(0).price().doubleValue(), mid = (bid + ask) / 2;
            inputs.put("spreadBps", observation((ask - bid) / mid * 10_000, book));
            for (int bps : List.of(10, 25)) if (book.coversBasisPoints(bps)) {
                double low = mid * (1 - bps / 10_000.0), high = mid * (1 + bps / 10_000.0);
                double bids = book.bids().stream().filter(l -> l.price().doubleValue() >= low)
                        .mapToDouble(l -> l.price().doubleValue() * l.quantity().doubleValue()).sum();
                double asks = book.asks().stream().filter(l -> l.price().doubleValue() <= high)
                        .mapToDouble(l -> l.price().doubleValue() * l.quantity().doubleValue()).sum();
                inputs.put("depth" + bps + "Bps", observation(bids + asks, book));
                if (bps == 25 && bids + asks > 0) inputs.put("bookImbalance", observation((bids - asks) / (bids + asks), book));
            }
        });
        var minute = minuteVolatility.get(symbol);
        if (minute != null && !minute.availableAt().isAfter(at) && Duration.between(minute.observedAt(), at).getSeconds() <= 75)
            inputs.put("volatility1m", minute);
        return inputs;
    }

    private static AssetCardFeatureService.Observation observation(double value, AssetCardMarketDataService.SpotBook book) {
        return new AssetCardFeatureService.Observation(value, book.source(), book.observedAt(), book.availableAt());
    }
    private static AssetCardFeatureService.Bar featureBar(AssetCardMarketDataService.SpotBar bar) {
        return new AssetCardFeatureService.Bar(bar.openTime(), bar.closeTime(), bar.availableAt(), bar.open().doubleValue(),
                bar.high().doubleValue(), bar.low().doubleValue(), bar.close().doubleValue(), bar.volume().doubleValue(),
                bar.takerBuyBaseVolume() == null ? null : bar.takerBuyBaseVolume().doubleValue(), bar.tradeCount());
    }

    private void refreshMinuteVolatility(String symbol, Instant at) {
        try {
            var bars = market.bars(symbol, "1m", at, 13);
            if (bars.size() != 13 || Duration.between(bars.get(12).closeTime(), at).getSeconds() > 75) return;
            double[] returns = new double[12]; double mean = 0;
            for (int i = 1; i < bars.size(); i++) {
                if (!bars.get(i).openTime().equals(bars.get(i - 1).openTime().plusSeconds(60))) return;
                returns[i-1] = Math.log(bars.get(i).close().doubleValue() / bars.get(i-1).close().doubleValue()); mean += returns[i-1] / 12;
            }
            double variance = 0; for (double value : returns) variance += (value - mean) * (value - mean) / 12;
            minuteVolatility.put(symbol, new AssetCardFeatureService.Observation(Math.sqrt(variance), "BINANCE_SPOT_CLOSED_1M",
                    bars.get(12).closeTime(), bars.stream().map(AssetCardMarketDataService.SpotBar::availableAt).max(Comparator.naturalOrder()).orElseThrow()));
        } catch (RuntimeException failure) { minuteVolatility.remove(symbol); }
    }

    private void refreshRisksSafely() {
        if (!properties.isEnabled()) return;
        for (String symbol : market.subscribedSymbols()) {
            try { refreshRisk(symbol, Instant.now()); }
            catch (RuntimeException failure) {
                failures.put(symbol, "卡片风险证据暂时不可用");
                log.warn("[asset-card] Risk refresh failed ({})", failure.getClass().getSimpleName());
            }
        }
    }

    synchronized void refreshRisk(String symbol, Instant at) {
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
        var result = risks.evaluate(new AssetCardRiskService.Input(symbol, at, metrics,
                model.riskDistributions().getOrDefault(symbol, Map.of()), event, coreComplete, sourceLost, breach));
        var signal = current.signal();
        if (result.invalidate() || !frameFresh && signal.direction() != null) {
            var state = signalStates.get(symbol);
            if (state != null) {
                state = signals.invalidate(state, at); signalStates.put(symbol, state); signal = state.signal();
            } else signal = signal.invalidated();
        }
        publishSignalAndRisk(current, signal, result.risk(), current.featureVersion(), current.modelVersion(), current.calibrationVersion(), at);
        failures.remove(symbol, "卡片风险证据暂时不可用");
    }

    private Map<String, AssetCardRiskService.Metric> riskMetrics(String symbol, AssetCardFeatureService.Frame frame, Instant at) {
        Map<String, AssetCardFeatureService.Observation> facts = rawInputs(symbol, at);
        Map<String, Instant> expiry = new HashMap<>();
        var cg = providerFacts.get(symbol);
        if (cg != null) cg.facts().forEach((key, fact) -> expiry.put(key, fact.expiresAt()));
        if (frame != null) for (String key : List.of("structuralCenterDistanceAtr", "extensionAtr", "volatility5m", "timeframeConflict")) {
            var value = frame.realInputs().get(key);
            if (value != null) { facts.put(key, value); expiry.put(key, frame.closed5mAt().plusSeconds(315)); }
        }
        var quote = market.quote(symbol, at).orElse(null);
        if (frame != null && quote != null && frame.atr() != null && frame.atr() > 0
                && frame.structuralSupport() != null && frame.structuralResistance() != null) {
            double price = quote.price().doubleValue(), center = (frame.structuralSupport() + frame.structuralResistance()) / 2;
            facts.put("structuralCenterDistanceAtr", new AssetCardFeatureService.Observation(Math.abs(price - center) / frame.atr(),
                    "BINANCE_SPOT_STRUCTURE_AND_TRADE", quote.observedAt(), quote.availableAt()));
            facts.put("extensionAtr", new AssetCardFeatureService.Observation(Math.max(0,Math.max(price - frame.structuralResistance(),
                    frame.structuralSupport() - price)) / frame.atr(), "BINANCE_SPOT_STRUCTURE_AND_TRADE", quote.observedAt(), quote.availableAt()));
            expiry.put("structuralCenterDistanceAtr", quote.observedAt().plus(properties.getPriceTtl()));
            expiry.put("extensionAtr", quote.observedAt().plus(properties.getPriceTtl()));
        }
        for (String[] mapping : List.of(new String[]{"fundingRate","absFundingRate"}, new String[]{"bookImbalance","absBookImbalance"},
                new String[]{"openInterestChange1h","absOpenInterestChange1h"})) {
            var fact = facts.get(mapping[0]);
            if (fact != null) {
                facts.put(mapping[1],new AssetCardFeatureService.Observation(Math.abs(fact.value()),fact.source(),fact.observedAt(),fact.availableAt()));
                if (expiry.containsKey(mapping[0])) expiry.put(mapping[1],expiry.get(mapping[0]));
            }
        }
        var ratio = facts.get("longShortRatio");
        if (ratio != null && ratio.value() > 0) {
            facts.put("absLogLongShortRatio",new AssetCardFeatureService.Observation(Math.abs(Math.log(ratio.value())),ratio.source(),ratio.observedAt(),ratio.availableAt()));
            if (expiry.containsKey("longShortRatio")) expiry.put("absLogLongShortRatio",expiry.get("longShortRatio"));
        }
        var longs = facts.get("longLiquidation"); var shorts = facts.get("shortLiquidation");
        if (longs != null && shorts != null && longs.value() >= 0 && shorts.value() >= 0) {
            double sum = longs.value()+shorts.value();
            facts.put("absLiquidationImbalance",new AssetCardFeatureService.Observation(sum == 0 ? 0 : Math.abs(longs.value()-shorts.value())/sum,
                    longs.source()+"+"+shorts.source(),later(longs.observedAt(),shorts.observedAt()),later(longs.availableAt(),shorts.availableAt())));
            Instant a = expiry.get("longLiquidation"), b = expiry.get("shortLiquidation");
            if (a != null && b != null) expiry.put("absLiquidationImbalance",a.isBefore(b)?a:b);
        }
        Map<String, AssetCardRiskService.Metric> result = new LinkedHashMap<>();
        facts.forEach((key, fact) -> {
            String unit = switch (key) {
                case "structuralCenterDistanceAtr", "extensionAtr" -> "ATR_MULTIPLE";
                case "volatility1m", "volatility5m" -> "LOG_RETURN_STD";
                case "spreadBps" -> "BASIS_POINTS";
                case "depth10Bps", "depth25Bps", "longLiquidation", "shortLiquidation" -> "QUOTE_CURRENCY";
                case "absFundingRate" -> "RATE";
                case "absOpenInterestChange1h" -> "PERCENT";
                case "absLogLongShortRatio" -> "LOG_RATIO";
                case "absBookImbalance", "absLiquidationImbalance", "timeframeConflict" -> "RATIO";
                default -> null;
            };
            if (unit != null && fact.value() != null && fact.observedAt() != null)
                result.put(key,new AssetCardRiskService.Metric(fact.value(),unit,fact.source(),fact.observedAt(),fact.availableAt(),
                        expiry.getOrDefault(key,fact.observedAt().plusSeconds("volatility1m".equals(key)?75:properties.getPriceTtl().getSeconds()))));
        });
        return result;
    }

    private static Instant later(Instant a, Instant b) { return a.isAfter(b) ? a : b; }

    private void publishSignalAndRisk(AssetCardSnapshot current, AssetCardSnapshot.Signal signal, AssetCardSnapshot.Risk risk,
                                      String featureVersion, String modelVersion, String calibrationVersion, Instant at) {
        boolean signalChanged = !Objects.equals(signal,current.signal()) || !Objects.equals(featureVersion,current.featureVersion())
                || !Objects.equals(modelVersion,current.modelVersion()) || !Objects.equals(calibrationVersion,current.calibrationVersion());
        boolean riskChanged = !Objects.equals(risk,current.risk());
        if (!signalChanged && !riskChanged) return;
        Instant clock = sameEffectiveSignal(signal,current.signal()) && sameEffectiveRisk(risk,current.risk()) ? current.cardAsOf() : at;
        long version = mapper.nextSnapshotVersion(current.symbol());
        var next = new AssetCardSnapshot(current.symbol(),current.assetName(),current.spotPrice(),current.latestPriceAt(),signal,risk,
                current.health(),clock,version,featureVersion,modelVersion,calibrationVersion);
        persist(next);
        if (signalChanged) publish(next,"ASSET_CARD_SIGNAL",payload("signal",signal,"cardAsOf",clock,
                "featureVersion",featureVersion,"modelVersion",modelVersion,"calibrationVersion",calibrationVersion),at);
        if (riskChanged) publish(next,"ASSET_CARD_RISK",payload("risk",risk,"cardAsOf",clock),at);
    }

    static boolean sameEffectiveSignal(AssetCardSnapshot.Signal a, AssetCardSnapshot.Signal b) {
        return a == b || a != null && b != null && a.direction() == b.direction() && Objects.equals(a.status(),b.status())
                && Objects.equals(a.calibratedConfidence(),b.calibratedConfidence());
    }
    static boolean sameEffectiveRisk(AssetCardSnapshot.Risk a, AssetCardSnapshot.Risk b) {
        if (a == b) return true;
        if (a == null || b == null || !Objects.equals(a.overallLevel(),b.overallLevel()) || a.items().size() != b.items().size()) return false;
        for (int i=0;i<a.items().size();i++) {
            var x=a.items().get(i); var y=b.items().get(i);
            if (!Objects.equals(x.type(),y.type()) || !Objects.equals(x.assessmentStatus(),y.assessmentStatus())
                    || !Objects.equals(x.level(),y.level()) || !Objects.equals(x.evidenceValue(),y.evidenceValue())
                    || !Objects.equals(x.unit(),y.unit()) || !Objects.equals(x.source(),y.source()) || !Objects.equals(x.reason(),y.reason())) return false;
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
            try { refreshPrice(symbol, at); failures.remove(symbol, "卡片快照存储暂时不可用"); }
            catch (RuntimeException failure) {
                failures.put(symbol, "卡片快照存储暂时不可用");
                log.warn("[asset-card] Snapshot publication failed ({})", failure.getClass().getSimpleName());
            }
        }
    }

    private synchronized void refreshPrice(String symbol, Instant at) {
        recoverRuntimeState(symbol, at);
        var current = loadSnapshot(symbol, symbol);
        var quote = market.quote(symbol, at).orElse(null);
        var price = quote == null ? null : quote.price();
        var priceAt = quote == null ? null : quote.observedAt();
        var health = new AssetCardSnapshot.Health(quote == null ? "SOURCE_UNAVAILABLE" : "HEALTHY",
                quote == null ? "Binance现货成交数据尚未就绪或已过期" : null, priceAt);
        var signal = current.signal();
        var risk = current.risk();
        if (quote == null && signal != null && signal.direction() != null) {
            var state = signalStates.get(symbol);
            if (state != null) {
                state = signals.invalidate(state, at); signalStates.put(symbol, state); signal = state.signal();
            } else signal = signal.invalidated();
            List<AssetCardSnapshot.RiskItem> items = new ArrayList<>(risk.items());
            items.removeIf(item -> "DATA".equals(item.type()));
            items.add(risks.evaluate(new AssetCardRiskService.Input(symbol, at, Map.of(), Map.of(), null,
                    false, true, null)).risk().items().stream().filter(item -> "DATA".equals(item.type())).findFirst().orElseThrow());
            risk = new AssetCardSnapshot.Risk("HIGH", items, at);
        }
        boolean priceChanged = !Objects.equals(price, current.spotPrice()) || !Objects.equals(priceAt, current.latestPriceAt());
        boolean signalChanged = !Objects.equals(signal, current.signal());
        boolean riskChanged = !Objects.equals(risk, current.risk());
        boolean healthChanged = current.health() == null || !Objects.equals(health.status(), current.health().status())
                || !Objects.equals(health.reason(), current.health().reason());
        if (!priceChanged && !signalChanged && !riskChanged && !healthChanged) return;
        long version = mapper.nextSnapshotVersion(symbol);
        var next = new AssetCardSnapshot(symbol, current.assetName(), price, priceAt, signal, risk, health,
                sameEffectiveSignal(signal, current.signal()) && sameEffectiveRisk(risk, current.risk()) ? current.cardAsOf() : at, version,
                current.featureVersion(), current.modelVersion(), current.calibrationVersion());
        persist(next);
        if (priceChanged) publish(next, "ASSET_CARD_PRICE", payload("spotPrice", price, "latestPriceAt", priceAt), at);
        if (signalChanged) publish(next, "ASSET_CARD_SIGNAL", payload("signal", signal, "cardAsOf", next.cardAsOf(),
                "featureVersion",next.featureVersion(),"modelVersion",next.modelVersion(),"calibrationVersion",next.calibrationVersion()), at);
        if (riskChanged) publish(next, "ASSET_CARD_RISK", payload("risk", risk, "cardAsOf", next.cardAsOf()), at);
        if (healthChanged) publish(next, "ASSET_CARD_HEALTH", payload("health", health), at);
    }

    private void persist(AssetCardSnapshot snapshot) {
        try {
            com.fasterxml.jackson.databind.node.ObjectNode stored = json.valueToTree(snapshot);
            var state = signalStates.get(snapshot.symbol());
            var frame = featureFrames.get(snapshot.symbol());
            if (state != null && frame != null) stored.set("_runtime", json.valueToTree(payload(
                    "state", state, "frame", frame, "signalFrame", signalFrames.get(snapshot.symbol()),
                    "modelMode", properties.getModelMode(), "dataKind", "LIVE_OBSERVED_CARD_INPUTS")));
            if (mapper.saveSnapshot(snapshot.symbol(), snapshot.snapshotVersion(), json.writeValueAsString(stored), snapshot.cardAsOf()) != 1)
                throw new IllegalStateException("ASSET_CARD_CONCURRENT_SNAPSHOT_REJECTED");
            snapshots.put(snapshot.symbol(), snapshot);
        } catch (JsonProcessingException failure) { throw new IllegalStateException("ASSET_CARD_SERIALIZATION_FAILED", failure); }
    }

    private void publish(AssetCardSnapshot snapshot, String type, Map<String, Object> values, Instant at) {
        values.put("symbol", snapshot.symbol());
        values.put("snapshotVersion", snapshot.snapshotVersion());
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
        var value = loadSnapshot(normalized, name);
        Instant now = Instant.now();
        if (failures.containsKey(normalized) || value.latestPriceAt() != null &&
                (value.latestPriceAt().isAfter(now) || Duration.between(value.latestPriceAt(), now).compareTo(properties.getPriceTtl()) > 0)) {
            var signal = value.signal() != null && value.signal().direction() != null ? value.signal().invalidated() : value.signal();
            return new AssetCardSnapshot(normalized, name, null, null, signal, value.risk(),
                    new AssetCardSnapshot.Health("SOURCE_UNAVAILABLE", failures.getOrDefault(normalized, "现货成交价格已过期"), null),
                    value.cardAsOf(), value.snapshotVersion(), value.featureVersion(), value.modelVersion(), value.calibrationVersion());
        }
        return new AssetCardSnapshot(normalized, name, value.spotPrice(), value.latestPriceAt(), value.signal(), value.risk(),
                value.health(), value.cardAsOf(), value.snapshotVersion(), value.featureVersion(), value.modelVersion(), value.calibrationVersion());
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
        return symbols.stream().map(symbol -> snapshot(symbol, names.get(symbol))).toList();
    }

    private static String normalize(String symbol) {
        if (symbol == null) throw new IllegalArgumentException("Card symbol required");
        String value = symbol.trim().toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z0-9]{2,32}")) throw new IllegalArgumentException("Invalid card symbol");
        return value;
    }
    @PreDestroy public void close() { started = false; background.shutdownNow(); inference.shutdownNow(); model.close(); }
}
