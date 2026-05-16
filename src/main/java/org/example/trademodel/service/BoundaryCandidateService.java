package org.example.trademodel.service;

import org.example.trademodel.dto.planboundary.BoundaryCandidateDTO;
import org.example.trademodel.dto.planboundary.BoundaryEntryDTO;
import org.example.trademodel.dto.planboundary.BoundarySourceFieldsDTO;
import org.example.trademodel.dto.planboundary.BoundaryStopDTO;
import org.example.trademodel.dto.planboundary.BoundaryTakeProfitLevelDTO;
import org.example.trademodel.dto.planboundary.DerivativesRiskContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.vo.DashboardDetailResponseVO;

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

    BoundaryCandidateDTO evaluateBoundaryCandidate(
            String symbol,
            String timeframe,
            RuntimeKlineContextDTO runtimeKlineContext,
            DerivativesRiskContextDTO derivativesRiskContext,
            BoundaryEntryDTO entry,
            BoundaryStopDTO stop,
            List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
            BoundarySourceFieldsDTO sourceFields,
            BigDecimal dataQualityScore
    );

    BoundaryCandidateDTO evaluateBoundaryCandidate(
            String symbol,
            String timeframe,
            SourceTraceDTO sourceTrace,
            BoundaryEntryDTO entry,
            BoundaryStopDTO stop,
            List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
            BoundarySourceFieldsDTO sourceFields,
            BigDecimal dataQualityScore,
            DashboardDetailResponseVO.RiskActionGuardDisplayVO riskActionGuardDisplay
    );
}
