package org.example.trademodel.uireview;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.ai.AiRoleResultsPayload;
import org.example.trademodel.service.DecisionChainAuditQueryService;
import org.example.trademodel.vo.DecisionChainAuditVO;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read-only Analysis role-state fixture for isolated browser acceptance. */
@Primary
@Profile("ui-review")
@Service
public class UiReviewDecisionChainAuditQueryService implements DecisionChainAuditQueryService {
    private static final LocalDateTime GENERATED_AT = LocalDateTime.of(2026, 8, 22, 10, 0);
    private final ObjectMapper objectMapper;

    public UiReviewDecisionChainAuditQueryService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<DecisionChainAuditVO> queryForUser(
            Long userId, String analysisId, String traceId, String candidateId) {
        if (userId == null || userId <= 0 || analysisId == null || analysisId.isBlank()
                || !analysisId.startsWith("ui-review-")) {
            return Optional.empty();
        }
        String mode = analysisId.contains("preview") ? "ANALYSIS_PREVIEW"
                : analysisId.contains("unknown") ? "UNKNOWN" : "OPPORTUNITY_DECISION";
        DecisionChainAuditVO audit = new DecisionChainAuditVO();
        audit.setAnalysis(new DecisionChainAuditVO.AnalysisStage(
                analysisId, "ui-review-request", "ui-review-trace", "BTCUSDT", "15m",
                "ui-review-rule", 88, "SUCCESS", mode, "ANALYSIS_PREVIEW".equals(mode),
                GENERATED_AT.minusMinutes(2), GENERATED_AT));
        audit.setOpportunity(new DecisionChainAuditVO.OpportunityStage(
                "ui-review-opportunity", analysisId, "BTCUSDT", "15m", "waiting_trigger",
                18, "UI_REVIEW_FIXTURE", "CONTROLLED_VISUAL_FIXTURE", "ui-review-rule",
                "ui-review-trace", GENERATED_AT.minusMinutes(5), null, GENERATED_AT));
        audit.setAiRoleResults(roleResults(analysisId));
        audit.setCandidateFinalIsolated(true);
        audit.setResolverOwnedSeparately(true);
        audit.setRuleValidationOwnedSeparately(true);
        audit.setNotTradeInstruction(true);
        return Optional.of(audit);
    }

    private AiRoleResultsPayload roleResults(String analysisId) {
        Map<String, Object> roles = new LinkedHashMap<>();
        roles.put("GPT_FINAL", gpt(analysisId));
        roles.put("GEMINI_REVIEW", gemini(analysisId));
        roles.put("GROK_CHALLENGE", grok(analysisId));
        return objectMapper.convertValue(map(
                "schemaVersion", "v2",
                "analysisId", analysisId,
                "traceId", "ui-review-trace",
                "ruleVersion", "ui-review-rule",
                "orchestrationMode", "UI_REVIEW_FIXTURE",
                "roles", roles,
                "safety", map(
                        "reviewOnly", true,
                        "manualReviewOnly", true,
                        "notTradeInstruction", true,
                        "notExecutable", true,
                        "notAutoTrading", true,
                        "notOrderExecution", true,
                        "notUserPositionCreation", true,
                        "notPositionMutation", true,
                        "notStateMachineOverride", true,
                        "notFinalExecutionPlanCreation", true,
                        "ruleDirectionPreserved", true)
        ), AiRoleResultsPayload.class);
    }

    private Map<String, Object> gpt(String analysisId) {
        Map<String, Object> role = baseRole(analysisId, "GPT_FINAL", "OPENAI");
        role.put("coreJudgment", map(
                "marketBias", "WEAK_BULLISH",
                "opportunityState", "WAITING_TRIGGER",
                "text", "多周期结构保持，但仍等待触发与重新校验"));
        role.put("supportingEvidence", List.of());
        role.put("supportingEvidenceState", "NONE_FOUND");
        role.put("opposingEvidence", List.of());
        role.put("opposingEvidenceState", "NONE_FOUND");
        role.put("candidateSummary", map(
                "planMode", "PREPARATION",
                "confidence", "MEDIUM",
                "riskLevel", "HIGH",
                "worthOpening", false,
                "recommendedAction", "等待触发；触发后重新校验，通过后再进入人工确认",
                "summary", "Candidate 仍为非 Final，仅供结构化复核"));
        applyBlockedScenario(analysisId, role);
        return role;
    }

    private Map<String, Object> gemini(String analysisId) {
        Map<String, Object> role = baseRole(analysisId, "GEMINI_REVIEW", "GEMINI");
        String review = analysisId.contains("gemini-approve") ? "APPROVE"
                : analysisId.contains("gemini-reject") ? "REJECT_CANDIDATE"
                : analysisId.contains("gemini-risk") ? "RISK_WARNING" : "DOWNGRADE";
        role.put("reviewResult", review);
        role.put("finalDirectionImpact", "保持规则方向，仅调整 Candidate 参与方式");
        role.put("recoveryCondition", "触发条件和数据质量重新通过校验");
        if (!"APPROVE".equals(review)) {
            role.put("downgradeSuggestion", map(
                    "before", "PREPARATION",
                    "after", "OBSERVATION",
                    "reason", "短周期证据仍有冲突",
                    "recoveryCondition", "短周期结构与来源恢复一致"));
        }
        role.put("evidenceGaps", List.of());
        role.put("evidenceGapsState", "NONE_FOUND");
        role.put("logicConflicts", List.of());
        role.put("logicConflictsState", "NONE_FOUND");
        role.put("underestimatedRisks", List.of());
        role.put("underestimatedRisksState", "NONE_FOUND");
        return role;
    }

    private Map<String, Object> grok(String analysisId) {
        Map<String, Object> role = baseRole(analysisId, "GROK_CHALLENGE", "XAI");
        boolean emptyFound = analysisId.contains("grok-found-empty");
        boolean noPath = analysisId.contains("grok-no-path");
        role.put("failurePathState", noPath ? "NO_VERIFIABLE_FAILURE_PATH" : "FOUND");
        role.put("failurePaths", noPath || emptyFound ? List.of() : List.of(map(
                "failurePathId", "ui-review-failure-path",
                "hypothesis", "触发后流动性未能跟随",
                "triggerCondition", "价格触发但成交与结构确认缺失",
                "causalPath", "确认不足导致追价风险扩大",
                "invalidatingEvidence", "结构重新站稳且成交恢复")));
        role.put("opposingScenarios", List.of());
        role.put("opposingScenariosState", "NONE_FOUND");
        role.put("externalEventRisks", List.of());
        role.put("externalEventRisksState", "NONE_FOUND");
        role.put("microstructureRisks", List.of());
        role.put("microstructureRisksState", "NONE_FOUND");
        role.put("watchIndicators", List.of());
        role.put("watchIndicatorsState", "NONE_FOUND");
        role.put("challengeSummary", "等待触发后仍需验证流动性与结构确认");
        role.put("currentDirectionChallenge", "弱偏多方向尚未被推翻，但不能直接进入人工确认");
        role.put("majorCounterEvidence", false);
        return role;
    }

    private Map<String, Object> baseRole(String analysisId, String roleName, String provider) {
        return map(
                "role", roleName,
                "provider", provider,
                "sourceRole", roleName,
                "callStatus", "SUCCESS",
                "analysisId", analysisId,
                "traceId", "ui-review-trace-" + roleName.toLowerCase(),
                "roleState", "READY",
                "dataState", "READY",
                "generatedAt", "2026-08-22T10:00:00Z",
                "resultAvailable", true,
                "summary", "UI_REVIEW_FIXTURE",
                "fallback", false,
                "manualReviewRequired", true);
    }

    private void applyBlockedScenario(String analysisId, Map<String, Object> role) {
        String state = analysisId.contains("role-unavailable") ? "UNAVAILABLE"
                : analysisId.contains("role-error") ? "ERROR"
                : analysisId.contains("role-fallback") ? "FALLBACK" : null;
        if (state == null) return;
        role.put("roleState", state);
        role.put("dataState", "SOURCE_UNAVAILABLE");
        role.put("callStatus", "FAILED");
        role.put("resultAvailable", false);
        role.put("fallback", "FALLBACK".equals(state));
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            values.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return values;
    }
}
