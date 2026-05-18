package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.EntryOwnershipRequest;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationResult;

/**
 * Fail-closed validation boundary for future SourceTrace entry ownership.
 */
public interface SourceTraceEntryOwnershipValidator {

    EntryOwnershipValidationResult validateEntryOwnership(EntryOwnershipRequest request);
}
