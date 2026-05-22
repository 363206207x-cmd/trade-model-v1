package org.example.trademodel.service.planboundary;

import org.example.trademodel.dto.planboundary.MarketReadOnlyCandidateResultDTO;
import org.example.trademodel.dto.planboundary.MarketReadOnlyEvidenceSnapshotDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;

public interface SourceTraceRuntimePopulationService {

    SourceTraceDTO populate(
            MarketReadOnlyEvidenceSnapshotDTO snapshot,
            MarketReadOnlyCandidateResultDTO result
    );
}
