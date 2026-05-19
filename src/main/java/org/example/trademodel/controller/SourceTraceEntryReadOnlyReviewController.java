package org.example.trademodel.controller;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionDowngradeReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionTransitionEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryReadOnlyApiResponseDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inert read-only review endpoint for already-built SourceTrace entry API
 * response DTO output.
 *
 * <p>This controller does not call resolver, validation, readiness, dashboard,
 * schema, order, automation, or external data paths.
 */
@RestController
@RequestMapping(SourceTraceEntryReadOnlyReviewController.ROUTE)
public class SourceTraceEntryReadOnlyReviewController {

    public static final String ROUTE = "/api/review/source-trace-entry-completion";
    public static final String STATE_PATH = "/state";

    private static final String API_OUTPUT_FIELD = "sourceTraceEntryReadOnlyApiResponseOutput";
    private static final String SEAM_UNWIRED_FIELD = "readOnlyIntegrationSeamUnwired";
    private static final String MISSING_FIELDS_FIELD = "missingFields";
    private static final String BLOCKING_FIELDS_FIELD = "blockingFields";
    private static final Set<String> ALLOWED_DOWNGRADE_REASONS = Set.of(
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD.name(),
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION.name(),
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED.name()
    );

    private final Supplier<SourceTraceEntryReadOnlyApiResponseDTO> responseSupplier;

    public SourceTraceEntryReadOnlyReviewController() {
        this(() -> null);
    }

    SourceTraceEntryReadOnlyReviewController(Supplier<SourceTraceEntryReadOnlyApiResponseDTO> responseSupplier) {
        this.responseSupplier = responseSupplier == null ? () -> null : responseSupplier;
    }

    @GetMapping(STATE_PATH)
    public SourceTraceEntryReadOnlyApiResponseDTO reviewState() {
        try {
            return reviewResponse(responseSupplier.get());
        } catch (RuntimeException ignored) {
            return unavailableResponse();
        }
    }

    private SourceTraceEntryReadOnlyApiResponseDTO reviewResponse(SourceTraceEntryReadOnlyApiResponseDTO supplied) {
        if (supplied == null) {
            return unavailableResponse();
        }

        List<String> missingFields = safeList(supplied.getMissingFields());
        List<String> unsafeFields = safeList(supplied.getUnsafeFields());
        List<String> blockingFields = safeList(supplied.getBlockingFields());
        List<String> safetyGaps = requiredSafetyGaps(supplied, missingFields, blockingFields);

        List<String> reviewMissingFields = dedupe(combine(missingFields, safetyGaps));
        List<String> reviewUnsafeFields = dedupe(unsafeFields);
        List<String> reviewBlockingFields = dedupe(combine(blockingFields, reviewMissingFields, reviewUnsafeFields));

        SourceTraceEntryReadOnlyApiResponseDTO response = new SourceTraceEntryReadOnlyApiResponseDTO();
        response.setSymbol(supplied.getSymbol());
        response.setTimeframe(supplied.getTimeframe());
        response.setCompletionStatus(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE.name());
        response.setCompletionTransition(SourceTraceEntryPositiveCompletionTransitionEnum.NONE.name());
        response.setDowngradeReason(downgradeReason(supplied, reviewUnsafeFields, safetyGaps));
        response.setReviewMode(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY.name());
        response.setReadOnlyIntegrationSeamUnwired(supplied.isReadOnlyIntegrationSeamUnwired());
        response.setManualReviewRequired(true);
        response.setNotTradeInstruction(true);
        response.setSourceTraceEntryCompleted(false);
        response.setCompletionReady(false);
        response.setMissingFields(reviewMissingFields);
        response.setUnsafeFields(reviewUnsafeFields);
        response.setBlockingFields(reviewBlockingFields);
        applyDowngradeCopy(
                response,
                SourceTraceEntryPositiveCompletionDowngradeReasonEnum.valueOf(response.getDowngradeReason())
        );
        return response;
    }

    private SourceTraceEntryReadOnlyApiResponseDTO unavailableResponse() {
        SourceTraceEntryReadOnlyApiResponseDTO response = new SourceTraceEntryReadOnlyApiResponseDTO();
        List<String> blockers = List.of(API_OUTPUT_FIELD, SEAM_UNWIRED_FIELD, BLOCKING_FIELDS_FIELD);
        response.setMissingFields(blockers);
        response.setBlockingFields(blockers);
        response.setDowngradeReason(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD.name());
        applyDowngradeCopy(response, SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD);
        return response;
    }

    private List<String> requiredSafetyGaps(
            SourceTraceEntryReadOnlyApiResponseDTO supplied,
            List<String> missingFields,
            List<String> blockingFields
    ) {
        List<String> gaps = new ArrayList<>();
        if (!SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE.name().equals(supplied.getCompletionStatus())) {
            gaps.add("completionStatus");
        }
        if (!SourceTraceEntryPositiveCompletionTransitionEnum.NONE.name().equals(supplied.getCompletionTransition())) {
            gaps.add("completionTransition");
        }
        if (!isAllowedReason(supplied.getDowngradeReason())) {
            gaps.add("downgradeReason");
        }
        if (!SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY.name().equals(supplied.getReviewMode())) {
            gaps.add("reviewMode");
        }
        if (!supplied.isManualReviewRequired()) {
            gaps.add("manualReviewRequired");
        }
        if (!supplied.isNotTradeInstruction()) {
            gaps.add("notTradeInstruction");
        }
        if (supplied.isSourceTraceEntryCompleted()) {
            gaps.add("sourceTraceEntryCompleted");
        }
        if (supplied.isCompletionReady()) {
            gaps.add("completionReady");
        }
        if (!supplied.isReadOnlyIntegrationSeamUnwired()) {
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

    private String downgradeReason(
            SourceTraceEntryReadOnlyApiResponseDTO supplied,
            List<String> unsafeFields,
            List<String> safetyGaps
    ) {
        if (!unsafeFields.isEmpty()
                || SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION.name()
                .equals(supplied.getDowngradeReason())) {
            return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION.name();
        }
        if (!safetyGaps.isEmpty()) {
            return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD.name();
        }
        if (SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED.name()
                .equals(supplied.getDowngradeReason())) {
            return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED.name();
        }
        return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD.name();
    }

    private boolean isAllowedReason(String downgradeReason) {
        return downgradeReason != null && ALLOWED_DOWNGRADE_REASONS.contains(downgradeReason);
    }

    private void applyDowngradeCopy(
            SourceTraceEntryReadOnlyApiResponseDTO response,
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum downgradeReason
    ) {
        if (SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED.equals(downgradeReason)) {
            response.setDowngradeLabel("Completion path unwired");
            response.setHelperCopy("The read-only endpoint is present, but completion wiring is not active.");
            return;
        }
        if (SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION.equals(downgradeReason)) {
            response.setDowngradeLabel("Unsafe completion evidence");
            response.setHelperCopy("Unsafe or runtime-like evidence blocks completion and requires review.");
            return;
        }
        response.setDowngradeLabel("Missing required source evidence");
        response.setHelperCopy("Required API response evidence is missing or malformed.");
    }

    private List<String> safeList(List<String> fields) {
        return fields == null ? List.of() : new ArrayList<>(fields);
    }

    private List<String> combine(List<String> first, List<String> second) {
        List<String> combined = new ArrayList<>(first);
        combined.addAll(second);
        return combined;
    }

    private List<String> combine(List<String> first, List<String> second, List<String> third) {
        List<String> combined = combine(first, second);
        combined.addAll(third);
        return combined;
    }

    private List<String> dedupe(List<String> fields) {
        return new ArrayList<>(new LinkedHashSet<>(fields));
    }
}
