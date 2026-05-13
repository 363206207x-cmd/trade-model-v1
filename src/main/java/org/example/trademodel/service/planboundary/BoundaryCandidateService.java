package org.example.trademodel.service.planboundary;

import org.example.trademodel.dto.planboundary.BoundaryCandidateDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;

import java.math.BigDecimal;

public interface BoundaryCandidateService {

    BoundaryCandidateDTO evaluateBoundaryCandidate(String symbol,
                                                   String timeframe,
                                                   RuntimeKlineContextDTO runtimeKlineContext,
                                                   BigDecimal latestPrice,
                                                   BigDecimal dataQualityScore);
}
