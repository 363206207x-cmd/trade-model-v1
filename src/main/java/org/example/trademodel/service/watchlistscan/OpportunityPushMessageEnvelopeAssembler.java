package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryPipelineResultDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushMessageEnvelopeDTO;

public interface OpportunityPushMessageEnvelopeAssembler {

    OpportunityPushMessageEnvelopeDTO evaluate(
            String symbol,
            OpportunityPushDeliveryPipelineResultDTO pipelineResult
    );
}
