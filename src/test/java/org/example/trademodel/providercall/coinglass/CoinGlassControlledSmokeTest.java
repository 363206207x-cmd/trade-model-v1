package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallResult;
import org.example.trademodel.providercall.snapshot.DerivativesRiskSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "COINGLASS_SMOKE_ENABLE_EXTERNAL_CALLS", matches = "true")
class CoinGlassControlledSmokeTest {
    @Autowired
    private CoinGlassDerivativesSnapshotService service;

    @Test
    void controlledSmokeUsesCoordinatorAndReturnsSanitizedSummary() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("TRADE_MODEL_PROVIDER_EXTERNAL_CALLS_ENABLED")));
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("TRADE_MODEL_COINGLASS_ENABLED")));
        Assumptions.assumeTrue(System.getenv("COINGLASS_API_KEY") != null
                && !System.getenv("COINGLASS_API_KEY").isBlank());
        ProviderCallResult<DerivativesRiskSnapshot> result = service.get("BTCUSDT", AssetPriority.P1_WATCHLIST,
                Duration.ofSeconds(60), "controlled-coinglass-smoke");
        DerivativesRiskSnapshot payload = result.payload();
        int available = payload == null ? 0 : payload.availableDatasets().size();
        int missing = payload == null ? 4 : payload.missingDatasets().size();
        System.out.println("COINGLASS_DATASET_AVAILABLE_COUNT: " + available);
        System.out.println("COINGLASS_DATASET_MISSING_COUNT: " + missing);
        System.out.println("COINGLASS_SOURCE_STATUS: " + result.metadata().sourceStatus());
        System.out.println("COINGLASS_LIVE_SMOKE: " + (available == 4 ? "PASS" : "FAIL"));
        assertThat(result.metadata()).isNotNull();
        assertThat(Objects.requireNonNull(payload).availableDatasets()).hasSize(4);
    }
}
