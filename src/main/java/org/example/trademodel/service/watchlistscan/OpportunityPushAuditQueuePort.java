package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditPersistenceResultDTO;
import org.example.trademodel.dto.watchlistscan.OpportunityPushAuditQueueResultDTO;

public interface OpportunityPushAuditQueuePort {

    OpportunityPushAuditQueueResultDTO evaluate(
            String symbol,
            OpportunityPushAuditPersistenceResultDTO persistenceResult
    );
}
