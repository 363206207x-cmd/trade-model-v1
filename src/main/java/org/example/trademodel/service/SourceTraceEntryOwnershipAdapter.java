package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.EntryOwnershipRequest;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipResult;

/**
 * Future adapter contract for SourceTrace entry ownership resolution.
 *
 * <p>No production implementation is provided in P31. This interface only
 * defines the DTO boundary for a later fail-closed implementation.
 */
public interface SourceTraceEntryOwnershipAdapter {

    SourceTraceEntrySourceOwnershipResult resolveEntryOwnership(EntryOwnershipRequest request);
}
