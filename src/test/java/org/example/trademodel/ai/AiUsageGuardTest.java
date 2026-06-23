package org.example.trademodel.ai;

import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.service.AiCallLogService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiUsageGuardTest {
    @Test
    void evaluate_allowsWhenBudgetAndRatePermit() {
        AiOrchestratorProperties properties = configuredProperties();
        FakeLogService logService = new FakeLogService();
        AiUsageGuard guard = new AiUsageGuard(properties, logService);

        AiUsageGuardResult result = guard.evaluate(new FakeClient(properties.getOpenai()));

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getReservedCostUsd()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void evaluate_blocksWhenRateLimitReached() {
        AiOrchestratorProperties properties = configuredProperties();
        properties.getOpenai().setRequestsPerMinute(1);
        FakeLogService logService = new FakeLogService();
        logService.attempts = 1;
        AiUsageGuard guard = new AiUsageGuard(properties, logService);

        AiUsageGuardResult result = guard.evaluate(new FakeClient(properties.getOpenai()));

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getStatus()).isEqualTo(AiProviderCallStatus.RATE_LIMITED);
        assertThat(result.getReasonCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void evaluate_blocksWhenDailyBudgetWouldBeExceeded() {
        AiOrchestratorProperties properties = configuredProperties();
        properties.setDailyBudgetUsd(new BigDecimal("0.00001"));
        FakeLogService logService = new FakeLogService();
        logService.spent = new BigDecimal("0.000009");
        AiUsageGuard guard = new AiUsageGuard(properties, logService);

        AiUsageGuardResult result = guard.evaluate(new FakeClient(properties.getOpenai()));

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getStatus()).isEqualTo(AiProviderCallStatus.BUDGET_BLOCKED);
        assertThat(result.getReasonCode()).isEqualTo("DAILY_BUDGET_EXCEEDED");
    }

    @Test
    void evaluate_failClosesWhenCostRateIsUnknown() {
        AiOrchestratorProperties properties = configuredProperties();
        properties.getOpenai().setInputCostPerMillionUsd(BigDecimal.ZERO);
        AiUsageGuard guard = new AiUsageGuard(properties, new FakeLogService());

        AiUsageGuardResult result = guard.evaluate(new FakeClient(properties.getOpenai()));

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getStatus()).isEqualTo(AiProviderCallStatus.BUDGET_BLOCKED);
        assertThat(result.getReasonCode()).isEqualTo("COST_RATE_NOT_CONFIGURED");
    }

    @Test
    void evaluate_blocksWhenProviderDisabled() {
        AiOrchestratorProperties properties = configuredProperties();
        properties.getOpenai().setEnabled(false);
        AiUsageGuard guard = new AiUsageGuard(properties, new FakeLogService());

        AiUsageGuardResult result = guard.evaluate(new FakeClient(properties.getOpenai()));

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getStatus()).isEqualTo(AiProviderCallStatus.DISABLED);
    }

    static AiOrchestratorProperties configuredProperties() {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(true);
        properties.setDailyBudgetUsd(new BigDecimal("5.00"));
        properties.setPerAnalysisBudgetUsd(new BigDecimal("1.00"));
        properties.setMaxInputChars(4000);
        properties.setMaxOutputTokens(200);
        properties.getOpenai().setEnabled(true);
        properties.getOpenai().setApiKey("key");
        properties.getOpenai().setModel("model");
        properties.getOpenai().setBaseUrl("https://ai.test");
        properties.getOpenai().setRequestsPerMinute(3);
        properties.getOpenai().setInputCostPerMillionUsd(new BigDecimal("1.00"));
        properties.getOpenai().setOutputCostPerMillionUsd(new BigDecimal("2.00"));
        return properties;
    }

    private record FakeClient(AiProviderProperties providerProperties) implements AiProviderClient {
        @Override public AiProviderName provider() { return AiProviderName.OPENAI; }
        @Override public AiProviderRole role() { return AiProviderRole.GPT_RULE_REVIEW; }
        @Override public AiProviderReadiness readiness() {
            boolean configured = providerProperties.hasKeyAndModel() && providerProperties.getBaseUrl() != null
                    && !providerProperties.getBaseUrl().isBlank();
            return new AiProviderReadiness(provider(), role(), providerProperties.isEnabled(), configured,
                    providerProperties.isEnabled() && configured, providerProperties.getModel(), List.of());
        }
        @Override public AiProviderReviewResult review(AiProviderRequest request) { return null; }
    }

    private static final class FakeLogService implements AiCallLogService {
        int attempts;
        BigDecimal spent = BigDecimal.ZERO;

        @Override public AiCallLogDO startCall(AiProviderRequest request, AiProviderClient client, BigDecimal reservedCostUsd) { return null; }
        @Override public void completeCall(AiCallLogDO log, AiProviderReviewResult result) { }
        @Override public AiCallLogDO recordSkipped(AiProviderRequest request, AiProviderClient client, AiProviderReviewResult result, BigDecimal reservedCostUsd) { return null; }
        @Override public List<AiCallLogDO> query(String analysisId, String traceId, String providerName, String callStatus, LocalDateTime from, LocalDateTime to, int limit) { return List.of(); }
        @Override public int countProviderAttemptsSince(String providerName, LocalDateTime since) { return attempts; }
        @Override public BigDecimal sumChargeableCostSince(LocalDateTime since) { return spent; }
    }
}
