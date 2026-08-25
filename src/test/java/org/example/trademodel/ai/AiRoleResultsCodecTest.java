package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiRoleResultsCodecTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiRoleResultsCodec codec = new AiRoleResultsCodec(objectMapper);

    @Test
    void realProducerCreatesVersionedStructuredAiRolePayload() throws Exception {
        AiOrchestratorResult result = result(
                role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        AiReviewStance.SUPPORT, "GPT_ONLY", "GPT summary"),
                role(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                        AiReviewStance.CHALLENGE, "GEMINI_ONLY", "Gemini summary"),
                role(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                        AiReviewStance.CHALLENGE, "GROK_ONLY", "Grok summary"));

        String json = codec.serialize(result, "v1.0", synthesis());
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.path("schemaVersion").asText()).isEqualTo("v1");
        assertThat(root.path("roles").path("GPT_FINAL").path("summary").asText()).isEqualTo("GPT summary");
        assertThat(root.path("roles").path("GEMINI_REVIEW").path("summary").asText()).isEqualTo("Gemini summary");
        assertThat(root.path("roles").path("GROK_CHALLENGE").path("summary").asText()).isEqualTo("Grok summary");
        assertThat(root.path("synthesis").path("finalMarketBias").asText()).isEqualTo("BULLISH");
        assertThat(json).doesNotContain("; providers=", "providerRequestId", "Authorization", "apiKey");
        assertThat(codec.parse(json).current()).isTrue();
    }

    @Test
    void fullRolePayloadIsSanitized() {
        AiProviderReviewResult gpt = role(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                AiReviewStance.SUPPORT, "OK", "Authorization: Bearer raw-token apiKey=raw-key sk-secretvalue");
        gpt.setReasonCodes(List.of("providerSecret=raw-reason-secret", "sk-reason-secret"));
        gpt.setFallback(true);
        gpt.setFallbackReason("providerSecret=raw-secret");

        String json = codec.serialize(result(gpt), "v1.0", synthesis());

        assertThat(json).doesNotContain("raw-token", "raw-key", "secretvalue", "raw-secret", "raw-reason-secret",
                "reason-secret");
        assertThat(json).contains("***");
    }

    @Test
    void malformedAndLegacyPayloadsAreNeverCurrentEvidence() {
        assertThat(codec.parse("{not-json").status()).isEqualTo(AiRoleResultsCodec.ParseStatus.MALFORMED);
        assertThat(codec.parse("orchestrationMode=RULE_ONLY_FALLBACK; providers=OPENAI:SUCCESS")
                .status()).isEqualTo(AiRoleResultsCodec.ParseStatus.LEGACY_PLAIN_TEXT);
        assertThat(codec.parse("{\"roles\":{}}")
                .status()).isEqualTo(AiRoleResultsCodec.ParseStatus.UNSUPPORTED_SCHEMA);
    }

    @Test
    void v2DecisionChainCarriesThreeRoleMetadataAndIndependentEmptyCollectionStates() throws Exception {
        Map<AiDecisionChainRole, AiDecisionChainResult> results = new EnumMap<>(AiDecisionChainRole.class);
        results.put(AiDecisionChainRole.GPT_FINAL, decisionResult(
                AiDecisionChainRole.GPT_FINAL, AiProviderName.OPENAI,
                AiDecisionChainContractTest.gptPayload()));
        results.put(AiDecisionChainRole.GEMINI_REVIEW, decisionResult(
                AiDecisionChainRole.GEMINI_REVIEW, AiProviderName.GEMINI,
                AiDecisionChainContractTest.geminiPayload()));
        results.put(AiDecisionChainRole.GROK_CHALLENGE, decisionResult(
                AiDecisionChainRole.GROK_CHALLENGE, AiProviderName.XAI,
                AiDecisionChainContractTest.grokPayload()));

        String json = codec.serializeDecisionChain(
                "analysis-1", "trace-1", "BULLISH", results,
                new AiRoleResultsPayload.SynthesisPayload(
                        "WEAK_BULLISH", "MEDIUM", "MEDIUM", "PREPARATION", false,
                        "LEVEL_1_CONSISTENT", 0, "UNCHANGED", "UNCHANGED",
                        "UNCHANGED", false, null, "rule direction retained", null));
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.path("schemaVersion").asText()).isEqualTo("v2");
        assertThat(root.path("roles").size()).isEqualTo(3);
        assertThat(root.path("roles").path("GPT_FINAL").path("stance").asText()).isEqualTo("SUPPORT");
        assertThat(root.path("roles").path("GEMINI_REVIEW").path("stance").asText()).isEqualTo("SUPPORT");
        assertThat(root.path("roles").path("GROK_CHALLENGE").path("stance").asText()).isEqualTo("ABSTAIN");
        root.path("roles").forEach(role -> {
            assertThat(role.path("analysisId").asText()).isEqualTo("analysis-1");
            assertThat(role.path("traceId").asText()).isEqualTo("trace-1");
            assertThat(role.path("roleState").asText()).isEqualTo("READY");
            assertThat(role.path("resultAvailable").asBoolean()).isTrue();
            assertThat(role.path("generatedAt").asText()).isNotBlank();
        });
        assertThat(root.path("roles").path("GPT_FINAL").path("supportingEvidence").isArray()).isTrue();
        assertThat(root.path("roles").path("GPT_FINAL").path("supportingEvidenceState").asText())
                .isEqualTo("NONE_FOUND");
        assertThat(codec.parse(json).current()).isTrue();
    }

    @Test
    void missingRoleIsExplicitUnavailableInsteadOfBeingOmittedOrFabricated() throws Exception {
        Map<AiDecisionChainRole, AiDecisionChainResult> onlyGpt = new EnumMap<>(AiDecisionChainRole.class);
        onlyGpt.put(AiDecisionChainRole.GPT_FINAL, decisionResult(
                AiDecisionChainRole.GPT_FINAL, AiProviderName.OPENAI,
                AiDecisionChainContractTest.gptPayload()));

        JsonNode root = objectMapper.readTree(codec.serializeDecisionChain(
                "analysis-2", "trace-2", "BULLISH", onlyGpt,
                AiRoleResultsPayload.SynthesisPayload.empty()));
        JsonNode gemini = root.path("roles").path("GEMINI_REVIEW");

        assertThat(gemini.path("roleState").asText()).isEqualTo("UNAVAILABLE");
        assertThat(gemini.path("resultAvailable").asBoolean()).isFalse();
        assertThat(gemini.path("dataState").asText()).isEqualTo("SOURCE_UNAVAILABLE");
        assertThat(gemini.path("evidenceGapsState").asText()).isEqualTo("SOURCE_UNAVAILABLE");
        assertThat(gemini.path("evidenceGaps").isArray()).isTrue();
        assertThat(gemini.path("evidenceGaps")).isEmpty();
        assertThat(gemini.path("fallbackReason").asText()).isEqualTo("ROLE_RESULT_UNAVAILABLE");
    }

    private AiOrchestratorResult result(AiProviderReviewResult... roles) {
        AiOrchestratorResult result = new AiOrchestratorResult();
        result.setAnalysisId("analysis-ai-contract");
        result.setTraceId("trace-ai-contract");
        result.setProviderResults(List.of(roles));
        return result;
    }

    private AiProviderReviewResult role(AiProviderName provider, AiProviderRole role,
                                        AiReviewStance stance, String reason, String summary) {
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(provider);
        result.setRole(role);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setStance(stance);
        result.setConflictLevel(stance == AiReviewStance.CHALLENGE
                ? AiReviewConflictLevel.MAJOR
                : AiReviewConflictLevel.NONE);
        result.setReasonCodes(List.of(reason));
        result.setSummary(summary);
        return result;
    }

    private static AiDecisionChainResult decisionResult(AiDecisionChainRole role,
                                                        AiProviderName provider,
                                                        String payload) {
        AiDecisionChainResult result = new AiDecisionChainResult();
        result.setRole(role);
        result.setProvider(provider);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setPayloadJson(payload);
        return result;
    }

    private AiRoleResultsPayload.SynthesisPayload synthesis() {
        return new AiRoleResultsPayload.SynthesisPayload(
                "BULLISH", "MEDIUM", "HIGH", false,
                "LEVEL_3_SIGNIFICANT_DISAGREEMENT", 62,
                "MEDIUM", "RAISED", "PREPARE_ONLY", false,
                "GEMINI_ONLY");
    }
}
