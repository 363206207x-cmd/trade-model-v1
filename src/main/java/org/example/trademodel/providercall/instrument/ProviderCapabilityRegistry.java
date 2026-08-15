package org.example.trademodel.providercall.instrument;

import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.example.trademodel.providercall.ProviderAdapterResponse;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderFailureClassifier;
import org.example.trademodel.providercall.ProviderRequestKey;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProviderCapabilityRegistry {
    private static final List<String> PROVIDER_ORDER = List.of("KRAKEN", "BINANCE");

    private final ProviderSymbolMappingRegistry mappingRegistry;
    private final Environment environment;
    private final long freshnessSeconds;
    private final Clock clock;
    private final ObjectProvider<ProviderCapabilityDirectory> directories;
    private final Map<Key, CapabilityObservation> observations = new ConcurrentHashMap<>();

    @Autowired
    public ProviderCapabilityRegistry(ProviderSymbolMappingRegistry mappingRegistry,
                                      Environment environment,
                                      @Value("${trade-model.instruments.capability-freshness-seconds:2592000}")
                                      long freshnessSeconds,
                                      ObjectProvider<ProviderCapabilityDirectory> directories) {
        this(mappingRegistry, environment, freshnessSeconds, Clock.systemUTC(), directories);
    }

    ProviderCapabilityRegistry(ProviderSymbolMappingRegistry mappingRegistry,
                               Environment environment,
                               long freshnessSeconds,
                               Clock clock) {
        this(mappingRegistry, environment, freshnessSeconds, clock, null);
    }

    ProviderCapabilityRegistry(ProviderSymbolMappingRegistry mappingRegistry,
                               Environment environment,
                               long freshnessSeconds,
                               Clock clock,
                               ObjectProvider<ProviderCapabilityDirectory> directories) {
        this.mappingRegistry = mappingRegistry;
        this.environment = environment;
        this.freshnessSeconds = Math.max(1L, freshnessSeconds);
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.directories = directories;
    }

    public List<ProviderInstrumentCapability> capabilities(String symbol, String timeframe) {
        String normalized = normalizeSymbol(symbol);
        String effectiveTimeframe = normalizeTimeframe(timeframe);
        Instant now = clock.instant();
        List<ProviderInstrumentCapability> result = new ArrayList<>();
        for (String provider : PROVIDER_ORDER) {
            result.add(inspect(provider, normalized, effectiveTimeframe, MarketType.SPOT, ContractType.NONE,
                    ProviderDatasetType.OHLCV));
        }
        return result.stream()
                .sorted(Comparator.comparingInt(value -> providerRank(value.provider())))
                .toList();
    }

    public ProviderInstrumentCapability best(String symbol, String timeframe) {
        return capabilities(symbol, timeframe).stream()
                .min(Comparator.comparingInt(ProviderCapabilityRegistry::identityRank)
                        .thenComparingInt(value -> stateRank(value.capabilityState())))
                .orElse(null);
    }

    /** No-call capability inspection. This method never invokes a provider directory. */
    public ProviderInstrumentCapability inspect(String provider,
                                                String symbol,
                                                String timeframe,
                                                MarketType marketType,
                                                ContractType contractType) {
        return inspect(provider, symbol, timeframe, marketType, contractType,
                defaultDataset(timeframe, marketType));
    }

    public ProviderInstrumentCapability inspect(String provider,
                                                String symbol,
                                                String timeframe,
                                                MarketType marketType,
                                                ContractType contractType,
                                                ProviderDatasetType datasetType) {
        String normalizedProvider = normalizeProvider(provider);
        String normalizedSymbol = normalizeSymbol(symbol);
        String normalizedTimeframe = normalizeTimeframe(timeframe);
        ProviderDatasetType normalizedDataset = java.util.Objects.requireNonNull(datasetType, "datasetType");
        Instant now = clock.instant();
        Key key = new Key(normalizedProvider, normalizedSymbol, normalizedTimeframe, marketType, contractType,
                normalizedDataset);
        CapabilityObservation observation = observations.get(key);
        ProviderInstrumentCapability observed = observation == null ? null : observation.capability();
        if (observed != null) {
            return applyFreshness(observed, now);
        }
        return configured(normalizedProvider, normalizedSymbol, normalizedTimeframe,
                marketType, contractType, normalizedDataset, now).orElseGet(() -> unsupported(
                normalizedProvider, normalizedSymbol, normalizedTimeframe, marketType, contractType, now));
    }

    /**
     * Unified pre-call gate. A market-data adapter may call its provider only when this method returns
     * `SUPPORTED`. Stale or previously unknown identities are revalidated only through a provider
     * directory implementation; the gate itself never probes an OHLCV or quote endpoint.
     */
    public ProviderInstrumentCapability authorize(String provider,
                                                  String symbol,
                                                  String timeframe,
                                                  MarketType marketType,
                                                  ContractType contractType) {
        return authorize(provider, symbol, timeframe, marketType, contractType,
                defaultDataset(timeframe, marketType));
    }

    public ProviderInstrumentCapability authorize(String provider,
                                                  String symbol,
                                                  String timeframe,
                                                  MarketType marketType,
                                                  ContractType contractType,
                                                  ProviderDatasetType datasetType) {
        String normalizedProvider = normalizeProvider(provider);
        String normalizedSymbol = normalizeSymbol(symbol);
        String normalizedTimeframe = normalizeTimeframe(timeframe);
        ProviderDatasetType normalizedDataset = java.util.Objects.requireNonNull(datasetType, "datasetType");
        Instant now = clock.instant();
        CanonicalInstrumentId requested = requestedInstrument(normalizedProvider, normalizedSymbol,
                marketType, contractType);
        if (!providerEnabled(normalizedProvider, normalizedTimeframe, normalizedDataset)) {
            return blocked(requested, normalizedTimeframe, ProviderCapabilityState.PROVIDER_DISABLED,
                    "PROVIDER_DISABLED", now);
        }
        if (!providerExternalCallsEnabled(normalizedProvider, normalizedTimeframe, normalizedDataset)) {
            return blocked(requested, normalizedTimeframe, ProviderCapabilityState.NOT_CONFIGURED,
                    "PROVIDER_EXTERNAL_CALLS_NOT_CONFIGURED", now);
        }

        Key key = new Key(normalizedProvider, normalizedSymbol, normalizedTimeframe, marketType, contractType,
                normalizedDataset);
        boolean observedBefore = observations.containsKey(key);
        ProviderInstrumentCapability current = inspect(normalizedProvider, normalizedSymbol,
                normalizedTimeframe, marketType, contractType, normalizedDataset);
        if (current.capabilityState() == ProviderCapabilityState.SUPPORTED
                || current.capabilityState() == ProviderCapabilityState.UNSUPPORTED_TIMEFRAME
                || current.capabilityState() == ProviderCapabilityState.REGION_RESTRICTED
                || current.capabilityState() == ProviderCapabilityState.PROVIDER_DISABLED
                || current.capabilityState() == ProviderCapabilityState.NOT_CONFIGURED
                || (observedBefore && current.capabilityState() == ProviderCapabilityState.UNSUPPORTED_SYMBOL)) {
            return current;
        }
        if (observedBefore && current.capabilityState() == ProviderCapabilityState.SOURCE_UNAVAILABLE) {
            return current;
        }

        ProviderCapabilityDirectory directory = directory(normalizedProvider);
        if (directory == null) {
            return current;
        }
        ProviderInstrumentCapability verified;
        try {
            verified = directory.verify(requested, normalizedTimeframe, now);
        } catch (RuntimeException failure) {
            verified = blocked(requested, normalizedTimeframe, ProviderCapabilityState.SOURCE_UNAVAILABLE,
                    "CAPABILITY_DIRECTORY_UNAVAILABLE", now);
        }
        ProviderInstrumentCapability exact = exactOrBlocked(requested, normalizedTimeframe,
                normalizedDataset, verified, now);
        observations.put(key, new CapabilityObservation(exact, null, now.plusSeconds(freshnessSeconds)));
        return exact;
    }

    public void recordOhlcv(String provider,
                            String symbol,
                            String timeframe,
                            String providerSymbol,
                            String sourceVersion,
                            PublicOhlcvProviderResult result) {
        recordOhlcv(provider, symbol, timeframe, providerSymbol, sourceVersion, result, null);
    }

    public void recordOhlcv(String provider,
                            String symbol,
                            String timeframe,
                            String providerSymbol,
                            String sourceVersion,
                            PublicOhlcvProviderResult result,
                            String traceId) {
        String normalizedProvider = normalizeProvider(provider);
        String normalizedSymbol = normalizeSymbol(symbol);
        String normalizedTimeframe = normalizeTimeframe(timeframe);
        Instant now = clock.instant();
        CanonicalInstrumentId instrument = exactSpotInstrument(normalizedProvider, normalizedSymbol);
        ProviderCapabilityState state = classify(result);
        String reason = result == null ? "PROVIDER_RESULT_MISSING" : result.reasonCode();
        String exactProviderSymbol = providerSymbol == null || providerSymbol.isBlank()
                ? null : providerSymbol.trim().toUpperCase(Locale.ROOT);
        String exactSourceVersion = sourceVersion == null || sourceVersion.isBlank()
                ? normalizedProvider + "_RUNTIME_CAPABILITY_V1" : sourceVersion;
        ProviderInstrumentCapability capability = new ProviderInstrumentCapability(
                        normalizedProvider,
                        instrument.canonical(),
                        instrument.baseAsset(),
                        instrument.quoteAsset(),
                        instrument.marketType(),
                        instrument.contractType(),
                        exactProviderSymbol,
                        List.of(normalizedTimeframe),
                        state,
                        exactSourceVersion,
                        conclusive(state) ? now : null,
                        state == ProviderCapabilityState.SUPPORTED ? null : reason,
                        now);
        observations.put(new Key(normalizedProvider, normalizedSymbol, normalizedTimeframe,
                        MarketType.SPOT, ContractType.NONE, ProviderDatasetType.OHLCV),
                new CapabilityObservation(capability, traceId, now.plusSeconds(freshnessSeconds)));
    }

    public void record(ProviderRequestKey requestKey,
                       ProviderAdapterResponse<?> response,
                       String traceId) {
        if (requestKey == null) throw new IllegalArgumentException("requestKey is required");
        String provider = normalizeProvider(requestKey.provider());
        CanonicalInstrumentId instrument = requestKey.canonicalInstrumentId();
        String symbol = normalizeSymbol(instrument.baseAsset() + instrument.quoteAsset());
        String timeframe = normalizeTimeframe(requestKey.timeframe());
        ProviderCapabilityState state = classify(response);
        String reason = response == null ? "PROVIDER_RESULT_MISSING"
                : ProviderFailureClassifier.canonicalReason(response.httpStatus(), response.reasonCode());
        Instant now = clock.instant();
        ProviderInstrumentCapability capability = new ProviderInstrumentCapability(
                provider, instrument.canonical(), instrument.baseAsset(), instrument.quoteAsset(),
                instrument.marketType(), instrument.contractType(), requestKey.providerSymbol(),
                "GLOBAL".equals(timeframe) ? List.of("GLOBAL") : List.of(timeframe), state,
                requestKey.sourceVersion(), conclusive(state) ? now : null,
                state == ProviderCapabilityState.SUPPORTED ? null : reason, now);
        observations.put(new Key(provider, symbol, timeframe, instrument.marketType(),
                        instrument.contractType(), requestKey.datasetType()),
                new CapabilityObservation(capability, traceId, now.plusSeconds(freshnessSeconds)));
    }

    private java.util.Optional<ProviderInstrumentCapability> configured(String provider,
                                                                         String symbol,
                                                                         String timeframe,
                                                                         MarketType marketType,
                                                                         ContractType contractType,
                                                                         ProviderDatasetType datasetType,
                                                                         Instant now) {
        java.util.Optional<ProviderSymbolMapping> configured = mappingRegistry.findExact(
                provider, symbol, marketType, contractType);
        if (configured.isEmpty()) return java.util.Optional.empty();
        ProviderSymbolMapping mapping = configured.get();
        ProviderCapabilityState state;
        String reason = null;
        if (!providerEnabled(provider, timeframe, datasetType)) {
            state = ProviderCapabilityState.PROVIDER_DISABLED;
            reason = "PROVIDER_DISABLED";
        } else if (!providerExternalCallsEnabled(provider, timeframe, datasetType)) {
            state = ProviderCapabilityState.NOT_CONFIGURED;
            reason = "PROVIDER_EXTERNAL_CALLS_NOT_CONFIGURED";
        } else if (datasetType == ProviderDatasetType.OHLCV
                && !"GLOBAL".equals(timeframe) && !mapping.supportedTimeframes().contains(timeframe)) {
            state = ProviderCapabilityState.UNSUPPORTED_TIMEFRAME;
            reason = "UNSUPPORTED_TIMEFRAME";
        } else if (mapping.verifiedAt() == null
                || now.isAfter(mapping.verifiedAt().plusSeconds(freshnessSeconds))) {
            state = ProviderCapabilityState.STALE_CAPABILITY;
            reason = mapping.verifiedAt() == null ? "CAPABILITY_NOT_VERIFIED" : "CAPABILITY_VERIFICATION_STALE";
        } else {
            state = ProviderCapabilityState.SUPPORTED;
        }
        CanonicalInstrumentId instrument = mapping.canonicalInstrumentId();
        return java.util.Optional.of(new ProviderInstrumentCapability(
                mapping.provider(), instrument.canonical(), instrument.baseAsset(), instrument.quoteAsset(),
                instrument.marketType(), instrument.contractType(), mapping.providerSymbol(),
                datasetType == ProviderDatasetType.OHLCV
                        ? mapping.supportedTimeframes() : List.of(timeframe),
                state, mapping.sourceVersion(), mapping.verifiedAt(), reason, now));
    }

    private ProviderInstrumentCapability applyFreshness(ProviderInstrumentCapability capability, Instant now) {
        if (capability.verifiedAt() == null
                || (capability.capabilityState() != ProviderCapabilityState.SUPPORTED
                && capability.capabilityState() != ProviderCapabilityState.REGION_RESTRICTED)
                || !now.isAfter(capability.verifiedAt().plusSeconds(freshnessSeconds))) {
            return capability;
        }
        return new ProviderInstrumentCapability(
                capability.provider(), capability.canonicalAssetId(), capability.baseAsset(),
                capability.quoteAsset(), capability.marketType(), capability.contractType(),
                capability.providerSymbol(), capability.supportedTimeframes(),
                ProviderCapabilityState.STALE_CAPABILITY, capability.sourceVersion(),
                capability.verifiedAt(), "CAPABILITY_VERIFICATION_STALE", now);
    }

    private ProviderInstrumentCapability unsupported(String provider,
                                                      String symbol,
                                                      String timeframe,
                                                      MarketType marketType,
                                                      ContractType contractType,
                                                      Instant now) {
        CanonicalInstrumentId instrument = requestedInstrument(provider, symbol, marketType, contractType);
        return new ProviderInstrumentCapability(
                provider, instrument.canonical(), instrument.baseAsset(), instrument.quoteAsset(),
                instrument.marketType(), instrument.contractType(), null, List.of(timeframe),
                ProviderCapabilityState.UNSUPPORTED_SYMBOL, "UNVERIFIED", null,
                "NO_EXACT_PROVIDER_MAPPING", now);
    }

    private boolean providerEnabled(String provider, String timeframe, ProviderDatasetType datasetType) {
        return switch (provider) {
            case "KRAKEN" -> booleanProperty("trade-model.ohlcv.kraken.enabled");
            case "BINANCE" -> "GLOBAL".equals(timeframe)
                    ? booleanProperty("trade-model.provider-call.enabled")
                    : booleanProperty("trade-model.ohlcv.binance.enabled")
                    || booleanProperty("trade-model.ohlcv.public-provider.enabled");
            case "COINGLASS" -> booleanProperty("trade-model.providers.coinglass.enabled");
            default -> false;
        };
    }

    private boolean providerExternalCallsEnabled(String provider, String timeframe, ProviderDatasetType datasetType) {
        return switch (provider) {
            case "KRAKEN" -> booleanProperty("trade-model.ohlcv.kraken.external-calls-enabled");
            case "BINANCE" -> "GLOBAL".equals(timeframe)
                    ? booleanProperty("trade-model.provider-call.external-calls-enabled")
                    : booleanProperty("trade-model.ohlcv.binance.external-calls-enabled")
                    || booleanProperty("trade-model.ohlcv.public-provider.external-calls-enabled");
            case "COINGLASS" -> booleanProperty("trade-model.providers.coinglass.external-calls-enabled");
            default -> false;
        };
    }

    private boolean booleanProperty(String key) {
        return environment != null && Boolean.parseBoolean(environment.getProperty(key, "false"));
    }

    private static ProviderCapabilityState classify(PublicOhlcvProviderResult result) {
        if (result == null) return ProviderCapabilityState.SOURCE_UNAVAILABLE;
        String reason = ProviderFailureClassifier.canonicalReason(0, result.reasonCode());
        if (result.sourceState() == OhlcvSourceState.READY && result.batch() != null) {
            return ProviderCapabilityState.SUPPORTED;
        }
        if (reason.contains("REGION_RESTRICTED") || reason.contains("HTTP_451")) {
            return ProviderCapabilityState.REGION_RESTRICTED;
        }
        if (reason.contains("PAIR_NOT_SUPPORTED") || reason.contains("UNKNOWN_PAIR")) {
            return ProviderCapabilityState.UNSUPPORTED_SYMBOL;
        }
        if (reason.contains("TIMEFRAME") || reason.contains("INTERVAL")) {
            return ProviderCapabilityState.UNSUPPORTED_TIMEFRAME;
        }
        if (result.sourceState() == OhlcvSourceState.DISABLED) {
            return ProviderCapabilityState.PROVIDER_DISABLED;
        }
        if (result.sourceState() == OhlcvSourceState.NOT_CONFIGURED) {
            return ProviderCapabilityState.NOT_CONFIGURED;
        }
        return ProviderCapabilityState.SOURCE_UNAVAILABLE;
    }

    private static ProviderCapabilityState classify(ProviderAdapterResponse<?> result) {
        if (result == null) return ProviderCapabilityState.SOURCE_UNAVAILABLE;
        String reason = ProviderFailureClassifier.canonicalReason(result.httpStatus(), result.reasonCode());
        if (result.ready() || result.sourceStatus() == UnifiedSourceStatus.EMPTY_CONFIRMED) {
            return ProviderCapabilityState.SUPPORTED;
        }
        if (ProviderFailureClassifier.isRegionRestricted(result)) {
            return ProviderCapabilityState.REGION_RESTRICTED;
        }
        if (reason.contains("PAIR_NOT_SUPPORTED") || reason.contains("UNKNOWN_PAIR")
                || reason.contains("SYMBOL_UNSUPPORTED")) {
            return ProviderCapabilityState.UNSUPPORTED_SYMBOL;
        }
        if (reason.contains("TIMEFRAME") || reason.contains("INTERVAL")) {
            return ProviderCapabilityState.UNSUPPORTED_TIMEFRAME;
        }
        if (result.sourceStatus() == UnifiedSourceStatus.DISABLED) {
            return ProviderCapabilityState.PROVIDER_DISABLED;
        }
        if (result.sourceStatus() == UnifiedSourceStatus.NOT_CONFIGURED) {
            return ProviderCapabilityState.NOT_CONFIGURED;
        }
        return ProviderCapabilityState.SOURCE_UNAVAILABLE;
    }

    private static boolean conclusive(ProviderCapabilityState state) {
        return state == ProviderCapabilityState.SUPPORTED
                || state == ProviderCapabilityState.UNSUPPORTED_SYMBOL
                || state == ProviderCapabilityState.UNSUPPORTED_TIMEFRAME
                || state == ProviderCapabilityState.REGION_RESTRICTED;
    }

    private static CanonicalInstrumentId exactSpotInstrument(String provider, String symbol) {
        if (!symbol.endsWith("USDT") || symbol.length() <= 4) {
            throw new IllegalArgumentException("EXACT_USDT_INSTRUMENT_REQUIRED");
        }
        return new CanonicalInstrumentId(symbol.substring(0, symbol.length() - 4), "USDT",
                MarketType.SPOT, provider, ContractType.NONE);
    }

    private ProviderCapabilityDirectory directory(String provider) {
        if (directories == null) return null;
        return directories.orderedStream()
                .filter(value -> provider.equals(normalizeProvider(value.provider())))
                .findFirst().orElse(null);
    }

    private static ProviderInstrumentCapability exactOrBlocked(CanonicalInstrumentId requested,
                                                               String timeframe,
                                                               ProviderDatasetType datasetType,
                                                               ProviderInstrumentCapability verified,
                                                               Instant now) {
        if (verified == null) {
            return blocked(requested, timeframe, ProviderCapabilityState.SOURCE_UNAVAILABLE,
                    "CAPABILITY_DIRECTORY_RESULT_MISSING", now);
        }
        boolean identityMatches = requested.baseAsset().equals(verified.baseAsset())
                && requested.quoteAsset().equals(verified.quoteAsset())
                && requested.marketType() == verified.marketType()
                && requested.contractType() == verified.contractType()
                && requested.venue().equalsIgnoreCase(verified.provider());
        if (!identityMatches || (verified.capabilityState() == ProviderCapabilityState.SUPPORTED
                && (verified.providerSymbol() == null || verified.providerSymbol().isBlank()))) {
            return blocked(requested, timeframe, ProviderCapabilityState.UNSUPPORTED_SYMBOL,
                    "CAPABILITY_EXACT_IDENTITY_MISMATCH", now);
        }
        if (datasetType == ProviderDatasetType.OHLCV
                && verified.capabilityState() == ProviderCapabilityState.SUPPORTED
                && !"GLOBAL".equals(timeframe)
                && !verified.supportedTimeframes().contains(timeframe)) {
            return blocked(requested, timeframe, ProviderCapabilityState.UNSUPPORTED_TIMEFRAME,
                    "UNSUPPORTED_TIMEFRAME", now);
        }
        return verified;
    }

    private static ProviderInstrumentCapability blocked(CanonicalInstrumentId instrument,
                                                        String timeframe,
                                                        ProviderCapabilityState state,
                                                        String reason,
                                                        Instant now) {
        return new ProviderInstrumentCapability(instrument.venue(), instrument.canonical(),
                instrument.baseAsset(), instrument.quoteAsset(), instrument.marketType(), instrument.contractType(),
                null, List.of(timeframe), state, instrument.venue() + "_CAPABILITY_V1",
                conclusive(state) ? now : null, reason, now);
    }

    private static CanonicalInstrumentId requestedInstrument(String provider,
                                                             String symbol,
                                                             MarketType marketType,
                                                             ContractType contractType) {
        if (!symbol.endsWith("USDT") || symbol.length() <= 4) {
            throw new IllegalArgumentException("EXACT_USDT_INSTRUMENT_REQUIRED");
        }
        return new CanonicalInstrumentId(symbol.substring(0, symbol.length() - 4), "USDT",
                marketType, provider, contractType);
    }

    private static int stateRank(ProviderCapabilityState state) {
        return switch (state) {
            case SUPPORTED -> 0;
            case REGION_RESTRICTED -> 1;
            case UNSUPPORTED_TIMEFRAME -> 2;
            case UNSUPPORTED_SYMBOL -> 3;
            case PROVIDER_DISABLED, NOT_CONFIGURED -> 4;
            case STALE_CAPABILITY, SOURCE_UNAVAILABLE -> 5;
        };
    }

    private static int providerRank(String provider) {
        int index = PROVIDER_ORDER.indexOf(normalizeProvider(provider));
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static int identityRank(ProviderInstrumentCapability capability) {
        return capability == null || capability.sourceVersion() == null
                || "UNVERIFIED".equalsIgnoreCase(capability.sourceVersion()) ? 1 : 0;
    }

    private static String normalizeProvider(String value) {
        return value == null || value.isBlank() ? "UNVERIFIED" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeSymbol(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("symbol is required");
        return value.trim().toUpperCase(Locale.ROOT).replace("/", "").replace("-", "").replace("_", "");
    }

    private static String normalizeTimeframe(String value) {
        return value == null || value.isBlank() ? "5m" : value.trim();
    }

    private static ProviderDatasetType defaultDataset(String timeframe, MarketType marketType) {
        if (timeframe != null && !"GLOBAL".equalsIgnoreCase(timeframe.trim())) {
            return ProviderDatasetType.OHLCV;
        }
        return marketType == MarketType.PERPETUAL
                ? ProviderDatasetType.DERIVATIVES : ProviderDatasetType.PRICE;
    }

    private record Key(String provider, String symbol, String timeframe,
                       MarketType marketType, ContractType contractType, ProviderDatasetType datasetType) {
    }

    private record CapabilityObservation(ProviderInstrumentCapability capability,
                                         String traceId,
                                         Instant reverifyAfter) {
    }
}
