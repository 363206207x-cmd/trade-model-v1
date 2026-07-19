package org.example.trademodel.providercall;

import org.example.trademodel.providercall.scan.AssetPriorityResolver;
import org.example.trademodel.providercall.scan.PositionScanAsset;
import org.example.trademodel.providercall.scan.ScanPlanItem;
import org.example.trademodel.providercall.scan.ScanUniverseInput;
import org.example.trademodel.providercall.scan.ScanUniverseResolver;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ProviderSymbolMappingRegistry;
import org.example.trademodel.providercall.profile.FrequencyMatrixVersionService;
import org.example.trademodel.providercall.profile.ProviderCallProfileResolver;
import org.example.trademodel.providercall.profile.ProviderDueTimePolicy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScanUniverseResolverTest {
    private static final Instant NOW = Instant.parse("2026-07-10T10:00:00Z");
    private final ProviderCallProperties properties = new ProviderCallProperties();
    private final ProviderSymbolMappingRegistry mappingRegistry = ProviderCallTestFixtures.binanceRegistry(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT", "ADAUSDT");
    private final ScanUniverseResolver resolver = new ScanUniverseResolver(properties,
            new AssetPriorityResolver(mappingRegistry), new ProviderCallProfileResolver(),
            new ProviderDueTimePolicy(properties), new FrequencyMatrixVersionService(properties));

    @Test
    void configuredWatchlistIsReplaceableAndNotFixedToSixAssets() {
        List<ScanPlanItem> plan = resolver.resolve(input(
                List.of("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT", "ADAUSDT"),
                List.of(), List.of(), List.of(), UserScanProfile.STANDARD, RuntimeScanProfile.LOW, Map.of(), Map.of()));

        assertThat(plan).hasSize(7);
        assertThat(plan).allMatch(item -> item.effectivePriority() == AssetPriority.P1_WATCHLIST);
    }

    @Test
    void positionAndCoreDuplicateSymbolScansOnce() {
        List<ScanPlanItem> plan = resolver.resolve(input(List.of("BTCUSDT"),
                List.of(position("BTCUSDT", "OPEN")), List.of("BTCUSDT"), List.of("BTCUSDT"),
                UserScanProfile.STANDARD, RuntimeScanProfile.LOW, Map.of(), Map.of()));

        assertThat(plan).singleElement().satisfies(item ->
                assertThat(item.effectivePriority()).isEqualTo(AssetPriority.P0_POSITION));
    }

    @Test
    void positionPriorityOverridesAllOtherPriorities() {
        ScanPlanItem item = resolver.resolve(input(List.of("ETHUSDT"),
                List.of(position("ETHUSDT", "PARTIALLY_CLOSED")), List.of("ETHUSDT"),
                List.of("ETHUSDT"), UserScanProfile.LOW, RuntimeScanProfile.LOW, Map.of(), Map.of())).get(0);
        assertThat(item.effectivePriority()).isEqualTo(AssetPriority.P0_POSITION);
        assertThat(item.profileReasonCodes()).contains("ACTIVE_POSITION");
    }

    @Test
    void candidatePriorityOverridesPool() {
        ScanPlanItem item = resolver.resolve(input(List.of(), List.of(), List.of("SOLUSDT"), List.of("SOLUSDT"),
                UserScanProfile.LOW, RuntimeScanProfile.LOW, Map.of(), Map.of())).get(0);
        assertThat(item.effectivePriority()).isEqualTo(AssetPriority.P2_CANDIDATE);
    }

    @Test
    void lowGlobalProfileKeepsOpenPositionAtSafetyFloor() {
        Map<ScanUniverseInput.DatasetRefreshKey, Instant> last = Map.of(
                new ScanUniverseInput.DatasetRefreshKey(perpetual("BTCUSDT"), ProviderDatasetType.PRICE), NOW);
        ScanPlanItem item = resolver.resolve(input(List.of(), List.of(position("BTCUSDT", "OPEN")),
                List.of(), List.of(), UserScanProfile.LOW, RuntimeScanProfile.LOW, Map.of(), last)).get(0);
        assertThat(item.priceDueAt()).isEqualTo(NOW.plusSeconds(15));
        assertThat(item.effectiveProfile()).isEqualTo(RuntimeScanProfile.LOW);
    }

    @Test
    void manualHighProfileIsNotAutoDowngraded() {
        ScanPlanItem item = resolver.resolve(input(List.of("BTCUSDT"), List.of(), List.of(), List.of(),
                UserScanProfile.HIGH, RuntimeScanProfile.LOW, Map.of(), Map.of())).get(0);
        assertThat(item.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
    }

    @Test
    void autoModeEscalatesStandardToHigh() {
        ScanPlanItem item = resolver.resolve(input(List.of("BTCUSDT"), List.of(), List.of(), List.of(),
                UserScanProfile.AUTO, RuntimeScanProfile.HIGH, Map.of(), Map.of())).get(0);
        assertThat(item.effectiveProfile()).isEqualTo(RuntimeScanProfile.HIGH);
    }

    @Test
    void hotResetEscalatesOnlyAffectedSymbolsToEmergency() {
        List<ScanPlanItem> plan = resolver.resolve(input(List.of("BTCUSDT", "ETHUSDT"), List.of(), List.of(), List.of(),
                UserScanProfile.STANDARD, RuntimeScanProfile.STANDARD,
                Map.of("BTCUSDT", RuntimeScanProfile.EMERGENCY), Map.of()));
        assertThat(find(plan, "BTCUSDT").effectiveProfile()).isEqualTo(RuntimeScanProfile.EMERGENCY);
        assertThat(find(plan, "ETHUSDT").effectiveProfile()).isEqualTo(RuntimeScanProfile.STANDARD);
    }

    @Test
    void emergencyDoesNotAccelerateEntirePool() {
        List<ScanPlanItem> plan = resolver.resolve(input(List.of("BTCUSDT"), List.of(), List.of(), List.of("ADAUSDT"),
                UserScanProfile.LOW, RuntimeScanProfile.LOW,
                Map.of("BTCUSDT", RuntimeScanProfile.EMERGENCY), Map.of()));
        assertThat(find(plan, "BTCUSDT").effectiveProfile()).isEqualTo(RuntimeScanProfile.EMERGENCY);
        assertThat(find(plan, "ADAUSDT").effectiveProfile()).isEqualTo(RuntimeScanProfile.LOW);
    }

    @Test
    void closedPositionLosesHighFrequencySafetyFloor() {
        ScanPlanItem item = resolver.resolve(input(List.of(), List.of(position("BTCUSDT", "CLOSED")),
                List.of(), List.of("BTCUSDT"), UserScanProfile.LOW, RuntimeScanProfile.LOW, Map.of(), Map.of())).get(0);
        assertThat(item.effectivePriority()).isEqualTo(AssetPriority.P3_DISCOVERY);
    }

    @Test
    void positionPriceCanRefreshEveryConfiguredFiveSeconds() {
        Map<ScanUniverseInput.DatasetRefreshKey, Instant> last = Map.of(
                new ScanUniverseInput.DatasetRefreshKey(perpetual("BTCUSDT"), ProviderDatasetType.PRICE), NOW);
        ScanPlanItem item = resolver.resolve(input(List.of(), List.of(position("BTCUSDT", "OPEN")),
                List.of(), List.of(), UserScanProfile.HIGH, RuntimeScanProfile.LOW, Map.of(), last)).get(0);
        assertThat(item.priceDueAt()).isEqualTo(NOW.plusSeconds(5));
    }

    @Test
    void priceRefreshDoesNotRefreshDerivatives() {
        Map<ScanUniverseInput.DatasetRefreshKey, Instant> last = Map.of(
                new ScanUniverseInput.DatasetRefreshKey(perpetual("BTCUSDT"), ProviderDatasetType.PRICE), NOW.minusSeconds(6),
                new ScanUniverseInput.DatasetRefreshKey(perpetual("BTCUSDT"), ProviderDatasetType.DERIVATIVES), NOW.minusSeconds(6));
        ScanPlanItem item = resolver.resolve(input(List.of(), List.of(position("BTCUSDT", "OPEN")),
                List.of(), List.of(), UserScanProfile.HIGH, RuntimeScanProfile.LOW, Map.of(), last)).get(0);
        assertThat(item.dueDatasets()).contains(ProviderDatasetType.PRICE).doesNotContain(ProviderDatasetType.DERIVATIVES);
    }

    @Test
    void everyScanPlanCarriesStableFrequencyMatrixVersion() {
        List<ScanPlanItem> first = resolver.resolve(input(List.of("BTCUSDT", "ETHUSDT"),
                List.of(), List.of(), List.of(), UserScanProfile.STANDARD,
                RuntimeScanProfile.LOW, Map.of(), Map.of()));
        List<ScanPlanItem> second = resolver.resolve(input(List.of("BTCUSDT", "ETHUSDT"),
                List.of(), List.of(), List.of(), UserScanProfile.STANDARD,
                RuntimeScanProfile.LOW, Map.of(), Map.of()));

        assertThat(first).allSatisfy(item -> assertThat(item.frequencyMatrixVersion()).startsWith("FM-"));
        assertThat(first).extracting(ScanPlanItem::frequencyMatrixVersion)
                .containsExactlyElementsOf(second.stream().map(ScanPlanItem::frequencyMatrixVersion).toList());
    }

    private ScanUniverseInput input(List<String> core, List<PositionScanAsset> positions, List<String> candidate,
                                    List<String> pool, UserScanProfile base, RuntimeScanProfile auto,
                                    Map<String, RuntimeScanProfile> escalations,
                                    Map<?, ?> rawLast) {
        @SuppressWarnings("unchecked")
        Map<ScanUniverseInput.DatasetRefreshKey, Instant> last = (Map<ScanUniverseInput.DatasetRefreshKey, Instant>) rawLast;
        Map<CanonicalInstrumentId, RuntimeScanProfile> canonicalEscalations = escalations.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(entry -> perpetual(entry.getKey()), Map.Entry::getValue));
        return new ScanUniverseInput(instruments(core), positions, instruments(candidate), instruments(pool),
                base, auto, canonicalEscalations, Map.of(), last, NOW);
    }

    private static ScanPlanItem find(List<ScanPlanItem> plan, String symbol) {
        return plan.stream().filter(item -> symbol.equals(item.symbol())).findFirst().orElseThrow();
    }

    private static PositionScanAsset position(String symbol, String status) {
        return new PositionScanAsset(perpetual(symbol), symbol, status);
    }

    private static List<CanonicalInstrumentId> instruments(List<String> symbols) {
        return symbols.stream().map(ScanUniverseResolverTest::perpetual).toList();
    }

    private static CanonicalInstrumentId perpetual(String symbol) {
        return ProviderCallTestFixtures.perpetual(symbol);
    }
}
