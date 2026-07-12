package org.example.trademodel.ai;

import com.fasterxml.jackson.core.JsonProcessingException;

/** Typed contract failure that carries only sanitized Gemini Interactions diagnostics. */
public final class GeminiInteractionContractException extends JsonProcessingException {
    private final GeminiInteractionFailureReason reason;
    private final GeminiInteractionDiagnostic diagnostic;

    public GeminiInteractionContractException(
            GeminiInteractionFailureReason reason, GeminiInteractionDiagnostic diagnostic) {
        super(reason.name());
        this.reason = reason;
        this.diagnostic = diagnostic == null ? null : diagnostic.withFailure(reason);
    }

    public GeminiInteractionFailureReason reason() {
        return reason;
    }

    public GeminiInteractionDiagnostic diagnostic() {
        return diagnostic;
    }
}
