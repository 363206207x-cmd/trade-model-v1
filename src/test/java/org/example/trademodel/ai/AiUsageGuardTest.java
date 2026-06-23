package org.example.trademodel.ai;

import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.service.AiCallLogService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiUsageGuardTest {
    @Test
    void evaluate_allowsSingleProviderBelowCumulativeAnalysisBudget() {
        AiOrchestratorProperties properties = configuredProperties();
        FakeLogService logService = new FakeLogService();
        AiUsageGuard guard = new AiUsageGuard(properties, logService);

        AiUsageGuardResult result = guard.evaluate(new FakeClient(properties.getOpenai()), "analysis-1");

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getReservedCostUsd()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void evaluate_blocksThirdProviderWhenCumulativeAnalysisBudgetWouldBeExceeded() {
        AiOrchestratorProperties properties = configuredProperties();
        setReservation(properties.getOpenai(), "0.40");
        FakeLogService logService = new FakeLogService();
        AiUsageGuard guard = new AiUsageGuard(properties, logService);
        FakeClient client = new FakeClient(properties.getOpenai());

        AiUsageGuardResult first = guard.evaluate(client, "analysis-1");
        logService.analysisSpent.put("analysis-1", first.getReservedCostUsd());
        AiUsageGuardResult second = guard.evaluate(client, "analysis-1");
        logService.analysisSpent.put("analysis-1", logService.analysisSpent.get("analysis-1").add(second.getReservedCostUsd()));
        AiUsageGuardResult third = guard.evaluate(client, "analysis-1");

        assertThat(first.isAllowed()).isTrue();
        assertThat(second.isAllowed()).isTrue();
        assertThat(third.isAllowed()).isFalse();
        assertThat(third.getStatus()).isEqualTo(AiProviderCallStatus.BUDGET_BLOCKED);
        assertThat(third.getReasonCode()).isEqualTo("PER_ANALYSIS_BUDGET_EXCEEDED");
    }

    @Test
    void evaluate_blocksWhenHistoricalAnalysisCostPlusReservationWouldExceedBudget() {
        AiOrchestratorProperties properties = configuredProperties();
        setReservation(properties.getOpenai(), "0.40");
        FakeLogService logService = new FakeLogService();
        logService.analysisSpent.put("analysis-1", new BigDecimal("0.70"));
        AiUsageGuard guard = new AiUsageGuard(properties, logService);

        AiUsageGuardResult result = guard.evaluate(new FakeClient(properties.getOpenai()), "analysis-1");

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getReasonCode()).isEqualTo("PER_ANALYSIS_BUDGET_EXCEEDED");
    }

    @Test
    void evaluate_keepsAnalysisBudgetsIsolatedByAnalysisId() {
        AiOrchestratorProperties properties = configuredProperties();
        setReservation(properties.getOpenai(), "0.40");
        FakeLogService logService = new FakeLogService();
        logService.analysisSpent.put("other-analysis", new BigDecimal("0.90"));
        AiUsageGuard guard = new AiUsageGuard(properties, logService);

        AiUsageGuardResult result = guard.evaluate(new FakeClient(properties.getOpenai()), "analysis-1");

        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    void evaluate_doesNotChargeSkippedBudgetBlockedLogsAgain() {
        AiOrchestratorProperties properties = configuredProperties();
        setReservation(properties.getOpenai(), "0.40");
        FakeLogService logService = new FakeLogService();
        logService.skippedBudgetBlockedReservations.put("analysis-1", new BigDecimal("10.00"));
        AiUsageGuard guard = new AiUsageGuard(properties, logService);

        AiUsageGuardResult result = guard.evaluate(new FakeClient(properties.getOpenai()), "analysis-1");

        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    void evaluate_failClosesWhenAnalysisIdIsMissing() {
        AiOrchestratorProperties properties = configuredProperties();
        AiUsageGuard guard = new AiUsageGuard(properties, new FakeLogService());

        AiUsageGuardResult result = guard.evaluate(new FakeClient(properties.getOpenai()), " ");

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getStatus()).isEqualTo(AiProviderCallStatus.BUDGET_BLOCKED);
        assertThat(result.getReasonCode()).isEqualTo("ANALYSIS_ID_REQUIRED");
    }

    @Test
    void evaluate_blocksWhenStartedDailyReservationConsumesRemainingBudget() {
        AiOrchestratorProperties properties = configuredProperties();
        setReservation(properties.getOpenai(), "0.50");
        properties.setDailyBudgetUsd(new BigDecimal("1.00"));
        FakeLogService logService = new FakeLogService();
        logService.spent = new BigDecimal("0.60");
        AiUsageGuard guard = new AiUsageGuard(properties, logService);

        AiUsageGuardResult result = guard.evaluate(new FakeClient(properties.getOpenai()), "analysis-1");

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getStatus()).isEqualTo(AiProviderCallStatus.BUDGET_BLOCKED);
        assertThat(result.getReasonCode()).isEqualTo("DAILY_BUDGET_EXCEEDED");
    }

    @Test
    void evaluate_blocksWhenRateLimitReached() {
        AiOrchestratorProperties properties = configuredProperties();
        properties.getOpenai().setRequestsPerMinute(1);
        FakeLogService logService = new FakeLogService();
        logService.attempts = 1;
        AiUsageGuard guard = new AiUsageGuard(properties, logService);

        AiUsageGuardResult result = guard.evaluate(new FakeClient(properties.getOpenai()), "analysis-1");

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getStatus()).isEqualTo(AiProviderCallStatus.RATE_LIMITED);
        assertThat(result.getReasonCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void evaluate_failClosesWhenCostRateIsUnknown() {
        AiOrchestratorProperties properties = configuredProperties();
        properties.getOpenai().setInputCostPerMillionUsd(BigDecimal.ZERO);
        AiUsageGuard guard = new AiUsageGuard(properties, new FakeLogService());

        AiUsageGuardResult result = guard.evaluate(new FakeClient(properties.getOpenai()), "analysis-1");

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getStatus()).isEqualTo(AiProviderCallStatus.BUDGET_BLOCKED);
        assertThat(result.getReasonCode()).isEqualTo("COST_RATE_NOT_CONFIGURED");
    }

    @Test
    void evaluate_blocksWhenProviderDisabled() {
        AiOrchestratorProperties properties = configuredProperties();
        properties.getOpenai().setEnabled(false);
        AiUsageGuard guard = new AiUsageGuard(properties, new FakeLogService());

        AiUsageGuardResult result = guard.evaluate(new FakeClient(properties.getOpenai()), "analysis-1");

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

    private static void setReservation(AiProviderProperties properties, String expectedReservation) {
        BigDecimal target = new BigDecimal(expectedReservation);
        properties.setInputCostPerMillionUsd(target.multiply(new BigDecimal("1000")).subtract(new BigDecimal("0.20")));
        properties.setOutputCostPerMillionUsd(BigDecimal.ONE);
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
        final Map<String, BigDecimal> analysisSpent = new HashMap<>();
        final Map<String, BigDecimal> skippedBudgetBlockedReservations = new HashMap<>();

        @Override public AiCallLogDO startCall(AiProviderRequest request, AiProviderClient client, BigDecimal reservedCostUsd) { return null; }
        @Override public void completeCall(AiCallLogDO log, AiProviderReviewResult result) { }
        @Override public AiCallLogDO recordSkipped(AiProviderRequest request, AiProviderClient client, AiProviderReviewResult result, BigDecimal reservedCostUsd) { return null; }
        @Override public List<AiCallLogDO> query(String analysisId, String traceId, String providerName, String callStatus, LocalDateTime from, LocalDateTime to, int limit) { return List.of(); }
        @Override public int countProviderAttemptsSince(String providerName, LocalDateTime since) { return attempts; }
        @Override public BigDecimal sumChargeableCostSince(LocalDateTime since) { return spent; }
        @Override public BigDecimal sumChargeableCostByAnalysisId(String analysisId) {
            return analysisSpent.getOrDefault(analysisId, BigDecimal.ZERO);
        }
    }
}
