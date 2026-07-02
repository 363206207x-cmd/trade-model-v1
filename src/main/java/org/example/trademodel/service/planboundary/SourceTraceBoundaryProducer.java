package org.example.trademodel.service.planboundary;

import org.example.trademodel.dto.planboundary.MarketStructureBoundaryDTO;
import org.example.trademodel.dto.planboundary.SourceTraceBoundaryProducerResult;

public interface SourceTraceBoundaryProducer {

    SourceTraceBoundaryProducerResult produce(MarketStructureBoundaryDTO boundary);
}
