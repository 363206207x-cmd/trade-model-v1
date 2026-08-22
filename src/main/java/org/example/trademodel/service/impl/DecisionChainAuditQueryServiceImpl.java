package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.ai.AiRoleResultsCodec;
import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.DecisionResult;
import org.example.trademodel.entity.EvidenceItemDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.PositionMonitorLogDO;
import org.example.trademodel.entity.ReviewResultDO;
import org.example.trademodel.entity.ScoreItemDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.ConflictResolverResultMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.ExecutionPlanCandidateMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.PositionMonitorLogMapper;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.mapper.ReviewResultMapper;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.mapper.UserPositionMapper;
import org.example.trademodel.service.AiCallLogService;
import org.example.trademodel.service.DecisionChainAuditQueryService;
import org.example.trademodel.vo.DecisionChainAuditVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class DecisionChainAuditQueryServiceImpl implements DecisionChainAuditQueryService {
    private static final Set<String> V41_ROLES = Set.of("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
    private static final AiRoleResultsCodec AI_ROLE_RESULTS_CODEC =
            new AiRoleResultsCodec(new ObjectMapper());

    private final AnalysisRunMapper analysisRunMapper;
    private final ExecutionPlanCandidateMapper candidateMapper;
    private final ConflictResolverResultMapper conflictMapper;
    private final ExecutionPlanMapper executionPlanMapper;
    private final AssetStateMapper assetStateMapper;
    private final ReviewResultMapper reviewResultMapper;
    private final AiCallLogService aiCallLogService;
    private final EvidenceItemMapper evidenceItemMapper;
    private final ScoreItemMapper scoreItemMapper;
    private final DecisionResultMapper decisionResultMapper;
    private final PushSnapshotMapper pushSnapshotMapper;
    private final PushRecheckLogMapper pushRecheckLogMapper;
    private final UserPositionMapper userPositionMapper;
    private final PositionMonitorLogMapper positionMonitorLogMapper;

    public DecisionChainAuditQueryServiceImpl(
            AnalysisRunMapper analysisRunMapper,
            ExecutionPlanCandidateMapper candidateMapper,
            ConflictResolverResultMapper conflictMapper,
            ExecutionPlanMapper executionPlanMapper,
            AssetStateMapper assetStateMapper,
            ReviewResultMapper reviewResultMapper,
            AiCallLogService aiCallLogService,
            EvidenceItemMapper evidenceItemMapper,
            ScoreItemMapper scoreItemMapper,
            DecisionResultMapper decisionResultMapper,
            PushSnapshotMapper pushSnapshotMapper,
            PushRecheckLogMapper pushRecheckLogMapper,
            UserPositionMapper userPositionMapper,
            PositionMonitorLogMapper positionMonitorLogMapper) {
        this.analysisRunMapper = analysisRunMapper;
        this.candidateMapper = candidateMapper;
        this.conflictMapper = conflictMapper;
        this.executionPlanMapper = executionPlanMapper;
        this.assetStateMapper = assetStateMapper;
        this.reviewResultMapper = reviewResultMapper;
        this.aiCallLogService = aiCallLogService;
        this.evidenceItemMapper = evidenceItemMapper;
        this.scoreItemMapper = scoreItemMapper;
        this.decisionResultMapper = decisionResultMapper;
        this.pushSnapshotMapper = pushSnapshotMapper;
        this.pushRecheckLogMapper = pushRecheckLogMapper;
        this.userPositionMapper = userPositionMapper;
        this.positionMonitorLogMapper = positionMonitorLogMapper;
    }

    @Override
    public Optional<DecisionChainAuditVO> queryForUser(
            Long userId, String analysisId, String traceId, String candidateId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        String requestedAnalysis = clean(analysisId);
        String requestedTrace = clean(traceId);
        String requestedCandidate = clean(candidateId);
        if (requestedAnalysis == null && requestedTrace == null && requestedCandidate == null) {
            throw new IllegalArgumentException("analysisId, traceId or candidateId is required");
        }

        ExecutionPlanCandidateDO selectedCandidate = requestedCandidate == null
                ? null : candidateMapper.selectById(requestedCandidate);
        if (requestedCandidate != null && selectedCandidate == null) {
            return Optional.empty();
        }
        String resolvedAnalysis = requestedAnalysis;
        if (selectedCandidate != null) {
            resolvedAnalysis = consistent(resolvedAnalysis, selectedCandidate.getAnalysisId(), "candidateId");
        }

        List<AiCallLogDO> traceMatches = requestedTrace == null ? List.of()
                : aiCallLogService.queryOwned(userId, null, requestedTrace, requestedCandidate,
                null, null, null, null, null, 500);
        if (requestedTrace != null && traceMatches.isEmpty()) {
            return Optional.empty();
        }
        if (!traceMatches.isEmpty()) {
            resolvedAnalysis = consistent(resolvedAnalysis, traceMatches.get(0).getAnalysisId(), "traceId");
        }
        if (resolvedAnalysis == null) {
            return Optional.empty();
        }

        AnalysisRunDO analysis = analysisRunMapper.selectReadableByUser(resolvedAnalysis, userId);
        if (analysis == null) {
            return Optional.empty();
        }
        if (selectedCandidate == null) {
            selectedCandidate = candidateMapper.selectByAnalysisId(resolvedAnalysis);
        }
        if (requestedCandidate != null && (selectedCandidate == null
                || !requestedCandidate.equals(selectedCandidate.getCandidateId()))) {
            return Optional.empty();
        }

        String candidateKey = selectedCandidate == null ? null : selectedCandidate.getCandidateId();
        List<AiCallLogDO> traces = aiCallLogService.queryOwned(userId, resolvedAnalysis,
                requestedTrace, candidateKey, null, null, null, null, null, 500).stream()
                .filter(row -> V41_ROLES.contains(row.getAiRole()))
                .toList();
        ConflictResolverResultDO conflict = candidateKey == null
                ? conflictMapper.selectByAnalysisId(resolvedAnalysis)
                : conflictMapper.selectByCandidateId(candidateKey);
        ExecutionPlanDO plan = candidateKey == null
                ? executionPlanMapper.selectLatestByAnalysisId(resolvedAnalysis)
                : executionPlanMapper.selectLatestByCandidateId(candidateKey);
        String opportunityId = selectedCandidate != null ? selectedCandidate.getOpportunityId()
                : plan == null ? null : plan.getOpportunityId();
        AssetStateDO opportunity = opportunityId == null ? null : assetStateMapper.selectByOpportunityId(opportunityId);
        List<EvidenceItemDO> evidence = safe(evidenceItemMapper.listByAnalysisId(resolvedAnalysis));
        List<ScoreItemDO> scores = safe(scoreItemMapper.listByAnalysisId(resolvedAnalysis));
        DecisionResult decision = decisionResultMapper.selectLatestByAnalysisId(resolvedAnalysis);
        List<DecisionChainAuditVO.PushStage> pushes = loadPushes(resolvedAnalysis);
        List<DecisionChainAuditVO.PositionStage> positions = loadPositions(
                userId, resolvedAnalysis, isValidatedFinal(plan) ? plan.getPlanId() : null);
        List<ReviewResultDO> reviews = reviewResultMapper.listByAnalysisIdForUser(resolvedAnalysis, userId);

        DecisionChainAuditVO result = new DecisionChainAuditVO();
        result.setAnalysis(toAnalysis(analysis));
        result.setOpportunity(toOpportunity(opportunity));
        result.setEvidence(evidence);
        result.setScores(scores);
        result.setDecisionBundle(decision);
        result.setCandidate(selectedCandidate);
        result.setAiTraces(traces.stream().map(DecisionChainAuditQueryServiceImpl::toTrace).toList());
        AiRoleResultsCodec.ParseResult roleResults = AI_ROLE_RESULTS_CODEC.parse(
                decision == null ? null : decision.getAiRoleResults());
        if (roleResults.current() && resolvedAnalysis.equals(roleResults.payload().analysisId())) {
            result.setAiRoleResults(roleResults.payload());
        }
        result.setConflictResolver(conflict);
        result.setRuleValidation(toValidation(plan));
        result.setFinalExecutionPlan(isValidatedFinal(plan) ? plan : null);
        result.setPushes(pushes);
        result.setPositions(positions);
        result.setReviews(reviews == null ? List.of() : reviews.stream().map(DecisionChainAuditQueryServiceImpl::toReview).toList());
        result.setOrderedStages(stages(analysis, evidence, scores, decision, opportunity,
                selectedCandidate, traces, conflict, plan, pushes, positions, reviews));
        return Optional.of(result);
    }

    private List<DecisionChainAuditVO.PushStage> loadPushes(String analysisId) {
        return safe(pushSnapshotMapper.listByAnalysisId(analysisId)).stream()
                .map(push -> new DecisionChainAuditVO.PushStage(
                        push, safe(pushRecheckLogMapper.selectByPushId(push.getPushId()))))
                .toList();
    }

    private List<DecisionChainAuditVO.PositionStage> loadPositions(
            Long userId, String analysisId, String validatedFinalPlanId) {
        List<PositionMonitorLogDO> monitorLogs = safe(
                positionMonitorLogMapper.listByAnalysisIdAndUserId(analysisId, userId));
        Map<Long, UserPositionDO> ownedPositions = new LinkedHashMap<>();
        if (validatedFinalPlanId != null) {
            safe(userPositionMapper.listByFinalPlanIdAndUserId(validatedFinalPlanId, userId))
                    .forEach(position -> ownedPositions.put(position.getId(), position));
        }
        monitorLogs.stream().map(PositionMonitorLogDO::getPositionId).distinct()
                .map(positionId -> userPositionMapper.selectByIdAndUserId(positionId, userId))
                .filter(java.util.Objects::nonNull)
                .forEach(position -> ownedPositions.putIfAbsent(position.getId(), position));
        return ownedPositions.values().stream()
                .map(position -> new DecisionChainAuditVO.PositionStage(position, monitorLogs.stream()
                        .filter(log -> position.getId().equals(log.getPositionId()))
                        .toList()))
                .toList();
    }

    private static DecisionChainAuditVO.AnalysisStage toAnalysis(AnalysisRunDO row) {
        return new DecisionChainAuditVO.AnalysisStage(row.getAnalysisId(), row.getRequestId(), row.getTraceId(),
                row.getSymbol(), row.getTimeframe(), row.getRuleVersion(), row.getDataQualityScore(), row.getStatus(),
                row.getAnalysisMode(), row.getPreview(), row.getAnalysisTime(), row.getCompletedAt());
    }

    private static DecisionChainAuditVO.OpportunityStage toOpportunity(AssetStateDO row) {
        if (row == null) return null;
        return new DecisionChainAuditVO.OpportunityStage(row.getOpportunityId(), row.getLastAnalysisId(), row.getSymbol(),
                row.getTimeframe(), row.getState() == null ? null : row.getState().name(), row.getConfusedScore(),
                row.getLastTransitionReason(), row.getLastTriggerSource(), row.getRuleVersion(), row.getTraceId(),
                row.getStateEnteredAt(), row.getCoolingUntil(), row.getLastUpdateTime());
    }

    private static DecisionChainAuditVO.AiTraceStage toTrace(AiCallLogDO row) {
        return new DecisionChainAuditVO.AiTraceStage(row.getCallId(), row.getTraceId(), row.getRequestId(),
                row.getAnalysisId(), row.getOpportunityId(), row.getCandidateId(), row.getAiRole(),
                row.getProviderName(), row.getModelName(), row.getRequestHash(), row.getRequestSummary(),
                row.getOutputPayload(), row.getCallStatus(), row.getErrorCode(), row.getErrorMessage(),
                row.getFallbackFlag(), row.getFallbackReason(), row.getCacheHit(), row.getInputTokens(),
                row.getOutputTokens(), row.getCalculatedCostUsd(), row.getLatencyMs(), row.getObservedAt(), row.getCreatedAt());
    }

    private static DecisionChainAuditVO.RuleValidationStage toValidation(ExecutionPlanDO row) {
        if (row == null) return null;
        return new DecisionChainAuditVO.RuleValidationStage(row.getValidationResultId(), row.getCandidateId(),
                row.getResolverResultId(), row.getAnalysisId(), row.getRuleValidationStatus(), row.getValidationReasons(),
                row.getRuleVetoReason(), row.getChainStatus(), row.getSourceGateStatus(), row.getSourceGateComplete(),
                row.getFinalPlan(), row.getFinalizedAt());
    }

    private static DecisionChainAuditVO.ReviewStage toReview(ReviewResultDO row) {
        return new DecisionChainAuditVO.ReviewStage(row.getId(), row.getReviewType(), row.getOpportunityId(),
                row.getAnalysisId(), row.getCandidateId(), row.getFinalPlanId(), row.getResolverResultId(),
                row.getValidationResultId(), row.getTraceId(), row.getOutcome(), row.getExecutionDeviation(),
                row.getAiAssessment(), row.getRuleAssessment(), row.getRuleFeedback(), row.getMetricsJson(),
                row.getContractVersion(), row.getCreateTime(), row.getUpdateTime());
    }

    private static List<DecisionChainAuditVO.StageStatus> stages(
            AnalysisRunDO analysis, List<EvidenceItemDO> evidence, List<ScoreItemDO> scores,
            DecisionResult decision, AssetStateDO opportunity,
            ExecutionPlanCandidateDO candidate, List<AiCallLogDO> traces,
            ConflictResolverResultDO conflict, ExecutionPlanDO plan,
            List<DecisionChainAuditVO.PushStage> pushes,
            List<DecisionChainAuditVO.PositionStage> positions,
            List<ReviewResultDO> reviews) {
        List<DecisionChainAuditVO.StageStatus> out = new ArrayList<>();
        out.add(stage(1, "ANALYSIS_INPUT", "AnalysisRun", analysis.getStatus(), analysis.getAnalysisId()));
        out.add(collectionStage(2, "EVIDENCE", "EvidenceItem", evidence,
                evidence == null || evidence.isEmpty() ? null : evidence.get(0).getEvidenceId()));
        out.add(collectionStage(3, "EIGHT_SCORES", "ScoreItem", scores,
                scores == null || scores.isEmpty() ? null : scores.get(0).getScoreId()));
        out.add(stage(4, "DECISION_BUNDLE", "DecisionResult",
                decision == null ? "MISSING" : "RECORDED",
                decision == null ? null : decision.getDecisionId()));
        out.add(stage(5, "OPPORTUNITY_STATE", "AssetState/Opportunity",
                opportunity == null ? "NOT_PROMOTED" : opportunity.getState().name(),
                opportunity == null ? null : opportunity.getOpportunityId()));
        out.add(aiStage(6, "GPT_CANDIDATE", "GPT_FINAL", traces));
        out.add(stage(7, "EXECUTION_PLAN_CANDIDATE", "ExecutionPlanCandidate",
                candidate == null ? "MISSING" : candidate.getCandidateStatus(),
                candidate == null ? null : candidate.getCandidateId()));
        out.add(aiStage(8, "GEMINI_REVIEW", "GEMINI_REVIEW", traces));
        out.add(aiStage(9, "GROK_CHALLENGE", "GROK_CHALLENGE", traces));
        out.add(stage(10, "CONFLICT_RESOLVER", "ConflictResolverResult",
                conflict == null ? "MISSING" : "RECORDED",
                conflict == null ? null : conflict.getResolverResultId()));
        out.add(stage(11, "RULE_VALIDATION", "ExecutionPlan.validation",
                plan == null ? "MISSING" : plan.getRuleValidationStatus(),
                plan == null ? null : plan.getValidationResultId()));
        out.add(stage(12, "FINAL_EXECUTION_PLAN", "ExecutionPlan(final)",
                isValidatedFinal(plan) ? "VALIDATED" : "NOT_FINAL",
                isValidatedFinal(plan) ? plan.getPlanId() : null));
        out.add(collectionStage(13, "PUSH_RECHECK", "PushSnapshot/PushRecheckLog", pushes,
                pushes == null || pushes.isEmpty() ? null : String.valueOf(pushes.get(0).snapshot().getPushId())));
        out.add(collectionStage(14, "USER_POSITION_MONITORING", "UserPosition/PositionMonitorLog", positions,
                positions == null || positions.isEmpty() ? null : String.valueOf(positions.get(0).position().getId())));
        out.add(stage(15, "OUTCOME_REVIEW_RULE_FEEDBACK", "ReviewResult",
                reviews == null || reviews.isEmpty() ? "NOT_RECORDED" : "RECORDED",
                reviews == null || reviews.isEmpty() ? null : reviews.get(0).getId()));
        return List.copyOf(out);
    }

    private static DecisionChainAuditVO.StageStatus collectionStage(
            int order, String stage, String owner, List<?> rows, String referenceId) {
        int count = rows == null ? 0 : rows.size();
        return stage(order, stage, owner, count == 0 ? "NOT_RECORDED" : "RECORDED(" + count + ")", referenceId);
    }

    private static DecisionChainAuditVO.StageStatus aiStage(
            int order, String stage, String role, List<AiCallLogDO> traces) {
        AiCallLogDO row = traces == null ? null : traces.stream()
                .filter(item -> role.equals(item.getAiRole())).findFirst().orElse(null);
        return stage(order, stage, "AITrace(" + role + ")", row == null ? "MISSING" : row.getCallStatus(),
                row == null ? null : row.getCallId());
    }

    private static DecisionChainAuditVO.StageStatus stage(
            int order, String stage, String owner, String status, String referenceId) {
        return new DecisionChainAuditVO.StageStatus(order, stage, owner, status, referenceId);
    }

    private static boolean isValidatedFinal(ExecutionPlanDO plan) {
        return plan != null && Boolean.TRUE.equals(plan.getFinalPlan())
                && "PASS".equals(plan.getRuleValidationStatus());
    }

    private static String consistent(String current, String discovered, String selector) {
        String value = clean(discovered);
        if (current == null) return value;
        if (value == null || current.equals(value)) return current;
        throw new IllegalArgumentException(selector + " does not belong to analysisId");
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static <T> List<T> safe(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
