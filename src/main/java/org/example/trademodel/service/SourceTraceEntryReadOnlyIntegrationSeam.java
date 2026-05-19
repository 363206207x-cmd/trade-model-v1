package org.example.trademodel.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationCompletionContext;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionContractDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionDowngradeReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionTransitionEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryReadOnlyCompletionRequest;

/**
 * Read-only, unwired seam between already-built validation/completion context
 * and the read-only SourceTrace entry completion assembler.
 *
 * <p>This class is deliberately not a Spring service and does not wire into
 * resolver, validation, readiness, dashboard, schema, order, automation, or
 * external data paths.
 */
public class SourceTraceEntryReadOnlyIntegrationSeam {

    private static final String CONTEXT_MISSING_FIELD = "entryOwnershipValidationCompletionContext";
    private static final String REQUEST_MISSING_FIELD = "sourceTraceEntryReadOnlyCompletionRequest";
    private static final String SEAM_UNWIRED_FIELD = "readOnlyIntegrationSeamUnwired";

    private final SourceTraceEntryReadOnlyCompletionAssembler readOnlyAssembler;

    public SourceTraceEntryReadOnlyIntegrationSeam() {
        this(new SourceTraceEntryReadOnlyCompletionAssembler());
    }

    SourceTraceEntryReadOnlyIntegrationSeam(SourceTraceEntryReadOnlyCompletionAssembler readOnlyAssembler) {
        this.readOnlyAssembler = readOnlyAssembler == null
                ? new SourceTraceEntryReadOnlyCompletionAssembler()
                : readOnlyAssembler;
    }

    public SourceTraceEntryPositiveCompletionContractDTO combine(
            EntryOwnershipValidationCompletionContext validationCompletionContext,
            SourceTraceEntryReadOnlyCompletionRequest readOnlyRequest
    ) {
        if (validationCompletionContext == null || readOnlyRequest == null) {
            List<String> missingFields = new ArrayList<>();
            if (validationCompletionContext == null) {
                missingFields.add(CONTEXT_MISSING_FIELD);
            }
            if (readOnlyRequest == null) {
                missingFields.add(REQUEST_MISSING_FIELD);
            }
            return fail(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD, missingFields);
        }

        SourceTraceEntryPositiveCompletionContractDTO assembled = readOnlyAssembler.assemble(readOnlyRequest);
        assembled.setCompletionStatus(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assembled.setCompletionTransition(SourceTraceEntryPositiveCompletionTransitionEnum.NONE);
        assembled.setDowngradeReason(seamDowngradeReason(assembled.getDowngradeReason()));
        assembled.setMissingFields(combinedMissingFields(validationCompletionContext, assembled));
        return assembled;
    }

    private SourceTraceEntryPositiveCompletionContractDTO fail(
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum downgradeReason,
            List<String> missingFields
    ) {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                new SourceTraceEntryPositiveCompletionContractDTO();
        dto.setDowngradeReason(downgradeReason);
        dto.setMissingFields(missingFields);
        return dto;
    }

    private SourceTraceEntryPositiveCompletionDowngradeReasonEnum seamDowngradeReason(
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum assemblerDowngradeReason
    ) {
        if (SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD
                .equals(assemblerDowngradeReason)
                || SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION
                .equals(assemblerDowngradeReason)) {
            return assemblerDowngradeReason;
        }
        return SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED;
    }

    private List<String> combinedMissingFields(
            EntryOwnershipValidationCompletionContext validationCompletionContext,
            SourceTraceEntryPositiveCompletionContractDTO assembled
    ) {
        Set<String> missingFields = new LinkedHashSet<>();
        missingFields.addAll(validationCompletionContext.getMissingFields());
        missingFields.addAll(assembled.getMissingFields());
        missingFields.add(SEAM_UNWIRED_FIELD);
        return new ArrayList<>(missingFields);
    }
}
