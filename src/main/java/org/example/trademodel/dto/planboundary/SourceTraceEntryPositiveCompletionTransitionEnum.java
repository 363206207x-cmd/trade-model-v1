package org.example.trademodel.dto.planboundary;

public enum SourceTraceEntryPositiveCompletionTransitionEnum {
    NONE,
    INCOMPLETE_TO_POSITIVE_FIXTURE_READY,
    POSITIVE_FIXTURE_READY_TO_INCOMPLETE,
    INCOMPLETE_TO_POSITIVE_DESIGN_REVIEW_ONLY,
    POSITIVE_DESIGN_REVIEW_ONLY_TO_INCOMPLETE
}
