package org.example.trademodel.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.planboundary.EntryOwnershipRequest;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationResult;
import org.example.trademodel.dto.planboundary.EntrySourceConflictDTO;
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
        if (request.getFreshness() == null) {
            return EntryOwnershipValidationResult.missingSource(symbol, timeframe, List.of("freshness"));
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

    private void addConflictFailure(List<String> failures, String field, Boolean value) {
        if (value == null || Boolean.TRUE.equals(value)) {
            failures.add(field);
        }
    }
}
