package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderBudgetState;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.ProviderCircuitState;
import org.example.trademodel.providercall.ProviderDatasetType;
import org.example.trademodel.providercall.ProviderRateBudgetManager;
import org.example.trademodel.providercall.ProviderCallTestFixtures;
import org.example.trademodel.providercall.ProviderSnapshotMetadata;
import org.example.trademodel.providercall.RuntimeScanProfile;
import org.example.trademodel.providercall.SnapshotFreshnessStatus;
import org.example.trademodel.providercall.UnifiedSourceStatus;
import org.example.trademodel.providercall.UserScanProfile;
import org.example.trademodel.providercall.scan.DefaultProviderDatasetRefreshPort;
import org.example.trademodel.providercall.scan.ProviderRefreshStateRegistry;
import org.example.trademodel.providercall.scan.ScanPlanItem;
import org.example.trademodel.providercall.snapshot.AnalysisInputBundle;
import org.example.trademodel.providercall.snapshot.CoordinatedOhlcvSnapshotService;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshot;
import org.example.trademodel.providercall.snapshot.MarketPriceSnapshotService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoinGlassArchitectureAndSafetyTest {
    private static final Instant NOW = Instant.parse("2026-07-10T10:00:00Z");

    @Test
    void positionPriceTickDoesNotRefreshCoinGlass() {
        MarketPriceSnapshotService priceService = mock(MarketPriceSnapshotService.class);
        CoinGlassDerivativesSnapshotService derivativesService = mock(CoinGlassDerivativesSnapshotService.class);
        ProviderRefreshStateRegistry registry = new ProviderRefreshStateRegistry();
        when(priceService.get(anyString(), any(), any(), anyString())).thenReturn(priceResult());
        DefaultProviderDatasetRefreshPort port = new DefaultProviderDatasetRefreshPort(priceService,
                mock(CoordinatedOhlcvSnapshotService.class), mock(PersistedOhlcvBarMapper.class),
                ProviderCallTestFixtures.binanceRegistry("BTCUSDT"), derivativesService,
                new ProviderCallProperties(), registry);

        port.refresh(item(), ProviderDatasetType.PRICE);

        verify(derivativesService, never()).get(anyString(), any(), any(), anyString());
        assertThat(registry.get(ProviderCallTestFixtures.perpetual("BTCUSDT"),
                ProviderDatasetType.PRICE)).isNotNull();
    }

    @Test
    void emergencyRefreshRespectsFortySecondMinimumGap() {
        ProviderCallProperties properties = new ProviderCallProperties();
        assertThat(properties.intervalSeconds(RuntimeScanProfile.EMERGENCY,
                AssetPriority.P0_POSITION, ProviderDatasetType.DERIVATIVES)).isEqualTo(40);
        assertThat(new CoinGlassProperties().getEmergencyMinRefreshGapSeconds()).isEqualTo(40);
    }

    @Test
    void poolBudgetIsDroppedBeforePositionBudget() {
        ProviderCallProperties properties = new ProviderCallProperties();
        ProviderRateBudgetManager manager = new ProviderRateBudgetManager(properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
        manager.register("COINGLASS", 10);
        for (int i = 0; i < 4; i++) assertThat(manager.reserve("COINGLASS", AssetPriority.P3_DISCOVERY)).isTrue();
        assertThat(manager.reserve("COINGLASS", AssetPriority.P3_DISCOVERY)).isFalse();
        assertThat(manager.reserve("COINGLASS", AssetPriority.P0_POSITION)).isTrue();
        ProviderBudgetState state = manager.state("COINGLASS", ProviderCircuitState.CLOSED);
        assertThat(state.rejectedPriority()).isNull();
    }

    @Test
    void businessServicesDoNotDependOnCoinGlassClient() throws IOException {
        Path root = Path.of("src/main/java/org/example/trademodel");
        for (String directory : List.of("service", "positionmonitor", "opportunitylog")) {
            Path path = root.resolve(directory);
            if (!Files.exists(path)) continue;
            try (var files = Files.walk(path)) {
                for (Path file : files.filter(value -> value.toString().endsWith(".java")).toList()) {
                    assertThat(Files.readString(file)).as(file.toString()).doesNotContain("CoinGlassV4Client");
                }
            }
        }
    }

    @Test
    void analysisBundleCarriesDerivativesSnapshotWithoutChangingDecision() {
        DerivativesRiskSnapshot derivatives = snapshot();
        AnalysisInputBundle bundle = new AnalysisInputBundle("BTCUSDT", null, null, null, null,
                null, derivatives, null, null, null, "rule-v1", "trace", NOW);

        assertThat(bundle.derivatives()).isSameAs(derivatives);
        assertThat(List.of(AnalysisInputBundle.class.getRecordComponents()).stream()
                .map(java.lang.reflect.RecordComponent::getName)).doesNotContain("decision", "score", "plan");
    }

    @Test
    void productionCoinGlassRemainsDefaultOff() throws IOException {
        String prod = Files.readString(Path.of("src/main/resources/application-prod.yml"));
        assertThat(prod).contains("TRADE_MODEL_COINGLASS_ENABLED:false")
                .contains("TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED:false");
    }

    @Test
    void smokeScriptDefaultsToSkipped() throws Exception {
        Process process = new ProcessBuilder("bash", "scripts/coinglass-provider-smoke.sh")
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(process.waitFor()).isZero();
        assertThat(output.trim()).isEqualTo("COINGLASS_LIVE_SMOKE: SKIPPED_EXTERNAL_CALLS_DISABLED");
    }

    @Test
    void noSecretsInLogsOrDocs() throws IOException {
        String production = treeText(Path.of("src/main/java/org/example/trademodel/providercall/coinglass"));
        assertThat(production).doesNotContain("fixture-secret-key", "YOUR_API_KEY", "Authorization: Bearer");
        assertThat(production).doesNotContain("System.out", "printStackTrace");
    }

    @Test
    void noAutoOpenCloseReverseOrderOrTrading() throws IOException {
        String production = treeText(Path.of("src/main/java/org/example/trademodel/providercall/coinglass"));
        assertThat(production.toLowerCase()).doesNotContain(
                "auto-open", "auto-close", "auto-reverse", "order execution", "auto-trading");
    }

    @Test
    void noExternalPushOrTelegramSend() throws IOException {
        String production = treeText(Path.of("src/main/java/org/example/trademodel/providercall/coinglass"));
        assertThat(production).doesNotContain("PushDispatch", "Telegram", "sendMessage", "PushRecheckService");
    }

    private static String treeText(Path root) throws IOException {
        StringBuilder content = new StringBuilder();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(value -> value.toString().endsWith(".java")).toList()) {
                content.append(Files.readString(file));
            }
        }
        return content.toString();
    }

    private static ScanPlanItem item() {
        return new ScanPlanItem(ProviderCallTestFixtures.perpetual("BTCUSDT"), "BTCUSDT",
                AssetPriority.P0_POSITION, Set.of(), NOW, NOW, NOW, NOW, NOW, UserScanProfile.AUTO,
                RuntimeScanProfile.STANDARD, List.of("test"), "FM-TEST");
    }

    private static ProviderCallResult<MarketPriceSnapshot> priceResult() {
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata("TEST", ProviderDatasetType.PRICE,
                "BTCUSDT", "GLOBAL", NOW, NOW, NOW.plusSeconds(10), UnifiedSourceStatus.READY,
                SnapshotFreshnessStatus.FRESH, "trace", "key", false, false, null, List.of());
        return new ProviderCallResult<>(null, metadata, null);
    }

    private static DerivativesRiskSnapshot snapshot() {
        ProviderSnapshotMetadata metadata = new ProviderSnapshotMetadata("COINGLASS",
                ProviderDatasetType.DERIVATIVES, "BTCUSDT", "GLOBAL", NOW, NOW, NOW.plusSeconds(60),
                UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH, "trace", "key", false,
                false, null, List.of());
        return new DerivativesRiskSnapshot("BTCUSDT", "COINGLASS_V4", NOW, NOW, NOW.plusSeconds(60),
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, List.of(), List.of(), List.of(), UnifiedSourceStatus.READY,
                SnapshotFreshnessStatus.FRESH, "COMPLETE", List.of(), "trace", Map.of(), metadata);
    }
}
