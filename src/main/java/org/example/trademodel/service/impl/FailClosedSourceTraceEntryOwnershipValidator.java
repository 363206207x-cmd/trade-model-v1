package org.example.trademodel.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.planboundary.EntryOwnershipRequest;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationResult;
import org.example.trademodel.dto.planboundary.EntrySourceConflictDTO;
import org.example.trademodel.dto.planboundary.EntrySourceFreshnessDTO;
import org.example.trademodel.dto.planboundary.RuleOwnedEntryCandidateDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.service.SourceTraceEntryOwnershipValidator;
import org.springframework.stereotype.Service;

@Service
public class FailClosedSourceTraceEntryOwnershipValidator implements SourceTraceEntryOwnershipValidator {

    private static final String SUPPORTED_ENTRY_SOURCE_TYPE = "rule-owned-boundary";
    private static final String FRESHNESS_STATUS_FRESH = "FRESH";
    private static final List<String> UNWIRED_COMPLETION_PATH = List.of(
            "sourceTraceEntryOwnershipCompletionPath"
    );

    @Override
    public EntryOwnershipValidationResult validateEntryOwnership(EntryOwnershipRequest request) {
        if (request == null) {
            return EntryOwnershipValidationResult.missingSource(null, null, List.of("entryOwnershipRequest"));
        }

        RuntimeKlineContextDTO runtimeKlineContext = request.getRuntimeKlineContext();
        if (runtimeKlineContext == null) {
            return EntryOwnershipValidationResult.missingSource(null, null, List.of("runtimeKlineContext"));
        }

        String symbol = runtimeKlineContext.getSymbol();
        String timeframe = runtimeKlineContext.getTimeframe();
        if (!request.isManualReviewRequired()) {
            return EntryOwnershipValidationResult.missingSource(symbol, timeframe, List.of("manualReviewRequired"));
        }
        if (!request.isNotTradeInstruction()) {
            return EntryOwnershipValidationResult.missingSource(symbol, timeframe, List.of("notTradeInstruction"));
        }
        if (request.getRuleOwnedEntryCandidate() == null) {
            return EntryOwnershipValidationResult.missingSource(symbol, timeframe, List.of("ruleOwnedEntryCandidate"));
        }

        List<String> candidateFailures = candidateFailures(
                runtimeKlineContext,
                request.getRuleOwnedEntryCandidate()
        );
        if (!candidateFailures.isEmpty()) {
            return EntryOwnershipValidationResult.missingSource(symbol, timeframe, candidateFailures);
        }

        if (request.getFreshness() == null) {
            return EntryOwnershipValidationResult.missingSource(symbol, timeframe, List.of("freshness"));
        }

        List<String> freshnessFailures = freshnessFailures(request.getFreshness());
        if (!freshnessFailures.isEmpty()) {
            return EntryOwnershipValidationResult.missingSource(symbol, timeframe, freshnessFailures);
        }

        if (request.getConflict() == null) {
            return EntryOwnershipValidationResult.missingSource(symbol, timeframe, List.of("conflict"));
        }

        List<String> conflictFailures = conflictFailures(request.getConflict());
        if (!conflictFailures.isEmpty()) {
            return EntryOwnershipValidationResult.missingSource(symbol, timeframe, conflictFailures);
        }

        return EntryOwnershipValidationResult.missingSource(symbol, timeframe, UNWIRED_COMPLETION_PATH);
    }

    private List<String> candidateFailures(
            RuntimeKlineContextDTO runtimeKlineContext,
            RuleOwnedEntryCandidateDTO candidate
    ) {
        List<String> failures = new ArrayList<>();
        addBlankFieldFailure(failures, "ruleOwnedEntryCandidate.symbol", candidate.getSymbol());
        if (hasText(runtimeKlineContext.getSymbol()) && hasText(candidate.getSymbol())
                && !runtimeKlineContext.getSymbol().equals(candidate.getSymbol())) {
            failures.add("ruleOwnedEntryCandidate.symbol");
        }
        addBlankFieldFailure(failures, "ruleOwnedEntryCandidate.decisionTimeframe",
                candidate.getDecisionTimeframe());
        if (hasText(runtimeKlineContext.getTimeframe()) && hasText(candidate.getDecisionTimeframe())
                && !runtimeKlineContext.getTimeframe().equals(candidate.getDecisionTimeframe())) {
            failures.add("ruleOwnedEntryCandidate.decisionTimeframe");
        }
        if (candidate.getCandidateEntryBoundary() == null) {
            failures.add("ruleOwnedEntryCandidate.candidateEntryBoundary");
        }
        addBlankFieldFailure(failures, "ruleOwnedEntryCandidate.entrySourceType", candidate.getEntrySourceType());
        if (hasText(candidate.getEntrySourceType())
                && !SUPPORTED_ENTRY_SOURCE_TYPE.equals(candidate.getEntrySourceType())) {
            failures.add("ruleOwnedEntryCandidate.entrySourceType");
        }
        addBlankFieldFailure(failures, "ruleOwnedEntryCandidate.entrySourceTimeframe",
                candidate.getEntrySourceTimeframe());
        if (hasText(runtimeKlineContext.getTimeframe()) && hasText(candidate.getEntrySourceTimeframe())
                && !runtimeKlineContext.getTimeframe().equals(candidate.getEntrySourceTimeframe())) {
            failures.add("ruleOwnedEntryCandidate.entrySourceTimeframe");
        }
        addBlankFieldFailure(failures, "ruleOwnedEntryCandidate.entrySourceReason", candidate.getEntrySourceReason());
        addBlankFieldFailure(failures, "ruleOwnedEntryCandidate.entrySourceRef", candidate.getEntrySourceRef());
        if (hasAmbiguousSourceRef(candidate.getEntrySourceRef())) {
            failures.add("ruleOwnedEntryCandidate.entrySourceRef");
        }
        addBlankFieldFailure(failures, "ruleOwnedEntryCandidate.ruleId", candidate.getRuleId());
        addBlankFieldFailure(failures, "ruleOwnedEntryCandidate.ruleVersion", candidate.getRuleVersion());
        addBlankFieldFailure(failures, "ruleOwnedEntryCandidate.sourceWindow", candidate.getSourceWindow());
        return failures;
    }

    private List<String> freshnessFailures(EntrySourceFreshnessDTO freshness) {
        List<String> failures = new ArrayList<>();
        addBlankFieldFailure(failures, "freshness.freshnessStatus", freshness.getFreshnessStatus());
        if (hasText(freshness.getFreshnessStatus())
                && !FRESHNESS_STATUS_FRESH.equals(freshness.getFreshnessStatus())) {
            failures.add("freshness.freshnessStatus");
        }
        if (freshness.getObservedAtMs() == null) {
            failures.add("freshness.observedAtMs");
        }
        if (freshness.getDecisionCreateTimeMs() == null) {
            failures.add("freshness.decisionCreateTimeMs");
        }
        if (freshness.getObservedAtMs() != null
                && freshness.getDecisionCreateTimeMs() != null
                && freshness.getObservedAtMs() > freshness.getDecisionCreateTimeMs()) {
            failures.add("freshness.observedAtMs");
            failures.add("freshness.decisionCreateTimeMs");
        }
        return failures;
    }

    private List<String> conflictFailures(EntrySourceConflictDTO conflict) {
        List<String> failures = new ArrayList<>();
        addConflictFailure(failures, "conflictsWithStop", conflict.getConflictsWithStop());
        addConflictFailure(failures, "conflictsWithTakeProfit", conflict.getConflictsWithTakeProfit());
        addConflictFailure(failures, "conflictsWithRiskReward", conflict.getConflictsWithRiskReward());
        addConflictFailure(failures, "conflictsWithLiquidity", conflict.getConflictsWithLiquidity());
        addConflictFailure(failures, "conflictsWithMultiTimeframe", conflict.getConflictsWithMultiTimeframe());
        addConflictFailure(failures, "conflictsWithEvent", conflict.getConflictsWithEvent());
        addConflictFailure(failures, "conflictsWithWick", conflict.getConflictsWithWick());
        for (String reason : conflict.getConflictReasons()) {
            if ("LIQUIDITY_STRESS".equals(reason) || "STAMPEDE".equals(reason)) {
                addFailureIfMissing(failures, "conflictsWithLiquidity");
            }
            if ("MISSING_EVENT_DATA".equals(reason)) {
                addFailureIfMissing(failures, "conflictsWithEvent");
            }
            if ("MULTI_TIMEFRAME_AGREEMENT_ONLY".equals(reason)) {
                addFailureIfMissing(failures, "conflictsWithMultiTimeframe");
            }
            if ("WICK_PIN_BAR_ONLY".equals(reason)) {
                addFailureIfMissing(failures, "conflictsWithWick");
            }
        }
        return failures;
    }

    private void addBlankFieldFailure(List<String> failures, String field, String value) {
        if (value == null || value.isBlank()) {
            failures.add(field);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasAmbiguousSourceRef(String value) {
        return hasText(value) && (value.contains(",") || value.contains("|") || value.contains(";"));
    }

    private void addConflictFailure(List<String> failures, String field, Boolean value) {
        if (value == null || Boolean.TRUE.equals(value)) {
            failures.add(field);
        }
    }

    private void addFailureIfMissing(List<String> failures, String field) {
        if (!failures.contains(field)) {
            failures.add(field);
        }
    }
}
