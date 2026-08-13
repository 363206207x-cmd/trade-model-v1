package org.example.trademodel.vo;

import org.example.trademodel.enums.AssetStateEnum;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DecisionBundleVO {
    private String decisionId;
    private String marketBiasHierarchy;
    private String ruleMarketBias;
    private String finalMarketBias;
    private String ruleConfidence;
    private String ruleRisk;
    private String rulePlanMode;
    private Boolean ruleCanExecute;
    private String candidatePlanMode;
    private String finalPlanMode;
    private String biasAdjustmentReason;
    private String planModeAdjustmentReason;
    private String tradeType;
    private String confidenceLevel;
    private String riskLevel;
    private String actionPriority;
    private String conclusionSummary;
    private Boolean isWorthOpening;
    private String multiTfConvergence;
    private Map<String, Map<String, Object>> multiTimeframeDetails = new LinkedHashMap<>();
    private String aiRoleResults;
    private List<String> supportEvidences;
    private List<String> opposeEvidences;

    /** JSON 数组文本，空为 {@code []} */
    private String reviewReasons = "[]";
    private String aiConflictLevel;
    private Integer aiConflictScore;
    private String aiPlanMode;
    private Integer confusedScore;
    private Integer confusedLowStreak;
    private boolean directionalPushBlocked;
    private String directionalPushBlockReason;
    /** 本 run 合成权威状态，仅 {@link AssetStateEnum} */
    private AssetStateEnum assetState;
    /** 与 asset_state_snapshot 列一致，由引擎调用 AssetStateService 写入 */
    private String assetStateSnapshot;

    /** 本 run 多周期是否对齐（与 K 线 1m/5m 同向）；Hot Reset 最小规则直接读取，不解析 multiTfConvergence 字符串 */
    private boolean multiTimeframeAligned = true;

    /** 推送二次校验漂移基准：本 run 最后一根 1m 收盘价（与 K 线事实一致） */
    private BigDecimal pushTriggerPrice;
    /**
     * UTC-naive compatibility timestamp for {@code tm_push_snapshot.expires_at}.
     * It must only be produced and consumed with {@link java.time.ZoneOffset#UTC}.
     */
    private LocalDateTime pushExpiresAt;
    /** 权威计划有效起点，始终携带 UTC 偏移。 */
    private OffsetDateTime validFrom;
    /** 权威计划过期时点，始终携带 UTC 偏移。 */
    private OffsetDateTime expiresAt;
    /** 结构化失效：现价低于此价则 Recheck 判 INVALIDATION_HIT（看多场景） */
    private BigDecimal pushInvalidPriceBelow;
    /** 结构化失效：现价高于此价则 Recheck 判 INVALIDATION_HIT（看空场景） */
    private BigDecimal pushInvalidPriceAbove;
    /** 失效说明文本，写入 invalidation_condition_json 的 text */
    private String pushInvalidationSummary;
    private String externalContextStatus;
    private Integer activeExternalEventCount;
    private Integer activeMacroEventCount;
    private Integer activeNewsEventCount;
    private String externalContextRiskLevel;
    private Boolean externalContextBlocked;
    private List<String> externalEventIds;
    private List<String> externalContextReasonCodes;
    private LocalDateTime nextExternalEventTime;
    private LocalDateTime latestExternalEventTime;
    private String latestExternalEventLabel;
    private LocalDateTime externalEventWindowStart;
    private LocalDateTime externalEventWindowEnd;
    private String externalContextSourceHealth;
    private String derivativesStatus;
    private String derivativesFreshness;
    private Boolean derivativesRequired;
    private Boolean derivativesConfirmEligible;
    private String derivativesPushMode;
    private List<String> derivativesReasonCodes = new ArrayList<>();
    private Instant derivativesProviderDataTime;
    private String derivativesTraceId;

    // 空构造器 + 方便日志打印
    public DecisionBundleVO() {}

    public String getDecisionId() { return decisionId; }
    public void setDecisionId(String decisionId) { this.decisionId = decisionId; }
    public String getMarketBiasHierarchy() { return marketBiasHierarchy; }
    public void setMarketBiasHierarchy(String marketBiasHierarchy) { this.marketBiasHierarchy = marketBiasHierarchy; }
    public String getRuleMarketBias() { return ruleMarketBias; }
    public void setRuleMarketBias(String value) { this.ruleMarketBias = value; }
    public String getFinalMarketBias() { return finalMarketBias; }
    public void setFinalMarketBias(String value) { this.finalMarketBias = value; }
    public String getRuleConfidence() { return ruleConfidence; }
    public void setRuleConfidence(String value) { this.ruleConfidence = value; }
    public String getRuleRisk() { return ruleRisk; }
    public void setRuleRisk(String value) { this.ruleRisk = value; }
    public String getRulePlanMode() { return rulePlanMode; }
    public void setRulePlanMode(String value) { this.rulePlanMode = value; }
    public Boolean getRuleCanExecute() { return ruleCanExecute; }
    public void setRuleCanExecute(Boolean value) { this.ruleCanExecute = value; }
    public String getCandidatePlanMode() { return candidatePlanMode; }
    public void setCandidatePlanMode(String value) { this.candidatePlanMode = value; }
    public String getFinalPlanMode() { return finalPlanMode; }
    public void setFinalPlanMode(String value) { this.finalPlanMode = value; }
    public String getBiasAdjustmentReason() { return biasAdjustmentReason; }
    public void setBiasAdjustmentReason(String value) { this.biasAdjustmentReason = value; }
    public String getPlanModeAdjustmentReason() { return planModeAdjustmentReason; }
    public void setPlanModeAdjustmentReason(String value) { this.planModeAdjustmentReason = value; }
    public String getTradeType() { return tradeType; }
    public void setTradeType(String tradeType) { this.tradeType = tradeType; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getActionPriority() { return actionPriority; }
    public void setActionPriority(String actionPriority) { this.actionPriority = actionPriority; }
    public String getConclusionSummary() { return conclusionSummary; }
    public void setConclusionSummary(String conclusionSummary) { this.conclusionSummary = conclusionSummary; }
    public Boolean getIsWorthOpening() { return isWorthOpening; }
    public void setIsWorthOpening(Boolean isWorthOpening) { this.isWorthOpening = isWorthOpening; }
    public String getMultiTfConvergence() { return multiTfConvergence; }
    public void setMultiTfConvergence(String multiTfConvergence) { this.multiTfConvergence = multiTfConvergence; }
    public Map<String, Map<String, Object>> getMultiTimeframeDetails() {
        return new LinkedHashMap<>(multiTimeframeDetails);
    }
    public void setMultiTimeframeDetails(Map<String, Map<String, Object>> value) {
        this.multiTimeframeDetails = value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value);
    }
    public String getAiRoleResults() { return aiRoleResults; }
    public void setAiRoleResults(String aiRoleResults) { this.aiRoleResults = aiRoleResults; }
    public List<String> getSupportEvidences() { return supportEvidences; }
    public void setSupportEvidences(List<String> supportEvidences) { this.supportEvidences = supportEvidences; }
    public List<String> getOpposeEvidences() { return opposeEvidences; }
    public void setOpposeEvidences(List<String> opposeEvidences) { this.opposeEvidences = opposeEvidences; }

    public String getReviewReasons() { return reviewReasons; }
    public void setReviewReasons(String reviewReasons) { this.reviewReasons = reviewReasons; }
    public String getAiConflictLevel() { return aiConflictLevel; }
    public void setAiConflictLevel(String aiConflictLevel) { this.aiConflictLevel = aiConflictLevel; }
    public Integer getAiConflictScore() { return aiConflictScore; }
    public void setAiConflictScore(Integer aiConflictScore) { this.aiConflictScore = aiConflictScore; }
    public String getAiPlanMode() { return aiPlanMode; }
    public void setAiPlanMode(String aiPlanMode) { this.aiPlanMode = aiPlanMode; }
    public Integer getConfusedScore() { return confusedScore; }
    public void setConfusedScore(Integer confusedScore) { this.confusedScore = confusedScore; }
    public Integer getConfusedLowStreak() { return confusedLowStreak; }
    public void setConfusedLowStreak(Integer confusedLowStreak) { this.confusedLowStreak = confusedLowStreak; }
    public boolean isDirectionalPushBlocked() { return directionalPushBlocked; }
    public void setDirectionalPushBlocked(boolean directionalPushBlocked) { this.directionalPushBlocked = directionalPushBlocked; }
    public String getDirectionalPushBlockReason() { return directionalPushBlockReason; }
    public void setDirectionalPushBlockReason(String directionalPushBlockReason) { this.directionalPushBlockReason = directionalPushBlockReason; }
    public AssetStateEnum getAssetState() { return assetState; }
    public void setAssetState(AssetStateEnum assetState) { this.assetState = assetState; }
    public String getAssetStateSnapshot() { return assetStateSnapshot; }
    public void setAssetStateSnapshot(String assetStateSnapshot) { this.assetStateSnapshot = assetStateSnapshot; }

    public boolean isMultiTimeframeAligned() {
        return multiTimeframeAligned;
    }

    public void setMultiTimeframeAligned(boolean multiTimeframeAligned) {
        this.multiTimeframeAligned = multiTimeframeAligned;
    }

    public BigDecimal getPushTriggerPrice() {
        return pushTriggerPrice;
    }

    public void setPushTriggerPrice(BigDecimal pushTriggerPrice) {
        this.pushTriggerPrice = pushTriggerPrice;
    }

    public LocalDateTime getPushExpiresAt() {
        return pushExpiresAt;
    }

    public void setPushExpiresAt(LocalDateTime pushExpiresAt) {
        this.pushExpiresAt = pushExpiresAt;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public BigDecimal getPushInvalidPriceBelow() {
        return pushInvalidPriceBelow;
    }

    public void setPushInvalidPriceBelow(BigDecimal pushInvalidPriceBelow) {
        this.pushInvalidPriceBelow = pushInvalidPriceBelow;
    }

    public BigDecimal getPushInvalidPriceAbove() {
        return pushInvalidPriceAbove;
    }

    public void setPushInvalidPriceAbove(BigDecimal pushInvalidPriceAbove) {
        this.pushInvalidPriceAbove = pushInvalidPriceAbove;
    }

    public String getPushInvalidationSummary() {
        return pushInvalidationSummary;
    }

    public void setPushInvalidationSummary(String pushInvalidationSummary) {
        this.pushInvalidationSummary = pushInvalidationSummary;
    }

    public String getExternalContextStatus() { return externalContextStatus; }
    public void setExternalContextStatus(String externalContextStatus) { this.externalContextStatus = externalContextStatus; }
    public Integer getActiveExternalEventCount() { return activeExternalEventCount; }
    public void setActiveExternalEventCount(Integer activeExternalEventCount) { this.activeExternalEventCount = activeExternalEventCount; }
    public Integer getActiveMacroEventCount() { return activeMacroEventCount; }
    public void setActiveMacroEventCount(Integer activeMacroEventCount) { this.activeMacroEventCount = activeMacroEventCount; }
    public Integer getActiveNewsEventCount() { return activeNewsEventCount; }
    public void setActiveNewsEventCount(Integer activeNewsEventCount) { this.activeNewsEventCount = activeNewsEventCount; }
    public String getExternalContextRiskLevel() { return externalContextRiskLevel; }
    public void setExternalContextRiskLevel(String externalContextRiskLevel) { this.externalContextRiskLevel = externalContextRiskLevel; }
    public Boolean getExternalContextBlocked() { return externalContextBlocked; }
    public void setExternalContextBlocked(Boolean externalContextBlocked) { this.externalContextBlocked = externalContextBlocked; }
    public List<String> getExternalEventIds() { return externalEventIds; }
    public void setExternalEventIds(List<String> externalEventIds) { this.externalEventIds = externalEventIds; }
    public List<String> getExternalContextReasonCodes() { return externalContextReasonCodes; }
    public void setExternalContextReasonCodes(List<String> externalContextReasonCodes) { this.externalContextReasonCodes = externalContextReasonCodes; }
    public LocalDateTime getNextExternalEventTime() { return nextExternalEventTime; }
    public void setNextExternalEventTime(LocalDateTime nextExternalEventTime) { this.nextExternalEventTime = nextExternalEventTime; }
    public LocalDateTime getLatestExternalEventTime() { return latestExternalEventTime; }
    public void setLatestExternalEventTime(LocalDateTime latestExternalEventTime) { this.latestExternalEventTime = latestExternalEventTime; }
    public String getLatestExternalEventLabel() { return latestExternalEventLabel; }
    public void setLatestExternalEventLabel(String latestExternalEventLabel) { this.latestExternalEventLabel = latestExternalEventLabel; }
    public LocalDateTime getExternalEventWindowStart() { return externalEventWindowStart; }
    public void setExternalEventWindowStart(LocalDateTime externalEventWindowStart) { this.externalEventWindowStart = externalEventWindowStart; }
    public LocalDateTime getExternalEventWindowEnd() { return externalEventWindowEnd; }
    public void setExternalEventWindowEnd(LocalDateTime externalEventWindowEnd) { this.externalEventWindowEnd = externalEventWindowEnd; }
    public String getExternalContextSourceHealth() { return externalContextSourceHealth; }
    public void setExternalContextSourceHealth(String externalContextSourceHealth) { this.externalContextSourceHealth = externalContextSourceHealth; }
    public String getDerivativesStatus() { return derivativesStatus; }
    public void setDerivativesStatus(String derivativesStatus) { this.derivativesStatus = derivativesStatus; }
    public String getDerivativesFreshness() { return derivativesFreshness; }
    public void setDerivativesFreshness(String derivativesFreshness) { this.derivativesFreshness = derivativesFreshness; }
    public Boolean getDerivativesRequired() { return derivativesRequired; }
    public void setDerivativesRequired(Boolean derivativesRequired) { this.derivativesRequired = derivativesRequired; }
    public Boolean getDerivativesConfirmEligible() { return derivativesConfirmEligible; }
    public void setDerivativesConfirmEligible(Boolean derivativesConfirmEligible) { this.derivativesConfirmEligible = derivativesConfirmEligible; }
    public String getDerivativesPushMode() { return derivativesPushMode; }
    public void setDerivativesPushMode(String derivativesPushMode) { this.derivativesPushMode = derivativesPushMode; }
    public List<String> getDerivativesReasonCodes() { return List.copyOf(derivativesReasonCodes); }
    public void setDerivativesReasonCodes(List<String> derivativesReasonCodes) {
        this.derivativesReasonCodes = derivativesReasonCodes == null ? new ArrayList<>() : new ArrayList<>(derivativesReasonCodes);
    }
    public Instant getDerivativesProviderDataTime() { return derivativesProviderDataTime; }
    public void setDerivativesProviderDataTime(Instant derivativesProviderDataTime) { this.derivativesProviderDataTime = derivativesProviderDataTime; }
    public String getDerivativesTraceId() { return derivativesTraceId; }
    public void setDerivativesTraceId(String derivativesTraceId) { this.derivativesTraceId = derivativesTraceId; }
}
