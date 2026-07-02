package org.example.trademodel.service.planboundary;

import org.example.trademodel.dto.planboundary.MarketStructureBoundaryDTO;
import org.example.trademodel.dto.planboundary.MarketStructureBoundaryRequest;

public interface MarketStructureBoundaryExtractor {

    MarketStructureBoundaryDTO extract(MarketStructureBoundaryRequest request);
}
