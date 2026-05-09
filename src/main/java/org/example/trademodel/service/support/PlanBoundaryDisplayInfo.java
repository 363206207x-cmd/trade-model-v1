package org.example.trademodel.service.support;

import java.math.BigDecimal;

/**
 * Read-model summary derived from {@code plan_boundary_json}. Presentation-only; no raw JSON; no trading fields.
 */
public record PlanBoundaryDisplayInfo(
        String parseStatus,
        String source,
        String confidence,
        String stateLabel,
        String displayText,
        String warningText,
        String invalidPriceDirection,
        BigDecimal invalidPriceThreshold) {
}
