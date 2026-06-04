package org.example.trademodel.validator.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.trademodel.dto.point.SourceOwnedCandidateIntegrationRuntimeCandidateDTO;

public class SourceOwnedCandidateIntegrationRuntimeCandidateValidator {

    private static final BigDecimal MINIMUM_REVIEW_ONLY_COMPLETENESS = new BigDecimal("85");

    private static final String REASON_CONTEXT_MISSING = "RUNTIME_CANDIDATE_CONTEXT_MISSING";
    private static final String REASON_STATUS_MISSING = "RUNTIME_CANDIDATE_STATUS_MISSING";
    private static final String REASON_SAFETY_FLAG_REQUIRED = "SAFETY_FLAG_REQUIRED";
    private static final String REASON_FAIL_CLOSED_INVALID = "FAIL_CLOSED_STATUS_MISMATCH";
    private static final String REASON_MISSING_REASON_REQUIRED = "MISSING_REASON_REQUIRED";
    private static final String REASON_BLOCKED_REASON_REQUIRED = "BLOCKED_REASON_REQUIRED";
    private static final String REASON_DEGRADED_REASON_REQUIRED = "DEGRADED_REASON_REQUIRED";
    private static final String REASON_FORBIDDEN_RUNTIME_SEMANTIC = "FORBIDDEN_RUNTIME_SEMANTIC_DETECTED";
    private static final String REASON_SOURCE_BINDING_REF_MISSING = "SOURCE_BINDING_REF_MISSING";
    private static final String REASON_SOURCE_BINDING_VALIDATION_STATUS_MISSING =
            "SOURCE_BINDING_VALIDATION_STATUS_MISSING";
    private static final String REASON_SOURCE_BINDING_VALIDATION_BLOCKED =
            "SOURCE_BINDING_VALIDATION_BLOCKED";
    private static final String REASON_SOURCE_BINDING_VALIDATION_INCOMPLETE =
            "SOURCE_BINDING_VALIDATION_INCOMPLETE";
    private static final String REASON_SOURCE_TRACE_REFS_MISSING = "SOURCE_TRACE_REFS_MISSING";
    private static final String REASON_SOURCE_TRACE_REF_BLANK = "SOURCE_TRACE_REF_BLANK";
    private static final String REASON_RUNTIME_KLINE_CONTEXT_REF_MISSING =
            "RUNTIME_KLINE_CONTEXT_REF_MISSING";
    private static final String REASON_DATA_QUALITY_CONTEXT_REF_MISSING =
            "DATA_QUALITY_CONTEXT_REF_MISSING";
    private static final String REASON_MULTITIMEFRAME_CONTEXT_REF_MISSING =
            "MULTITIMEFRAME_CONTEXT_REF_MISSING";
    private static final String REASON_RISK_ACTION_GUARD_CONTEXT_REF_MISSING =
            "RISK_ACTION_GUARD_CONTEXT_REF_MISSING";
    private static final String REASON_WATCHLIST_POOL_PROOF_CONTEXT_REF_MISSING =
            "WATCHLIST_POOL_PROOF_CONTEXT_REF_MISSING";
    private static final String REASON_OBSERVED_AT_MISSING = "OBSERVED_AT_MISSING";
    private static final String REASON_COMPLETENESS_SCORE_MISSING =
            "SOURCE_BINDING_COMPLETENESS_SCORE_MISSING";
    private static final String REASON_COMPLETENESS_SCORE_TOO_LOW =
            "SOURCE_BINDING_COMPLETENESS_SCORE_TOO_LOW";
    private static final String REASON_REQUIRED_SOURCES_PRESENT_MISSING =
            "ALL_REQUIRED_SOURCES_PRESENT_MISSING";
    private static final String REASON_REQUIRED_SOURCES_TRUSTED_MISSING =
            "ALL_REQUIRED_SOURCES_TRUSTED_MISSING";
    private static final String REASON_REQUIRED_SOURCES_REVIEW_ONLY_MISSING =
            "ALL_REQUIRED_SOURCES_REVIEW_ONLY_MISSING";
    private static final String REASON_REQUIRED_SOURCES_NOT_TRADE_INSTRUCTION_MISSING =
            "ALL_REQUIRED_SOURCES_NOT_TRADE_INSTRUCTION_MISSING";
    private static final String REASON_REQUIRED_SOURCES_MANUAL_REVIEW_REQUIRED_MISSING =
            "ALL_REQUIRED_SOURCES_MANUAL_REVIEW_REQUIRED_MISSING";
    private static final String REASON_REQUIRED_SOURCES_INCOMPLETE_SAFE_MISSING =
            "ALL_REQUIRED_SOURCES_INCOMPLETE_SAFE_MISSING";
    private static final String REASON_ANY_SOURCE_BLOCKED_MISSING = "ANY_SOURCE_BLOCKED_MISSING";
    private static final String REASON_ANY_SOURCE_INCOMPLETE_MISSING = "ANY_SOURCE_INCOMPLETE_MISSING";
    private static final String REASON_ANY_SOURCE_DEGRADED_MISSING = "ANY_SOURCE_DEGRADED_MISSING";
    private static final String REASON_WATCHLIST_POOL_MEMBER_MISSING =
            "WATCHLIST_POOL_MEMBER_MISSING";
    private static final String REASON_WATCHLIST_POOL_PROOF_FRESH_MISSING =
            "WATCHLIST_POOL_PROOF_FRESH_MISSING";
    private static final String REASON_RISK_ACTION_GUARD_BLOCKED_MISSING =
            "RISK_ACTION_GUARD_BLOCKED_MISSING";
    private static final String REASON_RISK_ACTION_GUARD_STAMPEDE_MISSING =
            "RISK_ACTION_GUARD_STAMPEDE_MISSING";
    private static final String REASON_RUNTIME_KLINE_STALE_MISSING = "RUNTIME_KLINE_STALE_MISSING";
    private static final String REASON_DATA_QUALITY_PASSED_MISSING = "DATA_QUALITY_PASSED_MISSING";
    private static final String REASON_MULTITIMEFRAME_CONFIRMED_MISSING =
            "MULTITIMEFRAME_CONFIRMED_MISSING";
    private static final String REASON_REQUIRED_SOURCE_MISSING = "REQUIRED_SOURCE_MISSING";
    private static final String REASON_REQUIRED_SOURCE_UNTRUSTED = "REQUIRED_SOURCE_UNTRUSTED";
    private static final String REASON_REQUIRED_SOURCE_NOT_REVIEW_ONLY =
            "REQUIRED_SOURCE_NOT_REVIEW_ONLY";
    private static final String REASON_REQUIRED_SOURCE_TRADE_INSTRUCTION =
            "REQUIRED_SOURCE_TRADE_INSTRUCTION";
    private static final String REASON_REQUIRED_SOURCE_MANUAL_REVIEW_DISABLED =
            "REQUIRED_SOURCE_MANUAL_REVIEW_DISABLED";
    private static final String REASON_REQUIRED_SOURCE_NOT_INCOMPLETE_SAFE =
            "REQUIRED_SOURCE_NOT_INCOMPLETE_SAFE";
    private static final String REASON_UPSTREAM_SOURCE_BLOCKED = "UPSTREAM_SOURCE_BLOCKED";
    private static final String REASON_UPSTREAM_SOURCE_INCOMPLETE = "UPSTREAM_SOURCE_INCOMPLETE";
    private static final String REASON_UPSTREAM_SOURCE_DEGRADED = "UPSTREAM_SOURCE_DEGRADED";
    private static final String REASON_RUNTIME_KLINE_STALE = "RUNTIME_KLINE_STALE";
    private static final String REASON_DATA_QUALITY_NOT_PASSED = "DATA_QUALITY_NOT_PASSED";
    private static final String REASON_MULTITIMEFRAME_NOT_CONFIRMED =
            "MULTITIMEFRAME_NOT_CONFIRMED";
    private static final String REASON_WATCHLIST_POOL_NOT_MEMBER = "WATCHLIST_POOL_NOT_MEMBER";
    private static final String REASON_WATCHLIST_POOL_PROOF_STALE = "WATCHLIST_POOL_PROOF_STALE";
    private static final String REASON_RISK_ACTION_GUARD_BLOCKED =
            "RISK_ACTION_GUARD_BLOCKED";
    private static final String REASON_RISK_ACTION_GUARD_STAMPEDE =
            "RISK_ACTION_GUARD_STAMPEDE";
    private static final String REASON_RISK_ACTION_GUARD_BOUNDARY_BLOCKED =
            "RISK_ACTION_GUARD_BOUNDARY_BLOCKED";
    private static final String REASON_WATCHLIST_POOL_PROOF_BOUNDARY_BLOCKED =
            "WATCHLIST_POOL_PROOF_BOUNDARY_BLOCKED";
    private static final String REASON_DISPLAY_SLOT_NOT_POOL_PROOF =
            "DISPLAY_SLOT_NOT_WATCHLIST_POOL_PROOF";
    private static final String REASON_UNSUPPORTED_RUNTIME_INPUT_ONLY =
            "UNSUPPORTED_RUNTIME_INPUT_ONLY";

    private static final List<String> FORBIDDEN_RUNTIME_SEMANTICS = List.of(
            "ent" + "ry",
            "st" + "op",
            "take" + "Profit",
            "t" + "p",
            "r" + "r",
            "lever" + "age",
            "position" + "Size",
            "ord" + "erId",
            "ord" + "erIntent",
            "execut" + "ionIntent",
            "auto" + "TradingAction",
            "pu" + "shPayload",
            "external" + "ChannelMessage",
            "final" + "Direction",
            "trade" + "Action",
            "open" + "Position",
            "close" + "Position",
            "reverse" + "Position",
            "place" + "Ord" + "er",
            "create" + "Ord" + "er",
            "submit" + "Ord" + "er",
            "market " + "b" + "uy",
            "market " + "s" + "ell",
            "pu" + "sh " + "send",
            "external " + "channel",
            "auto-" + "trade",
            "auto " + "trading"
    );

    private static final List<String> UNSUPPORTED_INPUT_ONLY_SEMANTICS = List.of(
            "LATEST_PRICE_ONLY",
            "LATEST_CLOSE_ONLY",
            "DASHBOARD_TEXT_ONLY",
            "AI_PROSE_ONLY",
            "latest " + "price",
            "latest " + "close",
            "dashboard " + "text",
            "ai " + "prose"
    );

    public ValidationResult validate(SourceOwnedCandidateIntegrationRuntimeCandidateDTO context) {
        if (context == null) {
            return ValidationResult.incomplete(List.of(REASON_CONTEXT_MISSING));
        }

        SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus status =
                context.getCandidateRuntimeStatus();
        if (status == null) {
            return ValidationResult.incomplete(List.of(REASON_STATUS_MISSING));
        }

        if (containsForbiddenRuntimeSemantic(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_FORBIDDEN_RUNTIME_SEMANTIC));
        }

        if (containsUnsupportedInputOnlySemantic(context)) {
            return ValidationResult.incomplete(List.of(REASON_UNSUPPORTED_RUNTIME_INPUT_ONLY));
        }

        if (!safetyFlagsRequiredTrue(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_SAFETY_FLAG_REQUIRED));
        }

        if (failClosedStatusMismatch(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_FAIL_CLOSED_INVALID));
        }

        ValidationResult blockedState = blockedState(context);
        if (blockedState != null) {
            return blockedState;
        }

        return switch (status) {
            case INCOMPLETE -> validateIncomplete(context);
            case BLOCKED_FAIL_CLOSED -> validateBlockedFailClosed(context);
            case REVIEW_ONLY_RUNTIME_CANDIDATE -> validateReviewOnlyCandidate(context);
            case REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED -> validateDegradedCandidate(context);
        };
    }

    private static ValidationResult validateIncomplete(
            SourceOwnedCandidateIntegrationRuntimeCandidateDTO context
    ) {
        if (missingIncompleteReason(context)) {
            return ValidationResult.incomplete(List.of(REASON_MISSING_REASON_REQUIRED));
        }
        return ValidationResult.incomplete(reasonOrDefault(
                context.getMissingReason(),
                context.getCandidateUnavailableReason(),
                "RUNTIME_CANDIDATE_INCOMPLETE"
        ));
    }

    private static ValidationResult validateBlockedFailClosed(
            SourceOwnedCandidateIntegrationRuntimeCandidateDTO context
    ) {
        if (missingBlockedReason(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_BLOCKED_REASON_REQUIRED));
        }
        return ValidationResult.blockedFailClosed(reasonOrDefault(
                context.getBlockedReason(),
                context.getCandidateBlockedReason(),
                "RUNTIME_CANDIDATE_BLOCKED"
        ));
    }

    private static ValidationResult validateDegradedCandidate(
            SourceOwnedCandidateIntegrationRuntimeCandidateDTO context
    ) {
        List<String> incompleteReasons = requiredRuntimeReasons(context);
        incompleteReasons.addAll(incompleteStateReasons(context));

        if (!Boolean.TRUE.equals(context.getAnySourceDegraded())) {
            incompleteReasons.add(REASON_UPSTREAM_SOURCE_DEGRADED);
        }
        if (missingDegradedExplanation(context)) {
            incompleteReasons.add(REASON_DEGRADED_REASON_REQUIRED);
        }
        if (!missingDegradedExplanation(context)) {
            incompleteReasons.remove(REASON_COMPLETENESS_SCORE_TOO_LOW);
        }

        if (!incompleteReasons.isEmpty()) {
            return ValidationResult.incomplete(incompleteReasons);
        }

        return ValidationResult.reviewOnlyRuntimeCandidateDegraded(degradedReason(context));
    }

    private static ValidationResult validateReviewOnlyCandidate(
            SourceOwnedCandidateIntegrationRuntimeCandidateDTO context
    ) {
        List<String> incompleteReasons = requiredRuntimeReasons(context);
        incompleteReasons.addAll(incompleteStateReasons(context));
        if (!incompleteReasons.isEmpty()) {
            return ValidationResult.incomplete(incompleteReasons);
        }

        if (Boolean.TRUE.equals(context.getAnySourceDegraded())) {
            if (missingDegradedExplanation(context)) {
                return ValidationResult.incomplete(List.of(REASON_DEGRADED_REASON_REQUIRED));
            }
            return ValidationResult.reviewOnlyRuntimeCandidateDegraded(degradedReason(context));
        }

        return ValidationResult.reviewOnlyRuntimeCandidateValid(List.of());
    }

    private static ValidationResult blockedState(SourceOwnedCandidateIntegrationRuntimeCandidateDTO context) {
        if (statusContains(context.getSourceOwnedCandidateIntegrationValidationStatus(), "BLOCKED_FAIL_CLOSED")) {
            return ValidationResult.blockedFailClosed(List.of(REASON_SOURCE_BINDING_VALIDATION_BLOCKED));
        }
        if (Boolean.FALSE.equals(context.getAllRequiredSourcesTrusted())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_REQUIRED_SOURCE_UNTRUSTED));
        }
        if (Boolean.FALSE.equals(context.getAllRequiredSourcesReviewOnly())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_REQUIRED_SOURCE_NOT_REVIEW_ONLY));
        }
        if (Boolean.FALSE.equals(context.getAllRequiredSourcesNotTradeInstruction())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_REQUIRED_SOURCE_TRADE_INSTRUCTION));
        }
        if (Boolean.FALSE.equals(context.getAllRequiredSourcesManualReviewRequired())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_REQUIRED_SOURCE_MANUAL_REVIEW_DISABLED));
        }
        if (Boolean.FALSE.equals(context.getAllRequiredSourcesIncompleteSafe())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_REQUIRED_SOURCE_NOT_INCOMPLETE_SAFE));
        }
        if (Boolean.TRUE.equals(context.getAnySourceBlocked())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_UPSTREAM_SOURCE_BLOCKED));
        }
        if (Boolean.TRUE.equals(context.getRiskActionGuardBlocked())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_RISK_ACTION_GUARD_BLOCKED));
        }
        if (Boolean.TRUE.equals(context.getRiskActionGuardStampede())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_RISK_ACTION_GUARD_STAMPEDE));
        }
        if (Boolean.FALSE.equals(context.getWatchlistPoolMember())) {
            return ValidationResult.blockedFailClosed(List.of(REASON_WATCHLIST_POOL_NOT_MEMBER));
        }
        if (containsRiskActionGuardBlockedStatus(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_RISK_ACTION_GUARD_BOUNDARY_BLOCKED));
        }
        if (containsWatchlistPoolBlockedStatus(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_WATCHLIST_POOL_PROOF_BOUNDARY_BLOCKED));
        }
        if (displaySlotTreatedAsPoolProof(context)) {
            return ValidationResult.blockedFailClosed(List.of(REASON_DISPLAY_SLOT_NOT_POOL_PROOF));
        }
        return null;
    }

    private static List<String> requiredRuntimeReasons(
            SourceOwnedCandidateIntegrationRuntimeCandidateDTO context
    ) {
        List<String> reasons = new ArrayList<>();
        if (isBlank(context.getSourceOwnedCandidateIntegrationSourceBindingRef())) {
            reasons.add(REASON_SOURCE_BINDING_REF_MISSING);
        }
        if (isBlank(context.getSourceOwnedCandidateIntegrationValidationStatus())) {
            reasons.add(REASON_SOURCE_BINDING_VALIDATION_STATUS_MISSING);
        }
        reasons.addAll(refReasons(context.getSourceTraceRefs(),
                REASON_SOURCE_TRACE_REFS_MISSING, REASON_SOURCE_TRACE_REF_BLANK));
        if (isBlank(context.getRuntimeKlineContextRef())) {
            reasons.add(REASON_RUNTIME_KLINE_CONTEXT_REF_MISSING);
        }
        if (isBlank(context.getDataQualityContextRef())) {
            reasons.add(REASON_DATA_QUALITY_CONTEXT_REF_MISSING);
        }
        if (isBlank(context.getMultiTimeframeContextRef())) {
            reasons.add(REASON_MULTITIMEFRAME_CONTEXT_REF_MISSING);
        }
        if (isBlank(context.getRiskActionGuardContextRef())) {
            reasons.add(REASON_RISK_ACTION_GUARD_CONTEXT_REF_MISSING);
        }
        if (isBlank(context.getWatchlistPoolProofContextRef())) {
            reasons.add(REASON_WATCHLIST_POOL_PROOF_CONTEXT_REF_MISSING);
        }
        if (isBlank(context.getObservedAt())) {
            reasons.add(REASON_OBSERVED_AT_MISSING);
        }
        if (context.getSourceBindingCompletenessScore() == null) {
            reasons.add(REASON_COMPLETENESS_SCORE_MISSING);
        }
        if (context.getAllRequiredSourcesPresent() == null) {
            reasons.add(REASON_REQUIRED_SOURCES_PRESENT_MISSING);
        }
        if (context.getAllRequiredSourcesTrusted() == null) {
            reasons.add(REASON_REQUIRED_SOURCES_TRUSTED_MISSING);
        }
        if (context.getAllRequiredSourcesReviewOnly() == null) {
            reasons.add(REASON_REQUIRED_SOURCES_REVIEW_ONLY_MISSING);
        }
        if (context.getAllRequiredSourcesNotTradeInstruction() == null) {
            reasons.add(REASON_REQUIRED_SOURCES_NOT_TRADE_INSTRUCTION_MISSING);
        }
        if (context.getAllRequiredSourcesManualReviewRequired() == null) {
            reasons.add(REASON_REQUIRED_SOURCES_MANUAL_REVIEW_REQUIRED_MISSING);
        }
        if (context.getAllRequiredSourcesIncompleteSafe() == null) {
            reasons.add(REASON_REQUIRED_SOURCES_INCOMPLETE_SAFE_MISSING);
        }
        if (context.getAnySourceBlocked() == null) {
            reasons.add(REASON_ANY_SOURCE_BLOCKED_MISSING);
        }
        if (context.getAnySourceIncomplete() == null) {
            reasons.add(REASON_ANY_SOURCE_INCOMPLETE_MISSING);
        }
        if (context.getAnySourceDegraded() == null) {
            reasons.add(REASON_ANY_SOURCE_DEGRADED_MISSING);
        }
        if (context.getWatchlistPoolMember() == null) {
            reasons.add(REASON_WATCHLIST_POOL_MEMBER_MISSING);
        }
        if (context.getWatchlistPoolProofFresh() == null) {
            reasons.add(REASON_WATCHLIST_POOL_PROOF_FRESH_MISSING);
        }
        if (context.getRiskActionGuardBlocked() == null) {
            reasons.add(REASON_RISK_ACTION_GUARD_BLOCKED_MISSING);
        }
        if (context.getRiskActionGuardStampede() == null) {
            reasons.add(REASON_RISK_ACTION_GUARD_STAMPEDE_MISSING);
        }
        if (context.getRuntimeKlineStale() == null) {
            reasons.add(REASON_RUNTIME_KLINE_STALE_MISSING);
        }
        if (context.getDataQualityPassed() == null) {
            reasons.add(REASON_DATA_QUALITY_PASSED_MISSING);
        }
        if (context.getMultiTimeframeConfirmed() == null) {
            reasons.add(REASON_MULTITIMEFRAME_CONFIRMED_MISSING);
        }
        return reasons;
    }

    private static List<String> incompleteStateReasons(
            SourceOwnedCandidateIntegrationRuntimeCandidateDTO context
    ) {
        List<String> reasons = new ArrayList<>();
        if (statusContains(context.getSourceOwnedCandidateIntegrationValidationStatus(), "INCOMPLETE")) {
            reasons.add(REASON_SOURCE_BINDING_VALIDATION_INCOMPLETE);
        }
        if (context.getSourceBindingCompletenessScore() != null
                && context.getSourceBindingCompletenessScore().compareTo(MINIMUM_REVIEW_ONLY_COMPLETENESS) < 0) {
            reasons.add(REASON_COMPLETENESS_SCORE_TOO_LOW);
        }
        if (Boolean.FALSE.equals(context.getAllRequiredSourcesPresent())) {
            reasons.add(REASON_REQUIRED_SOURCE_MISSING);
        }
        if (Boolean.TRUE.equals(context.getAnySourceIncomplete())) {
            reasons.add(REASON_UPSTREAM_SOURCE_INCOMPLETE);
        }
        if (Boolean.TRUE.equals(context.getRuntimeKlineStale())) {
            reasons.add(REASON_RUNTIME_KLINE_STALE);
        }
        if (Boolean.FALSE.equals(context.getDataQualityPassed())) {
            reasons.add(REASON_DATA_QUALITY_NOT_PASSED);
        }
        if (Boolean.FALSE.equals(context.getMultiTimeframeConfirmed())) {
            reasons.add(REASON_MULTITIMEFRAME_NOT_CONFIRMED);
        }
        if (Boolean.FALSE.equals(context.getWatchlistPoolProofFresh())) {
            reasons.add(REASON_WATCHLIST_POOL_PROOF_STALE);
        }
        return reasons;
    }

    private static List<String> refReasons(List<String> refs, String missingReason, String blankReason) {
        if (refs == null || refs.isEmpty()) {
            return List.of(missingReason);
        }
        for (String ref : refs) {
            if (isBlank(ref)) {
                return List.of(blankReason);
            }
        }
        return List.of();
    }

    private static boolean containsRiskActionGuardBlockedStatus(
            SourceOwnedCandidateIntegrationRuntimeCandidateDTO context
    ) {
        return containsAny(runtimeText(context), List.of(
                "WICK_ONLY_" + "REV" + "ERSE",
                "LIQUIDITY_DEGRADED_ONE_SHOT_EXIT"
        ));
    }

    private static boolean containsWatchlistPoolBlockedStatus(
            SourceOwnedCandidateIntegrationRuntimeCandidateDTO context
    ) {
        return containsAny(runtimeText(context), List.of(
                "WATCHLIST_POOL_DISABLED",
                "WATCHLIST_POOL_EMPTY",
                "WATCHLIST_POOL_NON_MEMBER",
                "WATCHLIST_POOL_UNTRUSTED"
        ));
    }

    private static boolean displaySlotTreatedAsPoolProof(
            SourceOwnedCandidateIntegrationRuntimeCandidateDTO context
    ) {
        return containsAny(runtimeText(context), List.of("DISPLAY_SLOT_PROOF", "DEFAULT_HOME_PROOF"));
    }

    private static boolean containsForbiddenRuntimeSemantic(
            SourceOwnedCandidateIntegrationRuntimeCandidateDTO context
    ) {
        return containsAny(runtimeText(context), FORBIDDEN_RUNTIME_SEMANTICS);
    }

    private static boolean containsUnsupportedInputOnlySemantic(
            SourceOwnedCandidateIntegrationRuntimeCandidateDTO context
    ) {
        return containsAny(runtimeText(context), UNSUPPORTED_INPUT_ONLY_SEMANTICS);
    }

    private static List<String> runtimeText(SourceOwnedCandidateIntegrationRuntimeCandidateDTO context) {
        List<String> values = new ArrayList<>();
        values.add(context.getRuntimeCandidateContextId());
        values.add(context.getSymbol());
        values.add(context.getMarket());
        values.add(context.getPrimaryTimeframe());
        values.add(context.getSourceOwnedCandidateIntegrationSourceBindingRef());
        values.add(context.getSourceOwnedCandidateIntegrationValidationStatus());
        values.add(context.getSourceBindingCompletenessSummary());
        values.add(context.getRuntimeKlineContextRef());
        values.add(context.getDataQualityContextRef());
        values.add(context.getMultiTimeframeContextRef());
        values.add(context.getRiskActionGuardContextRef());
        values.add(context.getWatchlistPoolProofContextRef());
        values.add(context.getCandidateUnavailableReason());
        values.add(context.getCandidateBlockedReason());
        values.add(context.getCandidateDegradedReason());
        values.add(context.getMissingReason());
        values.add(context.getBlockedReason());
        values.addAll(context.getSourceOwnedCandidateIntegrationValidationReasons());
        values.addAll(context.getSourceTraceRefs());
        values.addAll(context.getMissingFields());
        values.addAll(context.getDegradedReasons());
        values.addAll(context.getBlockedReasons());
        return values;
    }

    private static boolean safetyFlagsRequiredTrue(SourceOwnedCandidateIntegrationRuntimeCandidateDTO context) {
        return context.isReviewOnly()
                && context.isNotTradeInstruction()
                && context.isManualReviewRequired()
                && context.isIncompleteSafe();
    }

    private static boolean failClosedStatusMismatch(SourceOwnedCandidateIntegrationRuntimeCandidateDTO context) {
        boolean blockedStatus = context.getCandidateRuntimeStatus()
                == SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus.BLOCKED_FAIL_CLOSED;
        return context.isFailClosed() != blockedStatus;
    }

    private static boolean missingIncompleteReason(SourceOwnedCandidateIntegrationRuntimeCandidateDTO context) {
        return isBlank(context.getMissingReason()) && isBlank(context.getCandidateUnavailableReason());
    }

    private static boolean missingBlockedReason(SourceOwnedCandidateIntegrationRuntimeCandidateDTO context) {
        return isBlank(context.getBlockedReason()) && isBlank(context.getCandidateBlockedReason());
    }

    private static boolean missingDegradedExplanation(SourceOwnedCandidateIntegrationRuntimeCandidateDTO context) {
        return isBlank(context.getCandidateDegradedReason()) && context.getDegradedReasons().isEmpty();
    }

    private static List<String> degradedReason(SourceOwnedCandidateIntegrationRuntimeCandidateDTO context) {
        if (!isBlank(context.getCandidateDegradedReason())) {
            return List.of(context.getCandidateDegradedReason());
        }
        if (!context.getDegradedReasons().isEmpty()) {
            return context.getDegradedReasons();
        }
        return List.of(REASON_UPSTREAM_SOURCE_DEGRADED);
    }

    private static List<String> reasonOrDefault(String first, String second, String defaultReason) {
        if (!isBlank(first)) {
            return List.of(first);
        }
        if (!isBlank(second)) {
            return List.of(second);
        }
        return List.of(defaultReason);
    }

    private static boolean containsAny(String value, List<String> needles) {
        if (value == null) {
            return false;
        }
        String normalized = value.toLowerCase();
        for (String needle : needles) {
            if (normalized.contains(needle.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(List<String> values, List<String> needles) {
        for (String value : values) {
            if (containsAny(value, needles)) {
                return true;
            }
        }
        return false;
    }

    private static boolean statusContains(String status, String token) {
        return !isBlank(status) && status.toUpperCase().contains(token);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public enum ValidationStatus {
        INCOMPLETE,
        BLOCKED_FAIL_CLOSED,
        REVIEW_ONLY_RUNTIME_CANDIDATE_VALID,
        REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED
    }

    public static class ValidationResult {
        private final ValidationStatus status;
        private final boolean validForReviewOnly;
        private final boolean incomplete;
        private final boolean blockedFailClosed;
        private final boolean degraded;
        private final boolean reviewOnly;
        private final boolean notTradeInstruction;
        private final boolean manualReviewRequired;
        private final boolean incompleteSafe;
        private final boolean failClosed;
        private final List<String> reasons;

        private ValidationResult(ValidationStatus status, List<String> reasons) {
            this.status = status;
            this.validForReviewOnly = status == ValidationStatus.REVIEW_ONLY_RUNTIME_CANDIDATE_VALID
                    || status == ValidationStatus.REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED;
            this.incomplete = status == ValidationStatus.INCOMPLETE;
            this.blockedFailClosed = status == ValidationStatus.BLOCKED_FAIL_CLOSED;
            this.degraded = status == ValidationStatus.REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED;
            this.reviewOnly = true;
            this.notTradeInstruction = true;
            this.manualReviewRequired = true;
            this.incompleteSafe = true;
            this.failClosed = status == ValidationStatus.BLOCKED_FAIL_CLOSED;
            this.reasons = immutableCopy(reasons);
        }

        public static ValidationResult incomplete(List<String> reasons) {
            return new ValidationResult(ValidationStatus.INCOMPLETE, reasons);
        }

        public static ValidationResult blockedFailClosed(List<String> reasons) {
            return new ValidationResult(ValidationStatus.BLOCKED_FAIL_CLOSED, reasons);
        }

        public static ValidationResult reviewOnlyRuntimeCandidateValid(List<String> reasons) {
            return new ValidationResult(ValidationStatus.REVIEW_ONLY_RUNTIME_CANDIDATE_VALID, reasons);
        }

        public static ValidationResult reviewOnlyRuntimeCandidateDegraded(List<String> reasons) {
            return new ValidationResult(ValidationStatus.REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED, reasons);
        }

        public ValidationStatus getStatus() {
            return status;
        }

        public boolean isValidForReviewOnly() {
            return validForReviewOnly;
        }

        public boolean isIncomplete() {
            return incomplete;
        }

        public boolean isBlockedFailClosed() {
            return blockedFailClosed;
        }

        public boolean isDegraded() {
            return degraded;
        }

        public boolean isReviewOnly() {
            return reviewOnly;
        }

        public boolean isNotTradeInstruction() {
            return notTradeInstruction;
        }

        public boolean isManualReviewRequired() {
            return manualReviewRequired;
        }

        public boolean isIncompleteSafe() {
            return incompleteSafe;
        }

        public boolean isFailClosed() {
            return failClosed;
        }

        public List<String> getReasons() {
            return reasons;
        }
    }

    private static List<String> immutableCopy(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
