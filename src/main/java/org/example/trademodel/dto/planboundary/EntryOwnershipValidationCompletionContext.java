package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

public class EntryOwnershipValidationCompletionContext {

    private EntryOwnershipValidationResult validationResult;
    private SourceTraceEntryCompletionResult completionResult;
    private final SourceTraceEntrySourceReviewModeEnum reviewMode =
            SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY;
    private final boolean completionReady = false;
    private final boolean manualReviewRequired = true;
    private final boolean notTradeInstruction = true;
    private final List<String> missingFields = new ArrayList<>();

    public static EntryOwnershipValidationCompletionContext from(
            EntryOwnershipValidationResult validationResult,
            SourceTraceEntryCompletionResult completionResult
    ) {
        EntryOwnershipValidationResult safeValidation = validationResult == null
                ? EntryOwnershipValidationResult.missingSource(null, null, List.of("entryOwnershipValidationResult"))
                : validationResult;
        SourceTraceEntryCompletionResult safeCompletion = completionResult == null
                ? SourceTraceEntryCompletionResult.incomplete(
                        safeValidation.getSymbol(),
                        safeValidation.getTimeframe(),
                        SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION,
                        List.of("sourceTraceEntryCompletionResult")
                )
                : completionResult;

        EntryOwnershipValidationCompletionContext context = new EntryOwnershipValidationCompletionContext();
        context.validationResult = safeValidation;
        context.completionResult = safeCompletion;
        context.missingFields.addAll(safeValidation.getMissingFields());
        context.missingFields.addAll(safeCompletion.getMissingFields());
        return context;
    }

    public EntryOwnershipValidationResult getValidationResult() {
        return validationResult;
    }

    public SourceTraceEntryCompletionResult getCompletionResult() {
        return completionResult;
    }

    public SourceTraceEntrySourceReviewModeEnum getReviewMode() {
        return reviewMode;
    }

    public boolean isCompletionReady() {
        return completionReady;
    }

    public boolean isManualReviewRequired() {
        return manualReviewRequired;
    }

    public boolean isNotTradeInstruction() {
        return notTradeInstruction;
    }

    public List<String> getMissingFields() {
        return new ArrayList<>(missingFields);
    }
}
