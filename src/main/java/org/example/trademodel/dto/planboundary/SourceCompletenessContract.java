package org.example.trademodel.dto.planboundary;

import java.util.List;

public interface SourceCompletenessContract {

    List<String> getMissingFields();

    SourceTraceFallbackStatusEnum getFallbackStatus();

    default boolean isComplete() {
        return getMissingFields() == null || getMissingFields().isEmpty();
    }
}
