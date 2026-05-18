package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.EntryOwnershipValidationResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionResult;

/**
 * Contract boundary for future SourceTrace entry completion.
 *
 * <p>P34 defines only the boundary. No production implementation is provided,
 * and validation remains fail closed until a later phase safely wires completion.
 */
public interface SourceTraceEntryCompletionContract {

    SourceTraceEntryCompletionResult resolveEntryCompletion(EntryOwnershipValidationResult validationResult);
}
