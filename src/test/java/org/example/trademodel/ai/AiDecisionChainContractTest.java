package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    void allThreeRolesRequireTheirCompleteStructuredContractAndCannotCrossAuthority() throws Exception {
        assertThat(parser.parse(AiProviderName.OPENAI, AiDecisionChainRole.GPT_FINAL, gptPayload()).successful())
                .isTrue();
        assertThat(parser.parse(AiProviderName.GEMINI, AiDecisionChainRole.GEMINI_REVIEW, geminiPayload()).successful())
                .isTrue();
        assertThat(parser.parse(AiProviderName.XAI, AiDecisionChainRole.GROK_CHALLENGE, grokPayload()).successful())
                .isTrue();

        ObjectNode gpt = (ObjectNode) objectMapper.readTree(gptPayload());
        gpt.put("finalExecutionPlan", true);
        ObjectNode gemini = (ObjectNode) objectMapper.readTree(geminiPayload());
        gemini.put("entryZone", "forbidden plan generation");
        ObjectNode grok = (ObjectNode) objectMapper.readTree(grokPayload());
        grok.put("recommendedAction", "forbidden plan generation");

        assertThat(parser.parse(AiProviderName.OPENAI, AiDecisionChainRole.GPT_FINAL, gpt.toString())
                .getFallbackReason()).isEqualTo("INVALID_FORBIDDEN_FIELD_FINALEXECUTIONPLAN");
        assertThat(parser.parse(AiProviderName.GEMINI, AiDecisionChainRole.GEMINI_REVIEW, gemini.toString())
                .getFallbackReason()).isEqualTo("INVALID_UNKNOWN_FIELD_ENTRYZONE");
        assertThat(parser.parse(AiProviderName.XAI, AiDecisionChainRole.GROK_CHALLENGE, grok.toString())
                .getFallbackReason()).isEqualTo("INVALID_UNKNOWN_FIELD_RECOMMENDEDACTION");
    }

    @Test
    void collectionStateIsIndependentAndCannotClaimFoundForEmptyOrHideFoundItems() throws Exception {
        ObjectNode emptyClaimedFound = (ObjectNode) objectMapper.readTree(gptPayload());
        emptyClaimedFound.put("supportingEvidenceState", "FOUND");
        assertThat(parser.parse(AiProviderName.OPENAI, AiDecisionChainRole.GPT_FINAL,
                emptyClaimedFound.toString()).getFallbackReason())
                .isEqualTo("INVALID_COLLECTION_STATE_SUPPORTINGEVIDENCE");

        ObjectNode hiddenEvidence = (ObjectNode) objectMapper.readTree(gptPayload());
        ((ArrayNode) hiddenEvidence.path("supportingEvidence")).add(evidence());
        assertThat(parser.parse(AiProviderName.OPENAI, AiDecisionChainRole.GPT_FINAL,
                hiddenEvidence.toString()).getFallbackReason())
                .isEqualTo("INVALID_COLLECTION_STATE_SUPPORTINGEVIDENCE");

        AiDecisionChainResult noPath = parser.parse(
                AiProviderName.XAI, AiDecisionChainRole.GROK_CHALLENGE, grokPayload());
        assertThat(noPath.successful()).isTrue();
        assertThat(objectMapper.readTree(noPath.getPayloadJson()).path("failurePathState").asText())
                .isEqualTo("NO_VERIFIABLE_FAILURE_PATH");
    }

    @Test
    void geminiFindingsRequireVerifiableSourceReferences() throws Exception {
        ObjectNode gemini = (ObjectNode) objectMapper.readTree(geminiPayload());
        gemini.put("evidenceGapsState", "FOUND");
        ObjectNode unsupportedFinding = finding("gap-1");
        unsupportedFinding.remove("evidenceRefs");
        gemini.set("evidenceGaps", objectMapper.createArrayNode().add(unsupportedFinding));

        assertThat(parser.parse(AiProviderName.GEMINI, AiDecisionChainRole.GEMINI_REVIEW,
                gemini.toString()).getFallbackReason())
                .isEqualTo("INVALID_MISSING_FIELD_EVIDENCEGAPS_0__EVIDENCEREFS");
    }

    @Test
    void invalidEnumsMissingStatesAndWrongTypesFailClosed() throws Exception {
        ObjectNode invalidMode = (ObjectNode) objectMapper.readTree(gptPayload());
        ((ObjectNode) invalidMode.path("candidateSummary")).put("planMode", "AUTO_ORDER");
        ObjectNode wrongType = (ObjectNode) objectMapper.readTree(geminiPayload());
        wrongType.put("evidenceGaps", "not-an-array");
        ObjectNode missingState = (ObjectNode) objectMapper.readTree(grokPayload());
        missingState.remove("watchIndicatorsState");

        assertThat(parser.parse(AiProviderName.OPENAI, AiDecisionChainRole.GPT_FINAL,
                invalidMode.toString()).getFallbackReason())
                .isEqualTo("INVALID_FIELD_VALUE_CANDIDATESUMMARY_PLANMODE");
        assertThat(parser.parse(AiProviderName.GEMINI, AiDecisionChainRole.GEMINI_REVIEW,
                wrongType.toString()).getFallbackReason())
                .isEqualTo("INVALID_FIELD_TYPE_EVIDENCEGAPS");
        assertThat(parser.parse(AiProviderName.XAI, AiDecisionChainRole.GROK_CHALLENGE,
                missingState.toString()).getFallbackReason())
                .isEqualTo("INVALID_MISSING_FIELD_WATCHINDICATORSSTATE");
    }

    @Test
    void grokFoundCollectionsRequireTheirRoleSpecificVerifiableStructure() throws Exception {
        ObjectNode grok = (ObjectNode) objectMapper.readTree(grokPayload());
        grok.put("opposingScenariosState", "FOUND");
        grok.set("opposingScenarios", objectMapper.createArrayNode().add(opposingScenario()));
        grok.put("externalEventRisksState", "FOUND");
        grok.set("externalEventRisks", objectMapper.createArrayNode().add(externalEventRisk()));
        grok.put("microstructureRisksState", "FOUND");
        grok.set("microstructureRisks", objectMapper.createArrayNode().add(microstructureRisk()));
        grok.put("watchIndicatorsState", "FOUND");
        grok.set("watchIndicators", objectMapper.createArrayNode().add(watchIndicator()));

        assertThat(parser.parse(AiProviderName.XAI, AiDecisionChainRole.GROK_CHALLENGE,
                grok.toString()).successful()).isTrue();

        ((ObjectNode) grok.path("opposingScenarios").get(0)).remove("triggerCondition");
        assertThat(parser.parse(AiProviderName.XAI, AiDecisionChainRole.GROK_CHALLENGE,
                grok.toString()).getFallbackReason())
                .isEqualTo("INVALID_MISSING_FIELD_OPPOSINGSCENARIOS_0__TRIGGERCONDITION");
    }

    @Test
    void evidenceConfidenceAndStrengthAreBoundedNumericFacts() throws Exception {
        ObjectNode gpt = (ObjectNode) objectMapper.readTree(gptPayload());
        gpt.put("supportingEvidenceState", "FOUND");
        ObjectNode evidence = evidence();
        ((ArrayNode) gpt.path("supportingEvidence")).add(evidence);
        assertThat(parser.parse(AiProviderName.OPENAI, AiDecisionChainRole.GPT_FINAL,
                gpt.toString()).successful()).isTrue();

        evidence.put("confidence", 101);
        assertThat(parser.parse(AiProviderName.OPENAI, AiDecisionChainRole.GPT_FINAL,
                gpt.toString()).getFallbackReason())
                .isEqualTo("INVALID_FIELD_VALUE_SUPPORTINGEVIDENCE_0__CONFIDENCE");
    }

    @Test
    void oversizedResponsesCollectionsAndTextFailClosed() throws Exception {
        AiDecisionChainResult responseTooLarge = parser.parse(
                AiProviderName.GEMINI, AiDecisionChainRole.GEMINI_REVIEW,
                "x".repeat(AiDecisionChainResponseParser.MAX_RESPONSE_CHARS + 1));

        ObjectNode tooManyFindings = (ObjectNode) objectMapper.readTree(geminiPayload());
        tooManyFindings.put("evidenceGapsState", "FOUND");
        ArrayNode findings = objectMapper.createArrayNode();
        for (int index = 0; index < 21; index++) {
            findings.add(finding("finding-" + index));
        }
        tooManyFindings.set("evidenceGaps", findings);

        ObjectNode oversizedText = (ObjectNode) objectMapper.readTree(gptPayload());
        ((ObjectNode) oversizedText.path("candidateSummary"))
                .put("summary", "x".repeat(AiDecisionChainResponseParser.MAX_TEXT_CHARS + 1));

        assertThat(responseTooLarge.getFallbackReason()).isEqualTo("INVALID_RESPONSE_TOO_LARGE");
        assertThat(responseTooLarge.getAuditOutput()).hasSize(AiDecisionChainResponseParser.MAX_RESPONSE_CHARS);
        assertThat(parser.parse(AiProviderName.GEMINI, AiDecisionChainRole.GEMINI_REVIEW,
                tooManyFindings.toString()).getFallbackReason())
                .isEqualTo("INVALID_FIELD_SIZE_EVIDENCEGAPS");
        assertThat(parser.parse(AiProviderName.OPENAI, AiDecisionChainRole.GPT_FINAL,
                oversizedText.toString()).getFallbackReason())
                .isEqualTo("INVALID_FIELD_SIZE_CANDIDATESUMMARY_SUMMARY");
    }

    @Test
    void legacyConflictLevelNamesFailClosed() throws Exception {
        ObjectNode legacy = (ObjectNode) objectMapper.readTree(geminiPayload());
        legacy.put("conflictLevel", "MINOR");

        assertThat(parser.parse(AiProviderName.GEMINI, AiDecisionChainRole.GEMINI_REVIEW,
                legacy.toString()).getFallbackReason())
                .isEqualTo("INVALID_FIELD_VALUE_CONFLICTLEVEL");
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
        assertThat(root.path("interpretationContract").path("humanLanguage").asText())
                .isEqualTo("SIMPLIFIED_CHINESE");
        assertThat(root.path("interpretationContract").path("requiredEvidenceDomains").toString())
                .contains("KLINE_MULTI_TIMEFRAME", "COINGLASS_OPEN_INTEREST",
                        "COINGLASS_WEIGHTED_FUNDING", "COINGLASS_LIQUIDATION",
                        "COINGLASS_LONG_SHORT_RATIO");
        assertThat(root.path("interpretationContract").path("crossEvidenceRules").toString())
                .contains("PRICE_UP_AND_OI_DOWN_IS_SHORT_COVERING",
                        "LIQUIDATION_IS_FORCED_FLOW_NOT_INDEPENDENT_DIRECTION_PROOF",
                        "LONG_SHORT_RATIO_IS_CROWDING_NOT_CAPITAL_OR_DIRECTION_PROOF");
        assertThat(AiDecisionChainPromptBuilder.systemInstruction(AiDecisionChainRole.GPT_FINAL))
                .contains("Simplified Chinese", "CoinGlass open interest", "weighted funding",
                        "liquidation", "long/short-ratio", "conclusion first");
        assertThat(AiDecisionChainPromptBuilder.systemInstruction(AiDecisionChainRole.GEMINI_REVIEW))
                .contains("can be trusted", "stop-loss/source problem", "REJECT_CANDIDATE");
        assertThat(AiDecisionChainPromptBuilder.systemInstruction(AiDecisionChainRole.GROK_CHALLENGE))
                .contains("most likely failure conclusion first", "forced-flow evidence", "crowding evidence");
        assertThat(root.path("outputContract").path("additionalProperties").asBoolean()).isFalse();
    }

    static String gptPayload() {
        return """
                {
                  "coreJudgment":{"marketBias":"BULLISH","opportunityState":"CANDIDATE","text":"Rule direction remains supported"},
                  "supportingEvidenceState":"NONE_FOUND","supportingEvidence":[],
                  "opposingEvidenceState":"NONE_FOUND","opposingEvidence":[],
                  "multiTimeframeExplanation":{"4h":"bullish context","1h":"bullish structure","15m":"setup forming","5m":"manual trigger pending"},
                  "biasAdjustment":{"before":"BULLISH","after":"WEAK_BULLISH","reason":"same-family evidence downgrade"},
                  "candidateSummary":{
                    "planMode":"PREPARATION","confidence":"MEDIUM","riskLevel":"MEDIUM","worthOpening":false,
                    "opportunityType":"TREND_CONTINUATION","recommendedAction":"WAIT_FOR_MANUAL_CONFIRMATION",
                    "riskExplanation":"bounded manual decision risk","summary":"Candidate only, not final"
                  }
                }
                """;
    }

    static String geminiPayload() {
        return """
                {
                  "evidenceGapsState":"NONE_FOUND","evidenceGaps":[],
                  "logicConflictsState":"NONE_FOUND","logicConflicts":[],
                  "underestimatedRisksState":"NONE_FOUND","underestimatedRisks":[],
                  "downgradeSuggestion":{"before":"PREPARATION","after":"PREPARATION","reason":"no further downgrade","recoveryCondition":"new verified analysis"},
                  "reviewResult":"APPROVE","conflictLevel":"LEVEL_1_CONSISTENT",
                  "finalDirectionImpact":"UNCHANGED","confidenceAdjustment":"UNCHANGED",
                  "riskAdjustment":"UNCHANGED","planModeAdjustment":"UNCHANGED",
                  "recoveryCondition":"new verified analysis"
                }
                """;
    }

    static String grokPayload() {
        return """
                {
                  "failurePathState":"NO_VERIFIABLE_FAILURE_PATH","failurePaths":[],
                  "opposingScenariosState":"NONE_FOUND","opposingScenarios":[],
                  "externalEventRisksState":"NONE_FOUND","externalEventRisks":[],
                  "microstructureRisksState":"NONE_FOUND","microstructureRisks":[],
                  "watchIndicatorsState":"NONE_FOUND","watchIndicators":[],
                  "challengeSummary":"No verifiable major challenge","currentDirectionChallenge":"No cross-family challenge",
                  "majorCounterEvidence":false,"conflictLevel":"LEVEL_1_CONSISTENT",
                  "riskAdjustment":"UNCHANGED","planModeImpact":"UNCHANGED"
                }
                """;
    }

    private ObjectNode evidence() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("evidenceId", "evidence-1");
        node.put("type", "MARKET_STRUCTURE");
        node.put("source", "verified-source");
        node.put("currentValue", "101");
        node.put("change", "+1");
        node.put("direction", "BULLISH");
        node.put("strength", 80.0);
        node.put("confidence", 87.0);
        node.put("observedAt", "2026-08-12T00:00:00Z");
        node.put("freshness", "FRESH");
        node.put("analysisId", "analysis-1");
        return node;
    }

    private ObjectNode finding(String id) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("findingId", id);
        node.put("category", "EVIDENCE");
        node.put("text", "bounded finding");
        node.put("impact", "review only");
        node.set("evidenceRefs", objectMapper.createArrayNode().add("evidence-1"));
        return node;
    }

    private ObjectNode opposingScenario() {
        ObjectNode node = sourcedFinding("scenario-1");
        node.put("triggerCondition", "verified support breaks");
        node.put("observationWindow", "next 4h close");
        node.set("validationIndicators", objectMapper.createArrayNode().add("4h structure"));
        return node;
    }

    private ObjectNode externalEventRisk() {
        ObjectNode node = sourcedFinding("event-1");
        node.put("source", "verified-source");
        node.put("observedAt", "2026-08-12T00:00:00Z");
        node.put("eventWindow", "2026-08-12T00:00:00Z/2026-08-12T04:00:00Z");
        return node;
    }

    private ObjectNode microstructureRisk() {
        ObjectNode node = sourcedFinding("micro-1");
        node.put("phenomenon", "liquidity sweep");
        node.put("timeframe", "5m");
        return node;
    }

    private ObjectNode watchIndicator() {
        ObjectNode node = sourcedFinding("watch-1");
        node.put("metric", "open interest");
        node.put("currentState", "stable");
        node.put("triggerCondition", "verified increase above the supplied boundary");
        return node;
    }

    private ObjectNode sourcedFinding(String id) {
        ObjectNode node = finding(id);
        node.set("evidenceRefs", objectMapper.createArrayNode().add("evidence-1"));
        return node;
    }
}
