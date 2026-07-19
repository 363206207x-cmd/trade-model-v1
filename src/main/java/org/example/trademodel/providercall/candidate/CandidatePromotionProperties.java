package org.example.trademodel.providercall.candidate;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trade-model.candidate-promotion")
public class CandidatePromotionProperties {
    private int promotionConfirmationCycles = 2;
    private int minimumCandidateHoldSeconds = 300;
    private int candidateTtlSeconds = 3600;
    private int promotionCooldownSeconds = 300;
    private int retriggerCooldownSeconds = 900;
    private int degradationConfirmationCycles = 2;

    @PostConstruct
    public void validate() {
        positive(promotionConfirmationCycles, "promotion-confirmation-cycles");
        positive(minimumCandidateHoldSeconds, "minimum-candidate-hold-seconds");
        positive(candidateTtlSeconds, "candidate-ttl-seconds");
        positive(promotionCooldownSeconds, "promotion-cooldown-seconds");
        positive(retriggerCooldownSeconds, "retrigger-cooldown-seconds");
        positive(degradationConfirmationCycles, "degradation-confirmation-cycles");
        if (candidateTtlSeconds < minimumCandidateHoldSeconds) {
            throw new IllegalStateException("candidate TTL must not be shorter than minimum hold");
        }
    }

    public int getPromotionConfirmationCycles() { return promotionConfirmationCycles; }
    public void setPromotionConfirmationCycles(int value) { this.promotionConfirmationCycles = value; }
    public int getMinimumCandidateHoldSeconds() { return minimumCandidateHoldSeconds; }
    public void setMinimumCandidateHoldSeconds(int value) { this.minimumCandidateHoldSeconds = value; }
    public int getCandidateTtlSeconds() { return candidateTtlSeconds; }
    public void setCandidateTtlSeconds(int value) { this.candidateTtlSeconds = value; }
    public int getPromotionCooldownSeconds() { return promotionCooldownSeconds; }
    public void setPromotionCooldownSeconds(int value) { this.promotionCooldownSeconds = value; }
    public int getRetriggerCooldownSeconds() { return retriggerCooldownSeconds; }
    public void setRetriggerCooldownSeconds(int value) { this.retriggerCooldownSeconds = value; }
    public int getDegradationConfirmationCycles() { return degradationConfirmationCycles; }
    public void setDegradationConfirmationCycles(int value) { this.degradationConfirmationCycles = value; }

    private static void positive(int value, String field) {
        if (value <= 0) throw new IllegalStateException(field + " must be greater than 0");
    }
}
