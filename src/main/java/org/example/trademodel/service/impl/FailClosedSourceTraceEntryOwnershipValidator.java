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

        List<String> candidateFailures = candidateFailures(request.getRuleOwnedEntryCandidate());
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

    private List<String> candidateFailures(RuleOwnedEntryCandidateDTO candidate) {
        List<String> failures = new ArrayList<>();
        if (candidate.getCandidateEntryBoundary() == null) {
            failures.add("ruleOwnedEntryCandidate.candidateEntryBoundary");
        }
        addBlankFieldFailure(failures, "ruleOwnedEntryCandidate.entrySourceType", candidate.getEntrySourceType());
        addBlankFieldFailure(failures, "ruleOwnedEntryCandidate.entrySourceTimeframe",
                candidate.getEntrySourceTimeframe());
        addBlankFieldFailure(failures, "ruleOwnedEntryCandidate.entrySourceReason", candidate.getEntrySourceReason());
        addBlankFieldFailure(failures, "ruleOwnedEntryCandidate.entrySourceRef", candidate.getEntrySourceRef());
        return failures;
    }

    private List<String> freshnessFailures(EntrySourceFreshnessDTO freshness) {
        List<String> failures = new ArrayList<>();
        addBlankFieldFailure(failures, "freshness.freshnessStatus", freshness.getFreshnessStatus());
        if (freshness.getObservedAtMs() == null) {
            failures.add("freshness.observedAtMs");
        }
        if (freshness.getDecisionCreateTimeMs() == null) {
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
        return failures;
    }

    private void addBlankFieldFailure(List<String> failures, String field, String value) {
        if (value == null || value.isBlank()) {
            failures.add(field);
        }
    }

    private void addConflictFailure(List<String> failures, String field, Boolean value) {
        if (value == null || Boolean.TRUE.equals(value)) {
            failures.add(field);
        }
    }
}
