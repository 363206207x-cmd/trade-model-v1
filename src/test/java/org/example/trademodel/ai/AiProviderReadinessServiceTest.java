package org.example.trademodel.ai;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiProviderReadinessServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-15T10:00:00Z");

    @Test
    void missingAndExplicitZeroCostRemainDistinctAndBothFailClosed() {
        AiOrchestratorProperties properties = configuredProperties(AiProviderName.OPENAI);
        properties.getOpenai().setInputCostPerMillionUsd(null);
        AiProviderReadinessService missing = service(properties, List.of());

        AiProviderRuntimeReadiness missingReadiness = missing.readiness(AiProviderName.OPENAI);
        properties.getOpenai().setInputCostPerMillionUsd(BigDecimal.ZERO);
        AiProviderReadinessService explicitZero = service(properties, List.of());
        AiProviderRuntimeReadiness zeroReadiness = explicitZero.readiness(AiProviderName.OPENAI);

        assertThat(missingReadiness.state()).isEqualTo(AiProviderReadinessState.COST_NOT_CONFIGURED);
        assertThat(missingReadiness.inputCostConfiguration()).isEqualTo(AiConfigurationPresence.MISSING);
        assertThat(zeroReadiness.state()).isEqualTo(AiProviderReadinessState.COST_NOT_CONFIGURED);
        assertThat(zeroReadiness.inputCostConfiguration()).isEqualTo(AiConfigurationPresence.EXPLICIT_ZERO);
    }

    @Test
    void rpmAndBudgetMissingHaveDedicatedStates() {
        AiOrchestratorProperties properties = configuredProperties(AiProviderName.OPENAI);
        properties.getOpenai().setRequestsPerMinute(null);
        assertThat(service(properties, List.of()).readiness(AiProviderName.OPENAI).state())
                .isEqualTo(AiProviderReadinessState.RPM_NOT_CONFIGURED);

        properties.getOpenai().setRequestsPerMinute(10);
        properties.setDailyBudgetUsd(null);
        assertThat(service(properties, List.of()).readiness(AiProviderName.OPENAI).state())
                .isEqualTo(AiProviderReadinessState.BUDGET_NOT_CONFIGURED);
    }

    @Test
    void exactModelProbeAuthorizesAndHealthReadsUseCacheWithoutPaidCalls() {
        AiOrchestratorProperties properties = configuredProperties(AiProviderName.OPENAI);
        AiProviderClient client = client(AiProviderName.OPENAI, successful("req-123"));
        AiProviderReadinessService service = service(properties, List.of(client));

        assertThat(service.readiness(AiProviderName.OPENAI).state())
                .isEqualTo(AiProviderReadinessState.MODEL_NOT_VERIFIED);
        AiProviderRuntimeReadiness verified = service.reverify(AiProviderName.OPENAI);

        assertThat(verified.state()).isEqualTo(AiProviderReadinessState.AUTHORIZED);
        assertThat(verified.model()).isEqualTo("gpt-5.6-sol");
        assertThat(verified.requestId()).isEqualTo("req-123");
        assertThat(service.readiness(AiProviderName.OPENAI).ready()).isTrue();
        assertThat(service.readiness(AiProviderName.OPENAI).ready()).isTrue();
        verify(client, times(1)).verifyExactModel("gpt-5.6-sol", 10_000L);
    }

    @Test
    void fallbackSuccessNeverAuthorizesExactModel() {
        AiOrchestratorProperties properties = configuredProperties(AiProviderName.OPENAI);
        AiProviderReviewResult fallback = successful("fallback-request");
        fallback.setFallback(true);
        fallback.setFallbackReason("MODEL_FALLBACK_USED");
        AiProviderReadinessService service = service(
                properties, List.of(client(AiProviderName.OPENAI, fallback)));

        AiProviderRuntimeReadiness result = service.reverify(AiProviderName.OPENAI);

        assertThat(result.state()).isEqualTo(AiProviderReadinessState.MODEL_NOT_VERIFIED);
        assertThat(result.ready()).isFalse();
    }

    @Test
    void authRateAndModelFailuresMapToCanonicalStatesWithoutSecrets() {
        assertFailure("PROVIDER_AUTH_FAILURE", AiProviderCallStatus.FAILED,
                AiProviderReadinessState.AUTH_FAILED);
        assertFailure("PROVIDER_RATE_LIMITED", AiProviderCallStatus.RATE_LIMITED,
                AiProviderReadinessState.RATE_LIMITED);
        assertFailure("PROVIDER_MODEL_NOT_FOUND", AiProviderCallStatus.FAILED,
                AiProviderReadinessState.MODEL_UNAVAILABLE);
    }

    @Test
    void providerExceptionFailsClosedAndIsCachedWithoutExposingExceptionText() {
        AiOrchestratorProperties properties = configuredProperties(AiProviderName.OPENAI);
        AiProviderClient client = mock(AiProviderClient.class);
        when(client.provider()).thenReturn(AiProviderName.OPENAI);
        when(client.role()).thenReturn(AiProviderRole.GPT_RULE_REVIEW);
        doThrow(new IllegalStateException("test-secret-provider-error"))
                .when(client).verifyExactModel("gpt-5.6-sol", 10_000L);
        AiProviderReadinessService service = service(properties, List.of(client));

        AiProviderRuntimeReadiness readiness = service.reverify(AiProviderName.OPENAI);

        assertThat(readiness.state()).isEqualTo(AiProviderReadinessState.PROVIDER_UNAVAILABLE);
        assertThat(readiness.reasonCode()).isEqualTo("PROVIDER_VERIFICATION_EXCEPTION");
        assertThat(readiness.toString()).doesNotContain("test-secret-provider-error");
        assertThat(service.readiness(AiProviderName.OPENAI).ready()).isFalse();
        verify(client, times(1)).verifyExactModel("gpt-5.6-sol", 10_000L);
    }

    @Test
    void configuredProvidersAreVerifiedOnceUntilTheirExistingTtlExpires() {
        AiOrchestratorProperties properties = fullyConfiguredProperties();
        AiProviderClient openai = client(AiProviderName.OPENAI, successful("openai-request"));
        AiProviderClient gemini = client(AiProviderName.GEMINI, successful("gemini-request"));
        AiProviderClient xai = client(AiProviderName.XAI, successful("xai-request"));
        MutableClock clock = new MutableClock(NOW);
        AiProviderReadinessService service = new AiProviderReadinessService(
                properties, List.of(openai, gemini, xai), 3600, clock);

        assertThat(service.verifyConfiguredProvidersIfDue())
                .extracting(AiProviderRuntimeReadiness::state)
                .containsOnly(AiProviderReadinessState.AUTHORIZED);
        assertThat(service.verifyConfiguredProvidersIfDue())
                .extracting(AiProviderRuntimeReadiness::state)
                .containsOnly(AiProviderReadinessState.AUTHORIZED);
        verify(openai, times(1)).verifyExactModel("gpt-5.6-sol", 10_000L);
        verify(gemini, times(1)).verifyExactModel("gemini-3.5-flash", 25_000L);
        verify(xai, times(1)).verifyExactModel("grok-4.5", 10_000L);

        clock.advance(Duration.ofSeconds(3601));
        assertThat(service.verifyConfiguredProvidersIfDue())
                .extracting(AiProviderRuntimeReadiness::state)
                .containsOnly(AiProviderReadinessState.AUTHORIZED);
        verify(openai, times(2)).verifyExactModel("gpt-5.6-sol", 10_000L);
        verify(gemini, times(2)).verifyExactModel("gemini-3.5-flash", 25_000L);
        verify(xai, times(2)).verifyExactModel("grok-4.5", 10_000L);
    }

    @Test
    void oneProviderVerificationFailureDoesNotBlockOtherConfiguredProviders() {
        AiOrchestratorProperties properties = fullyConfiguredProperties();
        AiProviderClient openai = mock(AiProviderClient.class);
        when(openai.provider()).thenReturn(AiProviderName.OPENAI);
        when(openai.role()).thenReturn(AiProviderRole.GPT_RULE_REVIEW);
        doThrow(new IllegalStateException("private-provider-detail"))
                .when(openai).verifyExactModel("gpt-5.6-sol", 10_000L);
        AiProviderClient gemini = client(AiProviderName.GEMINI, successful("gemini-request"));
        AiProviderClient xai = client(AiProviderName.XAI, successful("xai-request"));
        AiProviderReadinessService service = service(properties, List.of(openai, gemini, xai));

        List<AiProviderRuntimeReadiness> readiness = service.verifyConfiguredProvidersIfDue();

        assertThat(readiness).extracting(AiProviderRuntimeReadiness::state)
                .containsExactly(AiProviderReadinessState.PROVIDER_UNAVAILABLE,
                        AiProviderReadinessState.AUTHORIZED, AiProviderReadinessState.AUTHORIZED);
        assertThat(readiness.toString()).doesNotContain("private-provider-detail");
    }

    private static void assertFailure(String code,
                                      AiProviderCallStatus callStatus,
                                      AiProviderReadinessState expected) {
        AiOrchestratorProperties properties = configuredProperties(AiProviderName.OPENAI);
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(AiProviderName.OPENAI);
        result.setRole(AiProviderRole.GPT_RULE_REVIEW);
        result.setCallStatus(callStatus);
        result.setErrorCode(code);
        AiProviderRuntimeReadiness readiness = service(
                properties, List.of(client(AiProviderName.OPENAI, result)))
                .reverify(AiProviderName.OPENAI);
        assertThat(readiness.state()).isEqualTo(expected);
        assertThat(readiness.toString()).doesNotContain("test-secret");
    }

    private static AiProviderClient client(AiProviderName provider, AiProviderReviewResult result) {
        AiProviderClient client = mock(AiProviderClient.class);
        when(client.provider()).thenReturn(provider);
        when(client.verifyExactModel(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong())).thenReturn(result);
        return client;
    }

    private static AiProviderReviewResult successful(String requestId) {
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(AiProviderName.OPENAI);
        result.setRole(AiProviderRole.GPT_RULE_REVIEW);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setProviderRequestId(requestId);
        return result;
    }

    private static AiProviderReadinessService service(AiOrchestratorProperties properties,
                                                      List<AiProviderClient> clients) {
        return new AiProviderReadinessService(properties, clients, 3600,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static AiOrchestratorProperties configuredProperties(AiProviderName provider) {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(true);
        properties.setDailyBudgetUsd(new BigDecimal("10"));
        properties.setPerAnalysisBudgetUsd(new BigDecimal("1"));
        AiProviderProperties selected = switch (provider) {
            case OPENAI -> properties.getOpenai();
            case GEMINI -> properties.getGemini();
            case XAI -> properties.getXai();
        };
        selected.setEnabled(true);
        selected.setApiKey("test-secret");
        selected.setBaseUrl("https://provider.invalid");
        selected.setRequestsPerMinute(10);
        selected.setInputCostPerMillionUsd(new BigDecimal("1.25"));
        selected.setOutputCostPerMillionUsd(new BigDecimal("2.50"));
        if (provider == AiProviderName.OPENAI) {
            selected.getGptFinal().setFastModel("gpt-5.6-luna");
            selected.getGptFinal().setReasoningModel("gpt-5.6-sol");
            selected.getGptFinal().setFallbackModels(List.of("gpt-5.5", "gpt-5.4"));
        } else if (provider == AiProviderName.GEMINI) {
            selected.setModel("gemini-3.5-flash");
        } else {
            selected.setModel("grok-4.5");
        }
        return properties;
    }

    private static AiOrchestratorProperties fullyConfiguredProperties() {
        AiOrchestratorProperties properties = configuredProperties(AiProviderName.OPENAI);
        configureProvider(properties.getGemini(), AiProviderName.GEMINI);
        configureProvider(properties.getXai(), AiProviderName.XAI);
        return properties;
    }

    private static void configureProvider(AiProviderProperties selected, AiProviderName provider) {
        selected.setEnabled(true);
        selected.setApiKey("test-secret");
        selected.setBaseUrl("https://provider.invalid");
        selected.setRequestsPerMinute(10);
        selected.setInputCostPerMillionUsd(new BigDecimal("1.25"));
        selected.setOutputCostPerMillionUsd(new BigDecimal("2.50"));
        selected.setModel(provider == AiProviderName.GEMINI ? "gemini-3.5-flash" : "grok-4.5");
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
