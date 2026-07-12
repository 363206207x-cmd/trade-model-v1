package org.example.trademodel.ai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Structural extraction evidence only; response text and field values are never retained. */
public record GeminiExtractionDiagnostic(
        boolean candidatesPresent,
        int candidateCount,
        boolean contentPresent,
        boolean partsPresent,
        boolean textNodePresent,
        int textLength,
        boolean emptyText,
        boolean extractedJsonParsePassed,
        GeminiOutputClass outputClass) {

    public static GeminiExtractionDiagnostic analyze(
            ObjectMapper objectMapper, JsonNode root, String extractedText) {
        JsonNode candidates = root == null ? null : root.get("candidates");
        boolean candidatesPresent = candidates != null && !candidates.isNull();
        int candidateCount = candidates != null && candidates.isArray() ? candidates.size() : 0;
        JsonNode firstCandidate = candidateCount > 0 ? candidates.get(0) : null;
        JsonNode content = firstCandidate == null ? null : firstCandidate.get("content");
        boolean contentPresent = content != null && !content.isNull();
        JsonNode parts = content == null ? null : content.get("parts");
        boolean partsPresent = parts != null && !parts.isNull();
        JsonNode firstPart = parts != null && parts.isArray() && !parts.isEmpty() ? parts.get(0) : null;
        JsonNode textNode = firstPart == null ? null : firstPart.get("text");
        boolean textNodePresent = textNode != null && !textNode.isNull();
        int textLength = extractedText == null ? 0 : extractedText.length();
        boolean emptyText = extractedText == null || extractedText.isBlank();
        boolean jsonParsePassed = parsesAsSingleJsonValue(objectMapper, extractedText);
        GeminiOutputClass outputClass = classify(objectMapper, extractedText);
        return new GeminiExtractionDiagnostic(
                candidatesPresent, candidateCount, contentPresent, partsPresent,
                textNodePresent, textLength, emptyText, jsonParsePassed, outputClass);
    }

    public boolean successful() {
        return candidatesPresent && candidateCount > 0 && contentPresent && partsPresent
                && textNodePresent && !emptyText && extractedJsonParsePassed;
    }

    public String status() {
        return successful() ? "PASS" : "FAILED";
    }

    public String jsonParseStatus() {
        return extractedJsonParsePassed ? "PASS" : "FAIL";
    }

    private static GeminiOutputClass classify(ObjectMapper objectMapper, String text) {
        if (text == null || text.isBlank()) {
            return GeminiOutputClass.EMPTY_TEXT;
        }
        String trimmed = text.trim();
        if (trimmed.contains("```")) {
            return GeminiOutputClass.MARKDOWN_WRAPPER;
        }
        try (JsonParser parser = objectMapper.getFactory().createParser(trimmed)) {
            JsonNode value = objectMapper.readTree(parser);
            if (value != null && parser.nextToken() == null) {
                if (value.isObject()) {
                    return value.isEmpty()
                            ? GeminiOutputClass.EMPTY_JSON_OBJECT : GeminiOutputClass.VALID_JSON_OBJECT;
                }
                if (value.isArray()) {
                    return value.isEmpty()
                            ? GeminiOutputClass.EMPTY_JSON_ARRAY : GeminiOutputClass.VALID_JSON_ARRAY;
                }
                return GeminiOutputClass.MALFORMED_JSON;
            }
        } catch (Exception ignored) {
            // Classification continues without retaining the provider text.
        }
        String normalized = trimmed.toLowerCase(java.util.Locale.ROOT);
        if (isRefusal(normalized)) {
            return GeminiOutputClass.REFUSAL_PATTERN;
        }
        if (trimmed.length() <= 160 && !trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return GeminiOutputClass.PLAIN_TEXT_SHORT;
        }
        return GeminiOutputClass.MALFORMED_JSON;
    }

    private static boolean isRefusal(String normalized) {
        return normalized.contains("i cannot")
                || normalized.contains("i can't")
                || normalized.contains("i am unable")
                || normalized.contains("i'm unable")
                || normalized.contains("unable to comply")
                || normalized.contains("cannot comply")
                || normalized.contains("can't comply")
                || normalized.contains("i am sorry")
                || normalized.contains("i'm sorry")
                || normalized.contains("i refuse");
    }

    private static boolean parsesAsSingleJsonValue(ObjectMapper objectMapper, String text) {
        if (text == null || text.isBlank() || text.contains("```")) {
            return false;
        }
        try (JsonParser parser = objectMapper.getFactory().createParser(text.trim())) {
            JsonNode value = objectMapper.readTree(parser);
            return value != null && parser.nextToken() == null;
        } catch (Exception ignored) {
            return false;
        }
    }
}
