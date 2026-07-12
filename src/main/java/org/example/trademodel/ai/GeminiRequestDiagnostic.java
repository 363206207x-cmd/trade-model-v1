package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Pattern;

public record GeminiRequestDiagnostic(
        String model,
        String responseMimeType,
        boolean responseSchemaPresent,
        int maxOutputTokens,
        String temperature,
        int systemInstructionLength,
        int userInputLength,
        boolean stopSequencesPresent,
        boolean toolsPresent
) {
    private static final Pattern SAFE_MODEL = Pattern.compile("[A-Za-z0-9._:/-]{1,120}");

    public static GeminiRequestDiagnostic analyze(
            ObjectMapper objectMapper, String model, AiHttpRequest request) {
        String safeModel = safeModel(model);
        if (objectMapper == null || request == null) {
            return unavailable(safeModel);
        }
        try {
            JsonNode root = objectMapper.readTree(request.getBody());
            JsonNode generation = root.path("generation_config");
            JsonNode responseFormat = root.path("response_format");
            String requestModel = textOrDefault(root.path("model"), safeModel);
            return new GeminiRequestDiagnostic(
                    safeModel(requestModel),
                    textOrDefault(responseFormat.path("mime_type"), "--"),
                    responseFormat.path("schema").isObject(),
                    Math.max(0, generation.path("max_output_tokens").asInt(0)),
                    generation.path("temperature").isNumber()
                            ? generation.path("temperature").asText() : "NONE",
                    textLength(root.path("system_instruction")),
                    textLength(root.path("input")),
                    generation.has("stop_sequences"),
                    root.has("tools"));
        } catch (Exception ignored) {
            return unavailable(safeModel);
        }
    }

    private static GeminiRequestDiagnostic unavailable(String model) {
        return new GeminiRequestDiagnostic(model, "--", false, 0, "NONE", 0, 0, false, false);
    }

    private static String safeModel(String model) {
        String value = model == null ? "" : model.trim();
        return SAFE_MODEL.matcher(value).matches() ? value : "--";
    }

    private static String textOrDefault(JsonNode node, String fallback) {
        return node != null && node.isTextual() && !node.asText().isBlank() ? node.asText() : fallback;
    }

    private static int textLength(JsonNode text) {
        return text != null && text.isTextual() ? text.asText().length() : 0;
    }
}
