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
    private Normalization normalization = new Normalization();
    private ProviderMatrix providerMatrix = new ProviderMatrix();
    private AccountRisk accountRisk = new AccountRisk();
    private ExecutionFeasibility executionFeasibility = new ExecutionFeasibility();

    @PostConstruct
    public void validate() {
        ranking.validate();
        opportunityState.validate();
        aiGate.validate();
        multiTimeframe.validate();
        normalization.validate();
        providerMatrix.validate();
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
    public Normalization getNormalization() { return normalization; }
    public void setNormalization(Normalization value) {
        this.normalization = value == null ? new Normalization() : value;
    }
    public ProviderMatrix getProviderMatrix() { return providerMatrix; }
    public void setProviderMatrix(ProviderMatrix value) {
        this.providerMatrix = value == null ? new ProviderMatrix() : value;
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
        value.ranking.freshnessWeight = new BigDecimal("0.05");
        value.ranking.conflictWeight = new BigDecimal("0.07");
        value.ranking.stabilityWeight = new BigDecimal("0.05");
        value.ranking.directionStrengthWeight = new BigDecimal("0.30");
        value.ranking.finalConfidenceWeight = new BigDecimal("0.25");
        value.ranking.oneHourOpportunityWeight = new BigDecimal("0.20");
        value.ranking.fourHourAlignmentWeight = new BigDecimal("0.10");
        value.ranking.executionFeasibilityWeight = new BigDecimal("0.10");
        value.ranking.replacementThreshold = 5;
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
        value.aiGate.perRunTokenLimit = 24000;
        value.aiGate.perAssetCooldownSeconds = 300;
        value.aiGate.hourlyCallLimit = 180;
        value.aiGate.hourlyTokenLimit = 1440000;
        value.aiGate.dailyCallLimit = 1200;
        value.aiGate.dailyTokenLimit = 9600000;
        value.aiGate.dailyCostMicrosLimit = 100000000L;
        value.aiGate.concurrencyLimit = 3;
        value.aiGate.maxRetryPerRole = 1;
        value.multiTimeframe.fourHourWeight = new BigDecimal("0.57");
        value.multiTimeframe.oneHourWeight = new BigDecimal("0.43");
        value.multiTimeframe.fifteenMinuteWeight = BigDecimal.ZERO;
        value.multiTimeframe.fiveMinuteWeight = BigDecimal.ZERO;
        value.multiTimeframe.minimumAlignedCount = 2;
        value.multiTimeframe.minimumAlignedWeight = BigDecimal.ONE;
        value.multiTimeframe.maximumTrendScoreDifference = new BigDecimal("15");
        value.normalization.version = "V41-NORM-WREP-1";
        value.normalization.lookback = 200;
        value.normalization.minimumSampleCount = 60;
        value.normalization.winsorizeLowerPercentile = new BigDecimal("2.5");
        value.normalization.winsorizeUpperPercentile = new BigDecimal("97.5");
        value.providerMatrix.version = "V41-PROVIDER-MATRIX-1";
        value.providerMatrix.ohlcvRequirement = "MANDATORY";
        value.providerMatrix.derivativesRequirement = "OPTIONAL";
        value.providerMatrix.fiveMinuteTtlSeconds = 600;
        value.providerMatrix.fifteenMinuteTtlSeconds = 1800;
        value.providerMatrix.oneHourTtlSeconds = 7200;
        value.providerMatrix.fourHourTtlSeconds = 28800;
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
        private BigDecimal directionStrengthWeight;
        private BigDecimal finalConfidenceWeight;
        private BigDecimal oneHourOpportunityWeight;
        private BigDecimal fourHourAlignmentWeight;
        private BigDecimal executionFeasibilityWeight;
        private Integer replacementThreshold;

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
            positive(directionStrengthWeight, "ranking.direction-strength-weight");
            positive(finalConfidenceWeight, "ranking.final-confidence-weight");
            positive(oneHourOpportunityWeight, "ranking.one-hour-opportunity-weight");
            positive(fourHourAlignmentWeight, "ranking.four-hour-alignment-weight");
            positive(executionFeasibilityWeight, "ranking.execution-feasibility-weight");
            positive(replacementThreshold, "ranking.replacement-threshold");
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
        public BigDecimal getDirectionStrengthWeight() { return directionStrengthWeight; }
        public void setDirectionStrengthWeight(BigDecimal value) { this.directionStrengthWeight = value; }
        public BigDecimal getFinalConfidenceWeight() { return finalConfidenceWeight; }
        public void setFinalConfidenceWeight(BigDecimal value) { this.finalConfidenceWeight = value; }
        public BigDecimal getOneHourOpportunityWeight() { return oneHourOpportunityWeight; }
        public void setOneHourOpportunityWeight(BigDecimal value) { this.oneHourOpportunityWeight = value; }
        public BigDecimal getFourHourAlignmentWeight() { return fourHourAlignmentWeight; }
        public void setFourHourAlignmentWeight(BigDecimal value) { this.fourHourAlignmentWeight = value; }
        public BigDecimal getExecutionFeasibilityWeight() { return executionFeasibilityWeight; }
        public void setExecutionFeasibilityWeight(BigDecimal value) { this.executionFeasibilityWeight = value; }
        public Integer getReplacementThreshold() { return replacementThreshold; }
        public void setReplacementThreshold(Integer value) { this.replacementThreshold = value; }
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
        private Integer perRunTokenLimit;
        private Integer perAssetCooldownSeconds;
        private Integer hourlyCallLimit;
        private Integer hourlyTokenLimit;
        private Integer dailyCallLimit;
        private Integer dailyTokenLimit;
        private Long dailyCostMicrosLimit;
        private Integer concurrencyLimit;
        private Integer maxRetryPerRole;

        void validate() {
            range(minimumDataQuality, 0, 100, "ai-gate.minimum-data-quality");
            range(circuitBreakerScore, 0, 100, "ai-gate.circuit-breaker-score");
            range(minimumSignificantEvidenceStrength, 0, 100,
                    "ai-gate.minimum-significant-evidence-strength");
            positive(cacheTtlSeconds, "ai-gate.cache-ttl-seconds");
            positive(cacheMaxEntries, "ai-gate.cache-max-entries");
            positive(perRunTokenLimit, "ai-gate.per-run-token-limit");
            positive(perAssetCooldownSeconds, "ai-gate.per-asset-cooldown-seconds");
            positive(hourlyCallLimit, "ai-gate.hourly-call-limit");
            positive(hourlyTokenLimit, "ai-gate.hourly-token-limit");
            positive(dailyCallLimit, "ai-gate.daily-call-limit");
            positive(dailyTokenLimit, "ai-gate.daily-token-limit");
            if (dailyCostMicrosLimit == null || dailyCostMicrosLimit <= 0) {
                throw new IllegalStateException("ai-gate.daily-cost-micros-limit must be configured above 0");
            }
            positive(concurrencyLimit, "ai-gate.concurrency-limit");
            if (maxRetryPerRole == null || maxRetryPerRole < 0 || maxRetryPerRole > 1) {
                throw new IllegalStateException("ai-gate.max-retry-per-role must be in [0, 1]");
            }
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
        public Integer getPerRunTokenLimit() { return perRunTokenLimit; }
        public void setPerRunTokenLimit(Integer value) { this.perRunTokenLimit = value; }
        public Integer getPerAssetCooldownSeconds() { return perAssetCooldownSeconds; }
        public void setPerAssetCooldownSeconds(Integer value) { this.perAssetCooldownSeconds = value; }
        public Integer getHourlyCallLimit() { return hourlyCallLimit; }
        public void setHourlyCallLimit(Integer value) { this.hourlyCallLimit = value; }
        public Integer getHourlyTokenLimit() { return hourlyTokenLimit; }
        public void setHourlyTokenLimit(Integer value) { this.hourlyTokenLimit = value; }
        public Integer getDailyCallLimit() { return dailyCallLimit; }
        public void setDailyCallLimit(Integer value) { this.dailyCallLimit = value; }
        public Integer getDailyTokenLimit() { return dailyTokenLimit; }
        public void setDailyTokenLimit(Integer value) { this.dailyTokenLimit = value; }
        public Long getDailyCostMicrosLimit() { return dailyCostMicrosLimit; }
        public void setDailyCostMicrosLimit(Long value) { this.dailyCostMicrosLimit = value; }
        public Integer getConcurrencyLimit() { return concurrencyLimit; }
        public void setConcurrencyLimit(Integer value) { this.concurrencyLimit = value; }
        public Integer getMaxRetryPerRole() { return maxRetryPerRole; }
        public void setMaxRetryPerRole(Integer value) { this.maxRetryPerRole = value; }
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
            nonNegative(fifteenMinuteWeight, "multi-timeframe.fifteen-minute-weight");
            nonNegative(fiveMinuteWeight, "multi-timeframe.five-minute-weight");
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

    public static class Normalization {
        private String version;
        private Integer lookback;
        private Integer minimumSampleCount;
        private BigDecimal winsorizeLowerPercentile;
        private BigDecimal winsorizeUpperPercentile;

        void validate() {
            if (version == null || version.isBlank()) throw new IllegalStateException("normalization.version is required");
            positive(lookback, "normalization.lookback");
            positive(minimumSampleCount, "normalization.minimum-sample-count");
            if (minimumSampleCount > lookback) throw new IllegalStateException("normalization minimum sample exceeds lookback");
            nonNegative(winsorizeLowerPercentile, "normalization.winsorize-lower-percentile");
            positive(winsorizeUpperPercentile, "normalization.winsorize-upper-percentile");
            if (winsorizeLowerPercentile.compareTo(winsorizeUpperPercentile) >= 0
                    || winsorizeUpperPercentile.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalStateException("normalization winsor percentiles are invalid");
            }
        }
        public String getVersion() { return version; }
        public void setVersion(String value) { this.version = value; }
        public Integer getLookback() { return lookback; }
        public void setLookback(Integer value) { this.lookback = value; }
        public Integer getMinimumSampleCount() { return minimumSampleCount; }
        public void setMinimumSampleCount(Integer value) { this.minimumSampleCount = value; }
        public BigDecimal getWinsorizeLowerPercentile() { return winsorizeLowerPercentile; }
        public void setWinsorizeLowerPercentile(BigDecimal value) { this.winsorizeLowerPercentile = value; }
        public BigDecimal getWinsorizeUpperPercentile() { return winsorizeUpperPercentile; }
        public void setWinsorizeUpperPercentile(BigDecimal value) { this.winsorizeUpperPercentile = value; }
    }

    public static class ProviderMatrix {
        private String version;
        private String ohlcvRequirement;
        private String derivativesRequirement;
        private Integer fiveMinuteTtlSeconds;
        private Integer fifteenMinuteTtlSeconds;
        private Integer oneHourTtlSeconds;
        private Integer fourHourTtlSeconds;

        void validate() {
            if (version == null || version.isBlank()) throw new IllegalStateException("provider-matrix.version is required");
            if (!"MANDATORY".equals(ohlcvRequirement)) throw new IllegalStateException("OHLCV must be mandatory");
            if (!java.util.Set.of("OPTIONAL", "MANDATORY", "NOT_APPLICABLE").contains(derivativesRequirement)) {
                throw new IllegalStateException("provider-matrix.derivatives-requirement is invalid");
            }
            positive(fiveMinuteTtlSeconds, "provider-matrix.five-minute-ttl-seconds");
            positive(fifteenMinuteTtlSeconds, "provider-matrix.fifteen-minute-ttl-seconds");
            positive(oneHourTtlSeconds, "provider-matrix.one-hour-ttl-seconds");
            positive(fourHourTtlSeconds, "provider-matrix.four-hour-ttl-seconds");
        }
        public String getVersion() { return version; }
        public void setVersion(String value) { this.version = value; }
        public String getOhlcvRequirement() { return ohlcvRequirement; }
        public void setOhlcvRequirement(String value) { this.ohlcvRequirement = value; }
        public String getDerivativesRequirement() { return derivativesRequirement; }
        public void setDerivativesRequirement(String value) { this.derivativesRequirement = value; }
        public Integer getFiveMinuteTtlSeconds() { return fiveMinuteTtlSeconds; }
        public void setFiveMinuteTtlSeconds(Integer value) { this.fiveMinuteTtlSeconds = value; }
        public Integer getFifteenMinuteTtlSeconds() { return fifteenMinuteTtlSeconds; }
        public void setFifteenMinuteTtlSeconds(Integer value) { this.fifteenMinuteTtlSeconds = value; }
        public Integer getOneHourTtlSeconds() { return oneHourTtlSeconds; }
        public void setOneHourTtlSeconds(Integer value) { this.oneHourTtlSeconds = value; }
        public Integer getFourHourTtlSeconds() { return fourHourTtlSeconds; }
        public void setFourHourTtlSeconds(Integer value) { this.fourHourTtlSeconds = value; }
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

    private static void nonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) throw new IllegalStateException(field + " must be configured at or above 0");
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
