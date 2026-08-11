package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("core-regression")
class AiDecisionChainContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiDecisionChainResponseParser parser = new AiDecisionChainResponseParser(objectMapper);

    @Test
    void gptFinalCanProduceCandidateFieldsButCannotClaimFinalAuthority() {
        AiDecisionChainResult valid = parser.parse(AiProviderName.OPENAI, AiDecisionChainRole.GPT_FINAL, """
                {
                  "direction":"BULLISH",
                  "planMode":"CONFIRM",
                  "confidence":"HIGH",
                  "riskLevel":"MEDIUM",
                  "worthOpening":true,
                  "recommendedAction":"MANUAL_REVIEW",
                  "entryZone":"100-101",
                  "stopLoss":"95",
                  "takeProfitRules":"110 then 120",
                  "leverageSuggestion":"1x",
                  "positionSuggestion":"small",
                  "invalidCondition":"close below 95",
                  "validity":"2026-08-12T00:00Z",
                  "summary":"Candidate only"
                }
                """);
        assertThat(valid.successful()).isTrue();

        AiDecisionChainResult forbidden = parser.parse(AiProviderName.OPENAI, AiDecisionChainRole.GPT_FINAL, """
                {
                  "direction":"BULLISH",
                  "planMode":"CONFIRM",
                  "confidence":"HIGH",
                  "riskLevel":"MEDIUM",
                  "worthOpening":true,
                  "recommendedAction":"MANUAL_REVIEW",
                  "entryZone":"100-101",
                  "stopLoss":"95",
                  "takeProfitRules":"110",
                  "leverageSuggestion":"1x",
                  "positionSuggestion":"small",
                  "invalidCondition":"close below 95",
                  "validity":"2026-08-12T00:00Z",
                  "summary":"Candidate only",
                  "finalExecutionPlan":true
                }
        """);
        assertThat(forbidden.successful()).isFalse();
        assertThat(forbidden.getFallbackReason()).contains("FINALEXECUTIONPLAN");
    }

    @Test
    void geminiAndGrokAreReviewOnlyAndCannotGeneratePlanFields() {
        AiDecisionChainResult gemini = parser.parse(AiProviderName.GEMINI, AiDecisionChainRole.GEMINI_REVIEW, """
                {
                  "verdict":"DOWNGRADE",
                  "conflictLevel":"LEVEL_2_MINOR_DISAGREEMENT",
                  "confidenceAdjustment":"DOWNGRADE_ONE",
                  "riskAdjustment":"RAISE_ONE",
                  "planModeAdjustment":"DOWNGRADE_ONE",
                  "reasons":["weak evidence"],
                  "summary":"review",
                  "entryZone":"forbidden"
                }
                """);
        AiDecisionChainResult grok = parser.parse(AiProviderName.XAI, AiDecisionChainRole.GROK_CHALLENGE, """
                {
                  "opposingView":"risk",
                  "riskLevel":"HIGH",
                  "challengeLevel":"LEVEL_3_SIGNIFICANT_DISAGREEMENT",
                  "majorCounterEvidence":true,
                  "planModeImpact":"DOWNGRADE_TWO",
                  "reasons":["event risk"],
                  "summary":"challenge",
                  "recommendedAction":"forbidden"
                }
                """);

        assertThat(gemini.successful()).isFalse();
        assertThat(gemini.getFallbackReason()).contains("ENTRYZONE");
        assertThat(grok.successful()).isFalse();
        assertThat(grok.getFallbackReason()).contains("RECOMMENDEDACTION");
    }

    @Test
    void invalidEnumsAndWrongTypesFailClosed() {
        AiDecisionChainResult invalidMode = parser.parse(AiProviderName.OPENAI, AiDecisionChainRole.GPT_FINAL, """
                {
                  "direction":"BULLISH",
                  "planMode":"AUTO_ORDER",
                  "confidence":"HIGH",
                  "riskLevel":"MEDIUM",
                  "worthOpening":true,
                  "recommendedAction":"MANUAL_REVIEW",
                  "entryZone":"100-101",
                  "stopLoss":"95",
                  "takeProfitRules":"110",
                  "leverageSuggestion":"1x",
                  "positionSuggestion":"small",
                  "invalidCondition":"close below 95",
                  "validity":"2026-08-12T00:00Z",
                  "summary":"candidate"
                }
                """);
        AiDecisionChainResult wrongType = parser.parse(AiProviderName.GEMINI, AiDecisionChainRole.GEMINI_REVIEW, """
                {
                  "verdict":"APPROVE",
                  "conflictLevel":"LEVEL_1_CONSISTENT",
                  "confidenceAdjustment":"UNCHANGED",
                  "riskAdjustment":"UNCHANGED",
                  "planModeAdjustment":"UNCHANGED",
                  "reasons":"not-an-array",
                  "summary":"review"
                }
                """);

        assertThat(invalidMode.successful()).isFalse();
        assertThat(invalidMode.getFallbackReason()).isEqualTo("INVALID_FIELD_VALUE_PLANMODE");
        assertThat(wrongType.successful()).isFalse();
        assertThat(wrongType.getFallbackReason()).isEqualTo("INVALID_FIELD_TYPE_REASONS");
    }

    @Test
    void oversizedResponsesReasonsAndTextFailClosed() {
        AiDecisionChainResult responseTooLarge = parser.parse(
                AiProviderName.GEMINI, AiDecisionChainRole.GEMINI_REVIEW,
                "x".repeat(AiDecisionChainResponseParser.MAX_RESPONSE_CHARS + 1));
        AiDecisionChainResult tooManyReasons = parser.parse(
                AiProviderName.GEMINI, AiDecisionChainRole.GEMINI_REVIEW, """
                {
                  "verdict":"APPROVE",
                  "conflictLevel":"LEVEL_1_CONSISTENT",
                  "confidenceAdjustment":"UNCHANGED",
                  "riskAdjustment":"UNCHANGED",
                  "planModeAdjustment":"UNCHANGED",
                  "reasons":["1","2","3","4","5","6","7","8","9"],
                  "summary":"review"
                }
                """);
        String longSummary = "x".repeat(AiDecisionChainResponseParser.MAX_TEXT_CHARS + 1);
        AiDecisionChainResult oversizedText = parser.parse(
                AiProviderName.GEMINI, AiDecisionChainRole.GEMINI_REVIEW, """
                {
                  "verdict":"APPROVE",
                  "conflictLevel":"LEVEL_1_CONSISTENT",
                  "confidenceAdjustment":"UNCHANGED",
                  "riskAdjustment":"UNCHANGED",
                  "planModeAdjustment":"UNCHANGED",
                  "reasons":[],
                  "summary":"%s"
                }
                """.formatted(longSummary));

        assertThat(responseTooLarge.getFallbackReason()).isEqualTo("INVALID_RESPONSE_TOO_LARGE");
        assertThat(responseTooLarge.getAuditOutput())
                .hasSize(AiDecisionChainResponseParser.MAX_RESPONSE_CHARS);
        assertThat(tooManyReasons.getFallbackReason()).isEqualTo("INVALID_FIELD_SIZE_REASONS");
        assertThat(oversizedText.getFallbackReason()).isEqualTo("INVALID_FIELD_SIZE_SUMMARY");
    }

    @Test
    void legacyConflictLevelNamesFailClosed() {
        AiDecisionChainResult legacy = parser.parse(
                AiProviderName.GEMINI, AiDecisionChainRole.GEMINI_REVIEW, """
                {
                  "verdict":"APPROVE",
                  "conflictLevel":"MINOR",
                  "confidenceAdjustment":"UNCHANGED",
                  "riskAdjustment":"UNCHANGED",
                  "planModeAdjustment":"UNCHANGED",
                  "reasons":[],
                  "summary":"legacy conflict level"
                }
                """);

        assertThat(legacy.successful()).isFalse();
        assertThat(legacy.getFallbackReason()).isEqualTo("INVALID_FIELD_VALUE_CONFLICTLEVEL");
    }

    @Test
    void promptIncludesRoleBoundaryAndRecursivelyRedactsSecretsWithoutBreakingJson() throws Exception {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setMaxInputChars(1000);
        AiDecisionChainPromptBuilder builder = new AiDecisionChainPromptBuilder(objectMapper, properties);
        AiDecisionChainRequest request = new AiDecisionChainRequest();
        request.setRole(AiDecisionChainRole.GPT_FINAL);
        request.setAnalysisId("analysis-1");
        request.setTraceId("trace-1");
        request.setCandidateId("candidate-1");
        request.setSymbol("BTCUSDT");
        request.setTimeframe("5m");
        request.setInput(Map.of(
                "nested", Map.of("authorization", "Bearer abcdefghijklmnop"),
                "evidence", List.of(Map.of("providerSecret", "sk-supersecret")),
                "large", "x".repeat(1400)));

        AiDecisionChainPromptBuilder.PromptPayload payload = builder.build(request);
        JsonNode root = objectMapper.readTree(payload.dataJson());

        assertThat(payload.truncated()).isTrue();
        assertThat(payload.dataJson()).doesNotContain("abcdefghijklmnop", "sk-supersecret");
        assertThat(root.path("safetyBoundary").path("candidateGenerationOnly").asBoolean()).isTrue();
        assertThat(root.path("safetyBoundary").path("notFinalExecutionPlanCreation").asBoolean()).isTrue();
        assertThat(root.path("outputContract").path("additionalProperties").asBoolean()).isFalse();
    }
}
