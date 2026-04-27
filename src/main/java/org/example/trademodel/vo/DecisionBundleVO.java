package org.example.trademodel.vo;

import org.example.trademodel.enums.AssetStateEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class DecisionBundleVO {
    private String decisionId;
    private String marketBiasHierarchy;
    private String tradeType;
    private String confidenceLevel;
    private String riskLevel;
    private String actionPriority;
    private String conclusionSummary;
    private Boolean isWorthOpening;
    private String multiTfConvergence;
    private String aiRoleResults;
    private List<String> supportEvidences;
    private List<String> opposeEvidences;

    /** JSON 数组文本，空为 {@code []} */
    private String reviewReasons = "[]";
    private String aiConflictLevel;
    private Integer aiConflictScore;
    private String aiPlanMode;
    private Integer confusedScore;
    /** 本 run 合成权威状态，仅 {@link AssetStateEnum} */
    private AssetStateEnum assetState;
    /** 与 asset_state_snapshot 列一致，由引擎调用 AssetStateService 写入 */
    private String assetStateSnapshot;

    /** 本 run 多周期是否对齐（与 K 线 1m/5m 同向）；Hot Reset 最小规则直接读取，不解析 multiTfConvergence 字符串 */
    private boolean multiTimeframeAligned = true;

    /** 推送二次校验漂移基准：本 run 最后一根 1m 收盘价（与 K 线事实一致） */
    private BigDecimal pushTriggerPrice;
    /** 推送计划过期时刻（本 run 决策时刻 + 固定 TTL） */
    private LocalDateTime pushExpiresAt;
    /** 结构化失效：现价低于此价则 Recheck 判 INVALIDATION_HIT（看多场景） */
    private BigDecimal pushInvalidPriceBelow;
    /** 结构化失效：现价高于此价则 Recheck 判 INVALIDATION_HIT（看空场景） */
    private BigDecimal pushInvalidPriceAbove;
    /** 失效说明文本，写入 invalidation_condition_json 的 text */
    private String pushInvalidationSummary;

    // 空构造器 + 方便日志打印
    public DecisionBundleVO() {}

    public String getDecisionId() { return decisionId; }
    public void setDecisionId(String decisionId) { this.decisionId = decisionId; }
    public String getMarketBiasHierarchy() { return marketBiasHierarchy; }
    public void setMarketBiasHierarchy(String marketBiasHierarchy) { this.marketBiasHierarchy = marketBiasHierarchy; }
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
}
