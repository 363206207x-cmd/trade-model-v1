package org.example.trademodel.service.planboundary;

import org.example.trademodel.dto.planboundary.MarketReadOnlyCandidateResultDTO;
import org.example.trademodel.dto.planboundary.MarketReadOnlyEvidenceSnapshotDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceRuntimePopulationHelper;
import org.springframework.stereotype.Service;

@Service
public class SourceTraceRuntimePopulationServiceImpl implements SourceTraceRuntimePopulationService {

    @Override
    public SourceTraceDTO populate(
            MarketReadOnlyEvidenceSnapshotDTO snapshot,
            MarketReadOnlyCandidateResultDTO result
    ) {
        return SourceTraceRuntimePopulationHelper.populate(snapshot, result);
    }
}
