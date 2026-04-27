package org.example.trademodel.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 复盘聚合响应：一次拉取某次分析的运行、决策、计划、Push/Recheck、Missed、Hot Reset、告警等摘要。
 * <p>
 * 另含与 Dashboard detail 同源的 {@code evidenceTopItems}（{@code tm_evidence_item} top3）、{@code scoreTopItems}（{@code tm_score_item} top3 brief，字段同 {@link ScoreBriefVO}）；
 * Decision 中仍含 evidenceSummary、explanationJson、reviewReasons、assetStateSnapshot 等摘要/原样字段（evidenceSummary 为决策摘要文本，与 evidenceTopItems 并列、不互替）。
 * Push/Recheck 下的 JSON 型字段为库表原样字符串，供展示用，本 VO 不做结构化解析。
 * {@link ReviewHotResetSummary}：来自 {@code tm_asset_state} 按「本 run 的 symbol」的当前行，含最近一次写入的 hot_reset_*；非按 analysisId 存档的独立事件流（见该内嵌类字段说明）。
 * 用户可编辑的复盘结论在 {@code tm_review_result}，经 {@code /api/review/state} 与 {@code /api/review/save}，不在此对象内。
 */
public class ReviewAggregateVO {

    private ReviewClosureSummary reviewClosure;
    private GovernanceSummary governanceSummary;
    private ReviewRunSummary run;
    private ReviewDecisionSummary decision;
    private ReviewPlanSummary plan;
    private List<ReviewPushWithRecheck> pushRecheck;
    private List<RuleVersionLogSummary> ruleVersionLogs;
    private ReviewMarketEnvironmentSummary marketEnvironment;
    /** 与 Dashboard detail 同源：当前 analysis 下 tm_evidence_item 的 top3 brief（无则空列表）。 */
    private List<EvidenceBriefVO> evidenceTopItems;
    /** 与 Dashboard detail 同源：当前 analysis 下 tm_score_item 的 top3 brief（无则空列表）。 */
    private List<ScoreBriefVO> scoreTopItems;
    private List<ReviewMissedSummary> missed;
    private ReviewHotResetSummary hotReset;
    private List<ReviewAlertSummary> alerts;

    public ReviewRunSummary getRun() {
        return run;
    }

    public GovernanceSummary getGovernanceSummary() {
        return governanceSummary;
    }

    public void setGovernanceSummary(GovernanceSummary governanceSummary) {
        this.governanceSummary = governanceSummary;
    }

    public ReviewClosureSummary getReviewClosure() {
        return reviewClosure;
    }

    public void setReviewClosure(ReviewClosureSummary reviewClosure) {
        this.reviewClosure = reviewClosure;
    }

    public void setRun(ReviewRunSummary run) {
        this.run = run;
    }

    public ReviewDecisionSummary getDecision() {
        return decision;
    }

    public void setDecision(ReviewDecisionSummary decision) {
        this.decision = decision;
    }

    public ReviewPlanSummary getPlan() {
        return plan;
    }

    public void setPlan(ReviewPlanSummary plan) {
        this.plan = plan;
    }

    public List<ReviewPushWithRecheck> getPushRecheck() {
        return pushRecheck;
    }

    public void setPushRecheck(List<ReviewPushWithRecheck> pushRecheck) {
        this.pushRecheck = pushRecheck;
    }

    public List<ReviewMissedSummary> getMissed() {
        return missed;
    }

    public void setMissed(List<ReviewMissedSummary> missed) {
        this.missed = missed;
    }

    public List<RuleVersionLogSummary> getRuleVersionLogs() {
        return ruleVersionLogs;
    }

    public void setRuleVersionLogs(List<RuleVersionLogSummary> ruleVersionLogs) {
        this.ruleVersionLogs = ruleVersionLogs;
    }

    public ReviewMarketEnvironmentSummary getMarketEnvironment() {
        return marketEnvironment;
    }

    public void setMarketEnvironment(ReviewMarketEnvironmentSummary marketEnvironment) {
        this.marketEnvironment = marketEnvironment;
    }

    public List<EvidenceBriefVO> getEvidenceTopItems() {
        return evidenceTopItems;
    }

    public void setEvidenceTopItems(List<EvidenceBriefVO> evidenceTopItems) {
        this.evidenceTopItems = evidenceTopItems;
    }

    public List<ScoreBriefVO> getScoreTopItems() {
        return scoreTopItems;
    }

    public void setScoreTopItems(List<ScoreBriefVO> scoreTopItems) {
        this.scoreTopItems = scoreTopItems;
    }

    public ReviewHotResetSummary getHotReset() {
        return hotReset;
    }

    public void setHotReset(ReviewHotResetSummary hotReset) {
        this.hotReset = hotReset;
    }

    public List<ReviewAlertSummary> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<ReviewAlertSummary> alerts) {
        this.alerts = alerts;
    }

    public static class ReviewClosureSummary {
        private String stageLabel;
        private String decisionConclusion;
        private String executionHeadline;
        private List<String> deviationSignals;
        private List<String> deviationSourceTags;
        private ReviewCompletionSummary reviewCompletion;
        private String entryGuidance;
        private List<String> nextFocus;
        private List<ReviewFactRef> keyFacts;

        public String getStageLabel() {
            return stageLabel;
        }

        public void setStageLabel(String stageLabel) {
            this.stageLabel = stageLabel;
        }

        public String getDecisionConclusion() {
            return decisionConclusion;
        }

        public void setDecisionConclusion(String decisionConclusion) {
            this.decisionConclusion = decisionConclusion;
        }

        public String getExecutionHeadline() {
            return executionHeadline;
        }

        public void setExecutionHeadline(String executionHeadline) {
            this.executionHeadline = executionHeadline;
        }

        public List<String> getDeviationSignals() {
            return deviationSignals;
        }

        public void setDeviationSignals(List<String> deviationSignals) {
            this.deviationSignals = deviationSignals;
        }

        public List<String> getDeviationSourceTags() {
            return deviationSourceTags;
        }

        public void setDeviationSourceTags(List<String> deviationSourceTags) {
            this.deviationSourceTags = deviationSourceTags;
        }

        public ReviewCompletionSummary getReviewCompletion() {
            return reviewCompletion;
        }

        public void setReviewCompletion(ReviewCompletionSummary reviewCompletion) {
            this.reviewCompletion = reviewCompletion;
        }

        public String getEntryGuidance() {
            return entryGuidance;
        }

        public void setEntryGuidance(String entryGuidance) {
            this.entryGuidance = entryGuidance;
        }

        public List<String> getNextFocus() {
            return nextFocus;
        }

        public void setNextFocus(List<String> nextFocus) {
            this.nextFocus = nextFocus;
        }

        public List<ReviewFactRef> getKeyFacts() {
            return keyFacts;
        }

        public void setKeyFacts(List<ReviewFactRef> keyFacts) {
            this.keyFacts = keyFacts;
        }
    }

    public static class ReviewCompletionSummary {
        private String status;
        private Boolean completed;
        private String summary;
        private LocalDateTime updateTime;
        private Boolean hasContent;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Boolean getCompleted() {
            return completed;
        }

        public void setCompleted(Boolean completed) {
            this.completed = completed;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
        }

        public Boolean getHasContent() {
            return hasContent;
        }

        public void setHasContent(Boolean hasContent) {
            this.hasContent = hasContent;
        }
    }

    public static class ReviewFactRef {
        private String label;
        private String anchor;
        private String reason;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getAnchor() {
            return anchor;
        }

        public void setAnchor(String anchor) {
            this.anchor = anchor;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class GovernanceSummary {
        private String governanceStatus;
        private String governanceActionHint;
        private Boolean hasReviewContent;
        private String primaryIssueType;
        private LocalDateTime latestReviewUpdatedAt;
        private String linkedRuleLogId;
        private String linkedRuleLogCreatedAt;
        private String linkedRuleLogChangeCategory;

        public String getGovernanceStatus() {
            return governanceStatus;
        }

        public void setGovernanceStatus(String governanceStatus) {
            this.governanceStatus = governanceStatus;
        }

        public String getGovernanceActionHint() {
            return governanceActionHint;
        }

        public void setGovernanceActionHint(String governanceActionHint) {
            this.governanceActionHint = governanceActionHint;
        }

        public Boolean getHasReviewContent() {
            return hasReviewContent;
        }

        public void setHasReviewContent(Boolean hasReviewContent) {
            this.hasReviewContent = hasReviewContent;
        }

        public String getPrimaryIssueType() {
            return primaryIssueType;
        }

        public void setPrimaryIssueType(String primaryIssueType) {
            this.primaryIssueType = primaryIssueType;
        }

        public LocalDateTime getLatestReviewUpdatedAt() {
            return latestReviewUpdatedAt;
        }

        public void setLatestReviewUpdatedAt(LocalDateTime latestReviewUpdatedAt) {
            this.latestReviewUpdatedAt = latestReviewUpdatedAt;
        }

        public String getLinkedRuleLogId() {
            return linkedRuleLogId;
        }

        public void setLinkedRuleLogId(String linkedRuleLogId) {
            this.linkedRuleLogId = linkedRuleLogId;
        }

        public String getLinkedRuleLogCreatedAt() {
            return linkedRuleLogCreatedAt;
        }

        public void setLinkedRuleLogCreatedAt(String linkedRuleLogCreatedAt) {
            this.linkedRuleLogCreatedAt = linkedRuleLogCreatedAt;
        }

        public String getLinkedRuleLogChangeCategory() {
            return linkedRuleLogChangeCategory;
        }

        public void setLinkedRuleLogChangeCategory(String linkedRuleLogChangeCategory) {
            this.linkedRuleLogChangeCategory = linkedRuleLogChangeCategory;
        }
    }

    public static class ReviewRunSummary {
        private String analysisId;
        private String symbol;
        private String timeframe;
        private LocalDateTime analysisTime;
        private String ruleVersion;
        private Integer dataQualityScore;
        private String traceId;
        private String status;

        public String getAnalysisId() {
            return analysisId;
        }

        public void setAnalysisId(String analysisId) {
            this.analysisId = analysisId;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getTimeframe() {
            return timeframe;
        }

        public void setTimeframe(String timeframe) {
            this.timeframe = timeframe;
        }

        public LocalDateTime getAnalysisTime() {
            return analysisTime;
        }

        public void setAnalysisTime(LocalDateTime analysisTime) {
            this.analysisTime = analysisTime;
        }

        public String getRuleVersion() {
            return ruleVersion;
        }

        public void setRuleVersion(String ruleVersion) {
            this.ruleVersion = ruleVersion;
        }

        public Integer getDataQualityScore() {
            return dataQualityScore;
        }

        public void setDataQualityScore(Integer dataQualityScore) {
            this.dataQualityScore = dataQualityScore;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class ReviewDecisionSummary {
        private String decisionId;
        private String symbol;
        private String marketBiasHierarchy;
        private String tradeType;
        private String confidenceLevel;
        private String riskLevel;
        private String actionPriority;
        private String conclusionSummary;
        private Boolean isWorthOpening;
        private String multiTfConvergence;
        private Boolean isAdopted;
        private String validPeriod;
        private String invalidCondition;
        private String evidenceSummary;
        /** 决策解释 JSON（库表原样；展示端可做最小格式化）。 */
        private String explanationJson;
        /** 复核原因，通常为 JSON 数组字符串（库表原样）。 */
        private String reviewReasons;
        private String aiConflictLevel;
        private Integer aiConflictScore;
        private String aiPlanMode;
        private Integer confusedScore;
        private String assetStateSnapshot;
        private LocalDateTime createTime;

        public String getDecisionId() {
            return decisionId;
        }

        public void setDecisionId(String decisionId) {
            this.decisionId = decisionId;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getMarketBiasHierarchy() {
            return marketBiasHierarchy;
        }

        public void setMarketBiasHierarchy(String marketBiasHierarchy) {
            this.marketBiasHierarchy = marketBiasHierarchy;
        }

        public String getTradeType() {
            return tradeType;
        }

        public void setTradeType(String tradeType) {
            this.tradeType = tradeType;
        }

        public String getConfidenceLevel() {
            return confidenceLevel;
        }

        public void setConfidenceLevel(String confidenceLevel) {
            this.confidenceLevel = confidenceLevel;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public String getActionPriority() {
            return actionPriority;
        }

        public void setActionPriority(String actionPriority) {
            this.actionPriority = actionPriority;
        }

        public String getConclusionSummary() {
            return conclusionSummary;
        }

        public void setConclusionSummary(String conclusionSummary) {
            this.conclusionSummary = conclusionSummary;
        }

        public Boolean getIsWorthOpening() {
            return isWorthOpening;
        }

        public void setIsWorthOpening(Boolean worthOpening) {
            isWorthOpening = worthOpening;
        }

        public String getMultiTfConvergence() {
            return multiTfConvergence;
        }

        public void setMultiTfConvergence(String multiTfConvergence) {
            this.multiTfConvergence = multiTfConvergence;
        }

        public Boolean getIsAdopted() {
            return isAdopted;
        }

        public void setIsAdopted(Boolean adopted) {
            isAdopted = adopted;
        }

        public String getValidPeriod() {
            return validPeriod;
        }

        public void setValidPeriod(String validPeriod) {
            this.validPeriod = validPeriod;
        }

        public String getInvalidCondition() {
            return invalidCondition;
        }

        public void setInvalidCondition(String invalidCondition) {
            this.invalidCondition = invalidCondition;
        }

        public String getEvidenceSummary() {
            return evidenceSummary;
        }

        public void setEvidenceSummary(String evidenceSummary) {
            this.evidenceSummary = evidenceSummary;
        }

        public String getExplanationJson() {
            return explanationJson;
        }

        public void setExplanationJson(String explanationJson) {
            this.explanationJson = explanationJson;
        }

        public String getReviewReasons() {
            return reviewReasons;
        }

        public void setReviewReasons(String reviewReasons) {
            this.reviewReasons = reviewReasons;
        }

        public String getAiConflictLevel() {
            return aiConflictLevel;
        }

        public void setAiConflictLevel(String aiConflictLevel) {
            this.aiConflictLevel = aiConflictLevel;
        }

        public Integer getAiConflictScore() {
            return aiConflictScore;
        }

        public void setAiConflictScore(Integer aiConflictScore) {
            this.aiConflictScore = aiConflictScore;
        }

        public String getAiPlanMode() {
            return aiPlanMode;
        }

        public void setAiPlanMode(String aiPlanMode) {
            this.aiPlanMode = aiPlanMode;
        }

        public Integer getConfusedScore() {
            return confusedScore;
        }

        public void setConfusedScore(Integer confusedScore) {
            this.confusedScore = confusedScore;
        }

        public String getAssetStateSnapshot() {
            return assetStateSnapshot;
        }

        public void setAssetStateSnapshot(String assetStateSnapshot) {
            this.assetStateSnapshot = assetStateSnapshot;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }
    }

    public static class ReviewPlanSummary {
        private String planId;
        private String planMode;
        private String recommendedAction;
        private String entryZone;
        private String stopLoss;
        private String takeProfitRules;
        private String leverageSuggestion;
        private String positionSuggestion;
        private LocalDateTime createTime;

        public String getPlanId() {
            return planId;
        }

        public void setPlanId(String planId) {
            this.planId = planId;
        }

        public String getPlanMode() {
            return planMode;
        }

        public void setPlanMode(String planMode) {
            this.planMode = planMode;
        }

        public String getRecommendedAction() {
            return recommendedAction;
        }

        public void setRecommendedAction(String recommendedAction) {
            this.recommendedAction = recommendedAction;
        }

        public String getEntryZone() {
            return entryZone;
        }

        public void setEntryZone(String entryZone) {
            this.entryZone = entryZone;
        }

        public String getStopLoss() {
            return stopLoss;
        }

        public void setStopLoss(String stopLoss) {
            this.stopLoss = stopLoss;
        }

        public String getTakeProfitRules() {
            return takeProfitRules;
        }

        public void setTakeProfitRules(String takeProfitRules) {
            this.takeProfitRules = takeProfitRules;
        }

        public String getLeverageSuggestion() {
            return leverageSuggestion;
        }

        public void setLeverageSuggestion(String leverageSuggestion) {
            this.leverageSuggestion = leverageSuggestion;
        }

        public String getPositionSuggestion() {
            return positionSuggestion;
        }

        public void setPositionSuggestion(String positionSuggestion) {
            this.positionSuggestion = positionSuggestion;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }
    }

    public static class ReviewPushWithRecheck {
        private ReviewPushSummary push;
        private List<ReviewRecheckSummary> rechecks;

        public ReviewPushSummary getPush() {
            return push;
        }

        public void setPush(ReviewPushSummary push) {
            this.push = push;
        }

        public List<ReviewRecheckSummary> getRechecks() {
            return rechecks;
        }

        public void setRechecks(List<ReviewRecheckSummary> rechecks) {
            this.rechecks = rechecks;
        }
    }

    public static class RuleVersionLogSummary {
        private String id;
        private String analysisId;
        private String ruleVersion;
        private String errorType;
        private String changeCategory;
        private String changeSummary;
        private String changeDetail;
        private String operator;
        private String rollbackFlag;
        private String createdAt;
        private Boolean fallbackMatched;
        private Boolean linkedToLatestReview;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAnalysisId() {
            return analysisId;
        }

        public void setAnalysisId(String analysisId) {
            this.analysisId = analysisId;
        }

        public String getRuleVersion() {
            return ruleVersion;
        }

        public void setRuleVersion(String ruleVersion) {
            this.ruleVersion = ruleVersion;
        }

        public String getErrorType() {
            return errorType;
        }

        public void setErrorType(String errorType) {
            this.errorType = errorType;
        }

        public String getChangeCategory() {
            return changeCategory;
        }

        public void setChangeCategory(String changeCategory) {
            this.changeCategory = changeCategory;
        }

        public String getChangeSummary() {
            return changeSummary;
        }

        public void setChangeSummary(String changeSummary) {
            this.changeSummary = changeSummary;
        }

        public String getChangeDetail() {
            return changeDetail;
        }

        public void setChangeDetail(String changeDetail) {
            this.changeDetail = changeDetail;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public String getRollbackFlag() {
            return rollbackFlag;
        }

        public void setRollbackFlag(String rollbackFlag) {
            this.rollbackFlag = rollbackFlag;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public Boolean getFallbackMatched() {
            return fallbackMatched;
        }

        public void setFallbackMatched(Boolean fallbackMatched) {
            this.fallbackMatched = fallbackMatched;
        }

        public Boolean getLinkedToLatestReview() {
            return linkedToLatestReview;
        }

        public void setLinkedToLatestReview(Boolean linkedToLatestReview) {
            this.linkedToLatestReview = linkedToLatestReview;
        }
    }

    public static class ReviewMarketEnvironmentSummary {
        private String summary;
        private String sourceType;
        private String environmentType;
        private String riskMode;

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public String getEnvironmentType() {
            return environmentType;
        }

        public void setEnvironmentType(String environmentType) {
            this.environmentType = environmentType;
        }

        public String getRiskMode() {
            return riskMode;
        }

        public void setRiskMode(String riskMode) {
            this.riskMode = riskMode;
        }
    }

    /**
     * Push 快照摘要；{@code entryZoneJson}、{@code stopZoneJson}、{@code invalidationConditionJson} 为表中原样 JSON 文本。
     */
    public static class ReviewPushSummary {
        private Long pushId;
        private String analysisId;
        private String symbol;
        private String timeframe;
        private String pushType;
        private String pushStatus;
        private LocalDateTime pushCreateTime;
        private String ruleVersion;
        private BigDecimal triggerPrice;
        /** 入场区 JSON（原样字符串）。 */
        private String entryZoneJson;
        /** 止损区 JSON（原样字符串）。 */
        private String stopZoneJson;
        /** 失效条件 JSON（原样字符串）。 */
        private String invalidationConditionJson;
        private String planModeSnapshot;
        private String causeEffectAlignmentSnapshot;
        private Integer executionFeasibilitySnapshot;
        private Integer dataQualityScoreSnapshot;
        private Integer confusedScoreSnapshot;
        private Long accountRiskSnapshotId;
        private Boolean accountRiskAllowed;
        private String riskLevelSnapshot;
        private String riskReasonCode;
        private String riskReasonText;
        private BigDecimal positionExposure;
        private BigDecimal maxAllowedExposure;
        private String snapshotSource;
        private Integer snapshotVersion;
        private LocalDateTime expiresAt;
        private String traceId;
        private LocalDateTime createTime;

        public Long getPushId() {
            return pushId;
        }

        public void setPushId(Long pushId) {
            this.pushId = pushId;
        }

        public String getAnalysisId() {
            return analysisId;
        }

        public void setAnalysisId(String analysisId) {
            this.analysisId = analysisId;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getTimeframe() {
            return timeframe;
        }

        public void setTimeframe(String timeframe) {
            this.timeframe = timeframe;
        }

        public String getPushType() {
            return pushType;
        }

        public void setPushType(String pushType) {
            this.pushType = pushType;
        }

        public String getPushStatus() {
            return pushStatus;
        }

        public void setPushStatus(String pushStatus) {
            this.pushStatus = pushStatus;
        }

        public LocalDateTime getPushCreateTime() {
            return pushCreateTime;
        }

        public void setPushCreateTime(LocalDateTime pushCreateTime) {
            this.pushCreateTime = pushCreateTime;
        }

        public String getRuleVersion() {
            return ruleVersion;
        }

        public void setRuleVersion(String ruleVersion) {
            this.ruleVersion = ruleVersion;
        }

        public BigDecimal getTriggerPrice() {
            return triggerPrice;
        }

        public void setTriggerPrice(BigDecimal triggerPrice) {
            this.triggerPrice = triggerPrice;
        }

        public String getEntryZoneJson() {
            return entryZoneJson;
        }

        public void setEntryZoneJson(String entryZoneJson) {
            this.entryZoneJson = entryZoneJson;
        }

        public String getStopZoneJson() {
            return stopZoneJson;
        }

        public void setStopZoneJson(String stopZoneJson) {
            this.stopZoneJson = stopZoneJson;
        }

        public String getInvalidationConditionJson() {
            return invalidationConditionJson;
        }

        public void setInvalidationConditionJson(String invalidationConditionJson) {
            this.invalidationConditionJson = invalidationConditionJson;
        }

        public String getPlanModeSnapshot() {
            return planModeSnapshot;
        }

        public void setPlanModeSnapshot(String planModeSnapshot) {
            this.planModeSnapshot = planModeSnapshot;
        }

        public String getCauseEffectAlignmentSnapshot() {
            return causeEffectAlignmentSnapshot;
        }

        public void setCauseEffectAlignmentSnapshot(String causeEffectAlignmentSnapshot) {
            this.causeEffectAlignmentSnapshot = causeEffectAlignmentSnapshot;
        }

        public Integer getExecutionFeasibilitySnapshot() {
            return executionFeasibilitySnapshot;
        }

        public void setExecutionFeasibilitySnapshot(Integer executionFeasibilitySnapshot) {
            this.executionFeasibilitySnapshot = executionFeasibilitySnapshot;
        }

        public Integer getDataQualityScoreSnapshot() {
            return dataQualityScoreSnapshot;
        }

        public void setDataQualityScoreSnapshot(Integer dataQualityScoreSnapshot) {
            this.dataQualityScoreSnapshot = dataQualityScoreSnapshot;
        }

        public Integer getConfusedScoreSnapshot() {
            return confusedScoreSnapshot;
        }

        public void setConfusedScoreSnapshot(Integer confusedScoreSnapshot) {
            this.confusedScoreSnapshot = confusedScoreSnapshot;
        }

        public Long getAccountRiskSnapshotId() {
            return accountRiskSnapshotId;
        }

        public void setAccountRiskSnapshotId(Long accountRiskSnapshotId) {
            this.accountRiskSnapshotId = accountRiskSnapshotId;
        }

        public Boolean getAccountRiskAllowed() {
            return accountRiskAllowed;
        }

        public void setAccountRiskAllowed(Boolean accountRiskAllowed) {
            this.accountRiskAllowed = accountRiskAllowed;
        }

        public String getRiskLevelSnapshot() {
            return riskLevelSnapshot;
        }

        public void setRiskLevelSnapshot(String riskLevelSnapshot) {
            this.riskLevelSnapshot = riskLevelSnapshot;
        }

        public String getRiskReasonCode() {
            return riskReasonCode;
        }

        public void setRiskReasonCode(String riskReasonCode) {
            this.riskReasonCode = riskReasonCode;
        }

        public String getRiskReasonText() {
            return riskReasonText;
        }

        public void setRiskReasonText(String riskReasonText) {
            this.riskReasonText = riskReasonText;
        }

        public BigDecimal getPositionExposure() {
            return positionExposure;
        }

        public void setPositionExposure(BigDecimal positionExposure) {
            this.positionExposure = positionExposure;
        }

        public BigDecimal getMaxAllowedExposure() {
            return maxAllowedExposure;
        }

        public void setMaxAllowedExposure(BigDecimal maxAllowedExposure) {
            this.maxAllowedExposure = maxAllowedExposure;
        }

        public String getSnapshotSource() {
            return snapshotSource;
        }

        public void setSnapshotSource(String snapshotSource) {
            this.snapshotSource = snapshotSource;
        }

        public Integer getSnapshotVersion() {
            return snapshotVersion;
        }

        public void setSnapshotVersion(Integer snapshotVersion) {
            this.snapshotVersion = snapshotVersion;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }
    }

    /**
     * 单次 Recheck 日志摘要；{@code failReasonJson} 为表中原样 JSON 文本（可为 null）。
     */
    public static class ReviewRecheckSummary {
        private Long logId;
        private Long pushId;
        private LocalDateTime recheckTime;
        private String recheckStatus;
        private BigDecimal currentPrice;
        private BigDecimal priceDriftRatio;
        private BigDecimal currentSlippageEstimation;
        private Integer currentDataQualityScore;
        private Integer currentConfusedScore;
        private Boolean currentAccountRiskAllowed;
        /** 失败原因 JSON（原样字符串）。 */
        private String failReasonJson;
        private LocalDateTime createTime;

        public Long getLogId() {
            return logId;
        }

        public void setLogId(Long logId) {
            this.logId = logId;
        }

        public Long getPushId() {
            return pushId;
        }

        public void setPushId(Long pushId) {
            this.pushId = pushId;
        }

        public LocalDateTime getRecheckTime() {
            return recheckTime;
        }

        public void setRecheckTime(LocalDateTime recheckTime) {
            this.recheckTime = recheckTime;
        }

        public String getRecheckStatus() {
            return recheckStatus;
        }

        public void setRecheckStatus(String recheckStatus) {
            this.recheckStatus = recheckStatus;
        }

        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
        }

        public BigDecimal getPriceDriftRatio() {
            return priceDriftRatio;
        }

        public void setPriceDriftRatio(BigDecimal priceDriftRatio) {
            this.priceDriftRatio = priceDriftRatio;
        }

        public BigDecimal getCurrentSlippageEstimation() {
            return currentSlippageEstimation;
        }

        public void setCurrentSlippageEstimation(BigDecimal currentSlippageEstimation) {
            this.currentSlippageEstimation = currentSlippageEstimation;
        }

        public Integer getCurrentDataQualityScore() {
            return currentDataQualityScore;
        }

        public void setCurrentDataQualityScore(Integer currentDataQualityScore) {
            this.currentDataQualityScore = currentDataQualityScore;
        }

        public Integer getCurrentConfusedScore() {
            return currentConfusedScore;
        }

        public void setCurrentConfusedScore(Integer currentConfusedScore) {
            this.currentConfusedScore = currentConfusedScore;
        }

        public Boolean getCurrentAccountRiskAllowed() {
            return currentAccountRiskAllowed;
        }

        public void setCurrentAccountRiskAllowed(Boolean currentAccountRiskAllowed) {
            this.currentAccountRiskAllowed = currentAccountRiskAllowed;
        }

        public String getFailReasonJson() {
            return failReasonJson;
        }

        public void setFailReasonJson(String failReasonJson) {
            this.failReasonJson = failReasonJson;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }
    }

    public static class ReviewMissedSummary {
        private String missedId;
        private String decisionId;
        /** 与 {@code tm_missed_opportunity.analysis_id} 一致；复盘页上下文对齐。 */
        private String analysisId;
        private String symbol;
        private LocalDate bizDate;
        /** 表中原样 JSON 文本（可为 null）；与 Push Recheck 的 failReasonJson 展示策略一致。 */
        private String reasonJson;
        /** reasonJson 的读取侧解释层；解析失败不抛错，见 parseStatus。 */
        private MissedReasonViewVO reasonView;
        private String ruleVersion;
        private String traceId;
        private LocalDateTime createTime;

        public String getMissedId() {
            return missedId;
        }

        public void setMissedId(String missedId) {
            this.missedId = missedId;
        }

        public String getDecisionId() {
            return decisionId;
        }

        public void setDecisionId(String decisionId) {
            this.decisionId = decisionId;
        }

        public String getAnalysisId() {
            return analysisId;
        }

        public void setAnalysisId(String analysisId) {
            this.analysisId = analysisId;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public LocalDate getBizDate() {
            return bizDate;
        }

        public void setBizDate(LocalDate bizDate) {
            this.bizDate = bizDate;
        }

        public String getReasonJson() {
            return reasonJson;
        }

        public void setReasonJson(String reasonJson) {
            this.reasonJson = reasonJson;
        }

        public MissedReasonViewVO getReasonView() {
            return reasonView;
        }

        public void setReasonView(MissedReasonViewVO reasonView) {
            this.reasonView = reasonView;
        }

        public String getRuleVersion() {
            return ruleVersion;
        }

        public void setRuleVersion(String ruleVersion) {
            this.ruleVersion = ruleVersion;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }
    }

    /**
     * 复盘页 Hot Reset 区：与 {@code tm_asset_state} 当前行一致，按标的 symbol 读取。
     * <p>
     * 派生说明字段（semanticScope、*Zh）用于区分「当前权威表快照」与「本次 analysis 决策快照 / 严格历史事实」。
     */
    public static class ReviewHotResetSummary {
        /** 固定取值，便于前端或联调识别数据来源语义。 */
        public static final String SEMANTIC_SCOPE_CURRENT_ROW_BY_SYMBOL = "CURRENT_TM_ASSET_STATE_BY_SYMBOL";

        private String symbol;
        private String state;
        private Integer confusedScore;
        private Boolean hotResetFlag;
        private String hotResetTriggerType;
        private String hotResetTriggerValue;
        private LocalDateTime hotResetTime;
        private String preResetState;
        private String postResetState;
        private LocalDateTime lastUpdateTime;
        /** 本 analysisId 是否存在独立 Hot Reset 事件（tm_hot_reset_event）。 */
        private Boolean analysisEventRecorded;
        private String analysisEventId;
        private String analysisEventTraceId;
        private String analysisEventTriggerType;
        private String analysisEventTriggerValue;
        private String analysisEventDecisionId;
        private String analysisEventDecisionState;
        private Integer analysisEventConfusedScoreSnapshot;
        private Boolean analysisEventMultiTimeframeAlignedSnapshot;
        private String analysisEventTriggerReasonCode;
        private String analysisEventTriggerReasonText;
        private Integer analysisEventVersion;
        private LocalDateTime analysisEventTime;
        private String analysisEventPreState;
        private String analysisEventPostState;

        /** 与 {@link #SEMANTIC_SCOPE_CURRENT_ROW_BY_SYMBOL} 一致。 */
        private String semanticScope;
        /** 本块数据含义（固定说明）。 */
        private String scopeExplanationZh;
        /** 与本次 run / decision 的边界说明（含时间与 confused 交叉提示）。 */
        private String relationToThisAnalysisZh;
        /** pre/post 两列业务含义（固定说明）。 */
        private String prePostStateMeaningZh;
        /** 结合本 analysis 下 missed.reasonJson 的旁证说明（不解析规则，仅只读提示）。 */
        private String missedRelationHintZh;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public Integer getConfusedScore() {
            return confusedScore;
        }

        public void setConfusedScore(Integer confusedScore) {
            this.confusedScore = confusedScore;
        }

        public Boolean getHotResetFlag() {
            return hotResetFlag;
        }

        public void setHotResetFlag(Boolean hotResetFlag) {
            this.hotResetFlag = hotResetFlag;
        }

        public String getHotResetTriggerType() {
            return hotResetTriggerType;
        }

        public void setHotResetTriggerType(String hotResetTriggerType) {
            this.hotResetTriggerType = hotResetTriggerType;
        }

        public String getHotResetTriggerValue() {
            return hotResetTriggerValue;
        }

        public void setHotResetTriggerValue(String hotResetTriggerValue) {
            this.hotResetTriggerValue = hotResetTriggerValue;
        }

        public LocalDateTime getHotResetTime() {
            return hotResetTime;
        }

        public void setHotResetTime(LocalDateTime hotResetTime) {
            this.hotResetTime = hotResetTime;
        }

        public String getPreResetState() {
            return preResetState;
        }

        public void setPreResetState(String preResetState) {
            this.preResetState = preResetState;
        }

        public String getPostResetState() {
            return postResetState;
        }

        public void setPostResetState(String postResetState) {
            this.postResetState = postResetState;
        }

        public LocalDateTime getLastUpdateTime() {
            return lastUpdateTime;
        }

        public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        public Boolean getAnalysisEventRecorded() {
            return analysisEventRecorded;
        }

        public void setAnalysisEventRecorded(Boolean analysisEventRecorded) {
            this.analysisEventRecorded = analysisEventRecorded;
        }

        public String getAnalysisEventId() {
            return analysisEventId;
        }

        public void setAnalysisEventId(String analysisEventId) {
            this.analysisEventId = analysisEventId;
        }

        public String getAnalysisEventTraceId() {
            return analysisEventTraceId;
        }

        public void setAnalysisEventTraceId(String analysisEventTraceId) {
            this.analysisEventTraceId = analysisEventTraceId;
        }

        public String getAnalysisEventTriggerType() {
            return analysisEventTriggerType;
        }

        public void setAnalysisEventTriggerType(String analysisEventTriggerType) {
            this.analysisEventTriggerType = analysisEventTriggerType;
        }

        public String getAnalysisEventTriggerValue() {
            return analysisEventTriggerValue;
        }

        public void setAnalysisEventTriggerValue(String analysisEventTriggerValue) {
            this.analysisEventTriggerValue = analysisEventTriggerValue;
        }

        public String getAnalysisEventDecisionId() {
            return analysisEventDecisionId;
        }

        public void setAnalysisEventDecisionId(String analysisEventDecisionId) {
            this.analysisEventDecisionId = analysisEventDecisionId;
        }

        public String getAnalysisEventDecisionState() {
            return analysisEventDecisionState;
        }

        public void setAnalysisEventDecisionState(String analysisEventDecisionState) {
            this.analysisEventDecisionState = analysisEventDecisionState;
        }

        public Integer getAnalysisEventConfusedScoreSnapshot() {
            return analysisEventConfusedScoreSnapshot;
        }

        public void setAnalysisEventConfusedScoreSnapshot(Integer analysisEventConfusedScoreSnapshot) {
            this.analysisEventConfusedScoreSnapshot = analysisEventConfusedScoreSnapshot;
        }

        public Boolean getAnalysisEventMultiTimeframeAlignedSnapshot() {
            return analysisEventMultiTimeframeAlignedSnapshot;
        }

        public void setAnalysisEventMultiTimeframeAlignedSnapshot(Boolean analysisEventMultiTimeframeAlignedSnapshot) {
            this.analysisEventMultiTimeframeAlignedSnapshot = analysisEventMultiTimeframeAlignedSnapshot;
        }

        public String getAnalysisEventTriggerReasonCode() {
            return analysisEventTriggerReasonCode;
        }

        public void setAnalysisEventTriggerReasonCode(String analysisEventTriggerReasonCode) {
            this.analysisEventTriggerReasonCode = analysisEventTriggerReasonCode;
        }

        public String getAnalysisEventTriggerReasonText() {
            return analysisEventTriggerReasonText;
        }

        public void setAnalysisEventTriggerReasonText(String analysisEventTriggerReasonText) {
            this.analysisEventTriggerReasonText = analysisEventTriggerReasonText;
        }

        public Integer getAnalysisEventVersion() {
            return analysisEventVersion;
        }

        public void setAnalysisEventVersion(Integer analysisEventVersion) {
            this.analysisEventVersion = analysisEventVersion;
        }

        public LocalDateTime getAnalysisEventTime() {
            return analysisEventTime;
        }

        public void setAnalysisEventTime(LocalDateTime analysisEventTime) {
            this.analysisEventTime = analysisEventTime;
        }

        public String getAnalysisEventPreState() {
            return analysisEventPreState;
        }

        public void setAnalysisEventPreState(String analysisEventPreState) {
            this.analysisEventPreState = analysisEventPreState;
        }

        public String getAnalysisEventPostState() {
            return analysisEventPostState;
        }

        public void setAnalysisEventPostState(String analysisEventPostState) {
            this.analysisEventPostState = analysisEventPostState;
        }

        public String getSemanticScope() {
            return semanticScope;
        }

        public void setSemanticScope(String semanticScope) {
            this.semanticScope = semanticScope;
        }

        public String getScopeExplanationZh() {
            return scopeExplanationZh;
        }

        public void setScopeExplanationZh(String scopeExplanationZh) {
            this.scopeExplanationZh = scopeExplanationZh;
        }

        public String getRelationToThisAnalysisZh() {
            return relationToThisAnalysisZh;
        }

        public void setRelationToThisAnalysisZh(String relationToThisAnalysisZh) {
            this.relationToThisAnalysisZh = relationToThisAnalysisZh;
        }

        public String getPrePostStateMeaningZh() {
            return prePostStateMeaningZh;
        }

        public void setPrePostStateMeaningZh(String prePostStateMeaningZh) {
            this.prePostStateMeaningZh = prePostStateMeaningZh;
        }

        public String getMissedRelationHintZh() {
            return missedRelationHintZh;
        }

        public void setMissedRelationHintZh(String missedRelationHintZh) {
            this.missedRelationHintZh = missedRelationHintZh;
        }
    }

    public static class ReviewAlertSummary {
        private String id;
        private String alertType;
        private String alertLevel;
        private String alertMessage;
        private String status;
        /** OPEN 时由写入侧写入的预期冷却截止时间（字符串时间）；SUPPRESSED 时通常为空。 */
        private String cooldownUntil;
        /** 抑制原因（如 DB 节流）；OPEN 时通常为空。 */
        private String suppressReason;
        private String createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAlertType() {
            return alertType;
        }

        public void setAlertType(String alertType) {
            this.alertType = alertType;
        }

        public String getAlertLevel() {
            return alertLevel;
        }

        public void setAlertLevel(String alertLevel) {
            this.alertLevel = alertLevel;
        }

        public String getAlertMessage() {
            return alertMessage;
        }

        public void setAlertMessage(String alertMessage) {
            this.alertMessage = alertMessage;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getCooldownUntil() {
            return cooldownUntil;
        }

        public void setCooldownUntil(String cooldownUntil) {
            this.cooldownUntil = cooldownUntil;
        }

        public String getSuppressReason() {
            return suppressReason;
        }

        public void setSuppressReason(String suppressReason) {
            this.suppressReason = suppressReason;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}
