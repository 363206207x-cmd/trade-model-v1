package org.example.trademodel.providercall;

import org.example.trademodel.providercall.scan.AssetPriorityResolver;
import org.example.trademodel.providercall.scan.PositionScanAsset;
import org.example.trademodel.providercall.scan.ScanPlanItem;
import org.example.trademodel.providercall.scan.ScanUniverseInput;
import org.example.trademodel.providercall.scan.ScanUniverseResolver;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScanUniverseResolverTest {
    private static final Instant NOW = Instant.parse("2026-07-10T10:00:00Z");
    private final ProviderCallProperties properties = new ProviderCallProperties();
    private final ScanUniverseResolver resolver = new ScanUniverseResolver(properties, new AssetPriorityResolver());

    @Test
    void sixCoreAssetsSupportedWithoutUnboundedScan() {
        List<ScanPlanItem> plan = resolver.resolve(input(
                List.of("BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT", "ADAUSDT"),
                List.of(), List.of(), List.of(), UserScanProfile.STANDARD, RuntimeScanProfile.LOW, Map.of(), Map.of()));

        assertThat(plan).hasSize(6);
        assertThat(plan).allMatch(item -> item.effectivePriority() == AssetPriority.P1_CORE);
    }

    @Test
    void positionAndCoreDuplicateSymbolScansOnce() {
        List<ScanPlanItem> plan = resolver.resolve(input(List.of("BTCUSDT"),
                List.of(new PositionScanAsset("BTCUSDT", "OPEN")), List.of("BTCUSDT"), List.of("BTCUSDT"),
                UserScanProfile.STANDARD, RuntimeScanProfile.LOW, Map.of(), Map.of()));

        assertThat(plan).singleElement().satisfies(item ->
                assertThat(item.effectivePriority()).isEqualTo(AssetPriority.P0_POSITION));
    }

    @Test
    void positionPriorityOverridesAllOtherPriorities() {
        ScanPlanItem item = resolver.resolve(input(List.of("ETHUSDT"),
                List.of(new PositionScanAsset("ETHUSDT", "PARTIALLY_CLOSED")), List.of("ETHUSDT"),
                List.of("ETHUSDT"), UserScanProfile.LOW, RuntimeScanProfile.LOW, Map.of(), Map.of())).get(0);
        assertThat(item.effectivePriority()).isEqualTo(AssetPriority.P0_POSITION);
        assertThat(item.escalationReason()).isEqualTo("ACTIVE_POSITION_SAFETY_FLOOR");
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
                new ScanUniverseInput.DatasetRefreshKey("BTCUSDT", ProviderDatasetType.PRICE), NOW);
        ScanPlanItem item = resolver.resolve(input(List.of(), List.of(new PositionScanAsset("BTCUSDT", "OPEN")),
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
        ScanPlanItem item = resolver.resolve(input(List.of(), List.of(new PositionScanAsset("BTCUSDT", "CLOSED")),
                List.of(), List.of("BTCUSDT"), UserScanProfile.LOW, RuntimeScanProfile.LOW, Map.of(), Map.of())).get(0);
        assertThat(item.effectivePriority()).isEqualTo(AssetPriority.P3_POOL);
    }

    @Test
    void positionPriceCanRefreshEveryConfiguredFiveSeconds() {
        Map<ScanUniverseInput.DatasetRefreshKey, Instant> last = Map.of(
                new ScanUniverseInput.DatasetRefreshKey("BTCUSDT", ProviderDatasetType.PRICE), NOW);
        ScanPlanItem item = resolver.resolve(input(List.of(), List.of(new PositionScanAsset("BTCUSDT", "OPEN")),
                List.of(), List.of(), UserScanProfile.HIGH, RuntimeScanProfile.LOW, Map.of(), last)).get(0);
        assertThat(item.priceDueAt()).isEqualTo(NOW.plusSeconds(5));
    }

    @Test
    void priceRefreshDoesNotRefreshDerivatives() {
        Map<ScanUniverseInput.DatasetRefreshKey, Instant> last = Map.of(
                new ScanUniverseInput.DatasetRefreshKey("BTCUSDT", ProviderDatasetType.PRICE), NOW.minusSeconds(6),
                new ScanUniverseInput.DatasetRefreshKey("BTCUSDT", ProviderDatasetType.DERIVATIVES), NOW.minusSeconds(6));
        ScanPlanItem item = resolver.resolve(input(List.of(), List.of(new PositionScanAsset("BTCUSDT", "OPEN")),
                List.of(), List.of(), UserScanProfile.HIGH, RuntimeScanProfile.LOW, Map.of(), last)).get(0);
        assertThat(item.dueDatasets()).contains(ProviderDatasetType.PRICE).doesNotContain(ProviderDatasetType.DERIVATIVES);
    }

    private ScanUniverseInput input(List<String> core, List<PositionScanAsset> positions, List<String> candidate,
                                    List<String> pool, UserScanProfile base, RuntimeScanProfile auto,
                                    Map<String, RuntimeScanProfile> escalations,
                                    Map<?, ?> rawLast) {
        @SuppressWarnings("unchecked")
        Map<ScanUniverseInput.DatasetRefreshKey, Instant> last = (Map<ScanUniverseInput.DatasetRefreshKey, Instant>) rawLast;
        return new ScanUniverseInput(core, positions, candidate, pool, base, auto, RuntimeScanProfile.LOW,
                RuntimeScanProfile.LOW, escalations, Map.of(), last, NOW);
    }

    private static ScanPlanItem find(List<ScanPlanItem> plan, String symbol) {
        return plan.stream().filter(item -> symbol.equals(item.symbol())).findFirst().orElseThrow();
    }
}
