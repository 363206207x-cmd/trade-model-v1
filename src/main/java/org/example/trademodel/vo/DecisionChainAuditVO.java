package org.example.trademodel.vo;

import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.EvidenceItemDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.ScoreItemDO;
import org.example.trademodel.entity.TmPushRecheckLogDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Read-only aggregation of the v4.1 decision responsibility chain. */
public class DecisionChainAuditVO {
    private AnalysisStage analysis;
    private OpportunityStage opportunity;
    private List<EvidenceItemDO> evidence = List.of();
    private List<ScoreItemDO> scores = List.of();
    private DecisionResult decisionBundle;
    private ExecutionPlanCandidateDO candidate;
    private List<AiTraceStage> aiTraces = List.of();
    private ConflictResolverResultDO conflictResolver;
    private RuleValidationStage ruleValidation;
    private ExecutionPlanDO finalExecutionPlan;
    private List<PushStage> pushes = List.of();
    private List<PositionStage> positions = List.of();
    private List<ReviewStage> reviews = List.of();
    private List<StageStatus> orderedStages = List.of();
    private Boolean candidateFinalIsolated = true;
    private Boolean resolverOwnedSeparately = true;
    private Boolean ruleValidationOwnedSeparately = true;
    private Boolean notTradeInstruction = true;

    public AnalysisStage getAnalysis() { return analysis; }
    public void setAnalysis(AnalysisStage value) { this.analysis = value; }
    public OpportunityStage getOpportunity() { return opportunity; }
    public void setOpportunity(OpportunityStage value) { this.opportunity = value; }
    public List<EvidenceItemDO> getEvidence() { return evidence; }
    public void setEvidence(List<EvidenceItemDO> value) { this.evidence = value == null ? List.of() : value; }
    public List<ScoreItemDO> getScores() { return scores; }
    public void setScores(List<ScoreItemDO> value) { this.scores = value == null ? List.of() : value; }
    public DecisionResult getDecisionBundle() { return decisionBundle; }
    public void setDecisionBundle(DecisionResult value) { this.decisionBundle = value; }
    public ExecutionPlanCandidateDO getCandidate() { return candidate; }
    public void setCandidate(ExecutionPlanCandidateDO value) { this.candidate = value; }
    public List<AiTraceStage> getAiTraces() { return aiTraces; }
    public void setAiTraces(List<AiTraceStage> value) { this.aiTraces = value == null ? List.of() : value; }
    public ConflictResolverResultDO getConflictResolver() { return conflictResolver; }
    public void setConflictResolver(ConflictResolverResultDO value) { this.conflictResolver = value; }
    public RuleValidationStage getRuleValidation() { return ruleValidation; }
    public void setRuleValidation(RuleValidationStage value) { this.ruleValidation = value; }
    public ExecutionPlanDO getFinalExecutionPlan() { return finalExecutionPlan; }
    public void setFinalExecutionPlan(ExecutionPlanDO value) { this.finalExecutionPlan = value; }
    public List<PushStage> getPushes() { return pushes; }
    public void setPushes(List<PushStage> value) { this.pushes = value == null ? List.of() : value; }
    public List<PositionStage> getPositions() { return positions; }
    public void setPositions(List<PositionStage> value) { this.positions = value == null ? List.of() : value; }
    public List<ReviewStage> getReviews() { return reviews; }
    public void setReviews(List<ReviewStage> value) { this.reviews = value == null ? List.of() : value; }
    public List<StageStatus> getOrderedStages() { return orderedStages; }
    public void setOrderedStages(List<StageStatus> value) { this.orderedStages = value == null ? List.of() : value; }
    public Boolean getCandidateFinalIsolated() { return candidateFinalIsolated; }
    public void setCandidateFinalIsolated(Boolean value) { this.candidateFinalIsolated = value; }
    public Boolean getResolverOwnedSeparately() { return resolverOwnedSeparately; }
    public void setResolverOwnedSeparately(Boolean value) { this.resolverOwnedSeparately = value; }
    public Boolean getRuleValidationOwnedSeparately() { return ruleValidationOwnedSeparately; }
    public void setRuleValidationOwnedSeparately(Boolean value) { this.ruleValidationOwnedSeparately = value; }
    public Boolean getNotTradeInstruction() { return notTradeInstruction; }
    public void setNotTradeInstruction(Boolean value) { this.notTradeInstruction = value; }

    public record AnalysisStage(
            String analysisId,
            String requestId,
            String traceId,
            String symbol,
            String timeframe,
            String ruleVersion,
            Integer dataQualityScore,
            String status,
            Boolean preview,
            LocalDateTime analysisTime,
            LocalDateTime completedAt) { }

    public record OpportunityStage(
            String opportunityId,
            String analysisId,
            String symbol,
            String timeframe,
            String state,
            Integer confusedScore,
            String reason,
            String triggerSource,
            String ruleVersion,
            String traceId,
            LocalDateTime stateEnteredAt,
            LocalDateTime coolingUntil,
            LocalDateTime updatedAt) { }

    public record AiTraceStage(
            String callId,
            String traceId,
            String requestId,
            String analysisId,
            String opportunityId,
            String candidateId,
            String role,
            String provider,
            String model,
            String inputHash,
            String inputSummary,
            String outputJson,
            String status,
            String errorCode,
            String errorMessage,
            Boolean fallback,
            String fallbackReason,
            Boolean cacheHit,
            Long tokenInput,
            Long tokenOutput,
            BigDecimal tokenCost,
            Long latencyMs,
            LocalDateTime observedAt,
            LocalDateTime createdAt) { }

    public record RuleValidationStage(
            String validationResultId,
            String candidateId,
            String resolverResultId,
            String analysisId,
            String status,
            String reasons,
            String vetoReason,
            String chainStatus,
            String sourceGateStatus,
            Boolean sourceGateComplete,
            Boolean finalPlan,
            LocalDateTime finalizedAt) { }

    public record ReviewStage(
            String reviewId,
            String reviewType,
            String opportunityId,
            String analysisId,
            String candidateId,
            String finalPlanId,
            String resolverResultId,
            String validationResultId,
            String traceId,
            String outcome,
            String executionDeviation,
            String aiAssessment,
            String ruleAssessment,
            String ruleFeedback,
            String metricsJson,
            String contractVersion,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) { }

    public record PushStage(TmPushSnapshotDO snapshot, List<TmPushRecheckLogDO> rechecks) {
        public PushStage {
            rechecks = rechecks == null ? List.of() : List.copyOf(rechecks);
        }
    }

    public record PositionStage(UserPositionDO position, List<PositionMonitorLogDO> monitorLogs) {
        public PositionStage {
            monitorLogs = monitorLogs == null ? List.of() : List.copyOf(monitorLogs);
        }
    }

    public record StageStatus(int order, String stage, String owner, String status, String referenceId) { }
}
