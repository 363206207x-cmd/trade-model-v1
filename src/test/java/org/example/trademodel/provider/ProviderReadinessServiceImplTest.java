package org.example.trademodel.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.example.trademodel.dto.ohlcv.PublicProviderHealthSnapshot;
import org.example.trademodel.localreal.LocalRealDataStatusService;
import org.example.trademodel.service.readiness.ProviderReadinessServiceImpl;
import org.example.trademodel.vo.ProviderReadinessVO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

class ProviderReadinessServiceImplTest {

    @Test
    void missingProviderConfigDoesNotProduceFakeConnected() {
        ProviderReadinessVO readiness = service(new MockEnvironment()
                .withProperty("position.provider.type", "SIMULATED"))
                .getReadiness();

        assertThat(readiness.getMarketDataProviderStatus()).isEqualTo("WAITING_SYNC");
        assertThat(readiness.getAiProviderStatus()).isEqualTo("WAITING_SYNC");
        assertThat(readiness.getExternalContextProviderStatus()).isEqualTo("WAITING_SYNC");
        assertThat(readiness.getDataSourceText()).isEqualTo("Simulated fallback / WAITING_SYNC");
        assertNoConnected(readiness);
    }

    @Test
    void configOnlyProvidersMapToConfiguredNotConnected() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("position.provider.type", "BINANCE")
                .withProperty("binance.api.base-url", "https://fapi.binance.com")
                .withProperty("trade-model.ai.enabled", "true")
                .withProperty("trade-model.ai.openai.enabled", "true")
                .withProperty("trade-model.ai.openai.api-key", "configured-openai-key")
                .withProperty("trade-model.ai.openai.gpt-final.fast-model", "gpt-5.6-luna")
                .withProperty("trade-model.ai.openai.gpt-final.reasoning-model", "gpt-5.6-sol")
                .withProperty("trade-model.ai.openai.gpt-final.fallback-models[0]", "gpt-5.5")
                .withProperty("trade-model.ai.openai.gpt-final.fallback-models[1]", "gpt-5.4")
                .withProperty("trade-model.ai.openai.base-url", "https://api.openai.test")
                .withProperty("trade-model.external-context.news.api-key", "configured-news-key");

        ProviderReadinessVO readiness = service(environment).getReadiness();

        assertThat(readiness.getMarketDataProviderStatus()).isEqualTo("CONFIGURED");
        assertThat(readiness.getAiProviderStatus()).isEqualTo("CONFIGURED");
        assertThat(readiness.getExternalContextProviderStatus()).isEqualTo("CONFIGURED");
        assertThat(readiness.getDataSourceText()).isEqualTo("Binance public data / CONFIGURED");
        assertNoConnected(readiness);
    }

    @Test
    void explicitlyEnabledAiProviderWithoutKeyFailsClosed() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("trade-model.ai.enabled", "true")
                .withProperty("trade-model.ai.gemini.enabled", "true")
                .withProperty("trade-model.ai.gemini.model", "gemini-test")
                .withProperty("trade-model.ai.gemini.base-url", "https://gemini.test");

        ProviderReadinessVO readiness = service(environment).getReadiness();

        assertThat(readiness.getAiProviderStatus()).isEqualTo("FAIL_CLOSED");
        assertThat(readiness.getProviders()).anySatisfy(provider -> {
            assertThat(provider.getName()).isEqualTo("GEMINI");
            assertThat(provider.getStatus()).isEqualTo("FAIL_CLOSED");
            assertThat(provider.getReason()).isEqualTo("AI_PROVIDER_NOT_CONFIGURED");
        });
        assertNoConnected(readiness);
    }

    @Test
    void localSimulatedFallbackIsAllowedButNeverProductionReadyConnected() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("position.provider.type", "simulated")
                .withProperty("trade-model.ai.enabled", "false");

        ProviderReadinessVO readiness = service(environment).getReadiness();

        assertThat(readiness.getProviders()).anySatisfy(provider -> {
            assertThat(provider.getName()).isEqualTo("SIMULATED_FALLBACK");
            assertThat(provider.getStatus()).isEqualTo("WAITING_SYNC");
            assertThat(provider.getConnected()).isFalse();
        });
    }

    @Test
    void localRealRuntimeUsesFreshProviderHealthAsSingleReadinessSource() {
        LocalRealDataStatusService localRealStatus = mock(LocalRealDataStatusService.class);
        when(localRealStatus.providerReadinessSnapshot()).thenReturn(localRealStatus(
                true, "DASHBOARD_READY", "FRESH", "UP", false));
        ProviderReadinessServiceImpl service = service(new MockEnvironment()
                .withProperty("position.provider.type", "SIMULATED"));
        ReflectionTestUtils.setField(service, "localRealDataStatusService", localRealStatus);

        ProviderReadinessVO readiness = service.getReadiness();

        assertThat(readiness.getMarketDataProviderStatus()).isEqualTo("CONNECTED");
        assertThat(readiness.getDataSourceText()).isEqualTo("Kraken public data / CONNECTED");
        assertThat(readiness.getProviders()).anySatisfy(provider -> {
            assertThat(provider.getName()).isEqualTo("KRAKEN_PUBLIC_MARKET_DATA");
            assertThat(provider.getConnected()).isTrue();
            assertThat(provider.getReason()).isEqualTo("LOCAL_REAL_PROVIDER_VERIFIED_FRESH");
        });
    }

    @Test
    void localRealStaleMarketDataFailsClosedEvenWhenProviderWasUp() {
        LocalRealDataStatusService localRealStatus = mock(LocalRealDataStatusService.class);
        when(localRealStatus.providerReadinessSnapshot()).thenReturn(localRealStatus(
                true, "DASHBOARD_READY", "STALE", "UP", false));
        ProviderReadinessServiceImpl service = service(new MockEnvironment());
        ReflectionTestUtils.setField(service, "localRealDataStatusService", localRealStatus);

        ProviderReadinessVO readiness = service.getReadiness();

        assertThat(readiness.getMarketDataProviderStatus()).isEqualTo("FAIL_CLOSED");
        assertThat(readiness.getProviders()).anySatisfy(provider -> {
            if ("KRAKEN_PUBLIC_MARKET_DATA".equals(provider.getName())) {
                assertThat(provider.getConnected()).isFalse();
                assertThat(provider.getReason()).isEqualTo("LOCAL_REAL_MARKET_DATA_STALE");
            }
        });
    }

    @Test
    void localRealInvalidMarketDataFailsClosedEvenWhenProviderWasUp() {
        LocalRealDataStatusService localRealStatus = mock(LocalRealDataStatusService.class);
        when(localRealStatus.providerReadinessSnapshot()).thenReturn(localRealStatus(
                true, "DASHBOARD_READY", "INVALID", "UP", false));
        ProviderReadinessServiceImpl service = service(new MockEnvironment());
        ReflectionTestUtils.setField(service, "localRealDataStatusService", localRealStatus);

        ProviderReadinessVO readiness = service.getReadiness();

        assertThat(readiness.getMarketDataProviderStatus()).isEqualTo("FAIL_CLOSED");
        assertThat(readiness.getProviders()).anySatisfy(provider -> {
            if ("KRAKEN_PUBLIC_MARKET_DATA".equals(provider.getName())) {
                assertThat(provider.getConnected()).isFalse();
                assertThat(provider.getReason()).isEqualTo("LOCAL_REAL_MARKET_DATA_INVALID");
            }
        });
    }

    @Test
    void localRealStatusReadFailureFailsClosedWithoutStaticFallback() {
        LocalRealDataStatusService localRealStatus = mock(LocalRealDataStatusService.class);
        when(localRealStatus.providerReadinessSnapshot()).thenThrow(new IllegalStateException("status unavailable"));
        ProviderReadinessServiceImpl service = service(new MockEnvironment()
                .withProperty("position.provider.type", "SIMULATED"));
        ReflectionTestUtils.setField(service, "localRealDataStatusService", localRealStatus);

        ProviderReadinessVO readiness = service.getReadiness();

        assertThat(readiness.getMarketDataProviderStatus()).isEqualTo("FAIL_CLOSED");
        assertThat(readiness.getProviders()).anySatisfy(provider -> {
            if ("LOCAL_REAL_MARKET_DATA".equals(provider.getName())) {
                assertThat(provider.getConnected()).isFalse();
                assertThat(provider.getReason()).isEqualTo("LOCAL_REAL_PROVIDER_STATUS_UNAVAILABLE");
            }
        });
    }

    @Test
    void readinessServiceHasNoLiveProviderClientOrderExecutionTelegramOrPushDependency() {
        List<String> forbiddenTypeNames = List.of(
                HttpClient.class.getSimpleName(),
                RestTemplate.class.getSimpleName(),
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "BinancePositionProvider",
                "AiDecisionOrchestratorService",
                "Telegram",
                "Notification",
                "PushRecheckService",
                "PushDispatch",
                "Order",
                "Execution"
        );

        assertThat(Arrays.stream(ProviderReadinessServiceImpl.class.getDeclaredFields())
                .map(Field::getType)
                .map(Class::getSimpleName))
                .doesNotContain(forbiddenTypeNames.toArray(String[]::new));
    }

    private ProviderReadinessServiceImpl service(MockEnvironment environment) {
        return new ProviderReadinessServiceImpl(environment);
    }

    private LocalRealDataStatusService.ProviderReadinessSnapshot localRealStatus(
            boolean dashboardReady,
            String state,
            String freshness,
            String providerStatus,
            boolean circuitOpen) {
        Instant now = Instant.parse("2026-08-10T00:00:00Z");
        PublicProviderHealthSnapshot health = new PublicProviderHealthSnapshot(
                "KRAKEN", providerStatus, now, null, circuitOpen, null);
        return new LocalRealDataStatusService.ProviderReadinessSnapshot(
                "KRAKEN", state, dashboardReady, freshness, health,
                dashboardReady ? "REAL_DATA_AVAILABLE" : "LOCAL_REAL_PROVIDER_NOT_READY", 6, 6);
    }

    private void assertNoConnected(ProviderReadinessVO readiness) {
        assertThat(readiness.getProviders()).allSatisfy(provider -> {
            assertThat(provider.getStatus()).isNotEqualTo("CONNECTED");
            assertThat(provider.getConnected()).isFalse();
        });
    }
}
