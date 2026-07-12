package org.example.trademodel.ai;

/**
 * Presence-only and count-only diagnostics for Gemini Interactions responses.
 * No provider values, generated text, identifiers, prompts, headers, or token counts are retained.
 */
public record GeminiInteractionDiagnostic(
        String interactionStatus,
        boolean interactionIdPresent,
        boolean usagePresent,
        boolean totalInputTokensPresent,
        boolean totalOutputTokensPresent,
        boolean totalThoughtTokensPresent,
        boolean totalTokensPresent,
        int stepCount,
        int modelOutputStepCount,
        boolean finalModelOutputPresent,
        int finalTextBlockCount,
        int finalTextLength,
        String finalJsonParseStatus,
        String v1ContractStatus,
        GeminiInteractionFailureReason failureReason
) {
    public GeminiInteractionDiagnostic withFinalOutput(
            boolean outputPresent, int textBlockCount, int textLength, String jsonParseStatus) {
        return new GeminiInteractionDiagnostic(
                interactionStatus, interactionIdPresent, usagePresent,
                totalInputTokensPresent, totalOutputTokensPresent,
                totalThoughtTokensPresent, totalTokensPresent,
                stepCount, modelOutputStepCount, outputPresent,
                Math.max(0, textBlockCount), Math.max(0, textLength),
                safeStatus(jsonParseStatus), v1ContractStatus, failureReason);
    }

    public GeminiInteractionDiagnostic withFailure(GeminiInteractionFailureReason reason) {
        return new GeminiInteractionDiagnostic(
                interactionStatus, interactionIdPresent, usagePresent,
                totalInputTokensPresent, totalOutputTokensPresent,
                totalThoughtTokensPresent, totalTokensPresent,
                stepCount, modelOutputStepCount, finalModelOutputPresent,
                finalTextBlockCount, finalTextLength, finalJsonParseStatus,
                v1ContractStatus, reason);
    }

    public GeminiInteractionDiagnostic withV1ContractStatus(
            String status, GeminiInteractionFailureReason reason) {
        return new GeminiInteractionDiagnostic(
                interactionStatus, interactionIdPresent, usagePresent,
                totalInputTokensPresent, totalOutputTokensPresent,
                totalThoughtTokensPresent, totalTokensPresent,
                stepCount, modelOutputStepCount, finalModelOutputPresent,
                finalTextBlockCount, finalTextLength, finalJsonParseStatus,
                safeStatus(status), reason);
    }

    private static String safeStatus(String status) {
        return status == null || status.isBlank() ? "NOT_CHECKED" : status;
    }
}
