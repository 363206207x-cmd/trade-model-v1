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
    private static final Pattern SAFE_MODEL = Pattern.compile("[A-Za-z0-9._:-]{1,120}");

    public static GeminiRequestDiagnostic analyze(
            ObjectMapper objectMapper, String model, AiHttpRequest request) {
        String safeModel = safeModel(model);
        if (objectMapper == null || request == null) {
            return unavailable(safeModel);
        }
        try {
            JsonNode root = objectMapper.readTree(request.getBody());
            JsonNode generation = root.path("generationConfig");
            return new GeminiRequestDiagnostic(
                    safeModel,
                    textOrDefault(generation.path("responseMimeType"), "--"),
                    generation.hasNonNull("responseJsonSchema"),
                    Math.max(0, generation.path("maxOutputTokens").asInt(0)),
                    generation.path("temperature").isNumber()
                            ? generation.path("temperature").asText() : "NONE",
                    textLength(root.path("systemInstruction").path("parts")),
                    contentTextLength(root.path("contents")),
                    generation.has("stopSequences"),
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

    private static int contentTextLength(JsonNode contents) {
        if (contents == null || !contents.isArray()) {
            return 0;
        }
        int length = 0;
        for (JsonNode content : contents) {
            length += textLength(content.path("parts"));
        }
        return length;
    }

    private static int textLength(JsonNode parts) {
        if (parts == null || !parts.isArray()) {
            return 0;
        }
        int length = 0;
        for (JsonNode part : parts) {
            JsonNode text = part.path("text");
            if (text.isTextual()) {
                length += text.asText().length();
            }
        }
        return length;
    }
}
