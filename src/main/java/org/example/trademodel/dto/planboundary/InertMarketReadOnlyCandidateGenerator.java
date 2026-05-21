package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

public class InertMarketReadOnlyCandidateGenerator implements MarketReadOnlyCandidateGenerator {

    @Override
    public MarketReadOnlyCandidateResultDTO review(MarketReadOnlyEvidenceSnapshotDTO snapshot) {
        return MarketReadOnlyCandidateResultDTO.builder()
                .snapshot(snapshot)
                .entryReview("entry_review_token:review_only_source_context")
                .stopReview("stop_review_token:review_only_source_context")
                .tpReview("tp_review_token:review_only_source_context")
                .rrReview("rr_review_token:review_only_source_context")
                .sourceOwnershipSummary(sourceOwnershipSummary(snapshot))
                .numericSourceSummary("numeric_source_summary:review_token_only")
                .riskActionGuardReview(riskActionGuardReview(snapshot))
                .blockingReasons(directBlockers(snapshot))
                .build();
    }

    private String sourceOwnershipSummary(MarketReadOnlyEvidenceSnapshotDTO snapshot) {
        if (snapshot == null || isBlank(snapshot.getSourceOwner())) {
            return "source_owner_summary:missing_snapshot_or_owner";
        }
        return "source_owner_summary:" + snapshot.getSourceOwner();
    }

    private String riskActionGuardReview(MarketReadOnlyEvidenceSnapshotDTO snapshot) {
        if (snapshot == null || snapshot.getRiskActionGuardBlockers().isEmpty()) {
            return "risk_action_guard:review_only_no_direct_blocker";
        }
        return "risk_action_guard:review_only_blocked";
    }

    private List<String> directBlockers(MarketReadOnlyEvidenceSnapshotDTO snapshot) {
        List<String> blockers = new ArrayList<>();
        if (snapshot == null) {
            return blockers;
        }
        for (String marker : snapshot.getForbiddenInputMarkers()) {
            blockers.add("direct_forbidden_input:" + marker);
        }
        for (String marker : snapshot.getNoGoEvidenceMarkers()) {
            blockers.add("direct_no_go:" + marker);
        }
        for (String marker : snapshot.getRiskActionGuardBlockers()) {
            blockers.add("direct_risk_action_guard:" + marker);
        }
        return blockers;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
