package org.example.trademodel.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Product-owned v4.1 thresholds. Missing values fail application startup rather
 * than silently selecting a scattered runtime default.
 */
@Component
@ConfigurationProperties(prefix = "trade-model.fundamental-ai-v4-1")
public class FundamentalAiV41Properties {
    private Ranking ranking = new Ranking();
    private OpportunityState opportunityState = new OpportunityState();
    private AiGate aiGate = new AiGate();
    private MultiTimeframe multiTimeframe = new MultiTimeframe();
    private AccountRisk accountRisk = new AccountRisk();
    private ExecutionFeasibility executionFeasibility = new ExecutionFeasibility();

    @PostConstruct
    public void validate() {
        ranking.validate();
        opportunityState.validate();
        aiGate.validate();
        multiTimeframe.validate();
        accountRisk.validate();
        executionFeasibility.validate();
    }

    public Ranking getRanking() { return ranking; }
    public void setRanking(Ranking ranking) { this.ranking = ranking == null ? new Ranking() : ranking; }
    public OpportunityState getOpportunityState() { return opportunityState; }
    public void setOpportunityState(OpportunityState value) {
        this.opportunityState = value == null ? new OpportunityState() : value;
    }
    public AiGate getAiGate() { return aiGate; }
    public void setAiGate(AiGate aiGate) { this.aiGate = aiGate == null ? new AiGate() : aiGate; }
    public MultiTimeframe getMultiTimeframe() { return multiTimeframe; }
    public void setMultiTimeframe(MultiTimeframe value) {
        this.multiTimeframe = value == null ? new MultiTimeframe() : value;
    }
    public AccountRisk getAccountRisk() { return accountRisk; }
    public void setAccountRisk(AccountRisk value) {
        this.accountRisk = value == null ? new AccountRisk() : value;
    }
    public ExecutionFeasibility getExecutionFeasibility() { return executionFeasibility; }
    public void setExecutionFeasibility(ExecutionFeasibility value) {
        this.executionFeasibility = value == null ? new ExecutionFeasibility() : value;
    }

    /** Contract fixture used only by direct unit construction outside Spring binding. */
    public static FundamentalAiV41Properties contractFixture() {
        FundamentalAiV41Properties value = new FundamentalAiV41Properties();
        value.ranking.homeCapacity = 6;
        value.ranking.freshnessWindowSeconds = 3600;
        value.ranking.minimumDataQuality = 70;
        value.ranking.opportunityScoreWeight = new BigDecimal("0.30");
        value.ranking.confidenceWeight = new BigDecimal("0.15");
        value.ranking.riskWeight = new BigDecimal("0.10");
        value.ranking.planModeWeight = new BigDecimal("0.15");
        value.ranking.dataQualityWeight = new BigDecimal("0.10");
        value.ranking.freshnessWeight = new BigDecimal("0.08");
        value.ranking.conflictWeight = new BigDecimal("0.07");
        value.ranking.stabilityWeight = new BigDecimal("0.05");
        value.opportunityState.minimumDwellSeconds = 300;
        value.opportunityState.coolingSeconds = 900;
        value.opportunityState.candidatePromotionScore = 70;
        value.opportunityState.confusedEnterThreshold = 70;
        value.opportunityState.directionalPushBlockThreshold = 85;
        value.aiGate.minimumDataQuality = 85;
        value.aiGate.circuitBreakerScore = 70;
        value.aiGate.minimumSignificantEvidenceStrength = 60;
        value.aiGate.cacheTtlSeconds = 300;
        value.aiGate.cacheMaxEntries = 500;
        value.multiTimeframe.fourHourWeight = new BigDecimal("0.40");
        value.multiTimeframe.oneHourWeight = new BigDecimal("0.30");
        value.multiTimeframe.fifteenMinuteWeight = new BigDecimal("0.20");
        value.multiTimeframe.fiveMinuteWeight = new BigDecimal("0.10");
        value.multiTimeframe.minimumAlignedCount = 3;
        value.multiTimeframe.minimumAlignedWeight = new BigDecimal("0.60");
        value.multiTimeframe.maximumTrendScoreDifference = new BigDecimal("15");
        value.accountRisk.lowMaxExposure = new BigDecimal("0.30");
        value.accountRisk.mediumMaxExposure = new BigDecimal("0.20");
        value.accountRisk.highMaxExposure = new BigDecimal("0.10");
        value.accountRisk.extremeMaxExposure = new BigDecimal("0.05");
        value.accountRisk.maxLeverage = new BigDecimal("10");
        value.accountRisk.freshnessSeconds = 300;
        value.executionFeasibility.quoteFreshnessSeconds = 30;
        value.executionFeasibility.maxSpreadBps = new BigDecimal("15");
        value.executionFeasibility.minimumTopOfBookNotional = new BigDecimal("10000");
        value.executionFeasibility.maxEntryDriftBps = new BigDecimal("10");
        value.validate();
        return value;
    }

    public static class Ranking {
        private Integer homeCapacity;
        private Integer freshnessWindowSeconds;
        private Integer minimumDataQuality;
        private BigDecimal opportunityScoreWeight;
        private BigDecimal confidenceWeight;
        private BigDecimal riskWeight;
        private BigDecimal planModeWeight;
        private BigDecimal dataQualityWeight;
        private BigDecimal freshnessWeight;
        private BigDecimal conflictWeight;
        private BigDecimal stabilityWeight;

        void validate() {
            positive(homeCapacity, "ranking.home-capacity");
            if (homeCapacity != 6) throw new IllegalStateException("ranking.home-capacity must be 6");
            positive(freshnessWindowSeconds, "ranking.freshness-window-seconds");
            range(minimumDataQuality, 0, 100, "ranking.minimum-data-quality");
            positive(opportunityScoreWeight, "ranking.opportunity-score-weight");
            positive(confidenceWeight, "ranking.confidence-weight");
            positive(riskWeight, "ranking.risk-weight");
            positive(planModeWeight, "ranking.plan-mode-weight");
            positive(dataQualityWeight, "ranking.data-quality-weight");
            positive(freshnessWeight, "ranking.freshness-weight");
            positive(conflictWeight, "ranking.conflict-weight");
            positive(stabilityWeight, "ranking.stability-weight");
        }

        public Integer getHomeCapacity() { return homeCapacity; }
        public void setHomeCapacity(Integer value) { this.homeCapacity = value; }
        public Integer getFreshnessWindowSeconds() { return freshnessWindowSeconds; }
        public void setFreshnessWindowSeconds(Integer value) { this.freshnessWindowSeconds = value; }
        public Integer getMinimumDataQuality() { return minimumDataQuality; }
        public void setMinimumDataQuality(Integer value) { this.minimumDataQuality = value; }
        public BigDecimal getOpportunityScoreWeight() { return opportunityScoreWeight; }
        public void setOpportunityScoreWeight(BigDecimal value) { this.opportunityScoreWeight = value; }
        public BigDecimal getConfidenceWeight() { return confidenceWeight; }
        public void setConfidenceWeight(BigDecimal value) { this.confidenceWeight = value; }
        public BigDecimal getRiskWeight() { return riskWeight; }
        public void setRiskWeight(BigDecimal value) { this.riskWeight = value; }
        public BigDecimal getPlanModeWeight() { return planModeWeight; }
        public void setPlanModeWeight(BigDecimal value) { this.planModeWeight = value; }
        public BigDecimal getDataQualityWeight() { return dataQualityWeight; }
        public void setDataQualityWeight(BigDecimal value) { this.dataQualityWeight = value; }
        public BigDecimal getFreshnessWeight() { return freshnessWeight; }
        public void setFreshnessWeight(BigDecimal value) { this.freshnessWeight = value; }
        public BigDecimal getConflictWeight() { return conflictWeight; }
        public void setConflictWeight(BigDecimal value) { this.conflictWeight = value; }
        public BigDecimal getStabilityWeight() { return stabilityWeight; }
        public void setStabilityWeight(BigDecimal value) { this.stabilityWeight = value; }
    }

    public static class OpportunityState {
        private Integer minimumDwellSeconds;
        private Integer coolingSeconds;
        private Integer candidatePromotionScore;
        private Integer confusedEnterThreshold;
        private Integer directionalPushBlockThreshold;

        void validate() {
            positive(minimumDwellSeconds, "opportunity-state.minimum-dwell-seconds");
            positive(coolingSeconds, "opportunity-state.cooling-seconds");
            range(candidatePromotionScore, 0, 100, "opportunity-state.candidate-promotion-score");
            range(confusedEnterThreshold, 0, 100, "opportunity-state.confused-enter-threshold");
            range(directionalPushBlockThreshold, 0, 100,
                    "opportunity-state.directional-push-block-threshold");
            if (directionalPushBlockThreshold < confusedEnterThreshold) {
                throw new IllegalStateException("directional push threshold must not be below confused threshold");
            }
        }

        public Integer getMinimumDwellSeconds() { return minimumDwellSeconds; }
        public void setMinimumDwellSeconds(Integer value) { this.minimumDwellSeconds = value; }
        public Integer getCoolingSeconds() { return coolingSeconds; }
        public void setCoolingSeconds(Integer value) { this.coolingSeconds = value; }
        public Integer getCandidatePromotionScore() { return candidatePromotionScore; }
        public void setCandidatePromotionScore(Integer value) { this.candidatePromotionScore = value; }
        public Integer getConfusedEnterThreshold() { return confusedEnterThreshold; }
        public void setConfusedEnterThreshold(Integer value) { this.confusedEnterThreshold = value; }
        public Integer getDirectionalPushBlockThreshold() { return directionalPushBlockThreshold; }
        public void setDirectionalPushBlockThreshold(Integer value) {
            this.directionalPushBlockThreshold = value;
        }
    }

    public static class AiGate {
        private Integer minimumDataQuality;
        private Integer circuitBreakerScore;
        private Integer minimumSignificantEvidenceStrength;
        private Integer cacheTtlSeconds;
        private Integer cacheMaxEntries;

        void validate() {
            range(minimumDataQuality, 0, 100, "ai-gate.minimum-data-quality");
            range(circuitBreakerScore, 0, 100, "ai-gate.circuit-breaker-score");
            range(minimumSignificantEvidenceStrength, 0, 100,
                    "ai-gate.minimum-significant-evidence-strength");
            positive(cacheTtlSeconds, "ai-gate.cache-ttl-seconds");
            positive(cacheMaxEntries, "ai-gate.cache-max-entries");
            if (minimumDataQuality < circuitBreakerScore) {
                throw new IllegalStateException("AI quality threshold must not be below circuit breaker");
            }
        }

        public Integer getMinimumDataQuality() { return minimumDataQuality; }
        public void setMinimumDataQuality(Integer value) { this.minimumDataQuality = value; }
        public Integer getCircuitBreakerScore() { return circuitBreakerScore; }
        public void setCircuitBreakerScore(Integer value) { this.circuitBreakerScore = value; }
        public Integer getMinimumSignificantEvidenceStrength() { return minimumSignificantEvidenceStrength; }
        public void setMinimumSignificantEvidenceStrength(Integer value) {
            this.minimumSignificantEvidenceStrength = value;
        }
        public Integer getCacheTtlSeconds() { return cacheTtlSeconds; }
        public void setCacheTtlSeconds(Integer value) { this.cacheTtlSeconds = value; }
        public Integer getCacheMaxEntries() { return cacheMaxEntries; }
        public void setCacheMaxEntries(Integer value) { this.cacheMaxEntries = value; }
    }

    public static class MultiTimeframe {
        private BigDecimal fourHourWeight;
        private BigDecimal oneHourWeight;
        private BigDecimal fifteenMinuteWeight;
        private BigDecimal fiveMinuteWeight;
        private Integer minimumAlignedCount;
        private BigDecimal minimumAlignedWeight;
        private BigDecimal maximumTrendScoreDifference;

        void validate() {
            positive(fourHourWeight, "multi-timeframe.four-hour-weight");
            positive(oneHourWeight, "multi-timeframe.one-hour-weight");
            positive(fifteenMinuteWeight, "multi-timeframe.fifteen-minute-weight");
            positive(fiveMinuteWeight, "multi-timeframe.five-minute-weight");
            BigDecimal total = fourHourWeight.add(oneHourWeight)
                    .add(fifteenMinuteWeight).add(fiveMinuteWeight);
            if (total.compareTo(BigDecimal.ONE) != 0) {
                throw new IllegalStateException("multi-timeframe weights must sum to 1.00");
            }
            if (minimumAlignedCount == null || minimumAlignedCount < 1 || minimumAlignedCount > 4) {
                throw new IllegalStateException("multi-timeframe.minimum-aligned-count must be in [1, 4]");
            }
            if (minimumAlignedWeight == null || minimumAlignedWeight.signum() <= 0
                    || minimumAlignedWeight.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalStateException("multi-timeframe.minimum-aligned-weight must be in (0, 1]");
            }
            positive(maximumTrendScoreDifference,
                    "multi-timeframe.maximum-trend-score-difference");
        }

        public BigDecimal getFourHourWeight() { return fourHourWeight; }
        public void setFourHourWeight(BigDecimal value) { this.fourHourWeight = value; }
        public BigDecimal getOneHourWeight() { return oneHourWeight; }
        public void setOneHourWeight(BigDecimal value) { this.oneHourWeight = value; }
        public BigDecimal getFifteenMinuteWeight() { return fifteenMinuteWeight; }
        public void setFifteenMinuteWeight(BigDecimal value) { this.fifteenMinuteWeight = value; }
        public BigDecimal getFiveMinuteWeight() { return fiveMinuteWeight; }
        public void setFiveMinuteWeight(BigDecimal value) { this.fiveMinuteWeight = value; }
        public Integer getMinimumAlignedCount() { return minimumAlignedCount; }
        public void setMinimumAlignedCount(Integer value) { this.minimumAlignedCount = value; }
        public BigDecimal getMinimumAlignedWeight() { return minimumAlignedWeight; }
        public void setMinimumAlignedWeight(BigDecimal value) { this.minimumAlignedWeight = value; }
        public BigDecimal getMaximumTrendScoreDifference() { return maximumTrendScoreDifference; }
        public void setMaximumTrendScoreDifference(BigDecimal value) {
            this.maximumTrendScoreDifference = value;
        }
    }

    public static class AccountRisk {
        private BigDecimal lowMaxExposure;
        private BigDecimal mediumMaxExposure;
        private BigDecimal highMaxExposure;
        private BigDecimal extremeMaxExposure;
        private BigDecimal maxLeverage;
        private Integer freshnessSeconds;

        void validate() {
            fraction(lowMaxExposure, "account-risk.low-max-exposure");
            fraction(mediumMaxExposure, "account-risk.medium-max-exposure");
            fraction(highMaxExposure, "account-risk.high-max-exposure");
            fraction(extremeMaxExposure, "account-risk.extreme-max-exposure");
            positive(maxLeverage, "account-risk.max-leverage");
            positive(freshnessSeconds, "account-risk.freshness-seconds");
            if (lowMaxExposure.compareTo(mediumMaxExposure) < 0
                    || mediumMaxExposure.compareTo(highMaxExposure) < 0
                    || highMaxExposure.compareTo(extremeMaxExposure) < 0) {
                throw new IllegalStateException("account-risk exposure limits must become no more permissive as risk rises");
            }
        }

        public BigDecimal getLowMaxExposure() { return lowMaxExposure; }
        public void setLowMaxExposure(BigDecimal value) { this.lowMaxExposure = value; }
        public BigDecimal getMediumMaxExposure() { return mediumMaxExposure; }
        public void setMediumMaxExposure(BigDecimal value) { this.mediumMaxExposure = value; }
        public BigDecimal getHighMaxExposure() { return highMaxExposure; }
        public void setHighMaxExposure(BigDecimal value) { this.highMaxExposure = value; }
        public BigDecimal getExtremeMaxExposure() { return extremeMaxExposure; }
        public void setExtremeMaxExposure(BigDecimal value) { this.extremeMaxExposure = value; }
        public BigDecimal getMaxLeverage() { return maxLeverage; }
        public void setMaxLeverage(BigDecimal value) { this.maxLeverage = value; }
        public Integer getFreshnessSeconds() { return freshnessSeconds; }
        public void setFreshnessSeconds(Integer value) { this.freshnessSeconds = value; }

        public BigDecimal maxExposureFor(String riskLevel) {
            if (riskLevel == null || riskLevel.isBlank()) return null;
            return switch (riskLevel.trim().toUpperCase(java.util.Locale.ROOT)) {
                case "LOW" -> lowMaxExposure;
                case "MEDIUM" -> mediumMaxExposure;
                case "HIGH" -> highMaxExposure;
                case "EXTREME" -> extremeMaxExposure;
                default -> null;
            };
        }
    }

    public static class ExecutionFeasibility {
        private Integer quoteFreshnessSeconds;
        private BigDecimal maxSpreadBps;
        private BigDecimal minimumTopOfBookNotional;
        private BigDecimal maxEntryDriftBps;

        void validate() {
            positive(quoteFreshnessSeconds, "execution-feasibility.quote-freshness-seconds");
            positive(maxSpreadBps, "execution-feasibility.max-spread-bps");
            positive(minimumTopOfBookNotional,
                    "execution-feasibility.minimum-top-of-book-notional");
            positive(maxEntryDriftBps, "execution-feasibility.max-entry-drift-bps");
        }

        public Integer getQuoteFreshnessSeconds() { return quoteFreshnessSeconds; }
        public void setQuoteFreshnessSeconds(Integer value) { this.quoteFreshnessSeconds = value; }
        public BigDecimal getMaxSpreadBps() { return maxSpreadBps; }
        public void setMaxSpreadBps(BigDecimal value) { this.maxSpreadBps = value; }
        public BigDecimal getMinimumTopOfBookNotional() { return minimumTopOfBookNotional; }
        public void setMinimumTopOfBookNotional(BigDecimal value) {
            this.minimumTopOfBookNotional = value;
        }
        public BigDecimal getMaxEntryDriftBps() { return maxEntryDriftBps; }
        public void setMaxEntryDriftBps(BigDecimal value) { this.maxEntryDriftBps = value; }
    }

    private static void positive(Integer value, String field) {
        if (value == null || value <= 0) throw new IllegalStateException(field + " must be configured above 0");
    }

    private static void positive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) throw new IllegalStateException(field + " must be configured above 0");
    }

    private static void range(Integer value, int min, int max, String field) {
        if (value == null || value < min || value > max) {
            throw new IllegalStateException(field + " must be configured in [" + min + ", " + max + "]");
        }
    }

    private static void fraction(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException(field + " must be configured in (0, 1]");
        }
    }
}
