package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipReviewRequest;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipReviewResult;

/**
 * Read-only boundary contract for future production ownership review.
 *
 * <p>P88 defines the interface only. No production implementation is provided,
 * no Spring registration is added, and SourceTrace completion remains unwired.
 */
public interface SourceTraceEntryProductionOwnershipReviewBoundary {

    SourceTraceEntryProductionOwnershipReviewResult reviewEntryOwnership(
            SourceTraceEntryProductionOwnershipReviewRequest request
    );
}
