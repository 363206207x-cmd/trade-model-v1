package org.example.trademodel.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionDowngradeReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionTransitionEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryReadOnlyApiResponseDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntryReadOnlyDisplayDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;

/**
 * Inert mapper from already-built read-only display DTO output to an API
 * response shape.
 *
 * <p>This class is deliberately not a Spring service, controller, endpoint,
 * readiness gate, schema write, order path, or automation surface.
 */
public class SourceTraceEntryReadOnlyApiResponseMapper {

    private static final String DISPLAY_OUTPUT_FIELD = "sourceTraceEntryReadOnlyDisplayOutput";
    private static final String SEAM_UNWIRED_FIELD = "readOnlyIntegrationSeamUnwired";
    private static final String MISSING_FIELDS_FIELD = "missingFields";
    private static final String BLOCKING_FIELDS_FIELD = "blockingFields";

    private static final Set<String> UNSAFE_FIELD_MARKERS = Set.of(
            "LATEST_PRICE_ONLY",
            "RAW_KLINE_ONLY",
            "AI_TEXT",
            "DASHBOARD_TEXT",
            "EXTERNAL",
            "EXTERNAL_DATA",
            "ORDER",
            "ORDER_DATA",
            "EXECUTION",
            "EXECUTION_DATA",
            "BOUNDARYCANDIDATESERVICE_VALID",
            "EXECUTIONPLAN_READY",
            "SOURCETRACE_RUNTIME_COMPLETION",
            "PRODUCTION_COMPLETION",
            "TRADE_READY",
            "TRADEREADY",
            "READY_TO_TRADE",
            "READYTOTRADE",
            "ENTRY_READY",
            "ENTRYREADY",
            "EXECUTION_READY",
            "EXECUTIONREADY",
            "VALID",
            "COMPLETED",
            "SIGNAL",
            "BUY",
            "SELL",
            "OPEN",
            "CLOSE",
            "REVERSE",
            "ADVICE",
            "TRADE_ADVICE",
            "TRADEADVICE",
            "ENTRY_INSTRUCTION",
            "ENTRYINSTRUCTION",
            "TRADE_INSTRUCTION",
            "TRADEINSTRUCTION",
            "ORDER_INSTRUCTION",
            "ORDERINSTRUCTION",
            "UNSAFE_COMPLETION"
    );

    public SourceTraceEntryReadOnlyApiResponseDTO map(SourceTraceEntryReadOnlyDisplayDTO display) {
        if (display == null) {
            return missingDisplayOutput();
        }

        List<String> displayMissingFields = safeList(display.getMissingFields());
        List<String> displayUnsafeFields = safeList(display.getUnsafeFields());
        List<String> displayBlockingFields = safeList(display.getBlockingFields());
        List<String> safetyGaps = safetyGaps(display, displayMissingFields, displayBlockingFields);
        List<String> blockers = new ArrayList<>();
        blockers.addAll(displayBlockingFields);
        blockers.addAll(displayMissingFields);
        blockers.addAll(displayUnsafeFields);
        blockers.addAll(safetyGaps);
        List<String> dedupedBlockers = dedupe(blockers);
        List<String> unsafeFields = dedupe(combinedUnsafeFields(displayUnsafeFields, dedupedBlockers));

        SourceTraceEntryReadOnlyApiResponseDTO response = new SourceTraceEntryReadOnlyApiResponseDTO();
        response.setSymbol(display.getSymbol());
        response.setTimeframe(display.getTimeframe());
        response.setCompletionStatus(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE.name());
        response.setCompletionTransition(SourceTraceEntryPositiveCompletionTransitionEnum.NONE.name());
        response.setDowngradeReason(
                downgradeReason(display.getDowngradeReason(), dedupedBlockers, unsafeFields, safetyGaps).name()
        );
        response.setReviewMode(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY.name());
        response.setReadOnlyIntegrationSeamUnwired(display.isReadOnlyIntegrationSeamUnwired());
        response.setManualReviewRequired(true);
        response.setNotTradeInstruction(true);
        response.setSourceTraceEntryCompleted(false);
        response.setCompletionReady(false);
        response.setMissingFields(dedupedBlockers);
        response.setUnsafeFields(unsafeFields);
        response.setBlockingFields(dedupedBlockers);
        applyDowngradeCopy(
                response,
                SourceTraceEntryPositiveCompletionDowngradeReasonEnum.valueOf(response.getDowngradeReason())
        );
        return response;
    }

    private SourceTraceEntryReadOnlyApiResponseDTO missingDisplayOutput() {
        SourceTraceEntryReadOnlyApiResponseDTO response = new SourceTraceEntryReadOnlyApiResponseDTO();
        List<String> blockers = List.of(DISPLAY_OUTPUT_FIELD, SEAM_UNWIRED_FIELD, BLOCKING_FIELDS_FIELD);
        response.setMissingFields(blockers);
        response.setBlockingFields(blockers);
        response.setDowngradeReason(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD.name());
        applyDowngradeCopy(response, SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD);
        return response;
    }

    private List<String> safetyGaps(
            SourceTraceEntryReadOnlyDisplayDTO display,
            List<String> missingFields,
            List<String> blockingFields
    ) {
        List<String> gaps = new ArrayList<>();
        if (!SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE.name().equals(display.getCompletionStatus())) {
            gaps.add("completionStatus");
        }
        if (!SourceTraceEntryPositiveCompletionTransitionEnum.NONE.name().equals(display.getCompletionTransition())) {
            gaps.add("completionTransition");
        }
        if (!isAllowedDowngrade(display.getDowngradeReason())) {
            gaps.add("downgradeReason");
        }
        if (!SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY.name().equals(display.getReviewMode())) {
            gaps.add("reviewMode");
        }
        if (!display.isManualReviewRequired()) {
            gaps.add("manualReviewRequired");
        }
        if (!display.isNotTradeInstruction()) {
            gaps.add("notTradeInstruction");
        }
        if (display.isSourceTraceEntryCompleted()) {
            gaps.add("sourceTraceEntryCompleted");
        }
        if (display.isCompletionReady()) {
            gaps.add("completionReady");
        }
        if (!display.isReadOnlyIntegrationSeamUnwired()) {
            gaps.add(SEAM_UNWIRED_FIELD);
        }
        if (missingFields.isEmpty()) {
            gaps.add(MISSING_FIELDS_FIELD);
        }
        if (blockingFields.isEmpty()) {
            gaps.add(BLOCKING_FIELDS_FIELD);
        }
        return gaps;
    }

    private boolean isAllowedDowngrade(String downgradeReason) {
        return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD.name()
                .equals(downgradeReason)
                || SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION.name()
                .equals(downgradeReason)
                || SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED.name()
                .equals(downgradeReason);
    }

    private SourceTraceEntryPositiveCompletionDowngradeReasonEnum downgradeReason(
            String displayDowngradeReason,
            List<String> blockers,
            List<String> unsafeFields,
            List<String> safetyGaps
    ) {
        if (!unsafeFields.isEmpty()
                || SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION.name()
                .equals(displayDowngradeReason)) {
            return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION;
        }
        if (hasHardSafetyGaps(safetyGaps)) {
            return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD;
        }
        if (SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED.name()
                .equals(displayDowngradeReason)
                && blockers.contains(SEAM_UNWIRED_FIELD)) {
            return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED;
        }
        return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD;
    }

    private boolean hasHardSafetyGaps(List<String> safetyGaps) {
        return !safetyGaps.isEmpty();
    }

    private void applyDowngradeCopy(
            SourceTraceEntryReadOnlyApiResponseDTO response,
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum downgradeReason
    ) {
        if (SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED.equals(downgradeReason)) {
            response.setDowngradeLabel("Completion path unwired");
            response.setHelperCopy("The read-only response is present, but completion wiring is not active.");
            return;
        }
        if (SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION.equals(downgradeReason)) {
            response.setDowngradeLabel("Unsafe completion evidence");
            response.setHelperCopy("Unsafe or runtime-like evidence blocks completion and requires review.");
            return;
        }
        response.setDowngradeLabel("Missing required source evidence");
        response.setHelperCopy("Required display evidence is missing or malformed.");
    }

    private List<String> combinedUnsafeFields(List<String> unsafeFields, List<String> blockers) {
        List<String> combined = new ArrayList<>(unsafeFields);
        for (String blocker : blockers) {
            if (isUnsafeField(blocker)) {
                combined.add(blocker);
            }
        }
        return combined;
    }

    private boolean isUnsafeField(String field) {
        String normalized = normalize(field);
        for (String marker : UNSAFE_FIELD_MARKERS) {
            if (normalized.equals(marker)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String field) {
        if (field == null) {
            return "";
        }
        return field.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_");
    }

    private List<String> safeList(List<String> fields) {
        return fields == null ? List.of() : new ArrayList<>(fields);
    }

    private List<String> dedupe(List<String> fields) {
        return new ArrayList<>(new LinkedHashSet<>(fields));
    }
}
