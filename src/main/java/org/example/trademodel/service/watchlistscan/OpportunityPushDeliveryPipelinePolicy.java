package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditQueueResultDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushDeliveryPipelineResultDTO;

public interface OpportunityPushDeliveryPipelinePolicy {

    OpportunityPushDeliveryPipelineResultDTO evaluate(
            String symbol,
            OpportunityPushAuditQueueResultDTO queueResult
    );
}
