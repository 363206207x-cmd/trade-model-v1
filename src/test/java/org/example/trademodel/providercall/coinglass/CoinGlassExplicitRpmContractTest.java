package org.example.trademodel.providercall.coinglass;

import org.example.trademodel.providercall.AssetPriority;
import org.example.trademodel.providercall.ProviderCallProperties;
import org.example.trademodel.providercall.ProviderCircuitState;
import org.example.trademodel.providercall.ProviderRateBudgetManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class CoinGlassExplicitRpmContractTest {

    @Test
    void configurationPresenceKeepsMissingZeroAndPositiveDistinct() {
        assertThat(CoinGlassConfigurationState.evaluate(false, false, false, null))
                .isEqualTo(CoinGlassConfigurationState.NOT_CONFIGURED);
        assertThat(CoinGlassConfigurationState.evaluate(true, true, false, null))
                .isEqualTo(CoinGlassConfigurationState.KEY_MISSING);
        assertThat(CoinGlassConfigurationState.evaluate(true, true, true, null))
                .isEqualTo(CoinGlassConfigurationState.RPM_NOT_CONFIGURED);
        assertThat(CoinGlassConfigurationState.evaluate(true, true, true, 0))
                .isEqualTo(CoinGlassConfigurationState.INVALID_RPM);
        assertThat(CoinGlassConfigurationState.evaluate(true, true, true, -1))
                .isEqualTo(CoinGlassConfigurationState.INVALID_RPM);
        assertThat(CoinGlassConfigurationState.evaluate(true, true, true, 1))
                .isEqualTo(CoinGlassConfigurationState.CONFIGURED);
    }

    @Test
    void missingRpmNeverReceivesAnImplicitRuntimeBudget() {
        ProviderRateBudgetManager manager = new ProviderRateBudgetManager(new ProviderCallProperties());

        assertThat(manager.reserve("COINGLASS", AssetPriority.P1_WATCHLIST)).isFalse();
        assertThat(manager.state("COINGLASS", ProviderCircuitState.CLOSED).advertisedRpm()).isZero();
        assertThat(manager.state("COINGLASS", ProviderCircuitState.CLOSED).lastRejectionReason())
                .isEqualTo("PROVIDER_RPM_NOT_CONFIGURED");
    }

    @Test
    void anyExplicitPositivePlanCanBeRegistered() {
        ProviderRateBudgetManager manager = new ProviderRateBudgetManager(new ProviderCallProperties());

        manager.register("COINGLASS", 1);

        assertThat(manager.reserve("COINGLASS", AssetPriority.P1_WATCHLIST)).isTrue();
        assertThat(manager.state("COINGLASS", ProviderCircuitState.CLOSED).advertisedRpm()).isEqualTo(1);
    }

    @Test
    void explicitStartupAndStandardPlanValuesAreUsedExactly() {
        ProviderRateBudgetManager startup = new ProviderRateBudgetManager(new ProviderCallProperties());
        ProviderRateBudgetManager standard = new ProviderRateBudgetManager(new ProviderCallProperties());

        startup.register("COINGLASS", 80);
        standard.register("COINGLASS", 300);

        assertThat(startup.state("COINGLASS", ProviderCircuitState.CLOSED).advertisedRpm()).isEqualTo(80);
        assertThat(standard.state("COINGLASS", ProviderCircuitState.CLOSED).advertisedRpm()).isEqualTo(300);
    }

    @Test
    void productionImplicitCoinGlassRpmDefaultCountIsZero() throws Exception {
        StringBuilder production = new StringBuilder();
        for (Path root : List.of(Path.of("src/main"), Path.of("scripts"), Path.of(".env.example"))) {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    production.append(Files.readString(path)).append('\n');
                }
            }
        }
        for (String forbidden : List.of(
                "COINGLASS_ADVERTISED_RPM\\s*:\\s*300",
                "advertisedRpm\\s*=\\s*300",
                "coinglassAdvertisedRpm\\s*=\\s*300",
                "orElse\\s*\\(\\s*300\\s*\\)")) {
            assertThat(Pattern.compile(forbidden).matcher(production).find())
                    .as("production implicit CoinGlass RPM default: %s", forbidden)
                    .isFalse();
        }
    }
}
