package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.BoundaryCandidateDTO;
import org.example.trademodel.dto.planboundary.BoundaryEntryDTO;
import org.example.trademodel.dto.planboundary.BoundarySourceFieldsDTO;
import org.example.trademodel.dto.planboundary.BoundaryStopDTO;
import org.example.trademodel.dto.planboundary.BoundaryTakeProfitLevelDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;

import java.math.BigDecimal;
import java.util.List;

public interface BoundaryCandidateService {

    BoundaryCandidateDTO evaluateBoundaryCandidate(
            String symbol,
            String timeframe,
            SourceTraceDTO sourceTrace,
            BoundaryEntryDTO entry,
            BoundaryStopDTO stop,
            List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
            BoundarySourceFieldsDTO sourceFields,
            BigDecimal dataQualityScore
    );
}
