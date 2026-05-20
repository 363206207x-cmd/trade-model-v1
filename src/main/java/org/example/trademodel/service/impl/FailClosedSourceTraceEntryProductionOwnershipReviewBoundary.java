package org.example.trademodel.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipAuditEnvelope;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipReviewRequest;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipReviewResult;
import org.example.trademodel.service.SourceTraceEntryProductionOwnershipReviewBoundary;

/**
 * Non-Spring, read-only, fail-closed skeleton for production ownership review.
 *
 * <p>This implementation is intentionally inert. It does not complete
 * SourceTrace, produce readiness, generate trade values, persist data, or wire
 * to runtime consumers.
 */
public class FailClosedSourceTraceEntryProductionOwnershipReviewBoundary
        implements SourceTraceEntryProductionOwnershipReviewBoundary {

    private static final List<String> REQUIRED_OWNER_FIELDS = List.of(
            "sourceTraceEntryOwnershipCompletionPath",
            "entryPriceSource",
            "entrySourceType",
            "entrySourceTimeframe",
            "entrySourceReason",
            "entrySourceRef",
            "sourceWindow",
            "ruleId",
            "ruleVersion",
            "freshnessOwnership",
            "conflictFamilyOwnership"
    );
    private static final List<String> RUNTIME_SUBSTITUTION_TOKENS = List.of(
            "latestprice",
            "latest-price",
            "rawkline",
            "raw-kline",
            "klineitem",
            "ai text",
            "aitext",
            "dashboard",
            "external",
            "coinglass",
            "order",
            "execution"
    );
    private static final List<String> POSITIVE_LOOKING_TOKENS = List.of(
            "valid",
            "completed",
            "complete",
            "signal",
            "buy",
            "sell",
            "open",
            "ready"
    );
    private static final List<String> RISK_ACTION_GUARD_TOKENS = List.of(
            "highrisk",
            "high-risk",
            "wick",
            "pinbar",
            "pin-bar",
            "liquiditystress",
            "liquidity-stress",
            "stampede",
            "missingevent",
            "missing-event",
            "multitimeframe",
            "multi-timeframe"
    );
    private static final List<String> CONSUMER_BLOCKERS = List.of(
            "boundaryCandidateServiceValidIsolation",
            "executionPlanReadinessIsolation",
            "dashboardMutationIsolation",
            "schemaPersistenceIsolation",
            "resolverIsolation",
            "validatorReadinessIsolation",
            "orderPathIsolation",
            "executionPathIsolation",
            "automationPathIsolation",
            "externalDataPathIsolation"
    );

    @Override
    public SourceTraceEntryProductionOwnershipReviewResult reviewEntryOwnership(
            SourceTraceEntryProductionOwnershipReviewRequest request
    ) {
        SourceTraceEntryProductionOwnershipReviewResult result =
                new SourceTraceEntryProductionOwnershipReviewResult();
        result.setDowngradeReason(
                SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum.REVIEW_BOUNDARY_UNWIRED);

        LinkedHashSet<String> missingFields = new LinkedHashSet<>();
        LinkedHashSet<String> unsafeFields = new LinkedHashSet<>();
        LinkedHashSet<String> blockingFields = new LinkedHashSet<>(List.of(
                "productionOwnershipReviewBoundaryUnwired",
                "productionWiringStillBlocked",
                "failClosedImplementationSkeleton"
        ));

        if (request == null) {
            missingFields.add("request");
            blockingFields.add("nullRequest");
            apply(result, missingFields, unsafeFields, blockingFields);
            return result;
        }

        result.setSymbol(request.getSymbol());
        result.setTimeframe(request.getTimeframe());

        addMissingOwnerFields(request, missingFields, blockingFields);
        addOwnerEvidenceGuards(request, unsafeFields, blockingFields);
        addRuntimeSubstitutionGuards(request, unsafeFields, blockingFields);
        addAuditGuards(request.getAuditEnvelope(), missingFields, blockingFields);
        addVisibilityGuards(request, missingFields, blockingFields);
        addConsumerIsolationGuards(request.getConsumerIsolationEnvelope(), missingFields, blockingFields);
        addRiskActionGuardBlockers(request, unsafeFields, blockingFields);
        addPositiveLookingLabelBlockers(request, unsafeFields, blockingFields);

        if (containsToken(request.getAuditEnvelope().getDowngradeReason(), "downgrade")) {
            unsafeFields.add("downgradeRequired");
            blockingFields.add("downgradeRequired");
        }
        if (containsToken(request.getAuditEnvelope().getRollbackReason(), "rollback")) {
            unsafeFields.add("rollbackRequired");
            blockingFields.add("rollbackRequired");
        }

        if (missingFields.isEmpty()) {
            missingFields.add("productionOwnershipReviewBoundaryUnwired");
        }
        apply(result, missingFields, unsafeFields, blockingFields);
        return result;
    }

    private void addMissingOwnerFields(
            SourceTraceEntryProductionOwnershipReviewRequest request,
            Set<String> missingFields,
            Set<String> blockingFields
    ) {
        addIfBlank(request.getSourceTraceEntryOwnershipCompletionPath(),
                "sourceTraceEntryOwnershipCompletionPath", missingFields);
        addIfBlank(request.getEntryPriceSource(), "entryPriceSource", missingFields);
        addIfBlank(request.getEntrySourceType(), "entrySourceType", missingFields);
        addIfBlank(request.getEntrySourceTimeframe(), "entrySourceTimeframe", missingFields);
        addIfBlank(request.getEntrySourceReason(), "entrySourceReason", missingFields);
        addIfBlank(request.getEntrySourceRef(), "entrySourceRef", missingFields);
        addIfBlank(request.getSourceWindow(), "sourceWindow", missingFields);
        addIfBlank(request.getRuleId(), "ruleId", missingFields);
        addIfBlank(request.getRuleVersion(), "ruleVersion", missingFields);
        addIfBlank(request.getFreshnessOwnership(), "freshnessOwnership", missingFields);
        addIfBlank(request.getConflictFamilyOwnership(), "conflictFamilyOwnership", missingFields);
        if (!missingFields.stream().filter(REQUIRED_OWNER_FIELDS::contains).toList().isEmpty()
                || request.getOwnerEvidenceFields().isEmpty()) {
            blockingFields.add("ownerEvidenceMissing");
        }
    }

    private void addOwnerEvidenceGuards(
            SourceTraceEntryProductionOwnershipReviewRequest request,
            Set<String> unsafeFields,
            Set<String> blockingFields
    ) {
        if (hasDuplicate(request.getOwnerEvidenceFields()) || hasDuplicate(request.getSourceRefs())) {
            unsafeFields.add("duplicateOwnerEvidence");
            blockingFields.add("duplicateOwnerEvidence");
        }
        if (request.getSourceRefs().size() > 1 || containsToken(request.getEntrySourceRef(), "ambiguous")) {
            unsafeFields.add("ambiguousOwnerEvidence");
            blockingFields.add("ambiguousOwnerEvidence");
        }
        if (containsAnyToken(allRequestText(request), List.of("stale"))) {
            unsafeFields.add("staleOwnerEvidence");
            blockingFields.add("staleOwnerEvidence");
        }
    }

    private void addRuntimeSubstitutionGuards(
            SourceTraceEntryProductionOwnershipReviewRequest request,
            Set<String> unsafeFields,
            Set<String> blockingFields
    ) {
        if (containsAnyToken(allRequestText(request), RUNTIME_SUBSTITUTION_TOKENS)) {
            unsafeFields.add("runtimeLikeSubstitution");
            blockingFields.add("runtimeLikeSubstitution");
        }
    }

    private void addAuditGuards(
            SourceTraceEntryProductionOwnershipAuditEnvelope auditEnvelope,
            Set<String> missingFields,
            Set<String> blockingFields
    ) {
        if (auditEnvelope == null || !auditEnvelope.isAuditEvidencePresent()
                || !auditEnvelope.getMissingAuditFields().isEmpty()) {
            missingFields.add("auditEnvelope");
            blockingFields.add("auditMetadataMissing");
        }
    }

    private void addVisibilityGuards(
            SourceTraceEntryProductionOwnershipReviewRequest request,
            Set<String> missingFields,
            Set<String> blockingFields
    ) {
        if (isBlank(request.getAuthenticationVisibility())) {
            missingFields.add("authenticationVisibility");
            blockingFields.add("authenticationVisibilityMissing");
            blockingFields.add("payloadWithheldForReview");
        }
    }

    private void addConsumerIsolationGuards(
            SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope isolationEnvelope,
            Set<String> missingFields,
            Set<String> blockingFields
    ) {
        if (isolationEnvelope == null || !isolationEnvelope.isIsolationEvidencePresent()
                || !isolationEnvelope.getMissingIsolationFields().isEmpty()) {
            missingFields.add("consumerIsolationEnvelope");
            blockingFields.add("consumerIsolationMissing");
            blockingFields.addAll(CONSUMER_BLOCKERS);
        }
    }

    private void addRiskActionGuardBlockers(
            SourceTraceEntryProductionOwnershipReviewRequest request,
            Set<String> unsafeFields,
            Set<String> blockingFields
    ) {
        if (containsAnyToken(allRequestText(request), RISK_ACTION_GUARD_TOKENS)) {
            unsafeFields.add("riskActionGuardReviewRequired");
            blockingFields.add("riskActionGuardReviewRequired");
        }
    }

    private void addPositiveLookingLabelBlockers(
            SourceTraceEntryProductionOwnershipReviewRequest request,
            Set<String> unsafeFields,
            Set<String> blockingFields
    ) {
        if (containsAnyToken(allRequestText(request), POSITIVE_LOOKING_TOKENS)) {
            unsafeFields.add("positiveLookingLabel");
            blockingFields.add("positiveLookingLabel");
        }
    }

    private void apply(
            SourceTraceEntryProductionOwnershipReviewResult result,
            LinkedHashSet<String> missingFields,
            LinkedHashSet<String> unsafeFields,
            LinkedHashSet<String> blockingFields
    ) {
        result.setMissingFields(new ArrayList<>(missingFields));
        result.setUnsafeFields(new ArrayList<>(unsafeFields));
        result.setBlockingFields(new ArrayList<>(blockingFields));
    }

    private void addIfBlank(String value, String fieldName, Set<String> missingFields) {
        if (isBlank(value)) {
            missingFields.add(fieldName);
        }
    }

    private boolean hasDuplicate(List<String> values) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String value : values) {
            if (isBlank(value)) {
                continue;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (!seen.add(normalized)) {
                return true;
            }
        }
        return false;
    }

    private List<String> allRequestText(SourceTraceEntryProductionOwnershipReviewRequest request) {
        List<String> values = new ArrayList<>();
        values.add(request.getSourceTraceEntryOwnershipCompletionPath());
        values.add(request.getEntryPriceSource());
        values.add(request.getEntrySourceType());
        values.add(request.getEntrySourceTimeframe());
        values.add(request.getEntrySourceReason());
        values.add(request.getEntrySourceRef());
        values.add(request.getSourceWindow());
        values.add(request.getRuleId());
        values.add(request.getRuleVersion());
        values.add(request.getFreshnessOwnership());
        values.add(request.getConflictFamilyOwnership());
        values.add(request.getAuthenticationVisibility());
        values.addAll(request.getOwnerEvidenceFields());
        values.addAll(request.getSourceRefs());
        SourceTraceEntryProductionOwnershipAuditEnvelope auditEnvelope = request.getAuditEnvelope();
        if (auditEnvelope != null) {
            values.add(auditEnvelope.getFreshnessStatus());
            values.add(auditEnvelope.getDowngradeReason());
            values.add(auditEnvelope.getRollbackReason());
            values.add(auditEnvelope.getVisibilityState());
            values.add(auditEnvelope.getConflictFamilyEvidenceStatus());
        }
        return values.stream().filter(Objects::nonNull).toList();
    }

    private boolean containsAnyToken(List<String> values, List<String> tokens) {
        return values.stream().anyMatch(value -> tokens.stream().anyMatch(token -> containsToken(value, token)));
    }

    private boolean containsToken(String value, String token) {
        if (value == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
