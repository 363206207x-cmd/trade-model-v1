package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.EntryOwnershipValidationCompletionContext;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionResult;

/**
 * Minimal assembler for validator-facing SourceTrace entry completion context.
 *
 * <p>P38 deliberately keeps this as an unregistered skeleton facade. It does
 * not wire resolver output into readiness, dashboard, schema, orders, or
 * automation.
 */
public class EntryCompletionValidationContextAssembler {

    public EntryOwnershipValidationCompletionContext assemble(
            EntryOwnershipValidationResult validationResult,
            SourceTraceEntryCompletionResult completionResult
    ) {
        return EntryOwnershipValidationCompletionContext.from(validationResult, completionResult);
    }
}
