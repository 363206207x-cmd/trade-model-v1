package org.example.trademodel.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionContractDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionDowngradeReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionTransitionEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryReadOnlyDisplayDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;

/**
 * Inert mapper from already-built read-only seam output to display metadata.
 *
 * <p>This class is deliberately not a Spring service and does not create an
 * endpoint, dashboard binding, readiness gate, schema write, order path, or
 * automation surface.
 */
public class SourceTraceEntryReadOnlyDisplayMapper {

    private static final String SEAM_OUTPUT_FIELD = "sourceTraceEntryReadOnlyIntegrationSeamOutput";
    private static final String SEAM_UNWIRED_FIELD = "readOnlyIntegrationSeamUnwired";
    private static final String MISSING_FIELDS_FIELD = "missingFields";

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
            "UNSAFE_COMPLETION",
            "LIQUIDITYSTRESSREQUIRESREVIEW",
            "LIQUIDITYSTAMPEDEREQUIRESREVIEW",
            "EVENTDATAMISSING",
            "MULTITIMEFRAMEAGREEMENTONLY",
            "WICKORPINBAREVIDENCEONLY"
    );

    public SourceTraceEntryReadOnlyDisplayDTO map(SourceTraceEntryPositiveCompletionContractDTO seamOutput) {
        if (seamOutput == null) {
            return missingSeamOutput();
        }

        List<String> seamMissingFields = safeList(seamOutput.getMissingFields());
        List<String> blockers = new ArrayList<>(seamMissingFields);
        List<String> safetyGaps = safetyGaps(seamOutput, seamMissingFields);
        blockers.addAll(safetyGaps);
        List<String> dedupedBlockers = dedupe(blockers);
        List<String> unsafeFields = unsafeFields(dedupedBlockers);

        SourceTraceEntryReadOnlyDisplayDTO display = new SourceTraceEntryReadOnlyDisplayDTO();
        display.setSymbol(seamOutput.getSymbol());
        display.setTimeframe(seamOutput.getTimeframe());
        display.setCompletionStatus(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE.name());
        display.setCompletionTransition(SourceTraceEntryPositiveCompletionTransitionEnum.NONE.name());
        display.setDowngradeReason(
                downgradeReason(
                        seamOutput.getDowngradeReason(),
                        dedupedBlockers,
                        unsafeFields,
                        seamMissingFields.contains(SEAM_UNWIRED_FIELD),
                        hasHardSafetyGaps(safetyGaps)
                ).name()
        );
        display.setReviewMode(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY.name());
        display.setReadOnlyIntegrationSeamUnwired(seamMissingFields.contains(SEAM_UNWIRED_FIELD));
        display.setManualReviewRequired(true);
        display.setNotTradeInstruction(true);
        display.setSourceTraceEntryCompleted(false);
        display.setCompletionReady(false);
        display.setMissingFields(dedupedBlockers);
        display.setUnsafeFields(unsafeFields);
        display.setBlockingFields(dedupedBlockers);
        applyDowngradeCopy(
                display,
                SourceTraceEntryPositiveCompletionDowngradeReasonEnum.valueOf(display.getDowngradeReason())
        );
        return display;
    }

    private SourceTraceEntryReadOnlyDisplayDTO missingSeamOutput() {
        SourceTraceEntryReadOnlyDisplayDTO display = new SourceTraceEntryReadOnlyDisplayDTO();
        List<String> blockers = List.of(SEAM_OUTPUT_FIELD, SEAM_UNWIRED_FIELD);
        display.setMissingFields(blockers);
        display.setBlockingFields(blockers);
        display.setDowngradeReason(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD.name());
        applyDowngradeCopy(display, SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD);
        return display;
    }

    private List<String> safetyGaps(
            SourceTraceEntryPositiveCompletionContractDTO seamOutput,
            List<String> seamMissingFields
    ) {
        List<String> gaps = new ArrayList<>();
        if (seamOutput.getCompletionStatus() != SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE) {
            gaps.add("completionStatus");
        }
        if (seamOutput.getCompletionTransition() != SourceTraceEntryPositiveCompletionTransitionEnum.NONE) {
            gaps.add("completionTransition");
        }
        if (seamOutput.getDowngradeReason() == null) {
            gaps.add("downgradeReason");
        }
        if (seamOutput.getReviewMode() != SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY) {
            gaps.add("reviewMode");
        }
        if (!seamOutput.isManualReviewRequired()) {
            gaps.add("manualReviewRequired");
        }
        if (!seamOutput.isNotTradeInstruction()) {
            gaps.add("notTradeInstruction");
        }
        if (seamOutput.isSourceTraceEntryCompleted()) {
            gaps.add("sourceTraceEntryCompleted");
        }
        if (seamOutput.isCompletionReady()) {
            gaps.add("completionReady");
        }
        if (seamMissingFields.isEmpty()) {
            gaps.add(MISSING_FIELDS_FIELD);
        }
        if (!seamMissingFields.contains(SEAM_UNWIRED_FIELD)) {
            gaps.add(SEAM_UNWIRED_FIELD);
        }
        return gaps;
    }

    private SourceTraceEntryPositiveCompletionDowngradeReasonEnum downgradeReason(
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum seamReason,
            List<String> blockers,
            List<String> unsafeFields,
            boolean seamUnwiredPresent,
            boolean hardSafetyGapsPresent
    ) {
        if (!unsafeFields.isEmpty()
                || SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION.equals(seamReason)) {
            return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION;
        }
        if (hardSafetyGapsPresent) {
            return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD;
        }
        if (!blockers.isEmpty()) {
            if (SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED.equals(seamReason)
                    && seamUnwiredPresent
                    && blockers.contains(SEAM_UNWIRED_FIELD)
                    && !blockers.contains(MISSING_FIELDS_FIELD)) {
                return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED;
            }
            return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD;
        }
        return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD;
    }

    private boolean hasHardSafetyGaps(List<String> safetyGaps) {
        for (String safetyGap : safetyGaps) {
            if (!SEAM_UNWIRED_FIELD.equals(safetyGap)) {
                return true;
            }
        }
        return false;
    }

    private void applyDowngradeCopy(
            SourceTraceEntryReadOnlyDisplayDTO display,
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum downgradeReason
    ) {
        if (SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED.equals(downgradeReason)) {
            display.setDowngradeLabel("Completion path unwired");
            display.setDowngradeHelperCopy(
                    "The read-only seam is present, but completion wiring is not active."
            );
            return;
        }
        if (SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION.equals(downgradeReason)) {
            display.setDowngradeLabel("Unsafe completion evidence");
            display.setDowngradeHelperCopy(
                    "Unsafe or runtime-like evidence blocks completion and requires review."
            );
            return;
        }
        display.setDowngradeLabel("Missing required source evidence");
        display.setDowngradeHelperCopy("Required ownership/source/freshness/conflict evidence is missing.");
    }

    private List<String> unsafeFields(List<String> fields) {
        List<String> unsafeFields = new ArrayList<>();
        for (String field : fields) {
            if (isUnsafeField(field)) {
                unsafeFields.add(field);
            }
        }
        return unsafeFields;
    }

    private boolean isUnsafeField(String field) {
        String normalized = normalize(field);
        for (String marker : UNSAFE_FIELD_MARKERS) {
            if (normalized.equals(marker) || normalized.contains(marker)) {
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
