package org.example.trademodel.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionContractDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionDowngradeReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionTransitionEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryReadOnlyCompletionRequest;

/**
 * Read-only, unwired assembler skeleton for SourceTrace entry completion
 * review metadata.
 *
 * <p>This class is deliberately not a Spring service and does not wire into
 * readiness, dashboard, schema, orders, automation, or external data paths.
 */
public class SourceTraceEntryReadOnlyCompletionAssembler {

    private static final String UNWIRED_MISSING_FIELD = "readOnlyCompletionProductionPathUnwired";
    private static final String ENTRY_PRICE_SOURCE_MISSING_FIELD = "entryPriceSource";
    private static final Set<String> RUNTIME_LIKE_SOURCE_TAGS = Set.of(
            "LATEST_PRICE_ONLY",
            "RAW_KLINE_ONLY",
            "AI_TEXT",
            "DASHBOARD_TEXT",
            "EXTERNAL_DATA",
            "ORDER_DATA",
            "EXECUTION_DATA"
    );

    public SourceTraceEntryPositiveCompletionContractDTO assemble(
            SourceTraceEntryReadOnlyCompletionRequest request
    ) {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                new SourceTraceEntryPositiveCompletionContractDTO();
        if (request == null) {
            return fail(dto, SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD, "request");
        }

        List<String> missingFields = missingFields(request);
        if (!missingFields.isEmpty()) {
            return fail(
                    dto,
                    SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD,
                    missingFields
            );
        }

        List<String> unsafeFields = unsafeFields(request);
        if (!unsafeFields.isEmpty()) {
            return fail(
                    dto,
                    SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION,
                    unsafeFields
            );
        }

        populateReadOnlyMetadata(dto, request);
        dto.setCompletionStatus(SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_DESIGN_REVIEW_ONLY);
        dto.setCompletionTransition(
                SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_DESIGN_REVIEW_ONLY
        );
        dto.setDowngradeReason(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED);
        dto.setMissingFields(List.of(UNWIRED_MISSING_FIELD, ENTRY_PRICE_SOURCE_MISSING_FIELD));
        return dto;
    }

    private SourceTraceEntryPositiveCompletionContractDTO fail(
            SourceTraceEntryPositiveCompletionContractDTO dto,
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum downgradeReason,
            String missingField
    ) {
        return fail(dto, downgradeReason, List.of(missingField));
    }

    private SourceTraceEntryPositiveCompletionContractDTO fail(
            SourceTraceEntryPositiveCompletionContractDTO dto,
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum downgradeReason,
            List<String> missingFields
    ) {
        dto.setDowngradeReason(downgradeReason);
        dto.setMissingFields(missingFields);
        return dto;
    }

    private List<String> missingFields(SourceTraceEntryReadOnlyCompletionRequest request) {
        List<String> missingFields = new ArrayList<>();
        addIfBlank(missingFields, request.getSourceTraceEntryOwnershipCompletionPath(),
                "sourceTraceEntryOwnershipCompletionPath");
        addIfBlank(missingFields, request.getEntrySourceType(), "entrySourceType");
        addIfBlank(missingFields, request.getEntrySourceTimeframe(), "entrySourceTimeframe");
        addIfBlank(missingFields, request.getEntrySourceReason(), "entrySourceReason");
        addIfBlank(missingFields, request.getEntrySourceRef(), "entrySourceRef");
        addIfBlank(missingFields, request.getRuleId(), "ruleId");
        addIfBlank(missingFields, request.getRuleVersion(), "ruleVersion");
        addIfBlank(missingFields, request.getSourceWindow(), "sourceWindow");
        addIfBlank(missingFields, request.getFreshnessStatus(), "freshnessStatus");
        addIfNull(missingFields, request.getObservedAtMs(), "observedAtMs");
        addIfNull(missingFields, request.getDecisionCreateTimeMs(), "decisionCreateTimeMs");
        addIfNull(missingFields, request.getConflictsWithStop(), "conflictsWithStop");
        addIfNull(missingFields, request.getConflictsWithTakeProfit(), "conflictsWithTakeProfit");
        addIfNull(missingFields, request.getConflictsWithRiskReward(), "conflictsWithRiskReward");
        addIfNull(missingFields, request.getConflictsWithLiquidity(), "conflictsWithLiquidity");
        addIfNull(missingFields, request.getConflictsWithMultiTimeframe(), "conflictsWithMultiTimeframe");
        addIfNull(missingFields, request.getConflictsWithEvent(), "conflictsWithEvent");
        addIfNull(missingFields, request.getConflictsWithWick(), "conflictsWithWick");
        return missingFields;
    }

    private List<String> unsafeFields(SourceTraceEntryReadOnlyCompletionRequest request) {
        List<String> unsafeFields = new ArrayList<>();
        if (!"FRESH".equalsIgnoreCase(request.getFreshnessStatus())) {
            unsafeFields.add("freshnessStatus");
        }
        if (request.getObservedAtMs() > request.getDecisionCreateTimeMs()) {
            unsafeFields.add("observedAtMsFuture");
            unsafeFields.add("clockInversion");
        }
        addIfTrue(unsafeFields, request.getConflictsWithStop(), "conflictsWithStop");
        addIfTrue(unsafeFields, request.getConflictsWithTakeProfit(), "conflictsWithTakeProfit");
        addIfTrue(unsafeFields, request.getConflictsWithRiskReward(), "conflictsWithRiskReward");
        addIfTrue(unsafeFields, request.getConflictsWithLiquidity(), "conflictsWithLiquidity");
        addIfTrue(unsafeFields, request.getConflictsWithMultiTimeframe(), "conflictsWithMultiTimeframe");
        addIfTrue(unsafeFields, request.getConflictsWithEvent(), "conflictsWithEvent");
        addIfTrue(unsafeFields, request.getConflictsWithWick(), "conflictsWithWick");
        addRuntimeLikeSourceTags(unsafeFields, request.getSourceTags());
        addAmbiguousSourceRefs(unsafeFields, request.getSourceRefs());
        if (request.isLiquidityStress()) {
            unsafeFields.add("liquidityStressRequiresReview");
        }
        if (request.isLiquidityStampede()) {
            unsafeFields.add("liquidityStampedeRequiresReview");
        }
        if (request.isEventDataMissing()) {
            unsafeFields.add("eventDataMissing");
        }
        if (request.isMultiTimeframeAgreementOnly()) {
            unsafeFields.add("multiTimeframeAgreementOnly");
        }
        if (request.isWickOrPinBarEvidenceOnly()) {
            unsafeFields.add("wickOrPinBarEvidenceOnly");
        }
        return unsafeFields;
    }

    private void populateReadOnlyMetadata(
            SourceTraceEntryPositiveCompletionContractDTO dto,
            SourceTraceEntryReadOnlyCompletionRequest request
    ) {
        dto.setSymbol(request.getSymbol());
        dto.setTimeframe(request.getTimeframe());
        dto.setSourceTraceEntryOwnershipCompletionPath(request.getSourceTraceEntryOwnershipCompletionPath());
        dto.setEntrySourceType(request.getEntrySourceType());
        dto.setEntrySourceTimeframe(request.getEntrySourceTimeframe());
        dto.setEntrySourceReason(request.getEntrySourceReason());
        dto.setEntrySourceRef(request.getEntrySourceRef());
        dto.setRuleId(request.getRuleId());
        dto.setRuleVersion(request.getRuleVersion());
        dto.setSourceWindow(request.getSourceWindow());
        dto.setFreshnessStatus(request.getFreshnessStatus());
        dto.setObservedAtMs(request.getObservedAtMs());
        dto.setDecisionCreateTimeMs(request.getDecisionCreateTimeMs());
        dto.setConflictsWithStop(request.getConflictsWithStop());
        dto.setConflictsWithTakeProfit(request.getConflictsWithTakeProfit());
        dto.setConflictsWithRiskReward(request.getConflictsWithRiskReward());
        dto.setConflictsWithLiquidity(request.getConflictsWithLiquidity());
        dto.setConflictsWithMultiTimeframe(request.getConflictsWithMultiTimeframe());
        dto.setConflictsWithEvent(request.getConflictsWithEvent());
        dto.setConflictsWithWick(request.getConflictsWithWick());
    }

    private void addIfBlank(List<String> missingFields, String value, String missingField) {
        if (isBlank(value)) {
            missingFields.add(missingField);
        }
    }

    private void addIfNull(List<String> missingFields, Object value, String missingField) {
        if (value == null) {
            missingFields.add(missingField);
        }
    }

    private void addIfTrue(List<String> unsafeFields, Boolean value, String unsafeField) {
        if (Boolean.TRUE.equals(value)) {
            unsafeFields.add(unsafeField);
        }
    }

    private void addRuntimeLikeSourceTags(List<String> unsafeFields, List<String> sourceTags) {
        sourceTags.stream()
                .map(this::normalizeRuntimeLikeSourceTag)
                .filter(RUNTIME_LIKE_SOURCE_TAGS::contains)
                .forEach(unsafeFields::add);
    }

    private String normalizeRuntimeLikeSourceTag(String sourceTag) {
        if (sourceTag == null) {
            return "";
        }
        return sourceTag
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private void addAmbiguousSourceRefs(List<String> unsafeFields, List<String> sourceRefs) {
        if (sourceRefs.size() > 1) {
            unsafeFields.add("ambiguousSourceRefs");
        }
        if (new HashSet<>(sourceRefs).size() != sourceRefs.size()) {
            unsafeFields.add("duplicateSourceRefs");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
