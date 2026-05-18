package org.example.trademodel.service.impl;

import java.util.List;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionResult;
import org.example.trademodel.service.SourceTraceEntryCompletionContract;

/**
 * Fail-closed skeleton resolver for future SourceTrace entry completion.
 *
 * <p>P36 deliberately does not register this as a Spring service or wire it
 * into validation/readiness. It exists only as a concrete, testable boundary.
 */
public class FailClosedSourceTraceEntryCompletionResolver implements SourceTraceEntryCompletionContract {

    @Override
    public SourceTraceEntryCompletionResult resolveEntryCompletion(
            EntryOwnershipValidationResult validationResult
    ) {
        if (validationResult == null) {
            return SourceTraceEntryCompletionResult.incomplete(
                    null,
                    null,
                    SourceTraceEntryCompletionMissingReasonEnum.UNSAFE_COMPLETION,
                    List.of("entryOwnershipValidationResult")
            );
        }

        List<String> validationMissingFields = validationResult.getMissingFields();
        if (validationMissingFields.isEmpty()) {
            return SourceTraceEntryCompletionResult.incomplete(
                    validationResult.getSymbol(),
                    validationResult.getTimeframe(),
                    SourceTraceEntryCompletionMissingReasonEnum.UNSAFE_COMPLETION,
                    List.of("entryOwnershipValidationResult.missingFields")
            );
        }

        SourceTraceEntryCompletionMissingReasonEnum missingReason =
                validationMissingFields.contains("sourceTraceEntryOwnershipCompletionPath")
                        ? SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED
                        : SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION;
        return SourceTraceEntryCompletionResult.incomplete(
                validationResult.getSymbol(),
                validationResult.getTimeframe(),
                missingReason,
                validationMissingFields
        );
    }
}
