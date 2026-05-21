package org.example.trademodel.dto.planboundary;

public interface MarketReadOnlyCandidateGenerator {

    MarketReadOnlyCandidateResultDTO review(MarketReadOnlyEvidenceSnapshotDTO snapshot);
}
