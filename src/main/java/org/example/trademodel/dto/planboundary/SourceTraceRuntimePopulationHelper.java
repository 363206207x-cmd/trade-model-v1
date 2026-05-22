package org.example.trademodel.dto.planboundary;

import java.util.List;

public final class SourceTraceRuntimePopulationHelper {

    private static final String REVIEW_ONLY = SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY.name();

    private SourceTraceRuntimePopulationHelper() {
    }

    public static SourceTraceDTO populate(
            MarketReadOnlyEvidenceSnapshotDTO snapshot,
            MarketReadOnlyCandidateResultDTO result
    ) {
        SourceTraceDTO trace = new SourceTraceDTO();
        trace.setReviewMode(resolveReviewMode(result));
        trace.setManualReviewRequired(true);
        trace.setNotTradeInstruction(true);

        if (snapshot == null) {
            trace.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
            trace.setMissingFields(List.of("snapshot"));
            trace.setBlockingReasons(List.of("missing_snapshot"));
            return trace;
        }

        trace.setSymbol(snapshot.getSymbol());
        trace.setSymbolSource("market_read_only_snapshot.symbol");
        trace.setTimeframe(snapshot.getTimeframe());
        trace.setTimeframeSource("market_read_only_snapshot.timeframe");
        trace.setSourceOwner(snapshot.getSourceOwner());
        trace.setSourceRef(snapshot.getSourceRef());
        trace.setSourceTimeframe(snapshot.getSourceTimeframe());
        trace.setSourceWindow(snapshot.getSourceWindow());
        trace.setFreshnessStatus(statusName(snapshot.getFreshnessStatus()));
        trace.setQuoteFreshnessStatus(trace.getFreshnessStatus());
        trace.setFallbackStatus(resolveFallbackStatus(snapshot, result));
        trace.setMissingFields(snapshot.getMissingFields());
        trace.setBlockingReasons(resolveBlockingReasons(result));
        return trace;
    }

    private static SourceTraceFallbackStatusEnum resolveFallbackStatus(
            MarketReadOnlyEvidenceSnapshotDTO snapshot,
            MarketReadOnlyCandidateResultDTO result
    ) {
        MarketReadOnlyCandidateStatusEnum resultStatus = result == null ? null : result.getCandidateStatus();
        if (resultStatus == MarketReadOnlyCandidateStatusEnum.BLOCKED
                || snapshot.getSnapshotStatus() == MarketReadOnlySnapshotStatusEnum.BLOCKED) {
            return SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY;
        }
        if (resultStatus == MarketReadOnlyCandidateStatusEnum.INCOMPLETE
                || snapshot.getSnapshotStatus() == MarketReadOnlySnapshotStatusEnum.INCOMPLETE) {
            return SourceTraceFallbackStatusEnum.INCOMPLETE;
        }
        return SourceTraceFallbackStatusEnum.WATCH_ONLY;
    }

    private static List<String> resolveBlockingReasons(MarketReadOnlyCandidateResultDTO result) {
        if (result == null) {
            return List.of("missing_candidate_result");
        }
        return result.getBlockingReasons();
    }

    private static String resolveReviewMode(MarketReadOnlyCandidateResultDTO result) {
        if (result == null || result.getReviewMode() == null) {
            return REVIEW_ONLY;
        }
        return result.getReviewMode().name();
    }

    private static String statusName(MarketReadOnlyEvidenceStatusEnum status) {
        return status == null ? null : status.name();
    }
}
