package org.example.trademodel.ai;

import org.example.trademodel.service.AiCallLogService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class AiUsageGuard {
    private final AiOrchestratorProperties properties;
    private final AiCallLogService callLogService;

    public AiUsageGuard(AiOrchestratorProperties properties, AiCallLogService callLogService) {
        this.properties = properties;
        this.callLogService = callLogService;
    }

    public AiUsageGuardResult evaluate(AiProviderClient client) {
        if (!properties.isEnabled()) {
            return AiUsageGuardResult.blocked(AiProviderCallStatus.DISABLED, "AI_ORCHESTRATOR_DISABLED", BigDecimal.ZERO);
        }
        AiProviderReadiness readiness = client.readiness();
        if (!readiness.isEnabled()) {
            return AiUsageGuardResult.blocked(AiProviderCallStatus.DISABLED, "PROVIDER_DISABLED", BigDecimal.ZERO);
        }
        if (!readiness.isConfigured()) {
            return AiUsageGuardResult.blocked(AiProviderCallStatus.NOT_CONFIGURED, "PROVIDER_NOT_CONFIGURED", BigDecimal.ZERO);
        }
        AiProviderProperties providerProperties = client.providerProperties();
        if (providerProperties.getRequestsPerMinute() <= 0) {
            return AiUsageGuardResult.blocked(AiProviderCallStatus.RATE_LIMITED, "RATE_LIMIT_NOT_CONFIGURED", BigDecimal.ZERO);
        }
        BigDecimal reservedCost = estimateCost(providerProperties);
        if (reservedCost.compareTo(BigDecimal.ZERO) <= 0) {
            return AiUsageGuardResult.blocked(AiProviderCallStatus.BUDGET_BLOCKED, "COST_RATE_NOT_CONFIGURED", reservedCost);
        }
        if (properties.getDailyBudgetUsd().compareTo(BigDecimal.ZERO) <= 0
                || properties.getPerAnalysisBudgetUsd().compareTo(BigDecimal.ZERO) <= 0) {
            return AiUsageGuardResult.blocked(AiProviderCallStatus.BUDGET_BLOCKED, "BUDGET_NOT_CONFIGURED", reservedCost);
        }
        if (reservedCost.compareTo(properties.getPerAnalysisBudgetUsd()) > 0) {
            return AiUsageGuardResult.blocked(AiProviderCallStatus.BUDGET_BLOCKED, "PER_ANALYSIS_BUDGET_EXCEEDED", reservedCost);
        }
        try {
            LocalDateTime minuteAgo = LocalDateTime.now().minusMinutes(1);
            int attempts = callLogService.countProviderAttemptsSince(client.provider().name(), minuteAgo);
            if (attempts >= providerProperties.getRequestsPerMinute()) {
                return AiUsageGuardResult.blocked(AiProviderCallStatus.RATE_LIMITED, "RATE_LIMIT_EXCEEDED", reservedCost);
            }
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            BigDecimal spentToday = callLogService.sumChargeableCostSince(startOfDay);
            if (spentToday.add(reservedCost).compareTo(properties.getDailyBudgetUsd()) > 0) {
                return AiUsageGuardResult.blocked(AiProviderCallStatus.BUDGET_BLOCKED, "DAILY_BUDGET_EXCEEDED", reservedCost);
            }
            return AiUsageGuardResult.allowed(reservedCost);
        } catch (Exception e) {
            return AiUsageGuardResult.blocked(AiProviderCallStatus.BUDGET_BLOCKED, "AI_CALL_LOG_UNAVAILABLE", reservedCost);
        }
    }

    private BigDecimal estimateCost(AiProviderProperties providerProperties) {
        if (providerProperties.getInputCostPerMillionUsd().compareTo(BigDecimal.ZERO) <= 0
                || providerProperties.getOutputCostPerMillionUsd().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        long estimatedInputTokens = Math.max(1, properties.getMaxInputChars() / 4L);
        long estimatedOutputTokens = Math.max(1, properties.getMaxOutputTokens());
        BigDecimal input = BigDecimal.valueOf(estimatedInputTokens)
                .multiply(providerProperties.getInputCostPerMillionUsd())
                .divide(BigDecimal.valueOf(1_000_000L), 12, RoundingMode.HALF_UP);
        BigDecimal output = BigDecimal.valueOf(estimatedOutputTokens)
                .multiply(providerProperties.getOutputCostPerMillionUsd())
                .divide(BigDecimal.valueOf(1_000_000L), 12, RoundingMode.HALF_UP);
        return input.add(output).setScale(8, RoundingMode.HALF_UP);
    }
}
