package org.example.trademodel.providercall.instrument;

import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final Map<Key, ProviderInstrumentCapability> observations = new ConcurrentHashMap<>();

    @Autowired
    public ProviderCapabilityRegistry(ProviderSymbolMappingRegistry mappingRegistry,
                                      Environment environment,
                                      @Value("${trade-model.instruments.capability-freshness-seconds:2592000}")
                                      long freshnessSeconds) {
        this(mappingRegistry, environment, freshnessSeconds, Clock.systemUTC());
    }

    ProviderCapabilityRegistry(ProviderSymbolMappingRegistry mappingRegistry,
                               Environment environment,
                               long freshnessSeconds,
                               Clock clock) {
        this.mappingRegistry = mappingRegistry;
        this.environment = environment;
        this.freshnessSeconds = Math.max(1L, freshnessSeconds);
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public List<ProviderInstrumentCapability> capabilities(String symbol, String timeframe) {
        String normalized = normalizeSymbol(symbol);
        String effectiveTimeframe = normalizeTimeframe(timeframe);
        Instant now = clock.instant();
        List<ProviderInstrumentCapability> result = new ArrayList<>();
        for (String provider : PROVIDER_ORDER) {
            ProviderInstrumentCapability observed = observations.get(new Key(provider, normalized, effectiveTimeframe));
            if (observed != null) {
                result.add(applyFreshness(observed, now));
                continue;
            }
            configured(provider, normalized, effectiveTimeframe, now).ifPresent(result::add);
        }
        if (result.isEmpty()) {
            result.add(unsupported(normalized, effectiveTimeframe, now));
        }
        return result.stream()
                .sorted(Comparator.comparingInt(value -> providerRank(value.provider())))
                .toList();
    }

    public ProviderInstrumentCapability best(String symbol, String timeframe) {
        return capabilities(symbol, timeframe).stream()
                .min(Comparator.comparingInt(value -> stateRank(value.capabilityState())))
                .orElse(null);
    }

    public void recordOhlcv(String provider,
                            String symbol,
                            String timeframe,
                            String providerSymbol,
                            String sourceVersion,
                            PublicOhlcvProviderResult result) {
        String normalizedProvider = normalizeProvider(provider);
        String normalizedSymbol = normalizeSymbol(symbol);
        String normalizedTimeframe = normalizeTimeframe(timeframe);
        Instant now = clock.instant();
        CanonicalInstrumentId instrument = exactSpotInstrument(normalizedProvider, normalizedSymbol);
        ProviderCapabilityState state = classify(result);
        String reason = result == null ? "PROVIDER_RESULT_MISSING" : result.reasonCode();
        String exactProviderSymbol = providerSymbol == null || providerSymbol.isBlank()
                ? normalizedSymbol : providerSymbol.trim().toUpperCase(Locale.ROOT);
        String exactSourceVersion = sourceVersion == null || sourceVersion.isBlank()
                ? normalizedProvider + "_RUNTIME_CAPABILITY_V1" : sourceVersion;
        observations.put(new Key(normalizedProvider, normalizedSymbol, normalizedTimeframe),
                new ProviderInstrumentCapability(
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
                        now));
    }

    private java.util.Optional<ProviderInstrumentCapability> configured(String provider,
                                                                         String symbol,
                                                                         String timeframe,
                                                                         Instant now) {
        ProviderSymbolMapping mapping;
        try {
            mapping = mappingRegistry.resolve(provider, symbol, MarketType.SPOT);
        } catch (RuntimeException absent) {
            return java.util.Optional.empty();
        }
        ProviderCapabilityState state;
        String reason = null;
        if (!providerEnabled(provider)) {
            state = ProviderCapabilityState.PROVIDER_DISABLED;
            reason = "PROVIDER_DISABLED";
        } else if (!mapping.supportedTimeframes().contains(timeframe)) {
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
                mapping.supportedTimeframes(), state, mapping.sourceVersion(), mapping.verifiedAt(), reason, now));
    }

    private ProviderInstrumentCapability applyFreshness(ProviderInstrumentCapability capability, Instant now) {
        if (capability.verifiedAt() == null
                || capability.capabilityState() != ProviderCapabilityState.SUPPORTED
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

    private ProviderInstrumentCapability unsupported(String symbol, String timeframe, Instant now) {
        CanonicalInstrumentId instrument = exactSpotInstrument("UNVERIFIED", symbol);
        return new ProviderInstrumentCapability(
                "UNVERIFIED", instrument.canonical(), instrument.baseAsset(), instrument.quoteAsset(),
                instrument.marketType(), instrument.contractType(), null, List.of(timeframe),
                ProviderCapabilityState.UNSUPPORTED_SYMBOL, "UNVERIFIED", null,
                "NO_EXACT_PROVIDER_MAPPING", now);
    }

    private boolean providerEnabled(String provider) {
        return switch (provider) {
            case "KRAKEN" -> booleanProperty("trade-model.ohlcv.kraken.enabled");
            case "BINANCE" -> booleanProperty("trade-model.ohlcv.binance.enabled")
                    || booleanProperty("trade-model.ohlcv.public-provider.enabled");
            case "COINGLASS" -> booleanProperty("trade-model.providers.coinglass.enabled");
            default -> false;
        };
    }

    private boolean booleanProperty(String key) {
        return environment != null && Boolean.parseBoolean(environment.getProperty(key, "false"));
    }

    private static ProviderCapabilityState classify(PublicOhlcvProviderResult result) {
        if (result == null) return ProviderCapabilityState.SOURCE_UNAVAILABLE;
        String reason = result.reasonCode() == null ? "" : result.reasonCode().toUpperCase(Locale.ROOT);
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

    private static int stateRank(ProviderCapabilityState state) {
        return switch (state) {
            case SUPPORTED -> 0;
            case REGION_RESTRICTED -> 1;
            case UNSUPPORTED_SYMBOL, UNSUPPORTED_TIMEFRAME -> 2;
            case PROVIDER_DISABLED, NOT_CONFIGURED -> 3;
            case STALE_CAPABILITY, SOURCE_UNAVAILABLE -> 4;
        };
    }

    private static int providerRank(String provider) {
        int index = PROVIDER_ORDER.indexOf(normalizeProvider(provider));
        return index < 0 ? Integer.MAX_VALUE : index;
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

    private record Key(String provider, String symbol, String timeframe) {
    }
}
